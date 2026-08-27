package com.example.alarm

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.MedTimeApplication
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Keeps the medicine alarm process alive after AlarmManager delivers the broadcast.
 * The service is foreground only while an alarm is actively ringing.
 */
class AlarmRingingService : Service() {
    private var workJob: Job? = null
    private var notificationId: Int = 99999

    override fun onCreate() {
        super.onCreate()
        createChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) return START_NOT_STICKY

        val medicineId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICINE_ID, -1L)
        val medicineName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: "Medicine"
        val dosage = intent.getStringExtra(AlarmScheduler.EXTRA_DOSAGE) ?: "1 dose"
        val type = intent.getStringExtra(AlarmScheduler.EXTRA_TYPE) ?: "Tablet"
        val scheduledTime = intent.getStringExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME) ?: "08:00"
        val instruction = intent.getStringExtra(AlarmScheduler.EXTRA_INSTRUCTION) ?: ""
        val dateString = intent.getStringExtra(AlarmScheduler.EXTRA_DATE_STRING) ?: ""
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)

        notificationId = (if (medicineId > 0) medicineId.toInt() * 100 + scheduledTime.hashCode() % 100 else 99999).let { if (it < 0) -it else it }

        // Start foreground immediately, before doing any longer work.
        val notificationIntent = Intent(this, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_RINGING_DIALOG", true)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_DOSAGE, dosage)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(AlarmScheduler.EXTRA_INSTRUCTION, instruction)
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, notificationId, notificationIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val foregroundNotification = androidx.core.app.NotificationCompat.Builder(this, AlarmReceiver.CHANNEL_ID_MED_REMINDERS)
            .setSmallIcon(com.example.R.drawable.ic_launcher_foreground)
            .setContentTitle(if (isSnooze) "⏰ Snoozed: Time for $medicineName" else "💊 Medicine Reminder")
            .setContentText("Time to take $medicineName – $dosage")
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(notificationId, foregroundNotification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(notificationId, foregroundNotification)
        }

        val settings = SettingsRepository(this).settings.value
        AlarmSoundManager.startAlarm(
            context = this,
            soundOption = settings.soundOption,
            vibrate = settings.vibrate,
            medicineInfo = AlarmSoundManager.RingingMedicineInfo(
                medicineId, medicineName, dosage, scheduledTime, instruction, type, notificationId
            )
        )

        // Replace the simple foreground notification with the full interactive reminder.
        AlarmReceiver().showMedicineNotification(this, medicineId, medicineName, dosage, scheduledTime, instruction, dateString, isSnooze, notificationId)

        if (medicineId > 0 && !isSnooze) {
            workJob?.cancel()
            workJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = application as? MedTimeApplication
                    val medicine = app?.medicineRepository?.getMedicineById(medicineId)
                    if (medicine != null && medicine.isActive) {
                        medicine.reminderTimes.firstOrNull { it.toTimeString() == scheduledTime }?.let {
                            AlarmScheduler(this@AlarmRingingService).scheduleNextAlarm(medicine, it)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule next alarm", e)
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        workJob?.cancel()
        AlarmSoundManager.stopAlarm(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(android.app.NotificationManager::class.java)
            val channel = android.app.NotificationChannel(
                AlarmReceiver.CHANNEL_ID_MED_REMINDERS,
                "Medicine Reminders",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Medicine reminder alarms"
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "com.example.medtime.START_RINGING_ALARM"
        private const val TAG = "AlarmRingingService"
    }
}
