package com.tutorly.domain.model

import com.tutorly.models.LessonRecurrence
import com.tutorly.models.LessonStatus
import com.tutorly.models.PaymentStatus
import java.time.Duration
import java.time.Instant

/**
 * Aggregated lesson payload tailored for scheduling and calendar UIs.
 * Provides the normalized time bounds, student/subject descriptors and payment meta
 * so downstream layers do not have to touch raw entities.
 */
data class LessonDetails(
    val id: Long,
    val baseLessonId: Long,
    val studentId: Long,
    val startAt: Instant,
    val endAt: Instant,
    val duration: Duration,
    val studentName: String,
    val studentNote: String?,
    val subjectName: String?,
    val studentGrade: String?,
    val subjectColorArgb: Int?,
    val paymentStatus: PaymentStatus,
    val paymentStatusIcon: PaymentStatusIcon,
    val lessonStatus: LessonStatus = LessonStatus.PLANNED,
    val priceCents: Int,
    val paidCents: Int,
    val lessonTitle: String?,
    val lessonNote: String?,
    val isRecurring: Boolean = false,
    val seriesId: Long? = null,
    val originalStartAt: Instant? = null,
    val recurrenceLabel: String? = null,
    val recurrence: LessonRecurrence? = null
) {
    companion object {
        val DEFAULT_DURATION: Duration = Duration.ofMinutes(DEFAULT_LESSON_DURATION_MINUTES)
    }
}

enum class PaymentStatusIcon {
    PAID,
    OUTSTANDING,
    CANCELLED
}

internal fun resolveDuration(
    startAt: Instant,
    endAt: Instant,
    subjectDurationMinutes: Int?
): Duration {
    val raw = Duration.between(startAt, endAt)
    if (!raw.isNegative && !raw.isZero) return raw

    val preset = subjectDurationMinutes
        ?.takeIf { it > 0 }
        ?.let { Duration.ofMinutes(it.toLong()) }

    return preset ?: LessonDetails.DEFAULT_DURATION
}

fun PaymentStatus.asIcon(): PaymentStatusIcon = when (this) {
    PaymentStatus.PAID -> PaymentStatusIcon.PAID
    PaymentStatus.DUE, PaymentStatus.UNPAID -> PaymentStatusIcon.OUTSTANDING
    PaymentStatus.CANCELLED -> PaymentStatusIcon.CANCELLED
}

private const val DEFAULT_LESSON_DURATION_MINUTES = 60L
