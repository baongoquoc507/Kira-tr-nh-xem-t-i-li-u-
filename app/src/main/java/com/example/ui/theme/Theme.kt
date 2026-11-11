package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Liquid Glass Theme Configuration
class LiquidGlassConfig(
    glassOpacity: Float = 0.72f,
    reduceTransparency: Boolean = false,
    isDarkTheme: Boolean = false,
    accentColor: Color = IosBlue
) {
    var glassOpacity by mutableFloatStateOf(glassOpacity)
    var reduceTransparency by mutableStateOf(reduceTransparency)
    var isDarkTheme by mutableStateOf(isDarkTheme)
    var accentColor by mutableStateOf(accentColor)

    val surfaceColor: Color
        get() {
            if (reduceTransparency) {
                return if (isDarkTheme) Color(0xFF1E2430) else Color(0xFFF3F4F6)
            }
            val alpha = glassOpacity.coerceIn(0.15f, 0.95f)
            return if (isDarkTheme) {
                Color(0xFF121824).copy(alpha = alpha)
            } else {
                Color(0xFFFFFFFF).copy(alpha = alpha)
            }
        }

    val borderColor: Color
        get() = if (isDarkTheme) {
            Color.White.copy(alpha = if (reduceTransparency) 0.12f else 0.22f)
        } else {
            Color.White.copy(alpha = if (reduceTransparency) 0.35f else 0.65f)
        }

    val contentPrimary: Color
        get() = if (isDarkTheme) Color(0xFFF9FAFB) else Color(0xFF111827)

    val contentSecondary: Color
        get() = if (isDarkTheme) Color(0xFF9CA3AF) else Color(0xFF6B7280)
}

val LocalLiquidGlass = compositionLocalOf { LiquidGlassConfig() }

private val DarkColorScheme = darkColorScheme(
    primary = IosBlueDark,
    onPrimary = Color.White,
    primaryContainer = IosBlueDark.copy(alpha = 0.25f),
    onPrimaryContainer = Color.White,
    secondary = IosCyan,
    onSecondary = Color.Black,
    background = DarkGlassBackground,
    onBackground = Color(0xFFF9FAFB),
    surface = DarkGlassSurface,
    onSurface = Color(0xFFF9FAFB),
    surfaceVariant = DarkGlassSubtle,
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = DarkGlassBorder
)

private val LightColorScheme = lightColorScheme(
    primary = IosBlue,
    onPrimary = Color.White,
    primaryContainer = IosBlue.copy(alpha = 0.15f),
    onPrimaryContainer = IosBlue,
    secondary = IosCyan,
    onSecondary = Color.White,
    background = LightGlassBackground,
    onBackground = Color(0xFF111827),
    surface = LightGlassSurface,
    onSurface = Color(0xFF111827),
    surfaceVariant = LightGlassSubtle,
    onSurfaceVariant = Color(0xFF6B7280),
    outline = LightGlassBorder
)

@Composable
fun KiraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    glassOpacity: Float = 0.72f,
    reduceTransparency: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val liquidGlassConfig = LiquidGlassConfig(
        glassOpacity = glassOpacity,
        reduceTransparency = reduceTransparency,
        isDarkTheme = darkTheme,
        accentColor = if (darkTheme) IosBlueDark else IosBlue
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalLiquidGlass provides liquidGlassConfig
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
