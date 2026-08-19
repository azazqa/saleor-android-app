package com.bdf.saleor.feature.account

import com.bdf.saleor.core.testing.fake.FakeOrderRepository
import com.bdf.saleor.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun refresh_success_exposesOrders() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = OrderListViewModel(FakeOrderRepository())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.orders.size)
        assertEquals("12", state.orders.first().number)
    }

    @Test
    fun refresh_failure_setsError() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeOrderRepository().apply { shouldFailList = true }
        val viewModel = OrderListViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.error?.contains("orders failed") == true)
    }
}
