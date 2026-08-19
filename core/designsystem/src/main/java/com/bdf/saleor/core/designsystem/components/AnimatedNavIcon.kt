package com.bdf.saleor.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bdf.saleor.core.designsystem.theme.AppMotion
import com.bdf.saleor.core.designsystem.util.rememberReduceMotion

@Composable
fun AnimatedNavIcon(
    selected: Boolean,
    outlined: ImageVector,
    filled: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()

    val fill by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (reduceMotion) {
            snap()
        } else {
            tween(
                durationMillis = AppMotion.IconFadeIn,
                easing = AppMotion.EmphasizedDecelerate,
            )
        },
        label = "navIconFill",
    )

    val scale = remember { Animatable(1f) }
    var isFirstComposition by remember { mutableStateOf(true) }

    LaunchedEffect(selected) {
        if (isFirstComposition) {
            isFirstComposition = false
            scale.snapTo(1f)
            return@LaunchedEffect
        }
        if (selected && !reduceMotion) {
            scale.snapTo(0.92f)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.48f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        } else {
            scale.snapTo(1f)
        }
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    ) {
        Icon(
            imageVector = outlined,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { alpha = 1f - fill },
        )
        Icon(
            imageVector = filled,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { alpha = fill },
        )
    }
}
