package com.aazhou.mtforum.image

import com.aazhou.mtforum.data.network.CDN_IP_RATE_LIMITED
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

fun forumMediaClient(
    userAgent: String,
    cookieHeader: () -> String,
): OkHttpClient {
    
    val dispatcher = Dispatcher().apply {
        maxRequests = 4          
        maxRequestsPerHost = 2   
    }

    return OkHttpClient.Builder()
        .dispatcher(dispatcher)
        .retryOnConnectionFailure(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            val host = request.url.host
            
            
            
            val referer = when {
                request.url.toString().contains("forum.php") -> "https://bbs.binmt.cc/forum.php"
                request.url.toString().contains("thread-") -> "https://bbs.binmt.cc/thread-${extractTid(request)}-1-1.html"
                else -> "https://bbs.binmt.cc/"
            }

            val builder: Request.Builder = request.newBuilder()
                .header("User-Agent", userAgent)
                .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .header("Origin", "https://bbs.binmt.cc")

            if (host.endsWith("binmt.cc")) {
                builder.header("Referer", "https://bbs.binmt.cc/forum.php")
                val cookie = cookieHeader()
                if (cookie.isNotEmpty()) builder.header("Cookie", cookie)
            }
            
            chain.proceed(builder.build())
        }
        .build()
}


private fun extractTid(request: Request): String {
    val url = request.url.toString()
    val match = Regex("thread-(\\d+)").find(url)
    return match?.groupValues?.get(1) ?: ""
}
