package com.tutorly.data.repo.room

import com.tutorly.data.db.dao.LessonCountTuple
import com.tutorly.data.db.dao.LessonDao
import com.tutorly.data.db.dao.PaymentDao
import com.tutorly.data.db.dao.RecurrenceExceptionDao
import com.tutorly.data.db.dao.RecurrenceRuleDao
import com.tutorly.data.db.projections.toLessonDetails
import com.tutorly.data.db.projections.toLessonForToday
import com.tutorly.domain.model.LessonCreateRequest
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.model.LessonForToday
import com.tutorly.domain.model.LessonsRangeStats
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.models.Lesson
import com.tutorly.models.LessonStatus
import com.tutorly.models.Payment
import com.tutorly.models.PaymentStatus
import com.tutorly.models.RecurrenceException
import com.tutorly.models.RecurrenceExceptionType
import com.tutorly.models.RecurrenceFrequency
import com.tutorly.models.RecurrenceRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class RoomLessonsRepository(
    private val lessonDao: LessonDao,
    private val paymentDao: PaymentDao,
    private val recurrenceRuleDao: RecurrenceRuleDao,
    private val recurrenceExceptionDao: RecurrenceExceptionDao,
    private val prepaymentAllocator: StudentPrepaymentAllocator
) : LessonsRepository {
    override fun observeLessons(from: Instant, to: Instant): Flow<List<LessonDetails>> =
        combine(
            lessonDao.observeInRange(from, to),
            recurrenceRuleDao.observeAll(),
            recurrenceExceptionDao.observeAll()
        ) { lessons, rules, exceptions ->
            val baseLessons = lessons.map { it.toLessonDetails() }
            val activeRules = rules.filter { rule ->
                rule.startDateTime < to && (rule.untilDateTime == null || rule.untilDateTime >= from)
            }
            if (activeRules.isEmpty()) {
                return@combine baseLessons.sortedBy { it.startAt }
            }

            val labelBySeries = activeRules.associate { it.id to buildRecurrenceLabel(it) }
            val baseNormalized = baseLessons.map { detail ->
                val seriesId = detail.seriesId
                if (seriesId != null) {
                    detail.copy(
                        isRecurring = true,
                        recurrenceLabel = labelBySeries[seriesId],
                        originalStartAt = detail.originalStartAt ?: detail.startAt
                    )
                } else {
                    detail
                }
            }

            val exceptionsBySeries = exceptions.groupBy { it.seriesId }
            val generated = mutableListOf<LessonDetails>()
            for (rule in activeRules) {
                val baseLesson = lessonDao.findByIdWithStudent(rule.baseLessonId)?.toLessonDetails() ?: continue
                val occurrences = expandSeries(rule, from, to)
                if (occurrences.isEmpty()) continue
                val applied = applyExceptions(
                    template = baseLesson,
                    rule = rule,
                    occurrences = occurrences,
                    exceptions = exceptionsBySeries[rule.id].orEmpty(),
                    rangeStart = from,
                    rangeEnd = to
                )
                generated += applied
            }

            (baseNormalized + generated).sortedBy { it.startAt }
        }

    override fun observeTodayLessons(dayStart: Instant, dayEnd: Instant): Flow<List<LessonForToday>> =
        lessonDao.observeInRange(dayStart, dayEnd).map { lessons -> lessons.map { it.toLessonForToday() } }

    override fun observeOutstandingLessons(before: Instant): Flow<List<LessonForToday>> =
        lessonDao.observeOutstanding(before, PaymentStatus.outstandingStatuses)
            .map { lessons -> lessons.map { it.toLessonForToday() } }

    override fun observeOutstandingLessonDetails(before: Instant): Flow<List<LessonDetails>> =
        lessonDao.observeOutstanding(before, PaymentStatus.outstandingStatuses)
            .map { lessons -> lessons.map { it.toLessonDetails() } }

    override fun observeWeekStats(from: Instant, to: Instant): Flow<LessonsRangeStats> =
        combine(
            lessonDao.observeLessonCounts(
                from = from,
                to = to,
                paidStatus = PaymentStatus.PAID,
                outstandingStatuses = PaymentStatus.outstandingStatuses
            ),
            paymentDao.observeTotalInRange(
                from = from,
                to = to,
                status = PaymentStatus.PAID
            )
        ) { counts: LessonCountTuple, earned ->
            LessonsRangeStats(
                totalLessons = counts.totalLessons,
                paidLessons = counts.paidLessons,
                debtLessons = counts.debtLessons,
                earnedCents = earned
            )
        }

    override fun observeLessonDetails(id: Long): Flow<LessonDetails?> =
        lessonDao.observeById(id).map { it?.toLessonDetails() }

    override fun observeByStudent(studentId: Long): Flow<List<Lesson>> =
        lessonDao.observeByStudent(studentId)

    override suspend fun getById(id: Long): Lesson? = lessonDao.findById(id)

    override suspend fun upsert(lesson: Lesson): Long {
        val id = lessonDao.upsert(lesson)
        prepaymentAllocator.sync(lesson.studentId)
        return id
    }

    override suspend fun create(request: LessonCreateRequest): Long {
        val now = Instant.now()
        var lesson = Lesson(
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
            updatedAt = now
        )
        val id = lessonDao.upsert(lesson)
        val recurrenceRequest = request.recurrence
        if (recurrenceRequest != null) {
            val ruleId = recurrenceRuleDao.upsert(
                RecurrenceRule(
                    baseLessonId = id,
                    frequency = recurrenceRequest.frequency,
                    interval = recurrenceRequest.interval,
                    daysOfWeek = recurrenceRequest.daysOfWeek,
                    startDateTime = request.startAt,
                    untilDateTime = recurrenceRequest.until,
                    timezone = recurrenceRequest.timezone.id
                )
            )
            lesson = lesson.copy(id = id, seriesId = ruleId, updatedAt = Instant.now())
            lessonDao.upsert(lesson)
        }
        prepaymentAllocator.sync(lesson.studentId)
        return id
    }

    override suspend fun findConflicts(start: Instant, end: Instant): List<LessonDetails> {
        return lessonDao.findOverlapping(start, end).map { it.toLessonDetails() }
    }

    override suspend fun latestLessonForStudent(studentId: Long): Lesson? {
        return lessonDao.findLatestForStudent(studentId)?.lesson
    }

    override suspend fun delete(id: Long) {
        val lesson = lessonDao.findById(id)
        lessonDao.deleteById(id)
        if (lesson != null) {
            prepaymentAllocator.sync(lesson.studentId)
        }
    }

    override suspend fun markPaid(id: Long) {
        val lesson = lessonDao.findById(id) ?: return
        val now = Instant.now()
        lessonDao.updatePayment(id, PaymentStatus.PAID, lesson.priceCents, now, now)

        val existing = paymentDao.findByLesson(id)
        val payment = (existing ?: Payment(
            lessonId = lesson.id,
            studentId = lesson.studentId,
            amountCents = lesson.priceCents,
            status = PaymentStatus.PAID,
            at = now
        )).copy(
            amountCents = lesson.priceCents,
            status = PaymentStatus.PAID,
            at = now
        )

        if (existing == null) {
            paymentDao.insert(payment)
        } else {
            paymentDao.update(payment)
        }
    }

    override suspend fun markDue(id: Long) {
        val lesson = lessonDao.findById(id) ?: return
        val now = Instant.now()
        lessonDao.updatePayment(id, PaymentStatus.DUE, 0, now, now)

        val existing = paymentDao.findByLesson(id)
        val payment = (existing ?: Payment(
            lessonId = lesson.id,
            studentId = lesson.studentId,
            amountCents = lesson.priceCents,
            status = PaymentStatus.DUE,
            at = now
        )).copy(
            amountCents = lesson.priceCents,
            status = PaymentStatus.DUE,
            at = now
        )

        if (existing == null) {
            paymentDao.insert(payment)
        } else {
            paymentDao.update(payment)
        }
    }

    override suspend fun saveNote(id: Long, note: String?) =
        lessonDao.updateNote(id, note, Instant.now())

    override suspend fun resetPaymentStatus(id: Long) {
        val lesson = lessonDao.findById(id) ?: return
        val now = Instant.now()
        lessonDao.updatePayment(id, PaymentStatus.UNPAID, 0, now, null)

        val existing = paymentDao.findByLesson(id)
        if (existing != null) {
            paymentDao.delete(existing)
        }
    }

    private fun expandSeries(
        rule: RecurrenceRule,
        rangeStart: Instant,
        rangeEnd: Instant
    ): List<Instant> {
        if (rangeStart >= rangeEnd) return emptyList()
        val zone = runCatching { ZoneId.of(rule.timezone) }.getOrDefault(ZoneId.systemDefault())
        val base = rule.startDateTime.atZone(zone)
        val effectiveEndInstant = rule.untilDateTime?.takeIf { it < rangeEnd } ?: rangeEnd
        if (effectiveEndInstant <= rule.startDateTime) return emptyList()
        val windowStart = rangeStart.atZone(zone)
        val windowEnd = effectiveEndInstant.atZone(zone)
        val results = mutableSetOf<Instant>()

        when (rule.frequency) {
            RecurrenceFrequency.MONTHLY_BY_DOW -> {
                val monthsStep = maxOf(1, rule.interval)
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
                    if (instant != rule.startDateTime && !candidate.isBefore(windowStart) && instant < rangeEnd) {
                        results += instant
                    }
                    monthOffset += 1
                }
            }
            else -> {
                val intervalWeeks = when (rule.frequency) {
                    RecurrenceFrequency.WEEKLY -> maxOf(1, rule.interval)
                    RecurrenceFrequency.BIWEEKLY -> maxOf(1, rule.interval) * 2
                    RecurrenceFrequency.MONTHLY_BY_DOW -> 1 // unreachable
                }
                val targetDays = if (rule.daysOfWeek.isEmpty()) {
                    listOf(base.dayOfWeek)
                } else {
                    rule.daysOfWeek.sortedBy { it.value }
                }
                for (day in targetDays) {
                    var occurrence = base.with(TemporalAdjusters.nextOrSame(day))
                    if (occurrence.isBefore(base)) {
                        occurrence = occurrence.plusWeeks(intervalWeeks.toLong())
                    }
                    while (true) {
                        val instant = occurrence.toInstant()
                        if (instant > windowEnd.toInstant()) break
                        if (instant != rule.startDateTime) {
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

    private fun applyExceptions(
        template: LessonDetails,
        rule: RecurrenceRule,
        occurrences: List<Instant>,
        exceptions: List<RecurrenceException>,
        rangeStart: Instant,
        rangeEnd: Instant
    ): List<LessonDetails> {
        if (occurrences.isEmpty()) return emptyList()
        val exceptionByOriginal = exceptions.associateBy { it.originalDateTime }
        val label = buildRecurrenceLabel(rule)
        val results = mutableListOf<LessonDetails>()

        for (occurrence in occurrences) {
            val exception = exceptionByOriginal[occurrence]
            if (exception?.type == RecurrenceExceptionType.CANCELLED) {
                continue
            }
            val actualStart = exception?.overrideStartDateTime ?: occurrence
            if (actualStart < rangeStart || actualStart >= rangeEnd) continue
            val duration = exception?.overrideDurationMinutes
                ?.takeIf { it > 0 }
                ?.let { Duration.ofMinutes(it.toLong()) }
                ?: template.duration
            val actualEnd = actualStart.plus(duration)
            val note = exception?.overrideNotes ?: template.lessonNote
            val price = exception?.overridePrice ?: template.priceCents

            results += template.copy(
                id = syntheticId(rule.id, actualStart),
                startAt = actualStart,
                endAt = actualEnd,
                duration = duration,
                priceCents = price,
                paidCents = 0,
                lessonNote = note,
                isRecurring = true,
                seriesId = rule.id,
                originalStartAt = occurrence,
                recurrenceLabel = label
            )
        }

        return results
    }

    private fun buildRecurrenceLabel(rule: RecurrenceRule): String {
        return when (rule.frequency) {
            RecurrenceFrequency.MONTHLY_BY_DOW -> {
                val zone = runCatching { ZoneId.of(rule.timezone) }.getOrDefault(ZoneId.systemDefault())
                val base = rule.startDateTime.atZone(zone)
                val ordinal = ((base.dayOfMonth - 1) / 7) + 1
                val ordinalLabel = when (ordinal) {
                    1 -> "первую"
                    2 -> "вторую"
                    3 -> "третью"
                    4 -> "четвертую"
                    else -> "пятую"
                }
                "каждую ${ordinalLabel} ${base.dayOfWeek.toShortLabel()}"
            }
            RecurrenceFrequency.WEEKLY -> {
                val interval = maxOf(1, rule.interval)
                val targetDays = if (rule.daysOfWeek.isEmpty()) listOf(baseDayOfWeek(rule)) else rule.daysOfWeek
                val daysLabel = targetDays.joinToString(separator = ", ") { it.toShortLabel() }
                if (interval == 1) {
                    "каждую $daysLabel"
                } else {
                    "каждые $interval недели"
                }
            }
            RecurrenceFrequency.BIWEEKLY -> {
                val targetDays = if (rule.daysOfWeek.isEmpty()) listOf(baseDayOfWeek(rule)) else rule.daysOfWeek
                val daysLabel = targetDays.joinToString(separator = ", ") { it.toShortLabel() }
                "каждые 2 недели ($daysLabel)"
            }
        }
    }

    private fun baseDayOfWeek(rule: RecurrenceRule): DayOfWeek {
        val zone = runCatching { ZoneId.of(rule.timezone) }.getOrDefault(ZoneId.systemDefault())
        return rule.startDateTime.atZone(zone).dayOfWeek
    }

    private fun DayOfWeek.toShortLabel(): String {
        return when (this) {
            DayOfWeek.MONDAY -> "Пн"
            DayOfWeek.TUESDAY -> "Вт"
            DayOfWeek.WEDNESDAY -> "Ср"
            DayOfWeek.THURSDAY -> "Чт"
            DayOfWeek.FRIDAY -> "Пт"
            DayOfWeek.SATURDAY -> "Сб"
            DayOfWeek.SUNDAY -> "Вс"
        }
    }

    private fun syntheticId(seriesId: Long, start: Instant): Long {
        val combined = (seriesId shl 1) xor start.toEpochMilli()
        val positive = combined and Long.MAX_VALUE
        return -(positive + 1)
    }
}
