package com.vaulto.lite.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "vaulto_settings")

/**
 * Persists lightweight user preferences (appearance + currency) via Jetpack
 * DataStore. These are read by [com.vaulto.lite.ui.theme.VaultoLiteTheme]
 * (dark mode) and by currency formatting helpers across Home/Analytics/Settings.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CURRENCY = stringPreferencesKey("currency")
    }

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        ThemeMode.fromNameOrDefault(prefs[Keys.THEME_MODE])
    }

    val currency: Flow<Currency> = context.settingsDataStore.data.map { prefs ->
        Currency.fromCodeOrDefault(prefs[Keys.CURRENCY])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setCurrency(currency: Currency) {
        context.settingsDataStore.edit { it[Keys.CURRENCY] = currency.code }
    }
}
