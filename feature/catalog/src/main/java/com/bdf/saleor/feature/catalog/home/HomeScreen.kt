package com.bdf.saleor.feature.catalog.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
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
import com.bdf.saleor.core.model.CategoryItem
import com.bdf.saleor.core.model.ProductSummary
import com.bdf.saleor.feature.catalog.R
import com.bdf.saleor.core.designsystem.components.EmptyState
import com.bdf.saleor.core.designsystem.components.ErrorState
import com.bdf.saleor.core.designsystem.components.LoadingState
import com.bdf.saleor.core.ui.ProductGrid
import com.bdf.saleor.core.designsystem.theme.AppSpacing

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
            contentPadding = PaddingValues(AppSpacing.ScreenHorizontal),
            header = {
                Column {
                    HeroBanner()
                    Spacer(modifier = Modifier.height(AppSpacing.Section))
                    if (state.categories.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.home_categories),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.InSection))
                        CategoryChips(
                            categories = state.categories,
                            onCategoryClick = onCategoryClick,
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.Section))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.featuredTitle ?: stringResource(R.string.home_featured),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        TextButton(
                            onClick = onViewAllFeatured,
                            modifier = Modifier.testTag("home_view_all"),
                        ) {
                            Text(
                                text = stringResource(R.string.home_view_all),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
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
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primary)
            .padding(AppSpacing.Section),
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_hero_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.height(AppSpacing.CardGap))
            Text(
                text = stringResource(R.string.home_hero_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
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
            AssistChip(
                onClick = { onCategoryClick(category) },
                label = { Text(category.name) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("category_chip_${category.slug}"),
            )
        }
    }
}
