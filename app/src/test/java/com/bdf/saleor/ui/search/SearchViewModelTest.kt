package com.bdf.saleor.ui.search

import com.bdf.saleor.data.FakeCatalogRepository
import com.bdf.saleor.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun blankQuery_resetsToIdleState() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = SearchViewModel(FakeCatalogRepository())

        viewModel.onQueryChange("tea")
        advanceTimeBy(350)
        advanceUntilIdle()
        viewModel.onQueryChange("")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.query)
        assertFalse(state.hasSearched)
        assertTrue(state.products.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun debounce_thenLoadsResults() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCatalogRepository()
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("cof")
        advanceTimeBy(349)
        assertFalse(viewModel.uiState.value.hasSearched)

        advanceTimeBy(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasSearched)
        assertFalse(state.isLoading)
        assertEquals("cof", repository.searchQueryReceived)
        assertEquals(1, state.products.size)
        assertEquals("Coffee", state.products.first().name)
    }

    @Test
    fun searchFailure_setsError() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCatalogRepository().apply { shouldFailSearch = true }
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("x")
        advanceTimeBy(350)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("search failed") == true)
    }
}
