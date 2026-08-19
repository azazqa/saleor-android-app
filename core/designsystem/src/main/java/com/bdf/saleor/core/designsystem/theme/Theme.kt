package com.bdf.saleor.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrandNavy,
    onPrimary = BrandPrimaryForeground,
    primaryContainer = BrandGoldLight,
    onPrimaryContainer = BrandNavy,
    secondary = BrandGold,
    onSecondary = BrandPrimaryForeground,
    secondaryContainer = BrandGoldLight,
    onSecondaryContainer = BrandOnSurface,
    tertiary = BrandNavyAlt,
    onTertiary = BrandPrimaryForeground,
    background = BrandOffWhite,
    onBackground = BrandOnSurface,
    surface = BrandOffWhite,
    onSurface = BrandOnSurface,
    surfaceVariant = BrandOffWhite,
    onSurfaceVariant = BrandMuted,
    surfaceContainerLowest = BrandWhite,
    surfaceContainerLow = BrandSurfaceLow,
    surfaceContainer = BrandSurfaceContainer,
    outline = BrandBorder,
    outlineVariant = BrandBorder,
    error = BrandDestructive,
    onError = BrandPrimaryForeground,
)

@Composable
fun SaleorAppTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
