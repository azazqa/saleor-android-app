package com.bdf.saleor.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.bdf.saleor.core.designsystem.R
import com.bdf.saleor.core.designsystem.components.InfiniteGridHandler
import com.bdf.saleor.core.designsystem.components.LocalTabReselectTick
import com.bdf.saleor.core.designsystem.theme.AppSpacing
import com.bdf.saleor.core.model.ProductSummary

@Composable
fun ProductCard(
    product: ProductSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.testTag("product_card_${product.slug}"),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(AppSpacing.CardContent)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    model = product.thumbnailUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.CardGap))
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = product.price?.format() ?: stringResource(R.string.price_unavailable),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun ProductGrid(
    products: List<ProductSummary>,
    onProductClick: (ProductSummary) -> Unit,
    modifier: Modifier = Modifier,
    isLoadingMore: Boolean = false,
    hasNextPage: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(AppSpacing.ScreenHorizontal),
    header: (@Composable () -> Unit)? = null,
) {
    val gridState = rememberLazyGridState()
    val reselectTick = LocalTabReselectTick.current
    LaunchedEffect(reselectTick) {
        if (reselectTick > 0) {
            gridState.animateScrollToItem(0)
        }
    }
    if (onLoadMore != null) {
        InfiniteGridHandler(
            listState = gridState,
            buffer = 4,
            onLoadMore = onLoadMore,
            enabled = hasNextPage && !isLoadingMore,
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = modifier
            .fillMaxSize()
            .testTag("product_grid"),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.CardGap),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.CardGap),
    ) {
        if (header != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                header()
            }
        }
        items(products, key = { it.id }) { product ->
            ProductCard(
                product = product,
                onClick = { onProductClick(product) },
            )
        }
        if (isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.ScreenHorizontal),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
