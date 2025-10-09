package com.tutorly.ui.lessoncard

import com.tutorly.domain.model.PaymentStatusIcon
import com.tutorly.models.PaymentStatus
import java.time.Duration
import java.time.Instant

internal data class LessonCardDetails(
    val id: Long,
    val studentId: Long,
    val studentName: String,
    val studentNote: String?,
    val subjectName: String?,
    val lessonTitle: String?,
    val lessonNote: String?,
    val startAt: Instant,
    val endAt: Instant,
    val duration: Duration,
    val priceCents: Int,
    val paidCents: Int,
    val paymentStatus: PaymentStatus,
    val paymentStatusIcon: PaymentStatusIcon,
)

internal data class LessonCardUiState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val details: LessonCardDetails? = null,
    val noteDraft: String = "",
    val isNoteDirty: Boolean = false,
    val isSavingNote: Boolean = false,
    val isPaymentActionRunning: Boolean = false,
    val showMarkDueDialog: Boolean = false,
    val showUnsavedExitDialog: Boolean = false,
    val pendingExitAction: LessonCardExitAction? = null,
    val snackbarMessage: LessonCardMessage? = null,
)

internal sealed interface LessonCardMessage {
    data object NoteSaved : LessonCardMessage
    data class PaymentMarked(val status: PaymentStatus) : LessonCardMessage
    data class Error(val throwable: Throwable) : LessonCardMessage
}

internal sealed interface LessonCardExitAction {
    data object Close : LessonCardExitAction
    data class NavigateToEdit(val details: LessonCardDetails) : LessonCardExitAction
}

internal const val LESSON_CARD_NOTE_LIMIT = 500
