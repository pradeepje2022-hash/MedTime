package com.medtime.reminder

import android.app.*
import android.content.*
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Medicine"
        val dosage = intent.getStringExtra("dosage") ?: ""
        val time = intent.getStringExtra("time") ?: ""
        val id = intent.getLongExtra("medicineId", 0L)
        val channelId = "medicine_reminders"

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "Medicine Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Scheduled medicine reminders"
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val taken = PendingIntent.getBroadcast(context, id.toInt() + 1,
            actionIntent(context, "TAKEN", id, name, dosage, time),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val skip = PendingIntent.getBroadcast(context, id.toInt() + 2,
            actionIntent(context, "SKIPPED", id, name, dosage, time),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val snooze = PendingIntent.getBroadcast(context, id.toInt() + 3,
            actionIntent(context, "SNOOZE", id, name, dosage, time),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Medicine Reminder")
            .setContentText("Time to take $name – $dosage")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Time to take $name – $dosage\nScheduled: $time"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .addAction(0, "Taken", taken)
            .addAction(0, "Snooze", snooze)
            .addAction(0, "Skip", skip)
            .build()
        nm.notify(id.toInt(), notification)

        AlarmScheduler.scheduleAll(context)
    }

    private fun actionIntent(c: Context, action: String, id: Long, n: String, d: String, t: String) =
        Intent(c, ActionReceiver::class.java).apply {
            this.action = action
            putExtra("medicineId", id); putExtra("name", n); putExtra("dosage", d); putExtra("time", t)
        }
}
