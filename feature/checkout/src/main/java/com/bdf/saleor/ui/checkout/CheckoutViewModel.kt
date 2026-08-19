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
    val editingAddressId: String? = null,
    val useSelectedAddressAsDefault: Boolean = false,
    val deliveryOptions: List<DeliveryOption> = emptyList(),
    val selectedDeliveryMethodId: String? = null,
    val pointsBalance: Double = 0.0,
    val pointsCurrency: String = "KRW",
    val pointsInput: String = "",
    val pointsApplied: Double = 0.0,
    val pointsClampNotice: Boolean = false,
    val showPointsSection: Boolean = false,
    val promoCodeInput: String = "",
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
            val total = checkout.payableTotal() ?: return null
            val afterPoints = max(0.0, total - pointsApplied)
            if (afterPoints <= 0.0) return 0.0
            if (checkout.authorizeStatus == CheckoutAuthorizeStatus.PARTIAL) {
                val balance = checkout.totalBalance?.amount
                if (balance != null && balance > 0) return min(balance, afterPoints)
            }
            return afterPoints
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
        get() = selectedAddress != null &&
            !payBusy &&
            (session?.isShippingRequired == false || !selectedDeliveryMethodId.isNullOrBlank())

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
                            pointsInput = if (it.pointsInput.isBlank()) {
                                formatGroupedAmount(maxPoints)
                            } else {
                                it.pointsInput
                            },
                            showPointsSection = showPoints,
                            loggedIn = loggedIn,
                        )
                    }
                    if (loggedIn) {
                        checkoutRepository.attachCustomer()
                    }
                    if (_uiState.value.step == CheckoutStep.Payment) {
                        loadTossClientKey()
                    } else if (session.isShippingRequired) {
                        persistShippingAddressAndLoadDelivery()
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
        if (_uiState.value.step == CheckoutStep.Contact &&
            _uiState.value.session?.isShippingRequired != false
        ) {
            viewModelScope.launch { persistShippingAddressAndLoadDelivery(showValidationError = true) }
        }
    }

    fun changePaymentShippingAddress(address: Address) {
        selectSavedAddress(address)
        viewModelScope.launch {
            persistPaymentShippingAddress()
        }
    }

    fun onUseSelectedAddressAsDefaultChange(checked: Boolean) {
        _uiState.update { it.copy(useSelectedAddressAsDefault = checked) }
    }

    fun openAddressForm(address: Address? = null) = _uiState.update {
        it.copy(showAddressForm = true, editingAddressId = address?.id, error = null)
    }

    fun closeAddressForm() = _uiState.update {
        it.copy(showAddressForm = false, editingAddressId = null)
    }

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
        val persistOnPayment = _uiState.value.step == CheckoutStep.Payment
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
                        payBusy = true,
                        showAddressForm = false,
                        editingAddressId = null,
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
                        payBusy = true,
                        showAddressForm = false,
                        editingAddressId = null,
                        savedAddresses = existing + created,
                        selectedAddressId = created.id,
                        shippingDraft = draft,
                        useSelectedAddressAsDefault = false,
                    )
                }
            }
            if (persistOnPayment) {
                persistPaymentShippingAddress()
            } else if (_uiState.value.session?.isShippingRequired != false) {
                persistShippingAddressAndLoadDelivery(showValidationError = true)
            } else {
                _uiState.update { it.copy(payBusy = false) }
            }
        }
    }

    fun updateShippingAddress(draft: AddressDraft) {
        val addressId = _uiState.value.editingAddressId ?: return
        if (!draft.isValid()) {
            _uiState.update { it.copy(error = "배송지를 확인해 주세요") }
            return
        }
        val persistOnPayment = _uiState.value.step == CheckoutStep.Payment
        viewModelScope.launch {
            _uiState.update { it.copy(payBusy = true, error = null) }
            val loggedIn = authRepository.authState.value is AuthState.LoggedIn
            val existing = _uiState.value.savedAddresses.firstOrNull { it.id == addressId }
            val updated = if (loggedIn) {
                val result = accountRepository.updateAddress(addressId, draft)
                if (!result.success) {
                    _uiState.update { it.copy(payBusy = false, error = result.message ?: "배송지를 수정하지 못했습니다") }
                    return@launch
                }
                val profile = runCatching { authRepository.getProfile() }.getOrNull()
                profile?.addresses?.firstOrNull { it.id == addressId }
                    ?: draft.toAddress(id = addressId, isDefaultShipping = existing?.isDefaultShipping == true)
            } else {
                draft.toAddress(id = addressId, isDefaultShipping = existing?.isDefaultShipping == true)
            }
            val addresses = _uiState.value.savedAddresses.map { if (it.id == addressId) updated else it }
            val selectedId = _uiState.value.selectedAddressId
            _uiState.update {
                it.copy(
                    payBusy = true,
                    showAddressForm = false,
                    editingAddressId = null,
                    savedAddresses = addresses,
                    selectedAddressId = selectedId ?: updated.id,
                    shippingDraft = if (selectedId == null || selectedId == addressId) updated.toDraft() else it.shippingDraft,
                )
            }
            if (persistOnPayment && (_uiState.value.selectedAddressId == addressId)) {
                persistPaymentShippingAddress()
            } else if (_uiState.value.selectedAddressId == addressId &&
                _uiState.value.session?.isShippingRequired != false
            ) {
                persistShippingAddressAndLoadDelivery(showValidationError = true)
            } else {
                _uiState.update { it.copy(payBusy = false) }
            }
        }
    }

    fun onPointsInputChange(value: String) {
        val parsed = parseGroupedAmount(value) ?: 0.0
        val maxApplicable = min(_uiState.value.pointsBalance, _uiState.value.session?.total?.amount ?: 0.0)
        val clamped = min(parsed, maxApplicable)
        _uiState.update {
            it.copy(
                pointsInput = formatGroupedAmount(clamped),
                pointsClampNotice = false,
                error = null,
            )
        }
    }

    fun onPromoCodeInputChange(value: String) {
        _uiState.update { it.copy(promoCodeInput = value, error = null) }
    }

    fun applyPromoCode() {
        val code = _uiState.value.promoCodeInput.trim()
        if (code.isBlank()) {
            _uiState.update { it.copy(error = "할인 코드를 입력해 주세요") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(payBusy = true, error = null) }
            checkoutRepository.addPromoCode(code)
                .onSuccess { session ->
                    _uiState.update {
                        it.copy(
                            payBusy = false,
                            session = session,
                            promoCodeInput = "",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(payBusy = false, error = error.message) }
                }
        }
    }

    fun removePromoCode() {
        val code = _uiState.value.session?.voucherCode ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(payBusy = true, error = null) }
            checkoutRepository.removePromoCode(code)
                .onSuccess { session ->
                    _uiState.update { it.copy(payBusy = false, session = session, promoCodeInput = "") }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(payBusy = false, error = error.message) }
                }
        }
    }

    fun goBack() {
        if (_uiState.value.step == CheckoutStep.Payment) {
            goToStep(CheckoutStep.Contact)
        }
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
            val optionsResult = if (state.deliveryOptions.isEmpty()) {
                checkoutRepository.calculateDeliveryOptions()
            } else {
                Result.success(state.deliveryOptions)
            }
            val options = optionsResult.getOrDefault(state.deliveryOptions)
            val methodId = state.selectedDeliveryMethodId
                ?: options.firstOrNull { it.active }?.id
                ?: options.firstOrNull()?.id
            if (methodId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        payBusy = false,
                        session = session,
                        deliveryOptions = options,
                        error = optionsResult.exceptionOrNull()?.message ?: "배송 방법을 선택해 주세요",
                    )
                }
                return@launch
            }
            checkoutRepository.updateDeliveryMethod(methodId)
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            payBusy = false,
                            session = updated,
                            deliveryOptions = options,
                            selectedDeliveryMethodId = methodId,
                            step = CheckoutStep.Payment,
                        )
                    }
                    loadTossClientKey()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            payBusy = false,
                            session = session,
                            deliveryOptions = options,
                            error = error.message,
                        )
                    }
                }
        }
    }

    fun selectDelivery(deliveryMethodId: String) {
        _uiState.update { it.copy(selectedDeliveryMethodId = deliveryMethodId) }
    }

    fun applyPoints() {
        val state = _uiState.value
        val parsed = parseGroupedAmount(state.pointsInput)
        if (parsed == null || parsed <= 0) {
            _uiState.update { it.copy(error = "올바른 금액을 입력하세요") }
            return
        }
        if (parsed < PointsMinUnit) {
            _uiState.update { it.copy(error = "최소 사용 단위는 100 P입니다") }
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
                                pointsInput = formatGroupedAmount(authorized),
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
        val liveAmount = _uiState.value.payAmount
        if (liveAmount == null) {
            _uiState.update { it.copy(payBusy = false, error = "결제 금액을 확인할 수 없습니다") }
            return Result.failure(IllegalStateException("결제 금액을 확인할 수 없습니다"))
        }
        if (displayed > 0 && liveAmount > 0 && abs(displayed - liveAmount) > 0.01) {
            _uiState.update {
                it.copy(payBusy = false, priceChangeMessage = "결제 금액이 변경되었습니다. 다시 확인해 주세요.")
            }
            return Result.failure(IllegalStateException("price_change"))
        }
        val chargedAmount = when {
            liveAmount > 0 -> liveAmount
            displayed > 0 -> displayed
            else -> liveAmount
        }
        if (chargedAmount <= 0) {
            _uiState.update { it.copy(payBusy = false) }
            return Result.failure(IllegalStateException("결제 금액을 확인할 수 없습니다"))
        }
        val init = checkoutRepository.initializeTransaction(PaymentGateways.TOSS, chargedAmount)
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
            amount = chargedAmount,
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

    private suspend fun persistShippingAddressAndLoadDelivery(showValidationError: Boolean = false) {
        val state = _uiState.value
        val draft = state.shippingDraft
        if (!draft.isValid()) {
            if (showValidationError) {
                _uiState.update { it.copy(payBusy = false, error = "배송지를 확인해 주세요") }
            }
            return
        }
        _uiState.update { it.copy(payBusy = true, error = null) }
        val addressResult = checkoutRepository.updateShippingAddress(draft)
        if (addressResult.isFailure) {
            _uiState.update { it.copy(payBusy = false, error = addressResult.exceptionOrNull()?.message) }
            return
        }
        val session = addressResult.getOrThrow()
        if (!session.isShippingRequired) {
            _uiState.update {
                it.copy(
                    payBusy = false,
                    session = session,
                    deliveryOptions = emptyList(),
                    selectedDeliveryMethodId = null,
                )
            }
            return
        }
        val optionsResult = checkoutRepository.calculateDeliveryOptions()
        val options = optionsResult.getOrDefault(emptyList())
        val selectedId = pickDeliveryMethodId(options, state.selectedDeliveryMethodId)
        _uiState.update {
            it.copy(
                payBusy = false,
                session = session,
                deliveryOptions = options,
                selectedDeliveryMethodId = selectedId,
                error = optionsResult.exceptionOrNull()?.message,
            )
        }
    }

    private fun pickDeliveryMethodId(options: List<DeliveryOption>, previousId: String?): String? =
        when {
            previousId != null && options.any { it.id == previousId } -> previousId
            else -> options.firstOrNull { it.active }?.id ?: options.firstOrNull()?.id
        }

    private suspend fun persistPaymentShippingAddress() {
        val state = _uiState.value
        val draft = state.shippingDraft
        if (!draft.isValid()) {
            _uiState.update { it.copy(payBusy = false, error = "배송지를 확인해 주세요") }
            return
        }
        _uiState.update { it.copy(payBusy = true, error = null) }
        val addressResult = checkoutRepository.updateShippingAddress(draft)
        if (addressResult.isFailure) {
            _uiState.update { it.copy(payBusy = false, error = addressResult.exceptionOrNull()?.message) }
            return
        }
        var session = addressResult.getOrThrow()
        if (!session.isShippingRequired) {
            _uiState.update { it.copy(payBusy = false, session = session, step = CheckoutStep.Payment) }
            return
        }
        val optionsResult = checkoutRepository.calculateDeliveryOptions()
        val options = optionsResult.getOrDefault(emptyList())
        val previousId = state.selectedDeliveryMethodId ?: session.selectedDeliveryMethodId
        val selectedId = pickDeliveryMethodId(options, previousId)
        if (selectedId != null) {
            val deliveryResult = checkoutRepository.updateDeliveryMethod(selectedId)
            if (deliveryResult.isSuccess) {
                session = deliveryResult.getOrThrow()
            }
        }
        _uiState.update {
            it.copy(
                payBusy = false,
                session = session,
                deliveryOptions = options,
                selectedDeliveryMethodId = selectedId,
                step = CheckoutStep.Payment,
                error = optionsResult.exceptionOrNull()?.message,
            )
        }
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

internal fun CheckoutSession.payableTotal(): Double? {
    val listed = total?.amount
    if (listed != null && listed > 0) return listed
    val fromLines = lines.sumOf { it.totalPrice?.amount ?: 0.0 }
    val shippingAmount = shipping?.amount ?: 0.0
    val discountAmount = discount?.amount ?: 0.0
    val reconstructed = max(0.0, fromLines + shippingAmount - discountAmount)
    val pendingCharge = authorizeStatus != CheckoutAuthorizeStatus.NONE
    if (reconstructed > 0 && (listed == null || pendingCharge)) return reconstructed
    val subtotalAmount = subtotal?.amount
    if (subtotalAmount != null && subtotalAmount > 0 && (listed == null || pendingCharge)) {
        return subtotalAmount + shippingAmount
    }
    return listed
}
