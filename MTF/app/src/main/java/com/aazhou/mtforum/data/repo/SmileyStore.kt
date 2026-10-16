package com.aazhou.mtforum.data.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aazhou.mtforum.data.parser.SmileyPack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.smileyDataStore by preferencesDataStore(name = "mtf_smileys")


private const val RECENT_LIMIT = 30




data class CachedSmileys(
    val packs: List<SmileyPack>,
    val checkedAt: Long,
    val etag: String?,
)








class SmileyStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val packsKey = stringPreferencesKey("packs")
    private val checkedAtKey = longPreferencesKey("checked_at")
    private val etagKey = stringPreferencesKey("etag")
    private val recentKey = stringPreferencesKey("recent")

    
    val recent: Flow<List<String>> = context.smileyDataStore.data.map { prefs ->
        val raw = prefs[recentKey] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    
    suspend fun packs(): CachedSmileys? {
        val prefs = context.smileyDataStore.data.first()
        val raw = prefs[packsKey] ?: return null
        val packs = runCatching { json.decodeFromString<List<SmileyPack>>(raw) }.getOrNull() ?: return null
        return CachedSmileys(packs, prefs[checkedAtKey] ?: 0L, prefs[etagKey])
    }

    
    suspend fun savePacks(packs: List<SmileyPack>, etag: String?) {
        context.smileyDataStore.edit { prefs ->
            prefs[packsKey] = json.encodeToString(packs)
            prefs[checkedAtKey] = System.currentTimeMillis()
            if (etag.isNullOrEmpty()) prefs.remove(etagKey) else prefs[etagKey] = etag
        }
    }

    
    suspend fun markChecked() {
        context.smileyDataStore.edit { it[checkedAtKey] = System.currentTimeMillis() }
    }

    
    suspend fun addRecent(code: String) {
        if (code.isBlank()) return
        context.smileyDataStore.edit { prefs ->
            val list = prefs[recentKey]
                ?.let { raw -> runCatching { json.decodeFromString<List<String>>(raw) }.getOrNull() }
                .orEmpty()
            val updated = (listOf(code) + list.filter { it != code }).take(RECENT_LIMIT)
            prefs[recentKey] = json.encodeToString(updated)
        }
    }

    
    suspend fun clear() {
        context.smileyDataStore.edit { it.clear() }
    }

    
    suspend fun approximateBytes(): Long = context.smileyDataStore.data.first().let { prefs ->
        var total = 0L
        prefs.asMap().forEach { (_, value) ->
            total += when (value) {
                is String -> value.length.toLong()
                is Long -> 8L
                else -> 0L
            }
        }
        total
    }
}
