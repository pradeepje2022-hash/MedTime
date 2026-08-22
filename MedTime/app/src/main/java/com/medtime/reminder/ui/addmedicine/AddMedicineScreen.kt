package com.medtime.reminder.ui.addmedicine

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.medtime.reminder.data.Medicine
import com.medtime.reminder.data.MedicineType
import com.medtime.reminder.data.ReminderTime
import com.medtime.reminder.util.DateTimeUtils
import com.medtime.reminder.viewmodel.MedicineViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMedicineScreen(
    navController: NavController,
    viewModel: MedicineViewModel,
    medicineId: Long
) {
    val context = LocalContext.current
    val isEditing = medicineId != -1L

    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MedicineType.TABLET) }
    var notes by remember { mutableStateOf("") }
    var daysMask by remember { mutableIntStateOf(DateTimeUtils.ALL_DAYS_MASK) }
    var everyDay by remember { mutableStateOf(true) }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var times by remember { mutableStateOf(listOf<ReminderTime>()) }
    var loaded by remember { mutableStateOf(!isEditing) }

    if (isEditing) {
        val medicine by viewModel.getMedicine(medicineId).collectAsState(initial = null)
        val existingTimes by viewModel.getRemindersForMedicine(medicineId).collectAsState(initial = emptyList())
        LaunchedEffect(medicine, existingTimes) {
            if (medicine != null && !loaded) {
                name = medicine!!.name
                dosage = medicine!!.dosage
                type = medicine!!.type
                notes = medicine!!.notes
                daysMask = medicine!!.daysMask
                everyDay = daysMask == DateTimeUtils.ALL_DAYS_MASK
                startDate = medicine!!.startDate
                endDate = medicine!!.endDate
                if (existingTimes.isNotEmpty()) {
                    times = existingTimes
                    loaded = true
                }
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Medicine" else "Add Medicine") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Medicine name") }, modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = dosage, onValueChange = { dosage = it },
                    label = { Text("Dosage (e.g. 1 tablet, 5 ml)") }, modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text("Medicine type", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MedicineType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.name.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                        )
                    }
                }
            }
            item {
                Text("Reminder times", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                times.forEachIndexed { index, rt ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {
                                TimePickerDialog(context, { _, h, m ->
                                    times = times.toMutableList().also {
                                        it[index] = it[index].copy(hour = h, minute = m)
                                    }
                                }, rt.hour, rt.minute, false).show()
                            },
                            label = { Text(DateTimeUtils.formatTime(rt.hour, rt.minute, false)) }
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = rt.label,
                            onValueChange = { newLabel ->
                                times = times.toMutableList().also { it[index] = it[index].copy(label = newLabel) }
                            },
                            label = { Text("Label (e.g. Morning)") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            times = times.toMutableList().also { it.removeAt(index) }
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove")
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(context, { _, h, m ->
                            times = times + ReminderTime(medicineId = medicineId, hour = h, minute = m, label = "")
                        }, 8, 0, false).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add reminder time")
                }
            }
            item {
                Text("Repeat on", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = everyDay, onCheckedChange = {
                        everyDay = it
                        if (it) daysMask = DateTimeUtils.ALL_DAYS_MASK
                    })
                    Text("Every day")
                }
                if (!everyDay) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateTimeUtils.dayLabels.forEachIndexed { bit, label ->
                            val selected = (daysMask shr bit) and 1 == 1
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    daysMask = if (selected) daysMask and (1 shl bit).inv() else daysMask or (1 shl bit)
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
            item {
                Text("Start & end date (optional)", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val cal = Calendar.getInstance()
                        startDate?.let { cal.timeInMillis = it }
                        DatePickerDialog(context, { _, y, m, d ->
                            val c = Calendar.getInstance()
                            c.set(y, m, d, 0, 0, 0)
                            startDate = c.timeInMillis
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }) {
                        Text(startDate?.let { dateFormat.format(Date(it)) } ?: "Set start date")
                    }
                    OutlinedButton(onClick = {
                        val cal = Calendar.getInstance()
                        endDate?.let { cal.timeInMillis = it }
                        DatePickerDialog(context, { _, y, m, d ->
                            val c = Calendar.getInstance()
                            c.set(y, m, d, 23, 59, 0)
                            endDate = c.timeInMillis
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }) {
                        Text(endDate?.let { dateFormat.format(Date(it)) } ?: "Set end date")
                    }
                }
                if (startDate != null || endDate != null) {
                    TextButton(onClick = { startDate = null; endDate = null }) { Text("Clear dates") }
                }
            }
            item {
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2
                )
            }
            item {
                Button(
                    onClick = {
                        val medicine = Medicine(
                            id = if (isEditing) medicineId else 0L,
                            name = name.trim(),
                            dosage = dosage.trim(),
                            type = type,
                            daysMask = daysMask,
                            startDate = startDate,
                            endDate = endDate,
                            notes = notes.trim()
                        )
                        viewModel.saveMedicine(medicine, times) {
                            navController.popBackStack()
                        }
                    },
                    enabled = name.isNotBlank() && dosage.isNotBlank() && times.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isEditing) "Save Changes" else "Save Medicine")
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
