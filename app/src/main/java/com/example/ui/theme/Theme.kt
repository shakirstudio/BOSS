package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BmsPrimaryDark,
    onPrimary = Color(0xFF001F4E),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = BmsSecondary,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = BmsTertiary,
    onTertiary = Color(0xFF452B00),
    background = BmsDarkBackground,
    onBackground = BmsDarkTextPrimary,
    surface = BmsDarkSurface,
    onSurface = BmsDarkTextPrimary,
    surfaceVariant = BmsDarkSurfaceVariant,
    onSurfaceVariant = BmsDarkTextSecondary,
    outline = BmsDarkOutline,
    error = BmsError
)

private val LightColorScheme = lightColorScheme(
    primary = BmsPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = BmsSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = BmsTertiary,
    onTertiary = Color.White,
    background = BmsLightBackground,
    onBackground = BmsLightTextPrimary,
    surface = BmsLightSurface,
    onSurface = BmsLightTextPrimary,
    surfaceVariant = BmsLightSurfaceVariant,
    onSurfaceVariant = BmsLightTextSecondary,
    outline = BmsLightOutline,
    error = BmsError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek executive dark mode for creative studio aesthetic
    dynamicColor: Boolean = false, // Keep consistent high-end BMS branding
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
