package com.medtime.reminder.repository

import android.content.Context
import com.medtime.reminder.data.*
import kotlinx.coroutines.flow.Flow

class MedicineRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    val medicineDao = db.medicineDao()
    val reminderTimeDao = db.reminderTimeDao()
    val historyDao = db.historyDao()

    fun getAllMedicines(): Flow<List<Medicine>> = medicineDao.getAll()

    suspend fun getAllActiveMedicines(): List<Medicine> = medicineDao.getAllActive()

    fun getMedicine(id: Long): Flow<Medicine?> = medicineDao.getById(id)

    suspend fun getMedicineOnce(id: Long): Medicine? = medicineDao.getByIdOnce(id)

    fun getRemindersForMedicine(medicineId: Long): Flow<List<ReminderTime>> =
        reminderTimeDao.getForMedicine(medicineId)

    suspend fun getRemindersForMedicineOnce(medicineId: Long): List<ReminderTime> =
        reminderTimeDao.getForMedicineOnce(medicineId)

    /** Saves a medicine (insert or update) along with its reminder times. Returns medicine id. */
    suspend fun saveMedicineWithTimes(medicine: Medicine, times: List<ReminderTime>): Long {
        val medicineId = if (medicine.id == 0L) {
            medicineDao.insert(medicine)
        } else {
            medicineDao.update(medicine)
            medicine.id
        }
        reminderTimeDao.deleteAllForMedicine(medicineId)
        reminderTimeDao.insertAll(times.map { it.copy(medicineId = medicineId) })
        return medicineId
    }

    suspend fun deleteMedicine(medicine: Medicine) {
        reminderTimeDao.deleteAllForMedicine(medicine.id)
        medicineDao.delete(medicine)
    }

    suspend fun setActive(medicine: Medicine, active: Boolean) {
        medicineDao.update(medicine.copy(active = active))
    }

    fun getHistoryForDay(epochDay: Long): Flow<List<HistoryEntry>> = historyDao.getForDay(epochDay)

    fun getHistoryForRange(from: Long, to: Long): Flow<List<HistoryEntry>> =
        historyDao.getForRange(from, to)

    suspend fun getOrCreateHistoryEntry(
        medicine: Medicine,
        reminderTime: ReminderTime,
        epochDay: Long
    ): HistoryEntry {
        historyDao.findEntry(medicine.id, reminderTime.id, epochDay)?.let { return it }
        val entry = HistoryEntry(
            medicineId = medicine.id,
            reminderTimeId = reminderTime.id,
            medicineName = medicine.name,
            dosage = medicine.dosage,
            dateEpochDay = epochDay,
            scheduledHour = reminderTime.hour,
            scheduledMinute = reminderTime.minute
        )
        val id = historyDao.insert(entry)
        return entry.copy(id = id)
    }

    suspend fun updateHistoryStatus(entryId: Long, status: DoseStatus) {
        historyDao.getByIdOnce(entryId)?.let {
            historyDao.update(it.copy(status = status, actionTimestamp = System.currentTimeMillis()))
        }
    }

    suspend fun markOverdueAsMissed(epochDay: Long, nowMinutes: Int) {
        historyDao.getOverdueForDay(epochDay, nowMinutes).forEach {
            historyDao.update(it.copy(status = DoseStatus.MISSED))
        }
    }
}
