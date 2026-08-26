package com.example.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.MedTimeApplication
import com.example.data.SettingsRepository
import com.example.model.DoseStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val medicineId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICINE_ID, -1L)
        val medicineName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: "Medicine"
        val dosage = intent.getStringExtra(AlarmScheduler.EXTRA_DOSAGE) ?: ""
        val scheduledTime = intent.getStringExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME) ?: "08:00"
        val instruction = intent.getStringExtra(AlarmScheduler.EXTRA_INSTRUCTION) ?: ""
        val type = intent.getStringExtra(AlarmScheduler.EXTRA_TYPE) ?: "Tablet"

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateString = intent.getStringExtra(AlarmScheduler.EXTRA_DATE_STRING).takeIf { !it.isNullOrEmpty() }
            ?: sdf.format(Date())

        // Stop ringing alarm
        AlarmSoundManager.stopAlarm(context)

        // Dismiss notification
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notifId != 0) {
            notificationManager.cancel(notifId)
        }

        val app = context.applicationContext as? MedTimeApplication
        val repo = app?.medicineRepository

        when (intent.action) {
            ACTION_TAKEN -> {
                Toast.makeText(context, "Marked $medicineName as Taken! Great job!", Toast.LENGTH_SHORT).show()
                if (medicineId > 0 && repo != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        repo.recordDoseAction(
                            medicineId = medicineId,
                            medicineName = medicineName,
                            dosage = dosage,
                            dateString = dateString,
                            scheduledTime = scheduledTime,
                            status = DoseStatus.TAKEN
                        )
                    }
                }
            }

            ACTION_SNOOZE -> {
                val settingsRepo = SettingsRepository(context)
                val snoozeMinutes = settingsRepo.settings.value.defaultSnoozeMinutes
                Toast.makeText(context, "Snoozed $medicineName for $snoozeMinutes mins", Toast.LENGTH_SHORT).show()

                if (medicineId > 0) {
                    AlarmScheduler(context).scheduleSnooze(
                        medicineId = medicineId,
                        medicineName = medicineName,
                        dosage = dosage,
                        scheduledTime = scheduledTime,
                        instruction = instruction,
                        type = type,
                        snoozeMinutes = snoozeMinutes
                    )
                }

                if (medicineId > 0 && repo != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        repo.recordDoseAction(
                            medicineId = medicineId,
                            medicineName = medicineName,
                            dosage = dosage,
                            dateString = dateString,
                            scheduledTime = scheduledTime,
                            status = DoseStatus.SNOOZED,
                            snoozeUntil = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
                        )
                    }
                }
            }

            ACTION_SKIP -> {
                Toast.makeText(context, "Skipped $medicineName dose", Toast.LENGTH_SHORT).show()
                if (medicineId > 0 && repo != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        repo.recordDoseAction(
                            medicineId = medicineId,
                            medicineName = medicineName,
                            dosage = dosage,
                            dateString = dateString,
                            scheduledTime = scheduledTime,
                            status = DoseStatus.SKIPPED
                        )
                    }
                }
            }

            ACTION_DISMISS -> {
                // Just stopped sound and removed notification
            }
        }
    }

    companion object {
        const val ACTION_TAKEN = "com.example.medtime.ACTION_TAKEN"
        const val ACTION_SNOOZE = "com.example.medtime.ACTION_SNOOZE"
        const val ACTION_SKIP = "com.example.medtime.ACTION_SKIP"
        const val ACTION_DISMISS = "com.example.medtime.ACTION_DISMISS_ALARM"

        const val EXTRA_NOTIF_ID = "extra_notif_id"
    }
}
