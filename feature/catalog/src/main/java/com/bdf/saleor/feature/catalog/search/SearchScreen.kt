package com.bdf.saleor.feature.catalog.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.core.model.ProductSummary
import com.bdf.saleor.feature.catalog.R
import com.bdf.saleor.core.designsystem.components.EmptyState
import com.bdf.saleor.core.designsystem.components.ErrorState
import com.bdf.saleor.core.designsystem.components.LoadingState
import com.bdf.saleor.core.ui.ProductGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onProductClick: (ProductSummary) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_screen"),
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = {},
                    expanded = false,
                    onExpandedChange = {},
                    modifier = Modifier.testTag("search_field"),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                            }
                        }
                    },
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {}
        when {
            !state.hasSearched -> EmptyState(message = stringResource(R.string.search_empty))
            state.isLoading -> LoadingState()
            state.error != null && state.products.isEmpty() -> ErrorState(
                message = state.error ?: stringResource(DesignR.string.error_generic),
                onRetry = { viewModel.onQueryChange(state.query) },
            )
            state.products.isEmpty() -> EmptyState(message = stringResource(R.string.search_no_results))
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
