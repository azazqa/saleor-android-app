package com.bdf.saleor.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank() || state.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            val result = runCatching {
                authRepository.login(state.email.trim(), state.password)
            }.getOrElse { AuthResultError(it.message) }
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    error = if (result.success) null else result.message,
                )
            }
        }
    }
}

private fun AuthResultError(message: String?) = com.bdf.saleor.data.model.AuthResult(
    success = false,
    message = message ?: "Unknown error",
)
