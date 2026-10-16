package com.aazhou.mtforum.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.noticePollDataStore by preferencesDataStore(name = "mtf_notice_poll")








class NoticePollStore(private val context: Context) {

    private val enabledKey = booleanPreferencesKey("background_enabled")

    val enabled: Flow<Boolean> =
        context.noticePollDataStore.data.map { it[enabledKey] ?: false }

    suspend fun setEnabled(enabled: Boolean) {
        context.noticePollDataStore.edit { it[enabledKey] = enabled }
    }
}
