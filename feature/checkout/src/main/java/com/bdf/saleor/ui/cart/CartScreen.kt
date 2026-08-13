package com.bdf.saleor.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.bdf.saleor.ui.components.BackTextLink

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("cart_screen"),
    ) {
        BackTextLink(text = stringResource(R.string.checkout_back), onClick = onBack)
        Text(
            text = stringResource(R.string.cart_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        }
        if (cart == null || cart.lines.isEmpty()) {
            Text(
                text = stringResource(R.string.cart_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(24.dp)
                    .testTag("cart_empty"),
            )
            return
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            cart.lines.forEach { line ->
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
}

@Composable
private fun CartLineRow(
    line: CartLine,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
            .testTag("cart_line_${line.variantId}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = line.thumbnailUrl,
            contentDescription = line.productName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(line.productName, style = MaterialTheme.typography.titleMedium)
            if (line.variantName.isNotBlank()) {
                Text(line.variantName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(line.totalPrice?.format().orEmpty(), style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.testTag("cart_qty_decrease")) {
                    Text("−")
                }
                Text("${line.quantity}", modifier = Modifier.testTag("cart_qty"))
                IconButton(onClick = onIncrement, modifier = Modifier.testTag("cart_qty_increase")) {
                    Text("+")
                }
                TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.cart_remove))
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
