package com.aazhou.mtforum.data.repo

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aazhou.mtforum.data.model.DownloadedThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.downloadDataStore by preferencesDataStore(name = "mtf_downloads")







class DownloadStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("threads")

    val items: Flow<List<DownloadedThread>> =
        context.downloadDataStore.data.map { prefs ->
            val raw = prefs[key] ?: return@map emptyList()
            runCatching { json.decodeFromString<List<DownloadedThread>>(raw) }
                .getOrDefault(emptyList())
                .sortedByDescending { it.savedAt }
        }

    
    suspend fun upsert(entry: DownloadedThread) {
        val previous = currentItems().firstOrNull { it.tid == entry.tid }
        previous?.let { deleteDocumentFile(it.uri) }
        context.downloadDataStore.edit { prefs ->
            val list = prefs[key]
                ?.let { raw -> runCatching { json.decodeFromString<List<DownloadedThread>>(raw) }.getOrNull() }
                .orEmpty()
                .filter { it.tid != entry.tid } + entry
            prefs[key] = json.encodeToString(list)
        }
    }

    
    suspend fun remove(tid: String, onEntryGone: suspend () -> Unit = {}) {
        val entry = currentItems().firstOrNull { it.tid == tid }
        if (entry != null) deleteDocumentFile(entry.uri)
        context.downloadDataStore.edit { prefs ->
            val list = prefs[key]
                ?.let { raw -> runCatching { json.decodeFromString<List<DownloadedThread>>(raw) }.getOrNull() }
                .orEmpty()
                .filter { it.tid != tid }
            prefs[key] = json.encodeToString(list)
        }
        onEntryGone()
    }

    private suspend fun currentItems(): List<DownloadedThread> {
        val raw = context.downloadDataStore.data.map { it[key] }.first()
        return raw?.let { runCatching { json.decodeFromString<List<DownloadedThread>>(it) }.getOrNull() }
            .orEmpty()
    }

    private fun deleteDocumentFile(uriString: String) {
        runCatching {
            context.contentResolver.delete(Uri.parse(uriString), null, null)
        }
    }
}
