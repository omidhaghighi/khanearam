package com.khanearam.shop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// تم صورتی و بنفش ملایم
object Brand {
    val violet = Color(0xFF8E63C9)
    val violetDeep = Color(0xFF6F46AE)
    val pink = Color(0xFFE8789F)
    val pinkSoft = Color(0xFFF6B6CF)
    val lavender = Color(0xFFD9C8F2)

    val headerGradient = Brush.linearGradient(listOf(Color(0xFF8E63C9), Color(0xFFC77BC0), Color(0xFFEB84A8)))
    val promo1 = Brush.linearGradient(listOf(Color(0xFF8E63C9), Color(0xFFB486E0)))
    val promo2 = Brush.linearGradient(listOf(Color(0xFFE8789F), Color(0xFFF2A3BF)))
    val promo3 = Brush.linearGradient(listOf(Color(0xFF7A74D6), Color(0xFFE08AC0)))
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFF8E63C9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDF9),
    onPrimaryContainer = Color(0xFF3B1F63),
    secondary = Color(0xFFE8789F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0EB),
    onSecondaryContainer = Color(0xFF5A1631),
    tertiary = Color(0xFFB48AE0),
    background = Color(0xFFFCF7FD),
    onBackground = Color(0xFF2A1F33),
    surface = Color(0xFFFCF7FD),
    onSurface = Color(0xFF2A1F33),
    surfaceVariant = Color(0xFFF5ECF9),
    onSurfaceVariant = Color(0xFF4D4256),
    outline = Color(0xFFCDB8DD)
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFD0B4F2),
    onPrimary = Color(0xFF38205C),
    primaryContainer = Color(0xFF50357A),
    onPrimaryContainer = Color(0xFFEADDF9),
    secondary = Color(0xFFF4A6C3),
    onSecondary = Color(0xFF55193A),
    secondaryContainer = Color(0xFF6D2B4C),
    onSecondaryContainer = Color(0xFFFFD9E6),
    tertiary = Color(0xFFD0B4F2),
    background = Color(0xFF1B1520),
    onBackground = Color(0xFFEEE3F3),
    surface = Color(0xFF1B1520),
    onSurface = Color(0xFFEEE3F3),
    surfaceVariant = Color(0xFF2B2234),
    onSurfaceVariant = Color(0xFFE2D5EA),
    outline = Color(0xFF7C6A8C)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(30.dp)
        ),
        content = content
    )
}
