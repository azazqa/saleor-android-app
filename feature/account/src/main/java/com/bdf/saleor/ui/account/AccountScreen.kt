package com.bdf.saleor.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.data.model.AuthState
import com.bdf.saleor.feature.account.R
import com.bdf.saleor.ui.components.ErrorState
import com.bdf.saleor.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountRoute(
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onOrdersClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    when (authState) {
        AuthState.Unknown -> LoadingState(modifier.testTag("account_loading"))
        AuthState.LoggedOut -> LoginScreen(
            onRegisterClick = onRegisterClick,
            onForgotPasswordClick = onForgotPasswordClick,
            modifier = modifier,
        )
        is AuthState.LoggedIn -> AccountScreen(
            viewModel = viewModel,
            onOrdersClick = onOrdersClick,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onOrdersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("account_screen"),
    ) {
        TopAppBar(title = { Text(stringResource(R.string.account_title)) })
        when {
            state.isLoading -> LoadingState()
            state.error != null && state.profile == null -> ErrorState(
                message = state.error ?: stringResource(DesignR.string.error_generic),
                onRetry = viewModel::refresh,
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(
                    text = state.profile?.email.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag("account_email"),
                )
                if (!state.profile?.dateJoined.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stringResource(R.string.account_member_since)}: ${state.profile?.dateJoined}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(R.string.account_edit_name), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = viewModel::onFirstNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.register_first_name)) },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = viewModel::onLastNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.register_last_name)) },
                    singleLine = true,
                )
                if (!state.nameMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.nameMessage.orEmpty())
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = viewModel::saveName, enabled = !state.isSavingName) {
                    Text(stringResource(R.string.account_save))
                }

                Spacer(modifier = Modifier.height(28.dp))
                Text(stringResource(R.string.account_change_password), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.oldPassword,
                    onValueChange = viewModel::onOldPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.account_old_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.account_new_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.account_confirm_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                if (!state.passwordMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.passwordMessage.orEmpty())
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = viewModel::changePassword, enabled = !state.isSavingPassword) {
                    Text(stringResource(R.string.account_change_password))
                }

                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = onOrdersClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_orders"),
                ) {
                    Text(stringResource(R.string.account_orders))
                }
                Spacer(modifier = Modifier.height(12.dp))
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
        }
    }
}
