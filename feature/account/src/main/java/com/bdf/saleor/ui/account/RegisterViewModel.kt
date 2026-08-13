package com.bdf.saleor.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.data.AuthRepository
import com.bdf.saleor.data.model.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun onFirstNameChange(value: String) = _uiState.update { it.copy(firstName = value) }
    fun onLastNameChange(value: String) = _uiState.update { it.copy(lastName = value) }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.length < 8 || state.isSubmitting) {
            if (state.password.isNotBlank() && state.password.length < 8) {
                _uiState.update { it.copy(error = "비밀번호는 8자 이상이어야 합니다") }
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, successMessage = null) }
            val result = runCatching {
                authRepository.register(
                    email = state.email.trim(),
                    password = state.password,
                    firstName = state.firstName,
                    lastName = state.lastName,
                )
            }.getOrElse { AuthResult(false, it.message) }
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    error = if (result.success) null else result.message,
                    successMessage = if (result.success) result.message else null,
                )
            }
        }
    }
}
