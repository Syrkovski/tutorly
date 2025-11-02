package com.tutorly.domain.model

import com.tutorly.models.PaymentStatus
import java.time.Duration
import java.time.Instant

/**
 * Lightweight payload for the "Сегодня" summary screen. Combines lesson, student and
 * payment fields required to render swipe actions and the note editor.
 */
data class LessonForToday(
    val id: Long,
    val baseLessonId: Long,
    val studentId: Long,
    val studentName: String,
    val studentGrade: String?,
    val subjectName: String?,
    val lessonTitle: String?,
    val startAt: Instant,
    val endAt: Instant,
    val duration: Duration,
    val priceCents: Int,
    val studentRateCents: Int?,
    val note: String?,
    val paymentStatus: PaymentStatus,
    val markedAt: Instant?,
    val isRecurring: Boolean,
    val seriesId: Long?,
    val originalStartAt: Instant?,
    val recurrenceLabel: String?
)
