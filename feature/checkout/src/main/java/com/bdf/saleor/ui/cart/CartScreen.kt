package com.bdf.saleor.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bdf.saleor.data.model.Cart
import com.bdf.saleor.data.model.CartLine
import com.bdf.saleor.feature.checkout.R
import com.bdf.saleor.ui.checkout.CheckoutFlowCartStep
import com.bdf.saleor.ui.checkout.CheckoutFlowProgress
import com.bdf.saleor.ui.components.ScreenTopBar

@Composable
fun CartRoute(
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CartScreen(
        cart = cart,
        error = uiState.error,
        onBack = onBack,
        onIncrement = viewModel::increment,
        onDecrement = viewModel::decrement,
        onRemove = viewModel::remove,
        onCheckout = onCheckout,
        modifier = modifier,
    )
}

@Composable
fun CartScreen(
    cart: Cart?,
    error: String?,
    onBack: () -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasLines = cart != null && cart.lines.isNotEmpty()
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("cart_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(title = stringResource(R.string.cart_title), onBack = onBack)
        },
        bottomBar = {
            if (hasLines) {
                Button(
                    onClick = onCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("cart_checkout"),
                ) {
                    Text(stringResource(R.string.cart_checkout))
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            CheckoutFlowProgress(stepIndex = CheckoutFlowCartStep)
            if (!error.isNullOrBlank()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
            if (!hasLines) {
                Text(
                    text = stringResource(R.string.cart_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(24.dp)
                        .testTag("cart_empty"),
                )
                return@Column
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                cart!!.lines.forEach { line ->
                    CartLineRow(
                        line = line,
                        onIncrement = { onIncrement(line.id) },
                        onDecrement = { onDecrement(line.id) },
                        onRemove = { onRemove(line.id) },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                SummaryRow(stringResource(R.string.cart_subtotal), cart.subtotal?.format().orEmpty())
                SummaryRow(stringResource(R.string.cart_shipping), stringResource(R.string.cart_shipping_pending))
                SummaryRow(stringResource(R.string.cart_total), cart.total?.format().orEmpty(), emphasize = true)
            }
        }
    }
}

@Composable
private fun CartLineRow(
    line: CartLine,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_line_${line.variantId}"),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = line.thumbnailUrl,
                contentDescription = line.productName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(line.productName, style = MaterialTheme.typography.titleMedium)
                if (line.variantName.isNotBlank()) {
                    Text(line.variantName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(line.totalPrice?.format().orEmpty(), style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("cart_qty_decrease"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = stringResource(R.string.cart_qty_decrease),
                        )
                    }
                    Text("${line.quantity}", modifier = Modifier.testTag("cart_qty"))
                    IconButton(
                        onClick = onIncrement,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("cart_qty_increase"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.cart_qty_increase),
                        )
                    }
                    TextButton(onClick = onRemove) {
                        Text(stringResource(R.string.cart_remove))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
    }
}
