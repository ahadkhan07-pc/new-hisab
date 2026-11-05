package com.mydailyhisab.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "my_daily_hisab_theme_prefs")

class ThemePreferences(private val context: Context) {
    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode_preference")
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }

    /**
     * Flow emitting current theme mode.
     * CRITICAL RULE: On very first launch, the default MUST be Light Mode ("light").
     * Restores previously selected theme on every subsequent launch.
     * Never follows system theme automatically.
     */
    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: THEME_LIGHT
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }
}
