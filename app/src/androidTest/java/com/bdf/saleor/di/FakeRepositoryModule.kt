package com.bdf.saleor.di

import com.bdf.saleor.core.network.AccessTokenProvider
import com.bdf.saleor.core.data.AccountRepository
import com.bdf.saleor.core.data.AuthRepository
import com.bdf.saleor.core.data.CartRepository
import com.bdf.saleor.core.data.CatalogRepository
import com.bdf.saleor.core.data.CheckoutRepository
import com.bdf.saleor.core.testing.fake.FakeAccountRepository
import com.bdf.saleor.core.testing.fake.FakeAuthRepository
import com.bdf.saleor.core.testing.fake.FakeCartRepository
import com.bdf.saleor.core.testing.fake.FakeCatalogRepository
import com.bdf.saleor.core.testing.fake.FakeCheckoutRepository
import com.bdf.saleor.core.testing.fake.FakeFavoritesRepository
import com.bdf.saleor.core.testing.fake.FakeOrderRepository
import com.bdf.saleor.core.data.FavoritesRepository
import com.bdf.saleor.core.data.OrderRepository
import com.bdf.saleor.core.data.di.DataModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataModule::class],
)
object FakeRepositoryModule {
    @Provides
    @Singleton
    fun provideCatalogRepository(): CatalogRepository = FakeCatalogRepository()

    @Provides
    @Singleton
    fun provideCartRepository(): CartRepository = FakeCartRepository()

    @Provides
    @Singleton
    fun provideAuthRepository(cartRepository: CartRepository): AuthRepository =
        FakeAuthRepository(cartRepository = cartRepository)

    @Provides
    @Singleton
    fun provideOrderRepository(): OrderRepository = FakeOrderRepository()

    @Provides
    @Singleton
    fun provideAccountRepository(): AccountRepository = FakeAccountRepository()

    @Provides
    @Singleton
    fun provideCheckoutRepository(): CheckoutRepository = FakeCheckoutRepository()

    @Provides
    @Singleton
    fun provideFavoritesRepository(): FavoritesRepository = FakeFavoritesRepository()

    @Provides
    @Singleton
    fun provideAccessTokenProvider(): AccessTokenProvider = object : AccessTokenProvider {
        override suspend fun validAccessToken(): String? = null
        override suspend fun invalidate() = Unit
    }
}
