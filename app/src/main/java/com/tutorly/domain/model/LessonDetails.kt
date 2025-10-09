package com.tutorly.domain.model

import com.tutorly.models.Lesson
import com.tutorly.models.Payment
import com.tutorly.models.Student
import com.tutorly.models.SubjectPreset

/**
 * Aggregated lesson payload that mirrors the data required for scheduling UIs.
 */
data class LessonDetails(
    val lesson: Lesson,
    val student: Student,
    val subject: SubjectPreset?,
    val payments: List<Payment>
)
