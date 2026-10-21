package com.aazhou.mtforum.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest


internal val SmallImageSize = 72.dp


internal val GridImageSize = 200.dp


private val FullWidthImageSize = 460.dp







@Composable
internal fun smallImageRequest(url: String?, maxSize: Dp = SmallImageSize): ImageRequest =
    sizedImageRequest(url, maxSize, crossfade = true)






@Composable
internal fun postImageRequest(url: String?, maxSize: Dp = FullWidthImageSize): ImageRequest =
    sizedImageRequest(url, maxSize, crossfade = false)

@Composable
private fun sizedImageRequest(url: String?, maxSize: Dp, crossfade: Boolean): ImageRequest {
    val context = LocalContext.current
    val maxPx = with(LocalDensity.current) { maxSize.roundToPx() }
    return remember(url, maxPx, crossfade, context) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(crossfade)
            .allowHardware(true)
            .size(maxPx)
            .build()
    }
}
