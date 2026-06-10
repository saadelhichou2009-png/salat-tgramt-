package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IslamicColorScheme = darkColorScheme(
    primary = PrimaryGold,
    secondary = AccentGold,
    tertiary = EmeraldGreen,
    background = DarkStarryBg,
    surface = DarkSurfaceCard,
    onPrimary = Color(0xFF451A03), // Brownish charcoal for contrast on gold
    onSecondary = Color(0xFF451A03),
    onTertiary = Color.White,
    onBackground = LightText,
    onSurface = LightText,
    error = AccentRed,
    outline = MutedText
)

@Composable
fun AdhanTaghramtTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IslamicColorScheme,
        typography = Typography,
        content = content
    )
}
