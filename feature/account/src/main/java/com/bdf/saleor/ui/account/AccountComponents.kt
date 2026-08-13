package com.bdf.saleor.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.bdf.saleor.feature.account.R

enum class AccountTab {
    Overview,
    Orders,
    Points,
    Addresses,
    Settings,
}

@Composable
fun AccountTabRow(
    selected: AccountTab,
    onSelect: (AccountTab) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        AccountTabButton(
            selected = selected == AccountTab.Overview,
            icon = Icons.Outlined.GridView,
            label = stringResource(R.string.tab_overview),
            testTag = "account_tab_overview",
            onClick = { onSelect(AccountTab.Overview) },
        )
        AccountTabButton(
            selected = selected == AccountTab.Orders,
            icon = Icons.Outlined.Receipt,
            label = stringResource(R.string.tab_orders),
            testTag = "account_tab_orders",
            onClick = { onSelect(AccountTab.Orders) },
        )
        AccountTabButton(
            selected = selected == AccountTab.Points,
            icon = Icons.Outlined.MonetizationOn,
            label = stringResource(R.string.tab_points),
            testTag = "account_tab_points",
            onClick = { onSelect(AccountTab.Points) },
        )
        AccountTabButton(
            selected = selected == AccountTab.Addresses,
            icon = Icons.Outlined.LocationOn,
            label = stringResource(R.string.tab_addresses),
            testTag = "account_tab_addresses",
            onClick = { onSelect(AccountTab.Addresses) },
        )
        AccountTabButton(
            selected = selected == AccountTab.Settings,
            icon = Icons.Outlined.Settings,
            label = stringResource(R.string.tab_settings),
            testTag = "account_tab_settings",
            onClick = { onSelect(AccountTab.Settings) },
        )
        AccountTabButton(
            selected = false,
            icon = Icons.AutoMirrored.Outlined.Logout,
            label = stringResource(R.string.account_logout),
            testTag = "account_tab_logout",
            onClick = onLogout,
        )
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
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = content, maxLines = 1)
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        content = content,
    )
}
