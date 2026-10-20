package com.bluepinger.app

import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

class PingEngine {
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        if (job?.isActive == true) return
        PingState.setRunning(true)

        job = scope.launch {
            while (isActive) {
                val currentStatus = PingState.status.value
                val host = currentStatus.host
                val interval = currentStatus.intervalMs

                val latency = performPing(host, 3000)
                if (latency != null) {
                    PingState.recordPingResult(latency, "Reply from $host: time=${latency}ms")
                } else {
                    PingState.recordPingResult(null, "Request timed out for $host")
                }

                delay(interval)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        PingState.setRunning(false)
    }

    private fun performPing(host: String, timeoutMs: Int): Long? {
        return try {
            var time = 0L
            val reachable = try {
                var isReachable = false
                time = measureTimeMillis {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, 53), timeoutMs)
                        isReachable = true
                    }
                }
                isReachable
            } catch (e: Exception) {
                var isIcmpReachable = false
                time = measureTimeMillis {
                    isIcmpReachable = InetAddress.getByName(host).isReachable(timeoutMs)
                }
                isIcmpReachable
            }
            if (reachable) time else null
        } catch (e: Exception) {
            null
        }
    }
}