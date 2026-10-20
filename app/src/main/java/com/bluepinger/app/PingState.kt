package com.bluepinger.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PingStatus(
    val isRunning: Boolean = false,
    val host: String = "8.8.8.8",
    val intervalMs: Long = 1000L,
    val lastLatencyMs: Long? = null,
    val packetsSent: Int = 0,
    val packetsReceived: Int = 0,
    val logs: List<String> = emptyList()
)

object PingState {
    private val _status = MutableStateFlow(PingStatus())
    val status: StateFlow<PingStatus> = _status.asStateFlow()

    fun updateConfig(host: String, intervalMs: Long) {
        _status.value = _status.value.copy(host = host.trim(), intervalMs = intervalMs.coerceAtLeast(200L))
    }

    fun setRunning(running: Boolean) {
        if (!running) {
            _status.value = _status.value.copy(isRunning = false, lastLatencyMs = null)
        } else {
            _status.value = _status.value.copy(
                isRunning = true,
                packetsSent = 0,
                packetsReceived = 0,
                logs = listOf("Started pinging ${_status.value.host}...")
            )
        }
    }

    fun recordPingResult(latencyMs: Long?, message: String) {
        val current = _status.value
        val newSent = current.packetsSent + 1
        val newReceived = if (latencyMs != null) current.packetsReceived + 1 else current.packetsReceived
        val updatedLogs = (current.logs + message).takeLast(50)

        _status.value = current.copy(
            lastLatencyMs = latencyMs,
            packetsSent = newSent,
            packetsReceived = newReceived,
            logs = updatedLogs
        )
    }
}