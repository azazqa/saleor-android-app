package com.bdf.saleor.feature.catalog.home

import com.bdf.saleor.core.testing.fake.FakeCatalogRepository
import com.bdf.saleor.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun refresh_success_exposesFeaturedAndCategories() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCatalogRepository()
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Featured", state.featuredTitle)
        assertEquals(6, state.featuredProducts.size)
        assertEquals("Tea", state.featuredProducts.first().name)
        assertEquals(1, state.categories.size)
    }

    @Test
    fun refresh_failure_setsError() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCatalogRepository().apply { shouldFailHome = true }
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("home failed") == true)
        assertTrue(state.featuredProducts.isEmpty())
    }
}
