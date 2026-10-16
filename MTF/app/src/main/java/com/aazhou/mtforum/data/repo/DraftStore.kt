package com.aazhou.mtforum.data.repo

import com.aazhou.mtforum.data.model.PostDraft
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json










class DraftStore(private val dir: File) {

    private val json = Json { ignoreUnknownKeys = true }
    private val file = File(dir, FILE_NAME)

    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    




    private val writeMutex = Mutex()

    private val _items = MutableStateFlow(readFromDisk())
    val items: StateFlow<List<PostDraft>> = _items.asStateFlow()

    
    fun save(draft: PostDraft) {
        scope.launch {
            writeMutex.withLock {
                val merged = _items.value.filter { it.fid != draft.fid } + draft
                _items.value = merged.sortedByDescending { it.savedAt }
                writeToDisk(_items.value)
            }
        }
    }

    
    fun remove(fids: List<String>) {
        if (fids.isEmpty()) return
        scope.launch {
            writeMutex.withLock {
                val left = _items.value.filter { it.fid !in fids }
                if (left.size == _items.value.size) return@withLock
                _items.value = left
                writeToDisk(left)
            }
        }
    }

    
    fun clear() {
        scope.launch {
            writeMutex.withLock {
                _items.value = emptyList()
                writeToDisk(emptyList())
            }
        }
    }

    
    fun sizeBytes(): Long = if (file.isFile) file.length() else 0L

    
    private fun readFromDisk(): List<PostDraft> {
        if (!file.isFile) return emptyList()
        val parsed = runCatching { json.decodeFromString<List<PostDraft>>(file.readText()) }.getOrNull()
        if (parsed == null) {
            file.delete()
            return emptyList()
        }
        return parsed.sortedByDescending { it.savedAt }
    }

    private fun writeToDisk(items: List<PostDraft>) {
        runCatching {
            dir.mkdirs()
            val tmp = File(dir, "$FILE_NAME$TMP_SUFFIX")
            tmp.writeText(json.encodeToString(items))
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
        }
    }

    private companion object {
        const val FILE_NAME = "post_drafts.json"
        const val TMP_SUFFIX = ".tmp"
    }
}
