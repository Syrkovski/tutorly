package com.tutorly.models

import androidx.room.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

@Entity(
    tableName = "lessons",
    indices = [Index("startAt"), Index("studentId"), Index("paymentStatus")],
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubjectPreset::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Lesson(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val studentId: Long,
    val subjectId: Long?,                // null, если без пресета
    val title: String? = null,           // опционально: "Подготовка к ЕГЭ: Дроби"
    val startAt: Instant,
    val endAt: Instant,
    val priceCents: Int,                 // цена по факту для этого занятия
    val paidCents: Int = 0,              // сколько уже оплачено
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val markedAt: Instant? = null,
    val status: LessonStatus = LessonStatus.PLANNED,
    val note: String? = null,            // заметка урока (тема/замечания)
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val canceledAt: Instant? = null,
    val seriesId: Long? = null,          // ссылка на правило повторения, если есть
    val isInstance: Boolean = false,     // true только для материализованных экземпляров
    @Ignore val recurrence: LessonRecurrence? = null
) {
    constructor(
        id: Long = 0L,
        studentId: Long,
        subjectId: Long?,
        title: String? = null,
        startAt: Instant,
        endAt: Instant,
        priceCents: Int,
        paidCents: Int = 0,
        paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
        markedAt: Instant? = null,
        status: LessonStatus = LessonStatus.PLANNED,
        note: String? = null,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now(),
        canceledAt: Instant? = null,
        seriesId: Long? = null,
        isInstance: Boolean = false,
        recurrence: LessonRecurrence? = null
    ) : this(
        id = id,
        studentId = studentId,
        subjectId = subjectId,
        title = title,
        startAt = startAt,
        endAt = endAt,
        priceCents = priceCents,
        paidCents = paidCents,
        paymentStatus = paymentStatus,
        markedAt = markedAt,
        status = status,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        canceledAt = canceledAt,
        seriesId = seriesId,
        isInstance = isInstance,
        recurrence = recurrence
    )
}

data class LessonRecurrence(
    val frequency: RecurrenceFrequency,
    val interval: Int,
    val daysOfWeek: List<DayOfWeek>,
    val startDateTime: Instant,
    val untilDateTime: Instant?,
    val timezone: ZoneId
)
