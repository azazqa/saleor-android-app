package com.bdf.saleor.ui.account

import com.bdf.saleor.data.FakeAccountRepository
import com.bdf.saleor.data.FakeAuthRepository
import com.bdf.saleor.data.model.AddressDraft
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
class AddressViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun createAddress_addsToRepository() = runTest(mainDispatcherRule.dispatcher) {
        val accountRepository = FakeAccountRepository()
        val authRepository = FakeAuthRepository(initialState = AuthState.LoggedIn("user@test.com"))
        val viewModel = AddressViewModel(authRepository, accountRepository)
        advanceUntilIdle()

        viewModel.createAddress(
            AddressDraft(
                firstName = "민성",
                lastName = "김",
                streetAddress1 = "테헤란로 1",
                city = "서울",
                postalCode = "06236",
            ),
        )
        advanceUntilIdle()

        assertEquals(1, accountRepository.addresses.size)
        assertEquals("테헤란로 1", accountRepository.addresses.first().streetAddress1)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun deleteAddress_removesFromRepository() = runTest(mainDispatcherRule.dispatcher) {
        val accountRepository = FakeAccountRepository()
        accountRepository.createAddress(
            AddressDraft(firstName = "A", lastName = "B", streetAddress1 = "1", city = "Seoul", postalCode = "1"),
        )
        val viewModel = AddressViewModel(
            FakeAuthRepository(initialState = AuthState.LoggedIn("user@test.com")),
            accountRepository,
        )
        advanceUntilIdle()

        viewModel.deleteAddress(accountRepository.addresses.first().id)
        advanceUntilIdle()

        assertTrue(accountRepository.addresses.isEmpty())
    }
}
