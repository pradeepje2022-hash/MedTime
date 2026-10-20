package com.bluepinger.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF06122B)
private val Card = Color(0xFF0D1F44)
private val CardLight = Color(0xFF14295A)
private val Blue = Color(0xFF2F80FF)
private val Cyan = Color(0xFF38BDF8)
private val SoftBlue = Color(0xFF9CC2FF)
private val Good = Color(0xFF34D399)
private val Warn = Color(0xFFFBBF24)
private val Bad = Color(0xFFF87171)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Blue,
                    onPrimary = Color.White,
                    background = Navy,
                    surface = Card,
                    onSurface = Color(0xFFE6EEFF),
                    onBackground = Color(0xFFE6EEFF),
                    secondary = Cyan
                )
            ) {
                PingScreen()
            }
        }
    }
}

private fun quality(rtt: Double?): Pair<String, Color> = when {
    rtt == null -> "No reply" to Bad
    rtt < 50 -> "Excellent" to Good
    rtt < 100 -> "Good" to Cyan
    rtt < 200 -> "Fair" to Warn
    else -> "Poor" to Bad
}

@Composable
fun PingScreen() {
    val ctx = LocalContext.current
    val stats by PingState.stats.collectAsStateWithLifecycle()
    val running by PingState.running.collectAsStateWithLifecycle()

    var host by remember { mutableStateOf(Prefs.host(ctx)) }
    var interval by remember { mutableStateOf(Prefs.interval(ctx)) }
    var error by remember { mutableStateOf<String?>(null) }

    fun startNow() {
        val h = host.trim()
        if (!PingEngine.isValidHost(h)) {
            error = "Enter a valid host or IP"
            return
        }
        error = null
        Prefs.save(ctx, h, interval)
        PingService.start(ctx)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { startNow() }

    fun onToggle() {
        if (running) {
            PingService.stop(ctx)
        } else if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startNow()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1B3F), Navy)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (running) Good else SoftBlue.copy(alpha = 0.4f))
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    "Blue Pinger",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (running) "LIVE" else "IDLE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (running) Good else SoftBlue
                )
            }

            // Target card
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Card)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it; error = null },
                    label = { Text("Host / IP") },
                    singleLine = true,
                    enabled = !running,
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = SoftBlue.copy(alpha = 0.4f),
                        focusedLabelColor = Cyan,
                        unfocusedLabelColor = SoftBlue
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("8.8.8.8" to "Google", "1.1.1.1" to "Cloudflare", "google.com" to "Web").forEach { (h, label) ->
                        FilterChip(
                            selected = host == h,
                            enabled = !running,
                            onClick = { host = h; error = null },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = chipColors()
                        )
                    }
                }
                Text("Interval", color = SoftBlue, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(500L to "0.5 s", 1000L to "1 s", 2000L to "2 s", 5000L to "5 s").forEach { (ms, label) ->
                        FilterChip(
                            selected = interval == ms,
                            enabled = !running,
                            onClick = { interval = ms },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = chipColors()
                        )
                    }
                }
            }

            // Gauge
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Card)
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Gauge(stats, running)
            }

            // Stats
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("MIN", fmt(stats.min, stats.received > 0), Modifier.weight(1f))
                StatTile("AVG", fmt(stats.avg, stats.received > 0), Modifier.weight(1f))
                StatTile("MAX", fmt(stats.max, stats.received > 0), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("JITTER", fmt(stats.jitter, stats.received > 1), Modifier.weight(1f))
                StatTile("SENT", stats.sent.toString(), Modifier.weight(1f))
                StatTile(
                    "LOSS",
                    if (stats.sent > 0) "%.0f%%".format(stats.lossPct) else "–",
                    Modifier.weight(1f),
                    valueColor = if (stats.lost > 0) Bad else Color.White
                )
            }

            // Chart
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Card)
                    .padding(16.dp)
            ) {
                Text("Latency (last 60)", color = SoftBlue, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                LatencyChart(stats.samples.takeLast(60))
            }

            Button(
                onClick = { onToggle() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (running) Bad else Blue,
                    contentColor = Color.White
                )
            ) {
                Text(
                    if (running) "STOP" else "START PING",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            TextButton(
                onClick = {
                    try {
                        ctx.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:" + ctx.packageName)
                            )
                        )
                    } catch (_: Exception) {
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Allow background running (battery optimization)", color = SoftBlue, fontSize = 13.sp)
            }

            // Log
            if (stats.samples.isNotEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Card)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Recent replies", color = SoftBlue, fontSize = 12.sp)
                    val fmtTime = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
                    stats.samples.takeLast(12).reversed().forEach { s ->
                        Row(Modifier.fillMaxWidth()) {
                            Text("#${s.seq}", color = SoftBlue, fontSize = 13.sp, modifier = Modifier.weight(0.8f))
                            Text(fmtTime.format(Date(s.time)), color = SoftBlue, fontSize = 13.sp, modifier = Modifier.weight(1.4f))
                            Text(
                                if (s.rtt != null) "%.1f ms".format(s.rtt) else "Timeout",
                                color = if (s.rtt != null) quality(s.rtt).second else Bad,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = CardLight,
    labelColor = SoftBlue,
    selectedContainerColor = Blue,
    selectedLabelColor = Color.White,
    disabledContainerColor = CardLight.copy(alpha = 0.5f),
    disabledLabelColor = SoftBlue.copy(alpha = 0.5f),
    disabledSelectedContainerColor = Blue.copy(alpha = 0.6f),
    disabledSelectedLabelColor = Color.White.copy(alpha = 0.8f)
)

private fun fmt(v: Double, valid: Boolean): String = if (valid) "%.0f ms".format(v) else "–"

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = SoftBlue, fontSize = 11.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Gauge(stats: PingStats, running: Boolean) {
    val rtt = stats.last
    val hasData = stats.sent > 0
    val (label, color) = if (hasData) quality(rtt) else (if (running) "Waiting…" else "Ready") to SoftBlue
    val progress = if (rtt != null) (1.0 - (rtt.coerceAtMost(300.0) / 300.0)).toFloat().coerceIn(0.04f, 1f) else 0f

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = CardLight,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (progress > 0f) {
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = 270f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (rtt != null) "%.0f".format(rtt) else "–",
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text("ms", fontSize = 14.sp, color = SoftBlue)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
private fun LatencyChart(samples: List<Sample>) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val w = size.width
        val h = size.height
        for (i in 0..3) {
            val y = h * i / 3f
            drawLine(CardLight, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
        }
        if (samples.size < 2) return@Canvas

        val maxV = (samples.mapNotNull { it.rtt }.maxOrNull() ?: 100.0).coerceAtLeast(100.0) * 1.15
        val maxPoints = 60
        val step = w / (maxPoints - 1)
        val n = samples.size
        val path = Path()
        var started = false
        var lastPoint: Offset? = null

        samples.forEachIndexed { i, s ->
            val x = w - (n - 1 - i) * step
            if (s.rtt == null) {
                started = false
                drawLine(Bad.copy(alpha = 0.8f), Offset(x, h - 14.dp.toPx()), Offset(x, h), strokeWidth = 2.dp.toPx())
            } else {
                val y = (h - (s.rtt / maxV * h)).toFloat().coerceIn(2f, h - 2f)
                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
                lastPoint = Offset(x, y)
            }
        }
        drawPath(
            path,
            brush = Brush.horizontalGradient(listOf(Blue, Cyan)),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        lastPoint?.let { drawCircle(Cyan, radius = 5.dp.toPx(), center = it) }
    }
}
