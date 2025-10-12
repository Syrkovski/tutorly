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

    private val todayLessonsFlow = lessonsRepository.observeTodayLessons(
        dayStart = dayStart,
        dayEnd = dayEnd
    )
    private val outstandingLessonsFlow = lessonsRepository.observeOutstandingLessons(dayStart)

    val uiState: StateFlow<TodayUiState> = combine(
        todayLessonsFlow,
        outstandingLessonsFlow,
        nowState
    ) { today, outstanding, now ->
        buildUiState(today, outstanding, now)
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
        now: Instant
    ): TodayUiState {
        if (todayLessons.isEmpty() && outstandingLessons.isEmpty()) {
            return TodayUiState.Empty
        }

        val todaySorted = todayLessons.sortedWith(
            compareBy<LessonForToday> {
                if (it.endAt <= now && it.paymentStatus != PaymentStatus.UNPAID) 1 else 0
            }.thenBy { it.startAt }
        )
        val todayPending = todaySorted.filterNot { it.endAt <= now && it.paymentStatus != PaymentStatus.UNPAID }
        val todayMarked = todaySorted.filter { it.endAt <= now && it.paymentStatus != PaymentStatus.UNPAID }

        val sections = buildList {
            if (outstandingLessons.isNotEmpty()) {
                val grouped = outstandingLessons
                    .groupBy { it.startAt.atZone(zoneId).toLocalDate() }
                    .toSortedMap()
                grouped.forEach { (date, lessons) ->
                    add(
                        TodayLessonSection(
                            date = date,
                            isToday = false,
                            pending = lessons.sortedBy { it.startAt },
                            marked = emptyList()
                        )
                    )
                }
            }
            if (todaySorted.isNotEmpty()) {
                add(
                    TodayLessonSection(
                        date = dayStart.atZone(zoneId).toLocalDate(),
                        isToday = true,
                        pending = todayPending,
                        marked = todayMarked
                    )
                )
            }
        }

        if (sections.isEmpty()) {
            return TodayUiState.Empty
        }

        val outstandingUnpaidCount = outstandingLessons.count { it.paymentStatus == PaymentStatus.UNPAID }
        val todayUnpaidCount = todayPending.count { it.paymentStatus == PaymentStatus.UNPAID && it.endAt <= now }
        val pendingCount = outstandingUnpaidCount + todayUnpaidCount

        return TodayUiState.Content(
            sections = sections,
            pendingCount = pendingCount,
            isAllMarked = pendingCount == 0
        )
    }

}

sealed interface TodayUiState {
    data object Loading : TodayUiState
    data object Empty : TodayUiState
    data class Content(
        val sections: List<TodayLessonSection>,
        val pendingCount: Int,
        val isAllMarked: Boolean
    ) : TodayUiState
}

data class TodaySnackbarMessage(
    val lessonId: Long,
    val status: PaymentStatus
)

data class TodayLessonSection(
    val date: LocalDate,
    val isToday: Boolean,
    val pending: List<LessonForToday>,
    val marked: List<LessonForToday>
)
