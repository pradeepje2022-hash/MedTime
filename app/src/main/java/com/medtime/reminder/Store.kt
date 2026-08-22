package com.medtime.reminder

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class Store(private val context: Context) {
    private val prefs = context.getSharedPreferences("medtime", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun medicines(): MutableList<Medicine> = runCatching {
        json.decodeFromString<List<Medicine>>(prefs.getString("medicines","[]")!!)
    }.getOrDefault(emptyList()).toMutableList()

    fun saveMedicines(list: List<Medicine>) {
        prefs.edit().putString("medicines", json.encodeToString(list)).apply()
    }

    fun history(): MutableList<HistoryItem> = runCatching {
        json.decodeFromString<List<HistoryItem>>(prefs.getString("history","[]")!!)
    }.getOrDefault(emptyList()).toMutableList()

    fun saveHistory(list: List<HistoryItem>) {
        prefs.edit().putString("history", json.encodeToString(list)).apply()
    }

    fun settings(): AppSettings = runCatching {
        json.decodeFromString<AppSettings>(prefs.getString("settings", json.encodeToString(AppSettings()))!!)
    }.getOrDefault(AppSettings())

    fun saveSettings(s: AppSettings) {
        prefs.edit().putString("settings", json.encodeToString(s)).apply()
    }
}
