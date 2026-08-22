package com.medtime.reminder

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*

object AlarmScheduler {
    private const val ACTION = "com.medtime.reminder.REMINDER"

    fun scheduleAll(context: Context) {
        Store(context).medicines().filter { it.enabled }.forEach { medicine ->
            medicine.times.forEach { time -> scheduleNext(context, medicine, time) }
        }
    }

    fun scheduleNext(context: Context, medicine: Medicine, time: String) {
        val parts = time.replace(" ", "").split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) target.add(Calendar.DAY_OF_YEAR, 1)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION
            putExtra("medicineId", medicine.id)
            putExtra("name", medicine.name)
            putExtra("dosage", medicine.dosage)
            putExtra("time", time)
        }
        val requestCode = (medicine.id.toInt() xor time.hashCode())
        val pi = PendingIntent.getBroadcast(context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
        else am.setExact(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        Store(context).medicines().forEach { med ->
            med.times.forEach { time ->
                val i = Intent(context, ReminderReceiver::class.java).apply { action = ACTION }
                val pi = PendingIntent.getBroadcast(context, med.id.toInt() xor time.hashCode(), i,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                am.cancel(pi)
            }
        }
    }
}
