package com.medtime.reminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medtime.reminder.data.DoseStatus
import com.medtime.reminder.notification.NotificationHelper
import com.medtime.reminder.repository.MedicineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val historyId = intent.getLongExtra(NotificationHelper.EXTRA_HISTORY_ID, -1)
        val medicineId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICINE_ID, -1)
        val reminderTimeId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_TIME_ID, -1)
        val medicineName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_NAME) ?: ""
        val dosage = intent.getStringExtra(NotificationHelper.EXTRA_DOSAGE) ?: ""
        if (historyId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = MedicineRepository(context)
                when (intent.action) {
                    NotificationHelper.ACTION_TAKEN -> {
                        repo.updateHistoryStatus(historyId, DoseStatus.TAKEN)
                        AlarmRingActivity.stopRinging(context)
                        NotificationHelper.cancelNotification(context, historyId)
                    }
                    NotificationHelper.ACTION_SKIP -> {
                        repo.updateHistoryStatus(historyId, DoseStatus.SKIPPED)
                        AlarmRingActivity.stopRinging(context)
                        NotificationHelper.cancelNotification(context, historyId)
                    }
                    NotificationHelper.ACTION_SNOOZE -> {
                        val minutes = intent.getIntExtra(NotificationHelper.EXTRA_SNOOZE_MINUTES, 10)
                        AlarmRingActivity.stopRinging(context)
                        NotificationHelper.cancelNotification(context, historyId)
                        val medicine = repo.getMedicineOnce(medicineId)
                        val reminderTime = repo.reminderTimeDao.getByIdOnce(reminderTimeId)
                        if (medicine != null && reminderTime != null) {
                            val triggerAt = System.currentTimeMillis() + minutes * 60_000L
                            AlarmScheduler.scheduleOneOff(context, medicine, reminderTime, triggerAt)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
