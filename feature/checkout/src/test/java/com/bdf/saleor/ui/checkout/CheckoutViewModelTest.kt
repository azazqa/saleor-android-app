package com.bdf.saleor.ui.checkout

import com.bdf.saleor.data.FakeAuthRepository
import com.bdf.saleor.data.FakeCheckoutRepository
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

    @Test
    fun continueFromContact_withShipping_goesToShippingStep() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = CheckoutViewModel(FakeCheckoutRepository(), FakeAuthRepository())
        advanceUntilIdle()
        viewModel.onEmailChange("guest@test.com")
        viewModel.onShippingDraftChange(validDraft())

        viewModel.continueFromContact()
        advanceUntilIdle()

        assertEquals(CheckoutStep.Shipping, viewModel.uiState.value.step)
        assertEquals(1, viewModel.uiState.value.deliveryOptions.size)
        assertEquals("d1", viewModel.uiState.value.selectedDeliveryMethodId)
    }

    @Test
    fun continueFromShipping_usesDeliveryId() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository()
        val viewModel = CheckoutViewModel(checkout, FakeAuthRepository())
        advanceUntilIdle()
        viewModel.onEmailChange("guest@test.com")
        viewModel.onShippingDraftChange(validDraft())
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
        val viewModel = CheckoutViewModel(checkout, FakeAuthRepository())
        advanceUntilIdle()
        viewModel.onEmailChange("guest@test.com")
        viewModel.onShippingDraftChange(validDraft())

        viewModel.continueFromContact()
        advanceUntilIdle()

        assertEquals(CheckoutStep.Payment, viewModel.uiState.value.step)
    }

    @Test
    fun applyPoints_clampsToBalance() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository()
        val auth = FakeAuthRepository(AuthState.LoggedIn("user@test.com"))
        val viewModel = CheckoutViewModel(checkout, auth)
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
        val viewModel = CheckoutViewModel(checkout, FakeAuthRepository())
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
        val viewModel = CheckoutViewModel(checkout, FakeAuthRepository())
        advanceUntilIdle()
        viewModel.onShippingDraftChange(validDraft())

        val result = viewModel.prepareTossPayment()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("KRW") == true)
    }

    @Test
    fun prepareTossPayment_detectsPriceChange() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository()
        val viewModel = CheckoutViewModel(checkout, FakeAuthRepository())
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
        val viewModel = CheckoutViewModel(checkout, FakeAuthRepository())
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
        val viewModel = CheckoutViewModel(checkout, FakeAuthRepository())
        advanceUntilIdle()
        viewModel.onEmailChange("guest@test.com")
        viewModel.onShippingDraftChange(validDraft())
        viewModel.continueFromContact()
        advanceUntilIdle()

        viewModel.onPaymentFailed("cancelled")
        advanceUntilIdle()

        assertEquals(CheckoutStep.Payment, viewModel.uiState.value.step)
        assertEquals("cancelled", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.confirming)
    }
}
