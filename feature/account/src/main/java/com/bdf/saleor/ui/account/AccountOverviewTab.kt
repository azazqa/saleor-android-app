package com.bdf.saleor.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bdf.saleor.data.model.OrderSummary
import com.bdf.saleor.feature.account.R

@Composable
fun AccountOverviewTab(
    state: AccountUiState,
    onViewPoints: () -> Unit,
    onViewOrders: () -> Unit,
    onManageAddress: () -> Unit,
    onOrderClick: (OrderSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.profile
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.overview_welcome, profile?.welcomeName.orEmpty()),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("account_welcome"),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.overview_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))

        val membership = profile?.membership
        if (membership != null) {
            val isTopTier = membership.nextTierName == null
            val progress = membershipProgress(
                current = membership.currentSpend.amount,
                remaining = membership.amountToNextTier.amount,
                isTopTier = isTopTier,
            )
            AccountCard {
                Text(
                    text = membership.tierName ?: stringResource(R.string.membership_no_tier),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.primary,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isTopTier && membership.tierName != null) {
                        stringResource(R.string.membership_highest)
                    } else {
                        stringResource(
                            R.string.membership_remaining,
                            membership.amountToNextTier.format(),
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        AccountCard(modifier = Modifier.clickable(onClick = onViewPoints)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.points_teaser_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = profile?.pointsBalance?.format() ?: "₩0",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = stringResource(R.string.points_teaser_cta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.overview_recent_orders), style = MaterialTheme.typography.titleMedium)
            if (state.recentOrders.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.overview_view_all),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onViewOrders),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (state.recentOrders.isEmpty()) {
            DashedEmptyBox(message = stringResource(R.string.orders_empty))
        } else {
            state.recentOrders.forEach { order ->
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { onOrderClick(order) },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.order_number, order.number), style = MaterialTheme.typography.titleMedium)
                        Text(order.statusDisplay, style = MaterialTheme.typography.bodyMedium)
                        Text(order.total?.format().orEmpty(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.overview_default_address), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.overview_manage_address),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onManageAddress),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val address = profile?.defaultShippingAddress
        if (address == null) {
            DashedEmptyBox(message = stringResource(R.string.addresses_empty))
        } else {
            AccountCard {
                address.formattedLines().forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun membershipProgress(
    current: Double,
    remaining: Double,
    isTopTier: Boolean,
): Float {
    if (isTopTier) return 1f
    val total = current + remaining
    if (total <= 0.0) return 0f
    return (current / total).toFloat().coerceIn(0f, 1f)
}
