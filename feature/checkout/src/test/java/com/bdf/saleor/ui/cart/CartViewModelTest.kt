package com.bdf.saleor.ui.cart

import com.bdf.saleor.data.FakeCartRepository
import com.bdf.saleor.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun increment_updatesQuantityAndTotal() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCartRepository()
        repository.replace(FakeCartRepository.sampleCart())
        val viewModel = CartViewModel(repository)
        advanceUntilIdle()

        viewModel.increment("line-v1")
        advanceUntilIdle()

        assertEquals(2, viewModel.cart.value?.quantity)
        assertEquals(20_000.0, viewModel.cart.value?.total?.amount ?: -1.0, 0.0)
    }

    @Test
    fun decrement_toZero_removesLineAndClearsCart() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCartRepository()
        repository.replace(FakeCartRepository.sampleCart())
        val viewModel = CartViewModel(repository)
        advanceUntilIdle()

        viewModel.decrement("line-v1")
        advanceUntilIdle()

        assertTrue(viewModel.cart.value == null || viewModel.cart.value?.lines.isNullOrEmpty())
    }

    @Test
    fun remove_lastLine_clearsCart() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCartRepository()
        repository.replace(FakeCartRepository.sampleCart())
        val viewModel = CartViewModel(repository)
        advanceUntilIdle()

        viewModel.remove("line-v1")
        advanceUntilIdle()

        assertNull(viewModel.cart.value)
    }
}
