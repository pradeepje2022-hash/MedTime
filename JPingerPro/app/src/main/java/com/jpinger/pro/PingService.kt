package com.jpinger.pro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock

/**
 * Foreground service: keeps the pinger alive when the app is in the background or the
 * screen is off (e.g. while a video is playing in another app).
 */
class PingService : Service() {

    companion object {
        const val ACTION_STOP = "com.jpinger.pro.STOP"
        private const val EXTRA_LABEL = "label"
        private const val EXTRA_HOST = "host"
        private const val EXTRA_PORT = "port"
        private const val EXTRA_INTERVAL = "interval"
        private const val CHANNEL_ID = "jpinger_running"
        private const val NOTIF_ID = 1

        fun start(ctx: Context, t: Target, intervalMs: Long) {
            val i = Intent(ctx, PingService::class.java)
                .putExtra(EXTRA_LABEL, t.label)
                .putExtra(EXTRA_HOST, t.host)
                .putExtra(EXTRA_PORT, t.port)
                .putExtra(EXTRA_INTERVAL, intervalMs)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, PingService::class.java))
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var netCallback: ConnectivityManager.NetworkCallback? = null
    private lateinit var nm: NotificationManager

    private val listener = object : PingEngine.Listener {
        private var lastNotify = 0L

        override fun onSnapshot(s: Snapshot) {
            if (!s.running) return
            val now = SystemClock.elapsedRealtime()
            if (now - lastNotify < 2000) return          // don't spam the notification
            lastNotify = now
            val ping = when {
                s.sent == 0 -> "…"
                s.last >= 0 -> "${s.last} ms"
                else -> "TIMEOUT"
            }
            val text = "${s.network} • $ping • Loss ${s.recentLossPct}% • " +
                "Jitter ${s.jitter} ms • ${s.quality.name.lowercase().replaceFirstChar { it.uppercase() }}"
            nm.notify(NOTIF_ID, buildNotification(text))
        }

        override fun onLog(e: LogEntry) {}
    }

    override fun onCreate() {
        super.onCreate()
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "J Pinger running", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Must be called quickly after startForegroundService()
        val n = buildNotification("Starting…")
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }

        val host = intent.getStringExtra(EXTRA_HOST)
        if (host.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }
        val target = Target(intent.getStringExtra(EXTRA_LABEL) ?: host, host, intent.getIntExtra(EXTRA_PORT, 443))
        val interval = intent.getLongExtra(EXTRA_INTERVAL, 1000L)

        acquireLocks()
        watchNetwork()
        PingEngine.addListener(listener)
        PingEngine.start(target, interval)
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    override fun onDestroy() {
        PingEngine.removeListener(listener)
        PingEngine.stop()
        unwatchNetwork()
        releaseLocks()
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE)
        else stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------- helpers ----------

    private fun piFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)

    @Suppress("DEPRECATION")
    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), piFlags())
        val stop = PendingIntent.getService(
            this, 1, Intent(this, PingService::class.java).setAction(ACTION_STOP), piFlags()
        )
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID)
        else Notification.Builder(this)
        return b.setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("⚡ J Pinger Pro")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop)
            .build()
    }

    @Suppress("DEPRECATION")
    private fun acquireLocks() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jpinger:ping").apply {
                setReferenceCounted(false)
                acquire(6 * 60 * 60 * 1000L)            // safety timeout: 6 hours
            }
        } catch (e: Exception) {
        }
        try {
            // Stops Wi-Fi from dropping into power-save between pings
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val mode = if (Build.VERSION.SDK_INT >= 29) WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            else WifiManager.WIFI_MODE_FULL_HIGH_PERF
            wifiLock = wm.createWifiLock(mode, "jpinger:wifi").apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
        }
    }

    private fun releaseLocks() {
        try { wakeLock?.takeIf { it.isHeld }?.release() } catch (e: Exception) {}
        try { wifiLock?.takeIf { it.isHeld }?.release() } catch (e: Exception) {}
        wakeLock = null
        wifiLock = null
    }

    private fun watchNetwork() {
        PingEngine.setNetwork(NetUtil.current(this))
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                PingEngine.setNetwork(NetUtil.name(caps))
            }

            override fun onLost(network: Network) {
                PingEngine.setNetwork(NetUtil.current(this@PingService))
            }
        }
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                cm.registerDefaultNetworkCallback(cb)
            } else {
                val req = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                cm.registerNetworkCallback(req, cb)
            }
            netCallback = cb
        } catch (e: Exception) {
        }
    }

    private fun unwatchNetwork() {
        val cb = netCallback ?: return
        try {
            (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).unregisterNetworkCallback(cb)
        } catch (e: Exception) {
        }
        netCallback = null
    }
}
