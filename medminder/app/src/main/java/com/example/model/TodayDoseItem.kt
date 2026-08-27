package com.example.model

import java.util.Calendar

data class TodayDoseItem(
    val medicine: Medicine,
    val reminderTime: ReminderTime,
    val dateString: String,
    val log: MedicineLog? = null,
    val computedStatus: DoseStatus = DoseStatus.UPCOMING
) {
    val idKey: String
        get() = "${medicine.id}_${dateString}_${reminderTime.toTimeString()}"

    val effectiveStatus: DoseStatus
        get() {
            if (log != null) {
                return log.status
            }
            return computedStatus
        }

    fun isPastTime(): Boolean {
        val now = Calendar.getInstance()
        val doseCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminderTime.hour)
            set(Calendar.MINUTE, reminderTime.minute)
            set(Calendar.SECOND, 0)
        }
        return now.after(doseCal)
    }
}
