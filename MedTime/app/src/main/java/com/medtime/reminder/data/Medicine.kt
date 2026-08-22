package com.medtime.reminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MedicineType { TABLET, CAPSULE, SYRUP, OTHER }

/**
 * daysMask: bitmask for weekdays, bit 0 = Sunday ... bit 6 = Saturday.
 * If daysMask == 127 (all bits set) it means "Every day".
 */
@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String,
    val type: MedicineType,
    val daysMask: Int = 127,
    val startDate: Long? = null, // epoch millis, midnight
    val endDate: Long? = null,
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminder_times")
data class ReminderTime(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val hour: Int,
    val minute: Int,
    val label: String = "" // e.g. Morning / Afternoon / Evening / Night
)

enum class DoseStatus { PENDING, TAKEN, MISSED, SKIPPED }

@Entity(tableName = "history_entries")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val reminderTimeId: Long,
    val medicineName: String,
    val dosage: String,
    val dateEpochDay: Long, // LocalDate.toEpochDay()
    val scheduledHour: Int,
    val scheduledMinute: Int,
    var status: DoseStatus = DoseStatus.PENDING,
    var actionTimestamp: Long? = null
)
