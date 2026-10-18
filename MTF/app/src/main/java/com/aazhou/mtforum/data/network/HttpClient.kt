package com.aazhou.mtforum.data.network

import com.aazhou.mtforum.data.log.LogLevel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit











data class FetchedPage(val body: String, val etag: String?)

class HttpClient(
    val baseUrl: String = BASE_URL,
    val cookieStore: CookieStore = CookieStore(),
) {

    val host: String = baseUrl.toHttpUrlOrNull()?.host ?: "bbs.binmt.cc"

    private val rateLimiter = RateLimiter()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .cookieJar(cookieStore)
        .addInterceptor(rateLimiter)
        .build()

    private val _pendingVerification = MutableStateFlow(false)

    
    val pendingVerificationFlow: StateFlow<Boolean> = _pendingVerification.asStateFlow()

    private var pendingVerification: CompletableDeferred<String?>? = null

    





    var onSessionChanged: (() -> Unit)? = null

    



    var onLog: ((LogLevel, String, String, Throwable?) -> Unit)? = null

    
    val verificationUrl: String get() = baseUrl

    fun isLoggedIn(): Boolean =
        cookieStore.cookies(host).any { it.name.endsWith("_auth") && it.value.isNotEmpty() }

    fun cookieHeader(): String = cookieStore.cookieHeader(host)

    fun url(path: String): String =
        if (path.startsWith("http")) path else baseUrl + path.removePrefix("/")

    @Throws(IOException::class)
    suspend fun get(url: String, userAgent: String = MOBILE_UA): String =
        getWithFinalUrl(url, userAgent).second

    





    @Throws(IOException::class)
    suspend fun getWithFinalUrl(url: String, userAgent: String = MOBILE_UA): Pair<String, String> {
        var latest = try {
            executeGet(url, userAgent)
        } catch (e: IOException) {
            onLog?.invoke(LogLevel.ERROR, TAG, "GET 失败：$url", e)
            throw e
        }
        if (!WafSolver.looksLikeChallenge(latest.body)) return latest.finalUrl to latest.body

        onLog?.invoke(LogLevel.WARN, TAG, "命中 WAF 挑战，尝试本地求解：$url", null)
        runCatching { WafSolver.solve(latest.body) }.getOrNull()?.let { value ->
            cookieStore.setCookie(host, WafSolver.COOKIE_NAME, value)
            val retried = executeGet(url, userAgent)
            if (!WafSolver.looksLikeChallenge(retried.body)) {
                onLog?.invoke(LogLevel.INFO, TAG, "WAF 本地求解通过：$url", null)
                return retried.finalUrl to retried.body
            }
            latest = retried
        }

        
        onLog?.invoke(LogLevel.WARN, TAG, "WAF 本地求解失配，需要真人验证：$url", null)
        val verifiedCookie = awaitVerification()
        if (verifiedCookie != null) {
            applyCookieHeader(verifiedCookie)
            onLog?.invoke(LogLevel.INFO, TAG, "真人验证 cookie 已回灌：$url", null)
            val retried = executeGet(url, userAgent)
            return retried.finalUrl to retried.body
        }
        onLog?.invoke(LogLevel.WARN, TAG, "真人验证未完成（超时或取消），按挑战页返回：$url", null)
        return latest.finalUrl to latest.body
    }

    







    @Throws(IOException::class)
    suspend fun getIfChanged(
        url: String,
        etag: String?,
        userAgent: String = MOBILE_UA,
    ): FetchedPage? {
        var latest = try {
            executeGet(url, userAgent, etag)
        } catch (e: IOException) {
            onLog?.invoke(LogLevel.ERROR, TAG, "条件 GET 失败：$url", e)
            throw e
        }
        if (WafSolver.looksLikeChallenge(latest.body)) {
            onLog?.invoke(LogLevel.WARN, TAG, "命中 WAF 挑战，尝试本地求解：$url", null)
            runCatching { WafSolver.solve(latest.body) }.getOrNull()?.let { value ->
                cookieStore.setCookie(host, WafSolver.COOKIE_NAME, value)
                latest = executeGet(url, userAgent, etag)
            }
            if (WafSolver.looksLikeChallenge(latest.body)) {
                onLog?.invoke(LogLevel.WARN, TAG, "WAF 本地求解失配：$url", null)
                throw IOException("WAF 挑战未能通过：$url")
            }
            onLog?.invoke(LogLevel.INFO, TAG, "WAF 本地求解通过：$url", null)
        }
        return if (latest.notModified) null else FetchedPage(latest.body, latest.etag)
    }

    @Throws(IOException::class)
    fun post(url: String, params: Map<String, String>, referer: String? = null): String {
        val form = FormBody.Builder()
        for ((k, v) in params) form.add(k, v)
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", MOBILE_UA)
            .header("Accept", ACCEPT)
            .header("Accept-Language", ACCEPT_LANGUAGE)
            .header("Content-Type", "application/x-www-form-urlencoded")
        if (!referer.isNullOrEmpty()) builder.header("Referer", referer)
        val request = builder.post(form.build()).build()
        fun execute(): String =
            client.newCall(request).execute().use { response ->
                
                
                if (url.contains("loginsubmit")) {
                    val names = response.headers("Set-Cookie")
                        .map { it.substringBefore('=') }
                        .joinToString(",")
                    onLog?.invoke(
                        LogLevel.INFO,
                        TAG,
                        "登录 POST：HTTP ${response.code} 终址=${response.request.url} Set-Cookie=[$names]",
                        null,
                    )
                }
                response.body?.string() ?: ""
            }
        var body = execute()
        
        
        
        if (WafSolver.looksLikeChallenge(body)) {
            onLog?.invoke(LogLevel.WARN, TAG, "写操作命中 WAF 挑战，求解后重放：$url", null)
            runCatching { WafSolver.solve(body) }.getOrNull()?.let { cookieStore.setCookie(host, WafSolver.COOKIE_NAME, it) }
            val retried = execute()
            if (!WafSolver.looksLikeChallenge(retried)) {
                onLog?.invoke(LogLevel.INFO, TAG, "写操作 WAF 求解通过，已重放：$url", null)
                body = retried
            }
        }
        return body
    }

    






    @Throws(IOException::class)
    fun redirectLocation(url: String, userAgent: String = MOBILE_UA): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", ACCEPT)
            .header("Accept-Language", ACCEPT_LANGUAGE)
            .get()
            .build()
        client.newBuilder().followRedirects(false).build()
            .newCall(request).execute().use { response ->
                if (!response.isRedirect) return null
                val location = response.header("Location") ?: return null
                
                return if (location.startsWith("http")) {
                    location
                } else {
                    response.request.url.newBuilder().encodedPath(location.removePrefix("/")).build()
                        .toString()
                }
            }
    }

    @Throws(IOException::class)
    fun getBytes(url: String, referer: String? = null): ByteArray {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", MOBILE_UA)
        if (!referer.isNullOrEmpty()) builder.header("Referer", referer)
        return client.newCall(builder.build()).execute().use { it.body?.bytes() ?: ByteArray(0) }
    }

    
    @Throws(IOException::class)
    fun upload(
        url: String,
        file: File,
        fieldName: String = "Filedata",
        extraFields: Map<String, String> = emptyMap(),
        userAgent: String = MOBILE_UA,
    ): String {
        if (!file.isFile || file.length() <= 0L) throw IOException("上传文件不存在或为空")
        val mediaType = guessContentType(file).toMediaTypeOrNull()
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(fieldName, file.name, file.asRequestBody(mediaType))
        for ((k, v) in extraFields) builder.addFormDataPart(k, v)
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", ACCEPT)
            .header("Referer", baseUrl)
            .post(builder.build())
        val cookie = cookieHeader()
        if (cookie.isNotEmpty()) requestBuilder.header("Cookie", cookie)
        return client.newCall(requestBuilder.build()).execute().use {
            val body = it.body?.string() ?: ""
            if (!it.isSuccessful) {
                onLog?.invoke(LogLevel.ERROR, TAG, "上传失败（HTTP ${it.code}）：$url", null)
                throw IOException("上传失败（HTTP ${it.code}）")
            }
            body
        }
    }

    
    fun completeVerification(cookieHeader: String?) {
        val deferred = synchronized(this) { pendingVerification }
        if (cookieHeader.isNullOrBlank()) {
            _pendingVerification.value = false
            deferred?.complete(null)
            onLog?.invoke(LogLevel.WARN, TAG, "用户取消了真人验证", null)
        } else {
            deferred?.complete(cookieHeader)
            
            onLog?.invoke(LogLevel.INFO, TAG, "真人验证完成，cookie 已回灌", null)
            onSessionChanged?.invoke()
        }
    }

    
    fun applyCookieHeader(header: String) {
        for (pair in header.split(";")) {
            val eq = pair.indexOf('=')
            if (eq <= 0) continue
            val name = pair.substring(0, eq).trim()
            val value = pair.substring(eq + 1).trim()
            if (name.isEmpty() || value.isEmpty()) continue
            cookieStore.setCookie(host, name, value)
        }
    }

    private suspend fun awaitVerification(): String? {
        val deferred = synchronized(this) {
            pendingVerification ?: CompletableDeferred<String?>().also {
                pendingVerification = it
                _pendingVerification.value = true
            }
        }
        return try {
            withTimeoutOrNull(VERIFY_TIMEOUT_MS) { deferred.await() }
        } finally {
            synchronized(this) {
                if (pendingVerification === deferred) {
                    pendingVerification = null
                    _pendingVerification.value = false
                }
            }
        }
    }


private data class GetResult(
    val finalUrl: String,
    val body: String,
    val etag: String?,
    val notModified: Boolean,
)

    private fun executeGet(url: String, userAgent: String, etag: String? = null): GetResult {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", ACCEPT)
            .header("Accept-Language", ACCEPT_LANGUAGE)
            .get()
        if (!etag.isNullOrEmpty()) builder.header("If-None-Match", etag)
        val request = builder.build()
        return client.newCall(request).execute().use { response ->
            GetResult(
                
                finalUrl = response.request.url.toString(),
                body = response.body?.string() ?: "",
                etag = response.header("ETag"),
                notModified = response.code == HTTP_NOT_MODIFIED,
            )
        }
    }

    private fun guessContentType(file: File): String = when {
        file.name.endsWith(".png", true) -> "image/png"
        file.name.endsWith(".gif", true) -> "image/gif"
        file.name.endsWith(".webp", true) -> "image/webp"
        file.name.endsWith(".jpg", true) || file.name.endsWith(".jpeg", true) -> "image/jpeg"
        else -> "application/octet-stream"
    }

    companion object {
        const val BASE_URL = "https://bbs.binmt.cc/"
        const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        private const val ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        private const val ACCEPT_LANGUAGE = "zh-CN,zh;q=0.9,en;q=0.8"

        private const val VERIFY_TIMEOUT_MS = 5 * 60 * 1000L

        private const val HTTP_NOT_MODIFIED = 304

        
        private const val TAG = "HttpClient"

        fun httpUrl(url: String): HttpUrl? = url.toHttpUrlOrNull()
    }
}
