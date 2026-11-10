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
    primary = SchoolGreenAccent,
    onPrimary = Color.White,
    secondary = SchoolGold,
    onSecondary = DarkBackground,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFE2E8F0)
)

private val LightColorScheme = lightColorScheme(
    primary = SchoolGreenPrimary,
    onPrimary = Color.White,
    secondary = SchoolGold,
    onSecondary = Color.Black,
    tertiary = PrimaryIndigo,
    background = LightBackground,
    surface = LightSurface,
    onBackground = DarkBackground,
    onSurface = DarkBackground,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to prioritize authentic school identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
