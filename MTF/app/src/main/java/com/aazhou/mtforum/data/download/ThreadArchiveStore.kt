package com.aazhou.mtforum.data.download

import android.content.Context
import android.net.Uri
import com.aazhou.mtforum.data.export.sanitizeContentHtml
import com.aazhou.mtforum.data.model.ThreadSnapshot
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json









class ThreadArchiveStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private fun archiveDir(tid: String): File = File(context.filesDir, "thread_downloads/$tid")

    suspend fun archive(
        tid: String,
        snapshot: ThreadSnapshot,
        images: Map<String, ByteArray>,
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val dir = archiveDir(tid)
        
        dir.deleteRecursively()
        val imagesDir = File(dir, "images").apply { mkdirs() }
        val localOf = HashMap<String, String>()
        images.forEach { (url, bytes) ->
            val file = File(imagesDir, imageFileName(url))
            file.writeBytes(bytes)
            localOf[url] = Uri.fromFile(file).toString()
        }
        val localSnapshot = snapshot.copy(
            posts = snapshot.posts.map { post ->
                post.copy(
                    avatarUrl = post.avatarUrl?.let { localOf[it] ?: it },
                    contentHtml = sanitizeContentHtml(post.contentHtml) { localOf[it] },
                )
            },
        )
        File(dir, "snapshot.json").writeText(
            json.encodeToString(ThreadSnapshot.serializer(), localSnapshot),
        )
        localOf
    }

    suspend fun read(tid: String): ThreadSnapshot? = withContext(Dispatchers.IO) {
        val file = File(archiveDir(tid), "snapshot.json")
        if (!file.isFile) return@withContext null
        runCatching {
            json.decodeFromString(ThreadSnapshot.serializer(), file.readText())
        }.getOrNull()
    }

    suspend fun exists(tid: String): Boolean = withContext(Dispatchers.IO) {
        File(archiveDir(tid), "snapshot.json").isFile
    }

    suspend fun delete(tid: String) {
        withContext(Dispatchers.IO) { archiveDir(tid).deleteRecursively() }
    }

    
    private fun imageFileName(url: String): String {
        val ext = url.substringBefore('?').substringBefore('#').substringAfterLast('.', "jpg")
            .take(5).lowercase()
        val digest = MessageDigest.getInstance("SHA-1")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(20)
        return "$digest.$ext"
    }
}
