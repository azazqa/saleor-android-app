package com.bdf.saleor.feature.checkout.checkout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bdf.saleor.feature.checkout.R

@Composable
fun CheckoutCompleteScreen(
    orderId: String,
    orderNumber: String,
    onHome: () -> Unit,
    onViewOrder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayNumber = orderNumber.ifBlank { orderId }
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
            .testTag("checkout_complete_screen"),
    ) {
        Text(
            text = stringResource(R.string.checkout_complete_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.checkout_complete_number, displayNumber),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("checkout_complete_order_number"),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onViewOrder(orderId) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_complete_order"),
        ) {
            Text(stringResource(R.string.checkout_complete_order))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_complete_home"),
        ) {
            Text(stringResource(R.string.checkout_complete_home))
        }
    }
}
