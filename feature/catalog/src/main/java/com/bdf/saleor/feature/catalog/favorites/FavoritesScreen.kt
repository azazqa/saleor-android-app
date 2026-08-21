package com.bdf.saleor.feature.catalog.favorites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.core.designsystem.components.EmptyState
import com.bdf.saleor.core.designsystem.components.ErrorState
import com.bdf.saleor.core.designsystem.components.LoadingState
import com.bdf.saleor.core.designsystem.theme.AppSpacing
import com.bdf.saleor.core.model.ProductSummary
import com.bdf.saleor.core.ui.ProductGrid
import com.bdf.saleor.feature.catalog.R

@Composable
fun FavoritesScreen(
    onProductClick: (ProductSummary) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("favorites_screen"),
    ) {
        Text(
            text = stringResource(R.string.favorites_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                horizontal = AppSpacing.ScreenHorizontal,
                vertical = AppSpacing.InSection,
            ),
        )
        when {
            state.isLoading -> LoadingState()
            state.error != null && state.products.isEmpty() -> ErrorState(
                message = state.error ?: stringResource(DesignR.string.error_generic),
                onRetry = viewModel::refresh,
            )
            state.products.isEmpty() -> EmptyState(message = stringResource(R.string.favorites_empty))
            else -> ProductGrid(
                products = state.products,
                onProductClick = onProductClick,
            )
        }
    }
}
