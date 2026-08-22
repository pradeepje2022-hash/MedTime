package com.medtime.reminder.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.medtime.reminder.data.DoseStatus
import com.medtime.reminder.ui.navigation.Screen
import com.medtime.reminder.util.DateTimeUtils
import com.medtime.reminder.viewmodel.MedicineViewModel
import com.medtime.reminder.viewmodel.TodayItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: MedicineViewModel) {
    val items by viewModel.todayItems.collectAsState()
    val use24Hour by viewModel.prefs.use24Hour.collectAsState(initial = false)

    Scaffold(
        topBar = { TopAppBar(title = { Text("MedTime", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.AddMedicine.create()) }) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Medication, contentDescription = null, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No medicines scheduled for today.")
                    Text("Tap + to add your first medicine.")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("TODAY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                items(items, key = { it.medicineId.toString() + "_" + it.reminderTimeId }) { item ->
                    TodayCard(
                        item = item,
                        use24Hour = use24Hour,
                        onTaken = { viewModel.markStatus(item, DoseStatus.TAKEN) },
                        onSkip = { viewModel.markStatus(item, DoseStatus.SKIPPED) },
                        onClick = { navController.navigate(Screen.Details.create(item.medicineId)) }
                    )
                }
                item { Spacer(Modifier.height(64.dp)) }
            }
        }
    }
}

@Composable
private fun TodayCard(
    item: TodayItem,
    use24Hour: Boolean,
    onTaken: () -> Unit,
    onSkip: () -> Unit,
    onClick: () -> Unit
) {
    val statusColor = when (item.status) {
        DoseStatus.TAKEN -> MaterialTheme.colorScheme.tertiary
        DoseStatus.MISSED -> MaterialTheme.colorScheme.error
        DoseStatus.SKIPPED -> MaterialTheme.colorScheme.outline
        DoseStatus.PENDING -> MaterialTheme.colorScheme.primary
    }
    val statusLabel = when (item.status) {
        DoseStatus.TAKEN -> "Taken"
        DoseStatus.MISSED -> "Missed"
        DoseStatus.SKIPPED -> "Skipped"
        DoseStatus.PENDING -> "Upcoming"
    }

    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    DateTimeUtils.formatTime(item.hour, item.minute, use24Hour),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Medication, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(item.medicineName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Text(item.dosage, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(statusLabel, color = statusColor, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (item.status == DoseStatus.PENDING) {
                Column(horizontalAlignment = Alignment.End) {
                    Button(onClick = onTaken) { Text("Take Now") }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onSkip) { Text("Skip") }
                }
            }
        }
    }
}
