package com.bdf.saleor.feature.catalog.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.core.data.CatalogRepository
import com.bdf.saleor.core.model.CategoryItem
import com.bdf.saleor.core.model.ProductSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val featuredTitle: String? = null,
    val featuredProducts: List<ProductSummary> = emptyList(),
    val categories: List<CategoryItem> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CatalogRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.getHomeCatalog() }
                .onSuccess { catalog ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            featuredTitle = catalog.featuredTitle,
                            featuredProducts = catalog.featuredProducts,
                            categories = catalog.categories,
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
}
