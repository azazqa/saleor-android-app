package com.bdf.saleor.ui.account

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdf.saleor.feature.account.R
import com.bdf.saleor.ui.theme.BrandDestructive
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@Composable
fun AccountSettingsTab(
    viewModel: AccountViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editName by remember { mutableStateOf(false) }
    var changePassword by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val profile = state.profile

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("account_settings_tab"),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        AccountCard {
            Text(stringResource(R.string.login_email), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.MailOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = profile?.email.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("account_email"),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.settings_name), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(profile?.displayName.orEmpty(), style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = { editName = true }) { Text(stringResource(R.string.settings_edit)) }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.settings_password), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("••••••••", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = { changePassword = true }) { Text(stringResource(R.string.settings_change)) }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = stringResource(R.string.settings_member_since, formatJoinedMonth(profile?.dateJoined)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        AccountCard {
            Text(
                text = stringResource(R.string.settings_delete_title),
                style = MaterialTheme.typography.titleMedium,
                color = BrandDestructive,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_delete_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!state.deleteMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.deleteMessage.orEmpty())
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { confirmDelete = true },
                colors = ButtonDefaults.buttonColors(containerColor = BrandDestructive),
                enabled = !state.isDeleting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_delete_title))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = viewModel::logout,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("account_logout"),
        ) {
            Text(stringResource(R.string.account_logout))
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (editName) {
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text(stringResource(R.string.account_edit_name)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.firstName,
                        onValueChange = viewModel::onFirstNameChange,
                        label = { Text(stringResource(R.string.register_first_name)) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.lastName,
                        onValueChange = viewModel::onLastNameChange,
                        label = { Text(stringResource(R.string.register_last_name)) },
                    )
                    if (!state.nameMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.nameMessage.orEmpty())
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveName()
                        editName = false
                    },
                    enabled = !state.isSavingName,
                ) { Text(stringResource(R.string.account_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editName = false }) { Text(stringResource(R.string.settings_delete_cancel)) }
            },
        )
    }

    if (changePassword) {
        AlertDialog(
            onDismissRequest = { changePassword = false },
            title = { Text(stringResource(R.string.account_change_password)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.oldPassword,
                        onValueChange = viewModel::onOldPasswordChange,
                        label = { Text(stringResource(R.string.account_old_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.newPassword,
                        onValueChange = viewModel::onNewPasswordChange,
                        label = { Text(stringResource(R.string.account_new_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        label = { Text(stringResource(R.string.account_confirm_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    if (!state.passwordMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.passwordMessage.orEmpty())
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.changePassword()
                        changePassword = false
                    },
                    enabled = !state.isSavingPassword,
                ) { Text(stringResource(R.string.account_change_password)) }
            },
            dismissButton = {
                TextButton(onClick = { changePassword = false }) { Text(stringResource(R.string.settings_delete_cancel)) }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.settings_delete_title)) },
            text = { Text(stringResource(R.string.settings_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.requestDeletion()
                        confirmDelete = false
                    },
                ) { Text(stringResource(R.string.settings_delete_title)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.settings_delete_cancel)) }
            },
        )
    }
}

private fun formatJoinedMonth(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        val date = Instant.parse(iso).atZone(ZoneId.systemDefault())
        String.format(Locale.KOREA, "%d년 %d월", date.year, date.monthValue)
    }.getOrElse { iso }
}
