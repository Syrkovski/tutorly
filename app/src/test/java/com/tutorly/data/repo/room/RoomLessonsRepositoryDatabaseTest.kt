package com.tutorly.data.repo.room

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.tutorly.data.db.AppDatabase
import com.tutorly.data.db.dao.LessonDao
import com.tutorly.data.db.dao.PaymentDao
import com.tutorly.data.db.dao.RecurrenceExceptionDao
import com.tutorly.data.db.dao.RecurrenceRuleDao
import com.tutorly.data.db.dao.StudentDao
import com.tutorly.domain.model.LessonCreateRequest
import com.tutorly.domain.model.RecurrenceCreateRequest
import com.tutorly.models.RecurrenceFrequency
import com.tutorly.models.Student
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomLessonsRepositoryDatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var lessonDao: LessonDao
    private lateinit var ruleDao: RecurrenceRuleDao
    private lateinit var exceptionDao: RecurrenceExceptionDao
    private lateinit var paymentDao: PaymentDao
    private lateinit var studentDao: StudentDao
    private lateinit var repository: RoomLessonsRepository
    private var studentId: Long = 0

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        lessonDao = db.lessonDao()
        ruleDao = db.recurrenceRuleDao()
        exceptionDao = db.recurrenceExceptionDao()
        paymentDao = db.paymentDao()
        studentDao = db.studentDao()
        repository = RoomLessonsRepository(
            lessonDao = lessonDao,
            paymentDao = paymentDao,
            recurrenceRuleDao = ruleDao,
            recurrenceExceptionDao = exceptionDao,
            prepaymentAllocator = StudentPrepaymentAllocator(lessonDao, paymentDao),
            transactionRunner = object : TransactionRunner {
                override suspend fun <T> invoke(block: suspend () -> T): T =
                    db.withTransaction { block() }
            }
        )
        studentId = runBlocking { studentDao.insert(Student(name = "Alice")) }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createRecurringLessonPersistsRecurrenceAndRule() = runTest {
        val zone = ZoneId.of("Europe/Moscow")
        val start = ZonedDateTime.of(LocalDate.of(2024, 6, 3), LocalTime.of(10, 0), zone).toInstant()
        val end = start.plusSeconds(3_600)

        val request = LessonCreateRequest(
            studentId = studentId,
            subjectId = null,
            title = "Math",
            startAt = start,
            endAt = end,
            priceCents = 1_500,
            note = "Algebra",
            recurrence = RecurrenceCreateRequest(
                frequency = RecurrenceFrequency.WEEKLY,
                interval = 1,
                daysOfWeek = listOf(DayOfWeek.MONDAY),
                until = null,
                timezone = zone
            )
        )

        val lessonId = repository.create(request)

        val stored = lessonDao.findById(lessonId) ?: error("Lesson was not stored")
        assertNotNull("Series id should be assigned", stored.seriesId)
        val recurrence = stored.recurrence
        assertNotNull("Recurrence metadata should be persisted", recurrence)
        recurrence!!
        assertEquals(RecurrenceFrequency.WEEKLY, recurrence.frequency)
        assertEquals(1, recurrence.interval)
        assertEquals(listOf(DayOfWeek.MONDAY), recurrence.daysOfWeek)

        val rule = ruleDao.findByBaseLessonId(lessonId) ?: error("Recurrence rule missing")
        assertEquals(stored.seriesId, rule.id)
        assertEquals(lessonId, rule.baseLessonId)
        assertEquals(recurrence.frequency, rule.frequency)
        assertEquals(recurrence.interval, rule.interval)
        assertEquals(recurrence.daysOfWeek, rule.daysOfWeek)
        assertEquals(recurrence.startDateTime, rule.startDateTime)
        assertEquals(recurrence.untilDateTime, rule.untilDateTime)
        assertEquals(recurrence.timezone.id, rule.timezone)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createRecurringLessonMaterializesFutureInstances() = runTest {
        val zone = ZoneId.of("Europe/Moscow")
        val baseStart = ZonedDateTime.of(LocalDate.of(2030, 1, 6), LocalTime.of(9, 0), zone)
        val request = LessonCreateRequest(
            studentId = studentId,
            subjectId = null,
            title = "Physics",
            startAt = baseStart.toInstant(),
            endAt = baseStart.plusHours(1).toInstant(),
            priceCents = 2_000,
            note = null,
            recurrence = RecurrenceCreateRequest(
                frequency = RecurrenceFrequency.WEEKLY,
                interval = 1,
                daysOfWeek = listOf(DayOfWeek.SUNDAY),
                until = null,
                timezone = zone
            )
        )

        val lessonId = repository.create(request)

        val rule = ruleDao.findByBaseLessonId(lessonId) ?: error("Expected recurrence rule")
        val storedInstances = lessonDao.listInstancesForSeries(rule.id)
        assertTrue("Future lessons should be generated", storedInstances.isNotEmpty())
        assertTrue(storedInstances.all { it.isInstance })
        assertTrue(storedInstances.all { it.seriesId == rule.id })

        val earliest = storedInstances.minOf { it.startAt }
        val expectedFirst = baseStart.plusWeeks(1).toInstant()
        assertEquals(expectedFirst, earliest)
        assertTrue(storedInstances.all { it.recurrence == null })
    }
}
