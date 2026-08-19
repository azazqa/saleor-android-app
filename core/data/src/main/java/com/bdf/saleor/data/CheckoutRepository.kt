package com.bdf.saleor.data

import com.bdf.saleor.data.model.AddressDraft
import com.bdf.saleor.data.model.CheckoutSession
import com.bdf.saleor.data.model.CompletedOrder
import com.bdf.saleor.data.model.DeliveryOption
import com.bdf.saleor.data.model.PaymentResult

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
