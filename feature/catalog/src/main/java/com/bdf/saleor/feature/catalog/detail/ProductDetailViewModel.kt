package com.bdf.saleor.feature.catalog.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.core.data.CartRepository
import com.bdf.saleor.core.data.CatalogRepository
import com.bdf.saleor.core.data.FavoritesRepository
import com.bdf.saleor.core.data.parseEditorJsBlocks
import com.bdf.saleor.core.data.parseEditorJsDescription
import com.bdf.saleor.core.model.EditorJsBlock
import com.bdf.saleor.core.model.Money
import com.bdf.saleor.core.model.ProductDetail
import com.bdf.saleor.core.model.ProductVariant
import com.bdf.saleor.core.model.parseCartQuantity
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
    val quantity: Int = 1,
    val descriptionText: String = "",
    val descriptionBlocks: List<EditorJsBlock> = emptyList(),
    val addingToCart: Boolean = false,
    val addToCartMessage: String? = null,
    val buyNowReady: Boolean = false,
    val isFavorited: Boolean = false,
    val togglingFavorite: Boolean = false,
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

    val maxQuantity: Int?
        get() = selectedVariant?.quantityAvailable?.takeIf { it > 0 }

    val isOutOfStock: Boolean
        get() = selectedVariant?.quantityAvailable == 0

    val canDecrement: Boolean
        get() = !isOutOfStock && quantity > 1

    val canIncrement: Boolean
        get() = !isOutOfStock && (maxQuantity == null || quantity < maxQuantity!!)

    val lineTotal: Money?
        get() {
            val unit = displayPrice ?: return null
            return Money(unit.amount * quantity, unit.currency)
        }
}

@HiltViewModel(assistedFactory = ProductDetailViewModel.Factory::class)
class ProductDetailViewModel @AssistedInject constructor(
    private val repository: CatalogRepository,
    private val cartRepository: CartRepository,
    private val favoritesRepository: FavoritesRepository,
    @Assisted private val slug: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            favoritesRepository.favoriteIds.collect { ids ->
                val productId = _uiState.value.product?.id ?: return@collect
                _uiState.update { it.copy(isFavorited = productId in ids) }
            }
        }
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
                        val isFavorited = product.id in favoritesRepository.favoriteIds.value
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                product = product,
                                selectedVariantId = product.variants.firstOrNull()?.id,
                                quantity = 1,
                                descriptionText = parseEditorJsDescription(product.descriptionJson),
                                descriptionBlocks = parseEditorJsBlocks(product.descriptionJson),
                                isFavorited = isFavorited,
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
        _uiState.update { it.copy(selectedVariantId = variantId, quantity = 1) }
    }

    fun incrementQuantity() {
        _uiState.update { state ->
            if (!state.canIncrement) return@update state
            state.copy(quantity = state.quantity + 1)
        }
    }

    fun decrementQuantity() {
        _uiState.update { state ->
            if (!state.canDecrement) return@update state
            state.copy(quantity = state.quantity - 1)
        }
    }

    fun addToCart() {
        addToCart(navigateToCart = false)
    }

    fun buyNow() {
        addToCart(navigateToCart = true)
    }

    private fun addToCart(navigateToCart: Boolean) {
        val state = uiState.value
        val variant = state.selectedVariant ?: return
        if (state.addingToCart || state.isOutOfStock) return
        val quantity = parseCartQuantity(state.quantity, variant.quantityAvailable)
        viewModelScope.launch {
            _uiState.update { it.copy(addingToCart = true, addToCartMessage = null, buyNowReady = false) }
            cartRepository.addLine(variant.id, quantity)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            addingToCart = false,
                            addToCartMessage = if (navigateToCart) null else "added",
                            buyNowReady = navigateToCart,
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

    fun toggleFavorite() {
        val productId = _uiState.value.product?.id ?: return
        if (_uiState.value.togglingFavorite) return
        viewModelScope.launch {
            _uiState.update { it.copy(togglingFavorite = true) }
            favoritesRepository.toggle(productId)
                .onFailure { error ->
                    _uiState.update { it.copy(addToCartMessage = error.message) }
                }
            _uiState.update { it.copy(togglingFavorite = false) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(slug: String): ProductDetailViewModel
    }
}
