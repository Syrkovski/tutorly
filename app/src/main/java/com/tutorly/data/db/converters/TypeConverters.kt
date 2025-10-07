package com.tutorly.data.db.converters

import androidx.room.TypeConverter
import com.tutorly.models.LessonStatus
import com.tutorly.models.PaymentStatus
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
}

