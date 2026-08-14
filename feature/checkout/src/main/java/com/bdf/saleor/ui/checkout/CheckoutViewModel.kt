package com.bdf.saleor.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.data.AccountRepository
import com.bdf.saleor.data.AuthRepository
import com.bdf.saleor.data.CheckoutRepository
import com.bdf.saleor.data.model.Address
import com.bdf.saleor.data.model.AddressDraft
import com.bdf.saleor.data.model.AddressKind
import com.bdf.saleor.data.model.AuthState
import com.bdf.saleor.data.model.CheckoutAuthorizeStatus
import com.bdf.saleor.data.model.CheckoutSession
import com.bdf.saleor.data.model.DeliveryOption
import com.bdf.saleor.data.model.PaymentGateways
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CheckoutStep {
    Contact,
    Shipping,
    Payment,
}

data class TossPaymentRequest(
    val transactionId: String,
    val orderId: String,
    val orderName: String,
    val amount: Double,
    val customerKey: String?,
    val clientKey: String,
)

data class CheckoutUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val step: CheckoutStep = CheckoutStep.Contact,
    val session: CheckoutSession? = null,
    val email: String = "",
    val shippingDraft: AddressDraft = AddressDraft(),
    val billingDraft: AddressDraft = AddressDraft(),
    val sameAsBilling: Boolean = true,
    val savedAddresses: List<Address> = emptyList(),
    val selectedAddressId: String? = null,
    val showAddressForm: Boolean = false,
    val useSelectedAddressAsDefault: Boolean = false,
    val deliveryOptions: List<DeliveryOption> = emptyList(),
    val selectedDeliveryMethodId: String? = null,
    val pointsBalance: Double = 0.0,
    val pointsCurrency: String = "KRW",
    val pointsInput: String = "",
    val pointsApplied: Double = 0.0,
    val pointsClampNotice: Boolean = false,
    val showPointsSection: Boolean = false,
    val tossClientKey: String? = null,
    val payBusy: Boolean = false,
    val confirming: Boolean = false,
    val priceChangeMessage: String? = null,
    val completedOrderId: String? = null,
    val completedOrderNumber: String? = null,
    val loggedIn: Boolean = false,
) {
    val payAmount: Double?
        get() {
            val checkout = session ?: return null
            if (pointsApplied > 0) {
                return max(0.0, (checkout.total?.amount ?: 0.0) - pointsApplied)
            }
            if (checkout.authorizeStatus == CheckoutAuthorizeStatus.PARTIAL) {
                return checkout.totalBalance?.amount?.let { max(0.0, it) }
            }
            return checkout.total?.amount
        }

    val isFreeOrder: Boolean
        get() = payAmount == 0.0

    val currency: String
        get() = session?.total?.currency ?: "KRW"

    val selectedAddress: Address?
        get() = savedAddresses.firstOrNull { it.id == selectedAddressId }
            ?: savedAddresses.firstOrNull { it.isDefaultShipping }
            ?: savedAddresses.firstOrNull()

    val canContinueFromContact: Boolean
        get() = selectedAddress != null && !payBusy

    val showUseAsDefaultShipping: Boolean
        get() = loggedIn && selectedAddress != null && selectedAddress?.isDefaultShipping != true
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val checkoutRepository: CheckoutRepository,
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val loggedIn = authRepository.authState.value is AuthState.LoggedIn
            val user = if (loggedIn) {
                runCatching { authRepository.getProfile() }.getOrNull() ?: authRepository.currentUser.value
            } else {
                authRepository.currentUser.value
            }
            checkoutRepository.loadCheckout()
                .onSuccess { session ->
                    val defaultAddress = user?.defaultShippingAddress
                        ?: user?.addresses?.firstOrNull()
                        ?: session.shippingAddress
                    val draft = defaultAddress?.toDraft() ?: AddressDraft()
                    val pointsBalance = user?.pointsBalance?.amount ?: 0.0
                    val showPoints = loggedIn &&
                        pointsBalance > 0 &&
                        session.availablePaymentGateways.any { it.id == PaymentGateways.POINTS }
                    val maxPoints = min(pointsBalance, session.total?.amount ?: 0.0)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            session = session,
                            email = session.email ?: user?.email.orEmpty(),
                            shippingDraft = draft,
                            billingDraft = session.billingAddress?.toDraft() ?: draft,
                            savedAddresses = user?.addresses.orEmpty(),
                            selectedAddressId = defaultAddress?.id,
                            selectedDeliveryMethodId = session.selectedDeliveryMethodId,
                            pointsBalance = pointsBalance,
                            pointsCurrency = user?.pointsBalance?.currency ?: session.total?.currency ?: "KRW",
                            pointsInput = if (it.pointsInput.isBlank()) maxPoints.toPlainString() else it.pointsInput,
                            showPointsSection = showPoints,
                            loggedIn = loggedIn,
                        )
                    }
                    if (loggedIn) {
                        checkoutRepository.attachCustomer()
                    }
                    if (_uiState.value.step == CheckoutStep.Payment) {
                        loadTossClientKey()
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }

    fun onShippingDraftChange(draft: AddressDraft) = _uiState.update { it.copy(shippingDraft = draft) }

    fun onBillingDraftChange(draft: AddressDraft) = _uiState.update { it.copy(billingDraft = draft) }

    fun onSameAsBillingChange(value: Boolean) = _uiState.update { it.copy(sameAsBilling = value) }

    fun selectSavedAddress(address: Address) {
        _uiState.update {
            it.copy(
                shippingDraft = address.toDraft(),
                selectedAddressId = address.id,
                useSelectedAddressAsDefault = false,
                error = null,
            )
        }
    }

    fun onUseSelectedAddressAsDefaultChange(checked: Boolean) {
        _uiState.update { it.copy(useSelectedAddressAsDefault = checked) }
    }

    fun openAddressForm() = _uiState.update { it.copy(showAddressForm = true, error = null) }

    fun closeAddressForm() = _uiState.update { it.copy(showAddressForm = false) }

    fun reloadCustomerAddresses() {
        if (_uiState.value.showAddressForm) return
        viewModelScope.launch {
            if (authRepository.authState.value !is AuthState.LoggedIn) return@launch
            val user = runCatching { authRepository.getProfile() }.getOrNull() ?: return@launch
            val currentId = _uiState.value.selectedAddressId
            val selected = user.addresses.firstOrNull { it.id == currentId }
                ?: user.defaultShippingAddress
                ?: user.addresses.firstOrNull()
            _uiState.update {
                it.copy(
                    savedAddresses = user.addresses,
                    selectedAddressId = selected?.id,
                    shippingDraft = selected?.toDraft() ?: it.shippingDraft,
                    loggedIn = true,
                )
            }
        }
    }

    fun createShippingAddress(draft: AddressDraft) {
        if (!draft.isValid()) {
            _uiState.update { it.copy(error = "배송지를 확인해 주세요") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(payBusy = true, error = null) }
            val loggedIn = authRepository.authState.value is AuthState.LoggedIn
            val existing = knownShippingAddresses()
            val existingIds = existing.map { it.id }.toSet()
            val isFirstAddress = existing.isEmpty()
            if (loggedIn) {
                val result = accountRepository.createAddress(
                    draft,
                    defaultKind = if (isFirstAddress) AddressKind.SHIPPING else null,
                )
                if (!result.success) {
                    _uiState.update { it.copy(payBusy = false, error = result.message ?: "배송지를 저장하지 못했습니다") }
                    return@launch
                }
                val profile = runCatching { authRepository.getProfile() }.getOrNull()
                val created = profile?.addresses?.firstOrNull { it.id !in existingIds }
                    ?: draft.toAddress(id = "local-${existing.size}", isDefaultShipping = isFirstAddress)
                val addresses = when {
                    profile != null && profile.addresses.any { it.id == created.id } -> profile.addresses
                    profile != null -> profile.addresses + created
                    else -> existing + created
                }
                _uiState.update {
                    it.copy(
                        payBusy = false,
                        showAddressForm = false,
                        savedAddresses = addresses,
                        selectedAddressId = created.id,
                        shippingDraft = created.toDraft(),
                        useSelectedAddressAsDefault = false,
                    )
                }
            } else {
                val created = draft.toAddress(
                    id = if (isFirstAddress) "guest" else "guest-${existing.size}",
                    isDefaultShipping = isFirstAddress,
                )
                _uiState.update {
                    it.copy(
                        payBusy = false,
                        showAddressForm = false,
                        savedAddresses = existing + created,
                        selectedAddressId = created.id,
                        shippingDraft = draft,
                        useSelectedAddressAsDefault = false,
                    )
                }
            }
        }
    }

    fun onPointsInputChange(value: String) = _uiState.update { it.copy(pointsInput = value, pointsClampNotice = false) }

    fun goBack() {
        val state = _uiState.value
        val previous = when (state.step) {
            CheckoutStep.Payment -> if (state.session?.isShippingRequired == false) CheckoutStep.Contact else CheckoutStep.Shipping
            CheckoutStep.Shipping -> CheckoutStep.Contact
            CheckoutStep.Contact -> return
        }
        goToStep(previous)
    }

    fun goToStep(step: CheckoutStep) {
        _uiState.update { it.copy(step = step, error = null) }
        if (step == CheckoutStep.Payment && _uiState.value.tossClientKey.isNullOrBlank()) {
            viewModelScope.launch { loadTossClientKey() }
        }
    }

    fun continueFromContact() {
        val state = _uiState.value
        if (state.selectedAddress == null) {
            return
        }
        if (!state.email.contains("@") || !state.shippingDraft.isValid()) {
            _uiState.update { it.copy(error = "이메일과 배송지를 확인해 주세요") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(payBusy = true, error = null) }
            val emailResult = checkoutRepository.updateEmail(state.email)
            if (emailResult.isFailure) {
                _uiState.update { it.copy(payBusy = false, error = emailResult.exceptionOrNull()?.message) }
                return@launch
            }
            val addressResult = checkoutRepository.updateShippingAddress(state.shippingDraft)
            if (addressResult.isFailure) {
                _uiState.update { it.copy(payBusy = false, error = addressResult.exceptionOrNull()?.message) }
                return@launch
            }
            val session = addressResult.getOrThrow()
            if (!session.isShippingRequired) {
                _uiState.update { it.copy(payBusy = false, session = session, step = CheckoutStep.Payment) }
                loadTossClientKey()
                return@launch
            }
            val optionsResult = checkoutRepository.calculateDeliveryOptions()
            _uiState.update {
                it.copy(
                    payBusy = false,
                    session = session,
                    deliveryOptions = optionsResult.getOrDefault(emptyList()),
                    step = CheckoutStep.Shipping,
                    error = optionsResult.exceptionOrNull()?.message,
                    selectedDeliveryMethodId = it.selectedDeliveryMethodId
                        ?: optionsResult.getOrNull()?.firstOrNull { option -> option.active }?.id,
                )
            }
        }
    }

    fun selectDelivery(deliveryMethodId: String) {
        _uiState.update { it.copy(selectedDeliveryMethodId = deliveryMethodId) }
    }

    fun continueFromShipping() {
        val methodId = _uiState.value.selectedDeliveryMethodId
        if (methodId.isNullOrBlank()) {
            _uiState.update { it.copy(error = "배송 방법을 선택해 주세요") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(payBusy = true, error = null) }
            checkoutRepository.updateDeliveryMethod(methodId)
                .onSuccess { session ->
                    _uiState.update { it.copy(payBusy = false, session = session, step = CheckoutStep.Payment) }
                    loadTossClientKey()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(payBusy = false, error = error.message) }
                }
        }
    }

    fun applyPoints() {
        val state = _uiState.value
        val parsed = state.pointsInput.replace(",", "").toDoubleOrNull()
        if (parsed == null || parsed <= 0) {
            _uiState.update { it.copy(error = "올바른 금액을 입력하세요") }
            return
        }
        val maxApplicable = min(state.pointsBalance, state.session?.total?.amount ?: 0.0)
        val requestAmount = min(parsed, maxApplicable)
        viewModelScope.launch {
            _uiState.update { it.copy(payBusy = true, error = null, pointsClampNotice = false) }
            checkoutRepository.initializeTransaction(PaymentGateways.POINTS, requestAmount)
                .onSuccess { result ->
                    val authorized = result.authorizedAmount ?: requestAmount
                    checkoutRepository.loadCheckout().onSuccess { session ->
                        _uiState.update {
                            it.copy(
                                payBusy = false,
                                session = session,
                                pointsApplied = authorized,
                                pointsInput = authorized.toPlainString(),
                                pointsClampNotice = authorized + 0.01 < parsed,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(payBusy = false, error = error.message) }
                }
        }
    }

    suspend fun prepareTossPayment(): Result<TossPaymentRequest> {
        val displayed = _uiState.value.payAmount
        val currency = _uiState.value.currency
        if (displayed == null) return Result.failure(IllegalStateException("결제 금액을 확인할 수 없습니다"))
        if (!currency.equals("KRW", ignoreCase = true)) {
            return Result.failure(IllegalStateException("Toss 결제는 KRW만 지원합니다"))
        }
        _uiState.update { it.copy(payBusy = true, error = null, priceChangeMessage = null) }
        val billingDraft = _uiState.value.shippingDraft
        val billingResult = checkoutRepository.updateBillingAddress(billingDraft)
        if (billingResult.isFailure) {
            val message = billingResult.exceptionOrNull()?.message
            _uiState.update { it.copy(payBusy = false, error = message) }
            return Result.failure(IllegalStateException(message))
        }
        val live = checkoutRepository.loadCheckout().getOrElse { error ->
            _uiState.update { it.copy(payBusy = false, error = error.message) }
            return Result.failure(error)
        }
        _uiState.update { it.copy(session = live) }
        val liveAmount = _uiState.value.payAmount ?: return Result.failure(IllegalStateException("결제 금액을 확인할 수 없습니다"))
        if (abs(displayed - liveAmount) > 0.01) {
            _uiState.update {
                it.copy(payBusy = false, priceChangeMessage = "결제 금액이 변경되었습니다. 다시 확인해 주세요.")
            }
            return Result.failure(IllegalStateException("price_change"))
        }
        val init = checkoutRepository.initializeTransaction(PaymentGateways.TOSS)
        if (init.isFailure) {
            val message = init.exceptionOrNull()?.message
            _uiState.update { it.copy(payBusy = false, error = message) }
            return Result.failure(IllegalStateException(message))
        }
        val payment = init.getOrThrow()
        val clientKey = _uiState.value.tossClientKey
            ?: checkoutRepository.initializeTossClientKey().getOrElse { error ->
                _uiState.update { it.copy(payBusy = false, error = error.message) }
                return Result.failure(error)
            }
        val request = TossPaymentRequest(
            transactionId = payment.transactionId.orEmpty(),
            orderId = payment.orderId.orEmpty(),
            orderName = payment.orderName.orEmpty(),
            amount = payment.amount ?: liveAmount,
            customerKey = payment.customerKey,
            clientKey = clientKey,
        )
        _uiState.update { it.copy(payBusy = false) }
        return Result.success(request)
    }

    fun completeFreeOrder() {
        viewModelScope.launch {
            _uiState.update { it.copy(confirming = true, error = null) }
            val billingDraft = _uiState.value.shippingDraft
            checkoutRepository.updateBillingAddress(billingDraft)
            checkoutRepository.completeCheckout()
                .onSuccess { order ->
                    applyDefaultShippingIfRequested()
                    _uiState.update {
                        it.copy(
                            confirming = false,
                            completedOrderId = order.id,
                            completedOrderNumber = order.number,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(confirming = false, error = error.message) }
                }
        }
    }

    fun finishTossPayment(transactionId: String, paymentKey: String, orderId: String, amount: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(confirming = true, error = null) }
            checkoutRepository.processTransaction(transactionId, paymentKey, orderId, amount)
                .onFailure { error ->
                    _uiState.update { it.copy(confirming = false, error = error.message) }
                    return@launch
                }
            checkoutRepository.completeCheckout()
                .onSuccess { order ->
                    applyDefaultShippingIfRequested()
                    _uiState.update {
                        it.copy(
                            confirming = false,
                            completedOrderId = order.id,
                            completedOrderNumber = order.number,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(confirming = false, error = error.message) }
                }
        }
    }

    fun onPaymentFailed(message: String) {
        _uiState.update { it.copy(payBusy = false, confirming = false, error = message) }
    }

    private suspend fun loadTossClientKey() {
        checkoutRepository.initializeTossClientKey()
            .onSuccess { key -> _uiState.update { it.copy(tossClientKey = key) } }
            .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
    }

    private suspend fun applyDefaultShippingIfRequested() {
        val state = _uiState.value
        if (!state.loggedIn || !state.useSelectedAddressAsDefault) return
        val addressId = state.selectedAddressId ?: return
        accountRepository.setDefaultAddress(addressId, AddressKind.SHIPPING)
    }

    private suspend fun knownShippingAddresses(): List<Address> {
        val saved = _uiState.value.savedAddresses
        if (authRepository.authState.value !is AuthState.LoggedIn) return saved
        val profileAddresses = runCatching { authRepository.getProfile() }.getOrNull()?.addresses.orEmpty()
        return profileAddresses.ifEmpty { saved }
    }

    private fun Address.toDraft() = AddressDraft(
        firstName = firstName,
        lastName = lastName,
        companyName = companyName,
        streetAddress1 = streetAddress1,
        streetAddress2 = streetAddress2,
        city = city,
        cityArea = cityArea,
        postalCode = postalCode,
        countryCode = countryCode.ifBlank { "KR" },
        countryArea = countryArea,
        phone = displayPhone(),
    )

    private fun AddressDraft.toAddress(id: String, isDefaultShipping: Boolean = false) = Address(
        id = id,
        firstName = firstName,
        lastName = lastName.ifBlank { firstName },
        companyName = companyName,
        streetAddress1 = streetAddress1,
        streetAddress2 = streetAddress2,
        city = city,
        cityArea = cityArea,
        postalCode = postalCode,
        countryCode = countryCode.ifBlank { "KR" },
        countryName = "South Korea",
        countryArea = countryArea,
        phone = phone,
        isDefaultShipping = isDefaultShipping,
        isDefaultBilling = false,
    )

    private fun AddressDraft.isValid(): Boolean =
        firstName.isNotBlank() && streetAddress1.isNotBlank() && streetAddress2.isNotBlank() &&
            postalCode.isNotBlank() && phone.isNotBlank()

    private fun Double.toPlainString(): String =
        if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()
}
