package com.medtime.reminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.medtime.reminder.notification.NotificationHelper
import com.medtime.reminder.repository.MedicineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICINE_ID, -1)
        val reminderTimeId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_TIME_ID, -1)
        if (medicineId == -1L || reminderTimeId == -1L) return

        // Keep CPU awake while we do async DB work + show notification.
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "medtime:AlarmReceiverWakeLock"
        )
        wakeLock.acquire(60_000)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = MedicineRepository(context)
                val medicine = repo.getMedicineOnce(medicineId)
                val reminderTime = repo.reminderTimeDao.getByIdOnce(reminderTimeId)

                if (medicine != null && reminderTime != null && medicine.active) {
                    val today = LocalDate.now()
                    val entry = repo.getOrCreateHistoryEntry(medicine, reminderTime, today.toEpochDay())

                    NotificationHelper.showReminderNotification(
                        context = context,
                        historyId = entry.id,
                        medicineId = medicine.id,
                        reminderTimeId = reminderTime.id,
                        medicineName = medicine.name,
                        dosage = medicine.dosage,
                        timeLabel = reminderTime.label
                    )

                    // Schedule the following occurrence (next matching day).
                    AlarmScheduler.scheduleNext(context, medicine, reminderTime)
                }
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
                pendingResult.finish()
            }
        }
    }
}
