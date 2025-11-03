package com.tutorly.data.db.converters

import com.tutorly.models.LessonRecurrence
import com.tutorly.models.RecurrenceFrequency
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecurrenceConvertersTest {

    private val converters = RecurrenceConverters()

    @Test
    fun `round trip preserves recurrence fields`() {
        val recurrence = LessonRecurrence(
            frequency = RecurrenceFrequency.WEEKLY,
            interval = 2,
            daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            startDateTime = Instant.parse("2024-01-10T12:00:00Z"),
            untilDateTime = Instant.parse("2024-02-10T12:00:00Z"),
            timezone = ZoneId.of("Europe/Moscow")
        )

        val encoded = converters.fromLessonRecurrence(recurrence)
        val decoded = converters.toLessonRecurrence(encoded)

        assertEquals(recurrence, decoded)
    }

    @Test
    fun `null values remain null`() {
        assertNull(converters.fromLessonRecurrence(null))
        assertNull(converters.toLessonRecurrence(null))
        assertNull(converters.toLessonRecurrence(""))
    }
}
