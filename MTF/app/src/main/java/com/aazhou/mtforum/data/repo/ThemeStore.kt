package com.aazhou.mtforum.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "mtf_theme")


class ThemeStore(private val context: Context) {

    private val customColorKey = intPreferencesKey("theme_custom_color")
    private val darkOverrideKey = booleanPreferencesKey("dark_override")
    private val dimImagesKey = booleanPreferencesKey("dim_images_dark")

    
    val customColor: Flow<Int?> =
        context.themeDataStore.data.map { it[customColorKey] }

    val darkOverride: Flow<Boolean?> =
        context.themeDataStore.data.map { it[darkOverrideKey] }

    
    val dimImagesInDark: Flow<Boolean> =
        context.themeDataStore.data.map { it[dimImagesKey] ?: true }

    suspend fun setCustomColor(argb: Int?) {
        context.themeDataStore.edit { prefs ->
            if (argb == null) prefs.remove(customColorKey) else prefs[customColorKey] = argb
        }
    }

    suspend fun setDarkOverride(dark: Boolean?) {
        context.themeDataStore.edit { prefs ->
            if (dark == null) prefs.remove(darkOverrideKey) else prefs[darkOverrideKey] = dark
        }
    }

    suspend fun setDimImagesInDark(dim: Boolean) {
        context.themeDataStore.edit { it[dimImagesKey] = dim }
    }
}
