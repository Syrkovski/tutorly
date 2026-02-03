package com.tutorly.data.repo.memory

import com.tutorly.domain.model.LessonCreateRequest
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.model.LessonForToday
import com.tutorly.domain.model.LessonsRangeStats
import com.tutorly.domain.model.asIcon
import com.tutorly.domain.model.resolveDuration
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.models.Lesson
import com.tutorly.domain.recurrence.RecurrenceLabelFormatter
import com.tutorly.models.LessonRecurrence
import com.tutorly.models.Payment
import com.tutorly.models.PaymentStatus
import com.tutorly.models.LessonStatus
import com.tutorly.models.RecurrenceFrequency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class InMemoryLessonsRepository : LessonsRepository {
    private val seq = AtomicLong(1)
    private val recurrenceSeq = AtomicLong(1)
    private val store = ConcurrentHashMap<Long, Lesson>()
    private val lessonsFlow = MutableStateFlow<List<Lesson>>(emptyList())
    private val payments = ConcurrentHashMap<Long, Payment>()

    override fun observeLessons(from: Instant, to: Instant): Flow<List<LessonDetails>> =
        lessonsFlow.map { lessons ->
            val base = lessons.filter { it.startAt >= from && it.startAt < to }
                .map { it.toDetailsStub() }
            val generated = lessons.flatMap { lesson -> lesson.expandRecurrenceDetails(from, to) }
            (base + generated).sortedBy { it.startAt }
        }

    override fun observeTodayLessons(dayStart: Instant, dayEnd: Instant): Flow<List<LessonForToday>> =
        lessonsFlow.map { lessons ->
            val base = lessons.filter { it.startAt >= dayStart && it.startAt < dayEnd }
                .map { it.toTodayStub() }
            val generated = lessons.flatMap { lesson -> lesson.expandRecurrenceToday(dayStart, dayEnd) }
            (base + generated).sortedBy { it.startAt }
        }

    override fun observeOutstandingLessons(before: Instant): Flow<List<LessonForToday>> =
        lessonsFlow.map { lessons ->
            val base = lessons.filter { it.startAt < before && it.paymentStatus in PaymentStatus.outstandingStatuses }
                .map { it.toTodayStub() }
            val generated = lessons
                .filter { it.paymentStatus in PaymentStatus.outstandingStatuses }
                .flatMap { lesson -> lesson.expandRecurrenceToday(Instant.EPOCH, before) }
            (base + generated).sortedBy { it.startAt }
        }

    override fun observeOutstandingLessonDetails(before: Instant): Flow<List<LessonDetails>> =
        lessonsFlow.map { lessons ->
            val base = lessons.filter { it.startAt < before && it.paymentStatus in PaymentStatus.outstandingStatuses }
                .map { it.toDetailsStub() }
            val generated = lessons
                .filter { it.paymentStatus in PaymentStatus.outstandingStatuses }
                .flatMap { lesson -> lesson.expandRecurrenceDetails(Instant.EPOCH, before) }
            (base + generated).sortedBy { it.startAt }
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

    override suspend fun getById(id: Long): Lesson? = store[id]

    override suspend fun upsert(lesson: Lesson): Long {
        val id = if (lesson.id == 0L) seq.getAndIncrement() else lesson.id
        val existing = store[id]
        val updated = when {
            lesson.recurrence != null -> {
                val seriesId = lesson.seriesId ?: existing?.seriesId ?: recurrenceSeq.getAndIncrement()
                lesson.copy(id = id, seriesId = seriesId)
            }

            existing?.seriesId != null && lesson.seriesId == null -> {
                lesson.copy(id = id, recurrence = null)
            }

            else -> {
                val recurrence = existing?.recurrence
                lesson.copy(id = id, recurrence = recurrence, seriesId = lesson.seriesId ?: existing?.seriesId)
            }
        }
        store[id] = updated
        emit()
        return id
    }

    override suspend fun create(request: LessonCreateRequest): Long {
        val id = seq.getAndIncrement()
        val now = Instant.now()
        val recurrence = request.recurrence?.let { rule ->
            LessonRecurrence(
                frequency = rule.frequency,
                interval = rule.interval,
                daysOfWeek = rule.daysOfWeek,
                startDateTime = request.startAt,
                untilDateTime = rule.until,
                timezone = rule.timezone
            )
        }
        val seriesId = recurrence?.let { recurrenceSeq.getAndIncrement() }
        val newLesson = Lesson(
            id = id,
            studentId = request.studentId,
            subjectId = request.subjectId,
            title = request.title,
            startAt = request.startAt,
            endAt = request.endAt,
            priceCents = request.priceCents,
            paidCents = 0,
            paymentStatus = PaymentStatus.UNPAID,
            markedAt = null,
            status = LessonStatus.PLANNED,
            note = request.note,
            createdAt = now,
            updatedAt = now,
            seriesId = seriesId,
            isInstance = false,
            recurrence = recurrence
        )
        store[id] = newLesson
        emit()
        return id
    }

    override suspend fun moveLesson(lessonId: Long, newStart: Instant, newEnd: Instant) {
        store[lessonId]?.let { lesson ->
            store[lessonId] = lesson.copy(
                startAt = newStart,
                endAt = newEnd,
                updatedAt = Instant.now()
            )
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
        store.remove(id)
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
            )
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
            )
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
            store[id] = it.copy(note = note)
            emit()
        }
    }

    override suspend fun resetPaymentStatus(id: Long) {
        store[id]?.let { lesson ->
            store[id] = lesson.copy(
                paymentStatus = PaymentStatus.UNPAID,
                paidCents = 0,
                markedAt = null
            )
            payments.remove(id)
            emit()
        }
    }

    private fun emit() {
        lessonsFlow.value = store.values.sortedByDescending { it.startAt }
    }
}

private fun Lesson.expandRecurrenceDetails(rangeStart: Instant, rangeEnd: Instant): List<LessonDetails> {
    val recurrence = recurrence ?: return emptyList()
    if (rangeStart >= rangeEnd) return emptyList()
    val occurrences = expandRecurrence(rangeStart, rangeEnd, recurrence)
    if (occurrences.isEmpty()) return emptyList()
    val base = toDetailsStub()
    val duration = base.duration
    val seriesId = seriesId ?: id
    return occurrences.map { occurrence ->
        base.copy(
            id = syntheticId(seriesId, occurrence),
            baseLessonId = id,
            startAt = occurrence,
            endAt = occurrence.plus(duration),
            duration = duration,
            paidCents = 0,
            isRecurring = true,
            seriesId = seriesId,
            originalStartAt = occurrence,
            recurrenceLabel = base.recurrenceLabel
        )
    }
}

private fun Lesson.expandRecurrenceToday(rangeStart: Instant, rangeEnd: Instant): List<LessonForToday> {
    val recurrence = recurrence ?: return emptyList()
    if (rangeStart >= rangeEnd) return emptyList()
    val occurrences = expandRecurrence(rangeStart, rangeEnd, recurrence)
    if (occurrences.isEmpty()) return emptyList()
    val base = toTodayStub()
    val duration = base.duration
    val seriesId = seriesId ?: id
    return occurrences.map { occurrence ->
        base.copy(
            id = syntheticId(seriesId, occurrence),
            baseLessonId = id,
            startAt = occurrence,
            endAt = occurrence.plus(duration),
            duration = duration,
            paidCents = 0,
            isRecurring = true,
            seriesId = seriesId,
            originalStartAt = occurrence,
            recurrenceLabel = base.recurrenceLabel
        )
    }
}

private fun expandRecurrence(
    rangeStart: Instant,
    rangeEnd: Instant,
    recurrence: LessonRecurrence
): List<Instant> {
    val zone = recurrence.timezone
    val base = recurrence.startDateTime.atZone(zone)
    val effectiveEnd = recurrence.untilDateTime?.takeIf { it < rangeEnd } ?: rangeEnd
    if (effectiveEnd <= recurrence.startDateTime) return emptyList()
    val windowStart = rangeStart.atZone(zone)
    val windowEnd = effectiveEnd.atZone(zone)
    val results = mutableSetOf<Instant>()

    when (recurrence.frequency) {
        RecurrenceFrequency.MONTHLY_BY_DOW -> {
            val monthsStep = maxOf(1, recurrence.interval)
            val ordinal = ((base.dayOfMonth - 1) / 7) + 1
            var monthOffset = 0
            while (true) {
                val candidateMonthStart = base.withDayOfMonth(1).plusMonths(monthOffset.toLong() * monthsStep)
                var candidate = candidateMonthStart.with(
                    TemporalAdjusters.dayOfWeekInMonth(ordinal, base.dayOfWeek)
                )
                candidate = candidate.withHour(base.hour)
                    .withMinute(base.minute)
                    .withSecond(base.second)
                    .withNano(base.nano)

                val instant = candidate.toInstant()
                if (instant > windowEnd.toInstant()) break
                if (candidate.isBefore(base)) {
                    monthOffset += 1
                    continue
                }
                if (instant != recurrence.startDateTime && !candidate.isBefore(windowStart) && instant < rangeEnd) {
                    results += instant
                }
                monthOffset += 1
            }
        }

        else -> {
            val intervalWeeks = when (recurrence.frequency) {
                RecurrenceFrequency.WEEKLY -> maxOf(1, recurrence.interval)
                RecurrenceFrequency.BIWEEKLY -> maxOf(1, recurrence.interval) * 2
                RecurrenceFrequency.MONTHLY_BY_DOW -> 1
            }
            val targetDays = if (recurrence.daysOfWeek.isEmpty()) {
                listOf(base.dayOfWeek)
            } else {
                recurrence.daysOfWeek.sortedBy { it.value }
            }
            for (day in targetDays) {
                var occurrence = base.with(TemporalAdjusters.nextOrSame(day))
                if (occurrence.isBefore(base)) {
                    occurrence = occurrence.plusWeeks(intervalWeeks.toLong())
                }
                while (true) {
                    val instant = occurrence.toInstant()
                    if (instant > windowEnd.toInstant()) break
                    if (instant != recurrence.startDateTime) {
                        if (!occurrence.isBefore(windowStart) && instant < rangeEnd) {
                            results += instant
                        }
                    }
                    occurrence = occurrence.plusWeeks(intervalWeeks.toLong())
                }
            }
        }
    }

    return results.toList().sorted()
}

private fun syntheticId(seriesId: Long, start: Instant): Long {
    val combined = (seriesId shl 1) xor start.toEpochMilli()
    val positive = combined and Long.MAX_VALUE
    return -(positive + 1)
}

private fun Lesson.toDetailsStub(): LessonDetails {
    val duration = resolveDuration(startAt, endAt, null)
    val normalizedEnd = startAt.plus(duration)
    val recurrenceLabel = recurrence?.let { rule ->
        val start = rule.startDateTime.atZone(rule.timezone)
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
        recurrenceLabel = recurrenceLabel
    )
}

private fun Lesson.toTodayStub(): LessonForToday {
    val duration = resolveDuration(startAt, endAt, null)
    val normalizedEnd = startAt.plus(duration)
    val recurrenceLabel = recurrence?.let { rule ->
        val start = rule.startDateTime.atZone(rule.timezone)
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
        recurrenceLabel = recurrenceLabel,
        paidCents = TODO(),
    )
}
