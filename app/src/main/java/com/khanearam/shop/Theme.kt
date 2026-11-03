package com.khanearam.shop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val lightScheme = lightColorScheme(
    primary = Color(0xFF7B3F61),
    onPrimary = Color.White,
    secondary = Color(0xFFB57A98),
    background = Color(0xFFFBF6F8),
    surface = Color(0xFFFBF6F8),
    surfaceVariant = Color(0xFFF2E6EC)
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFE0A3C4),
    onPrimary = Color(0xFF3A1229),
    secondary = Color(0xFFC79AB3),
    background = Color(0xFF1B1418),
    onBackground = Color(0xFFEADFE4),
    surface = Color(0xFF1B1418),
    onSurface = Color(0xFFEADFE4),
    surfaceVariant = Color(0xFF2C2227),
    onSurfaceVariant = Color(0xFFEADFE4)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme, content = content)
}
