package com.bdf.saleor.ui.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.data.model.ProductSummary
import com.bdf.saleor.feature.catalog.R
import com.bdf.saleor.ui.components.BackTextLink
import com.bdf.saleor.ui.components.EmptyState
import com.bdf.saleor.ui.components.ErrorState
import com.bdf.saleor.ui.components.LoadingState
import com.bdf.saleor.ui.components.ProductGrid

@Composable
fun ProductListScreen(
    viewModel: ProductListViewModel,
    onProductClick: (ProductSummary) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("product_list_screen"),
    ) {
        BackTextLink(text = stringResource(R.string.back_link), onClick = onBack)
        Text(
            text = state.title.ifBlank { "Products" },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        when {
            state.isLoading -> LoadingState()
            state.error != null && state.products.isEmpty() -> ErrorState(
                message = state.error ?: stringResource(DesignR.string.error_generic),
                onRetry = viewModel::refresh,
            )
            state.products.isEmpty() -> EmptyState(message = stringResource(DesignR.string.products_empty))
            else -> ProductGrid(
                products = state.products,
                onProductClick = onProductClick,
                isLoadingMore = state.isLoadingMore,
                hasNextPage = state.hasNextPage,
                onLoadMore = viewModel::loadMore,
            )
        }
    }
}
