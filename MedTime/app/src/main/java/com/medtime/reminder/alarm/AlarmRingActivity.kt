package com.medtime.reminder.alarm

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.firstOrNull
import com.medtime.reminder.data.DoseStatus
import com.medtime.reminder.notification.NotificationHelper
import com.medtime.reminder.repository.MedicineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmRingActivity : ComponentActivity() {

    companion object {
        private var mediaPlayer: android.media.MediaPlayer? = null
        private var vibrator: Vibrator? = null

        fun stopRinging(context: Context) {
            mediaPlayer?.let { if (it.isPlaying) it.stop(); it.release() }
            mediaPlayer = null
            vibrator?.cancel()
            vibrator = null
        }
    }

    private var historyId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen and turn screen on.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        historyId = intent.getLongExtra(NotificationHelper.EXTRA_HISTORY_ID, -1)
        val medicineId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICINE_ID, -1)
        val reminderTimeId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_TIME_ID, -1)
        val medicineName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_NAME) ?: ""
        val dosage = intent.getStringExtra(NotificationHelper.EXTRA_DOSAGE) ?: ""

        startAlarmSoundAndVibration()

        setContent {
            MaterialTheme {
                AlarmScreen(
                    medicineName = medicineName,
                    dosage = dosage,
                    onTaken = { resolveAndFinish(medicineId, DoseStatus.TAKEN) },
                    onSnooze = { snoozeAndFinish(medicineId, reminderTimeId) },
                    onSkip = { resolveAndFinish(medicineId, DoseStatus.SKIPPED) }
                )
            }
        }
    }

    private fun startAlarmSoundAndVibration() {
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = android.media.MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 800, 400)
        vibrator?.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0))
    }

    private fun resolveAndFinish(medicineId: Long, status: DoseStatus) {
        stopRinging(this)
        CoroutineScope(Dispatchers.IO).launch {
            val repo = MedicineRepository(this@AlarmRingActivity)
            repo.updateHistoryStatus(historyId, status)
        }
        NotificationHelper.cancelNotification(this, historyId)
        finish()
    }

    private fun snoozeAndFinish(medicineId: Long, reminderTimeId: Long) {
        stopRinging(this)
        CoroutineScope(Dispatchers.IO).launch {
            val repo = MedicineRepository(this@AlarmRingActivity)
            val medicine = repo.getMedicineOnce(medicineId)
            val reminderTime = repo.reminderTimeDao.getByIdOnce(reminderTimeId)
            val prefs = com.medtime.reminder.util.PreferencesManager(this@AlarmRingActivity)
            val minutes = prefs.defaultSnoozeMinutes.firstOrNull() ?: 10
            if (medicine != null && reminderTime != null) {
                val triggerAt = System.currentTimeMillis() + minutes * 60_000L
                AlarmScheduler.scheduleOneOff(this@AlarmRingActivity, medicine, reminderTime, triggerAt)
            }
        }
        NotificationHelper.cancelNotification(this, historyId)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRinging(this)
    }
}

@Composable
private fun AlarmScreen(
    medicineName: String,
    dosage: String,
    onTaken: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Medication,
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text("Time to take your medicine", fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text(medicineName, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(dosage, fontSize = 18.sp)
            Spacer(Modifier.height(48.dp))
            Button(onClick = onTaken, modifier = Modifier.fillMaxWidth()) {
                Text("Taken", fontSize = 18.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth()) {
                Text("Snooze", fontSize = 18.sp)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Skip", fontSize = 16.sp)
            }
        }
    }
}
