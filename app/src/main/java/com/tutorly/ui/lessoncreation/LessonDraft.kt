package com.tutorly.ui.lessoncreation

import com.tutorly.models.RecurrenceFrequency
import kotlinx.serialization.Serializable

@Serializable
data class LessonDraft(
    val isVisible: Boolean = false,
    val studentQuery: String = "",
    val selectedStudentId: Long? = null,
    val studentGrade: String = "",
    val selectedSubjectId: Long? = null,
    val selectedSubjectChips: List<SelectedSubjectChipDraft> = emptyList(),
    val subjectInput: String = "",
    val dateEpochDay: Long,
    val timeMinutes: Int,
    val durationMinutes: Int = 60,
    val priceCents: Int = 0,
    val note: String = "",
    val origin: LessonCreationOrigin = LessonCreationOrigin.CALENDAR,
    val zoneId: String,
    val recurrenceMode: RecurrenceMode = RecurrenceMode.NONE,
    val recurrenceInterval: Int = 1,
    val recurrenceDays: Set<Int> = emptySet(),
    val recurrenceEndEnabled: Boolean = false,
    val recurrenceEndDateEpochDay: Long? = null,
    val repeat: RepeatRule? = null
)

@Serializable
data class RepeatRule(
    val frequency: RecurrenceFrequency,
    val interval: Int,
    val daysOfWeek: Set<Int>,
    val untilEpochDay: Long?
)

@Serializable
data class SelectedSubjectChipDraft(
    val id: Long?,
    val name: String,
    val colorArgb: Int?
)
