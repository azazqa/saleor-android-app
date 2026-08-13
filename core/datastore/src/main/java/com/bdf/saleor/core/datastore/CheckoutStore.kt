package com.bdf.saleor.core.datastore

interface CheckoutStore {
    suspend fun checkoutId(channel: String): String?
    suspend fun setCheckoutId(channel: String, id: String?)
    suspend fun clear(channel: String)
}
