package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.MedTimeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received broadcast action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val app = context.applicationContext as? MedTimeApplication ?: return
            val repo = app.medicineRepository
            val scheduler = AlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val activeMeds = repo.getAllActiveMedicinesSync()
                    Log.d("BootReceiver", "Restoring alarms for ${activeMeds.size} active medicines")
                    for (med in activeMeds) {
                        scheduler.scheduleMedicine(med)
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to restore alarms on reboot", e)
                }
            }
        }
    }
}
