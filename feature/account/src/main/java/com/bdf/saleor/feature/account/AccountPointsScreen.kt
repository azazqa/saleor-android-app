package com.bdf.saleor.feature.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.core.model.PointsHistoryEntry
import com.bdf.saleor.feature.account.R
import com.bdf.saleor.core.designsystem.components.ErrorState
import com.bdf.saleor.core.designsystem.components.InfiniteListHandler
import com.bdf.saleor.core.designsystem.components.LoadingState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun AccountPointsScreen(
    modifier: Modifier = Modifier,
    viewModel: PointsViewModel = hiltViewModel(),
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
            .testTag("account_points_screen"),
    ) {
        when {
            state.isLoading -> LoadingState()
            state.error != null && state.entries.isEmpty() -> ErrorState(
                message = state.error ?: stringResource(DesignR.string.error_generic),
                onRetry = viewModel::refresh,
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                item {
                    Text(stringResource(R.string.points_title), style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.points_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AccountCard {
                        Text(
                            text = stringResource(R.string.points_balance_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.balance?.format() ?: "₩0",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(stringResource(R.string.points_history_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.points_count, state.totalCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (state.entries.isEmpty()) {
                    item { DashedEmptyBox(message = stringResource(R.string.points_empty)) }
                } else {
                    items(state.entries, key = { it.id }) { entry ->
                        PointsHistoryRow(entry)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun PointsHistoryRow(entry: PointsHistoryEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(pointsTypeLabel(entry.type), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = formatPointsDate(entry.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!entry.reason.isNullOrBlank()) {
                Text(
                    text = entry.reason.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text(entry.amount.format(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(R.string.points_balance_after, entry.balanceAfter.format()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun pointsTypeLabel(type: String): String = when (type.uppercase()) {
    "GRANTED" -> stringResource(R.string.points_type_granted)
    "DEDUCTED" -> stringResource(R.string.points_type_deducted)
    "USED_IN_ORDER" -> stringResource(R.string.points_type_used)
    "REFUNDED_IN_ORDER" -> stringResource(R.string.points_type_refunded)
    "AUTHORIZATION_CANCELED" -> stringResource(R.string.points_type_canceled)
    else -> type
}

private fun formatPointsDate(iso: String): String = runCatching {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).format(
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Locale.KOREA),
    )
}.getOrElse { iso }
