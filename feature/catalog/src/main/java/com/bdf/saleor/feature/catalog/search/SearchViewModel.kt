package com.bdf.saleor.feature.catalog.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.core.data.CatalogRepository
import com.bdf.saleor.core.model.ProductSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val products: List<ProductSummary> = emptyList(),
    val endCursor: String? = null,
    val hasNextPage: Boolean = false,
    val hasSearched: Boolean = false,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: CatalogRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    products = emptyList(),
                    hasSearched = false,
                    isLoading = false,
                    error = null,
                    hasNextPage = false,
                    endCursor = null,
                )
            }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            search(query = query, reset = true)
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasNextPage || state.query.isBlank()) return
        viewModelScope.launch {
            search(query = state.query, reset = false)
        }
    }

    private suspend fun search(query: String, reset: Boolean) {
        if (reset) {
            _uiState.update { it.copy(isLoading = true, error = null, hasSearched = true) }
        } else {
            _uiState.update { it.copy(isLoadingMore = true) }
        }
        val after = if (reset) null else _uiState.value.endCursor
        runCatching { repository.searchProducts(query = query, after = after) }
            .onSuccess { page ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        products = if (reset) page.items else it.products + page.items,
                        endCursor = page.endCursor,
                        hasNextPage = page.hasNextPage,
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = error.message ?: "Unknown error",
                    )
                }
            }
    }
}
