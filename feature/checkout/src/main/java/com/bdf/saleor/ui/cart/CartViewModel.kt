package com.bdf.saleor.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.data.CartRepository
import com.bdf.saleor.data.model.Cart
import com.bdf.saleor.data.model.CartLine
import com.bdf.saleor.data.model.Money
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
    val selectedLineIds: Set<String> = emptySet(),
    val knownLineIds: Set<String> = emptySet(),
    val selectionReady: Boolean = false,
    val isRestoring: Boolean = false,
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
        viewModelScope.launch {
            cartRepository.refresh()
            restoreParkedSelection()
            _uiState.update { it.copy(selectionReady = true) }
        }
        viewModelScope.launch {
            cart.collect { current ->
                if (!_uiState.value.selectionReady) return@collect
                syncSelectionWithCart(current)
            }
        }
    }

    fun onVisible() {
        viewModelScope.launch { restoreParkedSelection() }
    }

    fun toggleLine(lineId: String) {
        _uiState.update { state ->
            val next = if (lineId in state.selectedLineIds) {
                state.selectedLineIds - lineId
            } else {
                state.selectedLineIds + lineId
            }
            state.copy(selectedLineIds = next, error = null)
        }
    }

    fun toggleSelectAll() {
        val ids = cart.value?.lines?.map { it.id }.orEmpty().toSet()
        _uiState.update { state ->
            val allSelected = ids.isNotEmpty() && state.selectedLineIds.containsAll(ids)
            state.copy(
                selectedLineIds = if (allSelected) emptySet() else ids,
                error = null,
            )
        }
    }

    fun increment(lineId: String) {
        val line = cart.value?.lines?.firstOrNull { it.id == lineId } ?: return
        updateQuantity(lineId, line.quantity + 1)
    }

    fun decrement(lineId: String) {
        val line = cart.value?.lines?.firstOrNull { it.id == lineId } ?: return
        if (line.quantity <= 1) return
        updateQuantity(lineId, line.quantity - 1)
    }

    fun remove(lineId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null) }
            cartRepository.removeLine(lineId)
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
            _uiState.update { it.copy(isUpdating = false) }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null) }
            val ids = cart.value?.lines?.map { it.id }.orEmpty()
            ids.forEach { id ->
                cartRepository.removeLine(id)
                    .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
            }
            _uiState.update { it.copy(isUpdating = false) }
        }
    }

    fun prepareCheckout(onReady: (Boolean) -> Unit) {
        val selected = _uiState.value.selectedLineIds
        if (selected.isEmpty()) {
            _uiState.update { it.copy(error = "상품을 선택해 주세요") }
            onReady(false)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null) }
            cartRepository.parkUnselectedLines(selected)
                .onSuccess { onReady(true) }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                    onReady(false)
                }
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

    private suspend fun restoreParkedSelection() {
        _uiState.update { it.copy(isRestoring = true) }
        val restored = cartRepository.restoreParkedLines().getOrDefault(emptySet())
        applyRestoredSelection(restored)
        _uiState.update { it.copy(isRestoring = false) }
    }

    private fun applyRestoredSelection(restoredLineIds: Set<String>) {
        val lines = cartRepository.cart.value?.lines.orEmpty()
        val ids = lines.map { it.id }.toSet()
        _uiState.update { state ->
            val kept = state.selectedLineIds.intersect(ids) - restoredLineIds
            val next = when {
                ids.isEmpty() -> emptySet()
                kept.isEmpty() && restoredLineIds.isNotEmpty() && restoredLineIds.containsAll(ids) -> ids
                kept.isEmpty() && state.selectedLineIds.isEmpty() -> ids
                else -> kept
            }
            state.copy(selectedLineIds = next, knownLineIds = ids)
        }
    }

    private fun syncSelectionWithCart(current: Cart?) {
        val ids = current?.lines?.map { it.id }.orEmpty().toSet()
        _uiState.update { state ->
            if (state.isRestoring) {
                return@update state.copy(
                    selectedLineIds = state.selectedLineIds.intersect(ids),
                    knownLineIds = ids,
                )
            }
            val added = ids - state.knownLineIds
            val next = if (state.knownLineIds.isEmpty() && state.selectedLineIds.isEmpty()) {
                ids
            } else {
                state.selectedLineIds.intersect(ids) + added
            }
            state.copy(selectedLineIds = next, knownLineIds = ids)
        }
    }
}

internal fun Cart.selectedLines(selectedLineIds: Set<String>): List<CartLine> =
    lines.filter { it.id in selectedLineIds }

internal fun Cart.selectedQuantity(selectedLineIds: Set<String>): Int =
    selectedLines(selectedLineIds).sumOf { it.quantity }

internal fun Cart.selectedSubtotal(selectedLineIds: Set<String>): Money? {
    val amount = selectedLines(selectedLineIds).sumOf { it.totalPrice?.amount ?: 0.0 }
    val currency = subtotal?.currency ?: total?.currency ?: "KRW"
    return Money(amount, currency)
}
