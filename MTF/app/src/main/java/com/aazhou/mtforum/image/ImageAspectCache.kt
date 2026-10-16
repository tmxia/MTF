package com.aazhou.mtforum.image

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope









object ImageAspectCache {

    private val aspects = mutableStateMapOf<String, Float>()

    
    fun aspectOf(url: String): Float? = aspects[url]

    




    suspend fun prefetch(context: Context, loader: ImageLoader, urls: List<String>) {
        val pending = urls.filter { it.isNotBlank() && !aspects.containsKey(it) }.distinct()
        if (pending.isEmpty()) return
        val resolved = coroutineScope {
            pending.chunked(PREFETCH_PARALLELISM)
                .flatten()
                .map { url -> async(Dispatchers.IO) { url to decodeAspect(context, loader, url) } }
                .awaitAll()
        }
        resolved.forEach { (url, aspect) ->
            if (aspect != null) aspects[url] = aspect
        }
    }

    private suspend fun decodeAspect(context: Context, loader: ImageLoader, url: String): Float? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(PREFETCH_SAMPLE_PX)
            .build()
        val result = runCatching { loader.execute(request) }.getOrNull() as? SuccessResult ?: return null
        return imageAspectRatio(result.drawable.intrinsicWidth, result.drawable.intrinsicHeight)
    }
}


private const val MIN_ASPECT = 0.8f


private const val PREFETCH_SAMPLE_PX = 256


private const val PREFETCH_PARALLELISM = 4


internal fun imageAspectRatio(width: Int, height: Int): Float? =
    if (width > 0 && height > 0) (width.toFloat() / height).coerceAtLeast(MIN_ASPECT) else null
