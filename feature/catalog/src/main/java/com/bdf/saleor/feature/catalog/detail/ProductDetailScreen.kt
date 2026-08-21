package com.bdf.saleor.feature.catalog.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import com.bdf.saleor.core.model.Money
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.core.model.ProductCmsBlock
import com.bdf.saleor.core.model.ProductDetail
import com.bdf.saleor.feature.catalog.R
import com.bdf.saleor.core.designsystem.components.CartIconButton
import com.bdf.saleor.core.ui.CmsContent
import com.bdf.saleor.core.ui.EditorJsContent
import com.bdf.saleor.core.designsystem.components.ErrorState
import com.bdf.saleor.core.designsystem.components.LoadingState
import com.bdf.saleor.core.designsystem.components.LocalSnackbarHostState
import com.bdf.saleor.core.designsystem.components.ScreenTopBar
import com.bdf.saleor.core.model.EditorJsBlock
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    onBack: () -> Unit,
    onCartClick: () -> Unit,
    onBuyNow: () -> Unit,
    cartQuantity: Int,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val addedMessage = stringResource(R.string.added_to_cart)
    val viewCartLabel = stringResource(R.string.view_cart)

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
        topBar = {
            ScreenTopBar(
                title = state.product?.name.orEmpty(),
                onBack = onBack,
            ) {
                CartIconButton(
                    cartQuantity = cartQuantity,
                    onCartClick = onCartClick,
                    badgeBorderColor = MaterialTheme.colorScheme.background,
                    testTag = "product_detail_cart",
                    badgeTestTag = "product_detail_cart_badge",
                )
            }
        },
        bottomBar = {
            if (state.product != null) {
                ProductDetailBottomBar(
                    quantity = state.quantity,
                    lineTotal = state.lineTotal,
                    canDecrement = state.canDecrement,
                    canIncrement = state.canIncrement,
                    isOutOfStock = state.isOutOfStock,
                    addingToCart = state.addingToCart,
                    canSubmit = !state.addingToCart &&
                        state.selectedVariant != null &&
                        !state.isOutOfStock,
                    isFavorited = state.isFavorited,
                    togglingFavorite = state.togglingFavorite,
                    onDecrement = viewModel::decrementQuantity,
                    onIncrement = viewModel::incrementQuantity,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onAddToCart = viewModel::addToCart,
                    onBuyNow = viewModel::buyNow,
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        product = state.product!!,
                        displayPrice = state.displayPrice?.format()
                            ?: stringResource(DesignR.string.price_unavailable),
                        displayMedia = state.displayMedia,
                        descriptionBlocks = state.descriptionBlocks,
                        descriptionText = state.descriptionText,
                        selectedVariantId = state.selectedVariant?.id,
                        onSelectVariant = viewModel::selectVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: ProductDetail,
    displayPrice: String,
    displayMedia: List<String>,
    descriptionBlocks: List<EditorJsBlock>,
    descriptionText: String,
    selectedVariantId: String?,
    onSelectVariant: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedSection by remember { mutableStateOf(ProductDetailSection.Summary) }
    var isScrollingToSection by remember { mutableStateOf(false) }
    var stickyTabHeightPx by remember { mutableIntStateOf(0) }
    val emptySectionMessage = stringResource(R.string.section_empty)
    val qaMessage = stringResource(R.string.qa_coming_soon)

    LaunchedEffect(listState, isScrollingToSection) {
        if (isScrollingToSection) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                selectedSection = when {
                    index >= ProductDetailSectionIndices.QA -> ProductDetailSection.Qa
                    index >= ProductDetailSectionIndices.DETAIL -> ProductDetailSection.Detail
                    else -> ProductDetailSection.Summary
                }
            }
    }

    val scrollToSection: (ProductDetailSection) -> Unit = { section ->
        coroutineScope.launch {
            isScrollingToSection = true
            selectedSection = section
            listState.animateScrollToItem(
                index = section.toLazyItemIndex(),
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
                product = product,
                displayPrice = displayPrice,
                selectedVariantId = selectedVariantId,
                onSelectVariant = onSelectVariant,
            )
        }
        stickyHeader(key = "section_tabs") {
            ProductDetailSectionTabs(
                selected = selectedSection,
                onSelect = scrollToSection,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    stickyTabHeightPx = coordinates.size.height
                },
            )
        }
        item(key = "section_summary") {
            ProductDetailSummarySection(
                descriptionBlocks = descriptionBlocks,
                descriptionText = descriptionText,
                emptyMessage = emptySectionMessage,
            )
        }
        item(key = "section_detail") {
            ProductDetailCmsSection(
                cmsBlocks = product.cmsBlocks,
                emptyMessage = emptySectionMessage,
            )
        }
        item(key = "section_qa") {
            ProductDetailQaSection(message = qaMessage)
        }
    }
}

@Composable
private fun ProductDetailBuyBox(
    product: ProductDetail,
    displayPrice: String,
    selectedVariantId: String?,
    onSelectVariant: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = product.name,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag("product_detail_name"),
        )
        if (!product.categoryName.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = product.categoryName.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = displayPrice,
            style = MaterialTheme.typography.titleLarge,
        )
        if (product.variants.size > 1) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.select_variant),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(product.variants, key = { it.id }) { variant ->
                    val selected = variant.id == selectedVariantId
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectVariant(variant.id) },
                        label = {
                            Text(
                                variant.options.joinToString(" / ") { it.valueName }
                                    .ifBlank { variant.name },
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        }
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
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(24.dp))
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
private fun ProductDetailQaSection(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .testTag("product_detail_section_qa"),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ProductDetailBottomBar(
    quantity: Int,
    lineTotal: Money?,
    canDecrement: Boolean,
    canIncrement: Boolean,
    isOutOfStock: Boolean,
    addingToCart: Boolean,
    canSubmit: Boolean,
    isFavorited: Boolean,
    togglingFavorite: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToCart: () -> Unit,
    onBuyNow: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val favoriteLabel = stringResource(
        if (isFavorited) R.string.favorite_remove else R.string.favorite_add,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow)
            .testTag("product_detail_bottom_bar"),
    ) {
        HorizontalDivider(thickness = 1.dp, color = colors.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            QuantityStepper(
                quantity = quantity,
                canDecrement = canDecrement,
                canIncrement = canIncrement,
                enabled = !isOutOfStock && !addingToCart,
                onDecrement = onDecrement,
                onIncrement = onIncrement,
            )
            Column(horizontalAlignment = Alignment.End) {
                if (isOutOfStock) {
                    Text(
                        text = stringResource(R.string.out_of_stock),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.error,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.line_total_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = lineTotal?.format().orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        modifier = Modifier.testTag("line_total"),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onToggleFavorite,
                enabled = !togglingFavorite,
                modifier = Modifier
                    .height(44.dp)
                    .testTag("favorite_toggle"),
            ) {
                Icon(
                    imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = favoriteLabel,
                    tint = if (isFavorited) colors.error else colors.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onAddToCart,
                enabled = canSubmit,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("add_to_cart"),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.add_to_cart),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onBuyNow,
                enabled = canSubmit && !addingToCart,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("buy_now"),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.buy_now),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    canDecrement: Boolean,
    canIncrement: Boolean,
    enabled: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.medium)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("quantity_stepper"),
    ) {
        Text(
            text = stringResource(R.string.quantity_label),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp),
        )
        IconButton(
            onClick = onDecrement,
            enabled = enabled && canDecrement,
            modifier = Modifier.testTag("quantity_decrement"),
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = stringResource(R.string.quantity_decrease),
            )
        }
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .widthIn(min = 28.dp)
                .testTag("quantity_value"),
        )
        IconButton(
            onClick = onIncrement,
            enabled = enabled && canIncrement,
            modifier = Modifier.testTag("quantity_increment"),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.quantity_increase),
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
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) { page ->
        AsyncImage(
            model = urls[page],
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.large),
        )
    }
}
