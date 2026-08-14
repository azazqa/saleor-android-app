package com.bdf.saleor.ui.checkout

import com.bdf.saleor.data.FakeAccountRepository
import com.bdf.saleor.data.FakeAuthRepository
import com.bdf.saleor.data.FakeCheckoutRepository
import com.bdf.saleor.data.model.Address
import com.bdf.saleor.data.model.AddressDraft
import com.bdf.saleor.data.model.AuthState
import com.bdf.saleor.data.model.Money
import com.bdf.saleor.data.model.PaymentGateways
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
class CheckoutViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun validDraft() = AddressDraft(
        firstName = "홍",
        streetAddress1 = "테헤란로 1",
        streetAddress2 = "101호",
        postalCode = "06236",
        phone = "010-1234-5678",
        countryCode = "KR",
    )

    private fun viewModel(
        checkout: FakeCheckoutRepository = FakeCheckoutRepository(),
        auth: FakeAuthRepository = FakeAuthRepository(),
        account: FakeAccountRepository = FakeAccountRepository(),
    ) = CheckoutViewModel(checkout, auth, account)

    @Test
    fun continueFromContact_withShipping_goesToShippingStep() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEmailChange("guest@test.com")
        viewModel.createShippingAddress(validDraft())
        advanceUntilIdle()

        viewModel.continueFromContact()
        advanceUntilIdle()

        assertEquals(CheckoutStep.Shipping, viewModel.uiState.value.step)
        assertEquals(1, viewModel.uiState.value.deliveryOptions.size)
        assertEquals("d1", viewModel.uiState.value.selectedDeliveryMethodId)
    }

    @Test
    fun continueFromShipping_usesDeliveryId() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository()
        val viewModel = viewModel(checkout)
        advanceUntilIdle()
        viewModel.onEmailChange("guest@test.com")
        viewModel.createShippingAddress(validDraft())
        advanceUntilIdle()
        viewModel.continueFromContact()
        advanceUntilIdle()

        viewModel.continueFromShipping()
        advanceUntilIdle()

        assertEquals("d1", checkout.lastDeliveryMethodId)
        assertEquals(CheckoutStep.Payment, viewModel.uiState.value.step)
    }

    @Test
    fun continueFromContact_skipsShippingWhenNotRequired() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository().apply {
            session = FakeCheckoutRepository.sampleSession(isShippingRequired = false)
        }
        val viewModel = viewModel(checkout)
        advanceUntilIdle()
        viewModel.onEmailChange("guest@test.com")
        viewModel.createShippingAddress(validDraft())
        advanceUntilIdle()

        viewModel.continueFromContact()
        advanceUntilIdle()

        assertEquals(CheckoutStep.Payment, viewModel.uiState.value.step)
    }

    @Test
    fun applyPoints_clampsToBalance() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository()
        val auth = FakeAuthRepository(AuthState.LoggedIn("user@test.com"))
        val viewModel = viewModel(checkout, auth)
        advanceUntilIdle()
        viewModel.onPointsInputChange("99999")

        viewModel.applyPoints()
        advanceUntilIdle()

        assertEquals(PaymentGateways.POINTS, checkout.lastTransactionGateway)
        assertEquals(5_000.0, checkout.lastTransactionAmount ?: -1.0, 0.0)
        assertEquals(5_000.0, viewModel.uiState.value.pointsApplied, 0.0)
        assertTrue(viewModel.uiState.value.pointsClampNotice)
    }

    @Test
    fun completeFreeOrder_setsCompletedOrderId() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository().apply {
            session = FakeCheckoutRepository.sampleSession(total = 0.0)
        }
        val viewModel = viewModel(checkout)
        advanceUntilIdle()
        viewModel.onShippingDraftChange(validDraft())

        viewModel.completeFreeOrder()
        advanceUntilIdle()

        assertEquals("order-1", viewModel.uiState.value.completedOrderId)
        assertEquals("1001", viewModel.uiState.value.completedOrderNumber)
        assertTrue(viewModel.uiState.value.isFreeOrder)
    }

    @Test
    fun prepareTossPayment_rejectsNonKrw() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository().apply {
            session = FakeCheckoutRepository.sampleSession().copy(
                subtotal = Money(10.0, "USD"),
                total = Money(10.0, "USD"),
                totalBalance = Money(10.0, "USD"),
            )
        }
        val viewModel = viewModel(checkout)
        advanceUntilIdle()
        viewModel.onShippingDraftChange(validDraft())

        val result = viewModel.prepareTossPayment()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("KRW") == true)
    }

    @Test
    fun prepareTossPayment_detectsPriceChange() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository()
        val viewModel = viewModel(checkout)
        advanceUntilIdle()
        viewModel.onShippingDraftChange(validDraft())
        checkout.priceOnNextLoad = Money(12_000.0, "KRW")

        val result = viewModel.prepareTossPayment()

        assertTrue(result.isFailure)
        assertEquals("price_change", result.exceptionOrNull()?.message)
        assertFalse(viewModel.uiState.value.priceChangeMessage.isNullOrBlank())
    }

    @Test
    fun finishTossPayment_completesOrder() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository()
        val viewModel = viewModel(checkout)
        advanceUntilIdle()

        viewModel.finishTossPayment("tx-toss", "pay_key", "toss-order-1", 10_000.0)
        advanceUntilIdle()

        assertEquals(listOf("tx-toss"), checkout.processedTransactions)
        assertEquals("order-1", viewModel.uiState.value.completedOrderId)
        assertEquals("1001", viewModel.uiState.value.completedOrderNumber)
    }

    @Test
    fun onPaymentFailed_keepsPaymentStepAndShowsError() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository().apply {
            session = FakeCheckoutRepository.sampleSession(isShippingRequired = false)
        }
        val viewModel = viewModel(checkout)
        advanceUntilIdle()
        viewModel.onEmailChange("guest@test.com")
        viewModel.createShippingAddress(validDraft())
        advanceUntilIdle()
        viewModel.continueFromContact()
        advanceUntilIdle()

        viewModel.onPaymentFailed("cancelled")
        advanceUntilIdle()

        assertEquals(CheckoutStep.Payment, viewModel.uiState.value.step)
        assertEquals("cancelled", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.confirming)
    }

    @Test
    fun refresh_loggedIn_loadsDefaultShippingAddress() = runTest(mainDispatcherRule.dispatcher) {
        val address = Address(
            id = "addr-1",
            firstName = "김민성",
            lastName = "",
            companyName = "",
            streetAddress1 = "서울 성동구",
            streetAddress2 = "",
            city = "",
            cityArea = "",
            postalCode = "04780",
            countryCode = "KR",
            countryName = "South Korea",
            countryArea = "",
            phone = "010-0000-0000",
            isDefaultShipping = true,
            isDefaultBilling = false,
        )
        val auth = FakeAuthRepository(AuthState.LoggedIn("user@test.com"))
        auth.profile = auth.profile.copy(
            addresses = listOf(address),
            defaultShippingAddressId = address.id,
        )
        val viewModel = viewModel(auth = auth)
        advanceUntilIdle()

        assertEquals("addr-1", viewModel.uiState.value.selectedAddressId)
        assertEquals("김민성", viewModel.uiState.value.selectedAddress?.recipientName)
        assertEquals("서울 성동구", viewModel.uiState.value.shippingDraft.streetAddress1)
    }

    @Test
    fun createFirstAddress_setsDefaultShipping() = runTest(mainDispatcherRule.dispatcher) {
        val account = FakeAccountRepository()
        val auth = FakeAuthRepository(AuthState.LoggedIn("user@test.com"))
        val viewModel = viewModel(auth = auth, account = account)
        advanceUntilIdle()

        viewModel.createShippingAddress(validDraft())
        advanceUntilIdle()

        assertTrue(account.addresses.first().isDefaultShipping)
        assertFalse(viewModel.uiState.value.showAddressForm)
        assertEquals("홍", viewModel.uiState.value.selectedAddress?.firstName)
        assertEquals("테헤란로 1", viewModel.uiState.value.shippingDraft.streetAddress1)
        assertTrue(viewModel.uiState.value.canContinueFromContact)
    }

    @Test
    fun createAdditionalAddress_doesNotSetDefaultShipping() = runTest(mainDispatcherRule.dispatcher) {
        val existing = Address(
            id = "addr-1",
            firstName = "김민성",
            lastName = "",
            companyName = "",
            streetAddress1 = "서울 성동구",
            streetAddress2 = "101호",
            city = "",
            cityArea = "",
            postalCode = "04780",
            countryCode = "KR",
            countryName = "South Korea",
            countryArea = "",
            phone = "010-0000-0000",
            isDefaultShipping = true,
            isDefaultBilling = false,
        )
        val account = FakeAccountRepository().apply { addresses += existing }
        val auth = FakeAuthRepository(AuthState.LoggedIn("user@test.com"))
        auth.profile = auth.profile.copy(
            addresses = listOf(existing),
            defaultShippingAddressId = existing.id,
        )
        val viewModel = viewModel(auth = auth, account = account)
        advanceUntilIdle()

        viewModel.createShippingAddress(validDraft())
        advanceUntilIdle()

        assertTrue(account.addresses.first().isDefaultShipping)
        assertFalse(account.addresses.last().isDefaultShipping)
        assertEquals("홍", viewModel.uiState.value.selectedAddress?.firstName)
        assertFalse(viewModel.uiState.value.selectedAddress?.isDefaultShipping == true)
        assertTrue(viewModel.uiState.value.showUseAsDefaultShipping)
    }

    @Test
    fun completeFreeOrder_whenUseAsDefaultChecked_setsDefaultShipping() = runTest(mainDispatcherRule.dispatcher) {
        val existing = Address(
            id = "addr-1",
            firstName = "김민성",
            lastName = "",
            companyName = "",
            streetAddress1 = "서울 성동구",
            streetAddress2 = "101호",
            city = "",
            cityArea = "",
            postalCode = "04780",
            countryCode = "KR",
            countryName = "South Korea",
            countryArea = "",
            phone = "+82 1012341234",
            isDefaultShipping = true,
            isDefaultBilling = false,
        )
        val extra = existing.copy(id = "addr-2", firstName = "홍", isDefaultShipping = false)
        val account = FakeAccountRepository().apply {
            addresses += existing
            addresses += extra
        }
        val auth = FakeAuthRepository(AuthState.LoggedIn("user@test.com"))
        auth.profile = auth.profile.copy(
            addresses = listOf(existing, extra),
            defaultShippingAddressId = existing.id,
        )
        val checkout = FakeCheckoutRepository().apply {
            session = FakeCheckoutRepository.sampleSession(total = 0.0)
        }
        val viewModel = viewModel(checkout, auth, account)
        advanceUntilIdle()

        viewModel.selectSavedAddress(extra)
        assertTrue(viewModel.uiState.value.showUseAsDefaultShipping)
        viewModel.onUseSelectedAddressAsDefaultChange(true)

        viewModel.completeFreeOrder()
        advanceUntilIdle()

        assertEquals("addr-2", account.lastDefaultShippingId)
        assertTrue(account.addresses.first { it.id == "addr-2" }.isDefaultShipping)
        assertFalse(account.addresses.first { it.id == "addr-1" }.isDefaultShipping)
    }

    @Test
    fun continueFromContact_withoutAddress_staysOnContact() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEmailChange("guest@test.com")

        assertFalse(viewModel.uiState.value.canContinueFromContact)

        viewModel.continueFromContact()
        advanceUntilIdle()

        assertEquals(CheckoutStep.Contact, viewModel.uiState.value.step)
    }
}
