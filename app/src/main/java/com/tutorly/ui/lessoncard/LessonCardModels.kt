package com.tutorly.ui.lessoncard

import com.tutorly.models.PaymentStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

data class LessonCardUiState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val lessonId: Long? = null,
    val studentId: Long? = null,
    val studentName: String = "",
    val studentGrade: String? = null,
    val subjectName: String? = null,
    val studentOptions: List<LessonStudentOption> = emptyList(),
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val durationMinutes: Int = 60,
    val priceCents: Int = 0,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val paidCents: Int = 0,
    val note: String? = null,
    val snackbarMessage: LessonCardMessage? = null,
    val currencySymbol: String = "₽",
    val currencyCode: String = "RUB",
    val locale: Locale = Locale.getDefault(),
    val zoneId: ZoneId = ZoneId.systemDefault(),
    val isPaymentActionRunning: Boolean = false,
)

data class LessonStudentOption(
    val id: Long,
    val name: String,
    val grade: String?,
    val subject: String?,
)

sealed interface LessonCardMessage {
    data class Error(val message: String?) : LessonCardMessage
}

internal const val LESSON_CARD_NOTE_LIMIT = 500
