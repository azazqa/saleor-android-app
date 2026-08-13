package com.bdf.saleor.core.datastore

interface TokenStore {
    fun accessToken(): String?
    fun setAccessToken(token: String?)
    suspend fun refreshToken(): String?
    suspend fun setRefreshToken(token: String?)
    suspend fun clear()
}
