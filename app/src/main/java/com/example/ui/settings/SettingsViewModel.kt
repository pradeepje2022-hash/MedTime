package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MedTimeApplication
import com.example.alarm.AlarmScheduler
import com.example.model.AlarmSoundOption
import com.example.model.UserSettings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MedTimeApplication
    private val settingsRepo = app.settingsRepository
    private val alarmScheduler = AlarmScheduler(application)

    val settings: StateFlow<UserSettings> = settingsRepo.settings

    fun setDarkMode(mode: Boolean?) {
        settingsRepo.setDarkMode(mode)
    }

    fun set24HourFormat(enabled: Boolean) {
        settingsRepo.set24HourFormat(enabled)
    }

    fun setDefaultSnoozeMinutes(minutes: Int) {
        settingsRepo.setDefaultSnoozeMinutes(minutes)
    }

    fun setSoundOption(option: AlarmSoundOption) {
        settingsRepo.setSoundOption(option)
    }

    fun setVibrate(enabled: Boolean) {
        settingsRepo.setVibrate(enabled)
    }

    fun setFullScreenAlert(enabled: Boolean) {
        settingsRepo.setFullScreenAlert(enabled)
    }

    fun testAlarmIn5Seconds() {
        alarmScheduler.scheduleTestAlarm("Medication (Test)", "1 dose")
    }

    fun clearAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            app.medicineRepository.clearAllData()
            onComplete()
        }
    }
}
