package com.bdf.saleor.core.datastore

data class HeldCartLine(
    val variantId: String,
    val quantity: Int,
)

interface CheckoutStore {
    suspend fun checkoutId(channel: String): String?
    suspend fun setCheckoutId(channel: String, id: String?)
    suspend fun clear(channel: String)
    suspend fun heldCartLines(channel: String): List<HeldCartLine>
    suspend fun setHeldCartLines(channel: String, lines: List<HeldCartLine>)
}
