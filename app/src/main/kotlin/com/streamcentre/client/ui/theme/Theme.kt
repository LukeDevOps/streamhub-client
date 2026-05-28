package com.streamcentre.client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003258),
    background = Color(0xFF0A0A0F),
    surface = Color(0xFF141420),
    onBackground = Color(0xFFE8E8F0),
    onSurface = Color(0xFFE8E8F0),
    surfaceVariant = Color(0xFF1E1E2E),
    onSurfaceVariant = Color(0xFFB0B0C8),
)

@Composable
fun StreamcentreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
