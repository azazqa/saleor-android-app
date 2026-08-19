package com.bdf.saleor.feature.checkout.checkout

import com.tosspayments.paymentsdk.PaymentWidget
import com.tosspayments.paymentsdk.model.PaymentCallback
import com.tosspayments.paymentsdk.model.TossPaymentResult
import com.tosspayments.paymentsdk.view.PaymentMethod

/** Toss JS/Android docs use `PaymentWidget.ANONYMOUS`; 0.1.15 does not expose the constant. */
internal const val TossAnonymousCustomerKey = "ANONYMOUS"

fun PaymentWidget.requestTossPayment(
    orderId: String,
    orderName: String,
    onSuccess: (TossPaymentResult.Success) -> Unit,
    onFailed: (TossPaymentResult.Fail) -> Unit,
) {
    requestPayment(
        paymentInfo = PaymentMethod.PaymentInfo(orderId = orderId, orderName = orderName),
        paymentCallback = object : PaymentCallback {
            override fun onPaymentSuccess(success: TossPaymentResult.Success) = onSuccess(success)
            override fun onPaymentFailed(fail: TossPaymentResult.Fail) = onFailed(fail)
        },
    )
}
