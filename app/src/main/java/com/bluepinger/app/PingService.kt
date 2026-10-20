package com.bluepinger.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class PingService : Service() {
    private val engine = PingEngine()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "bluepinger_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPingingService()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundServiceCompat()
                acquireWakeLock()
                engine.start()
                observeStateForNotification()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceCompat() {
        val notification = buildNotification("Initializing ping...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeStateForNotification() {
        serviceScope.launch {
            PingState.status.collect { status ->
                if (status.isRunning) {
                    val text = if (status.lastLatencyMs != null) {
                        "Host: ${status.host} | Ping: ${status.lastLatencyMs}ms"
                    } else {
                        "Host: ${status.host} | Pinging..."
                    }
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, buildNotification(text))
                }
            }
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val stopIntent = Intent(this, PingService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BluePinger Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ping Service Status",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BluePinger::PingWakeLock").apply {
                acquire(60 * 60 * 1000L)
            }
        }
    }

    private fun stopPingingService() {
        engine.stop()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopPingingService()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}