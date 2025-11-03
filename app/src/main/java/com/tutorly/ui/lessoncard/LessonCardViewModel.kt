package com.tutorly.ui.lessoncard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.recurrence.RecurrenceLabelFormatter
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.domain.repo.UserSettingsRepository
import com.tutorly.models.Lesson
import com.tutorly.models.LessonRecurrence
import com.tutorly.models.RecurrenceFrequency
import com.tutorly.models.PaymentStatus
import com.tutorly.models.Student
import com.tutorly.ui.screens.normalizeGrade
import com.tutorly.ui.screens.normalizeSubject
import com.tutorly.ui.screens.titleCaseWords
import com.tutorly.ui.lessoncreation.RecurrenceMode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LessonCardViewModel @Inject constructor(
    private val lessonsRepository: LessonsRepository,
    private val studentsRepository: StudentsRepository,
    private val userSettingsRepository: UserSettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonCardUiState())
    val uiState: StateFlow<LessonCardUiState> = _uiState.asStateFlow()

    private var lessonJob: Job? = null
    private var currentLesson: Lesson? = null

    init {
        viewModelScope.launch {
            val settings = userSettingsRepository.get()
            val locale = Locale(settings.locale)
            val currencySymbol = runCatching { Currency.getInstance(settings.currency).getSymbol(locale) }
                .getOrDefault(settings.currency)
            _uiState.update {
                it.copy(
                    locale = locale,
                    currencySymbol = currencySymbol,
                    currencyCode = settings.currency,
                    zoneId = ZoneId.systemDefault(),
                )
            }
        }
    }

    fun open(lessonId: Long) {
        if (_uiState.value.isVisible && _uiState.value.lessonId == lessonId) {
            return
        }
        lessonJob?.cancel()
        currentLesson = null
        _uiState.update {
            it.copy(
                isVisible = true,
                isLoading = true,
                isSaving = false,
                isDeleting = false,
                lessonId = lessonId,
                snackbarMessage = null,
                recurrenceEditor = null,
            )
        }

        lessonJob = viewModelScope.launch {
            lessonsRepository.observeLessonDetails(lessonId).collect { details ->
                if (details == null) {
                    _uiState.update { state -> state.copy(isLoading = false) }
                } else {
                    currentLesson = lessonsRepository.getById(details.id)
                    val zone = _uiState.value.zoneId
                    val start = details.startAt.atZone(zone)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            studentId = details.studentId,
                            studentName = details.studentName,
                            studentGrade = normalizeGrade(details.studentGrade),
                            subjectName = details.subjectName
                                ?.takeIf { it.isNotBlank() }
                                ?.trim(),
                            date = start.toLocalDate(),
                            time = start.toLocalTime(),
                            durationMinutes = details.duration.toMinutes().toInt(),
                            priceCents = details.priceCents,
                            paymentStatus = details.paymentStatus,
                            paidCents = details.paidCents,
                            note = details.lessonNote,
                            studentOptions = reorderOptions(state.studentOptions, details.studentId),
                            isRecurring = details.isRecurring,
                            recurrenceLabel = details.recurrenceLabel,
                        )
                    }
                    loadStudents(details.studentId)
                }
            }
        }
    }

    fun dismiss() {
        lessonJob?.cancel()
        lessonJob = null
        currentLesson = null
        _uiState.update {
            it.copy(
                isVisible = false,
                lessonId = null,
                snackbarMessage = null,
                isSaving = false,
                isLoading = false,
                isPaymentActionRunning = false,
                isDeleting = false,
                recurrenceEditor = null,
            )
        }
    }

    fun deleteLesson() {
        val lessonId = _uiState.value.lessonId ?: return
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true, snackbarMessage = null) }
        viewModelScope.launch {
            val result = runCatching { lessonsRepository.delete(lessonId) }
            result.onSuccess {
                dismiss()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        snackbarMessage = LessonCardMessage.Error(error.message)
                    )
                }
            }
        }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun onStudentSelected(studentId: Long) {
        val lesson = currentLesson ?: return
        if (lesson.studentId == studentId) return
        val selected = _uiState.value.studentOptions.firstOrNull { it.id == studentId }
        submitUpdate(
            lesson.copy(studentId = studentId, updatedAt = Instant.now())
        ) { state ->
            state.copy(
                studentId = studentId,
                studentName = selected?.name ?: state.studentName,
                studentGrade = normalizeGrade(selected?.grade) ?: state.studentGrade,
                studentOptions = reorderOptions(state.studentOptions, studentId),
            )
        }
    }

    fun onDateSelected(date: LocalDate) {
        val lesson = currentLesson ?: return
        val state = _uiState.value
        val zone = state.zoneId
        val start = ZonedDateTime.of(date, state.time, zone)
        val end = start.plusMinutes(state.durationMinutes.toLong())
        submitUpdate(
            lesson.copy(startAt = start.toInstant(), endAt = end.toInstant(), updatedAt = Instant.now())
        ) { it.copy(date = date) }
        updateRecurrenceEditorBase(date = date, time = state.time)
    }

    fun onTimeSelected(time: LocalTime) {
        val lesson = currentLesson ?: return
        val state = _uiState.value
        val zone = state.zoneId
        val start = ZonedDateTime.of(state.date, time, zone)
        val end = start.plusMinutes(state.durationMinutes.toLong())
        submitUpdate(
            lesson.copy(startAt = start.toInstant(), endAt = end.toInstant(), updatedAt = Instant.now())
        ) { it.copy(time = time) }
        updateRecurrenceEditorBase(date = state.date, time = time)
    }

    fun onDurationSelected(minutes: Int) {
        if (minutes <= 0) return
        val lesson = currentLesson ?: return
        val state = _uiState.value
        val zone = state.zoneId
        val start = ZonedDateTime.of(state.date, state.time, zone)
        val end = start.plusMinutes(minutes.toLong())
        submitUpdate(
            lesson.copy(endAt = end.toInstant(), updatedAt = Instant.now())
        ) { it.copy(durationMinutes = minutes) }
    }

    fun onPriceChanged(priceCents: Int) {
        val lesson = currentLesson ?: return
        submitUpdate(
            lesson.copy(priceCents = priceCents.coerceAtLeast(0), updatedAt = Instant.now())
        ) { it.copy(priceCents = priceCents.coerceAtLeast(0)) }
    }

    fun onRecurrenceSelected(recurrence: LessonRecurrence?) {
        val lesson = currentLesson ?: return
        val updated = if (recurrence != null) {
            lesson.copy(recurrence = recurrence, updatedAt = Instant.now())
        } else {
            lesson.copy(seriesId = null, recurrence = null, updatedAt = Instant.now())
        }
        submitUpdate(updated) { it }
    }

    fun onRecurrenceRowClick() {
        val lesson = currentLesson ?: return
        val editor = buildRecurrenceEditorState(lesson, _uiState.value)
        _uiState.update { it.copy(recurrenceEditor = editor) }
    }

    fun onRecurrenceEditorDismissed() {
        _uiState.update { it.copy(recurrenceEditor = null) }
    }

    fun onRecurrenceEditorToggle(enabled: Boolean) {
        updateRecurrenceEditor { state ->
            if (enabled) {
                val targetMode = if (state.mode == RecurrenceMode.NONE) {
                    RecurrenceMode.WEEKLY
                } else {
                    state.mode
                }
                state.copy(isRecurring = true, mode = targetMode)
            } else {
                state.copy(isRecurring = false, mode = RecurrenceMode.NONE, endEnabled = false)
            }
        }
    }

    fun onRecurrenceEditorModeSelected(mode: RecurrenceMode) {
        updateRecurrenceEditor { state ->
            state.copy(mode = mode, isRecurring = mode != RecurrenceMode.NONE || state.isRecurring)
        }
    }

    fun onRecurrenceEditorDayToggled(day: DayOfWeek) {
        updateRecurrenceEditor { state ->
            val updated = if (state.days.contains(day)) state.days - day else state.days + day
            state.copy(days = updated)
        }
    }

    fun onRecurrenceEditorIntervalChanged(value: Int) {
        updateRecurrenceEditor { state -> state.copy(interval = value.coerceAtLeast(0)) }
    }

    fun onRecurrenceEditorEndToggle(enabled: Boolean) {
        updateRecurrenceEditor { state -> state.copy(endEnabled = enabled) }
    }

    fun onRecurrenceEditorEndDateSelected(date: LocalDate) {
        updateRecurrenceEditor { state -> state.copy(endEnabled = true, endDate = date) }
    }

    fun onRecurrenceEditorConfirm() {
        val editor = _uiState.value.recurrenceEditor ?: return
        val recurrence = buildRecurrenceFromEditor(editor)
        _uiState.update { it.copy(recurrenceEditor = null) }
        onRecurrenceSelected(recurrence)
    }

    fun onRecurrenceEditorClear() {
        if (_uiState.value.recurrenceEditor == null) return
        _uiState.update { it.copy(recurrenceEditor = null) }
        onRecurrenceSelected(null)
    }

    fun onPaymentStatusSelected(status: PaymentStatus) {
        val lessonId = _uiState.value.lessonId ?: return
        if (_uiState.value.paymentStatus == status && status != PaymentStatus.PAID) {
            return
        }
        if (_uiState.value.isPaymentActionRunning) return
        _uiState.update { it.copy(isPaymentActionRunning = true, snackbarMessage = null) }
        viewModelScope.launch {
            val result = runCatching {
                when (status) {
                    PaymentStatus.PAID -> lessonsRepository.markPaid(lessonId)
                    PaymentStatus.DUE -> lessonsRepository.markDue(lessonId)
                    PaymentStatus.UNPAID -> lessonsRepository.resetPaymentStatus(lessonId)
                    PaymentStatus.CANCELLED -> lessonsRepository.markDue(lessonId)
                }
            }
            _uiState.update { state ->
                state.copy(
                    isPaymentActionRunning = false,
                    snackbarMessage = result.exceptionOrNull()?.let { LessonCardMessage.Error(it.message) }
                )
            }
        }
    }

    fun onNoteChanged(note: String) {
        val lessonId = _uiState.value.lessonId ?: return
        _uiState.update { it.copy(isSaving = true, snackbarMessage = null) }
        viewModelScope.launch {
            val normalized = note.take(LESSON_CARD_NOTE_LIMIT).trim().ifBlank { null }
            runCatching { lessonsRepository.saveNote(lessonId, normalized) }
                .onSuccess {
                    _uiState.update { state -> state.copy(isSaving = false) }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            snackbarMessage = LessonCardMessage.Error(error.message)
                        )
                    }
                }
        }
    }

    private fun updateRecurrenceEditor(
        transform: (LessonCardRecurrenceEditorState) -> LessonCardRecurrenceEditorState,
    ) {
        val current = _uiState.value.recurrenceEditor ?: return
        val updated = transform(current)
        val normalized = refreshRecurrenceEditor(updated)
        _uiState.update { it.copy(recurrenceEditor = normalized) }
    }

    private fun updateRecurrenceEditorBase(date: LocalDate, time: LocalTime) {
        updateRecurrenceEditor { it.copy(startDate = date, startTime = time) }
    }

    private fun buildRecurrenceEditorState(
        lesson: Lesson,
        state: LessonCardUiState,
    ): LessonCardRecurrenceEditorState {
        val recurrence = lesson.recurrence
        val zone = state.zoneId
        val mode = recurrence?.let { determineMode(it) } ?: RecurrenceMode.WEEKLY
        val interval = recurrence?.let { determineInterval(it, mode) } ?: 1
        val endDate = recurrence?.untilDateTime?.atZone(zone)?.toLocalDate()
        val base = LessonCardRecurrenceEditorState(
            mode = if (recurrence != null) mode else RecurrenceMode.WEEKLY,
            isRecurring = recurrence != null,
            interval = interval,
            days = recurrence?.daysOfWeek?.toSet() ?: emptySet(),
            endEnabled = endDate != null,
            endDate = endDate,
            label = null,
            startDate = state.date,
            startTime = state.time,
            zoneId = zone,
            locale = state.locale,
            canClear = lesson.seriesId != null || recurrence != null,
        )
        return refreshRecurrenceEditor(base)
    }

    private fun refreshRecurrenceEditor(state: LessonCardRecurrenceEditorState): LessonCardRecurrenceEditorState {
        val targetMode = if (state.isRecurring) state.mode else RecurrenceMode.NONE
        val baseDay = state.startDate.dayOfWeek
        val sanitizedInterval = state.interval.coerceAtLeast(1)
        val normalizedDays = when (targetMode) {
            RecurrenceMode.WEEKLY, RecurrenceMode.CUSTOM_WEEKS ->
                if (state.days.isEmpty()) setOf(baseDay) else state.days
            else -> emptySet()
        }
        val normalizedInterval = when (targetMode) {
            RecurrenceMode.CUSTOM_WEEKS -> maxOf(2, sanitizedInterval)
            RecurrenceMode.MONTHLY_BY_DOW -> sanitizedInterval
            RecurrenceMode.WEEKLY -> 1
            RecurrenceMode.NONE -> sanitizedInterval
        }
        val endEnabled = state.endEnabled && targetMode != RecurrenceMode.NONE && state.isRecurring
        val endDate = if (endEnabled) {
            val candidate = state.endDate ?: defaultRecurrenceEnd(state.startDate)
            if (candidate.isBefore(state.startDate)) state.startDate else candidate
        } else {
            null
        }
        val isRecurring = state.isRecurring && targetMode != RecurrenceMode.NONE
        val label = if (isRecurring) {
            val start = ZonedDateTime.of(state.startDate, state.startTime, state.zoneId)
            val frequency = when (targetMode) {
                RecurrenceMode.MONTHLY_BY_DOW -> RecurrenceFrequency.MONTHLY_BY_DOW
                RecurrenceMode.WEEKLY, RecurrenceMode.CUSTOM_WEEKS -> RecurrenceFrequency.WEEKLY
                RecurrenceMode.NONE -> RecurrenceFrequency.WEEKLY
            }
            val days = if (targetMode == RecurrenceMode.MONTHLY_BY_DOW) {
                emptyList()
            } else {
                normalizedDays.toList().sortedBy { it.value }
            }
            RecurrenceLabelFormatter.format(frequency, normalizedInterval, days, start)
        } else {
            null
        }
        val adjustedMode = if (state.isRecurring) targetMode else RecurrenceMode.NONE
        val adjustedInterval = when (adjustedMode) {
            RecurrenceMode.CUSTOM_WEEKS -> normalizedInterval
            RecurrenceMode.MONTHLY_BY_DOW -> normalizedInterval
            RecurrenceMode.WEEKLY -> 1
            RecurrenceMode.NONE -> normalizedInterval
        }
        return state.copy(
            mode = adjustedMode,
            isRecurring = isRecurring,
            interval = adjustedInterval,
            days = normalizedDays,
            endEnabled = endEnabled,
            endDate = endDate,
            label = label,
        )
    }

    private fun buildRecurrenceFromEditor(state: LessonCardRecurrenceEditorState): LessonRecurrence? {
        if (!state.isRecurring || state.mode == RecurrenceMode.NONE) return null
        val frequency = when (state.mode) {
            RecurrenceMode.MONTHLY_BY_DOW -> RecurrenceFrequency.MONTHLY_BY_DOW
            RecurrenceMode.WEEKLY, RecurrenceMode.CUSTOM_WEEKS -> RecurrenceFrequency.WEEKLY
            RecurrenceMode.NONE -> RecurrenceFrequency.WEEKLY
        }
        val interval = when (state.mode) {
            RecurrenceMode.CUSTOM_WEEKS -> state.interval.coerceAtLeast(1)
            RecurrenceMode.MONTHLY_BY_DOW -> state.interval.coerceAtLeast(1)
            RecurrenceMode.WEEKLY -> 1
            RecurrenceMode.NONE -> 1
        }
        val days = if (state.mode == RecurrenceMode.MONTHLY_BY_DOW) {
            emptyList()
        } else {
            state.days.toList().sortedBy { it.value }
        }
        val start = ZonedDateTime.of(state.startDate, state.startTime, state.zoneId).toInstant()
        val until = if (state.endEnabled) {
            val candidate = state.endDate ?: state.startDate
            val normalized = if (candidate.isBefore(state.startDate)) state.startDate else candidate
            ZonedDateTime.of(normalized, state.startTime, state.zoneId).toInstant()
        } else {
            null
        }
        return LessonRecurrence(
            frequency = frequency,
            interval = interval,
            daysOfWeek = days,
            startDateTime = start,
            untilDateTime = until,
            timezone = state.zoneId,
        )
    }

    private fun determineMode(recurrence: LessonRecurrence): RecurrenceMode {
        return when (recurrence.frequency) {
            RecurrenceFrequency.MONTHLY_BY_DOW -> RecurrenceMode.MONTHLY_BY_DOW
            RecurrenceFrequency.BIWEEKLY -> RecurrenceMode.CUSTOM_WEEKS
            RecurrenceFrequency.WEEKLY -> if (recurrence.interval <= 1) {
                RecurrenceMode.WEEKLY
            } else {
                RecurrenceMode.CUSTOM_WEEKS
            }
        }
    }

    private fun determineInterval(
        recurrence: LessonRecurrence,
        mode: RecurrenceMode,
    ): Int {
        return when (recurrence.frequency) {
            RecurrenceFrequency.BIWEEKLY -> maxOf(1, recurrence.interval) * 2
            RecurrenceFrequency.MONTHLY_BY_DOW -> maxOf(1, recurrence.interval)
            RecurrenceFrequency.WEEKLY -> when (mode) {
                RecurrenceMode.CUSTOM_WEEKS -> maxOf(1, recurrence.interval)
                RecurrenceMode.WEEKLY -> 1
                RecurrenceMode.MONTHLY_BY_DOW, RecurrenceMode.NONE -> maxOf(1, recurrence.interval)
            }
        }
    }

    private fun defaultRecurrenceEnd(startDate: LocalDate): LocalDate {
        val currentYear = startDate.year
        val thisYearEnd = LocalDate.of(currentYear, Month.JUNE, 30)
        return if (startDate.isAfter(thisYearEnd)) {
            LocalDate.of(currentYear + 1, Month.JUNE, 30)
        } else {
            thisYearEnd
        }
    }

    private fun submitUpdate(
        updatedLesson: Lesson,
        reducer: (LessonCardUiState) -> LessonCardUiState,
    ) {
        viewModelScope.launch {
            _uiState.update { reducer(it).copy(isSaving = true, snackbarMessage = null) }
            runCatching {
                val id = lessonsRepository.upsert(updatedLesson)
                val persisted = lessonsRepository.getById(id)
                persisted?.copy(recurrence = updatedLesson.recurrence) ?: updatedLesson.copy(id = id)
            }
                .onSuccess { persisted ->
                    currentLesson = persisted
                    _uiState.update { it.copy(isSaving = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            snackbarMessage = LessonCardMessage.Error(error.message)
                        )
                    }
                }
        }
    }

    private suspend fun loadStudents(selectedId: Long?) {
        val locale = _uiState.value.locale
        val options = runCatching { studentsRepository.allActive() }
            .getOrDefault(emptyList())
            .map { it.toOption() }
        val augmented = if (selectedId != null && options.none { it.id == selectedId }) {
            val selected = studentsRepository.getById(selectedId)?.toOption()
            if (selected != null) listOf(selected) + options else options
        } else {
            options
        }
        val sorted = augmented.sortedWith(compareBy<String> { it.lowercase(locale) }.let { comparator ->
            Comparator<LessonStudentOption> { a, b ->
                when {
                    selectedId != null && a.id == selectedId && b.id != selectedId -> -1
                    selectedId != null && b.id == selectedId && a.id != selectedId -> 1
                    else -> comparator.compare(a.name, b.name)
                }
            }
        })
        _uiState.update { it.copy(studentOptions = sorted) }
    }

    private fun reorderOptions(options: List<LessonStudentOption>, selectedId: Long?): List<LessonStudentOption> {
        if (selectedId == null) return options
        val selected = options.firstOrNull { it.id == selectedId } ?: return options
        val remaining = options.filterNot { it.id == selectedId }
        return listOf(selected) + remaining
    }

    private fun Student.toOption(): LessonStudentOption {
        return LessonStudentOption(
            id = id,
            name = titleCaseWords(name),
            grade = normalizeGrade(grade),
            subject = subject
                ?.takeIf { it.isNotBlank() }
                ?.trim()
                ?.let { normalizeSubject(it) },
        )
    }
}
