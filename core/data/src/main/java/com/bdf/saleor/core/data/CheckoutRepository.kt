package com.bdf.saleor.core.data

import com.bdf.saleor.core.model.AddressDraft
import com.bdf.saleor.core.model.CheckoutSession
import com.bdf.saleor.core.model.CompletedOrder
import com.bdf.saleor.core.model.DeliveryOption
import com.bdf.saleor.core.model.PaymentResult

interface CheckoutRepository {
    suspend fun loadCheckout(): Result<CheckoutSession>

    suspend fun updateEmail(email: String): Result<CheckoutSession>

    suspend fun attachCustomer(): Result<CheckoutSession>

    suspend fun updateShippingAddress(draft: AddressDraft, saveAddress: Boolean = false): Result<CheckoutSession>

    suspend fun updateBillingAddress(draft: AddressDraft, saveAddress: Boolean = false): Result<CheckoutSession>

    suspend fun calculateDeliveryOptions(): Result<List<DeliveryOption>>

    suspend fun updateDeliveryMethod(deliveryMethodId: String): Result<CheckoutSession>

    suspend fun addPromoCode(promoCode: String): Result<CheckoutSession>

    suspend fun removePromoCode(promoCode: String): Result<CheckoutSession>

    suspend fun initializeTossClientKey(): Result<String>

    suspend fun initializeTransaction(gatewayId: String, amount: Double? = null): Result<PaymentResult>

    suspend fun processTransaction(
        transactionId: String,
        paymentKey: String,
        orderId: String,
        amount: Double,
    ): Result<PaymentResult>

    suspend fun completeCheckout(): Result<CompletedOrder>
}
