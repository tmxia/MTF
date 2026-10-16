package com.aazhou.mtforum.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aazhou.mtforum.data.model.NoticeTab
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.noticeUnreadDataStore by preferencesDataStore(name = "mtf_notice_unread")



















class NoticeUnreadStore(private val context: Context) {

    private val seenKey = stringSetPreferencesKey("seen_ids")
    private val unreadKey = booleanPreferencesKey("unread")
    private val pmUnreadKey = booleanPreferencesKey("pm_unread")
    
    private val pendingKey = stringSetPreferencesKey("pending_ids")

    val hasUnread: Flow<Boolean> =
        context.noticeUnreadDataStore.data.map { prefs ->
            (prefs[unreadKey] ?: false) || (prefs[pmUnreadKey] ?: false)
        }

    
    suspend fun onPolled(ids: List<String>): List<String> {
        if (ids.isEmpty()) return emptyList()
        var fresh = emptyList<String>()
        context.noticeUnreadDataStore.edit { prefs ->
            val seen = prefs[seenKey].orEmpty()
            
            fresh = if (seen.isEmpty()) emptyList() else ids.filterNot { it in seen }
            prefs[seenKey] = ids.toSet()
            if (fresh.isNotEmpty()) {
                prefs[pendingKey] = prefs[pendingKey].orEmpty() + fresh
                prefs[unreadKey] = true
            }
        }
        return fresh
    }

    
    suspend fun markRead(ids: List<String>) {
        context.noticeUnreadDataStore.edit { prefs ->
            
            if (ids.isNotEmpty()) prefs[seenKey] = ids.toSet()
            prefs[pendingKey] = emptySet()
            prefs[unreadKey] = false
            
            prefs[pmUnreadKey] = false
        }
    }

    





    suspend fun setPmUnread(unread: Boolean) {
        context.noticeUnreadDataStore.edit { it[pmUnreadKey] = unread }
    }

    
    val unreadTabs: Flow<Set<NoticeTab>> =
        context.noticeUnreadDataStore.data.map { prefs ->
            NoticeTab.entries.filter { prefs[categoryUnreadKey(it)] == true }.toSet()
        }

    






    suspend fun onCategoryPolled(category: NoticeTab, ids: List<String>): Boolean {
        if (ids.isEmpty()) return false
        var fresh = false
        context.noticeUnreadDataStore.edit { prefs ->
            val seen = prefs[categorySeenKey(category)].orEmpty()
            if (seen.isEmpty()) {
                prefs[categorySeenKey(category)] = ids.toSet()
            } else if (ids.any { it !in seen }) {
                prefs[categoryUnreadKey(category)] = true
                fresh = true
            }
        }
        return fresh
    }

    
    suspend fun markCategoryRead(category: NoticeTab, ids: List<String>) {
        context.noticeUnreadDataStore.edit { prefs ->
            if (ids.isNotEmpty()) prefs[categorySeenKey(category)] = ids.toSet()
            prefs[categoryUnreadKey(category)] = false
            
            
            val left = prefs[pendingKey].orEmpty() - ids.toSet()
            prefs[pendingKey] = left
            if (left.isEmpty()) prefs[unreadKey] = false
        }
    }
}


private fun categorySeenKey(category: NoticeTab) =
    stringSetPreferencesKey("seen_ids_${category.key}")

private fun categoryUnreadKey(category: NoticeTab) =
    booleanPreferencesKey("unread_${category.key}")
