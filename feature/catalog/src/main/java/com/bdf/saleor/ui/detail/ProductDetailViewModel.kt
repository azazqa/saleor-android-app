package com.bdf.saleor.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.data.CartRepository
import com.bdf.saleor.data.CatalogRepository
import com.bdf.saleor.data.model.Money
import com.bdf.saleor.data.model.ProductDetail
import com.bdf.saleor.data.model.ProductVariant
import com.bdf.saleor.ui.util.parseEditorJsDescription
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val product: ProductDetail? = null,
    val selectedVariantId: String? = null,
    val descriptionText: String = "",
    val addingToCart: Boolean = false,
    val addToCartMessage: String? = null,
    val buyNowReady: Boolean = false,
) {
    val selectedVariant: ProductVariant?
        get() = product?.variants?.firstOrNull { it.id == selectedVariantId }
            ?: product?.variants?.firstOrNull()

    val displayPrice: Money?
        get() = selectedVariant?.price
            ?: product?.priceRangeStart

    val displayMedia: List<String>
        get() {
            val variantMedia = selectedVariant?.mediaUrls.orEmpty()
            return variantMedia.ifEmpty { product?.mediaUrls.orEmpty() }
        }
}

@HiltViewModel(assistedFactory = ProductDetailViewModel.Factory::class)
class ProductDetailViewModel @AssistedInject constructor(
    private val repository: CatalogRepository,
    private val cartRepository: CartRepository,
    @Assisted private val slug: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.getProductDetail(slug) }
                .onSuccess { product ->
                    if (product == null) {
                        _uiState.update {
                            it.copy(isLoading = false, error = "not_found", product = null)
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                product = product,
                                selectedVariantId = product.variants.firstOrNull()?.id,
                                descriptionText = parseEditorJsDescription(product.descriptionJson),
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Unknown error")
                    }
                }
        }
    }

    fun selectVariant(variantId: String) {
        _uiState.update { it.copy(selectedVariantId = variantId) }
    }

    fun addToCart() {
        addToCart(navigateToCheckout = false)
    }

    fun buyNow() {
        addToCart(navigateToCheckout = true)
    }

    private fun addToCart(navigateToCheckout: Boolean) {
        val variantId = uiState.value.selectedVariant?.id ?: return
        if (_uiState.value.addingToCart) return
        viewModelScope.launch {
            _uiState.update { it.copy(addingToCart = true, addToCartMessage = null, buyNowReady = false) }
            cartRepository.addLine(variantId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            addingToCart = false,
                            addToCartMessage = if (navigateToCheckout) null else "added",
                            buyNowReady = navigateToCheckout,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(addingToCart = false, addToCartMessage = error.message) }
                }
        }
    }

    fun consumeAddToCartMessage() {
        _uiState.update { it.copy(addToCartMessage = null) }
    }

    fun consumeBuyNow() {
        _uiState.update { it.copy(buyNowReady = false) }
    }

    @AssistedFactory
    interface Factory {
        fun create(slug: String): ProductDetailViewModel
    }
}
