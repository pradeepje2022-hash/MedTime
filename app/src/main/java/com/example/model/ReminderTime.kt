package com.example.model

import java.util.Locale

data class ReminderTime(
    val hour: Int,
    val minute: Int,
    val label: String = "Reminder"
) {
    fun format12Hour(): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
    }

    fun format24Hour(): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    fun format(is24Hour: Boolean = false): String {
        return if (is24Hour) format24Hour() else format12Hour()
    }

    fun toTimeString(): String {
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    companion object {
        fun fromTimeString(timeStr: String, label: String = "Reminder"): ReminderTime? {
            val parts = timeStr.trim().split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull() ?: return null
                val m = parts[1].toIntOrNull() ?: return null
                return ReminderTime(h, m, label)
            }
            return null
        }
    }
}
