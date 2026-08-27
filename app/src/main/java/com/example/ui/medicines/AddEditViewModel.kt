package com.example.ui.medicines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MedTimeApplication
import com.example.model.FrequencyType
import com.example.model.MealInstruction
import com.example.model.Medicine
import com.example.model.MedicineType
import com.example.model.ReminderTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddEditUiState(
    val medicineId: Long = 0L,
    val name: String = "",
    val dosageAmount: String = "1",
    val dosageUnit: String = "tablet",
    val medicineType: MedicineType = MedicineType.TABLET,
    val colorHex: Long = 0xFF007A87,
    val mealInstruction: MealInstruction = MealInstruction.AFTER_MEAL,
    val frequencyType: FrequencyType = FrequencyType.EVERY_DAY,
    val selectedDaysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val reminderTimes: List<ReminderTime> = listOf(ReminderTime(8, 0, "Morning")),
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val hasEndDate: Boolean = false,
    val notes: String = "",
    val isActive: Boolean = true,
    val nameError: String? = null,
    val timesError: String? = null,
    val isSaving: Boolean = false
)

class AddEditViewModel(
    application: Application,
    private val initialMedicineId: Long?
) : AndroidViewModel(application) {

    private val app = application as MedTimeApplication
    private val repository = app.medicineRepository

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        if (initialMedicineId != null && initialMedicineId > 0) {
            loadMedicine(initialMedicineId)
        }
    }

    private fun loadMedicine(id: Long) {
        viewModelScope.launch {
            val med = repository.getMedicineById(id)
            if (med != null) {
                _uiState.value = AddEditUiState(
                    medicineId = med.id,
                    name = med.name,
                    dosageAmount = med.dosageAmount,
                    dosageUnit = med.dosageUnit,
                    medicineType = med.medicineType,
                    colorHex = med.colorHex,
                    mealInstruction = med.mealInstruction,
                    frequencyType = med.frequencyType,
                    selectedDaysOfWeek = med.selectedDaysOfWeek,
                    reminderTimes = med.reminderTimes.ifEmpty { listOf(ReminderTime(8, 0, "Morning")) },
                    startDate = med.startDate,
                    endDate = med.endDate,
                    hasEndDate = med.endDate != null,
                    notes = med.notes,
                    isActive = med.isActive
                )
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(name = name, nameError = null)
    }

    fun onDosageAmountChanged(amount: String) {
        _uiState.value = _uiState.value.copy(dosageAmount = amount)
    }

    fun onDosageUnitChanged(unit: String) {
        _uiState.value = _uiState.value.copy(dosageUnit = unit)
    }

    fun onMedicineTypeChanged(type: MedicineType) {
        val defaultUnit = type.defaultUnit
        _uiState.value = _uiState.value.copy(
            medicineType = type,
            dosageUnit = defaultUnit
        )
    }

    fun onColorChanged(colorHex: Long) {
        _uiState.value = _uiState.value.copy(colorHex = colorHex)
    }

    fun onMealInstructionChanged(instruction: MealInstruction) {
        _uiState.value = _uiState.value.copy(mealInstruction = instruction)
    }

    fun onFrequencyTypeChanged(type: FrequencyType) {
        _uiState.value = _uiState.value.copy(frequencyType = type)
    }

    fun toggleDayOfWeek(day: Int) {
        val currentDays = _uiState.value.selectedDaysOfWeek.toMutableList()
        if (currentDays.contains(day)) {
            if (currentDays.size > 1) { // keep at least 1 day
                currentDays.remove(day)
            }
        } else {
            currentDays.add(day)
        }
        _uiState.value = _uiState.value.copy(selectedDaysOfWeek = currentDays)
    }

    fun addReminderTime(hour: Int, minute: Int, label: String = "Reminder") {
        val newTimes = _uiState.value.reminderTimes.toMutableList()
        // avoid duplicates
        if (!newTimes.any { it.hour == hour && it.minute == minute }) {
            newTimes.add(ReminderTime(hour, minute, label))
            newTimes.sortBy { it.hour * 60 + it.minute }
            _uiState.value = _uiState.value.copy(reminderTimes = newTimes, timesError = null)
        }
    }

    fun removeReminderTime(time: ReminderTime) {
        val newTimes = _uiState.value.reminderTimes.toMutableList()
        if (newTimes.size > 1) {
            newTimes.remove(time)
            _uiState.value = _uiState.value.copy(reminderTimes = newTimes)
        }
    }

    fun addPresetSchedule(presetType: PresetSchedule) {
        val times = when (presetType) {
            PresetSchedule.ONCE_DAILY -> listOf(
                ReminderTime(8, 0, "Morning")
            )
            PresetSchedule.TWICE_DAILY -> listOf(
                ReminderTime(8, 0, "Morning"),
                ReminderTime(20, 0, "Night")
            )
            PresetSchedule.THRICE_DAILY -> listOf(
                ReminderTime(8, 0, "Morning"),
                ReminderTime(14, 0, "Afternoon"),
                ReminderTime(20, 0, "Night")
            )
            PresetSchedule.FOUR_TIMES_DAILY -> listOf(
                ReminderTime(8, 0, "Morning"),
                ReminderTime(12, 0, "Noon"),
                ReminderTime(17, 0, "Evening"),
                ReminderTime(22, 0, "Bedtime")
            )
        }
        _uiState.value = _uiState.value.copy(reminderTimes = times, timesError = null)
    }

    fun onHasEndDateChanged(hasEnd: Boolean) {
        val endDate = if (hasEnd) System.currentTimeMillis() + (7 * 86400000L) else null
        _uiState.value = _uiState.value.copy(hasEndDate = hasEnd, endDate = endDate)
    }

    fun onEndDateChanged(millis: Long?) {
        _uiState.value = _uiState.value.copy(endDate = millis)
    }

    fun onNotesChanged(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun saveMedicine(onSuccess: (Long) -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(nameError = "Please enter medicine name")
            return
        }

        if (state.reminderTimes.isEmpty()) {
            _uiState.value = state.copy(timesError = "Please add at least one reminder time")
            return
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val medicine = Medicine(
                id = state.medicineId,
                name = state.name.trim(),
                dosageAmount = state.dosageAmount.ifBlank { "1" },
                dosageUnit = state.dosageUnit.ifBlank { "dose" },
                medicineType = state.medicineType,
                colorHex = state.colorHex,
                mealInstruction = state.mealInstruction,
                frequencyType = state.frequencyType,
                selectedDaysOfWeek = state.selectedDaysOfWeek,
                reminderTimes = state.reminderTimes,
                startDate = state.startDate,
                endDate = if (state.hasEndDate) state.endDate else null,
                notes = state.notes.trim(),
                isActive = state.isActive
            )

            val id = repository.saveMedicine(medicine)
            _uiState.value = _uiState.value.copy(isSaving = false)
            onSuccess(id)
        }
    }
}

enum class PresetSchedule(val title: String) {
    ONCE_DAILY("1x Daily (8 AM)"),
    TWICE_DAILY("2x Daily (8 AM, 8 PM)"),
    THRICE_DAILY("3x Daily (8 AM, 2 PM, 8 PM)"),
    FOUR_TIMES_DAILY("4x Daily")
}
