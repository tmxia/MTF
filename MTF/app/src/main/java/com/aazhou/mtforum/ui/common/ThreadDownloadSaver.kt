package com.aazhou.mtforum.ui.common

import android.content.Context
import android.graphics.BitmapFactory
import com.aazhou.mtforum.data.download.ThreadArchiveStore
import com.aazhou.mtforum.data.export.buildThreadHtml
import com.aazhou.mtforum.data.export.collectContentImageUrls
import com.aazhou.mtforum.data.export.dataUriOf
import com.aazhou.mtforum.data.model.DownloadedThread
import com.aazhou.mtforum.data.model.ThreadPage
import com.aazhou.mtforum.data.model.ThreadSnapshot
import com.aazhou.mtforum.data.network.HttpClient
import com.aazhou.mtforum.data.repo.DownloadStore
import com.aazhou.mtforum.image.forumMediaClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request


private const val DOWNLOAD_PARALLELISM = 4


private const val FILE_NAME_SUBJECT_LIMIT = 40












internal suspend fun saveThreadDownload(
    context: Context,
    page: ThreadPage,
    downloadStore: DownloadStore,
    archiveStore: ThreadArchiveStore,
): DownloadedThread? {
    val images = downloadImageBytes(context, collectImageUrls(page))
    val localOf = archiveStore.archive(
        tid = page.tid,
        snapshot = page.toSnapshot(),
        images = images,
    )
    
    val html = buildThreadHtml(page) { url -> images[url]?.let { dataUriOf(url, it) } }
    val fileName = "MTF-${page.tid}-${safeFileName(page.subject)}.html"
    val uri = saveDocument(context, fileName, html.toByteArray()) ?: return null
    val entry = DownloadedThread(
        tid = page.tid,
        subject = page.subject?.takeIf { it.isNotBlank() } ?: "MTF 帖子",
        forumName = page.forumName,
        fileName = fileName,
        uri = uri.toString(),
        savedAt = System.currentTimeMillis(),
        replyCount = page.posts.count { !it.isFirstPost },
        sizeBytes = html.length.toLong(),
    )
    downloadStore.upsert(entry)
    return entry
}


private fun collectImageUrls(page: ThreadPage): List<String> {
    val urls = LinkedHashSet<String>()
    page.posts.forEach { post ->
        post.avatarUrl?.takeIf { it.startsWith("http") }?.let(urls::add)
        collectContentImageUrls(post.contentHtml).forEach(urls::add)
    }
    return urls.toList()
}


private suspend fun downloadImageBytes(
    context: Context,
    urls: List<String>,
): Map<String, ByteArray> {
    if (urls.isEmpty()) return emptyMap()
    val client = mediaClient(context)
    val result = LinkedHashMap<String, ByteArray>()
    for (batch in urls.chunked(DOWNLOAD_PARALLELISM)) {
        val fetched = coroutineScope {
            batch.map { url -> async(Dispatchers.IO) { url to fetch(client, url) } }.awaitAll()
        }
        fetched.forEach { (url, bytes) -> if (bytes != null) result[url] = bytes }
    }
    return result
}

private fun mediaClient(context: Context): OkHttpClient {
    val httpClient = context.container().httpClient
    return forumMediaClient(
        userAgent = HttpClient.MOBILE_UA,
        cookieHeader = { httpClient.cookieHeader() },
    )
}

private fun fetch(client: OkHttpClient, url: String): ByteArray? =
    runCatching {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return@use null
            response.body?.bytes()?.takeIf { it.isNotEmpty() }
        }
    }.getOrNull()


private fun safeFileName(subject: String?): String {
    val cleaned = subject.orEmpty()
        .replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), "")
        .trim()
        .take(FILE_NAME_SUBJECT_LIMIT)
    return cleaned.ifBlank { "thread" }
}


private fun ThreadPage.toSnapshot(): ThreadSnapshot = ThreadSnapshot(
    subject = subject,
    forumName = forumName,
    opUid = opUid,
    fid = fid,
    totalReplies = totalReplies,
    posts = posts,
    progress = emptyList(),
)


internal suspend fun deleteThreadDownload(
    tid: String,
    downloadStore: DownloadStore,
    archiveStore: ThreadArchiveStore,
) {
    downloadStore.remove(tid)
    archiveStore.delete(tid)
}

