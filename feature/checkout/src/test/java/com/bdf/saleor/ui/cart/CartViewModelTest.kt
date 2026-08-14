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
    fun decrement_atOne_doesNotRemoveLine() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCartRepository()
        repository.replace(FakeCartRepository.sampleCart())
        val viewModel = CartViewModel(repository)
        advanceUntilIdle()

        viewModel.decrement("line-v1")
        advanceUntilIdle()

        assertEquals(1, viewModel.cart.value?.quantity)
        assertEquals(1, viewModel.cart.value?.lines?.size)
    }

    @Test
    fun decrement_fromTwo_decreasesQuantity() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCartRepository()
        repository.replace(FakeCartRepository.sampleCart(listOf(FakeCartRepository.sampleLine(quantity = 2))))
        val viewModel = CartViewModel(repository)
        advanceUntilIdle()

        viewModel.decrement("line-v1")
        advanceUntilIdle()

        assertEquals(1, viewModel.cart.value?.quantity)
    }

    @Test
    fun clearAll_removesEveryLine() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCartRepository()
        repository.replace(
            FakeCartRepository.sampleCart(
                listOf(
                    FakeCartRepository.sampleLine("v1"),
                    FakeCartRepository.sampleLine("v2"),
                ),
            ),
        )
        val viewModel = CartViewModel(repository)
        advanceUntilIdle()

        viewModel.clearAll()
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

    @Test
    fun selection_defaultsToAllLines() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCartRepository()
        repository.replace(
            FakeCartRepository.sampleCart(
                listOf(
                    FakeCartRepository.sampleLine("v1"),
                    FakeCartRepository.sampleLine("v2"),
                ),
            ),
        )
        val viewModel = CartViewModel(repository)
        advanceUntilIdle()

        assertEquals(setOf("line-v1", "line-v2"), viewModel.uiState.value.selectedLineIds)
        assertEquals(20_000.0, viewModel.cart.value?.selectedSubtotal(viewModel.uiState.value.selectedLineIds)?.amount)
    }

    @Test
    fun toggleLine_updatesSelectedTotals() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCartRepository()
        repository.replace(
            FakeCartRepository.sampleCart(
                listOf(
                    FakeCartRepository.sampleLine("v1"),
                    FakeCartRepository.sampleLine("v2"),
                ),
            ),
        )
        val viewModel = CartViewModel(repository)
        advanceUntilIdle()

        viewModel.toggleLine("line-v2")

        assertEquals(setOf("line-v1"), viewModel.uiState.value.selectedLineIds)
        assertEquals(10_000.0, viewModel.cart.value?.selectedSubtotal(viewModel.uiState.value.selectedLineIds)?.amount)
        assertEquals(1, viewModel.cart.value?.selectedQuantity(viewModel.uiState.value.selectedLineIds))
    }

    @Test
    fun toggleSelectAll_clearsThenRestoresSelection() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCartRepository()
        repository.replace(
            FakeCartRepository.sampleCart(
                listOf(
                    FakeCartRepository.sampleLine("v1"),
                    FakeCartRepository.sampleLine("v2"),
                ),
            ),
        )
        val viewModel = CartViewModel(repository)
        advanceUntilIdle()

        viewModel.toggleSelectAll()
        assertTrue(viewModel.uiState.value.selectedLineIds.isEmpty())

        viewModel.toggleSelectAll()
        assertEquals(setOf("line-v1", "line-v2"), viewModel.uiState.value.selectedLineIds)
    }

    @Test
    fun prepareCheckout_parksUnselectedLines() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCartRepository()
        repository.replace(
            FakeCartRepository.sampleCart(
                listOf(
                    FakeCartRepository.sampleLine("v1"),
                    FakeCartRepository.sampleLine("v2"),
                ),
            ),
        )
        val viewModel = CartViewModel(repository)
        advanceUntilIdle()
        viewModel.toggleLine("line-v2")

        var ready: Boolean? = null
        viewModel.prepareCheckout { ready = it }
        advanceUntilIdle()

        assertEquals(true, ready)
        assertEquals(setOf("line-v1"), repository.lastParkedSelectedIds)
        assertEquals(listOf("v2" to 1), repository.parkedLines)
        assertEquals(listOf("line-v1"), viewModel.cart.value?.lines?.map { it.id })
    }

    @Test
    fun prepareCheckout_withoutSelection_fails() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCartRepository()
        repository.replace(
            FakeCartRepository.sampleCart(
                listOf(
                    FakeCartRepository.sampleLine("v1"),
                    FakeCartRepository.sampleLine("v2"),
                ),
            ),
        )
        val viewModel = CartViewModel(repository)
        advanceUntilIdle()
        viewModel.toggleSelectAll()

        var ready: Boolean? = null
        viewModel.prepareCheckout { ready = it }
        advanceUntilIdle()

        assertEquals(false, ready)
        assertTrue(viewModel.uiState.value.error?.contains("선택") == true)
        assertEquals(2, viewModel.cart.value?.lines?.size)
    }

    @Test
    fun onVisible_restoresParkedLinesUnselected() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCartRepository()
        repository.replace(
            FakeCartRepository.sampleCart(
                listOf(
                    FakeCartRepository.sampleLine("v1"),
                    FakeCartRepository.sampleLine("v2"),
                ),
            ),
        )
        val viewModel = CartViewModel(repository)
        advanceUntilIdle()
        viewModel.toggleLine("line-v2")
        viewModel.prepareCheckout { }
        advanceUntilIdle()

        viewModel.onVisible()
        advanceUntilIdle()

        assertEquals(2, viewModel.cart.value?.lines?.size)
        assertEquals(setOf("line-v1"), viewModel.uiState.value.selectedLineIds)
        assertTrue(repository.parkedLines.isEmpty())
    }
}
