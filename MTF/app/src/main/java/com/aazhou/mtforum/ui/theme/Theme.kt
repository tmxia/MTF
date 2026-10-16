package com.aazhou.mtforum.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp


val LocalIsDarkTheme = staticCompositionLocalOf { false }


private fun containerOf(primary: Color, darkTheme: Boolean = false) = if (darkTheme) {
    Color(
        red = primary.red * 0.22f + 0.12f,
        green = primary.green * 0.22f + 0.12f,
        blue = primary.blue * 0.22f + 0.12f,
    )
} else {
    Color(
        red = primary.red * 0.22f + 0.78f,
        green = primary.green * 0.22f + 0.78f,
        blue = primary.blue * 0.22f + 0.78f,
    )
}





private const val CustomColorNightLift = 0.18f

@Composable
fun MtfTheme(
    
    customColor: Int? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    
    dimImagesInDark: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkMtfColors else LightMtfColors
    val primary = customColor?.let { Color(it) }?.let { custom ->
        if (darkTheme) lerp(custom, Color.White, CustomColorNightLift) else custom
    } ?: if (darkTheme) DefaultPrimaryNight else DefaultPrimaryDay
    
    
    
    val onSurface = if (darkTheme) {
        colors.textPrimary.copy(alpha = DarkBrightTextAlpha)
    } else {
        colors.textPrimary
    }
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = containerOf(primary, darkTheme = true),
            secondary = primary,
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.quoteBackground,
            onSurface = onSurface,
            onSurfaceVariant = colors.textSecondary,
            outlineVariant = colors.divider,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = containerOf(primary, darkTheme = false),
            secondary = primary,
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.quoteBackground,
            onSurface = onSurface,
            onSurfaceVariant = colors.textSecondary,
            outlineVariant = colors.divider,
        )
    }
    CompositionLocalProvider(
        LocalMtfColors provides colors,
        LocalIsDarkTheme provides darkTheme,
        LocalDimImagesInDark provides dimImagesInDark,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
