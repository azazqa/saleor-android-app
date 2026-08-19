package com.bdf.saleor.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.bdf.saleor.core.datastore.CheckoutStore
import com.bdf.saleor.core.datastore.HeldCartLine
import com.bdf.saleor.data.model.Cart
import com.bdf.saleor.graphql.CheckoutCreateMutation
import com.bdf.saleor.graphql.CheckoutCustomerAttachMutation
import com.bdf.saleor.graphql.CheckoutDetailsQuery
import com.bdf.saleor.graphql.CheckoutLinesAddMutation
import com.bdf.saleor.graphql.CheckoutLinesDeleteMutation
import com.bdf.saleor.graphql.CheckoutLinesUpdateMutation
import com.bdf.saleor.graphql.CurrentUserCheckoutsQuery
import com.bdf.saleor.graphql.fragment.CheckoutDetails
import com.bdf.saleor.graphql.type.CheckoutLineInput
import com.bdf.saleor.graphql.type.CheckoutLineUpdateInput
import com.bdf.saleor.graphql.type.LanguageCodeEnum
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class DefaultCartRepository @Inject constructor(
    private val apolloClient: ApolloClient,
    private val checkoutStore: CheckoutStore,
    private val config: SaleorCatalogConfig,
) : CartRepository {
    private val _cart = MutableStateFlow<Cart?>(null)
    override val cart: StateFlow<Cart?> = _cart.asStateFlow()

    private val languageCode: LanguageCodeEnum
        get() = LanguageCodeEnum.safeValueOf(config.graphqlLanguageCode)

    override suspend fun refresh() {
        val id = checkoutStore.checkoutId(config.channel) ?: run {
            _cart.value = null
            return
        }
        val details = runCatching { fetchCheckout(id) }.getOrNull()
        if (details == null) {
            checkoutStore.clear(config.channel)
            _cart.value = null
            return
        }
        _cart.value = details.toSession().toCart()
    }

    override suspend fun addLine(variantId: String, quantity: Int): Result<Cart> = runCatching {
        val checkoutId = requireCheckoutId()
        val data = apolloClient.mutation(
            CheckoutLinesAddMutation(
                id = checkoutId,
                lines = listOf(CheckoutLineInput(quantity = quantity, variantId = variantId)),
                languageCode = languageCode,
            ),
        ).execute().dataAssertNoErrors
        val errors = data.checkoutLinesAdd?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            error(errors.first().message ?: "장바구니에 담지 못했습니다")
        }
        publish(data.checkoutLinesAdd?.checkout?.checkoutDetails)
    }

    override suspend fun updateLineQuantity(lineId: String, quantity: Int): Result<Cart> = runCatching {
        val checkoutId = checkoutStore.checkoutId(config.channel) ?: error("장바구니가 비어 있습니다")
        val data = apolloClient.mutation(
            CheckoutLinesUpdateMutation(
                id = checkoutId,
                lines = listOf(
                    CheckoutLineUpdateInput(
                        lineId = Optional.present(lineId),
                        quantity = Optional.present(quantity),
                    ),
                ),
                languageCode = languageCode,
            ),
        ).execute().dataAssertNoErrors
        val errors = data.checkoutLinesUpdate?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            error(errors.first().message ?: "수량을 변경하지 못했습니다")
        }
        publish(data.checkoutLinesUpdate?.checkout?.checkoutDetails)
    }

    override suspend fun removeLine(lineId: String): Result<Cart> = runCatching {
        val checkoutId = checkoutStore.checkoutId(config.channel) ?: error("장바구니가 비어 있습니다")
        val data = apolloClient.mutation(
            CheckoutLinesDeleteMutation(
                id = checkoutId,
                lineIds = listOf(lineId),
                languageCode = languageCode,
            ),
        ).execute().dataAssertNoErrors
        val errors = data.checkoutLinesDelete?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            error(errors.first().message ?: "상품을 삭제하지 못했습니다")
        }
        val details = data.checkoutLinesDelete?.checkout?.checkoutDetails
        if (details == null || details.lines.isEmpty()) {
            checkoutStore.clear(config.channel)
            _cart.value = null
            return@runCatching Cart(
                id = checkoutId,
                lines = emptyList(),
                subtotal = null,
                shipping = null,
                total = null,
                quantity = 0,
            )
        }
        publish(details)
    }

    override suspend fun replace(cart: Cart?) {
        _cart.value = cart
        if (cart == null) {
            checkoutStore.clear(config.channel)
        } else {
            checkoutStore.setCheckoutId(config.channel, cart.id)
        }
    }

    override suspend fun clearLocal() {
        checkoutStore.clear(config.channel)
        _cart.value = null
    }

    override suspend fun adoptLoggedInCart() {
        val guestId = checkoutStore.checkoutId(config.channel)
        val guestLines = snapshotGuestLines(guestId)
        val userCheckouts = runCatching { fetchUserCheckouts() }.getOrElse { emptyList() }
        applyLoginPlan(planCartLogin(guestId, guestLines, userCheckouts), guestLines)
    }

    override suspend fun releaseOnLogout() {
        runCatching { restoreParkedLines() }
        checkoutStore.setHeldCartLines(config.channel, emptyList())
        clearLocal()
    }

    override suspend fun parkUnselectedLines(selectedLineIds: Set<String>): Result<Cart> = runCatching {
        val current = _cart.value ?: error("장바구니가 비어 있습니다")
        val remaining = current.lines.filter { it.id in selectedLineIds }
        if (remaining.isEmpty()) error("상품을 선택해 주세요")
        val parked = current.lines.filter { it.id !in selectedLineIds }
        checkoutStore.setHeldCartLines(
            config.channel,
            parked.map { HeldCartLine(variantId = it.variantId, quantity = it.quantity) },
        )
        parked.forEach { line ->
            removeLine(line.id).getOrThrow()
        }
        _cart.value ?: Cart(
            id = current.id,
            lines = emptyList(),
            subtotal = null,
            shipping = null,
            total = null,
            quantity = 0,
        )
    }

    override suspend fun restoreParkedLines(): Result<Set<String>> = runCatching {
        val held = checkoutStore.heldCartLines(config.channel)
        if (held.isEmpty()) return@runCatching emptySet()
        checkoutStore.setHeldCartLines(config.channel, emptyList())
        val restoredLineIds = mutableSetOf<String>()
        val remaining = mutableListOf<HeldCartLine>()
        held.forEach { line ->
            val before = _cart.value?.lines.orEmpty().map { it.id }.toSet()
            val result = addLine(line.variantId, line.quantity)
            if (result.isFailure) {
                remaining += line
            } else {
                val added = _cart.value?.lines.orEmpty()
                    .map { it.id }
                    .filterNot { it in before }
                restoredLineIds += added
            }
        }
        if (remaining.isNotEmpty()) {
            checkoutStore.setHeldCartLines(config.channel, remaining)
        }
        restoredLineIds
    }

    private suspend fun requireCheckoutId(): String {
        checkoutStore.checkoutId(config.channel)?.let { existing ->
            val stillValid = runCatching { fetchCheckout(existing) }.getOrNull()
            if (stillValid != null) return existing
            checkoutStore.clear(config.channel)
        }
        val data = apolloClient.mutation(
            CheckoutCreateMutation(channel = config.channel, languageCode = languageCode),
        ).execute().dataAssertNoErrors
        val errors = data.checkoutCreate?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            error(errors.first().message ?: "장바구니를 만들지 못했습니다")
        }
        val details = data.checkoutCreate?.checkout?.checkoutDetails
            ?: error("장바구니를 만들지 못했습니다")
        checkoutStore.setCheckoutId(config.channel, details.id)
        _cart.value = details.toSession().toCart()
        return details.id
    }

    private suspend fun fetchCheckout(id: String): CheckoutDetails? {
        val data = apolloClient.query(CheckoutDetailsQuery(id = id, languageCode = languageCode))
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
            .dataAssertNoErrors
        return data.checkout?.checkoutDetails
    }

    private suspend fun publish(details: CheckoutDetails?): Cart {
        val session = details?.toSession() ?: error("장바구니를 불러오지 못했습니다")
        checkoutStore.setCheckoutId(config.channel, session.id)
        val cart = session.toCart()
        _cart.value = cart
        return cart
    }

    private suspend fun snapshotGuestLines(guestId: String?): List<HeldCartLine> {
        val fromMemory = _cart.value?.lines.orEmpty()
            .filter { it.quantity > 0 }
            .map { HeldCartLine(variantId = it.variantId, quantity = it.quantity) }
        if (fromMemory.isNotEmpty()) return fromMemory
        if (guestId.isNullOrBlank()) return emptyList()
        val details = runCatching { fetchCheckout(guestId) }.getOrNull() ?: return emptyList()
        return details.lines.mapNotNull { line ->
            val item = line.checkoutLineDetails
            if (item.quantity <= 0 || item.variant.id.isBlank()) null
            else HeldCartLine(variantId = item.variant.id, quantity = item.quantity)
        }
    }

    private suspend fun fetchUserCheckouts(): List<UserCheckoutSummary> {
        val data = apolloClient.query(CurrentUserCheckoutsQuery(channel = config.channel))
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
            .dataAssertNoErrors
        return data.me?.checkouts?.edges.orEmpty().map { edge ->
            UserCheckoutSummary(id = edge.node.id, quantity = edge.node.quantity)
        }
    }

    private suspend fun applyLoginPlan(plan: CartLoginPlan, guestLines: List<HeldCartLine>) {
        when {
            plan.attachGuestCheckout -> {
                val id = plan.targetCheckoutId ?: return
                val attached = attachCheckout(id)
                if (attached != null) {
                    publish(attached)
                } else {
                    checkoutStore.clear(config.channel)
                    _cart.value = null
                    mergeLines(guestLines)
                }
            }
            plan.targetCheckoutId != null -> {
                checkoutStore.setCheckoutId(config.channel, plan.targetCheckoutId)
                refresh()
                mergeLines(plan.linesToMerge)
            }
            else -> {
                checkoutStore.clear(config.channel)
                _cart.value = null
            }
        }
    }

    private suspend fun attachCheckout(id: String): CheckoutDetails? {
        val data = runCatching {
            apolloClient.mutation(
                CheckoutCustomerAttachMutation(id = id, languageCode = languageCode),
            ).execute().dataAssertNoErrors
        }.getOrNull() ?: return null
        if (data.checkoutCustomerAttach?.errors.orEmpty().isNotEmpty()) return null
        return data.checkoutCustomerAttach?.checkout?.checkoutDetails
    }

    private suspend fun mergeLines(lines: List<HeldCartLine>) {
        lines.forEach { line ->
            addLine(line.variantId, line.quantity)
        }
    }
}
