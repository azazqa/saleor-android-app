package com.bdf.saleor.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.data.CatalogRepository
import com.bdf.saleor.data.model.ProductPage
import com.bdf.saleor.data.model.ProductSummary
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductListUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val title: String = "",
    val products: List<ProductSummary> = emptyList(),
    val endCursor: String? = null,
    val hasNextPage: Boolean = false,
)

@HiltViewModel(assistedFactory = ProductListViewModel.Factory::class)
class ProductListViewModel @AssistedInject constructor(
    private val repository: CatalogRepository,
    @Assisted private val args: ProductListArgs,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ProductListUiState(title = args.title.ifBlank { "Products" }),
    )
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { fetchPage(after = null) }
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            products = page.items,
                            endCursor = page.endCursor,
                            hasNextPage = page.hasNextPage,
                            title = page.title ?: args.title.ifBlank { it.title },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Unknown error")
                    }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasNextPage) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            runCatching { fetchPage(after = state.endCursor) }
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            products = it.products + page.items,
                            endCursor = page.endCursor,
                            hasNextPage = page.hasNextPage,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    private suspend fun fetchPage(after: String?): ProductPage {
        return when (args.source) {
            ProductListSource.CATEGORY -> repository.getProductsByCategory(args.slug, after = after)
            ProductListSource.COLLECTION -> repository.getProductsByCollection(args.slug, after = after)
            else -> repository.getProducts(after = after)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(args: ProductListArgs): ProductListViewModel
    }
}
