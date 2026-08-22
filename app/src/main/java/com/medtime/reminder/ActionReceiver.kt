package com.medtime.reminder

import android.content.*
import java.text.SimpleDateFormat
import java.util.*

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("medicineId", 0L)
        val name = intent.getStringExtra("name") ?: "Medicine"
        val dosage = intent.getStringExtra("dosage") ?: ""
        val time = intent.getStringExtra("time") ?: ""
        val status = intent.action ?: "SKIPPED"
        val store = Store(context)
        val history = store.history()
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (status == "SNOOZE") {
            val snooze = store.settings().snoozeMinutes
            val med = store.medicines().firstOrNull { it.id == id }
            if (med != null) {
                val parts = time.replace(" ", "").split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val cal = Calendar.getInstance().apply {
                    add(Calendar.MINUTE, snooze)
                }
                val newTime = String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                AlarmScheduler.scheduleNext(context, med, newTime)
            }
            return
        }
        history.add(HistoryItem(id, name, dosage, date, time, if (status == "TAKEN") "Taken" else "Skipped"))
        store.saveHistory(history.takeLast(500))
    }
}
