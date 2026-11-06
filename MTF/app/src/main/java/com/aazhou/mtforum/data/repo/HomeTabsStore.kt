package com.aazhou.mtforum.data.repo

import android.content.Context
import com.aazhou.mtforum.ui.screen.home.HomeTabs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object HomeTabsStore {
    private const val PREFS_NAME = "home_tabs_settings"
    private const val KEY_ACTIVE_TABS = "active_tabs"

    private val _activeTabs = MutableStateFlow(HomeTabs.views)
    val activeTabs: StateFlow<List<String>> = _activeTabs.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_ACTIVE_TABS, null)
        if (!saved.isNullOrBlank()) {
            val list = saved.split(",").filter { it.isNotBlank() }
            if (list.isNotEmpty()) {
                _activeTabs.value = HomeTabs.views.filter { list.contains(it) }
            }
        }
    }

    fun updateTabs(context: Context, newTabs: List<String>) {
        if (newTabs.isEmpty()) return
        val sortedTabs = HomeTabs.views.filter { newTabs.contains(it) }
        _activeTabs.value = sortedTabs
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_TABS, sortedTabs.joinToString(","))
            .apply()
    }
}
