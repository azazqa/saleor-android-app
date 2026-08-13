package com.bdf.saleor.ui.checkout

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bdf.saleor.feature.checkout.R
import com.bdf.saleor.ui.theme.SaleorAppTheme
import com.tosspayments.paymentsdk.PaymentWidget
import com.tosspayments.paymentsdk.view.Agreement
import com.tosspayments.paymentsdk.view.PaymentMethod

class TossPaymentActivity : AppCompatActivity() {
    private lateinit var paymentWidget: PaymentWidget
    private var payBusy by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val clientKey = intent.getStringExtra(EXTRA_CLIENT_KEY)
        val orderId = intent.getStringExtra(EXTRA_ORDER_ID).orEmpty()
        val orderName = intent.getStringExtra(EXTRA_ORDER_NAME).orEmpty()
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val customerKey = intent.getStringExtra(EXTRA_CUSTOMER_KEY)
            ?.takeIf { it.isNotBlank() }
            ?: TossAnonymousCustomerKey
        if (clientKey.isNullOrBlank() || orderId.isBlank() || amount <= 0) {
            setResult(
                Activity.RESULT_CANCELED,
                Intent().putExtra(EXTRA_ERROR_MESSAGE, getString(R.string.checkout_toss_invalid)),
            )
            finish()
            return
        }
        paymentWidget = PaymentWidget(
            activity = this,
            clientKey = clientKey,
            customerKey = customerKey,
        )
        setContent {
            SaleorAppTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(orderName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        getString(R.string.checkout_toss_amount, amount.toInt().toString()),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    AndroidView(
                        factory = { context ->
                            PaymentMethod(context).also { method ->
                                paymentWidget.renderPaymentMethods(
                                    method,
                                    PaymentMethod.Rendering.Amount(
                                        value = amount,
                                        currency = PaymentMethod.Rendering.Currency.KRW,
                                        country = "KR",
                                    ),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AndroidView(
                        factory = { context ->
                            Agreement(context).also { agreement ->
                                paymentWidget.renderAgreement(agreement)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            if (payBusy) return@Button
                            payBusy = true
                            runCatching {
                                paymentWidget.requestTossPayment(
                                    orderId = orderId,
                                    orderName = orderName,
                                    onSuccess = { success ->
                                        setResult(
                                            Activity.RESULT_OK,
                                            Intent()
                                                .putExtra(EXTRA_PAYMENT_KEY, success.paymentKey)
                                                .putExtra(EXTRA_ORDER_ID, success.orderId)
                                                .putExtra(EXTRA_AMOUNT, success.amount.toDouble()),
                                        )
                                        finish()
                                    },
                                    onFailed = { fail ->
                                        setResult(
                                            Activity.RESULT_CANCELED,
                                            Intent().putExtra(
                                                EXTRA_ERROR_MESSAGE,
                                                fail.errorMessage.ifBlank { getString(R.string.checkout_toss_cancelled) },
                                            ),
                                        )
                                        finish()
                                    },
                                )
                            }.onFailure { error ->
                                payBusy = false
                                setResult(
                                    Activity.RESULT_CANCELED,
                                    Intent().putExtra(
                                        EXTRA_ERROR_MESSAGE,
                                        error.message ?: getString(R.string.checkout_toss_cancelled),
                                    ),
                                )
                                finish()
                            }
                        },
                        enabled = !payBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(getString(R.string.checkout_pay))
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_CLIENT_KEY = "client_key"
        const val EXTRA_CUSTOMER_KEY = "customer_key"
        const val EXTRA_ORDER_ID = "order_id"
        const val EXTRA_ORDER_NAME = "order_name"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_PAYMENT_KEY = "payment_key"
        const val EXTRA_ERROR_MESSAGE = "error_message"
        const val EXTRA_TRANSACTION_ID = "transaction_id"

        fun intent(context: Context, request: TossPaymentRequest): Intent =
            Intent(context, TossPaymentActivity::class.java)
                .putExtra(EXTRA_CLIENT_KEY, request.clientKey)
                .putExtra(EXTRA_CUSTOMER_KEY, request.customerKey)
                .putExtra(EXTRA_ORDER_ID, request.orderId)
                .putExtra(EXTRA_ORDER_NAME, request.orderName)
                .putExtra(EXTRA_AMOUNT, request.amount)
                .putExtra(EXTRA_TRANSACTION_ID, request.transactionId)
    }
}
