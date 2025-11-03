package com.tutorly.data.repo.memory

import com.tutorly.domain.model.LessonCreateRequest
import com.tutorly.domain.model.RecurrenceCreateRequest
import com.tutorly.models.RecurrenceFrequency
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class InMemoryLessonsRepositoryTest {

    private val repository = InMemoryLessonsRepository()

    @Test
    fun `create preserves recurrence metadata`() = runBlocking {
        val request = recurringLessonRequest()

        val id = repository.create(request)
        val stored = repository.getById(id)
        val recurrence = stored?.recurrence

        assertNotNull(stored?.seriesId, "Series identifier should be assigned")
        assertNotNull(recurrence, "Recurrence metadata should be preserved")
        assertEquals(RecurrenceFrequency.WEEKLY, recurrence.frequency)
        assertEquals(listOf(DayOfWeek.MONDAY), recurrence.daysOfWeek)
        assertEquals(ZoneId.of("Europe/Moscow"), recurrence.timezone)
    }

    @Test
    fun `lesson details stream exposes recurrence info`() = runBlocking {
        val request = recurringLessonRequest()
        val id = repository.create(request)

        val details = repository.observeLessonDetails(id).first { it != null }!!

        assertNotNull(details.seriesId, "Series identifier should surface in lesson details")
        assertEquals(true, details.isRecurring, "Lesson details should mark the lesson as recurring")
        assertNotNull(details.recurrenceLabel, "Recurrence label should be available for UI rendering")
    }

    @Test
    fun `student lessons stream keeps recurrence metadata`() = runBlocking {
        val request = recurringLessonRequest()
        repository.create(request)

        val lessons = repository.observeByStudent(request.studentId).first { it.isNotEmpty() }
        val lesson = lessons.single()

        assertNotNull(lesson.seriesId, "Series identifier should be populated in student stream")
        assertNotNull(lesson.recurrence, "Recurrence metadata should be available in student stream")
    }

    private fun recurringLessonRequest(): LessonCreateRequest {
        val start = Instant.parse("2024-04-01T10:00:00Z")
        return LessonCreateRequest(
            studentId = 1L,
            subjectId = 2L,
            title = "Алгебра",
            startAt = start,
            endAt = start.plusSeconds(3_600),
            priceCents = 1_500,
            note = "Test",
            recurrence = RecurrenceCreateRequest(
                frequency = RecurrenceFrequency.WEEKLY,
                interval = 1,
                daysOfWeek = listOf(DayOfWeek.MONDAY),
                until = start.plusSeconds(7L * 24L * 3_600L),
                timezone = ZoneId.of("Europe/Moscow")
            )
        )
    }
}
