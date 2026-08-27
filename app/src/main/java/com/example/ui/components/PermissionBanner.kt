package com.example.ui.components

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MedWarningAmber
import com.example.ui.theme.MedWarningContainer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/** Returns true if the Xiaomi autostart settings activity is resolvable on this device. */
private fun isXiaomiAutostartAvailable(context: Context): Boolean {
    val intent = Intent().apply {
        setClassName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )
    }
    return context.packageManager.resolveActivity(intent, 0) != null
}

private fun openAutostartSettings(context: Context) {
    val candidates = listOf(
        // Xiaomi / MIUI
        Intent().apply {
            setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        },
        // Samsung
        Intent().apply {
            setClassName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.ui.battery.BatteryActivity"
            )
        },
        // Oppo / ColorOS
        Intent().apply {
            setClassName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        },
        // Huawei
        Intent().apply {
            setClassName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        },
        // Fallback: app detail settings
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    )
    candidates.firstOrNull { context.packageManager.resolveActivity(it, 0) != null }
        ?.let { context.startActivity(it) }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    val needsNotificationPermission = notificationPermissionState != null &&
            !notificationPermissionState.status.isGranted

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    val canExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager?.canScheduleExactAlarms() ?: true
    } else {
        true
    }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isIgnoringBatteryOptimizations =
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true

    // Show autostart button only on devices where the setting is accessible
    val hasAutostartSetting = remember { isXiaomiAutostartAvailable(context) }

    val showBanner = needsNotificationPermission ||
            !canExactAlarm ||
            !isIgnoringBatteryOptimizations ||
            hasAutostartSetting

    if (showBanner) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("permission_banner_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MedWarningContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MedWarningAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Permissions Required for Timely Alarms",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "MedTime needs notification, exact alarm, and battery permissions so your medicine reminders ring reliably on time, even when the app is closed or the screen is locked.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Row 1: Notification + Exact Alarm
                Row {
                    if (needsNotificationPermission) {
                        Button(
                            onClick = { notificationPermissionState?.launchPermissionRequest() },
                            modifier = Modifier.testTag("grant_notification_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MedWarningAmber),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Allow Notifications", fontSize = 12.sp)
                        }
                    }

                    if (!canExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.testTag("grant_exact_alarm_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Exact Alarms", fontSize = 12.sp)
                        }
                    }

                    if (!isIgnoringBatteryOptimizations) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                    ).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    )
                                }
                            },
                            modifier = Modifier.testTag("grant_battery_optimization_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Allow Background", fontSize = 12.sp)
                        }
                    }
                }

                // Row 2: Autostart (Xiaomi/OEM specific)
                if (hasAutostartSetting) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { openAutostartSettings(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grant_autostart_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MedWarningAmber
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Enable Background Autostart →", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
