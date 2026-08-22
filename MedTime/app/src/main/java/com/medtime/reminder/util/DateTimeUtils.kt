package com.medtime.reminder.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object DateTimeUtils {
    /** bit 0 = Sunday ... bit 6 = Saturday, matching Calendar.DAY_OF_WEEK - 1 */
    fun dayOfWeekBit(date: LocalDate): Int {
        // LocalDate.dayOfWeek: MONDAY=1 ... SUNDAY=7. Convert to Sunday=0 ... Saturday=6
        val isoValue = date.dayOfWeek.value // 1=Mon .. 7=Sun
        val sundayBased = isoValue % 7 // Sun->0, Mon->1 ... Sat->6
        return sundayBased
    }

    fun matchesDay(daysMask: Int, date: LocalDate): Boolean {
        val bit = dayOfWeekBit(date)
        return (daysMask shr bit) and 1 == 1
    }

    fun toEpochMillis(date: LocalDate, hour: Int, minute: Int): Long {
        return LocalDateTime.of(date, LocalTime.of(hour, minute))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    fun today(): LocalDate = LocalDate.now()

    fun formatTime(hour: Int, minute: Int, use24Hour: Boolean): String {
        val t = LocalTime.of(hour, minute)
        return if (use24Hour) {
            String.format("%02d:%02d", hour, minute)
        } else {
            val h12 = if (hour % 12 == 0) 12 else hour % 12
            val ampm = if (hour < 12) "AM" else "PM"
            String.format("%02d:%02d %s", h12, minute, ampm)
        }
    }

    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    const val ALL_DAYS_MASK = 127
}
