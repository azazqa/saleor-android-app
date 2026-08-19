package com.bdf.saleor.core.data

import com.bdf.saleor.core.model.Cart
import kotlinx.coroutines.flow.StateFlow

interface CartRepository {
    val cart: StateFlow<Cart?>

    suspend fun refresh()

    suspend fun addLine(variantId: String, quantity: Int = 1): Result<Cart>

    suspend fun updateLineQuantity(lineId: String, quantity: Int): Result<Cart>

    suspend fun removeLine(lineId: String): Result<Cart>

    suspend fun replace(cart: Cart?)

    suspend fun clearLocal()

    suspend fun adoptLoggedInCart()

    suspend fun releaseOnLogout()

    suspend fun parkUnselectedLines(selectedLineIds: Set<String>): Result<Cart>

    suspend fun restoreParkedLines(): Result<Set<String>>
}
