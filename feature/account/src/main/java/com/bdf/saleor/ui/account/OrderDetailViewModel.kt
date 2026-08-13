package com.bdf.saleor.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.data.OrderRepository
import com.bdf.saleor.data.model.OrderDetail
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val order: OrderDetail? = null,
)

@HiltViewModel(assistedFactory = OrderDetailViewModel.Factory::class)
class OrderDetailViewModel @AssistedInject constructor(
    private val repository: OrderRepository,
    @Assisted private val orderId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.getOrderDetail(orderId) }
                .onSuccess { order ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            order = order,
                            error = if (order == null) "not_found" else null,
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

    @AssistedFactory
    interface Factory {
        fun create(orderId: String): OrderDetailViewModel
    }
}
