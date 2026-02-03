package com.tutorly.data.repo.room

import com.tutorly.data.db.dao.LessonCountTuple
import com.tutorly.data.db.dao.LessonDao
import com.tutorly.data.db.dao.PaymentDao
import com.tutorly.data.db.dao.RecurrenceExceptionDao
import com.tutorly.data.db.dao.RecurrenceRuleDao
import com.tutorly.data.db.projections.LessonWithStudent
import com.tutorly.data.db.projections.toLessonDetails
import com.tutorly.domain.model.LessonCreateRequest
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.model.LessonForToday
import com.tutorly.domain.model.LessonsRangeStats
import com.tutorly.domain.recurrence.RecurrenceLabelFormatter
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.models.Lesson
import com.tutorly.models.LessonStatus
import com.tutorly.models.LessonRecurrence
import com.tutorly.models.Payment
import com.tutorly.models.PaymentStatus
import com.tutorly.models.RecurrenceException
import com.tutorly.models.RecurrenceExceptionType
import com.tutorly.models.RecurrenceFrequency
import com.tutorly.models.RecurrenceRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
            buildLessonsInRange(
                baseLessons = lessons,
                rules = rules,
                exceptions = exceptions,
                rangeStart = from,
                rangeEnd = to
            ).map { it.details }
        }

    override fun observeTodayLessons(dayStart: Instant, dayEnd: Instant): Flow<List<LessonForToday>> =
        combine(
            lessonDao.observeInRange(dayStart, dayEnd),
            recurrenceRuleDao.observeAll(),
            recurrenceExceptionDao.observeAll()
        ) { lessons, rules, exceptions ->
            buildLessonsInRange(
                baseLessons = lessons,
                rules = rules,
                exceptions = exceptions,
                rangeStart = dayStart,
                rangeEnd = dayEnd
            ).map { bundle ->
                val detail = bundle.details
                LessonForToday(
                    id = detail.id,
                    baseLessonId = detail.baseLessonId,
                    studentId = detail.studentId,
                    studentName = detail.studentName,
                    studentGrade = detail.studentGrade,
                    subjectName = detail.subjectName,
                    lessonTitle = detail.lessonTitle,
                    startAt = detail.startAt,
                    endAt = detail.endAt,
                    duration = detail.duration,
                    priceCents = detail.priceCents,
                    studentRateCents = bundle.studentRateCents,
                    note = detail.lessonNote,
                    paymentStatus = detail.paymentStatus,
                    lessonStatus = detail.lessonStatus,
                    markedAt = bundle.markedAt,
                    isRecurring = detail.isRecurring,
                    seriesId = detail.seriesId,
                    originalStartAt = detail.originalStartAt,
                    recurrenceLabel = detail.recurrenceLabel,
                    paidCents = detail.paidCents,
                )
            }
        }

    override fun observeOutstandingLessons(before: Instant): Flow<List<LessonForToday>> =
        combine(
            lessonDao.observeOutstanding(before, PaymentStatus.outstandingStatuses),
            recurrenceRuleDao.observeAll(),
            recurrenceExceptionDao.observeAll()
        ) { lessons, rules, exceptions ->
            buildLessonsInRange(
                baseLessons = lessons,
                rules = rules,
                exceptions = exceptions,
                rangeStart = Instant.EPOCH,
                rangeEnd = before
            ).map { bundle ->
                val detail = bundle.details
                LessonForToday(
                    id = detail.id,
                    baseLessonId = detail.baseLessonId,
                    studentId = detail.studentId,
                    studentName = detail.studentName,
                    studentGrade = detail.studentGrade,
                    subjectName = detail.subjectName,
                    lessonTitle = detail.lessonTitle,
                    startAt = detail.startAt,
                    endAt = detail.endAt,
                    duration = detail.duration,
                    priceCents = detail.priceCents,
                    studentRateCents = bundle.studentRateCents,
                    note = detail.lessonNote,
                    paymentStatus = detail.paymentStatus,
                    lessonStatus = detail.lessonStatus,
                    markedAt = bundle.markedAt,
                    isRecurring = detail.isRecurring,
                    seriesId = detail.seriesId,
                    originalStartAt = detail.originalStartAt,
                    recurrenceLabel = detail.recurrenceLabel,
                    paidCents = detail.paidCents,
                )
            }
        }

    override fun observeOutstandingLessonDetails(before: Instant): Flow<List<LessonDetails>> =
        combine(
            lessonDao.observeOutstanding(before, PaymentStatus.outstandingStatuses),
            recurrenceRuleDao.observeAll(),
            recurrenceExceptionDao.observeAll()
        ) { lessons, rules, exceptions ->
            buildLessonsInRange(
                baseLessons = lessons,
                rules = rules,
                exceptions = exceptions,
                rangeStart = Instant.EPOCH,
                rangeEnd = before
            ).map { it.details }
        }

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
        combine(
            lessonDao.observeById(id),
            recurrenceRuleDao.observeAll()
        ) { lesson, rules ->
            lesson?.let { projection ->
                val details = projection.toLessonDetails()
                val resolvedRule = details.seriesId?.let { seriesId ->
                    rules.firstOrNull { rule -> rule.id == seriesId }
                } ?: rules.firstOrNull { rule -> rule.baseLessonId == details.baseLessonId }
                val label = resolvedRule?.let { rule -> RecurrenceLabelFormatter.format(rule) }
                details.copy(
                    isRecurring = resolvedRule != null,
                    seriesId = resolvedRule?.id ?: details.seriesId,
                    recurrenceLabel = label
                )
            }
        }

    override fun observeByStudent(studentId: Long): Flow<List<Lesson>> =
        combine(
            lessonDao.observeByStudent(studentId),
            recurrenceRuleDao.observeAll()
        ) { lessons, rules ->
            attachRecurrence(lessons, rules)
        }

    override suspend fun getById(id: Long): Lesson? {
        val lesson = lessonDao.findById(id) ?: return null
        val rule = loadRuleForLesson(lesson)
        val normalized = ensureSeriesLink(lesson, rule)
        return normalized.copy(recurrence = rule?.toLessonRecurrence())
    }

    override suspend fun findExactLesson(studentId: Long, startAt: Instant, endAt: Instant): Lesson? {
        return lessonDao.findByStudentAndTime(studentId, startAt, endAt)
    }

    override suspend fun upsert(lesson: Lesson): Long {
        val existing = lesson.id.takeIf { it != 0L }?.let { lessonDao.findById(it) }
        val id = lessonDao.upsert(lesson)
        val baseLessonId = if (lesson.id == 0L) id else lesson.id
        val recurrence = lesson.recurrence
        when {
            recurrence != null -> {
                val targetRuleId = lesson.seriesId ?: existing?.seriesId ?: 0L
                val ruleId = recurrenceRuleDao.upsert(
                    RecurrenceRule(
                        id = targetRuleId,
                        baseLessonId = baseLessonId,
                        frequency = recurrence.frequency,
                        interval = recurrence.interval,
                        daysOfWeek = recurrence.daysOfWeek,
                        startDateTime = recurrence.startDateTime,
                        untilDateTime = recurrence.untilDateTime,
                        timezone = recurrence.timezone.id
                    )
                )
                if (lesson.seriesId == null || lesson.seriesId != ruleId) {
                    val refreshed = lesson.copy(
                        id = baseLessonId,
                        seriesId = ruleId,
                        updatedAt = Instant.now()
                    )
                    lessonDao.upsert(refreshed)
                }
                val anchor = lessonDao.findById(baseLessonId)
                val rule = recurrenceRuleDao.findById(ruleId)
                if (anchor != null && rule != null) {
                    val normalizedAnchor = ensureSeriesLink(anchor, rule)
                    generateFutureOccurrences(normalizedAnchor, rule)
                }
            }

            existing?.seriesId != null && lesson.seriesId == null -> {
                recurrenceRuleDao.deleteById(existing.seriesId)
                lessonDao.deleteInstancesForSeries(existing.seriesId)
            }
        }
        prepaymentAllocator.sync(lesson.studentId)
        return baseLessonId
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
            val updatedAt = Instant.now()
            lesson = lesson.copy(id = id, seriesId = ruleId, updatedAt = updatedAt)
            lessonDao.updateSeriesId(id, ruleId, updatedAt)
            val rule = recurrenceRuleDao.findById(ruleId)
            if (rule != null) {
                val anchor = lessonDao.findById(id)
                val normalizedAnchor = if (anchor != null) ensureSeriesLink(anchor, rule) else lesson
                generateFutureOccurrences(normalizedAnchor, rule)
            }
        }
        prepaymentAllocator.sync(lesson.studentId)
        return id
    }


    override suspend fun moveLesson(lessonId: Long, newStart: Instant, newEnd: Instant) {
        val existing = lessonDao.findById(lessonId) ?: return
        val updated = existing.copy(
            startAt = newStart,
            endAt = newEnd,
            updatedAt = Instant.now()
        )
        lessonDao.upsert(updated)
        if (updated.seriesId != null) {
            val rule = recurrenceRuleDao.findById(updated.seriesId)
            if (rule != null) {
                recurrenceRuleDao.upsert(
                    rule.copy(
                        startDateTime = newStart,
                        untilDateTime = rule.untilDateTime
                    )
                )
            }
            recurrenceExceptionDao.deleteInstance(updated.seriesId, existing.startAt)
        }
    }

    override suspend fun moveRecurringOccurrence(
        seriesId: Long,
        originalStart: Instant,
        newStart: Instant,
        duration: Duration
    ) {
        val minutes = duration.toMinutes().toInt().coerceAtLeast(0)
        val exception = RecurrenceException(
            seriesId = seriesId,
            originalDateTime = originalStart,
            type = RecurrenceExceptionType.OVERRIDDEN,
            overrideStartDateTime = newStart,
            overrideDurationMinutes = minutes.takeIf { it > 0 },
            overrideNotes = null,
            overridePrice = null
        )
        recurrenceExceptionDao.upsert(exception)
    }

    override suspend fun clearRecurringOverride(seriesId: Long, originalStart: Instant) {
        recurrenceExceptionDao.deleteInstance(seriesId, originalStart)
    }

    override suspend fun findConflicts(start: Instant, end: Instant): List<LessonDetails> {
        return lessonDao.findOverlapping(start, end).map { it.toLessonDetails() }
    }

    override suspend fun latestLessonForStudent(studentId: Long): Lesson? {
        val projection = lessonDao.findLatestForStudent(studentId) ?: return null
        val lesson = projection.lesson
        val rule = loadRuleForLesson(lesson)
        val normalized = ensureSeriesLink(lesson, rule)
        return normalized.copy(recurrence = rule?.toLessonRecurrence())
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

    private suspend fun buildLessonsInRange(
        baseLessons: List<LessonWithStudent>,
        rules: List<RecurrenceRule>,
        exceptions: List<RecurrenceException>,
        rangeStart: Instant,
        rangeEnd: Instant
    ): List<LessonBundle> {
        val storedStartsBySeries = baseLessons
            .filter { it.lesson.seriesId != null && it.lesson.isInstance }
            .groupBy { it.lesson.seriesId!! }
            .mapValues { entry -> entry.value.map { it.lesson.startAt }.toSet() }
        val baseBundles = baseLessons.map { lesson ->
            LessonBundle(
                details = lesson.toLessonDetails(),
                studentRateCents = lesson.student.rateCents,
                markedAt = lesson.lesson.markedAt
            )
        }
        if (rules.isEmpty()) {
            return baseBundles.sortedBy { it.details.startAt }
        }

        val activeRules = rules.filter { rule ->
            rule.startDateTime < rangeEnd && (rule.untilDateTime == null || rule.untilDateTime >= rangeStart)
        }
        if (activeRules.isEmpty()) {
            return baseBundles.sortedBy { it.details.startAt }
        }

        val ruleById = activeRules.associateBy { it.id }
        val ruleByBase = activeRules.associateBy { it.baseLessonId }
        val labelBySeries = ruleById.mapValues { (_, rule) -> RecurrenceLabelFormatter.format(rule) }
        val normalizedBase = baseBundles.map { bundle ->
            val detail = bundle.details
            val rule = detail.seriesId?.let { ruleById[it] } ?: ruleByBase[detail.baseLessonId]
            if (rule != null) {
                val label = labelBySeries[rule.id]
                bundle.copy(
                    details = detail.copy(
                        baseLessonId = rule.baseLessonId,
                        isRecurring = true,
                        seriesId = rule.id,
                        recurrenceLabel = label,
                        originalStartAt = detail.originalStartAt ?: detail.startAt
                    )
                )
            } else {
                bundle
            }
        }
        val baseByLessonId = normalizedBase.associateBy { it.details.baseLessonId }

        val exceptionsBySeries = exceptions.groupBy { it.seriesId }
        val generated = mutableListOf<LessonBundle>()

        for (rule in activeRules) {
            val templateBundle = baseByLessonId[rule.baseLessonId]
                ?: lessonDao.findByIdWithStudent(rule.baseLessonId)?.let { base ->
                    val detail = base.toLessonDetails()
                    val normalizedDetail = detail.copy(
                        isRecurring = true,
                        seriesId = rule.id,
                        recurrenceLabel = labelBySeries[rule.id],
                        originalStartAt = detail.originalStartAt ?: detail.startAt
                    )
                    LessonBundle(
                        details = normalizedDetail,
                        studentRateCents = base.student.rateCents,
                        markedAt = base.lesson.markedAt
                    )
                }
                ?: continue

            val occurrences = expandSeries(rule, rangeStart, rangeEnd)
            val storedStarts = storedStartsBySeries[rule.id].orEmpty()
            val filteredOccurrences = occurrences.filter { it !in storedStarts }
            if (filteredOccurrences.isEmpty()) continue

            val applied = applyExceptions(
                template = templateBundle.details,
                rule = rule,
                occurrences = filteredOccurrences,
                exceptions = exceptionsBySeries[rule.id].orEmpty(),
                rangeStart = rangeStart,
                rangeEnd = rangeEnd
            ).map { detail ->
                LessonBundle(
                    details = detail,
                    studentRateCents = templateBundle.studentRateCents,
                    markedAt = null
                )
            }
            generated += applied
        }

        return (normalizedBase + generated).sortedBy { it.details.startAt }
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
        val label = RecurrenceLabelFormatter.format(rule)
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

    private fun attachRecurrence(
        lessons: List<Lesson>,
        rules: List<RecurrenceRule>
    ): List<Lesson> {
        if (lessons.isEmpty()) return emptyList()
        if (rules.isEmpty()) return lessons.map { it.copy(recurrence = null) }

        val ruleById = rules.associateBy { it.id }
        val ruleByBase = rules.associateBy { it.baseLessonId }
        return lessons.map { lesson ->
            val rule = lesson.seriesId?.let { seriesId ->
                ruleById[seriesId]
            } ?: ruleByBase[lesson.id]
            val recurrence = rule?.toLessonRecurrence()
            val seriesId = rule?.id ?: lesson.seriesId
            lesson.copy(seriesId = seriesId, recurrence = recurrence)
        }
    }

    private suspend fun generateFutureOccurrences(
        baseLesson: Lesson,
        rule: RecurrenceRule
    ) {
        val existingInstances = lessonDao.listInstancesForSeries(rule.id)
        val now = Instant.now()
        val horizon = determineGenerationHorizon(baseLesson.startAt, rule, now)
        if (horizon <= now) {
            existingInstances
                .filter { it.startAt >= now }
                .forEach { lessonDao.deleteById(it.id) }
            return
        }

        val occurrences = expandSeries(rule, baseLesson.startAt, horizon)
            .filter { it >= now }
        if (occurrences.isEmpty()) {
            existingInstances
                .filter { it.startAt >= now }
                .forEach { lessonDao.deleteById(it.id) }
            return
        }

        val limited = occurrences.take(DEFAULT_GENERATED_OCCURRENCES)
        val desiredStarts = limited.toSet()
        existingInstances
            .filter { it.startAt >= now && it.startAt !in desiredStarts }
            .forEach { lessonDao.deleteById(it.id) }

        val existingByStart = existingInstances.associateBy { it.startAt }
        val duration = resolveBaseDuration(baseLesson)
        val timestamp = Instant.now()
        for (start in limited) {
            val desiredEnd = start.plus(duration)
            val existing = existingByStart[start]
            if (existing != null) {
                val updated = existing.copy(
                    studentId = baseLesson.studentId,
                    subjectId = baseLesson.subjectId,
                    title = baseLesson.title,
                    endAt = desiredEnd,
                    priceCents = baseLesson.priceCents,
                    note = baseLesson.note,
                    updatedAt = timestamp
                )
                lessonDao.upsert(updated)
                continue
            }
            val instance = Lesson(
                studentId = baseLesson.studentId,
                subjectId = baseLesson.subjectId,
                title = baseLesson.title,
                startAt = start,
                endAt = desiredEnd,
                priceCents = baseLesson.priceCents,
                paidCents = 0,
                paymentStatus = PaymentStatus.UNPAID,
                markedAt = null,
                status = LessonStatus.PLANNED,
                note = baseLesson.note,
                createdAt = timestamp,
                updatedAt = timestamp,
                canceledAt = null,
                seriesId = rule.id,
                isInstance = true,
                recurrence = null
            )
            lessonDao.upsert(instance)
        }
    }

    private suspend fun loadRuleForLesson(lesson: Lesson): RecurrenceRule? {
        lesson.seriesId?.let { seriesId ->
            recurrenceRuleDao.findById(seriesId)?.let { return it }
        }
        return recurrenceRuleDao.findByBaseLessonId(lesson.id)
    }

    private suspend fun ensureSeriesLink(lesson: Lesson, rule: RecurrenceRule?): Lesson {
        if (rule == null) return lesson
        if (lesson.seriesId == rule.id) return lesson
        val updated = lesson.copy(seriesId = rule.id)
        lessonDao.upsert(updated)
        return updated
    }

    private fun determineGenerationHorizon(baseStart: Instant, rule: RecurrenceRule, now: Instant): Instant {
        val baseline = if (baseStart.isAfter(now)) baseStart else now
        val defaultHorizon = baseline.plus(Duration.ofDays(DEFAULT_GENERATION_WEEKS * 7))
        val until = rule.untilDateTime ?: return defaultHorizon
        return if (until.isBefore(defaultHorizon)) until else defaultHorizon
    }

    private fun resolveBaseDuration(lesson: Lesson): Duration {
        val duration = Duration.between(lesson.startAt, lesson.endAt)
        if (!duration.isNegative && !duration.isZero) return duration
        return Duration.ofMinutes(DEFAULT_LESSON_DURATION_MINUTES)
    }

    private fun RecurrenceRule.toLessonRecurrence(): LessonRecurrence {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
        return LessonRecurrence(
            frequency = frequency,
            interval = interval,
            daysOfWeek = daysOfWeek,
            startDateTime = startDateTime,
            untilDateTime = untilDateTime,
            timezone = zone
        )
    }

    private fun syntheticId(seriesId: Long, start: Instant): Long {
        val combined = (seriesId shl 1) xor start.toEpochMilli()
        val positive = combined and Long.MAX_VALUE
        return -(positive + 1)
    }

    private data class LessonBundle(
        val details: LessonDetails,
        val studentRateCents: Int?,
        val markedAt: Instant?
    )

    private companion object {
        private const val DEFAULT_GENERATION_WEEKS = 12L
        private const val DEFAULT_GENERATED_OCCURRENCES = 12
        private const val DEFAULT_LESSON_DURATION_MINUTES = 60L
    }
}
