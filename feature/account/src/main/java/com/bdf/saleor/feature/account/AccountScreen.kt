package com.bdf.saleor.feature.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.core.model.AuthState
import com.bdf.saleor.core.model.OrderSummary
import com.bdf.saleor.core.designsystem.components.ErrorState
import com.bdf.saleor.core.designsystem.components.LoadingState

@Composable
fun AccountRoute(
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onOrderClick: (OrderSummary) -> Unit,
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
        is AuthState.LoggedIn -> AccountShell(
            viewModel = viewModel,
            onOrderClick = onOrderClick,
            modifier = modifier,
        )
    }
}

@Composable
fun AccountShell(
    viewModel: AccountViewModel,
    onOrderClick: (OrderSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(AccountTab.Overview) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("account_screen"),
    ) {
        AccountTabRow(
            selected = tab,
            onSelect = { tab = it },
        )
        when {
            state.isLoading && state.profile == null && tab != AccountTab.Orders -> LoadingState()
            state.error != null && state.profile == null && tab == AccountTab.Overview -> ErrorState(
                message = state.error ?: stringResource(DesignR.string.error_generic),
                onRetry = viewModel::refresh,
            )
            else -> when (tab) {
                AccountTab.Overview -> AccountOverviewTab(
                    state = state,
                    onViewPoints = { tab = AccountTab.Points },
                    onViewOrders = { tab = AccountTab.Orders },
                    onManageAddress = { tab = AccountTab.Addresses },
                    onOrderClick = onOrderClick,
                )
                AccountTab.Orders -> OrderListScreen(onOrderClick = onOrderClick)
                AccountTab.Points -> AccountPointsScreen()
                AccountTab.Addresses -> AccountAddressesScreen()
                AccountTab.Settings -> AccountSettingsTab(viewModel = viewModel)
            }
        }
    }
}
