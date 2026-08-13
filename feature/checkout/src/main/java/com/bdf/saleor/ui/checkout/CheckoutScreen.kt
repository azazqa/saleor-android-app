package com.bdf.saleor.ui.checkout

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdf.saleor.data.model.Address
import com.bdf.saleor.data.model.AddressDraft
import com.bdf.saleor.data.model.DeliveryOption
import com.bdf.saleor.feature.checkout.R
import com.bdf.saleor.ui.components.BackTextLink
import com.bdf.saleor.ui.components.LoadingState
import kotlinx.coroutines.launch

@Composable
fun CheckoutRoute(
    onBack: () -> Unit,
    onCompleted: (orderId: String, orderNumber: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingTransactionId by remember { mutableStateOf<String?>(null) }
    val tossLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val transactionId = pendingTransactionId
        pendingTransactionId = null
        if (result.resultCode == Activity.RESULT_OK && !transactionId.isNullOrBlank()) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            viewModel.finishTossPayment(
                transactionId = transactionId,
                paymentKey = data.getStringExtra(TossPaymentActivity.EXTRA_PAYMENT_KEY).orEmpty(),
                orderId = data.getStringExtra(TossPaymentActivity.EXTRA_ORDER_ID).orEmpty(),
                amount = data.getDoubleExtra(TossPaymentActivity.EXTRA_AMOUNT, 0.0),
            )
        } else {
            viewModel.onPaymentFailed(
                result.data?.getStringExtra(TossPaymentActivity.EXTRA_ERROR_MESSAGE)
                    ?: context.getString(R.string.checkout_toss_cancelled),
            )
        }
    }
    LaunchedEffect(state.completedOrderId) {
        val orderId = state.completedOrderId ?: return@LaunchedEffect
        onCompleted(orderId, state.completedOrderNumber.orEmpty())
    }
    if (!state.completedOrderId.isNullOrBlank()) {
        LoadingState()
        return
    }
    CheckoutScreen(
        state = state,
        onBack = {
            if (state.step == CheckoutStep.Contact) onBack() else viewModel.goBack()
        },
        onEmailChange = viewModel::onEmailChange,
        onShippingChange = viewModel::onShippingDraftChange,
        onBillingChange = viewModel::onBillingDraftChange,
        onSameAsBilling = viewModel::onSameAsBillingChange,
        onSelectSavedAddress = viewModel::selectSavedAddress,
        onContinueContact = viewModel::continueFromContact,
        onSelectDelivery = viewModel::selectDelivery,
        onContinueShipping = viewModel::continueFromShipping,
        onPointsInput = viewModel::onPointsInputChange,
        onApplyPoints = viewModel::applyPoints,
        onFreeComplete = viewModel::completeFreeOrder,
        onGoToStep = viewModel::goToStep,
        onPayWithToss = {
            scope.launch {
                val prepared = viewModel.prepareTossPayment().getOrNull() ?: return@launch
                pendingTransactionId = prepared.transactionId
                tossLauncher.launch(TossPaymentActivity.intent(context, prepared))
            }
        },
        modifier = modifier,
    )
}

@Composable
fun CheckoutScreen(
    state: CheckoutUiState,
    onBack: () -> Unit,
    onEmailChange: (String) -> Unit,
    onShippingChange: (AddressDraft) -> Unit,
    onBillingChange: (AddressDraft) -> Unit,
    onSameAsBilling: (Boolean) -> Unit,
    onSelectSavedAddress: (Address) -> Unit,
    onContinueContact: () -> Unit,
    onSelectDelivery: (String) -> Unit,
    onContinueShipping: () -> Unit,
    onPointsInput: (String) -> Unit,
    onApplyPoints: () -> Unit,
    onFreeComplete: () -> Unit,
    onGoToStep: (CheckoutStep) -> Unit,
    onPayWithToss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("checkout_screen"),
    ) {
        BackTextLink(text = stringResource(R.string.checkout_back), onClick = onBack)
        Text(
            text = when (state.step) {
                CheckoutStep.Contact -> stringResource(R.string.checkout_contact)
                CheckoutStep.Shipping -> stringResource(R.string.checkout_shipping)
                CheckoutStep.Payment -> stringResource(R.string.checkout_payment)
            },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (state.isLoading) {
            LoadingState()
            return
        }
        if (state.confirming) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("checkout_confirming"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.checkout_confirming))
            }
            return
        }
        if (!state.error.isNullOrBlank()) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        }
        if (!state.priceChangeMessage.isNullOrBlank()) {
            Text(
                text = state.priceChangeMessage,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        when (state.step) {
            CheckoutStep.Contact -> ContactStep(
                state = state,
                onEmailChange = onEmailChange,
                onShippingChange = onShippingChange,
                onSelectSavedAddress = onSelectSavedAddress,
                onContinue = onContinueContact,
            )
            CheckoutStep.Shipping -> ShippingStep(
                state = state,
                options = state.deliveryOptions,
                selectedId = state.selectedDeliveryMethodId,
                onSelect = onSelectDelivery,
                onContinue = onContinueShipping,
                onChangeContact = { onGoToStep(CheckoutStep.Contact) },
                busy = state.payBusy,
            )
            CheckoutStep.Payment -> PaymentStep(
                state = state,
                onBillingChange = onBillingChange,
                onSameAsBilling = onSameAsBilling,
                onPointsInput = onPointsInput,
                onApplyPoints = onApplyPoints,
                onFreeComplete = onFreeComplete,
                onChangeContact = { onGoToStep(CheckoutStep.Contact) },
                onChangeShipping = { onGoToStep(CheckoutStep.Shipping) },
                onPayWithToss = onPayWithToss,
            )
        }
    }
}

@Composable
private fun CheckoutContextSummary(
    email: String,
    shippingAddress: Address?,
    shippingDraft: AddressDraft,
    deliveryLabel: String?,
    showShipping: Boolean,
    onChangeContact: () -> Unit,
    onChangeShipping: (() -> Unit)? = null,
) {
    val shipTo = shippingAddress?.formattedLines()?.joinToString(", ")
        ?.ifBlank { null }
        ?: listOf(shippingDraft.streetAddress1, shippingDraft.streetAddress2, shippingDraft.postalCode)
            .filter { it.isNotBlank() }
            .joinToString(", ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("checkout_context_summary"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SummaryContextRow(
            label = stringResource(R.string.checkout_summary_contact),
            value = email,
            onChange = onChangeContact,
        )
        if (showShipping) {
            SummaryContextRow(
                label = stringResource(R.string.checkout_summary_ship_to),
                value = shipTo,
                onChange = onChangeContact,
            )
            if (!deliveryLabel.isNullOrBlank() && onChangeShipping != null) {
                SummaryContextRow(
                    label = stringResource(R.string.checkout_summary_method),
                    value = deliveryLabel,
                    onChange = onChangeShipping,
                )
            }
        }
    }
}

@Composable
private fun SummaryContextRow(label: String, value: String, onChange: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = onChange) {
            Text(stringResource(R.string.checkout_summary_change))
        }
    }
}

@Composable
private fun ContactStep(
    state: CheckoutUiState,
    onEmailChange: (String) -> Unit,
    onShippingChange: (AddressDraft) -> Unit,
    onSelectSavedAddress: (Address) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("checkout_contact"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_email"),
            label = { Text(stringResource(R.string.checkout_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
        )
        if (state.savedAddresses.isNotEmpty()) {
            Text(stringResource(R.string.checkout_select_address), style = MaterialTheme.typography.titleSmall)
            state.savedAddresses.forEach { address ->
                FilterChip(
                    selected = false,
                    onClick = { onSelectSavedAddress(address) },
                    label = { Text(address.formattedLines().firstOrNull() ?: address.streetAddress1) },
                )
            }
        }
        AddressFields(draft = state.shippingDraft, onChange = onShippingChange)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onContinue,
            enabled = !state.payBusy,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_contact_continue"),
        ) {
            Text(stringResource(R.string.checkout_continue))
        }
    }
}

@Composable
private fun ShippingStep(
    state: CheckoutUiState,
    options: List<DeliveryOption>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit,
    onChangeContact: () -> Unit,
    busy: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("checkout_shipping"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CheckoutContextSummary(
            email = state.email,
            shippingAddress = state.session?.shippingAddress,
            shippingDraft = state.shippingDraft,
            deliveryLabel = null,
            showShipping = true,
            onChangeContact = onChangeContact,
        )
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = option.id == selectedId,
                        onClick = { onSelect(option.id) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 8.dp)
                    .testTag("checkout_delivery_${option.id}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = option.id == selectedId,
                    onClick = { onSelect(option.id) },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(option.name, style = MaterialTheme.typography.titleMedium)
                    if (option.minDeliveryDays != null && option.maxDeliveryDays != null) {
                        Text(
                            stringResource(R.string.checkout_delivery_days, option.minDeliveryDays!!, option.maxDeliveryDays!!),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(option.price?.format().orEmpty())
            }
        }
        Button(
            onClick = onContinue,
            enabled = !busy && !selectedId.isNullOrBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_shipping_continue"),
        ) {
            Text(stringResource(R.string.checkout_continue))
        }
    }
}

@Composable
private fun PaymentStep(
    state: CheckoutUiState,
    onBillingChange: (AddressDraft) -> Unit,
    onSameAsBilling: (Boolean) -> Unit,
    onPointsInput: (String) -> Unit,
    onApplyPoints: () -> Unit,
    onFreeComplete: () -> Unit,
    onChangeContact: () -> Unit,
    onChangeShipping: () -> Unit,
    onPayWithToss: () -> Unit,
) {
    val selectedDelivery = state.deliveryOptions.firstOrNull { it.id == state.selectedDeliveryMethodId }
    val deliveryLabel = selectedDelivery?.let { option ->
        listOf(option.name, option.price?.format().orEmpty()).filter { it.isNotBlank() }.joinToString(" · ")
    } ?: state.session?.shipping?.format()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("checkout_payment"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CheckoutContextSummary(
            email = state.email,
            shippingAddress = state.session?.shippingAddress,
            shippingDraft = state.shippingDraft,
            deliveryLabel = deliveryLabel,
            showShipping = state.session?.isShippingRequired != false,
            onChangeContact = onChangeContact,
            onChangeShipping = if (state.session?.isShippingRequired == false) null else onChangeShipping,
        )
        Text(stringResource(R.string.checkout_summary), style = MaterialTheme.typography.titleMedium)
        state.session?.lines?.forEach { line ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${line.productName} × ${line.quantity}")
                Text(line.totalPrice?.format().orEmpty())
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.cart_shipping))
            Text(state.session?.shipping?.format().orEmpty())
        }
        val discount = state.session?.discount
        if (discount != null && discount.amount > 0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.checkout_discount))
                Text("-${discount.format()}")
            }
        }
        if (state.pointsApplied > 0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.checkout_points_applied, state.pointsApplied.toInt().toString()))
                Text("-${state.pointsApplied.toInt()}")
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.cart_total), style = MaterialTheme.typography.titleMedium)
            Text(
                state.payAmount?.let { "${it.toInt()}" } ?: state.session?.total?.format().orEmpty(),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.sameAsBilling, onCheckedChange = onSameAsBilling)
            Text(stringResource(R.string.checkout_same_as_shipping))
        }
        if (!state.sameAsBilling) {
            Text(stringResource(R.string.checkout_billing), style = MaterialTheme.typography.titleSmall)
            AddressFields(draft = state.billingDraft, onChange = onBillingChange)
        }

        if (state.showPointsSection) {
            Text(stringResource(R.string.checkout_points), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.checkout_points_balance, "${state.pointsBalance.toInt()} ${state.pointsCurrency}"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.pointsInput,
                    onValueChange = onPointsInput,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("checkout_points_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedButton(onClick = onApplyPoints, modifier = Modifier.testTag("checkout_points_apply")) {
                    Text(stringResource(R.string.checkout_points_apply))
                }
            }
            if (state.pointsClampNotice) {
                Text(stringResource(R.string.checkout_points_clamped), style = MaterialTheme.typography.bodySmall)
            }
        }

        if (state.isFreeOrder) {
            Button(
                onClick = onFreeComplete,
                enabled = !state.payBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_free_complete"),
            ) {
                Text(stringResource(R.string.checkout_free_complete))
            }
        } else {
            val payLabel = state.payAmount?.let { stringResource(R.string.checkout_pay_total, it.toInt().toString()) }
                ?: stringResource(R.string.checkout_pay)
            Button(
                onClick = onPayWithToss,
                enabled = !state.payBusy && !state.tossClientKey.isNullOrBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_pay"),
            ) {
                Text(payLabel)
            }
        }
    }
}

@Composable
private fun AddressFields(
    draft: AddressDraft,
    onChange: (AddressDraft) -> Unit,
) {
    OutlinedTextField(
        value = draft.firstName,
        onValueChange = { onChange(draft.copy(firstName = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.field_first_name)) },
    )
    OutlinedTextField(
        value = draft.postalCode,
        onValueChange = { onChange(draft.copy(postalCode = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.field_postal)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    OutlinedTextField(
        value = draft.streetAddress1,
        onValueChange = { onChange(draft.copy(streetAddress1 = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.field_street)) },
    )
    OutlinedTextField(
        value = draft.streetAddress2,
        onValueChange = { onChange(draft.copy(streetAddress2 = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.field_street2)) },
    )
    OutlinedTextField(
        value = draft.phone,
        onValueChange = { onChange(draft.copy(phone = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.field_phone)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    )
    OutlinedTextField(
        value = draft.city,
        onValueChange = { onChange(draft.copy(city = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.field_city_optional)) },
    )
}
