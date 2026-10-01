package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TurboOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5A1400),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = TurboCyan,
    onSecondary = Color(0xFF00363A),
    secondaryContainer = Color(0xFF004D54),
    onSecondaryContainer = Color(0xFF99F3FF),
    tertiary = TurboGold,
    onTertiary = Color.Black,
    background = TurboDarkBg,
    onBackground = Color.White,
    surface = TurboSurface,
    onSurface = Color.White,
    surfaceVariant = TurboSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
