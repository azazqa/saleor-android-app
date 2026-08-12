package com.bdf.saleor.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.data.CatalogRepository
import com.bdf.saleor.data.model.Money
import com.bdf.saleor.data.model.ProductDetail
import com.bdf.saleor.data.model.ProductVariant
import com.bdf.saleor.ui.util.parseEditorJsDescription
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repository: CatalogRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val slug: String = checkNotNull(savedStateHandle["slug"]) {
        "Product slug is required"
    }

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
}
