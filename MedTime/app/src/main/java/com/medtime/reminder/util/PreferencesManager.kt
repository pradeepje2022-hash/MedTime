package com.medtime.reminder.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "medtime_settings")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

class PreferencesManager(private val context: Context) {
    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val NOTIFICATION_SOUND_ON = booleanPreferencesKey("notification_sound_on")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map {
        ThemeMode.valueOf(it[THEME_MODE] ?: ThemeMode.SYSTEM.name)
    }

    val defaultSnoozeMinutes: Flow<Int> = context.dataStore.data.map {
        it[DEFAULT_SNOOZE_MINUTES] ?: 10
    }

    val use24Hour: Flow<Boolean> = context.dataStore.data.map { it[USE_24_HOUR] ?: false }

    val notificationSoundOn: Flow<Boolean> = context.dataStore.data.map {
        it[NOTIFICATION_SOUND_ON] ?: true
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[DEFAULT_SNOOZE_MINUTES] = minutes }
    }

    suspend fun setUse24Hour(value: Boolean) {
        context.dataStore.edit { it[USE_24_HOUR] = value }
    }

    suspend fun setNotificationSoundOn(value: Boolean) {
        context.dataStore.edit { it[NOTIFICATION_SOUND_ON] = value }
    }
}
