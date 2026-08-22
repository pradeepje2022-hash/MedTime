package com.medtime.reminder.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.medtime.reminder.data.Medicine
import com.medtime.reminder.ui.navigation.Screen
import com.medtime.reminder.util.DateTimeUtils
import com.medtime.reminder.viewmodel.MedicineViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineDetailsScreen(navController: NavController, viewModel: MedicineViewModel, medicineId: Long) {
    val medicine by viewModel.getMedicine(medicineId).collectAsState(initial = null)
    val times by viewModel.getRemindersForMedicine(medicineId).collectAsState(initial = emptyList())
    val use24Hour by viewModel.prefs.use24Hour.collectAsState(initial = false)
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val med = medicine
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medicine Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (med != null) {
                        IconButton(onClick = { navController.navigate(Screen.AddMedicine.create(medicineId)) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (med == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
            ) {
                Text(med.name, style = MaterialTheme.typography.headlineSmall)
                Text(med.dosage, style = MaterialTheme.typography.bodyLarge)
                Text(med.type.name.lowercase().replaceFirstChar { it.uppercase() })
                Spacer(Modifier.height(16.dp))

                Text("Reminder times", style = MaterialTheme.typography.labelLarge)
                times.forEach { t ->
                    Text("• ${DateTimeUtils.formatTime(t.hour, t.minute, use24Hour)}${if (t.label.isNotBlank()) " — ${t.label}" else ""}")
                }

                Spacer(Modifier.height(16.dp))
                Text("Repeats", style = MaterialTheme.typography.labelLarge)
                Text(
                    if (med.daysMask == DateTimeUtils.ALL_DAYS_MASK) "Every day"
                    else DateTimeUtils.dayLabels.filterIndexed { i, _ -> (med.daysMask shr i) and 1 == 1 }.joinToString(", ")
                )

                if (med.startDate != null || med.endDate != null) {
                    Spacer(Modifier.height(16.dp))
                    Text("Duration", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${med.startDate?.let { dateFormat.format(Date(it)) } ?: "No start"} – " +
                            (med.endDate?.let { dateFormat.format(Date(it)) } ?: "No end")
                    )
                }

                if (med.notes.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Notes", style = MaterialTheme.typography.labelLarge)
                    Text(med.notes)
                }

                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(if (med.active) "Reminders active" else "Reminders paused", modifier = Modifier.weight(1f))
                    Switch(checked = med.active, onCheckedChange = { viewModel.setActive(med, it) })
                }
            }
        }
    }

    if (showDeleteConfirm && med != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete medicine?") },
            text = { Text("This will remove ${med.name} and all its reminders. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMedicine(med)
                    showDeleteConfirm = false
                    navController.popBackStack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
