package com.medtime.reminder

import kotlinx.serialization.Serializable

@Serializable
data class Medicine(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val dosage: String,
    val type: String = "Tablet",
    val times: List<String>,
    val days: List<Int> = listOf(1,2,3,4,5,6,7),
    val startDate: String = "",
    val endDate: String = "",
    val notes: String = "",
    val enabled: Boolean = true
)

@Serializable
data class HistoryItem(
    val medicineId: Long,
    val medicineName: String,
    val dosage: String,
    val date: String,
    val time: String,
    val status: String
)

@Serializable
data class AppSettings(
    val snoozeMinutes: Int = 10,
    val darkMode: Boolean = false,
    val use24Hour: Boolean = false,
    val soundEnabled: Boolean = true
)
