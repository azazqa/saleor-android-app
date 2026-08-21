package com.bdf.saleor.feature.catalog.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.core.data.FavoritesRepository
import com.bdf.saleor.core.model.ProductSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val products: List<ProductSummary> = emptyList(),
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            favoritesRepository.loadFavoriteProducts()
                .onSuccess { products ->
                    _uiState.update { it.copy(isLoading = false, products = products) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Unknown error")
                    }
                }
        }
    }

    fun toggleFavorite(productId: String) {
        viewModelScope.launch {
            favoritesRepository.toggle(productId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(products = state.products.filterNot { it.id == productId })
                    }
                }
        }
    }
}
