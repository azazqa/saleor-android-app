package com.bdf.saleor.ui.detail

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.feature.catalog.R
import com.bdf.saleor.ui.components.CartIconButton
import com.bdf.saleor.ui.components.ErrorState
import com.bdf.saleor.ui.components.LoadingState
import com.bdf.saleor.ui.components.LocalSnackbarHostState
import com.bdf.saleor.ui.components.ScreenTopBar

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
            )
        },
        bottomBar = {
            if (state.product != null) {
                ProductDetailBottomBar(
                    cartQuantity = cartQuantity,
                    addingToCart = state.addingToCart,
                    canSubmit = !state.addingToCart && state.selectedVariant != null,
                    onCartClick = onCartClick,
                    onAddToCart = viewModel::addToCart,
                    onBuyNow = viewModel::buyNow,
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
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
                    val product = state.product!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        MediaGallery(urls = state.displayMedia)
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
                                text = state.displayPrice?.format()
                                    ?: stringResource(DesignR.string.price_unavailable),
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
                                        val selected = variant.id == state.selectedVariant?.id
                                        FilterChip(
                                            selected = selected,
                                            onClick = { viewModel.selectVariant(variant.id) },
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
                            if (state.descriptionText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = stringResource(R.string.description),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.descriptionText,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductDetailBottomBar(
    cartQuantity: Int,
    addingToCart: Boolean,
    canSubmit: Boolean,
    onCartClick: () -> Unit,
    onAddToCart: () -> Unit,
    onBuyNow: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
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
                .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CartIconButton(
                cartQuantity = cartQuantity,
                onCartClick = onCartClick,
                badgeBorderColor = colors.surfaceContainerLow,
                testTag = "product_detail_cart",
                badgeTestTag = "product_detail_cart_badge",
            )
            OutlinedButton(
                onClick = onAddToCart,
                enabled = canSubmit,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("add_to_cart"),
                shape = CircleShape,
            ) {
                Text(
                    text = stringResource(R.string.add_to_cart),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Button(
                onClick = onBuyNow,
                enabled = canSubmit && !addingToCart,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("buy_now"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.buy_now),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
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
            Text("No image", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
