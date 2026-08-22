package com.medtime.reminder.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.medtime.reminder.MainActivity
import com.medtime.reminder.R
import com.medtime.reminder.alarm.AlarmRingActivity
import com.medtime.reminder.alarm.NotificationActionReceiver

object NotificationHelper {
    const val CHANNEL_ID = "medtime_reminders"
    const val ACTION_TAKEN = "com.medtime.reminder.ACTION_TAKEN"
    const val ACTION_SNOOZE = "com.medtime.reminder.ACTION_SNOOZE"
    const val ACTION_SKIP = "com.medtime.reminder.ACTION_SKIP"
    const val EXTRA_HISTORY_ID = "extra_history_id"
    const val EXTRA_MEDICINE_ID = "extra_medicine_id"
    const val EXTRA_REMINDER_TIME_ID = "extra_reminder_time_id"
    const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
    const val EXTRA_DOSAGE = "extra_dosage"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Medicine Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts you when it's time to take your medicine"
                    enableVibration(true)
                    setBypassDnd(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showReminderNotification(
        context: Context,
        historyId: Long,
        medicineId: Long,
        reminderTimeId: Long,
        medicineName: String,
        dosage: String,
        timeLabel: String
    ) {
        createChannel(context)

        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPending = PendingIntent.getActivity(
            context, historyId.toInt(), contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun actionIntent(action: String, extraSnooze: Int? = null): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_HISTORY_ID, historyId)
                putExtra(EXTRA_MEDICINE_ID, medicineId)
                putExtra(EXTRA_REMINDER_TIME_ID, reminderTimeId)
                putExtra(EXTRA_MEDICINE_NAME, medicineName)
                putExtra(EXTRA_DOSAGE, dosage)
                if (extraSnooze != null) putExtra(EXTRA_SNOOZE_MINUTES, extraSnooze)
            }
            val requestCode = (historyId.toInt() * 10) + when (action) {
                ACTION_TAKEN -> 1
                ACTION_SNOOZE -> 2
                ACTION_SKIP -> 3
                else -> 4
            }
            return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val fullScreenIntent = Intent(context, AlarmRingActivity::class.java).apply {
            putExtra(EXTRA_HISTORY_ID, historyId)
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_REMINDER_TIME_ID, reminderTimeId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPending = PendingIntent.getActivity(
            context, (historyId.toInt() * 10) + 9, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Medicine Reminder")
            .setContentText("Time to take $medicineName – $dosage")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Time to take $medicineName – $dosage ($timeLabel)")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .setFullScreenIntent(fullScreenPending, true)
            .addAction(0, "Taken", actionIntent(ACTION_TAKEN))
            .addAction(0, "Snooze", actionIntent(ACTION_SNOOZE, 10))
            .addAction(0, "Skip", actionIntent(ACTION_SKIP))
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(historyId.toInt(), notification)
    }

    fun cancelNotification(context: Context, historyId: Long) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(historyId.toInt())
    }
}
