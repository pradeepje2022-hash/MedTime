package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AlarmSoundOption
import com.example.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("medtime_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        val darkModeRaw = prefs.getString(KEY_DARK_MODE, "SYSTEM")
        val isDarkMode = when (darkModeRaw) {
            "DARK" -> true
            "LIGHT" -> false
            else -> null
        }
        val is24Hour = prefs.getBoolean(KEY_24_HOUR, false)
        val snoozeMins = prefs.getInt(KEY_SNOOZE_MINUTES, 10)
        val soundRaw = prefs.getString(KEY_SOUND_OPTION, AlarmSoundOption.DEFAULT_ALARM.name)
        val soundOption = try {
            AlarmSoundOption.valueOf(soundRaw ?: AlarmSoundOption.DEFAULT_ALARM.name)
        } catch (e: Exception) {
            AlarmSoundOption.DEFAULT_ALARM
        }
        val vibrate = prefs.getBoolean(KEY_VIBRATE, true)
        val fullScreen = prefs.getBoolean(KEY_FULL_SCREEN, true)

        return UserSettings(
            isDarkMode = isDarkMode,
            is24HourFormat = is24Hour,
            defaultSnoozeMinutes = snoozeMins,
            soundOption = soundOption,
            vibrate = vibrate,
            fullScreenAlert = fullScreen
        )
    }

    fun setDarkMode(mode: Boolean?) {
        val modeStr = when (mode) {
            true -> "DARK"
            false -> "LIGHT"
            null -> "SYSTEM"
        }
        prefs.edit().putString(KEY_DARK_MODE, modeStr).apply()
        _settings.value = _settings.value.copy(isDarkMode = mode)
    }

    fun set24HourFormat(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_24_HOUR, enabled).apply()
        _settings.value = _settings.value.copy(is24HourFormat = enabled)
    }

    fun setDefaultSnoozeMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SNOOZE_MINUTES, minutes).apply()
        _settings.value = _settings.value.copy(defaultSnoozeMinutes = minutes)
    }

    fun setSoundOption(option: AlarmSoundOption) {
        prefs.edit().putString(KEY_SOUND_OPTION, option.name).apply()
        _settings.value = _settings.value.copy(soundOption = option)
    }

    fun setVibrate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE, enabled).apply()
        _settings.value = _settings.value.copy(vibrate = enabled)
    }

    fun setFullScreenAlert(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FULL_SCREEN, enabled).apply()
        _settings.value = _settings.value.copy(fullScreenAlert = enabled)
    }

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_24_HOUR = "24_hour_format"
        private const val KEY_SNOOZE_MINUTES = "snooze_minutes"
        private const val KEY_SOUND_OPTION = "sound_option"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_FULL_SCREEN = "full_screen_alert"
    }
}
