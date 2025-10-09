package com.tutorly.domain.model

import java.time.Instant

/**
 * Payload used when composing a brand new lesson from the creation sheet.
 */
data class LessonCreateRequest(
    val studentId: Long,
    val subjectId: Long?,
    val title: String?,
    val startAt: Instant,
    val endAt: Instant,
    val priceCents: Int,
    val note: String?
)
