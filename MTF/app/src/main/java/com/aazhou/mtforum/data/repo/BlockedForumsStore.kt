package com.aazhou.mtforum.data.repo

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BlockedForumsStore {
    private const val PREFS_NAME = "blocked_forums_settings"
    private const val KEY_BLOCKED_FORUMS = "blocked_forums"
    private const val KEY_KNOWN_FORUMS = "known_forums"

    // 已屏蔽的版块
    private val _blockedForums = MutableStateFlow<Set<String>>(emptySet())
    val blockedForums: StateFlow<Set<String>> = _blockedForums.asStateFlow()

    // 历史抓取到的所有版块（供用户勾选）
    private val _knownForums = MutableStateFlow<Set<String>>(emptySet())
    val knownForums: StateFlow<Set<String>> = _knownForums.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val savedBlocked = prefs.getString(KEY_BLOCKED_FORUMS, null)
        if (!savedBlocked.isNullOrBlank()) {
            _blockedForums.value = savedBlocked.split(",").filter { it.isNotBlank() }.toSet()
        }

        val savedKnown = prefs.getString(KEY_KNOWN_FORUMS, null)
        if (!savedKnown.isNullOrBlank()) {
            _knownForums.value = savedKnown.split(",").filter { it.isNotBlank() }.toSet()
        }
    }

    fun updateBlockedForums(context: Context, forums: Set<String>) {
        _blockedForums.value = forums
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BLOCKED_FORUMS, forums.joinToString(","))
            .apply()
    }

    fun addKnownForums(context: Context, forums: Set<String>) {
        if (forums.isEmpty()) return
        val current = _knownForums.value
        val newForums = current + forums
        if (newForums.size > current.size) {
            _knownForums.value = newForums
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_KNOWN_FORUMS, newForums.joinToString(","))
                .apply()
        }
    }
}
