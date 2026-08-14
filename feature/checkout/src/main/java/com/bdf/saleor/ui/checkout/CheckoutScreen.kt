package com.bdf.saleor.ui.checkout

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdf.saleor.data.model.Address
import com.bdf.saleor.data.model.AddressDraft
import com.bdf.saleor.data.model.DeliveryOption
import com.bdf.saleor.data.model.Money
import com.bdf.saleor.data.model.digitsOnlyMobile
import com.bdf.saleor.data.model.formatKoreanMobileNumber
import com.bdf.saleor.feature.checkout.R
import com.bdf.saleor.ui.components.LoadingState
import com.bdf.saleor.ui.components.LocalSnackbarHostState
import com.bdf.saleor.ui.components.ScreenTopBar
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
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.reloadCustomerAddresses()
    }
    LaunchedEffect(state.completedOrderId) {
        val orderId = state.completedOrderId ?: return@LaunchedEffect
        onCompleted(orderId, state.completedOrderNumber.orEmpty())
    }
    if (!state.completedOrderId.isNullOrBlank()) {
        LoadingState()
        return
    }
    if (state.showAddressForm) {
        CheckoutAddressCreateScreen(
            busy = state.payBusy,
            error = state.error,
            onBack = viewModel::closeAddressForm,
            onSubmit = viewModel::createShippingAddress,
            modifier = modifier,
        )
        return
    }
    CheckoutScreen(
        state = state,
        onBack = {
            if (state.step == CheckoutStep.Contact) onBack() else viewModel.goBack()
        },
        onEmailChange = viewModel::onEmailChange,
        onSelectSavedAddress = viewModel::selectSavedAddress,
        onChangePaymentAddress = viewModel::changePaymentShippingAddress,
        onUseAsDefaultChange = viewModel::onUseSelectedAddressAsDefaultChange,
        onRegisterAddress = viewModel::openAddressForm,
        onContinueContact = viewModel::continueFromContact,
        onSelectDelivery = viewModel::selectDelivery,
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
    onSelectSavedAddress: (Address) -> Unit,
    onChangePaymentAddress: (Address) -> Unit,
    onUseAsDefaultChange: (Boolean) -> Unit,
    onRegisterAddress: () -> Unit,
    onContinueContact: () -> Unit,
    onSelectDelivery: (String) -> Unit,
    onPointsInput: (String) -> Unit,
    onApplyPoints: () -> Unit,
    onFreeComplete: () -> Unit,
    onGoToStep: (CheckoutStep) -> Unit,
    onPayWithToss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    LaunchedEffect(state.priceChangeMessage) {
        val message = state.priceChangeMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
    }
    val stepTitle = when (state.step) {
        CheckoutStep.Contact -> stringResource(R.string.checkout_contact)
        CheckoutStep.Payment -> stringResource(R.string.checkout_payment)
    }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("checkout_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(title = stepTitle, onBack = onBack)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            CheckoutFlowProgress(stepIndex = state.step.toFlowIndex())
            when {
                state.isLoading -> LoadingState()
                state.confirming -> Column(
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
                else -> {
                    if (!state.error.isNullOrBlank()) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    when (state.step) {
                        CheckoutStep.Contact -> ContactStep(
                            state = state,
                            onEmailChange = onEmailChange,
                            onSelectSavedAddress = onSelectSavedAddress,
                            onUseAsDefaultChange = onUseAsDefaultChange,
                            onRegisterAddress = onRegisterAddress,
                            onSelectDelivery = onSelectDelivery,
                            onContinue = onContinueContact,
                        )
                        CheckoutStep.Payment -> PaymentStep(
                            state = state,
                            onPointsInput = onPointsInput,
                            onApplyPoints = onApplyPoints,
                            onFreeComplete = onFreeComplete,
                            onChangeContact = { onGoToStep(CheckoutStep.Contact) },
                            onChangeShipping = { onGoToStep(CheckoutStep.Contact) },
                            onChangePaymentAddress = onChangePaymentAddress,
                            onRegisterAddress = onRegisterAddress,
                            onPayWithToss = onPayWithToss,
                        )
                    }
                }
            }
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
    onChangeAddress: (() -> Unit)? = null,
    showContact: Boolean = true,
) {
    val shipTo = shippingAddress?.formattedLines()?.joinToString(if (showContact) ", " else "\n")
        ?.ifBlank { null }
        ?: listOf(shippingDraft.streetAddress1, shippingDraft.streetAddress2, shippingDraft.postalCode)
            .filter { it.isNotBlank() }
            .joinToString(if (showContact) ", " else "\n")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("checkout_context_summary"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showContact) {
            SummaryContextRow(
                label = stringResource(R.string.checkout_summary_contact),
                value = email,
                onChange = onChangeContact,
            )
        }
        if (showShipping) {
            SummaryContextRow(
                label = stringResource(R.string.checkout_summary_ship_to),
                value = shipTo,
                onChange = onChangeAddress ?: onChangeContact,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactStep(
    state: CheckoutUiState,
    onEmailChange: (String) -> Unit,
    onSelectSavedAddress: (Address) -> Unit,
    onUseAsDefaultChange: (Boolean) -> Unit,
    onRegisterAddress: () -> Unit,
    onSelectDelivery: (String) -> Unit,
    onContinue: () -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val selected = state.selectedAddress
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("checkout_contact"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
        ShippingAddressCard(
            address = selected,
            onChange = { pickerOpen = true },
            onRegister = onRegisterAddress,
        )
        if (state.showUseAsDefaultShipping) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.useSelectedAddressAsDefault,
                        onValueChange = onUseAsDefaultChange,
                        role = Role.Checkbox,
                    )
                    .testTag("checkout_use_as_default_row"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.useSelectedAddressAsDefault,
                    onCheckedChange = null,
                    modifier = Modifier.testTag("checkout_use_as_default"),
                )
                Text(stringResource(R.string.checkout_use_as_default))
            }
        }
        if (state.session?.isShippingRequired != false && selected != null) {
            Text(stringResource(R.string.checkout_shipping), style = MaterialTheme.typography.titleMedium)
            if (state.payBusy && state.deliveryOptions.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            DeliveryMethodOptions(
                options = state.deliveryOptions,
                selectedId = state.selectedDeliveryMethodId,
                onSelect = onSelectDelivery,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onContinue,
            enabled = state.canContinueFromContact,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_contact_continue"),
        ) {
            Text(stringResource(R.string.checkout_continue))
        }
    }
    if (pickerOpen) {
        AddressPickerSheet(
            addresses = state.savedAddresses,
            selectedId = selected?.id,
            onDismiss = { pickerOpen = false },
            onSelect = { address ->
                onSelectSavedAddress(address)
                pickerOpen = false
            },
            onRegisterNew = {
                pickerOpen = false
                onRegisterAddress()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressPickerSheet(
    addresses: List<Address>,
    selectedId: String?,
    onDismiss: () -> Unit,
    onSelect: (Address) -> Unit,
    onRegisterNew: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .testTag("checkout_address_picker"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.checkout_select_address),
                style = MaterialTheme.typography.titleLarge,
            )
            addresses.forEach { address ->
                val selected = address.id == selectedId
                ListItem(
                    headlineContent = {
                        Text(address.recipientName.ifBlank { address.streetAddress1 })
                    },
                    supportingContent = {
                        Text(address.localityLine())
                    },
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (address.isDefaultShipping) {
                                DefaultShippingBadge()
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(address) },
                )
            }
            Button(
                onClick = onRegisterNew,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_address_picker_add"),
            ) {
                Text(stringResource(R.string.checkout_new_address))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ShippingAddressCard(
    address: Address?,
    onChange: () -> Unit,
    onRegister: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("checkout_shipping_card"),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.checkout_summary_ship_to),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (address != null) {
                TextButton(onClick = onChange, modifier = Modifier.testTag("checkout_address_change")) {
                    Text(stringResource(R.string.checkout_change_address))
                }
            }
        }
        if (address == null) {
            Text(
                text = stringResource(R.string.checkout_no_address),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_register_address"),
            ) {
                Text(stringResource(R.string.checkout_register_address))
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = address.recipientName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (address.isDefaultShipping) {
                    DefaultShippingBadge()
                }
            }
            Text(address.localityLine(), style = MaterialTheme.typography.bodyMedium)
            if (address.streetAddress1.isNotBlank() && address.streetAddress1 != address.localityLine()) {
                Text(address.streetAddress1, style = MaterialTheme.typography.bodyMedium)
            }
            if (address.streetAddress2.isNotBlank()) {
                Text(address.streetAddress2, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = listOf(address.recipientName, address.displayPhone()).filter { it.isNotBlank() }.joinToString(" "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        }
    }
}

@Composable
private fun DefaultShippingBadge() {
    Badge {
        Text(stringResource(R.string.checkout_default_shipping))
    }
}

@Composable
private fun CheckoutAddressCreateScreen(
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSubmit: (AddressDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf(AddressDraft()) }
    var showPostcode by remember { mutableStateOf(false) }
    if (showPostcode) {
        KakaoPostcodeDialog(
            onResult = { patch ->
                draft = draft.applyKakao(patch)
                showPostcode = false
            },
            onDismiss = { showPostcode = false },
            modifier = modifier,
        )
        return
    }
    BackHandler(onBack = onBack)
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("checkout_address_create"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.checkout_register_address_title),
                onBack = onBack,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        if (!error.isNullOrBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        OutlinedTextField(
            value = draft.firstName,
            onValueChange = { draft = draft.copy(firstName = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_first_name)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = draft.phone,
            onValueChange = { draft = draft.copy(phone = digitsOnlyMobile(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_mobile)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = KoreanMobileVisualTransformation,
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = draft.postalCode,
                onValueChange = { draft = draft.copy(postalCode = it) },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.field_postal)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedButton(
                onClick = { showPostcode = true },
                modifier = Modifier
                    .height(56.dp)
                    .testTag("checkout_find_address"),
            ) {
                Text(stringResource(R.string.checkout_find_address))
            }
        }
        OutlinedTextField(
            value = draft.streetAddress1,
            onValueChange = { draft = draft.copy(streetAddress1 = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_street)) },
        )
        OutlinedTextField(
            value = draft.streetAddress2,
            onValueChange = { draft = draft.copy(streetAddress2 = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_street2)) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onSubmit(draft.copy(phone = formatKoreanMobileNumber(draft.phone))) },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_address_create_submit"),
        ) {
            Text(stringResource(R.string.checkout_register_address))
        }
        OutlinedButton(
            onClick = onBack,
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_address_create_cancel"),
        ) {
            Text(stringResource(R.string.checkout_cancel))
        }
        }
    }
}

@Composable
private fun DeliveryMethodOptions(
    options: List<DeliveryOption>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            stringResource(
                                R.string.checkout_delivery_days,
                                option.minDeliveryDays!!,
                                option.maxDeliveryDays!!,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(option.price?.format().orEmpty())
            }
        }
    }
}

@Composable
private fun PaymentStep(
    state: CheckoutUiState,
    onPointsInput: (String) -> Unit,
    onApplyPoints: () -> Unit,
    onFreeComplete: () -> Unit,
    onChangeContact: () -> Unit,
    onChangeShipping: () -> Unit,
    onChangePaymentAddress: (Address) -> Unit,
    onRegisterAddress: () -> Unit,
    onPayWithToss: () -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }
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
            shippingAddress = state.selectedAddress ?: state.session?.shippingAddress,
            shippingDraft = state.shippingDraft,
            deliveryLabel = deliveryLabel,
            showContact = false,
            showShipping = state.session?.isShippingRequired != false,
            onChangeContact = onChangeContact,
            onChangeShipping = if (state.session?.isShippingRequired == false) null else onChangeShipping,
            onChangeAddress = { pickerOpen = true },
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
                state.payAmount?.let { Money(it, state.currency).format() }
                    ?: state.session?.total?.format().orEmpty(),
                style = MaterialTheme.typography.titleMedium,
            )
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
    if (pickerOpen) {
        AddressPickerSheet(
            addresses = state.savedAddresses,
            selectedId = state.selectedAddressId,
            onDismiss = { pickerOpen = false },
            onSelect = { address ->
                onChangePaymentAddress(address)
                pickerOpen = false
            },
            onRegisterNew = {
                pickerOpen = false
                onRegisterAddress()
            },
        )
    }
}
