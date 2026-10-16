package com.aazhou.mtforum.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color






const val DarkBrightTextAlpha = 0.69f





const val DarkImageDimAlpha = 0.32f






@Composable
fun bodyTextColor(): Color =
    LocalMtfColors.current.textPrimary.dimmedForDark()







@Composable
fun accentTextColor(): Color =
    MaterialTheme.colorScheme.primary.dimmedForDark()

@Composable
private fun Color.dimmedForDark(): Color =
    if (LocalIsDarkTheme.current) copy(alpha = DarkBrightTextAlpha) else this





val LocalDimImagesInDark = staticCompositionLocalOf { true }








@Composable
fun Modifier.dimImageInDark(): Modifier {
    if (!LocalIsDarkTheme.current || !LocalDimImagesInDark.current) return this
    val scrim = LocalMtfColors.current.background
    return drawWithContent {
        drawContent()
        drawRect(color = scrim, alpha = DarkImageDimAlpha)
    }
}
