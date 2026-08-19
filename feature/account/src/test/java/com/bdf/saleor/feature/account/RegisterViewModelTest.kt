package com.bdf.saleor.feature.account

import com.bdf.saleor.core.testing.fake.FakeAuthRepository
import com.bdf.saleor.core.testing.MainDispatcherRule
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
class RegisterViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun submit_success_setsSuccessMessage() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = RegisterViewModel(FakeAuthRepository())
        viewModel.onEmailChange("user@test.com")
        viewModel.onPasswordChange("password")
        viewModel.submit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(
            "회원가입이 완료되었습니다. 로그인해 주세요.",
            viewModel.uiState.value.successMessage,
        )
    }

    @Test
    fun submit_failure_setsError() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeAuthRepository().apply { shouldFailRegister = true }
        val viewModel = RegisterViewModel(repository)
        viewModel.onEmailChange("user@test.com")
        viewModel.onPasswordChange("password")
        viewModel.submit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.successMessage)
        assertTrue(viewModel.uiState.value.error?.contains("register failed") == true)
    }
}
