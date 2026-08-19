package com.bdf.saleor.core.designsystem.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.bdf.saleor.core.designsystem.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorefrontTopBar(
    storeName: String,
    onStoreNameClick: () -> Unit,
    onCartClick: () -> Unit,
    cartQuantity: Int = 0,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val colors = MaterialTheme.colorScheme
    TopAppBar(
        modifier = modifier.testTag("storefront_top_bar"),
        title = {
            Text(
                text = storeName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .clickable(onClick = onStoreNameClick)
                    .testTag("storefront_logo"),
            )
        },
        actions = {
            CartIconButton(
                cartQuantity = cartQuantity,
                onCartClick = onCartClick,
                badgeBorderColor = colors.surfaceContainerLow,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.surfaceContainerLow,
            scrolledContainerColor = colors.surfaceContainerLow,
            titleContentColor = colors.onSurface,
            actionIconContentColor = colors.onSurface,
        ),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun CartIconButton(
    cartQuantity: Int,
    onCartClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeBorderColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    testTag: String = "storefront_cart",
    badgeTestTag: String = "storefront_cart_badge",
) {
    IconButton(
        onClick = onCartClick,
        modifier = modifier
            .size(48.dp)
            .testTag(testTag),
    ) {
        BadgedBox(
            badge = {
                if (cartQuantity > 0) {
                    Badge(
                        modifier = Modifier
                            .offset(x = 6.dp, y = (-4).dp)
                            .border(2.dp, badgeBorderColor, CircleShape)
                            .testTag(badgeTestTag),
                    ) {
                        Text(if (cartQuantity > 99) "99+" else cartQuantity.toString())
                    }
                }
            },
        ) {
            Icon(
                imageVector = Icons.Outlined.ShoppingBag,
                contentDescription = stringResource(R.string.cart),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun rememberBottomNavScrollConnection(
    onHide: () -> Unit,
    onShow: () -> Unit,
    enabled: Boolean = true,
): NestedScrollConnection {
    val thresholdPx = with(LocalDensity.current) { 12.dp.toPx() }
    return remember(enabled, thresholdPx, onHide, onShow) {
        object : NestedScrollConnection {
            private var accumulated = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!enabled) return Offset.Zero
                if (available.y > 0f) {
                    onShow()
                    accumulated = 0f
                    return Offset.Zero
                }
                val dy = consumed.y
                if (dy == 0f) return Offset.Zero
                accumulated += dy
                if (accumulated <= -thresholdPx) {
                    onHide()
                    accumulated = 0f
                } else if (accumulated >= thresholdPx) {
                    onShow()
                    accumulated = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!enabled) return Velocity.Zero
                when {
                    consumed.y < 0f || available.y < 0f -> onHide()
                    consumed.y > 0f || available.y > 0f -> onShow()
                }
                accumulated = 0f
                return Velocity.Zero
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    actions: @Composable RowScope.() -> Unit = {},
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
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}
