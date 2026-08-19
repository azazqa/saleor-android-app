package com.bdf.saleor.core.data

import com.apollographql.apollo.ApolloClient
import com.bdf.saleor.core.datastore.TokenStore
import com.bdf.saleor.core.network.AccessTokenProvider
import com.bdf.saleor.graphql.TokenRefreshMutation
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class DefaultAccessTokenProvider @Inject constructor(
    @Named("bare") private val bareClient: ApolloClient,
    private val tokenStore: TokenStore,
) : AccessTokenProvider {
    private val mutex = Mutex()

    override suspend fun validAccessToken(): String? {
        val current = tokenStore.accessToken()
        if (!current.isNullOrBlank() && !Jwt.isExpired(current)) return current
        return mutex.withLock {
            val latest = tokenStore.accessToken()
            if (!latest.isNullOrBlank() && !Jwt.isExpired(latest)) return@withLock latest
            refreshLocked()
        }
    }

    override suspend fun invalidate() {
        tokenStore.setAccessToken(null)
    }

    private suspend fun refreshLocked(): String? {
        val refresh = tokenStore.refreshToken() ?: return null
        val data = runCatching {
            bareClient.mutation(TokenRefreshMutation(refresh)).execute().data
        }.getOrNull()
        val token = data?.tokenRefresh?.token
        val errors = data?.tokenRefresh?.errors.orEmpty()
        if (token.isNullOrBlank() || errors.isNotEmpty()) {
            tokenStore.clear()
            return null
        }
        tokenStore.setAccessToken(token)
        return token
    }
}
