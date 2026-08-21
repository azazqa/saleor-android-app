package com.bdf.saleor.core.data.di

import com.bdf.saleor.core.network.AccessTokenProvider
import com.bdf.saleor.core.data.AccountRepository
import com.bdf.saleor.core.data.AuthRepository
import com.bdf.saleor.core.data.CartRepository
import com.bdf.saleor.core.data.CatalogRepository
import com.bdf.saleor.core.data.CheckoutRepository
import com.bdf.saleor.core.data.DefaultAccessTokenProvider
import com.bdf.saleor.core.data.DefaultAccountRepository
import com.bdf.saleor.core.data.DefaultAuthRepository
import com.bdf.saleor.core.data.DefaultCartRepository
import com.bdf.saleor.core.data.DefaultCatalogRepository
import com.bdf.saleor.core.data.DefaultCheckoutRepository
import com.bdf.saleor.core.data.DefaultFavoritesRepository
import com.bdf.saleor.core.data.DefaultOrderRepository
import com.bdf.saleor.core.data.FavoritesRepository
import com.bdf.saleor.core.data.OrderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindCatalogRepository(impl: DefaultCatalogRepository): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: DefaultAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: DefaultOrderRepository): OrderRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: DefaultAccountRepository): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCartRepository(impl: DefaultCartRepository): CartRepository

    @Binds
    @Singleton
    abstract fun bindCheckoutRepository(impl: DefaultCheckoutRepository): CheckoutRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: DefaultFavoritesRepository): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindAccessTokenProvider(impl: DefaultAccessTokenProvider): AccessTokenProvider
}
