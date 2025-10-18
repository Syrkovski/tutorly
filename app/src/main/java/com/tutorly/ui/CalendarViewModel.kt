package com.tutorly.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.model.LessonsRangeStats
import com.tutorly.domain.model.PaymentStatusIcon
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.domain.repo.UserProfileRepository
import com.tutorly.models.PaymentStatus
import com.tutorly.models.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val lessonsRepository: LessonsRepository,
    private val savedStateHandle: SavedStateHandle,
    private val userProfileRepository: UserProfileRepository,
    private val studentsRepository: StudentsRepository
) : ViewModel() {

    companion object {
        const val ARG_ANCHOR_DATE: String = "calendarDate"
        const val ARG_CALENDAR_MODE: String = "calendarMode"
    }

    private val zoneId: ZoneId = ZoneId.systemDefault()

    private val anchor = MutableStateFlow(savedStateHandle.restoreAnchor(zoneId))
    private val mode = MutableStateFlow(savedStateHandle.restoreMode())
    private val currentDateTime = MutableStateFlow(ZonedDateTime.now(zoneId))

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

    private val statsFlow = anchor
        .map { it.toWeekRange(zoneId) }
        .distinctUntilChanged()
        .flatMapLatest { range ->
            lessonsRepository.observeWeekStats(range.start, range.end)
        }

    private val userProfileFlow = userProfileRepository.profile
    private val hasStudentsFlow = studentsRepository
        .observeStudents("")
        .map { students -> students.isNotEmpty() }
        .distinctUntilChanged()

    init {
        viewModelScope.launch {
            while (isActive) {
                currentDateTime.value = ZonedDateTime.now(zoneId)
                delay(30_000)
            }
        }
        viewModelScope.launch {
            anchor.collect { savedStateHandle[ARG_ANCHOR_DATE] = it.toString() }
        }
        viewModelScope.launch {
            mode.collect { savedStateHandle[ARG_CALENDAR_MODE] = it.name }
        }
    }

    private val _events = MutableSharedFlow<CalendarEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CalendarEvent> = _events.asSharedFlow()

    fun onLessonSelected(lesson: CalendarLesson) {
        _events.tryEmit(
            CalendarEvent.OpenLesson(
                lessonId = lesson.id,
                studentId = lesson.studentId,
                start = lesson.start
            )
        )
    }

    fun onEmptySlotSelected(date: LocalDate, time: LocalTime, duration: Duration) {
        val start = date.atTime(time).atZone(zoneId)
        _events.tryEmit(CalendarEvent.CreateLesson(start, duration, null))
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        queryFlow,
        lessonsFlow,
        statsFlow,
        currentDateTime,
        userProfileFlow,
        hasStudentsFlow
    ) { query, lessons, stats, now, profile, hasStudents ->
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
            stats = stats,
            currentDateTime = now,
            workDayStartMinutes = profile.workDayStartMinutes,
            workDayEndMinutes = profile.workDayEndMinutes,
            weekendDays = profile.weekendDays,
            hasStudents = hasStudents
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(
            anchor = anchor.value,
            mode = mode.value,
            zoneId = zoneId,
            stats = LessonsRangeStats.EMPTY,
            currentDateTime = currentDateTime.value,
            workDayStartMinutes = UserProfile.DEFAULT_WORK_DAY_START,
            workDayEndMinutes = UserProfile.DEFAULT_WORK_DAY_END,
            weekendDays = emptySet(),
            hasStudents = true
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
        val today = currentDateTime.value.toLocalDate()
        anchor.value = today
        mode.value = newMode
    }
}

private data class CalendarQuery(
    val anchor: LocalDate,
    val mode: CalendarMode,
    val range: CalendarRange
)

private fun LocalDate.toWeekRange(zoneId: ZoneId): CalendarRange {
    val weekStart = with(DayOfWeek.MONDAY)
    val start = weekStart.atStartOfDay(zoneId).toInstant()
    val end = weekStart.plusWeeks(1).atStartOfDay(zoneId).toInstant()
    return CalendarRange(start, end)
}

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
        studentId = studentId,
        start = lessonStart,
        end = lessonEnd,
        duration = duration,
        studentName = studentName,
        studentNote = studentNote,
        subjectName = subjectName,
        studentGrade = studentGrade,
        lessonTitle = lessonTitle,
        lessonNote = lessonNote,
        paymentStatus = paymentStatus,
        paymentStatusIcon = paymentStatusIcon,
        paidCents = paidCents,
        priceCents = priceCents,
        subjectColorArgb = subjectColorArgb
    )
}

private data class CalendarRange(val start: Instant, val end: Instant)

data class CalendarLesson(
    val id: Long,
    val studentId: Long,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val duration: Duration,
    val studentName: String,
    val studentNote: String?,
    val subjectName: String?,
    val studentGrade: String?,
    val lessonTitle: String?,
    val lessonNote: String?,
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
    val stats: LessonsRangeStats = LessonsRangeStats.EMPTY,
    val currentDateTime: ZonedDateTime,
    val workDayStartMinutes: Int,
    val workDayEndMinutes: Int,
    val weekendDays: Set<DayOfWeek>,
    val hasStudents: Boolean = true
)

sealed interface CalendarEvent {
    data class CreateLesson(
        val start: ZonedDateTime,
        val duration: Duration,
        val studentId: Long?
    ) : CalendarEvent

    data class OpenLesson(
        val lessonId: Long,
        val studentId: Long,
        val start: ZonedDateTime
    ) : CalendarEvent
}

private fun SavedStateHandle.restoreAnchor(zoneId: ZoneId): LocalDate {
    val raw = get<String>(CalendarViewModel.ARG_ANCHOR_DATE)
    return raw?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) } ?: LocalDate.now(zoneId)
}

private fun SavedStateHandle.restoreMode(): CalendarMode {
    val raw = get<String>(CalendarViewModel.ARG_CALENDAR_MODE)
    return raw?.let { runCatching { CalendarMode.valueOf(it) }.getOrNull() } ?: CalendarMode.DAY
}
