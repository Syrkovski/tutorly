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

    private val lessonsFlow = lessonsRepository.observeTodayLessons(
        dayStart = dayStart,
        dayEnd = dayEnd
    )

    val uiState: StateFlow<TodayUiState> = combine(lessonsFlow, nowState) { lessons, now ->
        buildUiState(lessons, now)
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

    fun onNoteSave(lessonId: Long, text: String?) {
        viewModelScope.launch {
            runCatching { lessonsRepository.saveNote(lessonId, text) }
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
        lessons: List<LessonForToday>,
        now: Instant
    ): TodayUiState {
        val pastLessons = lessons.filter { it.endAt <= now }
        if (pastLessons.isEmpty()) {
            return TodayUiState.Empty
        }

        val (pending, marked) = pastLessons.partition { it.paymentStatus == PaymentStatus.UNPAID }
        val sortedPending = pending.sortedBy { it.startAt }
        val sortedMarked = marked.sortedWith(
            compareByDescending<LessonForToday> { it.markedAt ?: Instant.EPOCH }
                .thenBy { it.startAt }
        )

        val allItems = sortedPending + sortedMarked
        return TodayUiState.Content(
            lessons = allItems,
            remainingCount = sortedPending.size,
            isAllMarked = sortedPending.isEmpty()
        )
    }

}

sealed interface TodayUiState {
    data object Loading : TodayUiState
    data object Empty : TodayUiState
    data class Content(
        val lessons: List<LessonForToday>,
        val remainingCount: Int,
        val isAllMarked: Boolean
    ) : TodayUiState
}

data class TodaySnackbarMessage(
    val lessonId: Long,
    val status: PaymentStatus
)
