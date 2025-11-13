package com.tutorly.data.repo.memory

import com.tutorly.domain.model.LessonCreateRequest
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.model.LessonForToday
import com.tutorly.domain.model.LessonsRangeStats
import com.tutorly.domain.model.RecurrenceCreateRequest
import com.tutorly.domain.model.asIcon
import com.tutorly.domain.model.resolveDuration
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.models.Lesson
import com.tutorly.models.LessonRecurrence
import com.tutorly.domain.recurrence.RecurrenceLabelFormatter
import com.tutorly.models.Payment
import com.tutorly.models.PaymentStatus
import com.tutorly.models.LessonStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class InMemoryLessonsRepository : LessonsRepository {
    private val seq = AtomicLong(1)
    private val recurrenceSeq = AtomicLong(1)
    private val store = ConcurrentHashMap<Long, Lesson>()
    private val lessonsFlow = MutableStateFlow<List<Lesson>>(emptyList())
    private val payments = ConcurrentHashMap<Long, Payment>()
    private val recurrenceRules = ConcurrentHashMap<Long, LessonRecurrence>()

    override fun observeLessons(from: Instant, to: Instant): Flow<List<LessonDetails>> =
        lessonsFlow.map { lessons ->
            lessons.filter { it.startAt >= from && it.startAt < to }
                .sortedBy { it.startAt }
                .map { it.toDetailsStub() }
        }

    override fun observeTodayLessons(dayStart: Instant, dayEnd: Instant): Flow<List<LessonForToday>> =
        lessonsFlow.map { lessons ->
            lessons.filter { it.startAt >= dayStart && it.startAt < dayEnd }
                .sortedBy { it.startAt }
                .map { it.toTodayStub() }
        }

    override fun observeOutstandingLessons(before: Instant): Flow<List<LessonForToday>> =
        lessonsFlow.map { lessons ->
            lessons.filter { it.startAt < before && it.paymentStatus in PaymentStatus.outstandingStatuses }
                .sortedBy { it.startAt }
                .map { it.toTodayStub() }
        }

    override fun observeOutstandingLessonDetails(before: Instant): Flow<List<LessonDetails>> =
        lessonsFlow.map { lessons ->
            lessons.filter { it.startAt < before && it.paymentStatus in PaymentStatus.outstandingStatuses }
                .sortedBy { it.startAt }
                .map { it.toDetailsStub() }
        }

    override fun observeWeekStats(from: Instant, to: Instant): Flow<LessonsRangeStats> =
        lessonsFlow.map { lessons ->
            val relevant = lessons.filter { it.startAt >= from && it.startAt < to }
            val paidStatus = PaymentStatus.PAID
            val outstanding = PaymentStatus.outstandingStatuses.toSet()
            LessonsRangeStats(
                totalLessons = relevant.size,
                paidLessons = relevant.count { it.paymentStatus == paidStatus },
                debtLessons = relevant.count { it.paymentStatus in outstanding },
                earnedCents = relevant
                    .filter { it.paymentStatus == paidStatus }
                    .sumOf { it.paidCents.toLong() }
            )
        }

    override fun observeLessonDetails(id: Long): Flow<LessonDetails?> =
        lessonsFlow.map { lessons -> lessons.firstOrNull { it.id == id }?.toDetailsStub() }

    override fun observeByStudent(studentId: Long): Flow<List<Lesson>> =
        lessonsFlow.map { lessons -> lessons.filter { it.studentId == studentId } }

    override suspend fun getById(id: Long): Lesson? = store[id]?.withResolvedRecurrence()

    override suspend fun upsert(lesson: Lesson): Long {
        val id = if (lesson.id == 0L) seq.getAndIncrement() else lesson.id
        val existing = store[id]?.withResolvedRecurrence()
        val updated = when {
            lesson.recurrence != null -> {
                val seriesId = lesson.seriesId ?: existing?.seriesId ?: recurrenceSeq.getAndIncrement()
                recurrenceRules[seriesId] = lesson.recurrence
                lesson.copy(id = id, seriesId = seriesId)
            }

            existing?.seriesId != null && lesson.seriesId == null -> {
                existing.seriesId?.let(recurrenceRules::remove)
                lesson.copy(id = id, recurrence = null, seriesId = null)
            }

            else -> {
                val seriesId = lesson.seriesId ?: existing?.seriesId
                val recurrence = lesson.recurrence
                    ?: seriesId?.let(recurrenceRules::get)
                    ?: existing?.recurrence
                lesson.copy(id = id, recurrence = recurrence, seriesId = seriesId)
            }
        }.withResolvedRecurrence()
        store[id] = updated
        emit()
        return id
    }

    override suspend fun create(request: LessonCreateRequest): Long {
        val recurrence = request.recurrence?.toLessonRecurrence(request.startAt)
        val lesson = Lesson(
            studentId = request.studentId,
            subjectId = request.subjectId,
            title = request.title,
            startAt = request.startAt,
            endAt = request.endAt,
            priceCents = request.priceCents,
            note = request.note,
            recurrence = recurrence
        )
        return upsert(lesson)
    }

    override suspend fun moveLesson(lessonId: Long, newStart: Instant, newEnd: Instant) {
        store[lessonId]?.let { lesson ->
            store[lessonId] = lesson.copy(
                startAt = newStart,
                endAt = newEnd,
                updatedAt = Instant.now()
            ).withResolvedRecurrence()
            emit()
        }
    }

    override suspend fun moveRecurringOccurrence(
        seriesId: Long,
        originalStart: Instant,
        newStart: Instant,
        duration: Duration
    ) {
        // Recurrence support is not implemented in the in-memory repository used for tests.
    }

    override suspend fun clearRecurringOverride(seriesId: Long, originalStart: Instant) {
        // Recurrence support is not implemented in the in-memory repository used for tests.
    }

    override suspend fun findConflicts(start: Instant, end: Instant): List<LessonDetails> {
        return store.values
            .filter { lesson -> lesson.startAt < end && lesson.endAt > start }
            .sortedBy { it.startAt }
            .map { it.toDetailsStub() }
    }

    override suspend fun latestLessonForStudent(studentId: Long): Lesson? {
        return store.values
            .filter { it.studentId == studentId }
            .maxByOrNull { it.startAt }
    }

    override suspend fun delete(id: Long) {
        val removed = store.remove(id)
        removed?.seriesId?.let { seriesId ->
            if (removed.isInstance.not()) {
                recurrenceRules.remove(seriesId)
            }
        }
        payments.remove(id)
        emit()
    }

    override suspend fun markPaid(id: Long) {
        store[id]?.let { lesson ->
            val now = Instant.now()
            val updatedLesson = lesson.copy(
                paymentStatus = PaymentStatus.PAID,
                paidCents = lesson.priceCents,
                markedAt = now
            ).withResolvedRecurrence()
            store[id] = updatedLesson
            val payment = payments[id]
            payments[id] = (payment ?: Payment(
                id = id,
                lessonId = updatedLesson.id,
                studentId = updatedLesson.studentId,
                amountCents = updatedLesson.priceCents,
                status = PaymentStatus.PAID,
                at = now
            )).copy(
                amountCents = updatedLesson.priceCents,
                status = PaymentStatus.PAID,
                at = now
            )
            emit()
        }
    }

    override suspend fun markDue(id: Long) {
        store[id]?.let { lesson ->
            val now = Instant.now()
            val updatedLesson = lesson.copy(
                paymentStatus = PaymentStatus.DUE,
                paidCents = 0,
                markedAt = now
            ).withResolvedRecurrence()
            store[id] = updatedLesson
            val payment = payments[id]
            payments[id] = (payment ?: Payment(
                id = id,
                lessonId = updatedLesson.id,
                studentId = updatedLesson.studentId,
                amountCents = updatedLesson.priceCents,
                status = PaymentStatus.DUE,
                at = now
            )).copy(
                amountCents = updatedLesson.priceCents,
                status = PaymentStatus.DUE,
                at = now
            )
            emit()
        }
    }

    override suspend fun saveNote(id: Long, note: String?) {
        store[id]?.let {
            store[id] = it.copy(note = note).withResolvedRecurrence()
            emit()
        }
    }

    override suspend fun resetPaymentStatus(id: Long) {
        store[id]?.let { lesson ->
            store[id] = lesson.copy(
                paymentStatus = PaymentStatus.UNPAID,
                paidCents = 0,
                markedAt = null
            ).withResolvedRecurrence()
            payments.remove(id)
            emit()
        }
    }

    private fun Lesson.withResolvedRecurrence(): Lesson {
        val resolved = recurrence ?: seriesId?.let(recurrenceRules::get)
        return if (resolved === recurrence || resolved == recurrence) this else copy(recurrence = resolved)
    }

    private fun emit() {
        lessonsFlow.value = store.values
            .map { it.withResolvedRecurrence() }
            .sortedByDescending { it.startAt }
    }
}

private fun RecurrenceCreateRequest.toLessonRecurrence(start: Instant): LessonRecurrence {
    return LessonRecurrence(
        frequency = frequency,
        interval = interval,
        daysOfWeek = daysOfWeek,
        startDateTime = start,
        untilDateTime = until,
        timezone = timezone
    )
}

private fun Lesson.toDetailsStub(): LessonDetails {
    val duration = resolveDuration(startAt, endAt, null)
    val normalizedEnd = startAt.plus(duration)
    val recurrenceLabel = recurrence?.let { rule ->
        val zone = rule.timezone
        val start = rule.startDateTime.atZone(zone)
        RecurrenceLabelFormatter.format(rule.frequency, rule.interval, rule.daysOfWeek, start)
    }

    return LessonDetails(
        id = id,
        baseLessonId = id,
        studentId = studentId,
        startAt = startAt,
        endAt = normalizedEnd,
        duration = duration,
        studentName = "Student #$studentId",
        studentNote = null,
        subjectName = null,
        studentGrade = null,
        subjectColorArgb = null,
        paymentStatus = paymentStatus,
        paymentStatusIcon = paymentStatus.asIcon(),
        lessonStatus = status,
        priceCents = priceCents,
        paidCents = paidCents,
        lessonTitle = title,
        lessonNote = note,
        isRecurring = seriesId != null,
        seriesId = seriesId,
        originalStartAt = startAt,
        recurrenceLabel = recurrenceLabel,
        recurrence = recurrence
    )
}

private fun Lesson.toTodayStub(): LessonForToday {
    val duration = resolveDuration(startAt, endAt, null)
    val normalizedEnd = startAt.plus(duration)
    val recurrenceLabel = recurrence?.let { rule ->
        val zone = rule.timezone
        val start = rule.startDateTime.atZone(zone)
        RecurrenceLabelFormatter.format(rule.frequency, rule.interval, rule.daysOfWeek, start)
    }

    return LessonForToday(
        id = id,
        baseLessonId = id,
        studentId = studentId,
        studentName = "Student #$studentId",
        studentGrade = null,
        subjectName = null,
        lessonTitle = title,
        startAt = startAt,
        endAt = normalizedEnd,
        duration = duration,
        priceCents = priceCents,
        studentRateCents = null,
        note = note,
        paymentStatus = paymentStatus,
        lessonStatus = status,
        markedAt = markedAt,
        isRecurring = seriesId != null,
        seriesId = seriesId,
        originalStartAt = startAt,
        recurrenceLabel = recurrenceLabel
    )
}
