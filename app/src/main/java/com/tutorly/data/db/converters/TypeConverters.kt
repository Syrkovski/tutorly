package com.tutorly.data.db.converters

import androidx.room.TypeConverter
import com.tutorly.models.LessonRecurrence
import com.tutorly.models.LessonStatus
import com.tutorly.models.PaymentStatus
import com.tutorly.models.RecurrenceExceptionType
import com.tutorly.models.RecurrenceFrequency
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId


class InstantConverter {
    @TypeConverter fun toEpoch(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun fromEpoch(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}

class EnumConverters {
    @TypeConverter fun toPaymentStatus(v: String?): PaymentStatus? = v?.let { PaymentStatus.valueOf(it) }
    @TypeConverter fun fromPaymentStatus(v: PaymentStatus?): String? = v?.name
    @TypeConverter fun toLessonStatus(v: String?): LessonStatus? = v?.let { LessonStatus.valueOf(it) }
    @TypeConverter fun fromLessonStatus(v: LessonStatus?): String? = v?.name
    @TypeConverter fun toRecurrenceFrequency(v: String?): RecurrenceFrequency? = v?.let { RecurrenceFrequency.valueOf(it) }
    @TypeConverter fun fromRecurrenceFrequency(v: RecurrenceFrequency?): String? = v?.name
    @TypeConverter fun toRecurrenceExceptionType(v: String?): RecurrenceExceptionType? =
        v?.let { RecurrenceExceptionType.valueOf(it) }
    @TypeConverter fun fromRecurrenceExceptionType(v: RecurrenceExceptionType?): String? = v?.name
    @TypeConverter fun toDayOfWeekList(v: String?): List<DayOfWeek> =
        v?.takeIf { it.isNotBlank() }
            ?.split(',')
            ?.mapNotNull { token -> token.toIntOrNull()?.let { DayOfWeek.of(it) } }
            ?: emptyList()
    @TypeConverter fun fromDayOfWeekList(v: List<DayOfWeek>?): String? =
        v?.takeIf { it.isNotEmpty() }?.joinToString(separator = ",") { it.value.toString() }
}

class RecurrenceConverters {
    private companion object {
        private const val DELIMITER = "|"
        private const val DAYS_DELIMITER = ","
    }

    @TypeConverter
    fun toLessonRecurrence(value: String?): LessonRecurrence? {
        if (value.isNullOrBlank()) return null
        val parts = value.split(DELIMITER)
        if (parts.size != 6) return null

        val frequency = runCatching { RecurrenceFrequency.valueOf(parts[0]) }.getOrNull()
            ?: return null
        val interval = parts[1].toIntOrNull() ?: return null
        val days = parts[2]
            .takeIf { it.isNotBlank() }
            ?.split(DAYS_DELIMITER)
            ?.mapNotNull { token -> token.toIntOrNull()?.let(DayOfWeek::of) }
            ?: emptyList()
        val start = parts[3].toLongOrNull()?.let(Instant::ofEpochMilli) ?: return null
        val until = parts[4].toLongOrNull()?.let(Instant::ofEpochMilli)
        val zone = runCatching { ZoneId.of(parts[5]) }.getOrNull() ?: return null

        return LessonRecurrence(
            frequency = frequency,
            interval = interval,
            daysOfWeek = days,
            startDateTime = start,
            untilDateTime = until,
            timezone = zone
        )
    }

    @TypeConverter
    fun fromLessonRecurrence(recurrence: LessonRecurrence?): String? {
        recurrence ?: return null
        val days = recurrence.daysOfWeek.joinToString(DAYS_DELIMITER) { it.value.toString() }
        val until = recurrence.untilDateTime?.toEpochMilli()?.toString() ?: ""

        return listOf(
            recurrence.frequency.name,
            recurrence.interval.toString(),
            days,
            recurrence.startDateTime.toEpochMilli().toString(),
            until,
            recurrence.timezone.id
        ).joinToString(DELIMITER)
    }
}

