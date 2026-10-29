package com.autowall.changer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autowall.changer.model.AutoWallSettings
import com.autowall.changer.model.FitMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "autowall_settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsStore

    val settings: Flow<AutoWallSettings> = store.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { it.toSettings() }

    suspend fun update(transform: (AutoWallSettings) -> AutoWallSettings) {
        store.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.ENABLED] = next.enabled
            prefs[Keys.INTERVAL] = next.intervalMinutes
            prefs[Keys.FIT_MODE] = next.fitMode.name
            prefs[Keys.CORNER_RADIUS] = next.cornerRadiusDp
            prefs[Keys.TONE_INDEX] = next.toneIndex
            prefs[Keys.TONE_STRENGTH] = next.toneStrength
            prefs[Keys.SHUFFLE] = next.shuffle
            prefs[Keys.LOCK_SCREEN] = next.applyToLockScreen
            prefs[Keys.LAST_APPLIED_AT] = next.lastAppliedAt
            prefs[Keys.LAST_INDEX] = next.lastIndex
        }
    }

    suspend fun markApplied(index: Int, at: Long = System.currentTimeMillis()) {
        update { it.copy(lastIndex = index, lastAppliedAt = at) }
    }

    private fun Preferences.toSettings(): AutoWallSettings = AutoWallSettings(
        enabled = this[Keys.ENABLED] ?: false,
        intervalMinutes = this[Keys.INTERVAL] ?: 60,
        fitMode = runCatching { FitMode.valueOf(this[Keys.FIT_MODE] ?: FitMode.FILL.name) }
            .getOrDefault(FitMode.FILL),
        cornerRadiusDp = this[Keys.CORNER_RADIUS] ?: 24,
        toneIndex = this[Keys.TONE_INDEX] ?: 0,
        toneStrength = this[Keys.TONE_STRENGTH] ?: 0.22f,
        shuffle = this[Keys.SHUFFLE] ?: false,
        applyToLockScreen = this[Keys.LOCK_SCREEN] ?: true,
        lastAppliedAt = this[Keys.LAST_APPLIED_AT] ?: 0L,
        lastIndex = this[Keys.LAST_INDEX] ?: -1,
    )

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val INTERVAL = intPreferencesKey("interval_minutes")
        val FIT_MODE = stringPreferencesKey("fit_mode")
        val CORNER_RADIUS = intPreferencesKey("corner_radius_dp")
        val TONE_INDEX = intPreferencesKey("tone_index")
        val TONE_STRENGTH = floatPreferencesKey("tone_strength")
        val SHUFFLE = booleanPreferencesKey("shuffle")
        val LOCK_SCREEN = booleanPreferencesKey("lock_screen")
        val LAST_APPLIED_AT = longPreferencesKey("last_applied_at")
        val LAST_INDEX = intPreferencesKey("last_index")
    }
}
