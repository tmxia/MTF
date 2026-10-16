package com.aazhou.mtforum.data.log

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json









class LogStore(
    private val file: File,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    
    private var cached: List<LogEntry>? = null

    
    suspend fun append(level: LogLevel, tag: String, message: String, detail: String? = null) {
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val current = load()
                val entry = LogEntry(
                    id = (current.lastOrNull()?.id ?: 0L) + 1,
                    time = System.currentTimeMillis(),
                    level = level,
                    tag = tag,
                    message = message,
                    detail = detail,
                )
                persist(current + entry)
            }
        }
    }

    
    suspend fun entries(): List<LogEntry> = withContext(Dispatchers.IO) {
        synchronized(lock) { load() }.asReversed()
    }

    
    suspend fun clear() {
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                cached = emptyList()
                file.delete()
            }
        }
    }

    
    suspend fun remove(id: Long) {
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val current = load()
                val kept = current.filterNot { it.id == id }
                if (kept.size != current.size) persist(kept)
            }
        }
    }

    
    fun sizeBytes(): Long = file.length()

    
    suspend fun markCrashed() {
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                runCatching {
                    file.parentFile?.mkdirs()
                    File(file.parentFile, CRASH_MARKER).writeText(
                        System.currentTimeMillis().toString(),
                    )
                }
            }
        }
    }

    
    suspend fun consumeCrashMarker(): Boolean = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val marker = File(file.parentFile, CRASH_MARKER)
            val existed = marker.isFile
            if (existed) marker.delete()
            existed
        }
    }

    
    private fun load(): List<LogEntry> {
        cached?.let { return it }
        val loaded = if (file.isFile) {
            file.readLines().mapNotNull { line ->
                if (line.isBlank()) {
                    null
                } else {
                    
                    runCatching { json.decodeFromString<LogEntry>(line) }.getOrNull()
                }
            }
        } else {
            emptyList()
        }
        cached = loaded
        return loaded
    }

    
    private fun persist(entries: List<LogEntry>) {
        val trimmed = if (entries.size > maxEntries) entries.takeLast(maxEntries) else entries
        cached = trimmed
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + TMP_SUFFIX)
            tmp.writeText(trimmed.joinToString("\n") { json.encodeToString(LogEntry.serializer(), it) })
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
        }
    }

    private companion object {
        
        const val TMP_SUFFIX = ".tmp"

        
        const val CRASH_MARKER = "crash_marker"

        
        const val DEFAULT_MAX_ENTRIES = 50
    }
}
