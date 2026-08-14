package com.bdf.saleor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bdf.saleor.core.designsystem.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorefrontTopBar(
    storeName: String,
    initials: String?,
    onStoreNameClick: () -> Unit,
    onAccountClick: () -> Unit,
    onCartClick: () -> Unit,
    cartQuantity: Int = 0,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        modifier = modifier.testTag("storefront_top_bar"),
        title = {
            Text(
                text = storeName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .clickable(onClick = onStoreNameClick)
                    .testTag("storefront_logo")
                    .semantics { contentDescription = storeName },
            )
        },
        actions = {
            IconButton(
                onClick = onAccountClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("storefront_account")
                    .semantics {
                        contentDescription = initials ?: "Account"
                    },
            ) {
                if (initials.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                    )
                } else {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            BadgedBox(
                badge = {
                    if (cartQuantity > 0) {
                        Badge(modifier = Modifier.testTag("storefront_cart_badge")) {
                            Text(if (cartQuantity > 99) "99+" else cartQuantity.toString())
                        }
                    }
                },
            ) {
                IconButton(
                    onClick = onCartClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("storefront_cart"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingBag,
                        contentDescription = stringResource(R.string.cart),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(title) },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("back"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}
