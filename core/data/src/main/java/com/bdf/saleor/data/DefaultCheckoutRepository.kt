package com.bdf.saleor.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.bdf.saleor.core.datastore.CheckoutStore
import com.bdf.saleor.data.model.AddressDraft
import com.bdf.saleor.data.model.CheckoutSession
import com.bdf.saleor.data.model.DeliveryOption
import com.bdf.saleor.data.model.Money
import com.bdf.saleor.data.model.CompletedOrder
import com.bdf.saleor.data.model.PaymentGateways
import com.bdf.saleor.data.model.PaymentResult
import com.bdf.saleor.graphql.CheckoutBillingAddressUpdateMutation
import com.bdf.saleor.graphql.CheckoutCompleteMutation
import com.bdf.saleor.graphql.CheckoutCustomerAttachMutation
import com.bdf.saleor.graphql.CheckoutDeliveryMethodUpdateMutation
import com.bdf.saleor.graphql.CheckoutDetailsQuery
import com.bdf.saleor.graphql.CheckoutEmailUpdateMutation
import com.bdf.saleor.graphql.CheckoutShippingAddressUpdateMutation
import com.bdf.saleor.graphql.DeliveryOptionsCalculateMutation
import com.bdf.saleor.graphql.PaymentGatewayInitializeMutation
import com.bdf.saleor.graphql.TransactionInitializeMutation
import com.bdf.saleor.graphql.TransactionProcessMutation
import com.bdf.saleor.graphql.fragment.CheckoutDetails
import com.bdf.saleor.graphql.type.LanguageCodeEnum
import com.bdf.saleor.graphql.type.PaymentGatewayToInitialize
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCheckoutRepository @Inject constructor(
    private val apolloClient: ApolloClient,
    private val checkoutStore: CheckoutStore,
    private val cartRepository: CartRepository,
    private val config: SaleorCatalogConfig,
) : CheckoutRepository {
    private val languageCode: LanguageCodeEnum
        get() = LanguageCodeEnum.safeValueOf(config.graphqlLanguageCode)

    override suspend fun loadCheckout(): Result<CheckoutSession> = runCatching {
        publish(requireDetails())
    }

    override suspend fun updateEmail(email: String): Result<CheckoutSession> = runCatching {
        val id = requireId()
        val data = apolloClient.mutation(
            CheckoutEmailUpdateMutation(id = id, email = email.trim(), languageCode = languageCode),
        ).execute().dataAssertNoErrors
        val errors = data.checkoutEmailUpdate?.errors.orEmpty()
        if (errors.isNotEmpty()) error(errors.first().message ?: "이메일을 저장하지 못했습니다")
        publish(data.checkoutEmailUpdate?.checkout?.checkoutDetails)
    }

    override suspend fun attachCustomer(): Result<CheckoutSession> = runCatching {
        val id = requireId()
        val data = apolloClient.mutation(
            CheckoutCustomerAttachMutation(id = id, languageCode = languageCode),
        ).execute().dataAssertNoErrors
        val errors = data.checkoutCustomerAttach?.errors.orEmpty()
        if (errors.isNotEmpty()) error(errors.first().message ?: "계정을 연결하지 못했습니다")
        publish(data.checkoutCustomerAttach?.checkout?.checkoutDetails)
    }

    override suspend fun updateShippingAddress(
        draft: AddressDraft,
        saveAddress: Boolean,
    ): Result<CheckoutSession> = runCatching {
        val id = requireId()
        val data = apolloClient.mutation(
            CheckoutShippingAddressUpdateMutation(
                id = id,
                shippingAddress = draft.toCheckoutInput(),
                saveAddress = Optional.present(saveAddress),
                languageCode = languageCode,
            ),
        ).execute().dataAssertNoErrors
        val errors = data.checkoutShippingAddressUpdate?.errors.orEmpty()
        if (errors.isNotEmpty()) error(errors.first().message ?: "배송지를 저장하지 못했습니다")
        publish(data.checkoutShippingAddressUpdate?.checkout?.checkoutDetails)
    }

    override suspend fun updateBillingAddress(
        draft: AddressDraft,
        saveAddress: Boolean,
    ): Result<CheckoutSession> = runCatching {
        val id = requireId()
        val data = apolloClient.mutation(
            CheckoutBillingAddressUpdateMutation(
                id = id,
                billingAddress = draft.toCheckoutInput(),
                saveAddress = Optional.present(saveAddress),
                languageCode = languageCode,
            ),
        ).execute().dataAssertNoErrors
        val errors = data.checkoutBillingAddressUpdate?.errors.orEmpty()
        if (errors.isNotEmpty()) error(errors.first().message ?: "청구지를 저장하지 못했습니다")
        publish(data.checkoutBillingAddressUpdate?.checkout?.checkoutDetails)
    }

    override suspend fun calculateDeliveryOptions(): Result<List<DeliveryOption>> = runCatching {
        val id = requireId()
        val data = apolloClient.mutation(DeliveryOptionsCalculateMutation(id = id))
            .execute()
            .dataAssertNoErrors
        val payload = data.deliveryOptionsCalculate
        val errors = payload?.errors.orEmpty()
        if (errors.isNotEmpty()) error(errors.first().message ?: "배송 방법을 불러오지 못했습니다")
        payload?.deliveries.orEmpty().mapNotNull { delivery ->
            val method = delivery.shippingMethod ?: return@mapNotNull null
            DeliveryOption(
                id = delivery.id,
                shippingMethodId = method.id,
                name = method.name,
                price = Money(method.price.amount, method.price.currency),
                minDeliveryDays = method.minimumDeliveryDays,
                maxDeliveryDays = method.maximumDeliveryDays,
                active = method.active,
            )
        }
    }

    override suspend fun updateDeliveryMethod(deliveryMethodId: String): Result<CheckoutSession> = runCatching {
        val id = requireId()
        val data = apolloClient.mutation(
            CheckoutDeliveryMethodUpdateMutation(
                id = id,
                deliveryMethodId = deliveryMethodId,
                languageCode = languageCode,
            ),
        ).execute().dataAssertNoErrors
        val errors = data.checkoutDeliveryMethodUpdate?.errors.orEmpty()
        if (errors.isNotEmpty()) error(errors.first().message ?: "배송 방법을 저장하지 못했습니다")
        publish(data.checkoutDeliveryMethodUpdate?.checkout?.checkoutDetails)
    }

    override suspend fun initializeTossClientKey(): Result<String> = runCatching {
        val id = requireId()
        val data = apolloClient.mutation(
            PaymentGatewayInitializeMutation(
                id = id,
                paymentGateways = Optional.present(
                    listOf(PaymentGatewayToInitialize(id = PaymentGateways.TOSS)),
                ),
            ),
        ).execute().dataAssertNoErrors
        val payload = data.paymentGatewayInitialize
        val errors = payload?.errors.orEmpty()
        if (errors.isNotEmpty()) error(errors.first().message ?: "결제 수단을 초기화하지 못했습니다")
        val config = payload?.gatewayConfigs.orEmpty().firstOrNull { it.id == PaymentGateways.TOSS }
            ?: payload?.gatewayConfigs.orEmpty().firstOrNull()
        val configErrors = config?.errors.orEmpty()
        if (configErrors.isNotEmpty()) error(configErrors.first().message ?: "Toss 설정을 불러오지 못했습니다")
        parseClientKey(config?.data) ?: error("Toss clientKey가 없습니다")
    }

    override suspend fun initializeTransaction(
        gatewayId: String,
        amount: Double?,
    ): Result<PaymentResult> = runCatching {
        val id = requireId()
        val data = apolloClient.mutation(
            TransactionInitializeMutation(
                id = id,
                paymentGateway = PaymentGatewayToInitialize(id = gatewayId),
                amount = Optional.presentIfNotNull(amount),
            ),
        ).execute().dataAssertNoErrors
        val payload = data.transactionInitialize ?: error("결제를 시작하지 못했습니다")
        if (payload.errors.isNotEmpty()) {
            error(payload.errors.first().message ?: "결제를 시작하지 못했습니다")
        }
        val eventType = payload.transactionEvent?.type?.rawValue
        if (eventType != null && FAILED_TRANSACTION_EVENTS.contains(eventType)) {
            error(payload.transactionEvent?.message ?: "결제에 실패했습니다")
        }
        if (gatewayId == PaymentGateways.POINTS) {
            val authorized = payload.transaction?.authorizedAmount?.amount
            return@runCatching PaymentResult(
                success = true,
                transactionId = payload.transaction?.id,
                authorizedAmount = authorized ?: amount,
                amount = authorized ?: amount,
                currency = payload.transaction?.authorizedAmount?.currency,
            )
        }
        val parsed = parseTossTransactionData(
            transactionId = payload.transaction?.id,
            data = payload.data,
            authorizedAmount = payload.transaction?.authorizedAmount?.amount,
        )
        if (!parsed.success) error(parsed.message ?: "결제 정보를 확인할 수 없습니다")
        parsed
    }

    override suspend fun processTransaction(
        transactionId: String,
        paymentKey: String,
        orderId: String,
        amount: Double,
    ): Result<PaymentResult> = runCatching {
        val data = apolloClient.mutation(
            TransactionProcessMutation(
                id = transactionId,
                data = Optional.present(
                    mapOf(
                        "paymentKey" to paymentKey,
                        "orderId" to orderId,
                        "amount" to amount.toInt(),
                    ),
                ),
            ),
        ).execute().dataAssertNoErrors
        val payload = data.transactionProcess ?: error("결제를 확인하지 못했습니다")
        if (payload.errors.isNotEmpty()) {
            error(payload.errors.first().message ?: "결제를 확인하지 못했습니다")
        }
        val eventType = payload.transactionEvent?.type?.rawValue
        if (eventType != null && FAILED_TRANSACTION_EVENTS.contains(eventType)) {
            error(payload.transactionEvent?.message ?: "결제에 실패했습니다")
        }
        PaymentResult(success = true, transactionId = payload.transaction?.id, orderId = orderId, amount = amount)
    }

    override suspend fun completeCheckout(): Result<CompletedOrder> = runCatching {
        val id = requireId()
        val data = apolloClient.mutation(CheckoutCompleteMutation(id = id))
            .execute()
            .dataAssertNoErrors
        val payload = data.checkoutComplete ?: error("주문을 완료하지 못했습니다")
        if (payload.errors.isNotEmpty()) {
            error(payload.errors.first().message ?: "주문을 완료하지 못했습니다")
        }
        val order = payload.order ?: error("주문을 완료하지 못했습니다")
        cartRepository.clearLocal()
        CompletedOrder(id = order.id, number = order.number)
    }

    private suspend fun requireId(): String =
        checkoutStore.checkoutId(config.channel) ?: error("장바구니가 비어 있습니다")

    private suspend fun requireDetails(): CheckoutDetails {
        val id = requireId()
        val data = apolloClient.query(CheckoutDetailsQuery(id = id, languageCode = languageCode))
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
            .dataAssertNoErrors
        return data.checkout?.checkoutDetails ?: error("결제 정보를 불러오지 못했습니다")
    }

    private suspend fun publish(details: CheckoutDetails?): CheckoutSession {
        val session = details?.toSession() ?: error("결제 정보를 불러오지 못했습니다")
        cartRepository.replace(session.toCart())
        return session
    }

    private companion object {
        val FAILED_TRANSACTION_EVENTS = setOf(
            "AUTHORIZATION_FAILURE",
            "AUTHORIZATION_ADJUSTMENT_FAILURE",
            "CHARGE_FAILURE",
            "REFUND_FAILURE",
            "CANCEL_FAILURE",
        )
    }
}
