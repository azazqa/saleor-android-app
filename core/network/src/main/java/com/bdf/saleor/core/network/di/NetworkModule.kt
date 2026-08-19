package com.bdf.saleor.core.network.di

import android.content.Context
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import com.bdf.saleor.core.network.AccessTokenProvider
import com.bdf.saleor.core.network.ApolloCache
import com.bdf.saleor.core.network.AuthorizationInterceptor
import com.bdf.saleor.core.network.BuildConfig
import com.bdf.saleor.core.network.SaleorCatalogConfig
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OkHttpModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object CatalogConfigModule {
    @Provides
    @Singleton
    fun provideSaleorCatalogConfig(): SaleorCatalogConfig {
        return SaleorCatalogConfig(
            apiUrl = BuildConfig.SALEOR_API_URL,
            channel = BuildConfig.SALEOR_CHANNEL,
            locale = BuildConfig.SALEOR_LOCALE,
            checkoutCountry = BuildConfig.SALEOR_CHECKOUT_COUNTRY,
            featuredCollectionSlug = BuildConfig.FEATURED_COLLECTION_SLUG,
            storefrontUrl = BuildConfig.SALEOR_STOREFRONT_URL,
            cmsUrl = BuildConfig.SALEOR_CMS_URL,
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    @Named("bare")
    fun provideBareApolloClient(config: SaleorCatalogConfig): ApolloClient {
        return ApolloClient.Builder()
            .serverUrl(config.apiUrl)
            .build()
    }

    @Provides
    @Singleton
    fun provideApolloClient(
        @ApplicationContext context: Context,
        config: SaleorCatalogConfig,
        tokenProvider: Lazy<AccessTokenProvider>,
    ): ApolloClient {
        val sqlCache = SqlNormalizedCacheFactory(context, "saleor_apollo.db")
        val memoryThenSql = MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024).chain(sqlCache)
        return ApolloClient.Builder()
            .serverUrl(config.apiUrl)
            .addHttpInterceptor(AuthorizationInterceptor(tokenProvider))
            .normalizedCache(memoryThenSql)
            .build()
    }

    @Provides
    @Singleton
    fun provideApolloCache(apolloClient: ApolloClient): ApolloCache {
        return object : ApolloCache {
            override suspend fun clear() {
                apolloClient.apolloStore.clearAll()
            }
        }
    }
}
