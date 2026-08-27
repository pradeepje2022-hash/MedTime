package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.MedTimeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED
            )) return

        val pendingResult = goAsync()
        val app = context.applicationContext as? MedTimeApplication
        if (app == null) {
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // goAsync() deadline is 10 seconds — stay well within it.
                withTimeout(8_000L) {
                    val scheduler = AlarmScheduler(context.applicationContext)
                    val activeMeds = app.medicineRepository.getAllActiveMedicinesSync()
                    Log.d("BootReceiver", "Restoring alarms for ${activeMeds.size} active medicines")
                    activeMeds.forEach { scheduler.scheduleMedicine(it) }
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to restore alarms", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
