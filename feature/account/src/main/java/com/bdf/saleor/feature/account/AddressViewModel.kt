package com.bdf.saleor.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.core.data.AccountRepository
import com.bdf.saleor.core.data.AuthRepository
import com.bdf.saleor.core.model.Address
import com.bdf.saleor.core.model.AddressDraft
import com.bdf.saleor.core.model.AddressKind
import com.bdf.saleor.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddressUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val profile: UserProfile? = null,
    val addresses: List<Address> = emptyList(),
    val isSaving: Boolean = false,
)

@HiltViewModel
class AddressViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddressUiState())
    val uiState: StateFlow<AddressUiState> = _uiState.asStateFlow()

    init {
        refresh()
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
                            addresses = profile?.addresses.orEmpty(),
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

    fun createAddress(draft: AddressDraft) = mutate {
        accountRepository.createAddress(draft)
    }

    fun updateAddress(id: String, draft: AddressDraft) = mutate {
        accountRepository.updateAddress(id, draft)
    }

    fun deleteAddress(id: String) = mutate {
        accountRepository.deleteAddress(id)
    }

    fun setDefault(id: String, kind: AddressKind) = mutate {
        accountRepository.setDefaultAddress(id, kind)
    }

    private fun mutate(block: suspend () -> Result<String?>) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            val result = runCatching { block() }.fold(
                onSuccess = { it },
                onFailure = { Result.failure(it) },
            )
            _uiState.update {
                it.copy(isSaving = false, message = result.exceptionOrNull()?.message)
            }
            if (result.isSuccess) refresh()
        }
    }
}
