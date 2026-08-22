package com.medtime.reminder

import android.Manifest
import android.app.AlarmManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var store: Store
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        requestNotification()
        setContent { MedTimeApp() }
        AlarmScheduler.scheduleAll(this)
    }

    private fun requestNotification() {
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 50)
        if (Build.VERSION.SDK_INT >= 31) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
    }

    @Composable
    fun MedTimeApp() {
        var tab by remember { mutableIntStateOf(0) }
        var meds by remember { mutableStateOf(store.medicines()) }
        var showAdd by remember { mutableStateOf(false) }
        var selected by remember { mutableStateOf<Medicine?>(null) }
        var settings by remember { mutableStateOf(store.settings()) }

        if (showAdd) {
            AddMedicineScreen(onSave = { m ->
                meds = (meds + m).toMutableList(); store.saveMedicines(meds); AlarmScheduler.scheduleAll(this); showAdd = false
            }, onCancel = { showAdd = false })
            return
        }

        if (selected != null) {
            MedicineDetailsScreen(selected!!, onDelete = {
                meds = meds.filterNot { it.id == selected!!.id }.toMutableList()
                store.saveMedicines(meds); AlarmScheduler.scheduleAll(this); selected = null
            }, onClose = { selected = null })
            return
        }

        MaterialTheme {
            Scaffold(
                floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Text("+", fontSize=28.sp) } },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(tab==0, { tab=0 }, { Text("Home") })
                        NavigationBarItem(tab==1, { tab=1 }, { Text("History") })
                        NavigationBarItem(tab==2, { tab=2 }, { Text("Settings") })
                    }
                }
            ) { pad ->
                when(tab) {
                    0 -> HomeScreen(meds) { selected = it }
                    1 -> HistoryScreen(store.history())
                    2 -> SettingsScreen(settings) { s -> settings=s; store.saveSettings(s) }
                }
            }
        }
    }

    @Composable
    fun HomeScreen(meds: List<Medicine>, open: (Medicine)->Unit) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("MedTime", fontSize=32.sp, fontWeight=FontWeight.Bold)
            Text("Today's Medicines", fontSize=20.sp)
            Spacer(Modifier.height(16.dp))
            if (meds.isEmpty()) Text("No medicines added yet. Tap + to add one.")
            LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                items(meds.filter { it.enabled }) { m ->
                    Card(onClick={open(m)}, Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("💊 ${m.name}", fontSize=20.sp, fontWeight=FontWeight.Bold)
                            Text("${m.dosage} • ${m.type}")
                            Text(m.times.joinToString("  •  "))
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Reminder only: follow your doctor's or pharmacist's instructions. Do not change medicine or dosage without professional advice.",
                fontSize=12.sp)
        }
    }

    @Composable
    fun AddMedicineScreen(onSave:(Medicine)->Unit,onCancel:()->Unit) {
        var name by remember { mutableStateOf("") }
        var dose by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Tablet") }
        var time by remember { mutableStateOf("08:00") }
        var times by remember { mutableStateOf(listOf<String>()) }
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Add Medicine", fontSize=28.sp, fontWeight=FontWeight.Bold)
            OutlinedTextField(name,{name=it},label={Text("Medicine name")},Modifier.fillMaxWidth())
            OutlinedTextField(dose,{dose=it},label={Text("Dosage (e.g. 1 tablet, 5 ml)")},Modifier.fillMaxWidth())
            OutlinedTextField(type,{type=it},label={Text("Type")},Modifier.fillMaxWidth())
            OutlinedTextField(time,{time=it},label={Text("Reminder time (HH:mm)")},Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(onClick={ if(time.matches(Regex("\\d{1,2}:\\d{2}"))) times=times+time }) { Text("Add Reminder Time") }
            Text("Times: ${times.joinToString(", ")}")
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Button(enabled=name.isNotBlank()&&dose.isNotBlank()&&times.isNotEmpty(),
                    onClick={onSave(Medicine(name=name,dosage=dose,type=type,times=times))}) { Text("Save") }
                OutlinedButton(onClick=onCancel){Text("Cancel")}
            }
        }
    }

    @Composable
    fun MedicineDetailsScreen(m:Medicine,onDelete:()->Unit,onClose:()->Unit) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Medicine Details",fontSize=28.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text(m.name,fontSize=24.sp,fontWeight=FontWeight.Bold)
            Text("Dosage: ${m.dosage}")
            Text("Type: ${m.type}")
            Text("Reminder times: ${m.times.joinToString(", ")}")
            Spacer(Modifier.height(20.dp))
            Button(onClick=onDelete){Text("Delete Medicine")}
            OutlinedButton(onClick=onClose){Text("Close")}
        }
    }

    @Composable
    fun HistoryScreen(items: List<HistoryItem>) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Medicine History",fontSize=28.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if(items.isEmpty()) Text("No history yet.")
            LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                items(items.asReversed()) { h ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                        Text(h.medicineName,fontWeight=FontWeight.Bold)
                        Text("${h.date} • ${h.time} • ${h.status}")
                    }}
                }
            }
        }
    }

    @Composable
    fun SettingsScreen(s:AppSettings,onChange:(AppSettings)->Unit) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Settings",fontSize=28.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Default snooze: ${s.snoozeMinutes} minutes")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(5,10,15,30).forEach { n ->
                    OutlinedButton(onClick={onChange(s.copy(snoozeMinutes=n))}){Text("$n")}
                }
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("Dark mode",Modifier.weight(1f))
                Switch(s.darkMode,{onChange(s.copy(darkMode=it))})
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("24-hour time",Modifier.weight(1f))
                Switch(s.use24Hour,{onChange(s.copy(use24Hour=it))})
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("Notification sound",Modifier.weight(1f))
                Switch(s.soundEnabled,{onChange(s.copy(soundEnabled=it))})
            }
            Spacer(Modifier.height(16.dp))
            Text("For reliable reminders, allow notifications and exact alarms, and exclude MedTime from battery optimization when your phone provides that option.",
                fontSize=13.sp)
        }
    }
}
