package com.mydailyhisab.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LuxuryDarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = LuxuryBlack,
    secondary = AccentMagenta,
    onSecondary = TextWhite,
    tertiary = AccentOrange,
    background = LuxuryBlack,
    surface = LuxurySurface,
    onBackground = TextWhite,
    onSurface = TextWhite,
    surfaceVariant = LuxurySurfaceElevated,
    outline = LuxuryBorder
)

private val LuxuryLightColorScheme = lightColorScheme(
    primary = AccentVelvet,
    onPrimary = LuxuryLightSurface,
    secondary = AccentMagenta,
    onSecondary = LuxuryLightSurface,
    tertiary = AccentOrangeDeep,
    background = LuxuryLightBackground,
    surface = LuxuryLightSurface,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = LuxuryLightSurfaceElevated,
    outline = LuxuryLightBorder
)

@Composable
fun MyDailyHisabTheme(
    // Default on very first launch MUST be Light Mode (isDarkMode = false).
    // Restores user preference from DataStore on every launch thereafter.
    // Does NOT follow system theme automatically.
    isDarkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkMode) LuxuryDarkColorScheme else LuxuryLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
