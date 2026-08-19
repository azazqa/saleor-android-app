package com.bdf.saleor.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.core.data.AuthRepository
import com.bdf.saleor.core.data.CartRepository
import com.bdf.saleor.core.model.AuthState
import com.bdf.saleor.core.model.UserProfile
import com.bdf.saleor.core.network.SaleorCatalogConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StorefrontChromeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cartRepository: CartRepository,
    config: SaleorCatalogConfig,
) : ViewModel() {
    val featuredCollectionSlug: String = config.featuredCollectionSlug
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
    val cartQuantity: StateFlow<Int> = cartRepository.cart.map { it?.lineCount ?: 0 }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        cartRepository.cart.value?.lineCount ?: 0,
    )

    init {
        viewModelScope.launch { cartRepository.refresh() }
    }
}
