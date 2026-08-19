package com.bdf.saleor.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bdf.saleor.ui.theme.AppMotion
import com.bdf.saleor.ui.util.rememberReduceMotion
import kotlin.math.roundToInt

private val NavBarHeight = 60.dp

data class AppBottomNavItem(
    val label: String,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector,
    val selected: Boolean,
    val testTag: String,
    val onClick: () -> Unit,
)

@Composable
fun AppBottomNavigation(
    items: List<AppBottomNavItem>,
    modifier: Modifier = Modifier,
    scrollVisible: Boolean = true,
) {
    val reduceMotion = rememberReduceMotion()
    val hideFraction by animateFloatAsState(
        targetValue = if (scrollVisible) 0f else 1f,
        animationSpec = if (reduceMotion) {
            snap()
        } else {
            tween(durationMillis = 220, easing = AppMotion.Standard)
        },
        label = "bottomNavHide",
    )
    var barHeightPx by remember { mutableIntStateOf(0) }
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .wrapToMeasuredHeight()
            .fillMaxWidth()
            .offset {
                IntOffset(0, (barHeightPx * hideFraction).roundToInt())
            }
            .onSizeChanged { barHeightPx = it.height }
            .then(
                if (!scrollVisible) {
                    Modifier.semantics { hideFromAccessibility() }
                } else {
                    Modifier
                },
            )
            .background(colors.surfaceContainerLowest),
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = colors.outlineVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.navigationBars.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                )
                .defaultMinSize(minHeight = NavBarHeight)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                AppBottomNavItem(item = item)
            }
        }
    }
}

@Composable
private fun RowScope.AppBottomNavItem(item: AppBottomNavItem) {
    val colors = MaterialTheme.colorScheme
    val contentColor = if (item.selected) colors.onSurface else colors.onSurfaceVariant
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = NavBarHeight)
            .selectable(
                selected = item.selected,
                onClick = item.onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .then(
                if (focused) {
                    Modifier.border(1.dp, colors.outline, MaterialTheme.shapes.small)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 8.dp)
            .testTag(item.testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedNavIcon(
            selected = item.selected,
            outlined = item.iconOutlined,
            filled = item.iconFilled,
            tint = contentColor,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = if (item.selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/**
 * Parent slots such as Scaffold bottomBar may pass minHeight equal to the remaining
 * screen height. Ignore that minimum so the bar reports its content height only.
 */
private fun Modifier.wrapToMeasuredHeight(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    layout(placeable.width, placeable.height) {
        placeable.placeRelative(0, 0)
    }
}
