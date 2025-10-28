package com.tutorly.domain.model

import com.tutorly.models.RecurrenceFrequency
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

data class RecurrenceCreateRequest(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val daysOfWeek: List<DayOfWeek> = emptyList(),
    val until: Instant?,
    val timezone: ZoneId
)
