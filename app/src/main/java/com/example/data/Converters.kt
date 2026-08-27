package com.example.data

import androidx.room.TypeConverter
import com.example.model.DoseStatus
import com.example.model.FrequencyType
import com.example.model.MealInstruction
import com.example.model.MedicineType
import com.example.model.ReminderTime

class Converters {

    @TypeConverter
    fun fromMedicineType(value: MedicineType): String = value.name

    @TypeConverter
    fun toMedicineType(value: String): MedicineType = MedicineType.fromString(value)

    @TypeConverter
    fun fromMealInstruction(value: MealInstruction): String = value.name

    @TypeConverter
    fun toMealInstruction(value: String): MealInstruction =
        try { MealInstruction.valueOf(value) } catch (e: Exception) { MealInstruction.AFTER_MEAL }

    @TypeConverter
    fun fromFrequencyType(value: FrequencyType): String = value.name

    @TypeConverter
    fun toFrequencyType(value: String): FrequencyType =
        try { FrequencyType.valueOf(value) } catch (e: Exception) { FrequencyType.EVERY_DAY }

    @TypeConverter
    fun fromDoseStatus(value: DoseStatus): String = value.name

    @TypeConverter
    fun toDoseStatus(value: String): DoseStatus =
        try { DoseStatus.valueOf(value) } catch (e: Exception) { DoseStatus.UPCOMING }

    @TypeConverter
    fun fromDaysList(days: List<Int>?): String {
        return days?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toDaysList(value: String?): List<Int> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    @TypeConverter
    fun fromReminderTimes(times: List<ReminderTime>?): String {
        if (times.isNullOrEmpty()) return ""
        // format: "08:00#Morning;14:00#Afternoon"
        return times.joinToString(";") { "${it.hour}:${it.minute}#${it.label}" }
    }

    @TypeConverter
    fun toReminderTimes(value: String?): List<ReminderTime> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(";").mapNotNull { item ->
            val parts = item.split("#")
            if (parts.isNotEmpty()) {
                val timeParts = parts[0].split(":")
                if (timeParts.size == 2) {
                    val h = timeParts[0].toIntOrNull() ?: 0
                    val m = timeParts[1].toIntOrNull() ?: 0
                    val label = if (parts.size > 1) parts[1] else "Reminder"
                    ReminderTime(h, m, label)
                } else null
            } else null
        }
    }
}
