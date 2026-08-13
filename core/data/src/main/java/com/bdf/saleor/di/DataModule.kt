package com.bdf.saleor.di

import com.bdf.saleor.core.network.AccessTokenProvider
import com.bdf.saleor.data.AccountRepository
import com.bdf.saleor.data.AuthRepository
import com.bdf.saleor.data.CartRepository
import com.bdf.saleor.data.CatalogRepository
import com.bdf.saleor.data.CheckoutRepository
import com.bdf.saleor.data.DefaultAccessTokenProvider
import com.bdf.saleor.data.DefaultAccountRepository
import com.bdf.saleor.data.DefaultAuthRepository
import com.bdf.saleor.data.DefaultCartRepository
import com.bdf.saleor.data.DefaultCatalogRepository
import com.bdf.saleor.data.DefaultCheckoutRepository
import com.bdf.saleor.data.DefaultOrderRepository
import com.bdf.saleor.data.OrderRepository
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
    abstract fun bindAccessTokenProvider(impl: DefaultAccessTokenProvider): AccessTokenProvider
}
