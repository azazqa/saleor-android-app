package com.bdf.saleor.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.data.CartRepository
import com.bdf.saleor.data.model.Cart
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartUiState(
    val isUpdating: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
) : ViewModel() {
    val cart: StateFlow<Cart?> = cartRepository.cart.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        cartRepository.cart.value,
    )

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { cartRepository.refresh() }
    }

    fun increment(lineId: String) {
        val line = cart.value?.lines?.firstOrNull { it.id == lineId } ?: return
        updateQuantity(lineId, line.quantity + 1)
    }

    fun decrement(lineId: String) {
        val line = cart.value?.lines?.firstOrNull { it.id == lineId } ?: return
        if (line.quantity <= 1) {
            remove(lineId)
        } else {
            updateQuantity(lineId, line.quantity - 1)
        }
    }

    fun remove(lineId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null) }
            cartRepository.removeLine(lineId)
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
            _uiState.update { it.copy(isUpdating = false) }
        }
    }

    private fun updateQuantity(lineId: String, quantity: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null) }
            cartRepository.updateLineQuantity(lineId, quantity)
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
            _uiState.update { it.copy(isUpdating = false) }
        }
    }
}
