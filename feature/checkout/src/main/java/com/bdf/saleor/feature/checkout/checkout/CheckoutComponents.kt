package com.bdf.saleor.feature.checkout.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bdf.saleor.core.model.Address
import com.bdf.saleor.feature.checkout.R
import com.bdf.saleor.core.designsystem.theme.AppSpacing

@Composable
internal fun SectionCard(
    icon: @Composable () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    tonal: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (tonal) colors.secondaryContainer else colors.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (tonal) 0.dp else 1.dp),
    ) {
        Column(modifier = Modifier.padding(AppSpacing.CardContent)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                icon()
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                action?.invoke()
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
internal fun SectionIconChip(
    imageVector: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(MaterialTheme.shapes.small)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
internal fun CheckoutChangeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
    ) {
        Text(
            text = stringResource(R.string.checkout_summary_change),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun AddressBlock(
    address: Address,
    modifier: Modifier = Modifier,
    showPhone: Boolean = true,
    compact: Boolean = false,
    showDefaultBadge: Boolean = false,
) {
    val lines = address.displayLines(includePhone = compact && showPhone)
    val nameStyle = if (compact) {
        MaterialTheme.typography.bodyLarge
    } else {
        MaterialTheme.typography.titleMedium
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = lines.name.ifBlank { lines.street },
                style = nameStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (showDefaultBadge && address.isDefaultShipping) {
                DefaultShippingBadge(compact = compact)
            }
        }
        if (lines.street.isNotBlank()) {
            Text(
                text = lines.street,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (lines.locality.isNotBlank()) {
            Text(
                text = lines.locality,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showPhone && !compact && lines.phone.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = lines.phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun DefaultShippingBadge(compact: Boolean = false) {
    val label = if (compact) {
        stringResource(R.string.checkout_default_shipping_short)
    } else {
        stringResource(R.string.checkout_default_shipping)
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
internal fun CheckoutBottomBar(
    summaryLabel: String,
    amount: String,
    ctaLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    ctaTestTag: String = "checkout_bottom_cta",
    ctaIcon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor),
    ) {
        HorizontalDivider(thickness = 1.dp, color = colors.outlineVariant)
        Column(
            modifier = Modifier.padding(
                start = AppSpacing.ScreenHorizontal,
                end = AppSpacing.ScreenHorizontal,
                top = AppSpacing.CardGap,
                bottom = AppSpacing.DividerVertical,
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.CardGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = summaryLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.primary,
                )
            }
            Button(
                onClick = onClick,
                enabled = enabled && !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.ListItemMinHeight)
                    .testTag(ctaTestTag),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) {
                if (ctaIcon != null) {
                    Icon(
                        imageVector = ctaIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                    )
                }
                Text(
                    text = ctaLabel,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}
