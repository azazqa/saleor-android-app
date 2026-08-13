package com.bdf.saleor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.bdf.saleor.core.designsystem.R

@Composable
fun StorefrontTopBar(
    storeName: String,
    initials: String?,
    onStoreNameClick: () -> Unit,
    onAccountClick: () -> Unit,
    onCartClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .testTag("storefront_top_bar"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = storeName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable(onClick = onStoreNameClick)
                    .testTag("storefront_logo")
                    .semantics { contentDescription = storeName },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = onAccountClick)
                        .testTag("storefront_account")
                        .semantics {
                            contentDescription = initials ?: "Account"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (initials.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    } else {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
                IconButton(
                    onClick = onCartClick,
                    modifier = Modifier.testTag("storefront_cart"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingBag,
                        contentDescription = stringResource(R.string.cart),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.testTag("storefront_menu"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = stringResource(R.string.menu),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun BackTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("back")
            .semantics { contentDescription = "Back" }
            .clickable(onClick = onClick),
    )
}
