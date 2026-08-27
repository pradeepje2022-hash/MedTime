package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.Medicine
import com.example.model.ReminderTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"
        const val ACTION_ALARM = "com.example.medtime.ALARM_TRIGGER"
        const val EXTRA_MEDICINE_ID = "extra_medicine_id"
        const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_INSTRUCTION = "extra_instruction"
        const val EXTRA_DATE_STRING = "extra_date_string"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"

        fun getRequestCode(medicineId: Long, hour: Int, minute: Int): Int {
            return (medicineId * 10000 + hour * 100 + minute).toInt()
        }

        fun getSnoozeRequestCode(medicineId: Long, hour: Int, minute: Int): Int {
            return (medicineId * 10000 + hour * 100 + minute + 500000).toInt()
        }
    }

    /**
     * Schedule all active reminder times for a medicine
     */
    fun scheduleMedicine(medicine: Medicine) {
        if (!medicine.isActive) {
            cancelMedicineAlarms(medicine)
            return
        }

        for (reminderTime in medicine.reminderTimes) {
            scheduleNextAlarm(medicine, reminderTime)
        }
    }

    /**
     * Compute next occurrence timestamp and schedule exact alarm
     */
    fun scheduleNextAlarm(medicine: Medicine, reminderTime: ReminderTime) {
        val nextTriggerTime = calculateNextTriggerTime(medicine, reminderTime) ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_MEDICINE_NAME, medicine.name)
            putExtra(EXTRA_DOSAGE, medicine.fullDosage)
            putExtra(EXTRA_TYPE, medicine.medicineType.displayName)
            putExtra(EXTRA_SCHEDULED_TIME, reminderTime.toTimeString())
            putExtra(EXTRA_INSTRUCTION, medicine.mealInstruction.displayName)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            putExtra(EXTRA_DATE_STRING, sdf.format(Date(nextTriggerTime)))
            putExtra(EXTRA_IS_SNOOZE, false)
        }

        val requestCode = getRequestCode(medicine.id, reminderTime.hour, reminderTime.minute)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    val showIntent = Intent(context, com.example.MainActivity::class.java)
                    val showPendingIntent = PendingIntent.getActivity(
                        context,
                        requestCode,
                        showIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(nextTriggerTime, showPendingIntent)
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
            }
            Log.d(TAG, "Scheduled alarm for ${medicine.name} at ${Date(nextTriggerTime)}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm for ${medicine.name}", e)
        }
    }

    /**
     * Schedule a snoozed reminder for X minutes from now
     */
    fun scheduleSnooze(
        medicineId: Long,
        medicineName: String,
        dosage: String,
        scheduledTime: String,
        instruction: String,
        type: String,
        snoozeMinutes: Int
    ) {
        val snoozeMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_INSTRUCTION, instruction)
            putExtra(EXTRA_DATE_STRING, todayStr)
            putExtra(EXTRA_IS_SNOOZE, true)
        }

        val parts = scheduledTime.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val requestCode = getSnoozeRequestCode(medicineId, h, m)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeMillis, pendingIntent)
            }
            Log.d(TAG, "Snoozed alarm for $medicineName for $snoozeMinutes mins (at ${Date(snoozeMillis)})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule snooze", e)
        }
    }

    /**
     * Cancel all alarms for a medicine
     */
    fun cancelMedicineAlarms(medicine: Medicine) {
        for (reminderTime in medicine.reminderTimes) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM
            }
            val requestCode = getRequestCode(medicine.id, reminderTime.hour, reminderTime.minute)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }

            val snoozeCode = getSnoozeRequestCode(medicine.id, reminderTime.hour, reminderTime.minute)
            val snoozePending = PendingIntent.getBroadcast(
                context,
                snoozeCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (snoozePending != null) {
                alarmManager.cancel(snoozePending)
                snoozePending.cancel()
            }
        }
    }

    /**
     * Calculate next trigger time in epoch millis
     */
    private fun calculateNextTriggerTime(medicine: Medicine, reminderTime: ReminderTime): Long? {
        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminderTime.hour)
            set(Calendar.MINUTE, reminderTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If today's reminder time has already passed, start searching from tomorrow
        if (calendar.before(now) || calendar.timeInMillis <= now.timeInMillis + 1000) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Search up to 30 days ahead for the next valid scheduled day
        for (i in 0..30) {
            if (medicine.isScheduledForDay(calendar)) {
                return calendar.timeInMillis
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    /**
     * Schedules a quick test alarm (fires in 5 seconds) to test device sound and notifications
     */
    fun scheduleTestAlarm(medicineName: String = "Test Paracetamol", dosage: String = "1 tablet") {
        val triggerTime = System.currentTimeMillis() + 5000L
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeSdf = SimpleDateFormat("HH:mm", Locale.US)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra(EXTRA_MEDICINE_ID, -1L)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_TYPE, "Tablet")
            putExtra(EXTRA_SCHEDULED_TIME, timeSdf.format(Date(triggerTime)))
            putExtra(EXTRA_INSTRUCTION, "After meal")
            putExtra(EXTRA_DATE_STRING, sdf.format(Date(triggerTime)))
            putExtra(EXTRA_IS_SNOOZE, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            99999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                val showIntent = Intent(context, com.example.MainActivity::class.java)
                val showPendingIntent = PendingIntent.getActivity(
                    context, 99999, showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent), pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule test alarm", e)
        }
    }
}
