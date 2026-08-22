package com.medtime.reminder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medtime.reminder.alarm.AlarmScheduler
import com.medtime.reminder.data.*
import com.medtime.reminder.repository.MedicineRepository
import com.medtime.reminder.util.DateTimeUtils
import com.medtime.reminder.util.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayItem(
    val historyId: Long,
    val medicineId: Long,
    val reminderTimeId: Long,
    val medicineName: String,
    val dosage: String,
    val type: MedicineType,
    val hour: Int,
    val minute: Int,
    val status: DoseStatus
)

class MedicineViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = MedicineRepository(application)
    val prefs = PreferencesManager(application)

    val allMedicines: StateFlow<List<Medicine>> = repo.getAllMedicines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Builds today's dashboard by ensuring a history entry exists for every reminder scheduled
     * for today (matching day mask + date range) and combining it with live status. */
    val todayItems: StateFlow<List<TodayItem>> = allMedicines
        .map { medicines -> medicines.filter { it.active } }
        .flatMapLatest { medicines ->
            val today = LocalDate.now()
            val epochDay = today.toEpochDay()
            combine(
                flowOf(medicines),
                repo.getHistoryForDay(epochDay)
            ) { meds, historyToday ->
                val items = mutableListOf<TodayItem>()
                for (medicine in meds) {
                    if (!DateTimeUtils.matchesDay(medicine.daysMask, today)) continue
                    if (medicine.startDate != null) {
                        val start = java.time.Instant.ofEpochMilli(medicine.startDate)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        if (today.isBefore(start)) continue
                    }
                    if (medicine.endDate != null) {
                        val end = java.time.Instant.ofEpochMilli(medicine.endDate)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        if (today.isAfter(end)) continue
                    }
                    val times = repo.getRemindersForMedicineOnce(medicine.id)
                    for (t in times) {
                        val existing = historyToday.find { it.medicineId == medicine.id && it.reminderTimeId == t.id }
                        items.add(
                            TodayItem(
                                historyId = existing?.id ?: -1L,
                                medicineId = medicine.id,
                                reminderTimeId = t.id,
                                medicineName = medicine.name,
                                dosage = medicine.dosage,
                                type = medicine.type,
                                hour = t.hour,
                                minute = t.minute,
                                status = existing?.status ?: DoseStatus.PENDING
                            )
                        )
                    }
                }
                items.sortedBy { it.hour * 60 + it.minute }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveMedicine(medicine: Medicine, times: List<ReminderTime>, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.saveMedicineWithTimes(medicine, times)
            val savedTimes = repo.getRemindersForMedicineOnce(id)
            val saved = repo.getMedicineOnce(id)
            if (saved != null) {
                savedTimes.forEach { AlarmScheduler.scheduleNext(getApplication(), saved, it) }
            }
            onDone(id)
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            val times = repo.getRemindersForMedicineOnce(medicine.id)
            times.forEach { AlarmScheduler.cancel(getApplication(), medicine, it) }
            repo.deleteMedicine(medicine)
        }
    }

    fun setActive(medicine: Medicine, active: Boolean) {
        viewModelScope.launch {
            repo.setActive(medicine, active)
            val times = repo.getRemindersForMedicineOnce(medicine.id)
            times.forEach {
                if (active) AlarmScheduler.scheduleNext(getApplication(), medicine.copy(active = true), it)
                else AlarmScheduler.cancel(getApplication(), medicine, it)
            }
        }
    }

    fun markStatus(item: TodayItem, status: DoseStatus) {
        viewModelScope.launch {
            val entryId = if (item.historyId == -1L) {
                val medicine = repo.getMedicineOnce(item.medicineId) ?: return@launch
                val reminderTime = repo.reminderTimeDao.getByIdOnce(item.reminderTimeId) ?: return@launch
                repo.getOrCreateHistoryEntry(medicine, reminderTime, LocalDate.now().toEpochDay()).id
            } else item.historyId
            repo.updateHistoryStatus(entryId, status)
        }
    }

    fun getMedicine(id: Long) = repo.getMedicine(id)
    fun getRemindersForMedicine(id: Long) = repo.getRemindersForMedicine(id)
    fun getHistoryForRange(from: Long, to: Long) = repo.getHistoryForRange(from, to)
}
