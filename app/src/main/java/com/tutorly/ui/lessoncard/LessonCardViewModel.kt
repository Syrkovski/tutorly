package com.tutorly.ui.lessoncard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.domain.repo.UserSettingsRepository
import com.tutorly.models.Lesson
import com.tutorly.models.PaymentStatus
import com.tutorly.models.Student
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
                            studentGrade = details.studentGrade,
                            subjectName = details.subjectName,
                            date = start.toLocalDate(),
                            time = start.toLocalTime(),
                            durationMinutes = details.duration.toMinutes().toInt(),
                            priceCents = details.priceCents,
                            paymentStatus = details.paymentStatus,
                            paidCents = details.paidCents,
                            note = details.lessonNote,
                            studentOptions = reorderOptions(state.studentOptions, details.studentId),
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
                studentGrade = selected?.grade ?: state.studentGrade,
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

    private fun submitUpdate(
        updatedLesson: Lesson,
        reducer: (LessonCardUiState) -> LessonCardUiState,
    ) {
        viewModelScope.launch {
            _uiState.update { reducer(it).copy(isSaving = true, snackbarMessage = null) }
            runCatching { lessonsRepository.upsert(updatedLesson) }
                .onSuccess {
                    currentLesson = updatedLesson
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
            name = name,
            grade = grade?.takeIf { it.isNotBlank() }?.trim(),
            subject = subject?.takeIf { it.isNotBlank() }?.trim(),
        )
    }
}
