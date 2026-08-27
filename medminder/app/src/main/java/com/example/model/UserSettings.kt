package com.example.model

enum class AlarmSoundOption(val displayName: String, val resName: String) {
    DEFAULT_ALARM("System Alarm", "system_alarm"),
    GENTLE_CHIME("Gentle Chime", "gentle_chime"),
    MEDICAL_BEEP("Medical Tone", "medical_beep"),
    CLASSIC_BELL("Classic Bell", "classic_bell")
}

data class UserSettings(
    val isDarkMode: Boolean? = null, // null for system default
    val is24HourFormat: Boolean = false,
    val defaultSnoozeMinutes: Int = 10,
    val soundOption: AlarmSoundOption = AlarmSoundOption.DEFAULT_ALARM,
    val vibrate: Boolean = true,
    val fullScreenAlert: Boolean = true
)
