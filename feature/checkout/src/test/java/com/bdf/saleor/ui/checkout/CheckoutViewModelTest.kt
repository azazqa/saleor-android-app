package com.bdf.saleor.ui.checkout

import com.bdf.saleor.data.FakeAccountRepository
import com.bdf.saleor.data.FakeAuthRepository
import com.bdf.saleor.data.FakeCheckoutRepository
import com.bdf.saleor.data.model.Address
import com.bdf.saleor.data.model.AddressDraft
import com.bdf.saleor.data.model.AuthState
import com.bdf.saleor.data.model.CheckoutAuthorizeStatus
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
    fun continueFromContact_withShipping_goesToPayment() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository()
        val viewModel = viewModel(checkout)
        advanceUntilIdle()
        viewModel.onEmailChange("guest@test.com")
        viewModel.createShippingAddress(validDraft())
        advanceUntilIdle()

        assertEquals(CheckoutStep.Contact, viewModel.uiState.value.step)
        assertEquals(1, viewModel.uiState.value.deliveryOptions.size)
        assertEquals("d1", viewModel.uiState.value.selectedDeliveryMethodId)

        viewModel.continueFromContact()
        advanceUntilIdle()

        assertEquals(CheckoutStep.Payment, viewModel.uiState.value.step)
        assertEquals("d1", checkout.lastDeliveryMethodId)
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
        assertEquals("5,000", viewModel.uiState.value.pointsInput)

        viewModel.applyPoints()
        advanceUntilIdle()

        assertEquals(PaymentGateways.POINTS, checkout.lastTransactionGateway)
        assertEquals(5_000.0, checkout.lastTransactionAmount ?: -1.0, 0.0)
        assertEquals(5_000.0, viewModel.uiState.value.pointsApplied, 0.0)
        assertEquals("5,000", viewModel.uiState.value.pointsInput)
    }

    @Test
    fun applyPoints_belowMinUnit_showsError() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository()
        val auth = FakeAuthRepository(AuthState.LoggedIn("user@test.com"))
        val viewModel = viewModel(checkout, auth)
        advanceUntilIdle()
        viewModel.onPointsInputChange("50")

        viewModel.applyPoints()
        advanceUntilIdle()

        assertEquals(null, checkout.lastTransactionGateway)
        assertTrue(viewModel.uiState.value.error?.contains("100") == true)
        assertEquals(0.0, viewModel.uiState.value.pointsApplied, 0.0)
    }

    @Test
    fun applyPromoCode_updatesDiscountAndTotal() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository()
        val viewModel = viewModel(checkout)
        advanceUntilIdle()
        viewModel.onPromoCodeInputChange("SAVE10")

        viewModel.applyPromoCode()
        advanceUntilIdle()

        assertEquals("SAVE10", checkout.lastPromoCode)
        assertEquals("SAVE10", viewModel.uiState.value.session?.voucherCode)
        assertEquals(1_000.0, viewModel.uiState.value.session?.discount?.amount ?: -1.0, 0.0)
        assertEquals(9_000.0, viewModel.uiState.value.session?.total?.amount ?: -1.0, 0.0)
        assertEquals("", viewModel.uiState.value.promoCodeInput)
    }

    @Test
    fun applyPromoCode_blank_showsError() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository()
        val viewModel = viewModel(checkout)
        advanceUntilIdle()

        viewModel.applyPromoCode()
        advanceUntilIdle()

        assertEquals(null, checkout.lastPromoCode)
        assertTrue(viewModel.uiState.value.error?.contains("할인 코드") == true)
    }

    @Test
    fun applyPromoCode_invalid_showsError() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository()
        val viewModel = viewModel(checkout)
        advanceUntilIdle()
        viewModel.onPromoCodeInputChange("INVALID")

        viewModel.applyPromoCode()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.error?.contains("유효하지 않은") == true)
        assertEquals(null, viewModel.uiState.value.session?.voucherCode)
    }

    @Test
    fun removePromoCode_clearsDiscount() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository()
        val viewModel = viewModel(checkout)
        advanceUntilIdle()
        viewModel.onPromoCodeInputChange("SAVE10")
        viewModel.applyPromoCode()
        advanceUntilIdle()

        viewModel.removePromoCode()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.session?.voucherCode)
        assertEquals(null, viewModel.uiState.value.session?.discount)
        assertEquals(10_000.0, viewModel.uiState.value.session?.total?.amount ?: -1.0, 0.0)
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
    fun prepareTossPayment_recalculatesAmountFromCheckout() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository()
        val viewModel = viewModel(checkout)
        advanceUntilIdle()
        viewModel.onShippingDraftChange(validDraft())

        val result = viewModel.prepareTossPayment()

        assertTrue(result.isSuccess)
        assertEquals(10_000.0, result.getOrThrow().amount, 0.0)
        assertEquals(10_000.0, checkout.lastTransactionAmount)
        assertEquals(10_000.0, viewModel.uiState.value.payAmount ?: -1.0, 0.0)
        assertFalse(viewModel.uiState.value.isFreeOrder)
    }

    @Test
    fun payAmount_pendingPartialZeroBalance_usesOrderTotal() {
        val state = CheckoutUiState(
            session = FakeCheckoutRepository.sampleSession(total = 122_000.0).copy(
                authorizeStatus = CheckoutAuthorizeStatus.PARTIAL,
                totalBalance = Money(0.0, "KRW"),
            ),
        )
        assertEquals(122_000.0, state.payAmount ?: -1.0, 0.0)
        assertFalse(state.isFreeOrder)
    }

    @Test
    fun payAmount_pendingFullZeroBalance_usesOrderTotal() {
        val state = CheckoutUiState(
            session = FakeCheckoutRepository.sampleSession(total = 122_000.0).copy(
                authorizeStatus = CheckoutAuthorizeStatus.FULL,
                totalBalance = Money(0.0, "KRW"),
            ),
        )
        assertEquals(122_000.0, state.payAmount ?: -1.0, 0.0)
        assertFalse(state.isFreeOrder)
    }

    @Test
    fun payAmount_zeroListedTotal_fallsBackToLineTotals() {
        val sample = FakeCheckoutRepository.sampleSession(total = 0.0)
        val state = CheckoutUiState(
            session = sample.copy(
                total = Money(0.0, "KRW"),
                totalBalance = Money(0.0, "KRW"),
                authorizeStatus = CheckoutAuthorizeStatus.PARTIAL,
                subtotal = Money(10_000.0, "KRW"),
                shipping = Money(3_000.0, "KRW"),
            ),
        )
        assertEquals(13_000.0, state.payAmount ?: -1.0, 0.0)
    }

    @Test
    fun payAmount_pointsCoverOrder_isFree() {
        val state = CheckoutUiState(
            session = FakeCheckoutRepository.sampleSession(total = 10_000.0),
            pointsApplied = 10_000.0,
        )
        assertEquals(0.0, state.payAmount ?: -1.0, 0.0)
        assertTrue(state.isFreeOrder)
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
    fun changePaymentShippingAddress_staysOnPaymentAndUpdatesSession() = runTest(mainDispatcherRule.dispatcher) {
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
        val extra = existing.copy(
            id = "addr-2",
            firstName = "홍",
            streetAddress1 = "테헤란로 1",
            isDefaultShipping = false,
        )
        val auth = FakeAuthRepository(AuthState.LoggedIn("user@test.com"))
        auth.profile = auth.profile.copy(
            addresses = listOf(existing, extra),
            defaultShippingAddressId = existing.id,
        )
        val checkout = FakeCheckoutRepository()
        val viewModel = viewModel(checkout, auth)
        advanceUntilIdle()
        viewModel.onEmailChange("user@test.com")
        viewModel.continueFromContact()
        advanceUntilIdle()

        assertEquals(CheckoutStep.Payment, viewModel.uiState.value.step)

        viewModel.changePaymentShippingAddress(extra)
        advanceUntilIdle()

        assertEquals(CheckoutStep.Payment, viewModel.uiState.value.step)
        assertEquals("addr-2", viewModel.uiState.value.selectedAddressId)
        assertEquals("홍", checkout.session.shippingAddress?.firstName)
        assertEquals("테헤란로 1", checkout.session.shippingAddress?.streetAddress1)
        assertEquals("d1", viewModel.uiState.value.selectedDeliveryMethodId)
        assertEquals("d1", checkout.lastDeliveryMethodId)
    }

    @Test
    fun createShippingAddress_fromPayment_persistsAndStaysOnPayment() = runTest(mainDispatcherRule.dispatcher) {
        val checkout = FakeCheckoutRepository()
        val viewModel = viewModel(checkout)
        advanceUntilIdle()
        viewModel.onEmailChange("guest@test.com")
        viewModel.createShippingAddress(validDraft())
        advanceUntilIdle()
        viewModel.continueFromContact()
        advanceUntilIdle()

        assertEquals(CheckoutStep.Payment, viewModel.uiState.value.step)

        viewModel.createShippingAddress(
            validDraft().copy(firstName = "새주소", streetAddress1 = "강남대로 10"),
        )
        advanceUntilIdle()

        assertEquals(CheckoutStep.Payment, viewModel.uiState.value.step)
        assertEquals("새주소", viewModel.uiState.value.selectedAddress?.firstName)
        assertEquals("새주소", checkout.session.shippingAddress?.firstName)
        assertEquals("강남대로 10", checkout.session.shippingAddress?.streetAddress1)
        assertFalse(viewModel.uiState.value.showAddressForm)
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

    @Test
    fun goBack_fromPayment_returnsToContact() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEmailChange("guest@test.com")
        viewModel.createShippingAddress(validDraft())
        advanceUntilIdle()
        viewModel.continueFromContact()
        advanceUntilIdle()

        assertEquals(CheckoutStep.Payment, viewModel.uiState.value.step)

        viewModel.goBack()

        assertEquals(CheckoutStep.Contact, viewModel.uiState.value.step)
    }

    @Test
    fun updateShippingAddress_replacesSelectedAndClosesForm() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.createShippingAddress(validDraft())
        advanceUntilIdle()

        val selected = viewModel.uiState.value.selectedAddress
        assertEquals("홍", selected?.firstName)
        viewModel.openAddressForm(selected)
        assertTrue(viewModel.uiState.value.showAddressForm)

        viewModel.updateShippingAddress(
            validDraft().copy(firstName = "수정", streetAddress1 = "새주소로 2"),
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showAddressForm)
        assertEquals("수정", viewModel.uiState.value.selectedAddress?.firstName)
        assertEquals("새주소로 2", viewModel.uiState.value.selectedAddress?.streetAddress1)
        assertEquals(CheckoutStep.Contact, viewModel.uiState.value.step)
    }
}
