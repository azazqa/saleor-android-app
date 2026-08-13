package com.bdf.saleor.ui.account

import com.bdf.saleor.data.FakeAuthRepository
import com.bdf.saleor.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun submit_success_clearsError() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(repository)
        viewModel.onEmailChange("user@test.com")
        viewModel.onPasswordChange("password")
        viewModel.submit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals("user@test.com", repository.lastLoginEmail)
    }

    @Test
    fun submit_failure_setsError() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeAuthRepository().apply { shouldFailLogin = true }
        val viewModel = LoginViewModel(repository)
        viewModel.onEmailChange("user@test.com")
        viewModel.onPasswordChange("bad")
        viewModel.submit()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.error?.contains("login failed") == true)
    }
}
