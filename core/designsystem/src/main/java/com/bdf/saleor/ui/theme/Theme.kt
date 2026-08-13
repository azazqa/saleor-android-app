package com.bdf.saleor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandNavy,
    onPrimary = BrandPrimaryForeground,
    primaryContainer = BrandGoldLight,
    onPrimaryContainer = BrandNavy,
    secondary = BrandGold,
    onSecondary = BrandPrimaryForeground,
    secondaryContainer = BrandGoldLight,
    onSecondaryContainer = BrandNavy,
    tertiary = BrandNavyAlt,
    onTertiary = BrandPrimaryForeground,
    background = BrandOffWhite,
    onBackground = BrandNavy,
    surface = BrandCard,
    onSurface = BrandNavy,
    surfaceVariant = BrandOffWhite,
    onSurfaceVariant = BrandMuted,
    outline = BrandBorder,
    outlineVariant = BrandBorder,
    error = BrandDestructive,
    onError = Color.White,
)

@Composable
fun SaleorAppTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
