package com.stressless.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = StressTeal,
    onPrimary = Color.White,
    primaryContainer = StressAquaLight,
    onPrimaryContainer = StressNavy,

    secondary = StressBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4F1FF),
    onSecondaryContainer = StressNavy,

    tertiary = StressSuccess,
    onTertiary = Color.White,

    background = StressBackground,
    onBackground = StressTextPrimary,

    surface = StressSurface,
    onSurface = StressTextPrimary,

    surfaceVariant = Color(0xFFF0F7F9),
    onSurfaceVariant = StressTextSecondary,

    error = StressDanger,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = StressTeal,
    onPrimary = StressNavyDark,

    secondary = Color(0xFF80CFFF),
    onSecondary = StressNavyDark,

    background = StressNavyDark,
    onBackground = Color.White,

    surface = StressNavy,
    onSurface = Color.White,

    surfaceVariant = Color(0xFF102B4E),
    onSurfaceVariant = Color(0xFFD7E5EF),

    error = StressDanger,
    onError = Color.White
)

@Composable
fun StressLessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}