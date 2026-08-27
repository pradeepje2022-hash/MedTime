package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.alarm.AlarmReceiver
import com.example.data.AppDatabase
import com.example.data.MedicineRepository
import com.example.data.SettingsRepository
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedTimeApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var medicineRepository: MedicineRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getDatabase(this)
        settingsRepository = SettingsRepository(this)
        medicineRepository = MedicineRepository(this, database.medicineDao(), database.medicineLogDao())

        // Initialize Google Mobile Ads SDK safely
        try {
            MobileAds.initialize(this) { status ->
                android.util.Log.d("MedTimeAdMob", "MobileAds initialization complete: $status")
            }
        } catch (e: Exception) {
            android.util.Log.e("MedTimeAdMob", "MobileAds initialization failed", e)
        }

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val channel = NotificationChannel(
                AlarmReceiver.CHANNEL_ID_MED_REMINDERS,
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarms and notifications for scheduled medications"
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }
}
