package com.bdf.saleor.feature.account

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
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.core.model.Address
import com.bdf.saleor.core.model.AddressDraft
import com.bdf.saleor.core.model.AddressKind
import com.bdf.saleor.feature.account.R
import com.bdf.saleor.core.designsystem.components.ErrorState
import com.bdf.saleor.core.designsystem.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountAddressesScreen(
    modifier: Modifier = Modifier,
    viewModel: AddressViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Address?>(null) }
    var adding by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("account_addresses_screen"),
    ) {
        when {
            state.isLoading -> LoadingState()
            state.error != null && state.addresses.isEmpty() -> ErrorState(
                message = state.error ?: stringResource(DesignR.string.error_generic),
                onRetry = viewModel::refresh,
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.addresses_title),
                            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = stringResource(R.string.addresses_subtitle),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = { adding = true }) {
                        Text(stringResource(R.string.addresses_add))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (state.addresses.isEmpty()) {
                    DashedEmptyBox(
                        message = stringResource(R.string.addresses_empty),
                        action = {
                            OutlinedButton(onClick = { adding = true }) {
                                Text(stringResource(R.string.addresses_add))
                            }
                        },
                    )
                } else {
                    state.addresses.forEach { address ->
                        AccountCard(modifier = Modifier.padding(bottom = 12.dp)) {
                            address.formattedLines().forEach { line ->
                                Text(line, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (address.isDefaultShipping) {
                                Badge { Text(stringResource(R.string.addresses_default_shipping)) }
                            }
                            if (address.isDefaultBilling) {
                                Badge { Text(stringResource(R.string.addresses_default_billing)) }
                            }
                            Row {
                                TextButton(onClick = { editing = address }) {
                                    Text(stringResource(R.string.addresses_edit))
                                }
                                TextButton(onClick = { viewModel.deleteAddress(address.id) }) {
                                    Text(stringResource(R.string.addresses_delete))
                                }
                            }
                            Row {
                                if (!address.isDefaultShipping) {
                                    TextButton(onClick = { viewModel.setDefault(address.id, AddressKind.SHIPPING) }) {
                                        Text(stringResource(R.string.addresses_set_shipping))
                                    }
                                }
                                if (!address.isDefaultBilling) {
                                    TextButton(onClick = { viewModel.setDefault(address.id, AddressKind.BILLING) }) {
                                        Text(stringResource(R.string.addresses_set_billing))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (adding || editing != null) {
        AddressFormSheet(
            address = editing,
            onDismiss = { adding = false; editing = null },
            onSubmit = { draft ->
                val current = editing
                if (current == null) viewModel.createAddress(draft) else viewModel.updateAddress(current.id, draft)
                adding = false
                editing = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressFormSheet(
    address: Address?,
    onDismiss: () -> Unit,
    onSubmit: (AddressDraft) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember(address) {
        mutableStateOf(
            AddressDraft(
                firstName = address?.firstName.orEmpty(),
                lastName = address?.lastName.orEmpty(),
                companyName = address?.companyName.orEmpty(),
                streetAddress1 = address?.streetAddress1.orEmpty(),
                streetAddress2 = address?.streetAddress2.orEmpty(),
                city = address?.city.orEmpty(),
                cityArea = address?.cityArea.orEmpty(),
                postalCode = address?.postalCode.orEmpty(),
                countryCode = address?.countryCode?.ifBlank { "KR" } ?: "KR",
                countryArea = address?.countryArea.orEmpty(),
                phone = address?.displayPhone().orEmpty(),
            ),
        )
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(if (address == null) R.string.addresses_form_add else R.string.addresses_form_edit),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = draft.firstName,
                onValueChange = { draft = draft.copy(firstName = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.register_first_name)) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.lastName,
                onValueChange = { draft = draft.copy(lastName = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.register_last_name)) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.companyName,
                onValueChange = { draft = draft.copy(companyName = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.addresses_company)) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.streetAddress1,
                onValueChange = { draft = draft.copy(streetAddress1 = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.addresses_street)) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.streetAddress2,
                onValueChange = { draft = draft.copy(streetAddress2 = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.addresses_street2)) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.postalCode,
                onValueChange = { draft = draft.copy(postalCode = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.addresses_postal)) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.countryArea,
                onValueChange = { draft = draft.copy(countryArea = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.addresses_country_area)) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.phone,
                onValueChange = { draft = draft.copy(phone = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.addresses_phone)) },
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onSubmit(draft) },
                modifier = Modifier.fillMaxWidth(),
                enabled = draft.firstName.isNotBlank() &&
                    draft.streetAddress1.isNotBlank() &&
                    draft.streetAddress2.isNotBlank() &&
                    draft.postalCode.isNotBlank() &&
                    draft.phone.isNotBlank(),
            ) {
                Text(stringResource(R.string.account_save))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
