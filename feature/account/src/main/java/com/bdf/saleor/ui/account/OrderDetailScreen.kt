package com.bdf.saleor.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.feature.account.R
import com.bdf.saleor.ui.components.BackTextLink
import com.bdf.saleor.ui.components.ErrorState
import com.bdf.saleor.ui.components.LoadingState

@Composable
fun OrderDetailScreen(
    viewModel: OrderDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("order_detail_screen"),
    ) {
        BackTextLink(text = stringResource(R.string.back_link), onClick = onBack)
        Text(
            text = stringResource(R.string.order_detail_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        when {
            state.isLoading -> LoadingState()
            state.error == "not_found" -> ErrorState(
                message = stringResource(R.string.order_not_found),
                onRetry = onBack,
            )
            state.error != null -> ErrorState(
                message = state.error ?: stringResource(DesignR.string.error_generic),
                onRetry = viewModel::refresh,
            )
            state.order != null -> {
                val order = state.order!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.order_number, order.number),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.testTag("order_detail_number"),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${stringResource(R.string.order_status)}: ${order.statusDisplay}")
                    if (!order.paymentStatusDisplay.isNullOrBlank()) {
                        Text("${stringResource(R.string.order_payment)}: ${order.paymentStatusDisplay}")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.order_lines), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    order.lines.forEach { line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        ) {
                            AsyncImage(
                                model = line.thumbnailUrl,
                                contentDescription = line.productName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(line.productName, style = MaterialTheme.typography.titleMedium)
                                if (line.variantName.isNotBlank()) {
                                    Text(
                                        line.variantName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(stringResource(R.string.order_quantity, line.quantity))
                                Text(line.totalPrice?.format().orEmpty())
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    PriceRow(stringResource(R.string.order_subtotal), order.subtotal?.format().orEmpty())
                    PriceRow(stringResource(R.string.order_shipping), order.shippingPrice?.format().orEmpty())
                    PriceRow(stringResource(R.string.order_total), order.total?.format().orEmpty())
                    if (!order.shippingAddress.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.order_address), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(order.shippingAddress.orEmpty())
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Text(label)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
