package com.bluepinger.app

import java.net.InetSocketAddress
import java.net.Socket

object PingEngine {
    private val HOST_REGEX = Regex("^[A-Za-z0-9][A-Za-z0-9.:\\-]*$")
    private val TIME_REGEX = Regex("time[=<]\\s*([0-9.]+)\\s*ms")

    fun isValidHost(host: String): Boolean = host.length <= 253 && HOST_REGEX.matches(host)

    /** Returns round-trip time in ms, or null when the request timed out / failed. */
    fun ping(host: String, timeoutSec: Int = 2): Double? {
        if (!isValidHost(host)) return null
        return try {
            icmp(host, timeoutSec)
        } catch (e: Exception) {
            tcp(host, timeoutSec)
        }
    }

    private fun icmp(host: String, timeoutSec: Int): Double? {
        val process = ProcessBuilder("/system/bin/ping", "-c", "1", "-W", timeoutSec.toString(), host)
            .redirectErrorStream(true)
            .start()
        try {
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            return TIME_REGEX.find(output)?.groupValues?.get(1)?.toDoubleOrNull()
        } finally {
            process.destroy()
        }
    }

    // Fallback when the ping binary is not usable: time a TCP handshake instead.
    private fun tcp(host: String, timeoutSec: Int): Double? {
        return try {
            val start = System.nanoTime()
            Socket().use { it.connect(InetSocketAddress(host, 443), timeoutSec * 1000) }
            (System.nanoTime() - start) / 1_000_000.0
        } catch (e: Exception) {
            null
        }
    }
}
