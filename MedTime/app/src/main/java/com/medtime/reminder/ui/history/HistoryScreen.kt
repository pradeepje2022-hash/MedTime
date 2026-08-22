package com.medtime.reminder.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medtime.reminder.data.DoseStatus
import com.medtime.reminder.data.HistoryEntry
import com.medtime.reminder.util.DateTimeUtils
import com.medtime.reminder.viewmodel.MedicineViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MedicineViewModel) {
    val today = remember { LocalDate.now() }
    val fromDay = remember { today.minusDays(30).toEpochDay() }
    val toDay = remember { today.toEpochDay() }
    val entries by viewModel.getHistoryForRange(fromDay, toDay).collectAsState(initial = emptyList())
    val use24Hour by viewModel.prefs.use24Hour.collectAsState(initial = false)

    val grouped = remember(entries) { entries.groupBy { it.dateEpochDay }.toSortedMap(compareByDescending { it }) }
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMM yyyy") }

    Scaffold(topBar = { TopAppBar(title = { Text("History") }) }) { padding ->
        if (grouped.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No history yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (epochDay, dayEntries) ->
                    item {
                        val label = if (epochDay == today.toEpochDay()) "Today"
                        else LocalDate.ofEpochDay(epochDay).format(formatter)
                        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(dayEntries.sortedBy { it.scheduledHour * 60 + it.scheduledMinute }) { entry ->
                        HistoryRow(entry, use24Hour)
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, use24Hour: Boolean) {
    val (label, color) = when (entry.status) {
        DoseStatus.TAKEN -> "Taken" to MaterialTheme.colorScheme.tertiary
        DoseStatus.MISSED -> "Missed" to MaterialTheme.colorScheme.error
        DoseStatus.SKIPPED -> "Skipped" to MaterialTheme.colorScheme.outline
        DoseStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.primary
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.medicineName, fontWeight = FontWeight.Bold)
                Text(entry.dosage, style = MaterialTheme.typography.bodySmall)
                Text(
                    DateTimeUtils.formatTime(entry.scheduledHour, entry.scheduledMinute, use24Hour),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(label, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
