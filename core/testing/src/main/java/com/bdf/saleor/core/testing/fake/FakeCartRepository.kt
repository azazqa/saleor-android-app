package com.bdf.saleor.core.testing.fake

import com.bdf.saleor.core.data.CartRepository
import com.bdf.saleor.core.model.Cart
import com.bdf.saleor.core.model.CartLine
import com.bdf.saleor.core.model.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeCartRepository : CartRepository {
    private val _cart = MutableStateFlow<Cart?>(null)
    override val cart: StateFlow<Cart?> = _cart.asStateFlow()

    var parkedLines: List<Pair<String, Int>> = emptyList()
    var lastParkedSelectedIds: Set<String>? = null
    var shouldFailAdd: Boolean = false
    var shouldFailUpdate: Boolean = false
    var lastAddedVariantId: String? = null
    var refreshCount: Int = 0
    var clearCount: Int = 0
    var adoptLoggedInCount: Int = 0
    var releaseOnLogoutCount: Int = 0

    override suspend fun refresh() {
        refreshCount += 1
    }

    override suspend fun addLine(variantId: String, quantity: Int): Result<Cart> {
        lastAddedVariantId = variantId
        if (shouldFailAdd) return Result.failure(IllegalStateException("add failed"))
        val current = _cart.value
        val lines = current?.lines.orEmpty().toMutableList()
        val index = lines.indexOfFirst { it.variantId == variantId }
        if (index >= 0) {
            val line = lines[index]
            val nextQty = line.quantity + quantity
            lines[index] = line.copy(
                quantity = nextQty,
                totalPrice = Money((line.unitPrice?.amount ?: 10_000.0) * nextQty, "KRW"),
            )
        } else {
            lines += sampleLine(variantId, quantity)
        }
        val cart = buildCart(current?.id ?: "checkout-1", lines)
        _cart.value = cart
        return Result.success(cart)
    }

    override suspend fun updateLineQuantity(lineId: String, quantity: Int): Result<Cart> {
        if (shouldFailUpdate) return Result.failure(IllegalStateException("update failed"))
        val current = _cart.value ?: return Result.failure(IllegalStateException("empty"))
        val lines = current.lines.map { line ->
            if (line.id != lineId) line
            else line.copy(
                quantity = quantity,
                totalPrice = Money((line.unitPrice?.amount ?: 10_000.0) * quantity, "KRW"),
            )
        }
        val cart = buildCart(current.id, lines)
        _cart.value = cart
        return Result.success(cart)
    }

    override suspend fun removeLine(lineId: String): Result<Cart> {
        val current = _cart.value ?: return Result.failure(IllegalStateException("empty"))
        val lines = current.lines.filterNot { it.id == lineId }
        if (lines.isEmpty()) {
            _cart.value = null
            return Result.success(
                Cart(id = current.id, lines = emptyList(), subtotal = null, shipping = null, total = null, quantity = 0),
            )
        }
        val cart = buildCart(current.id, lines)
        _cart.value = cart
        return Result.success(cart)
    }

    override suspend fun replace(cart: Cart?) {
        _cart.value = cart
    }

    override suspend fun clearLocal() {
        clearCount += 1
        _cart.value = null
    }

    override suspend fun adoptLoggedInCart() {
        adoptLoggedInCount += 1
    }

    override suspend fun releaseOnLogout() {
        releaseOnLogoutCount += 1
        restoreParkedLines()
        parkedLines = emptyList()
        clearLocal()
    }

    override suspend fun parkUnselectedLines(selectedLineIds: Set<String>): Result<Cart> {
        val current = _cart.value ?: return Result.failure(IllegalStateException("empty"))
        val remaining = current.lines.filter { it.id in selectedLineIds }
        if (remaining.isEmpty()) return Result.failure(IllegalStateException("상품을 선택해 주세요"))
        lastParkedSelectedIds = selectedLineIds
        val parked = current.lines.filter { it.id !in selectedLineIds }
        parkedLines = parked.map { it.variantId to it.quantity }
        parked.forEach { line -> removeLine(line.id) }
        return Result.success(
            _cart.value ?: Cart(
                id = current.id,
                lines = emptyList(),
                subtotal = null,
                shipping = null,
                total = null,
                quantity = 0,
            ),
        )
    }

    override suspend fun restoreParkedLines(): Result<Set<String>> {
        val toRestore = parkedLines
        parkedLines = emptyList()
        val restored = mutableSetOf<String>()
        toRestore.forEach { (variantId, quantity) ->
            val before = _cart.value?.lines.orEmpty().map { it.id }.toSet()
            addLine(variantId, quantity)
            _cart.value?.lines.orEmpty()
                .map { it.id }
                .filterNot { it in before }
                .forEach { restored += it }
        }
        return Result.success(restored)
    }

    companion object {
        fun sampleLine(variantId: String = "v1", quantity: Int = 1) = CartLine(
            id = "line-$variantId",
            variantId = variantId,
            productName = "Tea",
            variantName = "Small",
            thumbnailUrl = null,
            quantity = quantity,
            unitPrice = Money(10_000.0, "KRW"),
            totalPrice = Money(10_000.0 * quantity, "KRW"),
        )

        fun sampleCart(lines: List<CartLine> = listOf(sampleLine())) = buildCart("checkout-1", lines)

        fun buildCart(id: String, lines: List<CartLine>): Cart {
            val quantity = lines.sumOf { it.quantity }
            val subtotal = Money(lines.sumOf { it.totalPrice?.amount ?: 0.0 }, "KRW")
            return Cart(
                id = id,
                lines = lines,
                subtotal = subtotal,
                shipping = Money(0.0, "KRW"),
                total = subtotal,
                quantity = quantity,
            )
        }
    }
}
