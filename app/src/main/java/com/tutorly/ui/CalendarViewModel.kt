package com.tutorly.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.model.PaymentStatusIcon
import com.tutorly.domain.model.LessonsRangeStats
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.models.PaymentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val lessonsRepository: LessonsRepository
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()

    private val anchor = MutableStateFlow(LocalDate.now(zoneId))
    private val mode = MutableStateFlow(CalendarMode.DAY)

    private val queryFlow = combine(anchor, mode) { currentAnchor, currentMode ->
        CalendarQuery(
            anchor = currentAnchor,
            mode = currentMode,
            range = currentMode.toRange(currentAnchor, zoneId)
        )
    }

    private val lessonsFlow = queryFlow.flatMapLatest { query ->
        lessonsRepository.observeLessons(query.range.start, query.range.end)
    }

    private val statsFlow = queryFlow.flatMapLatest { query ->
        lessonsRepository.observeWeekStats(query.range.start, query.range.end)
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        queryFlow,
        lessonsFlow,
        statsFlow
    ) { query, lessons, stats ->
        val calendarLessons = lessons
            .map { it.toCalendarLesson(zoneId) }
            .sortedBy { it.start }
        val groupedByDate = calendarLessons.groupBy { it.start.toLocalDate() }
        CalendarUiState(
            anchor = query.anchor,
            mode = query.mode,
            zoneId = zoneId,
            lessons = calendarLessons,
            lessonsByDate = groupedByDate,
            stats = stats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(
            anchor = anchor.value,
            mode = mode.value,
            zoneId = zoneId,
            stats = LessonsRangeStats.EMPTY
        )
    )

    fun goToPreviousPeriod() {
        anchor.update { current ->
            when (mode.value) {
                CalendarMode.DAY -> current.minusDays(1)
                CalendarMode.WEEK -> current.minusWeeks(1)
                CalendarMode.MONTH -> current.minusMonths(1)
            }
        }
    }

    fun goToNextPeriod() {
        anchor.update { current ->
            when (mode.value) {
                CalendarMode.DAY -> current.plusDays(1)
                CalendarMode.WEEK -> current.plusWeeks(1)
                CalendarMode.MONTH -> current.plusMonths(1)
            }
        }
    }

    fun selectDate(date: LocalDate) {
        anchor.value = date
    }

    fun setMode(newMode: CalendarMode) {
        if (mode.value == newMode) return
        mode.value = newMode
    }
}

private data class CalendarQuery(
    val anchor: LocalDate,
    val mode: CalendarMode,
    val range: CalendarRange
)

private fun CalendarMode.toRange(anchor: LocalDate, zoneId: ZoneId): CalendarRange = when (this) {
    CalendarMode.DAY -> {
        val start = anchor.atStartOfDay(zoneId).toInstant()
        val end = anchor.plusDays(1).atStartOfDay(zoneId).toInstant()
        CalendarRange(start, end)
    }
    CalendarMode.WEEK -> {
        val weekStart = anchor.with(DayOfWeek.MONDAY)
        val start = weekStart.atStartOfDay(zoneId).toInstant()
        val end = weekStart.plusWeeks(1).atStartOfDay(zoneId).toInstant()
        CalendarRange(start, end)
    }
    CalendarMode.MONTH -> {
        val monthStart = anchor.withDayOfMonth(1)
        val start = monthStart.atStartOfDay(zoneId).toInstant()
        val end = monthStart.plusMonths(1).atStartOfDay(zoneId).toInstant()
        CalendarRange(start, end)
    }
}

private fun LessonDetails.toCalendarLesson(zoneId: ZoneId): CalendarLesson {
    val lessonStart = startAt.atZone(zoneId)
    val lessonEnd = endAt.atZone(zoneId)

    return CalendarLesson(
        id = id,
        start = lessonStart,
        end = lessonEnd,
        duration = duration,
        studentName = studentName,
        studentNote = studentNote,
        subjectName = subjectName,
        lessonTitle = lessonTitle,
        paymentStatus = paymentStatus,
        paymentStatusIcon = paymentStatusIcon,
        paidCents = paidCents,
        priceCents = priceCents,
        subjectColorArgb = subjectColorArgb
    )
}

private data class CalendarRange(val start: java.time.Instant, val end: java.time.Instant)

data class CalendarLesson(
    val id: Long,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val duration: Duration,
    val studentName: String,
    val studentNote: String?,
    val subjectName: String?,
    val lessonTitle: String?,
    val paymentStatus: PaymentStatus,
    val paymentStatusIcon: PaymentStatusIcon,
    val paidCents: Int,
    val priceCents: Int,
    val subjectColorArgb: Int?
)

data class CalendarUiState(
    val anchor: LocalDate,
    val mode: CalendarMode,
    val zoneId: ZoneId,
    val lessons: List<CalendarLesson> = emptyList(),
    val lessonsByDate: Map<LocalDate, List<CalendarLesson>> = emptyMap(),
    val stats: LessonsRangeStats = LessonsRangeStats.EMPTY
)
