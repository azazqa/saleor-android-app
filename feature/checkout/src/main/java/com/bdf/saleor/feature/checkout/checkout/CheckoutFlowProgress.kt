package com.bdf.saleor.feature.checkout.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bdf.saleor.feature.checkout.R

internal const val CheckoutFlowStepCount = 3
internal const val CheckoutFlowCartStep = 0

private val CheckoutFlowLabels = listOf(
    R.string.cart_title,
    R.string.checkout_contact,
    R.string.checkout_payment,
)

internal fun CheckoutStep.toFlowIndex(): Int = when (this) {
    CheckoutStep.Contact -> 1
    CheckoutStep.Payment -> 2
}

@Composable
internal fun CheckoutFlowProgress(
    stepIndex: Int,
    modifier: Modifier = Modifier,
) = CheckoutStepper(stepIndex = stepIndex, modifier = modifier)

@Composable
internal fun CheckoutStepper(
    stepIndex: Int,
    modifier: Modifier = Modifier,
) {
    val current = stepIndex.coerceIn(0, CheckoutFlowStepCount - 1)
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("checkout_flow_progress"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckoutFlowLabels.forEachIndexed { index, _ ->
                val completed = index < current
                val selected = index == current
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(if (index <= current) colors.primary else colors.outlineVariant),
                    )
                }
                val circle = @Composable {
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val inner = @Composable {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            completed || selected -> colors.primary
                                            else -> colors.surfaceContainer
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (completed) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = colors.onPrimary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .border(4.dp, colors.primary.copy(alpha = 0.28f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                inner()
                            }
                        } else {
                            inner()
                        }
                    }
                }
                circle()
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            CheckoutFlowLabels.forEachIndexed { index, labelRes ->
                val selected = index == current
                val label = stringResource(labelRes)
                val description = if (selected) {
                    stringResource(
                        R.string.checkout_step_current_cd,
                        CheckoutFlowStepCount,
                        index + 1,
                        label,
                    )
                } else {
                    label
                }
                val align = when (index) {
                    0 -> TextAlign.Start
                    CheckoutFlowLabels.lastIndex -> TextAlign.End
                    else -> TextAlign.Center
                }
                Text(
                    text = label,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = description },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (selected) colors.primary else colors.onSurfaceVariant,
                    textAlign = align,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
