package com.bdf.saleor.ui.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.data.model.ProductSummary
import com.bdf.saleor.ui.components.EmptyState
import com.bdf.saleor.ui.components.ErrorState
import com.bdf.saleor.ui.components.LoadingState
import com.bdf.saleor.ui.components.ProductGrid
import com.bdf.saleor.ui.components.ScreenTopBar

@Composable
fun ProductListScreen(
    viewModel: ProductListViewModel,
    onProductClick: (ProductSummary) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("product_list_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                title = state.title.ifBlank { "Products" },
                onBack = onBack,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
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
}
