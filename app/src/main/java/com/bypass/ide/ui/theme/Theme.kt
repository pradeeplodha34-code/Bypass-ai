package com.bypass.ide.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BypassDarkColorScheme = darkColorScheme(
    primary = CyanElectric,
    onPrimary = ObsidianBg,
    primaryContainer = CyanMuted,
    onPrimaryContainer = CyanSoft,
    secondary = EmeraldMatrix,
    onSecondary = ObsidianBg,
    secondaryContainer = EmeraldMuted,
    onSecondaryContainer = EmeraldSoft,
    tertiary = VioletPulse,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianBorder,
    outlineVariant = ColorBorderSubtle,
    error = RoseError,
    onError = TextPrimary
)

private val ColorBorderSubtle get() = ObsidianBorder.copy(alpha = 0.55f)

val BypassShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun BypassIDETheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BypassDarkColorScheme,
        typography = BypassTypography,
        shapes = BypassShapes,
        content = content
    )
}
