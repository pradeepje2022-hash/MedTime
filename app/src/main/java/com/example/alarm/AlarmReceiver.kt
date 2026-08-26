package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SettingsRepository
import com.example.model.DoseStatus
import com.example.model.MedicineLog
import com.example.MedTimeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICINE_ID, -1L)
        val medicineName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: "Medicine"
        val dosage = intent.getStringExtra(AlarmScheduler.EXTRA_DOSAGE) ?: "1 dose"
        val type = intent.getStringExtra(AlarmScheduler.EXTRA_TYPE) ?: "Tablet"
        val scheduledTime = intent.getStringExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME) ?: "08:00"
        val instruction = intent.getStringExtra(AlarmScheduler.EXTRA_INSTRUCTION) ?: ""
        val dateString = intent.getStringExtra(AlarmScheduler.EXTRA_DATE_STRING) ?: ""
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)

        // Acquire WakeLock briefly so device stays awake while processing and ringing
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "medtime:AlarmWakeLock"
        )
        wakeLock.acquire(10_000L)

        val settingsRepo = SettingsRepository(context)
        val settings = settingsRepo.settings.value

        val notificationId = (if (medicineId > 0) medicineId.toInt() * 100 + scheduledTime.hashCode() % 100 else 99999).let {
            if (it < 0) -it else it
        }

        // 1. Play sound & start vibration
        val ringInfo = AlarmSoundManager.RingingMedicineInfo(
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            scheduledTime = scheduledTime,
            instruction = instruction,
            type = type,
            notificationId = notificationId
        )
        AlarmSoundManager.startAlarm(
            context = context,
            soundOption = settings.soundOption,
            vibrate = settings.vibrate,
            medicineInfo = ringInfo
        )

        // 2. Show notification
        showMedicineNotification(
            context = context,
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            scheduledTime = scheduledTime,
            instruction = instruction,
            dateString = dateString,
            isSnooze = isSnooze,
            notificationId = notificationId
        )

        // 3. Reschedule the NEXT regular cycle if this was not a test alarm and not a snooze
        if (medicineId > 0 && !isSnooze) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as? MedTimeApplication
                    val medicine = app?.medicineRepository?.getMedicineById(medicineId)
                    if (medicine != null && medicine.isActive) {
                        val reminder = medicine.reminderTimes.firstOrNull { it.toTimeString() == scheduledTime }
                        if (reminder != null) {
                            AlarmScheduler(context).scheduleNextAlarm(medicine, reminder)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showMedicineNotification(
        context: Context,
        medicineId: Long,
        medicineName: String,
        dosage: String,
        scheduledTime: String,
        instruction: String,
        dateString: String,
        isSnooze: Boolean,
        notificationId: Int
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = CHANNEL_ID_MED_REMINDERS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Medicine Reminders"
            val descriptionText = "Urgent scheduled medicine alarm alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Main Tap Intent -> Opens MainActivity with Ringing UI
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_RINGING_DIALOG", true)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_DOSAGE, dosage)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(AlarmScheduler.EXTRA_INSTRUCTION, instruction)
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: TAKEN
        val takenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TAKEN
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_DOSAGE, dosage)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(AlarmScheduler.EXTRA_DATE_STRING, dateString)
            putExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, notificationId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: SNOOZE
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_DOSAGE, dosage)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(AlarmScheduler.EXTRA_INSTRUCTION, instruction)
            putExtra(AlarmScheduler.EXTRA_DATE_STRING, dateString)
            putExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: SKIP
        val skipIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SKIP
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_DOSAGE, dosage)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(AlarmScheduler.EXTRA_DATE_STRING, dateString)
            putExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, notificationId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Delete Intent (if user swipes notification away -> stop alarm sound)
        val deleteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, notificationId)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 4,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isSnooze) "⏰ Snoozed: Time for $medicineName" else "💊 Medicine Reminder"
        val subtitle = "Time to take $medicineName – $dosage" + if (instruction.isNotEmpty()) " ($instruction)" else ""

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$subtitle\nScheduled for $scheduledTime"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setFullScreenIntent(contentPendingIntent, true)
            .addAction(0, "✓ Taken", takenPendingIntent)
            .addAction(0, "⏱ Snooze (10m)", snoozePendingIntent)
            .addAction(0, "↷ Skip", skipPendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    companion object {
        const val CHANNEL_ID_MED_REMINDERS = "medtime_medicine_reminders"
    }
}
