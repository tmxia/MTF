package com.aazhou.mtforum.data.cache

import java.io.File
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json










class ContentCache(
    private val dir: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
) {

    private val json = Json { ignoreUnknownKeys = true }

    






    suspend fun <T> read(
        key: String,
        ttlMillis: Long,
        allowExpired: Boolean = false,
        decode: (String) -> T?,
    ): T? =
        withContext(Dispatchers.IO) {
            val file = fileFor(key)
            if (!file.isFile) return@withContext null
            val entry = runCatching {
                json.decodeFromString<CacheEntry>(file.readText())
            }.getOrNull()
            if (entry == null) {
                file.delete()
                return@withContext null
            }
            if (System.currentTimeMillis() - entry.savedAt > ttlMillis && !allowExpired) {
                file.delete()
                return@withContext null
            }
            runCatching { decode(entry.data) }.getOrNull() ?: run {
                file.delete()
                null
            }
        }

    




    suspend fun <T> write(key: String, ttlMillis: Long, value: T, encode: (T) -> String) {
        withContext(Dispatchers.IO) {
            runCatching {
                dir.mkdirs()
                val file = fileFor(key)
                val payload = json.encodeToString(
                    CacheEntry.serializer(),
                    CacheEntry(
                        savedAt = System.currentTimeMillis(),
                        ttlMillis = ttlMillis,
                        data = encode(value),
                    ),
                )
                val tmp = File(dir, file.name + TMP_SUFFIX)
                tmp.writeText(payload)
                if (!tmp.renameTo(file)) {
                    
                    tmp.copyTo(file, overwrite = true)
                    tmp.delete()
                }
                evictIfTooLarge(keep = file)
            }.getOrDefault(Unit)
        }
    }

    
    suspend fun clear() {
        withContext(Dispatchers.IO) {
            dir.listFiles()?.forEach { it.delete() }
        }
    }

    
    fun sizeBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L

    






    private fun evictIfTooLarge(keep: File) {
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxBytes) return
        val now = System.currentTimeMillis()
        val oldestFirst = files.sortedBy { it.lastModified() }
        oldestFirst.forEach { file ->
            if (total <= maxBytes) return
            if (file == keep) return@forEach
            if (!isExpired(file, now)) return@forEach
            val size = file.length()
            if (file.delete()) total -= size
        }
        oldestFirst.forEach { file ->
            if (total <= maxBytes) return
            if (file == keep) return@forEach
            val size = file.length()
            if (file.delete()) total -= size
        }
    }

    



    private fun isExpired(file: File, now: Long): Boolean {
        val entry = runCatching {
            json.decodeFromString<CacheEntry>(file.readText())
        }.getOrNull() ?: return true
        val ttl = entry.ttlMillis ?: return false
        return now - entry.savedAt > ttl
    }

    private fun fileFor(key: String): File {
        val name = Base64.getUrlEncoder().withoutPadding().encodeToString(key.toByteArray())
        return File(dir, name)
    }

    private companion object {
        
        const val TMP_SUFFIX = ".tmp"

        





        const val DEFAULT_MAX_BYTES = 128L * 1024 * 1024
    }
}







@Serializable
data class CacheEntry(
    val savedAt: Long,
    val ttlMillis: Long? = null,
    val data: String,
)
