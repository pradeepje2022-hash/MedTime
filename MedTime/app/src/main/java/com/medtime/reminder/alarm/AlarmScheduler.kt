package com.medtime.reminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.medtime.reminder.data.Medicine
import com.medtime.reminder.data.ReminderTime
import com.medtime.reminder.util.DateTimeUtils
import java.time.LocalDate

/**
 * Schedules and cancels exact alarms for medicine reminders.
 * Each (medicineId, reminderTimeId) pair maps to a stable PendingIntent request code,
 * so rescheduling the same reminder simply replaces the previous alarm.
 */
object AlarmScheduler {

    const val EXTRA_MEDICINE_ID = "extra_medicine_id"
    const val EXTRA_REMINDER_TIME_ID = "extra_reminder_time_id"

    private fun requestCode(medicineId: Long, reminderTimeId: Long): Int {
        // Stable, collision-resistant enough for typical usage volumes.
        return ((medicineId % 100000) * 1000 + (reminderTimeId % 1000)).toInt()
    }

    private fun buildPendingIntent(
        context: Context,
        medicine: Medicine,
        reminderTime: ReminderTime
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_REMINDER_TIME_ID, reminderTime.id)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(medicine.id, reminderTime.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Finds the next date (today or later) on which this reminder should fire, respecting
     * days-of-week mask and optional start/end date range. Returns null if no future date matches. */
    fun nextTriggerDate(medicine: Medicine, reminderTime: ReminderTime, from: LocalDate = LocalDate.now()): LocalDate? {
        for (offset in 0..3660) { // look ahead up to ~10 years
            val candidate = from.plusDays(offset.toLong())
            if (medicine.endDate != null) {
                val end = java.time.Instant.ofEpochMilli(medicine.endDate)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                if (candidate.isAfter(end)) return null
            }
            if (medicine.startDate != null) {
                val start = java.time.Instant.ofEpochMilli(medicine.startDate)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                if (candidate.isBefore(start)) continue
            }
            if (DateTimeUtils.matchesDay(medicine.daysMask, candidate)) {
                if (offset == 0) {
                    // if today, only valid if time hasn't already passed
                    val now = java.time.LocalDateTime.now()
                    val triggerToday = java.time.LocalDateTime.of(candidate, java.time.LocalTime.of(reminderTime.hour, reminderTime.minute))
                    if (triggerToday.isAfter(now)) return candidate else continue
                }
                return candidate
            }
        }
        return null
    }

    fun scheduleNext(context: Context, medicine: Medicine, reminderTime: ReminderTime) {
        if (!medicine.active) {
            cancel(context, medicine, reminderTime)
            return
        }
        val date = nextTriggerDate(medicine, reminderTime) ?: run {
            cancel(context, medicine, reminderTime)
            return
        }
        val triggerMillis = DateTimeUtils.toEpochMillis(date, reminderTime.hour, reminderTime.minute)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, medicine, reminderTime)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun scheduleOneOff(context: Context, medicine: Medicine, reminderTime: ReminderTime, triggerMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, medicine, reminderTime)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun cancel(context: Context, medicine: Medicine, reminderTime: ReminderTime) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context, medicine, reminderTime))
    }

    /** Reschedules all active medicines' reminders. Used on boot and app start. */
    suspend fun rescheduleAll(context: Context) {
        val repo = com.medtime.reminder.repository.MedicineRepository(context)
        val activeMedicines = repo.getAllActiveMedicines()
        for (medicine in activeMedicines) {
            val times = repo.getRemindersForMedicineOnce(medicine.id)
            for (time in times) {
                scheduleNext(context, medicine, time)
            }
        }
    }
}
