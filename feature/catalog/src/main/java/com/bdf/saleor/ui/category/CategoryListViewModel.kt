package com.bdf.saleor.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.data.CatalogRepository
import com.bdf.saleor.data.model.CategoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryListUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val categories: List<CategoryItem> = emptyList(),
)

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val repository: CatalogRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoryListUiState())
    val uiState: StateFlow<CategoryListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.getCategories() }
                .onSuccess { categories ->
                    _uiState.update { it.copy(isLoading = false, categories = categories) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Unknown error")
                    }
                }
        }
    }
}
