package com.bdf.saleor.ui.account

import com.bdf.saleor.data.FakeAccountRepository
import com.bdf.saleor.data.FakeAuthRepository
import com.bdf.saleor.data.FakeCartRepository
import com.bdf.saleor.data.FakeOrderRepository
import com.bdf.saleor.data.model.AuthState
import com.bdf.saleor.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loggedIn_loadsProfileAndRecentOrders() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = AccountViewModel(
            FakeAuthRepository(initialState = AuthState.LoggedIn("user@test.com")),
            FakeAccountRepository(),
            FakeOrderRepository(),
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("user@test.com", viewModel.uiState.value.profile?.email)
        assertEquals("Test", viewModel.uiState.value.firstName)
        assertEquals(1, viewModel.uiState.value.recentOrders.size)
    }

    @Test
    fun logout_releasesLocalCart() = runTest(mainDispatcherRule.dispatcher) {
        val cart = FakeCartRepository()
        cart.replace(FakeCartRepository.sampleCart())
        val repository = FakeAuthRepository(
            initialState = AuthState.LoggedIn("user@test.com"),
            cartRepository = cart,
        )
        val viewModel = AccountViewModel(repository, FakeAccountRepository(), FakeOrderRepository())
        advanceUntilIdle()

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(1, repository.logoutCount)
        assertEquals(1, cart.releaseOnLogoutCount)
        assertEquals(null, cart.cart.value)
        assertTrue(repository.authState.value is AuthState.LoggedOut)
    }

    @Test
    fun requestDeletion_setsMessage() = runTest(mainDispatcherRule.dispatcher) {
        val accountRepository = FakeAccountRepository()
        val viewModel = AccountViewModel(
            FakeAuthRepository(initialState = AuthState.LoggedIn("user@test.com")),
            accountRepository,
            FakeOrderRepository(),
        )
        advanceUntilIdle()

        viewModel.requestDeletion()
        advanceUntilIdle()

        assertTrue(accountRepository.lastDeleteRequested)
        assertTrue(viewModel.uiState.value.deleteMessage?.contains("메일") == true)
    }
}
