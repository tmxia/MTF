package com.aazhou.mtforum.ui.common

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.aazhou.mtforum.data.network.HttpClient
import com.aazhou.mtforum.image.forumMediaClient
import okhttp3.Request
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch








private fun imagesDir(): String = "${Environment.DIRECTORY_PICTURES}/MTF"
private fun videosDir(): String = "${Environment.DIRECTORY_MOVIES}/MTF"
internal fun downloadBytes(context: Context, url: String): ByteArray =
    context.container().httpClient.getBytes(url, referer = "https://bbs.binmt.cc/")

internal fun mimeOf(url: String): String = when {
    url.contains(".png", true) -> "image/png"
    url.contains(".gif", true) -> "image/gif"
    url.contains(".webp", true) -> "image/webp"
    else -> "image/jpeg"
}


internal fun videoMimeOf(url: String): String = when {
    url.contains(".webm", true) -> "video/webm"
    url.contains(".mkv", true) -> "video/x-matroska"
    url.contains(".m3u8", true) -> "application/vnd.apple.mpegurl"
    else -> "video/mp4"
}

internal fun fileNameOf(url: String, fallbackExtension: String = "jpg"): String {
    val name = url.substringBefore('?').substringAfterLast('/')
    return if (name.contains('.')) name else "mtf_${System.currentTimeMillis()}.$fallbackExtension"
}


internal fun saveToGallery(context: Context, url: String, bytes: ByteArray): Uri? =
    saveBytes(context, url, bytes, imagesDir(), mimeOf(url))





internal fun saveDocument(context: Context, fileName: String, bytes: ByteArray): Uri? =
    saveBytes(context, fileName, bytes, "${Environment.DIRECTORY_DOWNLOADS}/MTF", "text/html")







internal fun saveTextDocument(context: Context, fileName: String, text: String): Uri? =
    saveBytes(context, fileName, text.toByteArray(), "${Environment.DIRECTORY_DOWNLOADS}/MTF/Logs", "text/plain")


internal fun deleteDocument(context: Context, uri: Uri?) {
    if (uri == null) return
    runCatching { context.contentResolver.delete(uri, null, null) }
}

private fun saveBytes(
    context: Context,
    url: String,
    bytes: ByteArray,
    relativePath: String,
    mime: String,
): Uri? {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileNameOf(url, mime.substringAfter('/')))
        put(MediaStore.MediaColumns.MIME_TYPE, mime)
        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values) ?: return null
    return try {
        resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return null
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        uri
    } catch (_: Exception) {
        resolver.delete(uri, null, null)
        null
    }
}


internal fun saveVideoToGallery(context: Context, url: String): Uri? {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileNameOf(url, "mp4"))
        put(MediaStore.MediaColumns.MIME_TYPE, videoMimeOf(url))
        put(MediaStore.MediaColumns.RELATIVE_PATH, videosDir())
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    val ok = try {
        copyToUri(context, url, uri)
    } catch (_: Exception) {
        false
    }
    if (!ok) {
        resolver.delete(uri, null, null)
        return null
    }
    values.clear()
    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
    resolver.update(uri, values, null, null)
    return uri
}







internal fun saveAttachment(context: Context, fileName: String, url: String): Uri? {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, attachmentMimeOf(fileName))
        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/MTF")
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values) ?: return null
    val ok = try {
        copyToUri(context, url, uri)
    } catch (_: Exception) {
        false
    }
    if (!ok) {
        resolver.delete(uri, null, null)
        return null
    }
    values.clear()
    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
    resolver.update(uri, values, null, null)
    return uri
}


private fun attachmentMimeOf(fileName: String): String = when {
    fileName.endsWith(".png", true) -> "image/png"
    fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "image/jpeg"
    fileName.endsWith(".gif", true) -> "image/gif"
    fileName.endsWith(".webp", true) -> "image/webp"
    fileName.endsWith(".mp4", true) -> "video/mp4"
    fileName.endsWith(".apk", true) -> "application/vnd.android.package-archive"
    fileName.endsWith(".zip", true) -> "application/zip"
    fileName.endsWith(".txt", true) -> "text/plain"
    fileName.endsWith(".pdf", true) -> "application/pdf"
    else -> "application/octet-stream"
}


private fun copyToUri(context: Context, url: String, uri: Uri): Boolean {
    val httpClient = context.container().httpClient
    val client = forumMediaClient(
        userAgent = HttpClient.MOBILE_UA,
        cookieHeader = { httpClient.cookieHeader() },
    )
    val request = Request.Builder().url(url).build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return false
        val body = response.body ?: return false
        context.contentResolver.openOutputStream(uri)?.use { out ->
            body.byteStream().copyTo(out)
            out.flush()
        } ?: return false
    }
    return true
}


internal fun writeCacheFile(context: Context, url: String, bytes: ByteArray): File {
    val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
    return File(dir, fileNameOf(url)).apply { writeBytes(bytes) }
}


internal fun writeVideoCacheFile(context: Context, url: String): File? {
    val dir = File(context.cacheDir, "shared_videos").apply { mkdirs() }
    val target = File(dir, fileNameOf(url, "mp4"))
    if (target.exists()) return target
    return try {
        val httpClient = context.container().httpClient
        val client = forumMediaClient(
            userAgent = HttpClient.MOBILE_UA,
            cookieHeader = { httpClient.cookieHeader() },
        )
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body ?: return null
            target.outputStream().use { body.byteStream().copyTo(it) }
        }
        target
    } catch (_: Exception) {
        target.delete()
        null
    }
}

internal fun shareImage(context: Context, url: String, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeOf(url)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享"))
}

internal fun shareVideo(context: Context, url: String, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = videoMimeOf(url)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享"))
}


internal fun runBusy(
    scope: CoroutineScope,
    setBusy: (Boolean) -> Unit,
    errorMessage: String,
    action: suspend () -> Boolean,
) {
    scope.launch {
        setBusy(true)
        val ok = runCatching { action() }.getOrDefault(false)
        setBusy(false)
        if (!ok) AppToast.show(errorMessage)
    }
}
