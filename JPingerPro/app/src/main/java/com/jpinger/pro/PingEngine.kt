package com.jpinger.pro

import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class Target(val label: String, val host: String, val port: Int)

enum class Quality { UNKNOWN, EXCELLENT, GOOD, FAIR, POOR }

data class LogEntry(val time: String, val msg: String, val ok: Boolean)

data class Snapshot(
    val running: Boolean = false,
    val last: Long = -1,          // last RTT in ms, -1 = timeout / none yet
    val sent: Int = 0,
    val ok: Int = 0,
    val lost: Int = 0,
    val min: Long = 0,
    val avg: Long = 0,
    val max: Long = 0,
    val jitter: Long = 0,         // over the last WINDOW pings
    val recentLossPct: Int = 0,   // over the last WINDOW pings
    val quality: Quality = Quality.UNKNOWN,
    val network: String = "—"
)

/**
 * Pings a host:port by timing a TCP connect. Lives outside the Activity so the
 * foreground service can keep it running while the app is in the background.
 */
object PingEngine {

    interface Listener {
        fun onSnapshot(s: Snapshot)
        fun onLog(e: LogEntry)
    }

    private const val TIMEOUT_MS = 2000
    private const val WINDOW = 20
    private const val MAX_LOGS = 50

    private val main = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<Listener>()
    private val lock = Any()

    private val logs = ArrayDeque<LogEntry>()      // newest first
    private val window = ArrayDeque<Long>()        // last WINDOW results, -1 = lost
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    private var executor: ScheduledExecutorService? = null
    private var target: Target? = null
    private var running = false
    private var cachedAddr: InetAddress? = null
    private var consecutiveLoss = 0

    private var sent = 0
    private var okCount = 0
    private var lost = 0
    private var sumOk = 0L
    private var minMs = 0L
    private var maxMs = 0L
    private var lastMs = -1L
    private var quality = Quality.UNKNOWN
    private var network = "—"

    // ---------- public API ----------

    fun addListener(l: Listener) {
        listeners.addIfAbsent(l)
    }

    fun removeListener(l: Listener) {
        listeners.remove(l)
    }

    fun snapshot(): Snapshot = synchronized(lock) { buildSnapshot() }

    /** Oldest first. */
    fun logHistory(): List<LogEntry> = synchronized(lock) { logs.toList().asReversed() }

    fun start(t: Target, intervalMs: Long) {
        synchronized(lock) {
            if (running) return
            running = true
            target = t
            cachedAddr = null
            consecutiveLoss = 0
            window.clear()
            quality = Quality.UNKNOWN
            addLog("J Pinger ආරම්භ විය → ${t.host}:${t.port}", true)
            val ex = Executors.newSingleThreadScheduledExecutor()
            executor = ex
            // fixed *delay*: next ping starts only after the previous one finished
            ex.scheduleWithFixedDelay({ safePing() }, 0, intervalMs, TimeUnit.MILLISECONDS)
        }
        publish()
    }

    fun stop() {
        var ex: ScheduledExecutorService? = null
        synchronized(lock) {
            if (!running) return
            running = false
            ex = executor
            executor = null
            addLog("J Pinger නතර විය.", true)
        }
        ex?.shutdownNow()
        publish()
    }

    fun resetStats() {
        synchronized(lock) {
            sent = 0; okCount = 0; lost = 0; sumOk = 0; minMs = 0; maxMs = 0; lastMs = -1
            consecutiveLoss = 0
            window.clear()
            logs.clear()
            quality = Quality.UNKNOWN
        }
        publish()
    }

    /** Called by the service when the default network changes (Wi-Fi <-> mobile data ...). */
    fun setNetwork(name: String) {
        synchronized(lock) {
            if (name == network) return
            val old = network
            network = name
            cachedAddr = null          // DNS answer may differ on the new network
            if (running) {
                window.clear()         // old samples say nothing about the new network
                quality = Quality.UNKNOWN
                addLog("ජාලය මාරු විය: $old → $name", name != "Offline")
            }
        }
        publish()
    }

    // ---------- internals ----------

    private fun safePing() {
        val t = synchronized(lock) {
            if (!running) return
            target
        } ?: return
        try {
            pingOnce(t)
        } catch (e: Throwable) {
            synchronized(lock) { if (running) addLog("Error: ${e.message}", false) }
            publish()
        }
    }

    private fun pingOnce(t: Target) {
        var addr = synchronized(lock) { cachedAddr }
        if (addr == null) {
            addr = try {
                InetAddress.getByName(t.host)
            } catch (e: UnknownHostException) {
                null
            }
            if (addr != null) synchronized(lock) { cachedAddr = addr }
        }
        val a: InetAddress = addr ?: run {
            record(-1, "DNS resolve failed [ERROR]")
            return
        }

        val start = System.nanoTime()
        val success = try {
            Socket().use { it.connect(InetSocketAddress(a, t.port), TIMEOUT_MS) }
            true
        } catch (e: IOException) {
            false
        }
        val ms = (System.nanoTime() - start) / 1_000_000
        record(if (success) ms else -1, null)
    }

    private fun record(ms: Long, note: String?) {
        synchronized(lock) {
            if (!running) return
            sent++
            lastMs = ms
            if (ms >= 0) {
                okCount++
                sumOk += ms
                if (okCount == 1 || ms < minMs) minMs = ms
                if (ms > maxMs) maxMs = ms
                consecutiveLoss = 0
                addLog("Reply received: time=${ms}ms [OK]", true)
            } else {
                lost++
                consecutiveLoss++
                addLog(note ?: "Request Timed Out [ERROR]", false)
                if (consecutiveLoss >= 3) cachedAddr = null   // force a fresh DNS lookup
            }
            window.addLast(ms)
            while (window.size > WINDOW) window.removeFirst()
            updateQuality()
        }
        publish()
    }

    private class WinStats(val lossPct: Int, val avg: Long, val jitter: Long, val samples: Int)

    private fun windowStats(): WinStats {
        val n = window.size
        val oks = window.filter { it >= 0 }
        val lossPct = if (n == 0) 0 else (n - oks.size) * 100 / n
        val avg = if (oks.isEmpty()) 0L else oks.sum() / oks.size
        var jitter = 0L
        if (oks.size >= 2) {
            var total = 0L
            for (i in 1 until oks.size) total += abs(oks[i] - oks[i - 1])
            jitter = total / (oks.size - 1)
        }
        return WinStats(lossPct, avg, jitter, n)
    }

    private fun computeQuality(): Quality {
        val w = windowStats()
        if (w.samples < 5) return Quality.UNKNOWN
        val allLost = window.none { it >= 0 }
        return when {
            allLost || w.lossPct >= 10 || w.jitter > 80 || w.avg > 400 -> Quality.POOR
            w.lossPct >= 3 || w.jitter > 40 || w.avg > 200 -> Quality.FAIR
            w.jitter > 15 || w.avg > 100 -> Quality.GOOD
            else -> Quality.EXCELLENT
        }
    }

    /** Must be called with [lock] held. */
    private fun updateQuality() {
        val old = quality
        val q = computeQuality()
        quality = q
        if (q == Quality.POOR && old != Quality.POOR) {
            addLog("⚠ සම්බන්ධතාව දුර්වලයි (loss/jitter වැඩියි) — Wi-Fi/Data මාරු කර බලන්න", false)
        } else if (old == Quality.POOR && (q == Quality.GOOD || q == Quality.EXCELLENT)) {
            addLog("✔ සම්බන්ධතාව යථා තත්ත්වයට පැමිණියා", true)
        }
    }

    /** Must be called with [lock] held. */
    private fun addLog(msg: String, ok: Boolean) {
        val e = LogEntry(timeFmt.format(Date()), msg, ok)
        logs.addFirst(e)
        while (logs.size > MAX_LOGS) logs.removeLast()
        main.post { for (l in listeners) l.onLog(e) }
    }

    /** Must be called with [lock] held. */
    private fun buildSnapshot(): Snapshot {
        val w = windowStats()
        return Snapshot(
            running = running,
            last = lastMs,
            sent = sent,
            ok = okCount,
            lost = lost,
            min = minMs,
            avg = if (okCount == 0) 0 else sumOk / okCount,
            max = maxMs,
            jitter = w.jitter,
            recentLossPct = w.lossPct,
            quality = quality,
            network = network
        )
    }

    private fun publish() {
        val s = snapshot()
        main.post { for (l in listeners) l.onSnapshot(s) }
    }
}
