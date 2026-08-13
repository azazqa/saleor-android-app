package com.bdf.saleor.ui.detail

import com.bdf.saleor.data.FakeCartRepository
import com.bdf.saleor.data.FakeCatalogRepository
import com.bdf.saleor.testing.MainDispatcherRule
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
class ProductDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun refresh_success_selectsFirstVariant() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ProductDetailViewModel(
            repository = FakeCatalogRepository(),
            cartRepository = FakeCartRepository(),
            slug = "tea",
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Tea", state.product?.name)
        assertEquals("v1", state.selectedVariantId)
        assertEquals(10.0, state.displayPrice?.amount ?: -1.0, 0.0)
        assertTrue(state.descriptionText.contains("Nice tea"))
    }

    @Test
    fun selectVariant_updatesPrice() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ProductDetailViewModel(
            repository = FakeCatalogRepository(),
            cartRepository = FakeCartRepository(),
            slug = "tea",
        )
        advanceUntilIdle()

        viewModel.selectVariant("v2")

        val state = viewModel.uiState.value
        assertEquals("v2", state.selectedVariantId)
        assertEquals(15.0, state.displayPrice?.amount ?: -1.0, 0.0)
    }

    @Test
    fun refresh_notFound_setsNotFoundError() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCatalogRepository().apply { productDetail = null }
        val viewModel = ProductDetailViewModel(
            repository = repository,
            cartRepository = FakeCartRepository(),
            slug = "missing",
        )

        advanceUntilIdle()

        assertEquals("not_found", viewModel.uiState.value.error)
    }

    @Test
    fun addToCart_usesSelectedVariant() = runTest(mainDispatcherRule.dispatcher) {
        val cart = FakeCartRepository()
        val viewModel = ProductDetailViewModel(
            repository = FakeCatalogRepository(),
            cartRepository = cart,
            slug = "tea",
        )
        advanceUntilIdle()

        viewModel.addToCart()
        advanceUntilIdle()

        assertEquals("v1", cart.lastAddedVariantId)
        assertEquals("added", viewModel.uiState.value.addToCartMessage)
        assertEquals(1, cart.cart.value?.quantity)
    }
}
