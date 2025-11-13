package com.tutorly.data.repo.room

import com.tutorly.data.db.dao.LessonDao
import com.tutorly.data.db.dao.PaymentDao
import com.tutorly.data.db.dao.RecurrenceExceptionDao
import com.tutorly.data.db.dao.RecurrenceRuleDao
import com.tutorly.data.db.projections.LessonWithStudent
import com.tutorly.domain.model.LessonCreateRequest
import com.tutorly.domain.model.RecurrenceCreateRequest
import com.tutorly.models.Lesson
import com.tutorly.models.LessonRecurrence
import com.tutorly.models.Payment
import com.tutorly.models.PaymentStatus
import com.tutorly.models.RecurrenceException
import com.tutorly.models.RecurrenceRule
import com.tutorly.models.Student
import com.tutorly.models.SubjectPreset
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomLessonsRepositoryTest {
    private val passthroughTransactionRunner = object : TransactionRunner {
        override suspend fun <T> invoke(block: suspend () -> T): T = block()
    }

    @Test
    fun `weekly recurrence generates stored future lessons`() = runBlocking {
        val lessonDao = FakeLessonDao()
        val paymentDao = FakePaymentDao()
        val recurrenceRuleDao = FakeRecurrenceRuleDao()
        val recurrenceExceptionDao = FakeRecurrenceExceptionDao()
        val prepaymentAllocator = StudentPrepaymentAllocator(lessonDao, paymentDao)

        val student = Student(id = 1L, name = "Alice")
        lessonDao.registerStudent(student)

        val repository = RoomLessonsRepository(
            lessonDao = lessonDao,
            paymentDao = paymentDao,
            recurrenceRuleDao = recurrenceRuleDao,
            recurrenceExceptionDao = recurrenceExceptionDao,
            prepaymentAllocator = prepaymentAllocator,
            transactionRunner = passthroughTransactionRunner
        )

        val zone = ZoneId.of("Europe/Moscow")
        val start = LocalDate.of(2024, 6, 3).atTime(LocalTime.of(10, 0)).atZone(zone)
        val end = start.plusHours(1)

        val request = LessonCreateRequest(
            studentId = student.id,
            subjectId = null,
            title = "Math",
            startAt = start.toInstant(),
            endAt = end.toInstant(),
            priceCents = 1500,
            note = null,
            recurrence = RecurrenceCreateRequest(
                frequency = com.tutorly.models.RecurrenceFrequency.WEEKLY,
                interval = 1,
                daysOfWeek = listOf(DayOfWeek.MONDAY),
                until = null,
                timezone = zone
            )
        )

        repository.create(request)

        val rule = recurrenceRuleDao.observeAll().first().single()
        val baseLesson = lessonDao.findById(rule.baseLessonId) ?: error("Base lesson missing")
        assertEquals(rule.id, baseLesson.seriesId)
        assertTrue(baseLesson.recurrence != null)

        val ruleId = rule.id
        val storedInstances = lessonDao.listInstancesForSeries(ruleId)
        assertTrue(storedInstances.isNotEmpty())
        val orderedStarts = storedInstances.map { it.startAt }.sorted()
        assertEquals(start.plusWeeks(1).toInstant(), orderedStarts.first())

        val rangeStart = start.toInstant()
        val rangeEnd = start.plusWeeks(4).toInstant()

        val lessons = repository.observeLessons(rangeStart, rangeEnd)
            .first { list -> list.any { it.seriesId == ruleId && it.id != it.baseLessonId } }

        val generated = lessons.filter { it.seriesId == ruleId && it.id != it.baseLessonId }
        assertTrue(generated.isNotEmpty())
        assertEquals(start.plusWeeks(1).toInstant(), generated.minOf { it.startAt })
    }

    @Test
    fun `recurring lesson surfaces in today stream`() = runBlocking {
        val lessonDao = FakeLessonDao()
        val paymentDao = FakePaymentDao()
        val recurrenceRuleDao = FakeRecurrenceRuleDao()
        val recurrenceExceptionDao = FakeRecurrenceExceptionDao()
        val prepaymentAllocator = StudentPrepaymentAllocator(lessonDao, paymentDao)

        val student = Student(id = 1L, name = "Alice")
        lessonDao.registerStudent(student)

        val repository = RoomLessonsRepository(
            lessonDao = lessonDao,
            paymentDao = paymentDao,
            recurrenceRuleDao = recurrenceRuleDao,
            recurrenceExceptionDao = recurrenceExceptionDao,
            prepaymentAllocator = prepaymentAllocator,
            transactionRunner = passthroughTransactionRunner
        )

        val zone = ZoneId.of("Europe/Moscow")
        val baseStart = LocalDate.of(2024, 6, 3).atTime(LocalTime.of(10, 0)).atZone(zone)

        val request = LessonCreateRequest(
            studentId = student.id,
            subjectId = null,
            title = "Math",
            startAt = baseStart.toInstant(),
            endAt = baseStart.plusHours(1).toInstant(),
            priceCents = 2000,
            note = null,
            recurrence = RecurrenceCreateRequest(
                frequency = com.tutorly.models.RecurrenceFrequency.WEEKLY,
                interval = 1,
                daysOfWeek = listOf(DayOfWeek.MONDAY),
                until = null,
                timezone = zone
            )
        )

        repository.create(request)

        val seriesId = recurrenceRuleDao.observeAll().first().single().id

        val targetStart = baseStart.plusWeeks(2)
        val lessons = repository.observeTodayLessons(
            dayStart = targetStart.toInstant(),
            dayEnd = targetStart.plusDays(1).toInstant()
        ).first { it.isNotEmpty() }

        val occurrence = lessons.single()
        assertTrue(occurrence.isRecurring)
        assertEquals(seriesId, occurrence.seriesId)
        assertEquals(targetStart.toInstant(), occurrence.startAt)
        assertTrue(occurrence.id > 0)
        assertTrue(occurrence.baseLessonId != occurrence.id)
    }

    @Test
    fun `upsert stores recurrence rule for existing lesson`() = runBlocking {
        val lessonDao = FakeLessonDao()
        val paymentDao = FakePaymentDao()
        val recurrenceRuleDao = FakeRecurrenceRuleDao()
        val recurrenceExceptionDao = FakeRecurrenceExceptionDao()
        val prepaymentAllocator = StudentPrepaymentAllocator(lessonDao, paymentDao)

        val student = Student(id = 1L, name = "Alice")
        lessonDao.registerStudent(student)

        val repository = RoomLessonsRepository(
            lessonDao = lessonDao,
            paymentDao = paymentDao,
            recurrenceRuleDao = recurrenceRuleDao,
            recurrenceExceptionDao = recurrenceExceptionDao,
            prepaymentAllocator = prepaymentAllocator,
            transactionRunner = passthroughTransactionRunner
        )

        val zone = ZoneId.of("Europe/Moscow")
        val baseStart = LocalDate.of(2024, 6, 3).atTime(LocalTime.of(9, 0)).atZone(zone)
        val baseLesson = Lesson(
            id = 10L,
            studentId = student.id,
            subjectId = null,
            title = "Physics",
            startAt = baseStart.toInstant(),
            endAt = baseStart.plusHours(1).toInstant(),
            priceCents = 1800,
            paidCents = 0,
            paymentStatus = PaymentStatus.UNPAID,
            markedAt = null,
            status = com.tutorly.models.LessonStatus.PLANNED,
            note = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        lessonDao.upsert(baseLesson)

        val recurrence = LessonRecurrence(
            frequency = com.tutorly.models.RecurrenceFrequency.WEEKLY,
            interval = 1,
            daysOfWeek = listOf(DayOfWeek.MONDAY),
            startDateTime = baseStart.toInstant(),
            untilDateTime = null,
            timezone = zone
        )

        repository.upsert(baseLesson.copy(recurrence = recurrence))

        val stored = lessonDao.findById(baseLesson.id)!!
        val rules = recurrenceRuleDao.observeAll().first()
        assertEquals(1, rules.size)
        val rule = rules.single()
        assertEquals(stored.seriesId, rule.id)
        assertEquals(stored.id, rule.baseLessonId)
        assertEquals(recurrence.frequency, rule.frequency)
        assertEquals(recurrence.interval, rule.interval)
        assertEquals(recurrence.daysOfWeek, rule.daysOfWeek)
        assertEquals(recurrence.startDateTime, rule.startDateTime)
        assertEquals(recurrence.untilDateTime, rule.untilDateTime)
        assertEquals(recurrence.timezone.id, rule.timezone)
    }

    @Test
    fun `repository exposes recurrence metadata`() = runBlocking {
        val lessonDao = FakeLessonDao()
        val paymentDao = FakePaymentDao()
        val recurrenceRuleDao = FakeRecurrenceRuleDao()
        val recurrenceExceptionDao = FakeRecurrenceExceptionDao()
        val prepaymentAllocator = StudentPrepaymentAllocator(lessonDao, paymentDao)

        val student = Student(id = 1L, name = "Alice")
        lessonDao.registerStudent(student)

        val repository = RoomLessonsRepository(
            lessonDao = lessonDao,
            paymentDao = paymentDao,
            recurrenceRuleDao = recurrenceRuleDao,
            recurrenceExceptionDao = recurrenceExceptionDao,
            prepaymentAllocator = prepaymentAllocator,
            transactionRunner = passthroughTransactionRunner
        )

        val zone = ZoneId.of("Europe/Moscow")
        val start = LocalDate.of(2024, 6, 3).atTime(LocalTime.of(9, 0)).atZone(zone)
        val baseLesson = Lesson(
            id = 42L,
            studentId = student.id,
            subjectId = null,
            title = "Physics",
            startAt = start.toInstant(),
            endAt = start.plusHours(1).toInstant(),
            priceCents = 1800,
            paidCents = 0,
            paymentStatus = PaymentStatus.UNPAID,
            markedAt = null,
            status = com.tutorly.models.LessonStatus.PLANNED,
            note = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        lessonDao.upsert(baseLesson)

        val recurrence = LessonRecurrence(
            frequency = com.tutorly.models.RecurrenceFrequency.WEEKLY,
            interval = 1,
            daysOfWeek = listOf(DayOfWeek.MONDAY),
            startDateTime = start.toInstant(),
            untilDateTime = null,
            timezone = zone
        )

        repository.upsert(baseLesson.copy(recurrence = recurrence))

        val persisted = repository.getById(baseLesson.id)!!
        val observed = repository.observeByStudent(student.id)
            .first { lessons -> lessons.any { it.id == baseLesson.id && it.recurrence != null } }
            .first { it.id == baseLesson.id }

        assertEquals(recurrence.frequency, persisted.recurrence?.frequency)
        assertEquals(recurrence.interval, persisted.recurrence?.interval)
        assertEquals(recurrence.daysOfWeek, persisted.recurrence?.daysOfWeek)
        assertEquals(recurrence.startDateTime, persisted.recurrence?.startDateTime)
        assertEquals(recurrence.untilDateTime, persisted.recurrence?.untilDateTime)
        assertEquals(recurrence.timezone, persisted.recurrence?.timezone)

        assertEquals(recurrence.frequency, observed.recurrence?.frequency)
        assertEquals(recurrence.interval, observed.recurrence?.interval)
        assertEquals(recurrence.daysOfWeek, observed.recurrence?.daysOfWeek)
        assertEquals(recurrence.startDateTime, observed.recurrence?.startDateTime)
        assertEquals(recurrence.untilDateTime, observed.recurrence?.untilDateTime)
        assertEquals(recurrence.timezone, observed.recurrence?.timezone)
    }

    @Test
    fun `missing series id is recovered from recurrence rule`() = runBlocking {
        val lessonDao = FakeLessonDao()
        val paymentDao = FakePaymentDao()
        val recurrenceRuleDao = FakeRecurrenceRuleDao()
        val recurrenceExceptionDao = FakeRecurrenceExceptionDao()
        val prepaymentAllocator = StudentPrepaymentAllocator(lessonDao, paymentDao)

        val student = Student(id = 1L, name = "Alice")
        lessonDao.registerStudent(student)

        val repository = RoomLessonsRepository(
            lessonDao = lessonDao,
            paymentDao = paymentDao,
            recurrenceRuleDao = recurrenceRuleDao,
            recurrenceExceptionDao = recurrenceExceptionDao,
            prepaymentAllocator = prepaymentAllocator,
            transactionRunner = passthroughTransactionRunner
        )

        val zone = ZoneId.of("Europe/Moscow")
        val start = LocalDate.of(2024, 6, 3).atTime(LocalTime.of(9, 0)).atZone(zone)
        val baseLesson = Lesson(
            id = 100L,
            studentId = student.id,
            subjectId = null,
            title = "Chemistry",
            startAt = start.toInstant(),
            endAt = start.plusHours(1).toInstant(),
            priceCents = 1500,
            paidCents = 0,
            paymentStatus = PaymentStatus.UNPAID,
            markedAt = null,
            status = com.tutorly.models.LessonStatus.PLANNED,
            note = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        lessonDao.upsert(baseLesson)

        val ruleId = recurrenceRuleDao.upsert(
            RecurrenceRule(
                baseLessonId = baseLesson.id,
                frequency = com.tutorly.models.RecurrenceFrequency.WEEKLY,
                interval = 1,
                daysOfWeek = listOf(DayOfWeek.MONDAY),
                startDateTime = start.toInstant(),
                untilDateTime = null,
                timezone = zone.id
            )
        )

        val details = repository.observeLessonDetails(baseLesson.id)
            .first { it != null }
            ?: error("Expected details")

        assertTrue(details.isRecurring)
        assertEquals(ruleId, details.seriesId)
        assertNotNull(details.recurrence)

        val persisted = repository.getById(baseLesson.id) ?: error("Expected lesson")
        assertEquals(ruleId, persisted.seriesId)
        assertTrue(persisted.recurrence != null)

        val stored = lessonDao.findById(baseLesson.id) ?: error("Stored lesson missing")
        assertEquals(ruleId, stored.seriesId)
    }

    @Test
    fun `upsert clears recurrence rule when removed`() = runBlocking {
        val lessonDao = FakeLessonDao()
        val paymentDao = FakePaymentDao()
        val recurrenceRuleDao = FakeRecurrenceRuleDao()
        val recurrenceExceptionDao = FakeRecurrenceExceptionDao()
        val prepaymentAllocator = StudentPrepaymentAllocator(lessonDao, paymentDao)

        val student = Student(id = 1L, name = "Alice")
        lessonDao.registerStudent(student)

        val repository = RoomLessonsRepository(
            lessonDao = lessonDao,
            paymentDao = paymentDao,
            recurrenceRuleDao = recurrenceRuleDao,
            recurrenceExceptionDao = recurrenceExceptionDao,
            prepaymentAllocator = prepaymentAllocator,
            transactionRunner = passthroughTransactionRunner
        )

        val zone = ZoneId.of("Europe/Moscow")
        val start = LocalDate.of(2024, 6, 3).atTime(LocalTime.of(10, 0)).atZone(zone)

        val request = LessonCreateRequest(
            studentId = student.id,
            subjectId = null,
            title = "Math",
            startAt = start.toInstant(),
            endAt = start.plusHours(1).toInstant(),
            priceCents = 2000,
            note = null,
            recurrence = RecurrenceCreateRequest(
                frequency = com.tutorly.models.RecurrenceFrequency.WEEKLY,
                interval = 1,
                daysOfWeek = listOf(DayOfWeek.MONDAY),
                until = null,
                timezone = zone
            )
        )

        repository.create(request)

        val rule = recurrenceRuleDao.observeAll().first().single()
        val baseLesson = lessonDao.findById(rule.baseLessonId)!!
        assertTrue(lessonDao.listInstancesForSeries(rule.id).isNotEmpty())

        repository.upsert(baseLesson.copy(seriesId = null, recurrence = null))

        assertTrue(recurrenceRuleDao.observeAll().first().isEmpty())
        val cleared = lessonDao.findById(baseLesson.id)
        assertTrue(cleared?.seriesId == null)
        assertTrue(lessonDao.listInstancesForSeries(rule.id).isEmpty())
    }
}

private class FakeLessonDao : LessonDao {
    private val idSeq = AtomicLong(1)
    private val lessons = mutableMapOf<Long, Lesson>()
    private val students = mutableMapOf<Long, Student>()
    private val subjects = mutableMapOf<Long, SubjectPreset>()
    private val payments = mutableMapOf<Long, MutableList<Payment>>()
    private val state = MutableStateFlow<List<Lesson>>(emptyList())

    fun registerStudent(student: Student) {
        students[student.id] = student
    }

    fun registerSubject(subject: SubjectPreset) {
        subjects[subject.id] = subject
    }

    private fun emit() {
        state.value = lessons.values.sortedBy { it.startAt }
    }

    override fun observeInRange(from: Instant, to: Instant): Flow<List<LessonWithStudent>> =
        state.map { list ->
            list.filter { it.startAt >= from && it.startAt < to }
                .mapNotNull { lesson -> toLessonWithStudent(lesson) }
        }

    override fun observeOutstanding(before: Instant, statuses: List<PaymentStatus>): Flow<List<LessonWithStudent>> =
        state.map { emptyList() }

    override fun observeLessonCounts(
        from: Instant,
        to: Instant,
        paidStatus: PaymentStatus,
        outstandingStatuses: List<PaymentStatus>
    ): Flow<com.tutorly.data.db.dao.LessonCountTuple> =
        MutableStateFlow(
            com.tutorly.data.db.dao.LessonCountTuple(0, 0, 0)
        )

    override fun observeByStudent(studentId: Long): Flow<List<Lesson>> =
        state.map { list -> list.filter { it.studentId == studentId } }

    override fun observeRecentWithSubject(studentId: Long, limit: Int): Flow<List<com.tutorly.data.db.projections.LessonWithSubject>> =
        state.map { emptyList() }

    override suspend fun findById(id: Long): Lesson? = lessons[id]

    override suspend fun findByIdWithStudent(id: Long): LessonWithStudent? =
        lessons[id]?.let { toLessonWithStudent(it) }

    override suspend fun findOverlapping(start: Instant, end: Instant): List<LessonWithStudent> =
        lessons.values
            .filter { it.startAt < end && it.endAt > start }
            .mapNotNull { toLessonWithStudent(it) }

    override suspend fun findLatestForStudent(studentId: Long): LessonWithStudent? =
        lessons.values
            .filter { it.studentId == studentId }
            .maxByOrNull { it.startAt }
            ?.let { toLessonWithStudent(it) }

    override fun observeById(id: Long): Flow<LessonWithStudent?> =
        state.map { lessons[id]?.let { toLessonWithStudent(it) } }

    override suspend fun listByStudentAscending(studentId: Long): List<Lesson> =
        lessons.values
            .filter { it.studentId == studentId }
            .sortedBy { it.startAt }

    override suspend fun listInstancesForSeries(seriesId: Long): List<Lesson> =
        lessons.values
            .filter { it.seriesId == seriesId && it.isInstance }
            .sortedBy { it.startAt }

    override suspend fun deleteInstancesForSeries(seriesId: Long) {
        val ids = lessons.filter { it.value.seriesId == seriesId && it.value.isInstance }.keys
        ids.forEach { lessons.remove(it) }
        emit()
    }

    override suspend fun upsert(lesson: Lesson): Long {
        val id = if (lesson.id == 0L) idSeq.getAndIncrement() else lesson.id
        lessons[id] = lesson.copy(id = id)
        emit()
        return id
    }

    override suspend fun updatePayment(
        id: Long,
        status: PaymentStatus,
        paid: Int,
        now: Instant,
        markedAt: Instant?
    ) {
        lessons[id]?.let {
            lessons[id] = it.copy(paymentStatus = status, paidCents = paid, updatedAt = now)
            emit()
        }
    }

    override suspend fun updateNote(id: Long, note: String?, now: Instant) {
        lessons[id]?.let {
            lessons[id] = it.copy(note = note, updatedAt = now)
            emit()
        }
    }

    override suspend fun deleteById(id: Long) {
        lessons.remove(id)
        emit()
    }

    private fun toLessonWithStudent(lesson: Lesson): LessonWithStudent? {
        val student = students[lesson.studentId] ?: return null
        val subject = lesson.subjectId?.let { subjects[it] }
        val lessonPayments = payments[lesson.id]?.toList() ?: emptyList()
        return LessonWithStudent(
            lesson = lesson,
            student = student,
            subject = subject,
            payments = lessonPayments
        )
    }
}

private class FakePaymentDao : PaymentDao {
    override fun observeTotalInRange(from: Instant, to: Instant, status: PaymentStatus): Flow<Long> =
        MutableStateFlow(0L)

    override suspend fun getByStudent(studentId: Long): List<Payment> = emptyList()

    override suspend fun totalPrepayment(studentId: Long, status: PaymentStatus): Long = 0L

    override suspend fun insert(payment: Payment): Long = 0L

    override suspend fun update(payment: Payment) {}

    override suspend fun delete(payment: Payment) {}

    override suspend fun findByLesson(lessonId: Long): Payment? = null

    override suspend fun upsert(payment: Payment): Long = 0L

    override fun observePaymentsForStudent(studentId: Long): Flow<List<Payment>> =
        MutableStateFlow(emptyList())
}

private class FakeRecurrenceRuleDao : RecurrenceRuleDao {
    private val idSeq = AtomicLong(1)
    private val rules = mutableMapOf<Long, RecurrenceRule>()
    private val state = MutableStateFlow<List<RecurrenceRule>>(emptyList())

    override suspend fun upsert(rule: RecurrenceRule): Long {
        val id = if (rule.id == 0L) idSeq.getAndIncrement() else rule.id
        rules[id] = rule.copy(id = id)
        state.value = rules.values.sortedBy { it.id }
        return id
    }

    override suspend fun findIntersecting(rangeStart: Instant?, rangeEnd: Instant): List<RecurrenceRule> =
        rules.values.toList()

    override suspend fun findById(id: Long): RecurrenceRule? = rules[id]

    override suspend fun findByBaseLessonId(baseLessonId: Long): RecurrenceRule? =
        rules.values.firstOrNull { it.baseLessonId == baseLessonId }

    override fun observeAll(): Flow<List<RecurrenceRule>> = state

    override suspend fun deleteById(id: Long) {
        rules.remove(id)
        state.value = rules.values.toList()
    }
}

private class FakeRecurrenceExceptionDao : RecurrenceExceptionDao {
    private val exceptions = mutableListOf<RecurrenceException>()
    private val state = MutableStateFlow<List<RecurrenceException>>(emptyList())

    override suspend fun upsert(exception: RecurrenceException): Long {
        exceptions.removeAll { it.seriesId == exception.seriesId && it.originalDateTime == exception.originalDateTime }
        exceptions += exception
        state.value = exceptions.toList()
        return exception.id.takeIf { it != 0L } ?: 1L
    }

    override suspend fun deleteInstance(seriesId: Long, originalDateTime: Instant) {
        exceptions.removeAll { it.seriesId == seriesId && it.originalDateTime == originalDateTime }
        state.value = exceptions.toList()
    }

    override suspend fun deleteForSeries(seriesId: Long) {
        exceptions.removeAll { it.seriesId == seriesId }
        state.value = exceptions.toList()
    }

    override fun observeAll(): Flow<List<RecurrenceException>> = state
}

