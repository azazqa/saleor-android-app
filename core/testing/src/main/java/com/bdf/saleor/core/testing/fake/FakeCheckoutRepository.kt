package com.bdf.saleor.core.testing.fake

import com.bdf.saleor.core.data.CheckoutRepository
import com.bdf.saleor.core.model.Address
import com.bdf.saleor.core.model.AddressDraft
import com.bdf.saleor.core.model.CheckoutAuthorizeStatus
import com.bdf.saleor.core.model.CheckoutSession
import com.bdf.saleor.core.model.CompletedOrder
import com.bdf.saleor.core.model.DeliveryOption
import com.bdf.saleor.core.model.Money
import com.bdf.saleor.core.model.PaymentGateways
import com.bdf.saleor.core.model.PaymentGatewayInfo
import com.bdf.saleor.core.model.PaymentResult

class FakeCheckoutRepository : CheckoutRepository {
    var session: CheckoutSession = sampleSession()
    var deliveryOptions: List<DeliveryOption> = listOf(
        DeliveryOption(
            id = "d1",
            shippingMethodId = "sm1",
            name = "일반 배송",
            price = Money(3_000.0, "KRW"),
            minDeliveryDays = 1,
            maxDeliveryDays = 3,
            active = true,
        ),
    )
    var tossClientKey: String = "test_ck_android"
    var completeOrderId: String = "order-1"
    var completeOrderNumber: String = "1001"
    var lastDeliveryMethodId: String? = null
    var lastTransactionGateway: String? = null
    var lastTransactionAmount: Double? = null
    var processedTransactions: MutableList<String> = mutableListOf()
    var shouldFailLoad: Boolean = false
    var shouldFailComplete: Boolean = false
    var priceOnNextLoad: Money? = null
    var lastPromoCode: String? = null
    var shouldFailPromo: Boolean = false

    override suspend fun loadCheckout(): Result<CheckoutSession> {
        if (shouldFailLoad) return Result.failure(IllegalStateException("load failed"))
        priceOnNextLoad?.let { next ->
            session = session.copy(total = next, totalBalance = next)
            priceOnNextLoad = null
        }
        return Result.success(session)
    }

    override suspend fun updateEmail(email: String): Result<CheckoutSession> {
        session = session.copy(email = email)
        return Result.success(session)
    }

    override suspend fun attachCustomer(): Result<CheckoutSession> = Result.success(session)

    override suspend fun updateShippingAddress(
        draft: AddressDraft,
        saveAddress: Boolean,
    ): Result<CheckoutSession> {
        session = session.copy(shippingAddress = draft.toAddress("addr-ship"))
        return Result.success(session)
    }

    override suspend fun updateBillingAddress(
        draft: AddressDraft,
        saveAddress: Boolean,
    ): Result<CheckoutSession> {
        session = session.copy(billingAddress = draft.toAddress("addr-bill"))
        return Result.success(session)
    }

    override suspend fun calculateDeliveryOptions(): Result<List<DeliveryOption>> =
        Result.success(deliveryOptions)

    override suspend fun updateDeliveryMethod(deliveryMethodId: String): Result<CheckoutSession> {
        lastDeliveryMethodId = deliveryMethodId
        session = session.copy(selectedDeliveryMethodId = deliveryMethodId)
        return Result.success(session)
    }

    override suspend fun addPromoCode(promoCode: String): Result<CheckoutSession> {
        val code = promoCode.trim()
        if (code.isEmpty()) return Result.failure(IllegalStateException("할인 코드를 입력해 주세요"))
        if (shouldFailPromo || code.equals("INVALID", ignoreCase = true)) {
            return Result.failure(IllegalStateException("유효하지 않은 할인 코드입니다"))
        }
        lastPromoCode = code
        val currency = session.total?.currency ?: "KRW"
        val previous = session.discount?.amount ?: 0.0
        val discountAmount = 1_000.0
        val restored = (session.total?.amount ?: 0.0) + previous
        session = session.copy(
            voucherCode = code,
            discountName = code,
            discount = Money(discountAmount, currency),
            total = Money(maxOf(0.0, restored - discountAmount), currency),
            totalBalance = Money(maxOf(0.0, restored - discountAmount), currency),
        )
        return Result.success(session)
    }

    override suspend fun removePromoCode(promoCode: String): Result<CheckoutSession> {
        lastPromoCode = null
        val currency = session.total?.currency ?: "KRW"
        val previous = session.discount?.amount ?: 0.0
        session = session.copy(
            voucherCode = null,
            discountName = null,
            discount = null,
            total = Money((session.total?.amount ?: 0.0) + previous, currency),
            totalBalance = Money((session.totalBalance?.amount ?: 0.0) + previous, currency),
        )
        return Result.success(session)
    }

    override suspend fun initializeTossClientKey(): Result<String> = Result.success(tossClientKey)

    override suspend fun initializeTransaction(
        gatewayId: String,
        amount: Double?,
    ): Result<PaymentResult> {
        lastTransactionGateway = gatewayId
        lastTransactionAmount = amount
        if (gatewayId == PaymentGateways.POINTS) {
            val authorized = amount ?: 0.0
            session = session.copy(
                authorizeStatus = CheckoutAuthorizeStatus.PARTIAL,
                totalBalance = Money(
                    (session.total?.amount ?: 0.0) - authorized,
                    session.total?.currency ?: "KRW",
                ),
            )
            return Result.success(
                PaymentResult(success = true, transactionId = "tx-points", authorizedAmount = authorized, amount = authorized),
            )
        }
        session = session.copy(
            authorizeStatus = CheckoutAuthorizeStatus.PARTIAL,
            totalBalance = Money(0.0, session.total?.currency ?: "KRW"),
        )
        return Result.success(
            PaymentResult(
                success = true,
                transactionId = "tx-toss",
                orderId = "toss-order-1",
                orderName = "Tea",
                amount = amount ?: session.totalBalance?.amount ?: session.total?.amount ?: 10_000.0,
                currency = "KRW",
                customerKey = null,
            ),
        )
    }

    override suspend fun processTransaction(
        transactionId: String,
        paymentKey: String,
        orderId: String,
        amount: Double,
    ): Result<PaymentResult> {
        processedTransactions += transactionId
        return Result.success(PaymentResult(success = true, transactionId = transactionId, orderId = orderId, amount = amount))
    }

    override suspend fun completeCheckout(): Result<CompletedOrder> {
        if (shouldFailComplete) return Result.failure(IllegalStateException("complete failed"))
        return Result.success(CompletedOrder(id = completeOrderId, number = completeOrderNumber))
    }

    companion object {
        fun sampleSession(
            total: Double = 10_000.0,
            isShippingRequired: Boolean = true,
        ) = CheckoutSession(
            id = "checkout-1",
            email = null,
            lines = listOf(FakeCartRepository.sampleLine()),
            quantity = 1,
            subtotal = Money(total, "KRW"),
            shipping = Money(0.0, "KRW"),
            discount = null,
            voucherCode = null,
            discountName = null,
            total = Money(total, "KRW"),
            totalBalance = Money(total, "KRW"),
            shippingAddress = null,
            billingAddress = null,
            isShippingRequired = isShippingRequired,
            selectedDeliveryMethodId = null,
            authorizeStatus = CheckoutAuthorizeStatus.NONE,
            availablePaymentGateways = listOf(
                PaymentGatewayInfo(PaymentGateways.TOSS, "Toss Payments"),
                PaymentGatewayInfo(PaymentGateways.POINTS, "Points"),
            ),
            channelSlug = "kr",
        )
    }
}

private fun AddressDraft.toAddress(id: String) = Address(
    id = id,
    firstName = firstName,
    lastName = lastName,
    companyName = companyName,
    streetAddress1 = streetAddress1,
    streetAddress2 = streetAddress2,
    city = city,
    cityArea = cityArea,
    postalCode = postalCode,
    countryCode = countryCode,
    countryName = "South Korea",
    countryArea = countryArea,
    phone = phone,
    isDefaultShipping = false,
    isDefaultBilling = false,
)
