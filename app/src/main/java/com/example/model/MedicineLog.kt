package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DoseStatus {
    UPCOMING,
    TAKEN,
    MISSED,
    SKIPPED,
    SNOOZED
}

@Entity(
    tableName = "medicine_logs",
    indices = [
        Index(value = ["medicineId", "dateString", "scheduledTime"], unique = true)
    ]
)
data class MedicineLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicineId: Long,
    val medicineName: String,
    val dosage: String,
    val medicineType: MedicineType = MedicineType.TABLET,
    val colorHex: Long = 0xFF007A87,
    val dateString: String, // e.g. "2026-08-22"
    val scheduledTime: String, // e.g. "08:00"
    val reminderLabel: String = "Reminder",
    val status: DoseStatus = DoseStatus.UPCOMING,
    val actionTimestamp: Long? = null, // when user took/skipped
    val snoozeUntil: Long? = null,
    val notes: String = ""
) {
    fun getFormattedActionTime(): String {
        if (actionTimestamp == null) return ""
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(actionTimestamp))
    }
}
