package com.tutorly.domain.model

import com.tutorly.models.PaymentStatus
import com.tutorly.models.Student
import java.time.Instant

/**
 * Aggregated profile for a student that the Students screen can render inside the
 * bottom sheet.
 */
data class StudentProfile(
    val student: Student,
    val subject: String?,
    val grade: String?,
    val rate: StudentProfileLessonRate?,
    val hasDebt: Boolean,
    val metrics: StudentProfileMetrics,
    val recentLessons: List<StudentProfileLesson>
)

data class StudentProfileMetrics(
    val totalLessons: Int,
    val paidLessons: Int,
    val debtLessons: Int,
    val totalPaidCents: Long,
    val averagePriceCents: Int?,
    val outstandingCents: Long,
    val prepaymentCents: Long
)

data class StudentProfileLesson(
    val id: Long,
    val title: String?,
    val subjectName: String?,
    val startAt: Instant,
    val endAt: Instant,
    val durationMinutes: Int,
    val priceCents: Int,
    val paymentStatus: PaymentStatus
)

data class StudentProfileLessonRate(
    val durationMinutes: Int,
    val priceCents: Int
)
