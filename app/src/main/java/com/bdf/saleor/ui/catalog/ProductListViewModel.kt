package com.bdf.saleor.ui.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.data.CatalogRepository
import com.bdf.saleor.data.model.ProductPage
import com.bdf.saleor.data.model.ProductSummary
import com.bdf.saleor.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
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

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val repository: CatalogRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val source: String =
        savedStateHandle.get<String>("source") ?: Routes.Source.ALL
    private val slug: String =
        savedStateHandle.get<String>("slug").orEmpty()
    private val initialTitle: String =
        decode(savedStateHandle.get<String>("title").orEmpty())

    private val _uiState = MutableStateFlow(ProductListUiState(title = initialTitle.ifBlank { "Products" }))
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
                            title = page.title ?: initialTitle.ifBlank { it.title },
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
        return when (source) {
            Routes.Source.CATEGORY -> repository.getProductsByCategory(slug, after = after)
            Routes.Source.COLLECTION -> repository.getProductsByCollection(slug, after = after)
            else -> repository.getProducts(after = after)
        }
    }

    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.toString()) }.getOrDefault(value)
}
