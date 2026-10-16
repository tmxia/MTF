package com.aazhou.mtforum.image

import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder







class MtfImageLoaderFactory(
    private val context: Context,
    private val userAgent: String,
    private val cookieHeader: () -> String,
) : ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(context)
            .okHttpClient {
                forumMediaClient(userAgent = userAgent, cookieHeader = cookieHeader)
            }
            .components {
                add(ImageDecoderDecoder.Factory())
            }
            .crossfade(true)
            .build()
}
