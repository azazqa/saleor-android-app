package com.bdf.saleor.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bdf.saleor.data.model.Cart
import com.bdf.saleor.data.model.CartLine
import com.bdf.saleor.feature.checkout.R
import com.bdf.saleor.ui.checkout.CheckoutBottomBar
import com.bdf.saleor.ui.checkout.CheckoutFlowCartStep
import com.bdf.saleor.ui.checkout.CheckoutStepper
import com.bdf.saleor.ui.checkout.SectionCard
import com.bdf.saleor.ui.checkout.SectionIconChip
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
    val onCheckoutNav by rememberUpdatedState(onCheckout)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onVisible()
    }
    CartScreen(
        cart = cart,
        error = uiState.error,
        selectedLineIds = uiState.selectedLineIds,
        busy = uiState.isUpdating,
        onBack = onBack,
        onToggleLine = viewModel::toggleLine,
        onToggleSelectAll = viewModel::toggleSelectAll,
        onIncrement = viewModel::increment,
        onDecrement = viewModel::decrement,
        onRemove = viewModel::remove,
        onClearAll = viewModel::clearAll,
        onCheckout = { viewModel.prepareCheckout { ready -> if (ready) onCheckoutNav() } },
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
    onClearAll: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
    selectedLineIds: Set<String> = emptySet(),
    busy: Boolean = false,
    onToggleLine: (String) -> Unit = {},
    onToggleSelectAll: () -> Unit = {},
) {
    val hasLines = cart != null && cart.lines.isNotEmpty()
    val colors = MaterialTheme.colorScheme
    var pendingRemoveId by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val lineIds = cart?.lines?.map { it.id }.orEmpty().toSet()
    val selectedIds = selectedLineIds.intersect(lineIds)
    val selectedQuantity = cart?.selectedQuantity(selectedIds) ?: 0
    val selectedSubtotal = cart?.selectedSubtotal(selectedIds)
    val allSelected = lineIds.isNotEmpty() && selectedIds.containsAll(lineIds)
    val shippingKnown = allSelected && (cart?.shipping?.amount ?: 0.0) > 0
    val displayTotal = if (shippingKnown) cart?.total else selectedSubtotal
    val selectAllState = when {
        selectedIds.isEmpty() -> ToggleableState.Off
        allSelected -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("cart_screen"),
        containerColor = colors.surface,
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.cart_title),
                onBack = onBack,
                containerColor = colors.surfaceContainerLow,
            )
        },
        bottomBar = {
            if (hasLines) {
                CheckoutBottomBar(
                    summaryLabel = stringResource(R.string.cart_pay_summary, selectedQuantity),
                    amount = displayTotal?.format().orEmpty(),
                    ctaLabel = stringResource(R.string.cart_checkout),
                    enabled = selectedIds.isNotEmpty(),
                    busy = busy,
                    onClick = onCheckout,
                    ctaTestTag = "cart_checkout",
                    ctaIcon = Icons.AutoMirrored.Filled.ArrowForward,
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            CheckoutStepper(stepIndex = CheckoutFlowCartStep)
            if (!error.isNullOrBlank()) {
                Text(
                    text = error,
                    color = colors.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
            if (cart == null || cart.lines.isEmpty()) {
                CartEmptyState(onContinue = onBack)
                return@Column
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionCard(
                    icon = { SectionIconChip(Icons.Outlined.ShoppingBag) },
                    title = stringResource(R.string.cart_item_count, cart.lines.size),
                    action = {
                        TextButton(
                            onClick = { confirmClear = true },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("cart_clear_all"),
                        ) {
                            Text(
                                text = stringResource(R.string.cart_clear_all),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary,
                            )
                        }
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            TriStateCheckbox(
                                state = selectAllState,
                                onClick = onToggleSelectAll,
                                modifier = Modifier.testTag("cart_select_all"),
                            )
                        }
                        Text(
                            text = stringResource(R.string.cart_select_all),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                        )
                    }
                    cart.lines.forEachIndexed { index, line ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = colors.outlineVariant,
                            )
                        }
                        CartLineRow(
                            line = line,
                            selected = line.id in selectedIds,
                            onToggle = { onToggleLine(line.id) },
                            onIncrement = { onIncrement(line.id) },
                            onDecrement = { onDecrement(line.id) },
                            onRemove = { pendingRemoveId = line.id },
                        )
                    }
                }
                SectionCard(
                    icon = { SectionIconChip(Icons.Outlined.ShoppingBag) },
                    title = stringResource(R.string.cart_total),
                ) {
                    CartAmountRow(
                        label = stringResource(R.string.cart_subtotal),
                        value = selectedSubtotal?.format().orEmpty(),
                    )
                    CartAmountRow(
                        label = stringResource(R.string.cart_shipping),
                        value = if (shippingKnown) {
                            cart.shipping?.format().orEmpty()
                        } else {
                            stringResource(R.string.cart_shipping_excluded)
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = colors.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (shippingKnown) {
                                stringResource(R.string.cart_total)
                            } else {
                                stringResource(R.string.cart_total_excluded)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                        )
                        Text(
                            text = displayTotal?.format().orEmpty(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.primary,
                        )
                    }
                }
            }
        }
    }
    val removeId = pendingRemoveId
    if (removeId != null) {
        AlertDialog(
            onDismissRequest = { pendingRemoveId = null },
            title = { Text(stringResource(R.string.cart_remove_confirm_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove(removeId)
                        pendingRemoveId = null
                    },
                ) {
                    Text(stringResource(R.string.cart_remove_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveId = null }) {
                    Text(stringResource(R.string.checkout_cancel))
                }
            },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.cart_clear_confirm_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        confirmClear = false
                    },
                ) {
                    Text(stringResource(R.string.cart_remove_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.checkout_cancel))
                }
            },
        )
    }
}

@Composable
private fun CartEmptyState(onContinue: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("cart_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ShoppingBag,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.cart_empty),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.cart_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onContinue, modifier = Modifier.testTag("cart_continue_shopping")) {
            Text(stringResource(R.string.cart_continue_shopping))
        }
    }
}

@Composable
private fun CartLineRow(
    line: CartLine,
    selected: Boolean,
    onToggle: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val selectLabel = stringResource(R.string.cart_select_item)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_line_${line.variantId}"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                modifier = Modifier
                    .testTag("cart_select_${line.variantId}")
                    .semantics { contentDescription = selectLabel },
            )
        }
        AsyncImage(
            model = line.thumbnailUrl,
            contentDescription = line.productName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceVariant),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = line.productName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                Text(
                    text = line.totalPrice?.format().orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
            }
            if (line.variantName.isNotBlank()) {
                Text(
                    text = stringResource(R.string.cart_variant_option, line.variantName),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuantityStepper(
                    quantity = line.quantity,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement,
                    decrementEnabled = line.quantity > 1,
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("cart_remove"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.cart_remove),
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    decrementEnabled: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .height(48.dp)
            .border(1.dp, colors.outlineVariant, CircleShape)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onDecrement,
            enabled = decrementEnabled,
            modifier = Modifier
                .size(48.dp)
                .testTag("cart_qty_decrease"),
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = stringResource(R.string.cart_qty_decrease),
                tint = if (decrementEnabled) colors.onSurface else colors.onSurfaceVariant,
            )
        }
        Text(
            text = "$quantity",
            modifier = Modifier.testTag("cart_qty"),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
        )
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
    }
}

@Composable
private fun CartAmountRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
