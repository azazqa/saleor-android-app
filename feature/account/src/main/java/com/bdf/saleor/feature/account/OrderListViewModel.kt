package com.bdf.saleor.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.core.data.OrderRepository
import com.bdf.saleor.core.model.OrderSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderListUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val orders: List<OrderSummary> = emptyList(),
    val endCursor: String? = null,
    val hasNextPage: Boolean = false,
)

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val repository: OrderRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OrderListUiState())
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.getOrders(after = null) }
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            orders = page.items,
                            endCursor = page.endCursor,
                            hasNextPage = page.hasNextPage,
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
            runCatching { repository.getOrders(after = state.endCursor) }
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            orders = it.orders + page.items,
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
}
