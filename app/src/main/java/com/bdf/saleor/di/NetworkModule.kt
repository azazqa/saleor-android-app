package com.bdf.saleor.di

import com.apollographql.apollo.ApolloClient
import com.bdf.saleor.data.SaleorCatalogConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideApolloClient(config: SaleorCatalogConfig): ApolloClient {
        return ApolloClient.Builder()
            .serverUrl(config.apiUrl)
            .build()
    }
}
