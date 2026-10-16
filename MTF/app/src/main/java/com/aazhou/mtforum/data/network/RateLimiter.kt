package com.aazhou.mtforum.data.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.atomic.AtomicLong








const val CDN_IP_RATE_LIMITED = 514


private val DEFAULT_BACKOFF_CODES = setOf(403, 429, CDN_IP_RATE_LIMITED)








class RateLimiter(
    private val minIntervalMs: Long = 400L,
    private val backoffCodes: Set<Int> = DEFAULT_BACKOFF_CODES,
    private val filter: (Request) -> Boolean = { true },
) : Interceptor {

    private val lastRequestAt = AtomicLong(0L)

    @Volatile
    private var blockedUntil = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!filter(request)) {
            return chain.proceed(request)
        }

        val waitForBlock = blockedUntil - System.currentTimeMillis()
        if (waitForBlock > 0) Thread.sleep(waitForBlock)

        synchronized(this) {
            val since = System.currentTimeMillis() - lastRequestAt.get()
            if (since < minIntervalMs) Thread.sleep(minIntervalMs - since)
            lastRequestAt.set(System.currentTimeMillis())
        }

        val response = chain.proceed(request)
        if (response.code in backoffCodes) {
            blockedUntil = System.currentTimeMillis() + 3000
        }
        return response
    }
}
