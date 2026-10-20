package com.bluepinger.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermission()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    BluePingerScreen(
                        onToggleService = { start -> togglePingService(start) }
                    )
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun togglePingService(start: Boolean) {
        val intent = Intent(this, PingService::class.java).apply {
            action = if (start) PingService.ACTION_START else PingService.ACTION_STOP
        }
        if (start) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            startService(intent)
        }
    }
}

@Composable
fun BluePingerScreen(onToggleService: (Boolean) -> Unit) {
    val status by PingState.status.collectAsState()
    var hostInput by remember { mutableStateOf(status.host) }
    var intervalInput by remember { mutableStateOf(status.intervalMs.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "BluePinger",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8)
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = hostInput,
            onValueChange = { hostInput = it },
            label = { Text("Target Host / IP") },
            enabled = !status.isRunning,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = intervalInput,
            onValueChange = { intervalInput = it },
            label = { Text("Interval (ms)") },
            enabled = !status.isRunning,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (!status.isRunning) {
                    val interval = intervalInput.toLongOrNull() ?: 1000L
                    PingState.updateConfig(hostInput, interval)
                    onToggleService(true)
                } else {
                    onToggleService(false)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (status.isRunning) Color(0xFFEF4444) else Color(0xFF0284C7)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = if (status.isRunning) "STOP PINGER" else "START PINGER",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Latency: ${status.lastLatencyMs?.let { "${it}ms" } ?: "--"}", color = Color.White)
            Text("Sent: ${status.packetsSent} | Recv: ${status.packetsReceived}", color = Color.LightGray)
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            items(status.logs.reversed()) { log ->
                Text(
                    text = log,
                    color = if (log.contains("timed out")) Color(0xFFF87171) else Color(0xFF4ADE80),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}