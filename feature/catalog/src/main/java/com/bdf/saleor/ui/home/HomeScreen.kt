package com.bdf.saleor.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.data.model.CategoryItem
import com.bdf.saleor.data.model.ProductSummary
import com.bdf.saleor.feature.catalog.R
import com.bdf.saleor.ui.components.EmptyState
import com.bdf.saleor.ui.components.ErrorState
import com.bdf.saleor.ui.components.LoadingState
import com.bdf.saleor.ui.components.ProductGrid
import com.bdf.saleor.ui.theme.BrandNavy
import com.bdf.saleor.ui.theme.BrandOffWhite

@Composable
fun HomeScreen(
    onProductClick: (ProductSummary) -> Unit,
    onCategoryClick: (CategoryItem) -> Unit,
    onViewAllFeatured: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        state.isLoading && state.featuredProducts.isEmpty() -> LoadingState(modifier.testTag("home_screen"))
        state.error != null && state.featuredProducts.isEmpty() -> ErrorState(
            message = state.error ?: stringResource(DesignR.string.error_generic),
            onRetry = viewModel::refresh,
            modifier = modifier.testTag("home_screen"),
        )
        else -> ProductGrid(
            products = state.featuredProducts,
            onProductClick = onProductClick,
            modifier = modifier.testTag("home_screen"),
            contentPadding = PaddingValues(16.dp),
            header = {
                Column {
                    HeroBanner()
                    Spacer(modifier = Modifier.height(24.dp))
                    if (state.categories.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.home_categories),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CategoryChips(
                            categories = state.categories,
                            onCategoryClick = onCategoryClick,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.featuredTitle ?: stringResource(R.string.home_featured),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        TextButton(
                            onClick = onViewAllFeatured,
                            modifier = Modifier.testTag("home_view_all"),
                        ) {
                            Text(stringResource(R.string.home_view_all))
                        }
                    }
                    if (state.featuredProducts.isEmpty()) {
                        EmptyState(
                            message = stringResource(DesignR.string.products_empty),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun HeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandNavy)
            .padding(24.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_hero_title),
                style = MaterialTheme.typography.headlineLarge,
                color = BrandOffWhite,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_hero_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = BrandOffWhite.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<CategoryItem>,
    onCategoryClick: (CategoryItem) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.take(12).forEach { category ->
            Box(
                modifier = Modifier
                    .testTag("category_chip_${category.slug}")
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onCategoryClick(category) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
