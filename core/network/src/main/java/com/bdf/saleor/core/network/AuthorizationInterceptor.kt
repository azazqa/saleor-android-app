package com.bdf.saleor.core.network

import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import dagger.Lazy

class AuthorizationInterceptor(
    private val tokenProvider: Lazy<AccessTokenProvider>,
) : HttpInterceptor {
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain,
    ): HttpResponse {
        val token = tokenProvider.get().validAccessToken()
        val authorizedRequest = if (token.isNullOrBlank()) {
            request
        } else {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        val response = chain.proceed(authorizedRequest)
        if (response.statusCode == 401 || response.statusCode == 403) {
            tokenProvider.get().invalidate()
        }
        return response
    }
}
