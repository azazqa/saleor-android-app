package com.bdf.saleor.di

import com.bdf.saleor.core.network.AccessTokenProvider
import com.bdf.saleor.data.AuthRepository
import com.bdf.saleor.data.CatalogRepository
import com.bdf.saleor.data.FakeAuthRepository
import com.bdf.saleor.data.FakeCatalogRepository
import com.bdf.saleor.data.FakeOrderRepository
import com.bdf.saleor.data.OrderRepository
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
    fun provideAuthRepository(): AuthRepository = FakeAuthRepository()

    @Provides
    @Singleton
    fun provideOrderRepository(): OrderRepository = FakeOrderRepository()

    @Provides
    @Singleton
    fun provideAccessTokenProvider(): AccessTokenProvider = object : AccessTokenProvider {
        override suspend fun validAccessToken(): String? = null
        override suspend fun invalidate() = Unit
    }
}
