package com.tutorly.data.db.converters

import androidx.room.TypeConverter
import com.tutorly.models.LessonStatus
import com.tutorly.models.PaymentStatus
import com.tutorly.models.RecurrenceExceptionType
import com.tutorly.models.RecurrenceFrequency
import java.time.DayOfWeek
import java.time.Instant


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
        v?.let { list ->
            if (list.isEmpty()) "" else list.joinToString(separator = ",") { it.value.toString() }
        }
}

