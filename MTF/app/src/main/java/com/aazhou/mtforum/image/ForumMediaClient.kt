package com.aazhou.mtforum.image

import com.aazhou.mtforum.data.network.CDN_IP_RATE_LIMITED
import com.aazhou.mtforum.data.network.RateLimiter
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit



















fun forumMediaClient(
    userAgent: String,
    cookieHeader: () -> String,
): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        
        .addInterceptor(MediaRetryInterceptor(filter = { shouldRateLimitMedia(it.url) }))
        .addInterceptor(
            RateLimiter(
                minIntervalMs = MEDIA_MIN_INTERVAL_MS,
                backoffCodes = setOf(CDN_IP_RATE_LIMITED),
                filter = { shouldRateLimitMedia(it.url) },
            ),
        )
        .addInterceptor { chain ->
            val request = chain.request()
            val host = request.url.host
            val builder: Request.Builder = request.newBuilder()
                .header("User-Agent", userAgent)
                .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
            if (host.endsWith("binmt.cc")) {
                builder.header("Referer", "https://bbs.binmt.cc/")
                val cookie = cookieHeader()
                if (cookie.isNotEmpty()) builder.header("Cookie", cookie)
            }
            chain.proceed(builder.build())
        }
        .build()







private class MediaRetryInterceptor(
    private val filter: (Request) -> Boolean = { true },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!filter(request)) {
            return chain.proceed(request)
        }
        var retries = 0
        while (true) {
            val response = chain.proceed(request)
            if (retries >= MAX_RETRIES || response.code !in RETRY_CODES) return response
            
            response.close()
            retries++
            Thread.sleep(BACKOFF_MS * retries)
        }
    }

    private companion object {
        const val MAX_RETRIES = 2
        const val BACKOFF_MS = 500L
        val RETRY_CODES = setOf(429, 503, CDN_IP_RATE_LIMITED)
    }
}






internal fun shouldRateLimitMedia(url: HttpUrl): Boolean {
    val path = url.encodedPath
    if (path.contains("/static/image/") || path.contains("/smiley/")) return false
    if (url.host.contains("avatar.")) return false
    return path.contains("forum.php") ||
        url.queryParameter("mod") in setOf("image", "attachment") ||
        path.contains("/attachment/")
}






private const val MEDIA_MIN_INTERVAL_MS = 150L
