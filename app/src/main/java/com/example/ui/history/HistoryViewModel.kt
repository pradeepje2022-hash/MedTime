package com.example.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MedTimeApplication
import com.example.model.DoseStatus
import com.example.model.MedicineLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryStats(
    val totalTaken: Int = 0,
    val totalLogged: Int = 0,
    val adherencePercent: Int = 0,
    val missedCount: Int = 0,
    val skippedCount: Int = 0
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MedTimeApplication
    private val repository = app.medicineRepository

    private val _statusFilter = MutableStateFlow<DoseStatus?>(null)
    val statusFilter: StateFlow<DoseStatus?> = _statusFilter.asStateFlow()

    val rawLogs: StateFlow<List<MedicineLog>> = repository.allRecentLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredLogs: StateFlow<List<MedicineLog>> = combine(
        rawLogs,
        _statusFilter
    ) { logs, filter ->
        if (filter == null) logs else logs.filter { it.status == filter }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stats: StateFlow<HistoryStats> = rawLogs.combine(_statusFilter) { logs, _ ->
        val total = logs.size
        val taken = logs.count { it.status == DoseStatus.TAKEN }
        val missed = logs.count { it.status == DoseStatus.MISSED }
        val skipped = logs.count { it.status == DoseStatus.SKIPPED }
        val pct = if (total > 0) (taken * 100) / total else 0
        HistoryStats(
            totalTaken = taken,
            totalLogged = total,
            adherencePercent = pct,
            missedCount = missed,
            skippedCount = skipped
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryStats()
    )

    fun setStatusFilter(status: DoseStatus?) {
        _statusFilter.value = status
    }

    fun updateLogStatus(log: MedicineLog, newStatus: DoseStatus) {
        viewModelScope.launch {
            repository.recordDoseAction(
                medicineId = log.medicineId,
                medicineName = log.medicineName,
                dosage = log.dosage,
                dateString = log.dateString,
                scheduledTime = log.scheduledTime,
                status = newStatus
            )
        }
    }
}
