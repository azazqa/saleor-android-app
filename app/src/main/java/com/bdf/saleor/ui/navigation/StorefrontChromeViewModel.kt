package com.bdf.saleor.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.data.AuthRepository
import com.bdf.saleor.data.model.AuthState
import com.bdf.saleor.data.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StorefrontChromeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    val authState: StateFlow<AuthState> = authRepository.authState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        authRepository.authState.value,
    )
    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        authRepository.currentUser.value,
    )

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
