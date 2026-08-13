package com.bdf.saleor.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.data.model.OrderSummary
import com.bdf.saleor.feature.account.R
import com.bdf.saleor.ui.components.EmptyState
import com.bdf.saleor.ui.components.ErrorState
import com.bdf.saleor.ui.components.InfiniteListHandler
import com.bdf.saleor.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    onOrderClick: (OrderSummary) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrderListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    InfiniteListHandler(
        listState = listState,
        buffer = 3,
        enabled = state.hasNextPage && !state.isLoadingMore,
        onLoadMore = viewModel::loadMore,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("order_list_screen"),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.orders_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
        when {
            state.isLoading -> LoadingState()
            state.error != null && state.orders.isEmpty() -> ErrorState(
                message = state.error ?: stringResource(DesignR.string.error_generic),
                onRetry = viewModel::refresh,
            )
            state.orders.isEmpty() -> EmptyState(message = stringResource(R.string.orders_empty))
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.orders, key = { it.id }) { order ->
                    OrderRow(order = order, onClick = { onOrderClick(order) })
                }
            }
        }
    }
}

@Composable
private fun OrderRow(
    order: OrderSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_row_${order.number}")
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(R.string.order_number, order.number),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = order.total?.format().orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(order.statusDisplay, style = MaterialTheme.typography.bodyMedium)
            if (!order.paymentStatusDisplay.isNullOrBlank()) {
                Text(
                    text = order.paymentStatusDisplay.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
