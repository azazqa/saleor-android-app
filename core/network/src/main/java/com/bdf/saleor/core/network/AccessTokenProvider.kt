package com.bdf.saleor.core.network

interface AccessTokenProvider {
    suspend fun validAccessToken(): String?
    suspend fun invalidate()
}

interface ApolloCache {
    suspend fun clear()
}
