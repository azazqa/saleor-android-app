package com.bdf.saleor.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.feature.catalog.R
import com.bdf.saleor.ui.components.ErrorState
import com.bdf.saleor.ui.components.LoadingState
import com.bdf.saleor.ui.components.LocalSnackbarHostState
import com.bdf.saleor.ui.components.ScreenTopBar

@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val addedMessage = stringResource(R.string.added_to_cart)

    LaunchedEffect(state.addToCartMessage) {
        val message = state.addToCartMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(if (message == "added") addedMessage else message)
        viewModel.consumeAddToCartMessage()
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
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = viewModel::addToCart,
                                enabled = !state.addingToCart && state.selectedVariant != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_to_cart"),
                            ) {
                                Text(stringResource(R.string.add_to_cart))
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
