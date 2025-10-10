package com.tutorly.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.domain.repo.UserSettingsRepository
import com.tutorly.models.Lesson
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LessonDetailsViewModel @Inject constructor(
    private val lessonsRepository: LessonsRepository,
    private val studentsRepository: StudentsRepository,
    private val userSettingsRepository: UserSettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val lessonId: Long = savedStateHandle.get<Long>("lessonId")
        ?: error("lessonId is required")

    private val _uiState = MutableStateFlow(LessonDetailsUiState(lessonId = lessonId))
    val uiState: StateFlow<LessonDetailsUiState> = _uiState.asStateFlow()

    private var currentLesson: Lesson? = null

    init {
        viewModelScope.launch {
            val settings = userSettingsRepository.get()
            val locale = Locale(settings.locale)
            val currencySymbol = runCatching { Currency.getInstance(settings.currency).getSymbol(locale) }
                .getOrDefault(settings.currency)
            _uiState.update {
                it.copy(
                    currencySymbol = currencySymbol,
                    currencyCode = settings.currency,
                    locale = locale,
                    zoneId = ZoneId.systemDefault()
                )
            }
        }

        viewModelScope.launch { loadStudents(null) }
        observeLesson()
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun onStudentSelected(studentId: Long) {
        val lesson = currentLesson ?: return
        if (lesson.studentId == studentId) return
        val selected = uiState.value.studentOptions.firstOrNull { it.id == studentId }
        submitUpdate(
            lesson.copy(studentId = studentId, updatedAt = Instant.now())
        ) { state ->
            state.copy(
                studentId = studentId,
                studentName = selected?.name ?: state.studentName,
                studentGrade = selected?.grade ?: state.studentGrade,
                studentOptions = reorderOptions(state.studentOptions, studentId)
            )
        }
    }

    fun onDateSelected(date: LocalDate) {
        val lesson = currentLesson ?: return
        val state = _uiState.value
        val zone = state.zoneId
        val duration = state.durationMinutes
        val start = ZonedDateTime.of(date, state.time, zone)
        val end = start.plusMinutes(duration.toLong())
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

    private fun observeLesson() {
        viewModelScope.launch {
            lessonsRepository.observeLessonDetails(lessonId).collect { details ->
                if (details == null) {
                    _uiState.update { it.copy(isLoading = false, isNotFound = true) }
                } else {
                    currentLesson = lessonsRepository.getById(details.id)
                    val zone = _uiState.value.zoneId
                    val start = details.startAt.atZone(zone)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isNotFound = false,
                            studentId = details.studentId,
                            studentName = details.studentName,
                            studentGrade = details.studentGrade,
                            subjectName = details.subjectName,
                            date = start.toLocalDate(),
                            time = start.toLocalTime(),
                            durationMinutes = details.duration.toMinutes().toInt(),
                            priceCents = details.priceCents,
                            studentOptions = reorderOptions(it.studentOptions, details.studentId)
                        )
                    }
                    loadStudents(details.studentId)
                }
            }
        }
    }

    private fun submitUpdate(updatedLesson: Lesson, onState: (LessonDetailsUiState) -> LessonDetailsUiState) {
        viewModelScope.launch {
            _uiState.update { onState(it).copy(isSaving = true, snackbarMessage = null) }
            runCatching { lessonsRepository.upsert(updatedLesson) }
                .onSuccess {
                    currentLesson = updatedLesson
                    _uiState.update { it.copy(isSaving = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            snackbarMessage = error.message ?: "Не удалось сохранить изменения"
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
            subject = subject?.takeIf { it.isNotBlank() }?.trim()
        )
    }
}

data class LessonDetailsUiState(
    val lessonId: Long = 0L,
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val studentId: Long? = null,
    val studentName: String = "",
    val studentGrade: String? = null,
    val subjectName: String? = null,
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val durationMinutes: Int = 60,
    val priceCents: Int = 0,
    val currencySymbol: String = "₽",
    val currencyCode: String = "RUB",
    val studentOptions: List<LessonStudentOption> = emptyList(),
    val isSaving: Boolean = false,
    val snackbarMessage: String? = null,
    val locale: Locale = Locale.getDefault(),
    val zoneId: ZoneId = ZoneId.systemDefault()
)

data class LessonStudentOption(
    val id: Long,
    val name: String,
    val grade: String?,
    val subject: String?
)
