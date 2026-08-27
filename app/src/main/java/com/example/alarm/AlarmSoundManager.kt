package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.model.AlarmSoundOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AlarmSoundManager {
    private const val TAG = "AlarmSoundManager"
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var autoStopRunnable: Runnable? = null

    data class RingingMedicineInfo(
        val medicineId: Long,
        val medicineName: String,
        val dosage: String,
        val scheduledTime: String,
        val instruction: String,
        val type: String,
        val notificationId: Int
    )

    private val _currentRingingMedicine = MutableStateFlow<RingingMedicineInfo?>(null)
    val currentRingingMedicine: StateFlow<RingingMedicineInfo?> = _currentRingingMedicine.asStateFlow()

    fun startAlarm(
        context: Context,
        soundOption: AlarmSoundOption = AlarmSoundOption.DEFAULT_ALARM,
        vibrate: Boolean = true,
        medicineInfo: RingingMedicineInfo? = null
    ) {
        stopAlarm(context)

        _currentRingingMedicine.value = medicineInfo

        try {
            val alertUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm sound", e)
        }

        if (vibrate) {
            startVibration(context)
        }

        // Auto-stop alarm sound after 60 seconds as safety
        autoStopRunnable = Runnable {
            stopAlarm(context)
        }
        handler.postDelayed(autoStopRunnable!!, 60_000L)
    }

    private fun startVibration(context: Context) {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 800, 400, 800, 400, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration", e)
        }
    }

    fun stopAlarm(context: Context) {
        autoStopRunnable?.let { handler.removeCallbacks(it) }
        autoStopRunnable = null

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibration", e)
        } finally {
            vibrator = null
        }

        _currentRingingMedicine.value = null
    }

    fun isRinging(): Boolean {
        return _currentRingingMedicine.value != null || mediaPlayer?.isPlaying == true
    }
}
