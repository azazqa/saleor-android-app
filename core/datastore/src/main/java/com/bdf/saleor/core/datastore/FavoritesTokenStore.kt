package com.bdf.saleor.core.datastore

interface FavoritesTokenStore {
    suspend fun token(): String?
    suspend fun setToken(token: String)
    suspend fun clear()
}
