package com.bdf.saleor.ui.account

import com.bdf.saleor.data.FakeAuthRepository
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
    fun loggedIn_loadsProfile() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeAuthRepository(initialState = AuthState.LoggedIn("user@test.com"))
        val viewModel = AccountViewModel(repository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("user@test.com", viewModel.uiState.value.profile?.email)
        assertEquals("Test", viewModel.uiState.value.firstName)
    }

    @Test
    fun logout_delegatesToRepository() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeAuthRepository(initialState = AuthState.LoggedIn("user@test.com"))
        val viewModel = AccountViewModel(repository)
        advanceUntilIdle()

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(1, repository.logoutCount)
        assertTrue(repository.authState.value is AuthState.LoggedOut)
    }
}
