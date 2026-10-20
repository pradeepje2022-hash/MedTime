package com.bluepinger.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

class PingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startPinging()
        return START_NOT_STICKY
    }

    private fun startPinging() {
        if (job?.isActive == true) return
        createChannel()
        val host = Prefs.host(this)
        val interval = Prefs.interval(this).coerceAtLeast(500L)

        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification("Starting…", host), type)

        PingState.reset()
        PingState.running.value = true
        PingTileService.refresh(this)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "bluepinger:ping").apply {
            acquire(12 * 60 * 60 * 1000L)
        }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        job = scope.launch {
            while (isActive) {
                val t0 = SystemClock.elapsedRealtime()
                val rtt = PingEngine.ping(host)
                PingState.stats.update { it.add(rtt, System.currentTimeMillis()) }
                val s = PingState.stats.value
                val text = if (rtt != null) {
                    "%.0f ms  ·  avg %.0f  ·  loss %.0f%%".format(rtt, s.avg, s.lossPct)
                } else {
                    "Timeout  ·  loss %.0f%%".format(s.lossPct)
                }
                nm.notify(NOTIF_ID, buildNotification(text, host))
                PingTileService.refresh(this@PingService)
                val wait = interval - (SystemClock.elapsedRealtime() - t0)
                delay(wait.coerceAtLeast(200L))
            }
        }
    }

    private fun buildNotification(text: String, host: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, PingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle("Pinging $host")
            .setContentText(text)
            .setColor(0xFF2F80FF.toInt())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .build()
    }

    private fun createChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(CHANNEL_ID, "Ping monitor", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(ch)
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        PingState.running.value = false
        PingTileService.refresh(this)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "ping_monitor"
        private const val NOTIF_ID = 1001
        const val ACTION_STOP = "com.bluepinger.app.STOP"

        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, PingService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, PingService::class.java))
        }
    }
}
