package com.bdf.saleor.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import com.bdf.saleor.data.model.CategoryItem
import com.bdf.saleor.feature.catalog.R
import com.bdf.saleor.ui.components.EmptyState
import com.bdf.saleor.ui.components.ErrorState
import com.bdf.saleor.ui.components.LoadingState

@Composable
fun CategoryListScreen(
    onCategoryClick: (CategoryItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("categories_screen"),
    ) {
        Text(
            text = stringResource(R.string.categories_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        when {
            state.isLoading -> LoadingState()
            state.error != null -> ErrorState(
                message = state.error ?: stringResource(DesignR.string.error_generic),
                onRetry = viewModel::refresh,
            )
            state.categories.isEmpty() -> EmptyState(message = stringResource(DesignR.string.products_empty))
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.categories, key = { it.id }) { category ->
                    CategoryRow(category = category, onClick = { onCategoryClick(category) })
                    category.children.forEach { child ->
                        CategoryRow(
                            category = child,
                            onClick = { onCategoryClick(child) },
                            indented = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategoryItem,
    onClick: () -> Unit,
    indented: Boolean = false,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indented) 16.dp else 0.dp)
            .testTag("category_row_${category.slug}"),
        shape = MaterialTheme.shapes.medium,
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = category.name,
                    style = if (indented) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = category.name,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}
