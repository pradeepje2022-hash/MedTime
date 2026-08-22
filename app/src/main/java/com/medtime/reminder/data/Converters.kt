package com.medtime.reminder.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMedicineType(value: MedicineType): String = value.name

    @TypeConverter
    fun toMedicineType(value: String): MedicineType = MedicineType.valueOf(value)

    @TypeConverter
    fun fromDoseStatus(value: DoseStatus): String = value.name

    @TypeConverter
    fun toDoseStatus(value: String): DoseStatus = DoseStatus.valueOf(value)
}
