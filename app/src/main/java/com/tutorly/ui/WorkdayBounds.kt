package com.tutorly.ui

import java.time.Duration
import java.time.LocalDate

private const val MINUTES_PER_HOUR: Int = 60
private const val MINUTES_IN_DAY: Int = MINUTES_PER_HOUR * 24
private const val SLOT_INCREMENT_MINUTES: Int = 30
private const val MAX_END_MINUTE: Int = MINUTES_IN_DAY
private const val MAX_START_MINUTE: Int = MAX_END_MINUTE - SLOT_INCREMENT_MINUTES

internal data class WorkdayBounds(
    val startMinutes: Int,
    val endMinutes: Int
)

internal fun sanitizeWorkdayBounds(startMinutes: Int, endMinutes: Int): WorkdayBounds {
    val sanitizedStart = startMinutes.coerceIn(0, MAX_START_MINUTE)
    val sanitizedEnd = endMinutes.coerceIn(
        sanitizedStart + SLOT_INCREMENT_MINUTES,
        MAX_END_MINUTE
    )
    return WorkdayBounds(
        startMinutes = sanitizedStart,
        endMinutes = sanitizedEnd
    )
}

internal fun CalendarLesson.isWithinBounds(
    date: LocalDate,
    bounds: WorkdayBounds
): Boolean {
    if (start.toLocalDate() != date) return false

    val lessonStartMinutes = start.hour * MINUTES_PER_HOUR + start.minute
    val lessonEndMinutes = if (end.toLocalDate().isAfter(date)) {
        val overflowMinutes = Duration.between(
            date.plusDays(1).atStartOfDay(end.zone),
            end
        ).toMinutes().toInt()
        MINUTES_IN_DAY + overflowMinutes
    } else {
        end.hour * MINUTES_PER_HOUR + end.minute
    }

    if (lessonEndMinutes <= lessonStartMinutes) return false

    return lessonStartMinutes >= bounds.startMinutes && lessonEndMinutes <= bounds.endMinutes
}

internal fun CalendarLesson.isWithinBounds(bounds: WorkdayBounds): Boolean {
    val date = start.toLocalDate()
    return isWithinBounds(date, bounds)
}
