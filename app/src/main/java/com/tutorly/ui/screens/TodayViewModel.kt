package com.tutorly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.model.LessonForToday
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.DayClosureRepository
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val lessonsRepository: LessonsRepository,
    private val dayClosureRepository: DayClosureRepository
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
    private val currentDate: LocalDate = LocalDate.now(zoneId)

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
            dayClosureRepository.observeDayClosed(currentDate).collect { isClosed ->
                dayClosedState.value = isClosed
            }
        }
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
        if (currentState is TodayUiState.DayInProgress && currentState.showCloseDayCallout) {
            setDayClosed(true)
        }
    }

    fun onReopenDay() {
        if (uiState.value is TodayUiState.DayClosed) {
            setDayClosed(false)
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
        val outstanding = outstandingLessons
            .filter { it.paymentStatus in PaymentStatus.outstandingStatuses }
            .sortedByDescending { it.priceCents }
        val pastDueLessonsPreview = outstanding.take(3)
        val hasMorePastLessons = outstanding.size > pastDueLessonsPreview.size

        if (todayLessons.isEmpty()) {
            if (dayClosedState.value) {
                setDayClosed(false)
            }
            return TodayUiState.Empty(
                pastDueLessonsPreview = pastDueLessonsPreview,
                hasMorePastDueLessons = hasMorePastLessons
            )
        }

        val todaySorted = todayLessons.sortedBy { it.startAt }
        val completedLessons = todaySorted.count { it.endAt <= now }
        val allLessonsCompleted = completedLessons == todaySorted.size && todaySorted.isNotEmpty()
        val reviewLessons = todaySorted.filter { it.endAt <= now && it.paymentStatus == PaymentStatus.UNPAID }
        val markedLessons = todaySorted.filter { it.paymentStatus != PaymentStatus.UNPAID }
        val allMarked = todaySorted.all { it.paymentStatus != PaymentStatus.UNPAID }

        var dayClosed = isDayClosed

        if (!allMarked && dayClosed) {
            setDayClosed(false)
            dayClosed = false
        }

        if (allLessonsCompleted && reviewLessons.isNotEmpty()) {
            return TodayUiState.ReviewPending(
                reviewLessons = reviewLessons,
                markedLessons = markedLessons,
                totalLessons = todaySorted.size,
                pastDueLessonsPreview = pastDueLessonsPreview,
                hasMorePastDueLessons = hasMorePastLessons
            )
        }

        if (dayClosed) {
            val paidAmountCents = todaySorted
                .filter { it.paymentStatus == PaymentStatus.PAID }
                .sumOf { it.priceCents.toLong() }
            val dueAmountCents = todaySorted
                .filter { it.paymentStatus == PaymentStatus.DUE }
                .sumOf { it.priceCents.toLong() }
            val todayDueLessons = todaySorted.filter { it.paymentStatus == PaymentStatus.DUE }

            return TodayUiState.DayClosed(
                paidAmountCents = paidAmountCents,
                todayDueAmountCents = dueAmountCents,
                todayDueLessons = todayDueLessons,
                pastDueLessonsPreview = pastDueLessonsPreview,
                hasMorePastDueLessons = hasMorePastLessons,
                lessons = todaySorted,
                canReopen = dayClosed
            )
        }

        val remainingLessons = (todaySorted.size - completedLessons).coerceAtLeast(0)
        val showCloseDayCallout = allLessonsCompleted && allMarked

        return TodayUiState.DayInProgress(
            lessons = todaySorted,
            completedLessons = completedLessons,
            totalLessons = todaySorted.size,
            remainingLessons = remainingLessons,
            showCloseDayCallout = showCloseDayCallout,
            pastDueLessonsPreview = pastDueLessonsPreview,
            hasMorePastDueLessons = hasMorePastLessons
        )
    }

    private fun setDayClosed(isClosed: Boolean) {
        dayClosedState.value = isClosed
        viewModelScope.launch {
            dayClosureRepository.setDayClosed(currentDate, isClosed)
        }
    }
}

sealed interface TodayUiState {
    data object Loading : TodayUiState
    data class Empty(
        val pastDueLessonsPreview: List<LessonForToday>,
        val hasMorePastDueLessons: Boolean
    ) : TodayUiState
    data class DayInProgress(
        val lessons: List<LessonForToday>,
        val completedLessons: Int,
        val totalLessons: Int,
        val remainingLessons: Int,
        val showCloseDayCallout: Boolean,
        val pastDueLessonsPreview: List<LessonForToday>,
        val hasMorePastDueLessons: Boolean
    ) : TodayUiState

    data class ReviewPending(
        val reviewLessons: List<LessonForToday>,
        val markedLessons: List<LessonForToday>,
        val totalLessons: Int,
        val pastDueLessonsPreview: List<LessonForToday>,
        val hasMorePastDueLessons: Boolean
    ) : TodayUiState

    data class DayClosed(
        val paidAmountCents: Long,
        val todayDueAmountCents: Long,
        val todayDueLessons: List<LessonForToday>,
        val pastDueLessonsPreview: List<LessonForToday>,
        val hasMorePastDueLessons: Boolean,
        val lessons: List<LessonForToday>,
        val canReopen: Boolean
    ) : TodayUiState
}

data class TodaySnackbarMessage(
    val lessonId: Long,
    val status: PaymentStatus
)
