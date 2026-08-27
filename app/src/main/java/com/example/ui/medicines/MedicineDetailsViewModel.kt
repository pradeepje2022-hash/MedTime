package com.example.ui.medicines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MedTimeApplication
import com.example.alarm.AlarmScheduler
import com.example.model.Medicine
import com.example.model.MedicineLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MedicineDetailsViewModel(
    application: Application,
    private val medicineId: Long
) : AndroidViewModel(application) {

    private val app = application as MedTimeApplication
    private val repository = app.medicineRepository
    private val alarmScheduler = AlarmScheduler(application)

    val medicine: StateFlow<Medicine?> = repository.getMedicineByIdFlow(medicineId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val logs: StateFlow<List<MedicineLog>> = repository.getLogsForMedicineFlow(medicineId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleActive() {
        viewModelScope.launch {
            medicine.value?.let { med ->
                repository.toggleActiveStatus(med)
            }
        }
    }

    fun deleteMedicine(onDeleted: () -> Unit) {
        viewModelScope.launch {
            medicine.value?.let { med ->
                repository.deleteMedicine(med)
                onDeleted()
            }
        }
    }

    fun testAlarmForThisMedicine() {
        val med = medicine.value ?: return
        alarmScheduler.scheduleTestAlarm(med.name, med.fullDosage)
    }
}
