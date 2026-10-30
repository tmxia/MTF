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
            .data(fixUrl(url))
            .crossfade(crossfade)
            .allowHardware(true)
            .size(maxPx)
            .build()
    }
}


// 统一处理 URL，补全缺少的协议头
internal fun fixUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val trimmed = url.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || 
        trimmed.startsWith("file://") || trimmed.startsWith("content://") || 
        trimmed.startsWith("data:")) {
        return trimmed
    }
    if (trimmed.startsWith("//")) return "https:$trimmed"
    return "https://bbs.binmt.cc/" + trimmed.removePrefix("/")
}
