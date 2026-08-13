package com.bdf.saleor.ui.account

import com.bdf.saleor.data.FakeAccountRepository
import com.bdf.saleor.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PointsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun refresh_success_exposesBalanceAndHistory() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = PointsViewModel(FakeAccountRepository())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1000.0, state.balance?.amount)
        assertEquals(1, state.entries.size)
        assertEquals("GRANTED", state.entries.first().type)
    }

    @Test
    fun refresh_failure_setsError() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeAccountRepository().apply { shouldFailPoints = true }
        val viewModel = PointsViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.error?.contains("points failed") == true)
    }
}
