package com.bdf.saleor.ui.checkout

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import coil3.compose.AsyncImage
import com.bdf.saleor.data.model.Address
import com.bdf.saleor.data.model.AddressDraft
import com.bdf.saleor.data.model.CartLine
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
                amount = data.tossAmountWon().toDouble(),
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
        val editing = state.savedAddresses.firstOrNull { it.id == state.editingAddressId }
        CheckoutAddressCreateScreen(
            busy = state.payBusy,
            error = state.error,
            initialDraft = editing?.toFormDraft() ?: AddressDraft(),
            isEditing = editing != null,
            onBack = viewModel::closeAddressForm,
            onSubmit = if (editing != null) viewModel::updateShippingAddress else viewModel::createShippingAddress,
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
        onRegisterAddress = { viewModel.openAddressForm() },
        onEditAddress = { viewModel.openAddressForm(it) },
        onContinueContact = viewModel::continueFromContact,
        onSelectDelivery = viewModel::selectDelivery,
        onPointsInput = viewModel::onPointsInputChange,
        onApplyPoints = viewModel::applyPoints,
        onFreeComplete = viewModel::completeFreeOrder,
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
    onEditAddress: (Address) -> Unit,
    onContinueContact: () -> Unit,
    onSelectDelivery: (String) -> Unit,
    onPointsInput: (String) -> Unit,
    onApplyPoints: () -> Unit,
    onFreeComplete: () -> Unit,
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
    val colors = MaterialTheme.colorScheme
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("checkout_screen"),
        containerColor = colors.surface,
        topBar = {
            ScreenTopBar(
                title = stepTitle,
                onBack = onBack,
                containerColor = colors.surfaceContainerLow,
            )
        },
        bottomBar = {
            if (!state.isLoading && !state.confirming) {
                when (state.step) {
                    CheckoutStep.Contact -> {
                        val shippingLabel = state.session?.shipping
                            ?.takeIf { it.amount > 0 }
                            ?.format()
                            ?: stringResource(R.string.cart_shipping_excluded)
                        CheckoutBottomBar(
                            summaryLabel = stringResource(
                                R.string.checkout_contact_summary,
                                state.session?.subtotal?.format().orEmpty(),
                                shippingLabel,
                            ),
                            amount = state.session?.total?.format()
                                ?: state.session?.subtotal?.format().orEmpty(),
                            ctaLabel = stringResource(R.string.checkout_continue),
                            enabled = state.canContinueFromContact,
                            busy = state.payBusy,
                            onClick = onContinueContact,
                            ctaTestTag = "checkout_contact_continue",
                            ctaIcon = Icons.AutoMirrored.Filled.ArrowForward,
                        )
                    }
                    CheckoutStep.Payment -> CheckoutBottomBar(
                        summaryLabel = stringResource(R.string.checkout_final_amount),
                        amount = state.payAmount?.let { Money(it, state.currency).format() }
                            ?: state.session?.total?.format().orEmpty(),
                        ctaLabel = stringResource(
                            if (state.isFreeOrder) R.string.checkout_free_complete else R.string.checkout_pay,
                        ),
                        enabled = !state.payBusy && (state.isFreeOrder || !state.tossClientKey.isNullOrBlank()),
                        busy = state.payBusy,
                        onClick = if (state.isFreeOrder) onFreeComplete else onPayWithToss,
                        ctaTestTag = if (state.isFreeOrder) "checkout_free_complete" else "checkout_pay",
                        ctaIcon = if (state.isFreeOrder) null else Icons.Outlined.CreditCard,
                    )
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
                            onEditAddress = onEditAddress,
                            onSelectDelivery = onSelectDelivery,
                        )
                        CheckoutStep.Payment -> PaymentStep(
                            state = state,
                            onPointsInput = onPointsInput,
                            onApplyPoints = onApplyPoints,
                            onChangePaymentAddress = onChangePaymentAddress,
                            onRegisterAddress = onRegisterAddress,
                            onEditAddress = onEditAddress,
                        )
                    }
                }
            }
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
    onEditAddress: (Address) -> Unit,
    onSelectDelivery: (String) -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    var emailDialogOpen by remember { mutableStateOf(false) }
    var emailDraft by remember(state.email) { mutableStateOf(state.email) }
    val selected = state.selectedAddress
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp)
            .testTag("checkout_contact"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard(
            icon = { SectionIconChip(Icons.Outlined.Lock) },
            title = stringResource(R.string.checkout_buyer),
            action = {
                CheckoutChangeButton(
                    onClick = {
                        emailDraft = state.email
                        emailDialogOpen = true
                    },
                    modifier = Modifier.testTag("checkout_email_change"),
                )
            },
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.checkout_email),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = state.email,
                            modifier = Modifier.testTag("checkout_email"),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
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
            SectionCard(
                icon = { SectionIconChip(Icons.Outlined.LocalShipping) },
                title = stringResource(R.string.checkout_shipping),
            ) {
                if (state.payBusy && state.deliveryOptions.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                DeliveryMethodOptions(
                    options = state.deliveryOptions,
                    selectedId = state.selectedDeliveryMethodId,
                    onSelect = onSelectDelivery,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
    if (emailDialogOpen) {
        AlertDialog(
            onDismissRequest = { emailDialogOpen = false },
            title = { Text(stringResource(R.string.checkout_email_change_title)) },
            text = {
                OutlinedTextField(
                    value = emailDraft,
                    onValueChange = { emailDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.checkout_email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEmailChange(emailDraft)
                        emailDialogOpen = false
                    },
                ) {
                    Text(stringResource(R.string.checkout_email_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { emailDialogOpen = false }) {
                    Text(stringResource(R.string.checkout_cancel))
                }
            },
        )
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
            onEdit = { address ->
                pickerOpen = false
                onEditAddress(address)
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
    onEdit: (Address) -> Unit,
    onRegisterNew: () -> Unit,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.8f
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .testTag("checkout_address_picker"),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.checkout_select_address),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("checkout_address_picker_close"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.checkout_close),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (addresses.isEmpty()) {
                Text(
                    text = stringResource(R.string.checkout_no_address),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight - 160.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(addresses, key = { it.id }) { address ->
                        AddressPickerRow(
                            address = address,
                            selected = address.id == selectedId,
                            onSelect = { onSelect(address) },
                            onEdit = { onEdit(address) },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            FilledTonalButton(
                onClick = onRegisterNew,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("checkout_address_picker_add"),
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.checkout_new_address))
            }
        }
    }
}

@Composable
private fun AddressPickerRow(
    address: Address,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.primary else colors.outlineVariant,
                shape = shape,
            )
            .clip(shape)
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
            ),
        shape = shape,
        color = if (selected) colors.primaryContainer else colors.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.size(20.dp),
            )
            AddressBlock(
                address = address,
                modifier = Modifier.weight(1f),
                showPhone = true,
                compact = true,
                showDefaultBadge = true,
            )
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("checkout_address_edit_${address.id}"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.checkout_edit_address_cd),
                )
            }
        }
    }
}

@Composable
private fun ShippingAddressCard(
    address: Address?,
    onChange: () -> Unit,
    onRegister: () -> Unit,
) {
    SectionCard(
        modifier = Modifier.testTag("checkout_shipping_card"),
        icon = { SectionIconChip(Icons.Outlined.LocationOn) },
        title = stringResource(R.string.checkout_summary_ship_to),
        action = if (address != null) {
            {
                CheckoutChangeButton(
                    onClick = onChange,
                    modifier = Modifier.testTag("checkout_address_change"),
                )
            }
        } else {
            null
        },
    ) {
        if (address == null) {
            Text(
                text = stringResource(R.string.checkout_no_address),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_register_address"),
            ) {
                Text(stringResource(R.string.checkout_register_address))
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onChange),
            ) {
                AddressBlock(
                    address = address,
                    showPhone = true,
                    showDefaultBadge = true,
                )
            }
        }
    }
}

@Composable
private fun CheckoutAddressCreateScreen(
    busy: Boolean,
    error: String?,
    initialDraft: AddressDraft,
    isEditing: Boolean,
    onBack: () -> Unit,
    onSubmit: (AddressDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf(initialDraft) }
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
                title = stringResource(
                    if (isEditing) R.string.checkout_edit_address else R.string.checkout_register_address_title,
                ),
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
            Text(
                stringResource(
                    if (isEditing) R.string.checkout_edit_address else R.string.checkout_register_address,
                ),
            )
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
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val selected = option.id == selectedId
            val shape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) colors.primary else colors.outlineVariant,
                        shape = shape,
                    )
                    .clip(shape)
                    .background(if (selected) colors.primaryContainer else Color.Transparent)
                    .selectable(
                        selected = selected,
                        onClick = { onSelect(option.id) },
                        role = Role.RadioButton,
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .testTag("checkout_delivery_${option.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceContainerLow),
                    contentAlignment = Alignment.Center,
                ) {
                    val initial = option.name.firstOrNull()?.uppercaseChar()?.toString() ?: "·"
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        option.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                    )
                    val minDays = option.minDeliveryDays
                    val maxDays = option.maxDeliveryDays
                    if (minDays != null && maxDays != null) {
                        Text(
                            stringResource(R.string.checkout_eta, minDays, maxDays),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    option.price?.format().orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun PaymentStep(
    state: CheckoutUiState,
    onPointsInput: (String) -> Unit,
    onApplyPoints: () -> Unit,
    onChangePaymentAddress: (Address) -> Unit,
    onRegisterAddress: () -> Unit,
    onEditAddress: (Address) -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val selectedDelivery = state.deliveryOptions.firstOrNull { it.id == state.selectedDeliveryMethodId }
    val address = state.selectedAddress ?: state.session?.shippingAddress
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp)
            .testTag("checkout_payment"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (state.session?.isShippingRequired != false) {
            ShippingInfoCard(
                address = address,
                draft = state.shippingDraft,
                delivery = selectedDelivery,
                shippingPrice = selectedDelivery?.price ?: state.session?.shipping,
                onChangeAddress = { pickerOpen = true },
            )
        }
        if (state.showPointsSection) {
            PointsCard(
                state = state,
                onPointsInput = onPointsInput,
                onApplyPoints = onApplyPoints,
            )
        }
        OrderSummaryCard(state = state)
        Spacer(modifier = Modifier.height(4.dp))
    }
    if (pickerOpen) {
        AddressPickerSheet(
            addresses = state.savedAddresses,
            selectedId = state.selectedAddressId,
            onDismiss = { pickerOpen = false },
            onSelect = { selected ->
                onChangePaymentAddress(selected)
                pickerOpen = false
            },
            onEdit = { address ->
                pickerOpen = false
                onEditAddress(address)
            },
            onRegisterNew = {
                pickerOpen = false
                onRegisterAddress()
            },
        )
    }
}

@Composable
private fun SubLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ShippingInfoCard(
    address: Address?,
    draft: AddressDraft,
    delivery: DeliveryOption?,
    shippingPrice: Money?,
    onChangeAddress: () -> Unit,
) {
    val displayAddress = address
    val draftLines = draft.displayLines()
    SectionCard(
        icon = { SectionIconChip(Icons.Outlined.LocationOn) },
        title = stringResource(R.string.checkout_contact),
        action = {
            CheckoutChangeButton(
                onClick = onChangeAddress,
                modifier = Modifier.testTag("checkout_payment_address_change"),
            )
        },
    ) {
        SubLabel(stringResource(R.string.checkout_sub_ship_to))
        Spacer(modifier = Modifier.height(6.dp))
        if (displayAddress != null) {
            AddressBlock(address = displayAddress, showPhone = true, showDefaultBadge = false)
        } else {
            Text(
                text = draftLines.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (draftLines.street.isNotBlank()) {
                Text(
                    text = draftLines.street,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (draftLines.locality.isNotBlank()) {
                Text(
                    text = draftLines.locality,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 14.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        SubLabel(stringResource(R.string.checkout_sub_method))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                val initial = delivery?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "·"
                if (delivery == null) {
                    Icon(
                        imageVector = Icons.Outlined.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = delivery?.name ?: stringResource(R.string.checkout_shipping),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val minDays = delivery?.minDeliveryDays
                val maxDays = delivery?.maxDeliveryDays
                if (minDays != null && maxDays != null) {
                    Text(
                        text = stringResource(R.string.checkout_eta, minDays, maxDays),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = shippingPrice?.format().orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun OrderSummaryCard(state: CheckoutUiState) {
    val colors = MaterialTheme.colorScheme
    SectionCard(
        icon = { SectionIconChip(Icons.Outlined.ShoppingBag) },
        title = stringResource(R.string.checkout_summary),
    ) {
        state.session?.lines?.forEach { line ->
            OrderLineRow(line)
            Spacer(modifier = Modifier.height(10.dp))
        }
        HorizontalDivider(thickness = 1.dp, color = colors.outlineVariant)
        Spacer(modifier = Modifier.height(12.dp))
        AmountRow(
            label = stringResource(R.string.checkout_product_subtotal),
            value = state.session?.subtotal?.format().orEmpty(),
        )
        AmountRow(
            label = stringResource(R.string.cart_shipping),
            value = state.session?.shipping?.format().orEmpty(),
        )
        val discount = state.session?.discount
        if (discount != null && discount.amount > 0) {
            AmountRow(
                label = stringResource(R.string.checkout_discount),
                value = "-${discount.format()}",
                valueColor = colors.error,
            )
        }
        if (state.pointsApplied > 0) {
            AmountRow(
                label = stringResource(R.string.checkout_points_use),
                value = "-${Money(state.pointsApplied, state.currency).format()}",
                valueColor = colors.error,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(thickness = 1.dp, color = colors.outlineVariant)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.cart_total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
            Text(
                text = state.payAmount?.let { Money(it, state.currency).format() }
                    ?: state.session?.total?.format().orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun OrderLineRow(line: CartLine) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = line.thumbnailUrl,
            contentDescription = line.productName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = line.productName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.checkout_line_qty, line.quantity),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = line.totalPrice?.format().orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AmountRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
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
            color = valueColor,
        )
    }
}

@Composable
private fun PointsCard(
    state: CheckoutUiState,
    onPointsInput: (String) -> Unit,
    onApplyPoints: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SectionCard(
        icon = {
            SectionIconChip(
                imageVector = Icons.Outlined.ShoppingBag,
                containerColor = colors.onPrimary.copy(alpha = 0.7f),
                contentColor = colors.onSecondaryContainer,
            )
        },
        title = stringResource(R.string.checkout_points),
        tonal = false,
        action = {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = colors.onPrimary.copy(alpha = 0.75f),
            ) {
                Text(
                    text = stringResource(
                        R.string.checkout_points_balance_chip,
                        formatGroupedAmount(state.pointsBalance),
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSecondaryContainer,
                )
            }
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.pointsInput,
                onValueChange = onPointsInput,
                modifier = Modifier
                    .weight(1f)
                    .testTag("checkout_points_input"),
                label = { Text(stringResource(R.string.checkout_points_input_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = MaterialTheme.shapes.small,
            )
            Button(
                onClick = onApplyPoints,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("checkout_points_apply"),
                shape = CircleShape,
            ) {
                Text(stringResource(R.string.checkout_points_apply))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.checkout_points_helper),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        if (state.pointsClampNotice) {
            Text(
                text = stringResource(R.string.checkout_points_clamped),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}