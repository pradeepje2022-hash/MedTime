package com.example.data

import android.content.Context
import com.example.alarm.AlarmScheduler
import com.example.model.DoseStatus
import com.example.model.FrequencyType
import com.example.model.MealInstruction
import com.example.model.Medicine
import com.example.model.MedicineLog
import com.example.model.MedicineType
import com.example.model.ReminderTime
import com.example.model.TodayDoseItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MedicineRepository(
    private val context: Context,
    private val medicineDao: MedicineDao,
    private val medicineLogDao: MedicineLogDao
) {
    private val alarmScheduler = AlarmScheduler(context)

    val allMedicines: Flow<List<Medicine>> = medicineDao.getAllMedicinesFlow()
    val activeMedicines: Flow<List<Medicine>> = medicineDao.getActiveMedicinesFlow()
    val allRecentLogs: Flow<List<MedicineLog>> = medicineLogDao.getAllRecentLogsFlow()
    val totalTakenCount: Flow<Int> = medicineLogDao.getTotalTakenCount()
    val totalLogsCount: Flow<Int> = medicineLogDao.getTotalLogsCount()

    fun getMedicineByIdFlow(id: Long): Flow<Medicine?> = medicineDao.getMedicineByIdFlow(id)

    suspend fun getMedicineById(id: Long): Medicine? = medicineDao.getMedicineById(id)

    suspend fun getAllActiveMedicinesSync(): List<Medicine> = medicineDao.getAllActiveMedicinesSync()

    fun getLogsForMedicineFlow(medicineId: Long): Flow<List<MedicineLog>> =
        medicineLogDao.getLogsForMedicineFlow(medicineId)

    fun getLogsForDateFlow(dateString: String): Flow<List<MedicineLog>> =
        medicineLogDao.getLogsForDateFlow(dateString)

    /**
     * Combines active medicines and date logs to produce rich TodayDoseItems
     */
    fun getDoseItemsForDateFlow(date: Calendar): Flow<List<TodayDoseItem>> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateStr = sdf.format(date.time)

        return combine(
            medicineDao.getAllMedicinesFlow(),
            medicineLogDao.getLogsForDateFlow(dateStr)
        ) { medicines, logs ->
            val list = mutableListOf<TodayDoseItem>()
            val logMap = logs.associateBy { "${it.medicineId}_${it.scheduledTime}" }

            for (med in medicines) {
                if (med.isScheduledForDay(date)) {
                    for (reminder in med.reminderTimes) {
                        val key = "${med.id}_${reminder.toTimeString()}"
                        val log = logMap[key]

                        // Compute default status if no log yet
                        val computed = if (log != null) {
                            log.status
                        } else {
                            val now = Calendar.getInstance()
                            val isToday = isSameDay(now, date)
                            val isPastDay = date.before(now) && !isToday

                            if (isPastDay) {
                                DoseStatus.MISSED
                            } else if (isToday) {
                                val doseCal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, reminder.hour)
                                    set(Calendar.MINUTE, reminder.minute)
                                    set(Calendar.SECOND, 0)
                                }
                                if (now.after(doseCal)) {
                                    // Past time today by 30+ minutes -> missed, or upcoming
                                    val diffMinutes = (now.timeInMillis - doseCal.timeInMillis) / 60000
                                    if (diffMinutes > 45) DoseStatus.MISSED else DoseStatus.UPCOMING
                                } else {
                                    DoseStatus.UPCOMING
                                }
                            } else {
                                DoseStatus.UPCOMING
                            }
                        }

                        list.add(
                            TodayDoseItem(
                                medicine = med,
                                reminderTime = reminder,
                                dateString = dateStr,
                                log = log,
                                computedStatus = computed
                            )
                        )
                    }
                }
            }

            // Sort by time: hour * 60 + minute
            list.sortedBy { it.reminderTime.hour * 60 + it.reminderTime.minute }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    suspend fun saveMedicine(medicine: Medicine): Long {
        val id = if (medicine.id == 0L) {
            val newId = medicineDao.insertMedicine(medicine)
            val savedMed = medicine.copy(id = newId)
            alarmScheduler.scheduleMedicine(savedMed)
            newId
        } else {
            medicineDao.updateMedicine(medicine)
            alarmScheduler.cancelMedicineAlarms(medicine)
            alarmScheduler.scheduleMedicine(medicine)
            medicine.id
        }
        return id
    }

    suspend fun toggleActiveStatus(medicine: Medicine) {
        val newStatus = !medicine.isActive
        medicineDao.updateActiveStatus(medicine.id, newStatus)
        val updated = medicine.copy(isActive = newStatus)
        if (newStatus) {
            alarmScheduler.scheduleMedicine(updated)
        } else {
            alarmScheduler.cancelMedicineAlarms(updated)
        }
    }

    suspend fun deleteMedicine(medicine: Medicine) {
        alarmScheduler.cancelMedicineAlarms(medicine)
        medicineDao.deleteMedicine(medicine)
        medicineLogDao.deleteLogsForMedicine(medicine.id)
    }

    suspend fun recordDoseAction(
        medicineId: Long,
        medicineName: String,
        dosage: String,
        dateString: String,
        scheduledTime: String,
        status: DoseStatus,
        snoozeUntil: Long? = null,
        notes: String = ""
    ) {
        val existingLog = medicineLogDao.getLog(medicineId, dateString, scheduledTime)
        val med = medicineDao.getMedicineById(medicineId)

        val log = (existingLog ?: MedicineLog(
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            medicineType = med?.medicineType ?: MedicineType.TABLET,
            colorHex = med?.colorHex ?: 0xFF007A87,
            dateString = dateString,
            scheduledTime = scheduledTime,
            reminderLabel = med?.reminderTimes?.firstOrNull { it.toTimeString() == scheduledTime }?.label ?: "Reminder"
        )).copy(
            status = status,
            actionTimestamp = System.currentTimeMillis(),
            snoozeUntil = snoozeUntil,
            notes = notes
        )

        medicineLogDao.insertOrUpdateLog(log)
    }

    suspend fun undoDoseAction(doseItem: TodayDoseItem) {
        val existingLog = medicineLogDao.getLog(
            doseItem.medicine.id,
            doseItem.dateString,
            doseItem.reminderTime.toTimeString()
        )
        if (existingLog != null) {
            medicineLogDao.updateStatus(existingLog.id, DoseStatus.UPCOMING, System.currentTimeMillis())
        }
    }

    suspend fun clearAllData() {
        val medicines = medicineDao.getAllActiveMedicinesSync()
        for (med in medicines) {
            alarmScheduler.cancelMedicineAlarms(med)
        }
        medicineDao.deleteAllMedicines()
        medicineLogDao.deleteAllLogs()
    }
}
