package com.bdf.saleor.feature.catalog.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.core.designsystem.components.CartIconButton
import com.bdf.saleor.core.designsystem.components.ErrorState
import com.bdf.saleor.core.designsystem.components.LoadingState
import com.bdf.saleor.core.model.EditorJsBlock
import com.bdf.saleor.core.model.Money
import com.bdf.saleor.core.model.ProductCmsBlock
import com.bdf.saleor.core.model.ProductDetail
import com.bdf.saleor.core.model.ProductSummary
import com.bdf.saleor.core.model.ProductVariant
import com.bdf.saleor.core.ui.CmsContent
import com.bdf.saleor.core.ui.EditorJsContent
import com.bdf.saleor.feature.catalog.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val GalleryFadeRangePx = 240f

@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    onBack: () -> Unit,
    onCartClick: () -> Unit,
    onBuyNow: () -> Unit,
    cartQuantity: Int,
    onRelatedProductClick: (ProductSummary) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val addedMessage = stringResource(R.string.added_to_cart)
    val viewCartLabel = stringResource(R.string.view_cart)
    val quantityMaxTemplate = stringResource(R.string.quantity_max_snackbar)
    val listState = rememberLazyListState()
    val appBarAlpha by remember {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset.toFloat()
            when {
                index > 0 -> 1f
                else -> (offset / GalleryFadeRangePx).coerceIn(0f, 1f)
            }
        }
    }

    LaunchedEffect(state.addToCartMessage) {
        val message = state.addToCartMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = if (message == "added") addedMessage else message,
            actionLabel = if (message == "added") viewCartLabel else null,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) onCartClick()
        viewModel.consumeAddToCartMessage()
    }

    LaunchedEffect(state.quantityLimitMessage) {
        val max = state.quantityLimitMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = quantityMaxTemplate.format(max.toIntOrNull() ?: 0),
            duration = SnackbarDuration.Short,
        )
        viewModel.consumeQuantityLimitMessage()
    }

    LaunchedEffect(state.buyNowReady) {
        if (!state.buyNowReady) return@LaunchedEffect
        onBuyNow()
        viewModel.consumeBuyNow()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("product_detail_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("product_detail_snackbar"),
            )
        },
        topBar = {
            ProductDetailTopBar(
                title = state.product?.name.orEmpty(),
                alpha = appBarAlpha,
                cartQuantity = cartQuantity,
                isFavorited = state.isFavorited,
                togglingFavorite = state.togglingFavorite,
                showFavorite = state.product != null,
                onBack = onBack,
                onCartClick = onCartClick,
                onToggleFavorite = viewModel::toggleFavorite,
            )
        },
        bottomBar = {
            if (state.product != null) {
                ProductDetailActionBar(
                    addingToCart = state.addingToCart,
                    isOutOfStock = state.isOutOfStock,
                    isFavorited = state.isFavorited,
                    togglingFavorite = state.togglingFavorite,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onOpenOptionSheet = viewModel::openOptionSheet,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> LoadingState()
                state.error == "not_found" -> ErrorState(
                    message = stringResource(R.string.product_unavailable),
                    onRetry = onBack,
                )
                state.error != null -> ErrorState(
                    message = state.error ?: stringResource(DesignR.string.error_generic),
                    onRetry = viewModel::refresh,
                )
                state.product != null -> {
                    ProductDetailContent(
                        listState = listState,
                        product = state.product!!,
                        displayPrice = state.displayPrice,
                        undiscountedPrice = state.displayUndiscountedPrice,
                        discountPercent = state.discountPercent,
                        displayMedia = state.displayMedia,
                        descriptionBlocks = state.descriptionBlocks,
                        descriptionText = state.descriptionText,
                        availableTabs = state.availableTabs,
                        isOutOfStock = state.isOutOfStock,
                        isLowStock = state.isLowStock,
                        lowStockCount = state.selectedVariant?.quantityAvailable,
                        relatedProducts = state.relatedProducts,
                        onRelatedProductClick = onRelatedProductClick,
                    )
                }
            }
        }
    }

    if (state.optionSheetOpen && state.product != null) {
        ProductOptionPurchaseSheet(
            product = state.product!!,
            sheetState = state.optionSheet,
            addingToCart = state.addingToCart,
            onDismiss = viewModel::dismissOptionSheet,
            onTogglePicker = viewModel::toggleOptionPicker,
            onSelectOption = viewModel::selectSheetOption,
            onRemoveLine = viewModel::removeSheetLine,
            onIncrementLine = viewModel::incrementSheetLine,
            onDecrementLine = viewModel::decrementSheetLine,
            onClearHighlight = viewModel::clearSheetHighlight,
            onAddToCart = viewModel::addToCart,
            onBuyNow = viewModel::buyNow,
        )
    }
}

@Composable
private fun ProductDetailTopBar(
    title: String,
    alpha: Float,
    cartQuantity: Int,
    isFavorited: Boolean,
    togglingFavorite: Boolean,
    showFavorite: Boolean,
    onBack: () -> Unit,
    onCartClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val scrim = Color.White.copy(alpha = 0.88f)
    val iconTint = lerp(colors.onSurface, colors.onSurface, alpha)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow.copy(alpha = alpha))
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(56.dp)
            .testTag("product_detail_top_bar"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScrimIconButton(
                onClick = onBack,
                alpha = alpha,
                scrim = scrim,
                testTag = "back",
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(DesignR.string.back),
                    tint = iconTint,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.onSurface.copy(alpha = alpha),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .testTag("product_detail_appbar_title"),
            )
            if (showFavorite && alpha < 0.5f) {
                ScrimIconButton(
                    onClick = onToggleFavorite,
                    enabled = !togglingFavorite,
                    alpha = alpha,
                    scrim = scrim,
                    testTag = "favorite_toggle_appbar",
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(
                            if (isFavorited) R.string.favorite_remove else R.string.favorite_add,
                        ),
                        tint = if (isFavorited) colors.error else iconTint,
                    )
                }
            }
            Box {
                if (alpha < 0.5f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(scrim.copy(alpha = scrim.alpha * (1f - alpha))),
                    )
                }
                CartIconButton(
                    cartQuantity = cartQuantity,
                    onCartClick = onCartClick,
                    badgeBorderColor = if (alpha < 0.5f) scrim else colors.surfaceContainerLow,
                    testTag = "product_detail_cart",
                    badgeTestTag = "product_detail_cart_badge",
                )
            }
        }
    }
}

@Composable
private fun ScrimIconButton(
    onClick: () -> Unit,
    alpha: Float,
    scrim: Color,
    testTag: String,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(48.dp),
    ) {
        if (alpha < 0.5f) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(scrim.copy(alpha = scrim.alpha * (1f - alpha))),
            )
        }
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.testTag(testTag),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductDetailContent(
    listState: LazyListState,
    product: ProductDetail,
    displayPrice: Money?,
    undiscountedPrice: Money?,
    discountPercent: Int?,
    displayMedia: List<String>,
    descriptionBlocks: List<EditorJsBlock>,
    descriptionText: String,
    availableTabs: List<ProductDetailSection>,
    isOutOfStock: Boolean,
    isLowStock: Boolean,
    lowStockCount: Int?,
    relatedProducts: List<ProductSummary>,
    onRelatedProductClick: (ProductSummary) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedSection by remember(availableTabs) {
        mutableStateOf(availableTabs.firstOrNull() ?: ProductDetailSection.Summary)
    }
    var isScrollingToSection by remember { mutableStateOf(false) }
    var stickyTabHeightPx by remember { mutableIntStateOf(0) }
    val emptySectionMessage = stringResource(R.string.section_empty)
    val priceUnavailable = stringResource(DesignR.string.price_unavailable)

    val contentStartIndex = remember(availableTabs) {
        if (availableTabs.isEmpty()) 2 else 3
    }

    LaunchedEffect(listState, isScrollingToSection, availableTabs, contentStartIndex) {
        if (isScrollingToSection || availableTabs.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val sectionIndex = (index - contentStartIndex).coerceAtLeast(0)
                availableTabs.getOrNull(sectionIndex)?.let { selectedSection = it }
            }
    }

    val scrollToSection: (ProductDetailSection) -> Unit = scroll@{ section ->
        val offsetInTabs = availableTabs.indexOf(section)
        if (offsetInTabs < 0) return@scroll
        coroutineScope.launch {
            isScrollingToSection = true
            selectedSection = section
            listState.animateScrollToItem(
                index = contentStartIndex + offsetInTabs,
                scrollOffset = -stickyTabHeightPx,
            )
            isScrollingToSection = false
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "gallery") {
            MediaGallery(urls = displayMedia)
        }
        item(key = "buybox") {
            ProductDetailBuyBox(
                productName = product.name,
                displayPrice = displayPrice?.format() ?: priceUnavailable,
                undiscountedPrice = undiscountedPrice?.format(),
                discountPercent = discountPercent,
                isOutOfStock = isOutOfStock,
                isLowStock = isLowStock,
                lowStockCount = lowStockCount,
            )
        }
        if (availableTabs.size > 1) {
            stickyHeader(key = "section_tabs") {
                ProductDetailSectionTabs(
                    sections = availableTabs,
                    selected = selectedSection,
                    onSelect = scrollToSection,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        stickyTabHeightPx = coordinates.size.height
                    },
                )
            }
        } else if (availableTabs.size == 1) {
            item(key = "section_title") {
                ProductDetailSectionTitle(section = availableTabs.first())
            }
        }
        availableTabs.forEach { section ->
            item(key = "section_${section.name}") {
                when (section) {
                    ProductDetailSection.Summary -> ProductDetailSummarySection(
                        descriptionBlocks = descriptionBlocks,
                        descriptionText = descriptionText,
                        emptyMessage = emptySectionMessage,
                    )
                    ProductDetailSection.Detail -> ProductDetailCmsSection(
                        cmsBlocks = product.cmsBlocks,
                        emptyMessage = emptySectionMessage,
                    )
                }
            }
        }
        item(key = "shipping_returns") {
            ShippingReturnsSection()
        }
        if (relatedProducts.isNotEmpty()) {
            item(key = "related") {
                RelatedProductsSection(
                    products = relatedProducts,
                    onProductClick = onRelatedProductClick,
                )
            }
        }
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProductDetailBuyBox(
    productName: String,
    displayPrice: String,
    undiscountedPrice: String?,
    discountPercent: Int?,
    isOutOfStock: Boolean,
    isLowStock: Boolean,
    lowStockCount: Int?,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = productName,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag("product_detail_name"),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (discountPercent != null) {
                Text(
                    text = stringResource(R.string.discount_percent, discountPercent),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.error,
                    modifier = Modifier
                        .background(Color(0xFFFBE9E7), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("discount_percent"),
                )
            }
            Text(
                text = displayPrice,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("product_detail_price"),
            )
            if (undiscountedPrice != null) {
                Text(
                    text = undiscountedPrice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA9A5A0),
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.testTag("product_detail_price_undiscounted"),
                )
            }
        }
        if (isLowStock && lowStockCount != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.low_stock, lowStockCount),
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                modifier = Modifier.testTag("low_stock"),
            )
        }
        if (isOutOfStock) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.out_of_stock),
                style = MaterialTheme.typography.labelLarge,
                color = colors.error,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        ShippingInfoBlock()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductOptionPurchaseSheet(
    product: ProductDetail,
    sheetState: OptionSheetState,
    addingToCart: Boolean,
    onDismiss: () -> Unit,
    onTogglePicker: () -> Unit,
    onSelectOption: (String) -> Unit,
    onRemoveLine: (String) -> Unit,
    onIncrementLine: (String) -> Unit,
    onDecrementLine: (String) -> Unit,
    onClearHighlight: () -> Unit,
    onAddToCart: () -> Unit,
    onBuyNow: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val modalState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.86f).dp
    val selectedScroll = rememberScrollState()

    LaunchedEffect(sheetState.highlightedVariantId) {
        if (sheetState.highlightedVariantId == null) return@LaunchedEffect
        delay(450)
        onClearHighlight()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalState,
        containerColor = colors.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("option_purchase_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            OptionPicker(
                expanded = sheetState.pickerExpanded,
                hasSelection = sheetState.selected.isNotEmpty(),
                variants = product.variants,
                onToggle = onTogglePicker,
                onSelect = onSelectOption,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = maxHeight * 0.45f),
            ) {
                if (sheetState.selected.isEmpty()) {
                    OptionSheetEmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(selectedScroll)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        sheetState.selected.forEach { line ->
                            SelectedOptionCard(
                                line = line,
                                highlighted = line.variantId == sheetState.highlightedVariantId,
                                onRemove = { onRemoveLine(line.variantId) },
                                onIncrement = { onIncrementLine(line.variantId) },
                                onDecrement = { onDecrementLine(line.variantId) },
                            )
                        }
                    }
                }
            }
            SheetTotalAndCta(
                totalCount = sheetState.totalCount,
                totalPrice = sheetState.totalPrice,
                canSubmit = sheetState.canSubmit && !addingToCart,
                onAddToCart = onAddToCart,
                onBuyNow = onBuyNow,
            )
        }
    }
}

@Composable
private fun OptionPicker(
    expanded: Boolean,
    hasSelection: Boolean,
    variants: List<ProductVariant>,
    onToggle: () -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = if (expanded) {
        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(8.dp)
    }
    val borderWidth = if (expanded) 1.5.dp else 1.dp
    val borderColor = if (expanded) colors.primary else colors.outline
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(shape)
                .border(borderWidth, borderColor, shape)
                .background(colors.surfaceContainerLowest)
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp)
                .testTag("option_picker_trigger"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(
                    if (hasSelection) R.string.select_option_more else R.string.select_option_prompt,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = colors.primary,
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 0.dp,
                            bottomStart = 8.dp,
                            bottomEnd = 8.dp,
                        ),
                    )
                    .background(colors.surfaceContainerLowest)
                    .testTag("option_picker_list"),
            ) {
                variants.forEachIndexed { index, variant ->
                    OptionPickerRow(
                        variant = variant,
                        onClick = { onSelect(variant.id) },
                    )
                    if (index < variants.lastIndex) {
                        HorizontalDivider(color = colors.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionPickerRow(
    variant: ProductVariant,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val soldOut = variant.quantityAvailable == 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (soldOut) 0.42f else 1f)
            .clickable(enabled = !soldOut, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("option_picker_item_${variant.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = variantDisplayName(variant),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
            )
            if (soldOut) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.out_of_stock),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.error,
                    modifier = Modifier
                        .background(Color(0xFFFBE9E7), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            text = variant.price?.format().orEmpty(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.onSurface,
        )
    }
}

@Composable
private fun SelectedOptionCard(
    line: SelectedLine,
    highlighted: Boolean,
    onRemove: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val borderColor = if (highlighted) colors.primary else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .background(colors.surfaceContainer)
            .padding(13.dp)
            .testTag("selected_option_${line.variantId}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = line.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("remove_option_${line.variantId}"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.remove_option),
                    modifier = Modifier.size(26.dp),
                    tint = colors.onSurfaceVariant,
                )
            }
        }
        if (!line.subLabel.isNullOrBlank()) {
            Text(
                text = line.subLabel,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CompactQuantityStepper(
                quantity = line.quantity,
                canDecrement = line.canDecrement,
                canIncrement = line.canIncrement,
                onDecrement = onDecrement,
                onIncrement = onIncrement,
                testTagPrefix = line.variantId,
            )
            Text(
                text = line.lineTotal.format(),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.testTag("selected_line_total_${line.variantId}"),
            )
        }
    }
}

@Composable
private fun OptionSheetEmptyState(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .padding(start = 26.dp, top = 10.dp, end = 26.dp, bottom = 20.dp)
            .testTag("option_sheet_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.ShoppingBag,
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            tint = Color(0xFFA9A5A0),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.option_sheet_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun SheetTotalAndCta(
    totalCount: Int,
    totalPrice: Money?,
    canSubmit: Boolean,
    onAddToCart: () -> Unit,
    onBuyNow: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow),
    ) {
        HorizontalDivider(color = colors.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp)
                .testTag("sheet_total_row"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sheet_total_count, totalCount),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = totalPrice?.format() ?: stringResource(R.string.zero_price),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (totalCount > 0) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.testTag("sheet_total_price"),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onAddToCart,
                enabled = canSubmit,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("sheet_add_to_cart"),
                shape = CircleShape,
            ) {
                Text(
                    text = stringResource(R.string.add_to_cart),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Button(
                onClick = onBuyNow,
                enabled = canSubmit,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("sheet_buy_now"),
                shape = CircleShape,
            ) {
                Text(
                    text = stringResource(R.string.buy_now),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ShippingInfoBlock() {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceContainerLow)
            .padding(12.dp)
            .testTag("shipping_info_block"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Filled.LocalShipping,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = colors.onSurface,
        )
        Text(
            text = stringResource(
                R.string.shipping_lead_time,
                SHIPPING_LEAD_MIN_DAYS,
                SHIPPING_LEAD_MAX_DAYS,
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
        )
    }
}

@Composable
private fun ShippingReturnsSection() {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .testTag("shipping_returns_section"),
    ) {
        HorizontalDivider(color = colors.outlineVariant)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.shipping_returns_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PolicyRow(
            label = stringResource(R.string.shipping_fee_label),
            value = stringResource(R.string.shipping_fee_value),
        )
        PolicyRow(
            label = stringResource(R.string.shipping_carrier_label),
            value = stringResource(R.string.shipping_carrier_value),
        )
        PolicyRow(
            label = stringResource(R.string.return_policy_label),
            value = stringResource(R.string.return_policy_value),
            note = stringResource(R.string.return_policy_note),
        )
    }
}

@Composable
private fun PolicyRow(
    label: String,
    value: String,
    note: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
            )
            if (note != null) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RelatedProductsSection(
    products: List<ProductSummary>,
    onProductClick: (ProductSummary) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("related_products_section"),
    ) {
        Text(
            text = stringResource(R.string.related_products_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(products, key = { it.id }) { product ->
                RelatedProductCard(
                    product = product,
                    onClick = { onProductClick(product) },
                )
            }
        }
    }
}

@Composable
private fun RelatedProductCard(
    product: ProductSummary,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
            .testTag("related_product_${product.slug}"),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceVariant),
        ) {
            AsyncImage(
                model = product.thumbnailUrl,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = product.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = colors.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = product.price?.format() ?: stringResource(DesignR.string.price_unavailable),
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurface,
        )
    }
}

@Composable
private fun ProductDetailSummarySection(
    descriptionBlocks: List<EditorJsBlock>,
    descriptionText: String,
    emptyMessage: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .testTag("product_detail_section_summary"),
    ) {
        when {
            descriptionBlocks.isNotEmpty() -> {
                EditorJsContent(
                    blocks = descriptionBlocks,
                    modifier = Modifier.testTag("product_detail_description"),
                )
            }
            descriptionText.isNotBlank() -> {
                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("product_detail_description"),
                )
            }
            else -> {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProductDetailCmsSection(
    cmsBlocks: List<ProductCmsBlock>,
    emptyMessage: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .testTag("product_detail_section_detail"),
    ) {
        if (cmsBlocks.isNotEmpty()) {
            CmsContent(blocks = cmsBlocks)
        } else {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProductDetailActionBar(
    addingToCart: Boolean,
    isOutOfStock: Boolean,
    isFavorited: Boolean,
    togglingFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenOptionSheet: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val favoriteLabel = stringResource(
        if (isFavorited) R.string.favorite_remove else R.string.favorite_add,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLowest)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("product_detail_bottom_bar"),
    ) {
        HorizontalDivider(thickness = 1.dp, color = colors.outlineVariant)
        if (isOutOfStock) {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .height(50.dp)
                    .testTag("out_of_stock_button"),
                shape = CircleShape,
            ) {
                Text(
                    text = stringResource(R.string.out_of_stock),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FavoriteBorderButton(
                    isFavorited = isFavorited,
                    enabled = !togglingFavorite,
                    label = favoriteLabel,
                    onClick = onToggleFavorite,
                )
                Button(
                    onClick = onOpenOptionSheet,
                    enabled = !addingToCart,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("buy_with_options"),
                    shape = CircleShape,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.buy_with_options),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteBorderButton(
    isFavorited: Boolean,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(width = 48.dp, height = 50.dp)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp))
            .testTag("favorite_toggle"),
    ) {
        Icon(
            imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = label,
            tint = if (isFavorited) colors.error else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompactQuantityStepper(
    quantity: Int,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    testTagPrefix: String,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .height(34.dp)
            .border(1.dp, colors.outlineVariant, CircleShape)
            .background(colors.surfaceContainerLowest, CircleShape)
            .padding(horizontal = 2.dp)
            .testTag("quantity_stepper_$testTagPrefix"),
    ) {
        IconButton(
            onClick = onDecrement,
            enabled = canDecrement,
            modifier = Modifier
                .size(30.dp)
                .testTag("quantity_decrement_$testTagPrefix"),
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = stringResource(R.string.quantity_decrease),
                tint = if (canDecrement) colors.onSurface else colors.onSurfaceVariant,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(36.dp),
        ) {
            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("quantity_value_$testTagPrefix"),
            )
        }
        IconButton(
            onClick = onIncrement,
            enabled = true,
            modifier = Modifier
                .size(30.dp)
                .testTag("quantity_increment_$testTagPrefix"),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.quantity_increase),
                tint = if (canIncrement) colors.onSurface else colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MediaGallery(urls: List<String>) {
    if (urls.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.no_image), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val pagerState = rememberPagerState(pageCount = { urls.size })
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            AsyncImage(
                model = urls[page],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (urls.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.32f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(urls.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .then(
                                if (selected) Modifier.size(width = 14.dp, height = 5.dp)
                                else Modifier.size(5.dp),
                            )
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) Color.White else Color.White.copy(alpha = 0.55f)),
                    )
                }
            }
        }
    }
}
