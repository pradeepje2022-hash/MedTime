package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MedTimeApplication
import com.example.alarm.AlarmScheduler
import com.example.model.DoseStatus
import com.example.model.TodayDoseItem
import com.example.model.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DoseFilter(val displayName: String) {
    ALL("All"),
    UPCOMING("Upcoming"),
    TAKEN("Taken"),
    MISSED("Missed"),
    SKIPPED("Skipped")
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MedTimeApplication
    private val repository = app.medicineRepository
    val settingsRepo = app.settingsRepository
    val userSettings: StateFlow<UserSettings> = settingsRepo.settings

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

    private val _selectedFilter = MutableStateFlow(DoseFilter.ALL)
    val selectedFilter: StateFlow<DoseFilter> = _selectedFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val doseItems: StateFlow<List<TodayDoseItem>> = _selectedDate
        .flatMapLatest { cal -> repository.getDoseItemsForDateFlow(cal) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectDate(calendar: Calendar) {
        _selectedDate.value = calendar.clone() as Calendar
    }

    fun selectFilter(filter: DoseFilter) {
        _selectedFilter.value = filter
    }

    fun goToToday() {
        _selectedDate.value = Calendar.getInstance()
    }

    fun markDoseTaken(doseItem: TodayDoseItem) {
        viewModelScope.launch {
            repository.recordDoseAction(
                medicineId = doseItem.medicine.id,
                medicineName = doseItem.medicine.name,
                dosage = doseItem.medicine.fullDosage,
                dateString = doseItem.dateString,
                scheduledTime = doseItem.reminderTime.toTimeString(),
                status = DoseStatus.TAKEN
            )
        }
    }

    fun markDoseSkipped(doseItem: TodayDoseItem) {
        viewModelScope.launch {
            repository.recordDoseAction(
                medicineId = doseItem.medicine.id,
                medicineName = doseItem.medicine.name,
                dosage = doseItem.medicine.fullDosage,
                dateString = doseItem.dateString,
                scheduledTime = doseItem.reminderTime.toTimeString(),
                status = DoseStatus.SKIPPED
            )
        }
    }

    fun snoozeDose(doseItem: TodayDoseItem, minutes: Int) {
        viewModelScope.launch {
            val snoozeUntil = System.currentTimeMillis() + (minutes * 60 * 1000L)
            repository.recordDoseAction(
                medicineId = doseItem.medicine.id,
                medicineName = doseItem.medicine.name,
                dosage = doseItem.medicine.fullDosage,
                dateString = doseItem.dateString,
                scheduledTime = doseItem.reminderTime.toTimeString(),
                status = DoseStatus.SNOOZED,
                snoozeUntil = snoozeUntil
            )

            AlarmScheduler(getApplication()).scheduleSnooze(
                medicineId = doseItem.medicine.id,
                medicineName = doseItem.medicine.name,
                dosage = doseItem.medicine.fullDosage,
                scheduledTime = doseItem.reminderTime.toTimeString(),
                instruction = doseItem.medicine.mealInstruction.displayName,
                type = doseItem.medicine.medicineType.displayName,
                snoozeMinutes = minutes
            )
        }
    }

    fun undoDoseAction(doseItem: TodayDoseItem) {
        viewModelScope.launch {
            repository.undoDoseAction(doseItem)
        }
    }
}
