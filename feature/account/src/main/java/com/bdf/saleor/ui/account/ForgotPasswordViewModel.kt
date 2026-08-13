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

data class ForgotPasswordUiState(
    val email: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || state.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, successMessage = null) }
            val result = runCatching {
                authRepository.requestPasswordReset(state.email.trim())
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
