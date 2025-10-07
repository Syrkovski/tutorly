package com.tutorly.models

import androidx.room.*
import java.time.Instant

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
    val status: LessonStatus = LessonStatus.PLANNED,
    val note: String? = null,            // заметка урока (тема/замечания)
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val canceledAt: Instant? = null
)
