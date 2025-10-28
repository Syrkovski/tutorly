package com.tutorly.domain.recurrence

import com.tutorly.models.RecurrenceFrequency
import com.tutorly.models.RecurrenceRule
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

object RecurrenceLabelFormatter {
    fun format(rule: RecurrenceRule): String {
        val zone = runCatching { ZoneId.of(rule.timezone) }.getOrDefault(ZoneId.systemDefault())
        val start = rule.startDateTime.atZone(zone)
        return format(rule.frequency, rule.interval, rule.daysOfWeek, start)
    }

    fun format(
        frequency: RecurrenceFrequency,
        interval: Int,
        daysOfWeek: List<DayOfWeek>,
        start: ZonedDateTime
    ): String {
        val sanitizedInterval = interval.coerceAtLeast(1)
        return when (frequency) {
            RecurrenceFrequency.MONTHLY_BY_DOW -> formatMonthly(start, sanitizedInterval)
            RecurrenceFrequency.WEEKLY -> formatWeekly(start, daysOfWeek, sanitizedInterval)
            RecurrenceFrequency.BIWEEKLY -> formatBiWeekly(start, daysOfWeek, sanitizedInterval)
        }
    }

    private fun formatMonthly(start: ZonedDateTime, interval: Int): String {
        val ordinal = ((start.dayOfMonth - 1) / 7) + 1
        val ordinalLabel = when (ordinal) {
            1 -> "первую"
            2 -> "вторую"
            3 -> "третью"
            4 -> "четвертую"
            else -> "пятую"
        }
        val base = start.dayOfWeek.toShortLabel()
        return if (interval <= 1) {
            "каждую ${ordinalLabel} $base"
        } else {
            val monthWord = pluralMonths(interval)
            "каждые $interval $monthWord в ${ordinalLabel} $base"
        }
    }

    private fun formatWeekly(
        start: ZonedDateTime,
        daysOfWeek: List<DayOfWeek>,
        interval: Int
    ): String {
        val targets = resolveTargets(start, daysOfWeek)
        val daysLabel = targets.joinToString(separator = ", ") { it.toShortLabel() }
        return if (interval <= 1) {
            "каждую $daysLabel"
        } else {
            val weekWord = pluralWeeks(interval)
            "каждые $interval $weekWord по $daysLabel"
        }
    }

    private fun formatBiWeekly(
        start: ZonedDateTime,
        daysOfWeek: List<DayOfWeek>,
        interval: Int
    ): String {
        val actualInterval = interval.coerceAtLeast(1) * 2
        val targets = resolveTargets(start, daysOfWeek)
        val daysLabel = targets.joinToString(separator = ", ") { it.toShortLabel() }
        val weekWord = pluralWeeks(actualInterval)
        return "каждые $actualInterval $weekWord по $daysLabel"
    }

    private fun resolveTargets(
        start: ZonedDateTime,
        days: List<DayOfWeek>
    ): List<DayOfWeek> {
        val resolved = if (days.isEmpty()) {
            listOf(start.dayOfWeek)
        } else {
            days
        }
        return resolved.sortedBy { it.value }
    }
}

private fun DayOfWeek.toShortLabel(): String {
    return when (this) {
        DayOfWeek.MONDAY -> "Пн"
        DayOfWeek.TUESDAY -> "Вт"
        DayOfWeek.WEDNESDAY -> "Ср"
        DayOfWeek.THURSDAY -> "Чт"
        DayOfWeek.FRIDAY -> "Пт"
        DayOfWeek.SATURDAY -> "Сб"
        DayOfWeek.SUNDAY -> "Вс"
    }
}

private fun pluralWeeks(value: Int): String {
    val mod100 = value % 100
    return if (mod100 in 11..14) {
        "недель"
    } else {
        when (value % 10) {
            2, 3, 4 -> "недели"
            else -> "недель"
        }
    }
}

private fun pluralMonths(value: Int): String {
    val mod100 = value % 100
    return if (mod100 in 11..14) {
        "месяцев"
    } else {
        when (value % 10) {
            1 -> "месяц"
            2, 3, 4 -> "месяца"
            else -> "месяцев"
        }
    }
}
