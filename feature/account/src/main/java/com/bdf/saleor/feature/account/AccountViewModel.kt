package com.bdf.saleor.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.core.data.AccountRepository
import com.bdf.saleor.core.data.AuthRepository
import com.bdf.saleor.core.data.OrderRepository
import com.bdf.saleor.core.model.AuthState
import com.bdf.saleor.core.model.OrderSummary
import com.bdf.saleor.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val profile: UserProfile? = null,
    val recentOrders: List<OrderSummary> = emptyList(),
    val firstName: String = "",
    val lastName: String = "",
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val nameMessage: String? = null,
    val passwordMessage: String? = null,
    val deleteMessage: String? = null,
    val isSavingName: Boolean = false,
    val isSavingPassword: Boolean = false,
    val isDeleting: Boolean = false,
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {
    val authState: StateFlow<AuthState> = authRepository.authState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        authRepository.authState.value,
    )

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is AuthState.LoggedIn) refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val profile = authRepository.getProfile()
                val orders = runCatching { orderRepository.getOrders(first = 3, after = null) }
                    .getOrNull()
                profile to orders
            }
                .onSuccess { (profile, orders) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profile,
                            recentOrders = orders?.items.orEmpty(),
                            firstName = profile?.firstName.orEmpty(),
                            lastName = profile?.lastName.orEmpty(),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Unknown error")
                    }
                }
        }
    }

    fun onFirstNameChange(value: String) = _uiState.update { it.copy(firstName = value, nameMessage = null) }
    fun onLastNameChange(value: String) = _uiState.update { it.copy(lastName = value, nameMessage = null) }
    fun onOldPasswordChange(value: String) = _uiState.update { it.copy(oldPassword = value, passwordMessage = null) }
    fun onNewPasswordChange(value: String) = _uiState.update { it.copy(newPassword = value, passwordMessage = null) }
    fun onConfirmPasswordChange(value: String) = _uiState.update { it.copy(confirmPassword = value, passwordMessage = null) }

    fun saveName() {
        val state = _uiState.value
        if (state.isSavingName) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingName = true, nameMessage = null) }
            val result = authRepository.updateName(state.firstName, state.lastName)
            _uiState.update {
                it.copy(
                    isSavingName = false,
                    nameMessage = result.exceptionOrNull()?.message ?: if (result.isSuccess) "저장되었습니다" else null,
                )
            }
            if (result.isSuccess) refresh()
        }
    }

    fun changePassword() {
        val state = _uiState.value
        when {
            state.newPassword.length < 8 -> {
                _uiState.update { it.copy(passwordMessage = "비밀번호는 8자 이상이어야 합니다") }
                return
            }
            state.newPassword != state.confirmPassword -> {
                _uiState.update { it.copy(passwordMessage = "새 비밀번호가 일치하지 않습니다") }
                return
            }
            state.isSavingPassword -> return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPassword = true, passwordMessage = null) }
            val result = authRepository.changePassword(state.oldPassword, state.newPassword)
            val ok = result.isSuccess
            _uiState.update {
                it.copy(
                    isSavingPassword = false,
                    passwordMessage = if (ok) "비밀번호가 변경되었습니다" else result.exceptionOrNull()?.message,
                    oldPassword = if (ok) "" else it.oldPassword,
                    newPassword = if (ok) "" else it.newPassword,
                    confirmPassword = if (ok) "" else it.confirmPassword,
                )
            }
        }
    }

    fun requestDeletion() {
        if (_uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteMessage = null) }
            val result = accountRepository.requestAccountDeletion()
            _uiState.update {
                it.copy(
                    isDeleting = false,
                    deleteMessage = result.getOrNull()
                        ?: result.exceptionOrNull()?.message
                        ?: "삭제 확인 메일을 보냈습니다.",
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
