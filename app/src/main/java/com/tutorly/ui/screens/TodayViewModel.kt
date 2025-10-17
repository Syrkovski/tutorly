package com.tutorly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.model.LessonForToday
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.models.PaymentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val lessonsRepository: LessonsRepository
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val dayBounds: Pair<Instant, Instant> = run {
        val today = LocalDate.now(zoneId)
        val start = today.atStartOfDay(zoneId).toInstant()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant()
        start to end
    }
    private val dayStart: Instant = dayBounds.first
    private val dayEnd: Instant = dayBounds.second

    private val nowState = MutableStateFlow(Instant.now())
    private val snackbarState = MutableStateFlow<TodaySnackbarMessage?>(null)
    private val dayClosedState = MutableStateFlow(false)

    private val todayLessonsFlow = lessonsRepository.observeTodayLessons(
        dayStart = dayStart,
        dayEnd = dayEnd
    )
    private val outstandingLessonsFlow = lessonsRepository.observeOutstandingLessons(dayStart)

    val uiState: StateFlow<TodayUiState> = combine(
        todayLessonsFlow,
        outstandingLessonsFlow,
        nowState,
        dayClosedState
    ) { today, outstanding, now, isDayClosed ->
        buildUiState(today, outstanding, now, isDayClosed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Loading)

    val snackbarMessage: StateFlow<TodaySnackbarMessage?> = snackbarState.asStateFlow()

    init {
        refreshNow()
        viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                refreshNow()
            }
        }
    }

    fun onSwipeRight(lessonId: Long) {
        markLesson(lessonId, PaymentStatus.PAID)
    }

    fun onSwipeLeft(lessonId: Long) {
        markLesson(lessonId, PaymentStatus.DUE)
    }

    fun onUndo(lessonId: Long) {
        viewModelScope.launch {
            runCatching { lessonsRepository.resetPaymentStatus(lessonId) }
            refreshNow()
        }
    }

    fun onSnackbarShown() {
        snackbarState.value = null
    }

    fun onDayCloseConfirmed() {
        val currentState = uiState.value
        if (currentState is TodayUiState.DayInProgress && currentState.canCloseDay) {
            dayClosedState.value = true
        }
    }

    private fun markLesson(lessonId: Long, status: PaymentStatus) {
        viewModelScope.launch {
            val result = runCatching {
                when (status) {
                    PaymentStatus.PAID -> lessonsRepository.markPaid(lessonId)
                    PaymentStatus.DUE -> lessonsRepository.markDue(lessonId)
                    else -> Unit
                }
            }
            if (result.isSuccess) {
                snackbarState.value = TodaySnackbarMessage(lessonId, status)
                refreshNow()
            }
        }
    }

    private fun refreshNow() {
        nowState.value = Instant.now()
    }

    private fun buildUiState(
        todayLessons: List<LessonForToday>,
        outstandingLessons: List<LessonForToday>,
        now: Instant,
        isDayClosed: Boolean
    ): TodayUiState {
        if (todayLessons.isEmpty()) {
            if (dayClosedState.value) {
                dayClosedState.value = false
            }
            return TodayUiState.Empty
        }

        val todaySorted = todayLessons.sortedBy { it.startAt }
        val allMarked = todaySorted.all { it.paymentStatus != PaymentStatus.UNPAID }

        if (!allMarked && isDayClosed) {
            dayClosedState.value = false
        }

        if (isDayClosed && allMarked) {
            val paidAmountCents = todaySorted
                .filter { it.paymentStatus == PaymentStatus.PAID }
                .sumOf { it.priceCents.toLong() }
            val dueAmountCents = todaySorted
                .filter { it.paymentStatus == PaymentStatus.DUE }
                .sumOf { it.priceCents.toLong() }
            val todayDebtors = aggregateDebtors(todaySorted, setOf(PaymentStatus.DUE))
            val pastDebtors = aggregateDebtors(outstandingLessons, PaymentStatus.outstandingStatuses.toSet())
            val pastDebtorsPreview = pastDebtors.take(3)
            val hasMorePastDebtors = pastDebtors.size > pastDebtorsPreview.size

            return TodayUiState.DayClosed(
                paidAmountCents = paidAmountCents,
                todayDueAmountCents = dueAmountCents,
                todayDebtors = todayDebtors,
                pastDebtorsPreview = pastDebtorsPreview,
                hasMorePastDebtors = hasMorePastDebtors,
                lessons = todaySorted
            )
        }

        val completedLessons = todaySorted.count { it.endAt <= now }
        val remainingLessons = (todaySorted.size - completedLessons).coerceAtLeast(0)

        return TodayUiState.DayInProgress(
            lessons = todaySorted,
            completedLessons = completedLessons,
            totalLessons = todaySorted.size,
            remainingLessons = remainingLessons,
            canCloseDay = allMarked
        )
    }

    private fun aggregateDebtors(
        lessons: List<LessonForToday>,
        statuses: Set<PaymentStatus>
    ): List<TodayDebtor> {
        if (lessons.isEmpty()) return emptyList()
        return lessons
            .filter { it.paymentStatus in statuses }
            .groupBy { it.studentId }
            .map { (studentId, items) ->
                TodayDebtor(
                    studentId = studentId,
                    studentName = items.first().studentName,
                    lessonCount = items.size,
                    amountCents = items.sumOf { it.priceCents.toLong() }
                )
            }
            .sortedByDescending { it.amountCents }
    }
}

sealed interface TodayUiState {
    data object Loading : TodayUiState
    data object Empty : TodayUiState
    data class DayInProgress(
        val lessons: List<LessonForToday>,
        val completedLessons: Int,
        val totalLessons: Int,
        val remainingLessons: Int,
        val canCloseDay: Boolean
    ) : TodayUiState

    data class DayClosed(
        val paidAmountCents: Long,
        val todayDueAmountCents: Long,
        val todayDebtors: List<TodayDebtor>,
        val pastDebtorsPreview: List<TodayDebtor>,
        val hasMorePastDebtors: Boolean,
        val lessons: List<LessonForToday>
    ) : TodayUiState
}

data class TodaySnackbarMessage(
    val lessonId: Long,
    val status: PaymentStatus
)

data class TodayDebtor(
    val studentId: Long,
    val studentName: String,
    val lessonCount: Int,
    val amountCents: Long
)
