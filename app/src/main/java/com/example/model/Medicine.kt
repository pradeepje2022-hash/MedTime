package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosageAmount: String, // e.g. "1", "5", "500"
    val dosageUnit: String, // e.g. "tablet", "ml", "mg", "capsule"
    val medicineType: MedicineType = MedicineType.TABLET,
    val colorHex: Long = 0xFF007A87, // Color integer
    val mealInstruction: MealInstruction = MealInstruction.AFTER_MEAL,
    val frequencyType: FrequencyType = FrequencyType.EVERY_DAY,
    val selectedDaysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1=Sunday, 2=Monday, ... 7=Saturday
    val reminderTimes: List<ReminderTime> = emptyList(),
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val fullDosage: String
        get() = "$dosageAmount $dosageUnit".trim()

    /**
     * Check if this medicine is scheduled on the given Calendar day
     */
    fun isScheduledForDay(calendar: Calendar): Boolean {
        if (!isActive) return false

        // Normalize calendar to start of day
        val checkCal = Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val checkMillis = checkCal.timeInMillis

        // Start Date check
        val startCal = Calendar.getInstance().apply {
            timeInMillis = startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (checkMillis < startCal.timeInMillis) return false

        // End Date check
        if (endDate != null) {
            val endCal = Calendar.getInstance().apply {
                timeInMillis = endDate
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            if (checkMillis > endCal.timeInMillis) return false
        }

        return when (frequencyType) {
            FrequencyType.EVERY_DAY -> true
            FrequencyType.SPECIFIC_DAYS -> {
                val dayOfWeek = checkCal.get(Calendar.DAY_OF_WEEK)
                selectedDaysOfWeek.contains(dayOfWeek)
            }
            FrequencyType.AS_NEEDED -> true
        }
    }
}
