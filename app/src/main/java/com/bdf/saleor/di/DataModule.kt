package com.bdf.saleor.di

import com.bdf.saleor.BuildConfig
import com.bdf.saleor.data.CatalogRepository
import com.bdf.saleor.data.DefaultCatalogRepository
import com.bdf.saleor.data.SaleorCatalogConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindCatalogRepository(
        impl: DefaultCatalogRepository,
    ): CatalogRepository
}
