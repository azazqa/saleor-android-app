package com.bdf.saleor.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bdf.saleor.feature.account.R
import com.bdf.saleor.core.designsystem.theme.AppSpacing

enum class AccountTab {
    Overview,
    Orders,
    Points,
    Addresses,
    Settings,
}

private data class AccountTabSpec(
    val tab: AccountTab,
    val icon: ImageVector,
    val labelRes: Int,
    val testTag: String,
)

private val AccountTabSpecs = listOf(
    AccountTabSpec(AccountTab.Overview, Icons.Outlined.SpaceDashboard, R.string.tab_overview, "account_tab_overview"),
    AccountTabSpec(AccountTab.Orders, Icons.Outlined.Receipt, R.string.tab_orders, "account_tab_orders"),
    AccountTabSpec(AccountTab.Points, Icons.Outlined.MonetizationOn, R.string.tab_points, "account_tab_points"),
    AccountTabSpec(AccountTab.Addresses, Icons.Outlined.LocationOn, R.string.tab_addresses, "account_tab_addresses"),
    AccountTabSpec(AccountTab.Settings, Icons.Outlined.Settings, R.string.tab_settings, "account_tab_settings"),
)

@Composable
fun AccountTabRow(
    selected: AccountTab,
    onSelect: (AccountTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(4.dp),
    ) {
        AccountTabSpecs.forEach { spec ->
            AccountTabButton(
                selected = spec.tab == selected,
                icon = spec.icon,
                label = stringResource(spec.labelRes),
                testTag = spec.testTag,
                onClick = { onSelect(spec.tab) },
            )
        }
    }
}

@Composable
private fun RowScope.AccountTabButton(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun DashedEmptyBox(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val outline = MaterialTheme.colorScheme.outline
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = outline,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                        ),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                    )
                }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (action != null) {
                Spacer(modifier = Modifier.height(16.dp))
                action()
            }
        }
    }
}

@Composable
fun AccountCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.CardContent),
            content = content,
        )
    }
}
