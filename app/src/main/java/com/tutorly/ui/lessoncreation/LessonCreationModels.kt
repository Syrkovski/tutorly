package com.tutorly.ui.lessoncreation

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

data class LessonCreationUiState(
    val isVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val studentQuery: String = "",
    val students: List<StudentOption> = emptyList(),
    val selectedStudent: StudentOption? = null,
    val subjects: List<SubjectOption> = emptyList(),
    val availableSubjects: List<SubjectOption> = emptyList(),
    val selectedSubjectId: Long? = null,
    val selectedSubjectChips: List<SelectedSubjectChip> = emptyList(),
    val subjectInput: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val durationMinutes: Int = 60,
    val priceCents: Int = 0,
    val pricePresets: List<Int> = emptyList(),
    val note: String = "",
    val currencySymbol: String = "₽",
    val slotStepMinutes: Int = 30,
    val errors: Map<LessonCreationField, String> = emptyMap(),
    val snackbarMessage: String? = null,
    val showConflictDialog: ConflictInfo? = null,
    val origin: LessonCreationOrigin = LessonCreationOrigin.CALENDAR,
    val locale: Locale = Locale.getDefault(),
    val zoneId: ZoneId = ZoneId.systemDefault()
)

data class LessonCreationConfig(
    val start: ZonedDateTime? = null,
    val duration: Duration? = null,
    val studentId: Long? = null,
    val subjectId: Long? = null,
    val note: String? = null,
    val zoneId: ZoneId? = null,
    val origin: LessonCreationOrigin = LessonCreationOrigin.CALENDAR
)

enum class LessonCreationField {
    STUDENT,
    TIME,
    DURATION,
    PRICE
}

enum class LessonCreationOrigin {
    CALENDAR,
    STUDENT
}

data class SubjectOption(
    val id: Long,
    val name: String,
    val colorArgb: Int,
    val durationMinutes: Int,
    val defaultPriceCents: Int
)

data class SelectedSubjectChip(
    val id: Long?,
    val name: String,
    val colorArgb: Int?
)

data class StudentOption(
    val id: Long,
    val name: String,
    val rateCents: Int?,
    val subjects: List<String> = emptyList()
)

data class ConflictInfo(
    val conflicts: List<ConflictLesson>,
    val start: ZonedDateTime,
    val end: ZonedDateTime
)

data class ConflictLesson(
    val id: Long,
    val studentName: String,
    val start: ZonedDateTime,
    val end: ZonedDateTime
)

sealed interface LessonCreationEvent {
    data class Created(
        val start: ZonedDateTime,
        val studentId: Long
    ) : LessonCreationEvent
}
