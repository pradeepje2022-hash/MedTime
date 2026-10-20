package com.bluepinger.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs

data class Sample(val seq: Int, val rtt: Double?, val time: Long)

data class PingStats(
    val sent: Int = 0,
    val received: Int = 0,
    val min: Double = 0.0,
    val max: Double = 0.0,
    val sum: Double = 0.0,
    val last: Double? = null,
    val jitter: Double = 0.0,
    val samples: List<Sample> = emptyList()
) {
    val lost: Int get() = sent - received
    val avg: Double get() = if (received > 0) sum / received else 0.0
    val lossPct: Double get() = if (sent > 0) lost * 100.0 / sent else 0.0

    fun add(rtt: Double?, now: Long): PingStats {
        val seq = sent + 1
        val list = (samples + Sample(seq, rtt, now)).takeLast(120)
        if (rtt == null) {
            return copy(sent = seq, last = null, samples = list)
        }
        val prev = last
        val newJitter = if (prev != null) jitter + (abs(rtt - prev) - jitter) / 16.0 else jitter
        return copy(
            sent = seq,
            received = received + 1,
            min = if (received == 0) rtt else minOf(min, rtt),
            max = maxOf(max, rtt),
            sum = sum + rtt,
            last = rtt,
            jitter = newJitter,
            samples = list
        )
    }
}

/** Shared state between the foreground service, the quick tile and the UI. */
object PingState {
    val running = MutableStateFlow(false)
    val stats = MutableStateFlow(PingStats())

    fun reset() {
        stats.value = PingStats()
    }
}

object Prefs {
    private const val FILE = "bluepinger"

    fun host(ctx: Context): String =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("host", "8.8.8.8") ?: "8.8.8.8"

    fun interval(ctx: Context): Long =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getLong("interval", 1000L)

    fun save(ctx: Context, host: String, interval: Long) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("host", host)
            .putLong("interval", interval)
            .apply()
    }
}
