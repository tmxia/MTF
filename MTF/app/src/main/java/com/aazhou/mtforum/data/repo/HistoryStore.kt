package com.aazhou.mtforum.data.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aazhou.mtforum.data.model.HistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.historyDataStore by preferencesDataStore(name = "mtf_history")


class HistoryStore(private val context: Context) {

    private val jsonKey = stringPreferencesKey("items_json")
    private val maxItems = 200

    val items: Flow<List<HistoryItem>> = context.historyDataStore.data.map { prefs ->
        parse(prefs[jsonKey] ?: "[]")
    }

    suspend fun add(item: HistoryItem) {
        context.historyDataStore.edit { prefs ->
            val current = parse(prefs[jsonKey] ?: "[]")
            val filtered = current.filterNot { it.tid == item.tid }.toMutableList()
            filtered.add(0, item)
            prefs[jsonKey] = serialize(filtered.take(maxItems))
        }
    }

    suspend fun remove(tid: String, type: String = "thread") {
        context.historyDataStore.edit { prefs ->
            val current = parse(prefs[jsonKey] ?: "[]")
            prefs[jsonKey] = serialize(
                current.filterNot { it.tid == tid && it.type == type }
            )
        }
    }

    suspend fun clear() {
        context.historyDataStore.edit { it[jsonKey] = "[]" }
    }

    private fun parse(json: String): List<HistoryItem> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            HistoryItem(
                tid = o.optString("tid"),
                title = o.optString("title"),
                author = o.optString("author"),
                avatarUrl = o.optString("avatarUrl").takeIf { it.isNotEmpty() },
                forumName = o.optString("forumName").takeIf { it.isNotEmpty() },
                viewedAt = o.optLong("viewedAt"),
                type = o.optString("type", "thread"),
            )
        }
    } catch (_: Exception) { emptyList() }

    private fun serialize(items: List<HistoryItem>): String {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().apply {
                put("tid", it.tid); put("title", it.title); put("author", it.author)
                put("avatarUrl", it.avatarUrl ?: ""); put("forumName", it.forumName ?: "")
                put("viewedAt", it.viewedAt)
                put("type", it.type)
            })
        }
        return arr.toString()
    }
}
