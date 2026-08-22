package com.medtime.reminder.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medtime.reminder.util.ThemeMode
import com.medtime.reminder.viewmodel.MedicineViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MedicineViewModel) {
    val scope = rememberCoroutineScope()
    val themeMode by viewModel.prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val snoozeMinutes by viewModel.prefs.defaultSnoozeMinutes.collectAsState(initial = 10)
    val use24Hour by viewModel.prefs.use24Hour.collectAsState(initial = false)
    val soundOn by viewModel.prefs.notificationSoundOn.collectAsState(initial = true)

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            Text("Appearance", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { scope.launch { viewModel.prefs.setThemeMode(mode) } },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Default snooze duration", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 30).forEach { minutes ->
                    FilterChip(
                        selected = snoozeMinutes == minutes,
                        onClick = { scope.launch { viewModel.prefs.setDefaultSnoozeMinutes(minutes) } },
                        label = { Text("$minutes min") }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Use 24-hour time format", modifier = Modifier.weight(1f))
                Switch(checked = use24Hour, onCheckedChange = { scope.launch { viewModel.prefs.setUse24Hour(it) } })
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Notification sound", modifier = Modifier.weight(1f))
                Switch(checked = soundOn, onCheckedChange = { scope.launch { viewModel.prefs.setNotificationSoundOn(it) } })
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Tip: For alarm volume, use your phone's Alarm volume slider in system settings, " +
                    "since the reminder sound follows that channel.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(
                "This app is only a reminder tool. Always follow your doctor's or pharmacist's " +
                    "instructions. Do not change your medicine or dosage without professional advice.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
