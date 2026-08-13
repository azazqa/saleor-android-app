package com.bdf.saleor.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.data.AuthRepository
import com.bdf.saleor.data.model.AuthState
import com.bdf.saleor.data.model.UserProfile
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
    val firstName: String = "",
    val lastName: String = "",
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val nameMessage: String? = null,
    val passwordMessage: String? = null,
    val isSavingName: Boolean = false,
    val isSavingPassword: Boolean = false,
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
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
            runCatching { authRepository.getProfile() }
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profile,
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

    fun onFirstNameChange(value: String) = _uiState.update { it.copy(firstName = value) }
    fun onLastNameChange(value: String) = _uiState.update { it.copy(lastName = value) }
    fun onOldPasswordChange(value: String) = _uiState.update { it.copy(oldPassword = value) }
    fun onNewPasswordChange(value: String) = _uiState.update { it.copy(newPassword = value) }
    fun onConfirmPasswordChange(value: String) = _uiState.update { it.copy(confirmPassword = value) }

    fun saveName() {
        val state = _uiState.value
        if (state.isSavingName) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingName = true, nameMessage = null) }
            val result = authRepository.updateName(state.firstName, state.lastName)
            _uiState.update {
                it.copy(
                    isSavingName = false,
                    nameMessage = if (result.success) "저장되었습니다" else result.message,
                )
            }
            if (result.success) refresh()
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
            _uiState.update {
                it.copy(
                    isSavingPassword = false,
                    passwordMessage = if (result.success) "비밀번호가 변경되었습니다" else result.message,
                    oldPassword = if (result.success) "" else it.oldPassword,
                    newPassword = if (result.success) "" else it.newPassword,
                    confirmPassword = if (result.success) "" else it.confirmPassword,
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
