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
import com.bdf.saleor.core.model.ProductSummary
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
import kotlin.math.roundToInt

private const val LOW_STOCK_THRESHOLD = 5
private const val RELATED_PRODUCTS_LIMIT = 12
internal const val SHIPPING_LEAD_MIN_DAYS = 2
internal const val SHIPPING_LEAD_MAX_DAYS = 5

data class SelectedLine(
    val variantId: String,
    val displayName: String,
    val subLabel: String? = null,
    val unitPrice: Money,
    val quantity: Int,
    val stockLimit: Int?,
) {
    val lineTotal: Money
        get() = Money(unitPrice.amount * quantity, unitPrice.currency)

    val canDecrement: Boolean
        get() = quantity > 1

    val canIncrement: Boolean
        get() = stockLimit == null || quantity < stockLimit
}

data class OptionSheetState(
    val selected: List<SelectedLine> = emptyList(),
    val pickerExpanded: Boolean = false,
    val highlightedVariantId: String? = null,
) {
    val totalCount: Int
        get() = selected.sumOf { it.quantity }

    val totalPrice: Money?
        get() {
            if (selected.isEmpty()) return null
            val currency = selected.first().unitPrice.currency
            val amount = selected.sumOf { it.unitPrice.amount * it.quantity }
            return Money(amount, currency)
        }

    val canSubmit: Boolean
        get() = selected.isNotEmpty()
}

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
    val quantityLimitMessage: String? = null,
    val buyNowReady: Boolean = false,
    val isFavorited: Boolean = false,
    val togglingFavorite: Boolean = false,
    val relatedProducts: List<ProductSummary> = emptyList(),
    val optionSheetOpen: Boolean = false,
    val optionSheet: OptionSheetState = OptionSheetState(),
) {
    val usesOptionSheet: Boolean
        get() = true

    val selectedVariant: ProductVariant?
        get() {
            val variants = product?.variants.orEmpty()
            if (variants.isEmpty()) return null
            return variants.firstOrNull { it.id == selectedVariantId }
                ?: variants.singleOrNull()
        }

    val displayPrice: Money?
        get() = selectedVariant?.price
            ?: product?.priceRangeStart

    val displayUndiscountedPrice: Money?
        get() {
            val undiscounted = selectedVariant?.priceUndiscounted ?: return null
            val sale = displayPrice ?: return null
            return undiscounted.takeIf { it.amount > sale.amount }
        }

    val discountPercent: Int?
        get() {
            val undiscounted = displayUndiscountedPrice ?: return null
            val sale = displayPrice ?: return null
            if (undiscounted.amount <= 0.0) return null
            val percent = ((undiscounted.amount - sale.amount) / undiscounted.amount * 100.0)
                .roundToInt()
            return percent.takeIf { it > 0 }
        }

    val displayMedia: List<String>
        get() {
            val variantMedia = selectedVariant?.mediaUrls.orEmpty()
            return variantMedia.ifEmpty { product?.mediaUrls.orEmpty() }
        }

    val maxQuantity: Int?
        get() = selectedVariant?.quantityAvailable?.takeIf { it > 0 }

    /** True when every purchasable variant is sold out (or the only variant is sold out). */
    val isOutOfStock: Boolean
        get() {
            val variants = product?.variants.orEmpty()
            if (variants.isEmpty()) return true
            return variants.all { it.quantityAvailable == 0 }
        }

    val isLowStock: Boolean
        get() {
            val variants = product?.variants.orEmpty()
            if (variants.size != 1) return false
            val available = variants.first().quantityAvailable ?: return false
            return available in 1..LOW_STOCK_THRESHOLD
        }

    val lineTotal: Money?
        get() = optionSheet.totalPrice

    val availableTabs: List<ProductDetailSection>
        get() = buildList {
            if (descriptionBlocks.isNotEmpty() || descriptionText.isNotBlank()) {
                add(ProductDetailSection.Summary)
            }
            if (!product?.cmsBlocks.isNullOrEmpty()) {
                add(ProductDetailSection.Detail)
            }
        }
}

internal fun variantDisplayName(variant: ProductVariant): String =
    variant.options.joinToString(" / ") { it.valueName }.ifBlank { variant.name }

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
                        val autoSelectId = when {
                            product.variants.size == 1 -> product.variants.first().id
                            else -> null
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                product = product,
                                selectedVariantId = autoSelectId,
                                quantity = 1,
                                descriptionText = parseEditorJsDescription(product.descriptionJson),
                                descriptionBlocks = parseEditorJsBlocks(product.descriptionJson),
                                isFavorited = isFavorited,
                                optionSheetOpen = false,
                                optionSheet = OptionSheetState(),
                            )
                        }
                        loadRelatedProducts(product)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Unknown error")
                    }
                }
        }
    }

    private suspend fun loadRelatedProducts(product: ProductDetail) {
        val categorySlug = product.categorySlug ?: run {
            _uiState.update { it.copy(relatedProducts = emptyList()) }
            return
        }
        runCatching {
            repository.getProductsByCategory(slug = categorySlug, first = RELATED_PRODUCTS_LIMIT)
        }.onSuccess { page ->
            val related = page.items.filter { it.id != product.id }.take(RELATED_PRODUCTS_LIMIT)
            _uiState.update { it.copy(relatedProducts = related) }
        }.onFailure {
            _uiState.update { it.copy(relatedProducts = emptyList()) }
        }
    }

    fun openOptionSheet() {
        val state = _uiState.value
        if (state.isOutOfStock) return
        val purchasable = state.product?.variants.orEmpty()
            .filter { it.quantityAvailable != 0 && it.price != null }
        val initialSelected = if (purchasable.size == 1) {
            val variant = purchasable.first()
            listOf(
                SelectedLine(
                    variantId = variant.id,
                    displayName = variantDisplayName(variant),
                    unitPrice = variant.price!!,
                    quantity = 1,
                    stockLimit = variant.quantityAvailable?.takeIf { it > 0 },
                ),
            )
        } else {
            emptyList()
        }
        _uiState.update {
            it.copy(
                optionSheetOpen = true,
                optionSheet = OptionSheetState(
                    selected = initialSelected,
                    pickerExpanded = initialSelected.isEmpty() && purchasable.isNotEmpty(),
                ),
            )
        }
    }

    fun dismissOptionSheet() {
        _uiState.update {
            it.copy(
                optionSheetOpen = false,
                optionSheet = OptionSheetState(),
            )
        }
    }

    fun toggleOptionPicker() {
        _uiState.update { state ->
            if (!state.optionSheetOpen) return@update state
            state.copy(
                optionSheet = state.optionSheet.copy(
                    pickerExpanded = !state.optionSheet.pickerExpanded,
                ),
            )
        }
    }

    fun selectSheetOption(variantId: String) {
        val state = _uiState.value
        val variant = state.product?.variants?.firstOrNull { it.id == variantId } ?: return
        if (variant.quantityAvailable == 0) return
        val unitPrice = variant.price ?: return
        val stockLimit = variant.quantityAvailable?.takeIf { it > 0 }
        _uiState.update { current ->
            val sheet = current.optionSheet
            val existing = sheet.selected.indexOfFirst { it.variantId == variantId }
            val nextSelected = if (existing >= 0) {
                val line = sheet.selected[existing]
                if (!line.canIncrement) {
                    return@update current.copy(
                        quantityLimitMessage = (line.stockLimit ?: line.quantity).toString(),
                        optionSheet = sheet.copy(
                            pickerExpanded = false,
                            highlightedVariantId = variantId,
                        ),
                    )
                }
                sheet.selected.toMutableList().also { list ->
                    list[existing] = line.copy(quantity = line.quantity + 1)
                }
            } else {
                sheet.selected + SelectedLine(
                    variantId = variant.id,
                    displayName = variantDisplayName(variant),
                    unitPrice = unitPrice,
                    quantity = 1,
                    stockLimit = stockLimit,
                )
            }
            current.copy(
                optionSheet = sheet.copy(
                    selected = nextSelected,
                    pickerExpanded = false,
                    highlightedVariantId = variantId,
                ),
            )
        }
    }

    fun removeSheetLine(variantId: String) {
        _uiState.update { state ->
            val next = state.optionSheet.selected.filterNot { it.variantId == variantId }
            state.copy(
                optionSheet = state.optionSheet.copy(
                    selected = next,
                    highlightedVariantId = null,
                ),
            )
        }
    }

    fun incrementSheetLine(variantId: String) {
        _uiState.update { state ->
            val sheet = state.optionSheet
            val index = sheet.selected.indexOfFirst { it.variantId == variantId }
            if (index < 0) return@update state
            val line = sheet.selected[index]
            if (!line.canIncrement) {
                return@update state.copy(
                    quantityLimitMessage = (line.stockLimit ?: line.quantity).toString(),
                )
            }
            val next = sheet.selected.toMutableList().also {
                it[index] = line.copy(quantity = line.quantity + 1)
            }
            state.copy(optionSheet = sheet.copy(selected = next))
        }
    }

    fun decrementSheetLine(variantId: String) {
        _uiState.update { state ->
            val sheet = state.optionSheet
            val index = sheet.selected.indexOfFirst { it.variantId == variantId }
            if (index < 0) return@update state
            val line = sheet.selected[index]
            if (!line.canDecrement) return@update state
            val next = sheet.selected.toMutableList().also {
                it[index] = line.copy(quantity = line.quantity - 1)
            }
            state.copy(optionSheet = sheet.copy(selected = next))
        }
    }

    fun clearSheetHighlight() {
        _uiState.update { state ->
            state.copy(optionSheet = state.optionSheet.copy(highlightedVariantId = null))
        }
    }

    fun consumeQuantityLimitMessage() {
        _uiState.update { it.copy(quantityLimitMessage = null) }
    }

    fun addToCart() {
        submitPurchase(navigateToCart = false)
    }

    fun buyNow() {
        submitPurchase(navigateToCart = true)
    }

    private fun submitPurchase(navigateToCart: Boolean) {
        val state = uiState.value
        if (state.addingToCart || state.isOutOfStock) return
        addSelectedToCart(navigateToCart)
    }

    private fun addSelectedToCart(navigateToCart: Boolean) {
        val state = uiState.value
        val lines = state.optionSheet.selected
        if (lines.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(addingToCart = true, addToCartMessage = null, buyNowReady = false) }
            var failure: String? = null
            for (line in lines) {
                val quantity = parseCartQuantity(line.quantity, line.stockLimit)
                val result = cartRepository.addLine(line.variantId, quantity)
                if (result.isFailure) {
                    failure = result.exceptionOrNull()?.message
                    break
                }
            }
            if (failure != null) {
                _uiState.update { it.copy(addingToCart = false, addToCartMessage = failure) }
            } else {
                _uiState.update {
                    it.copy(
                        addingToCart = false,
                        addToCartMessage = if (navigateToCart) null else "added",
                        buyNowReady = navigateToCart,
                        optionSheetOpen = false,
                        optionSheet = OptionSheetState(),
                    )
                }
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
