package com.tutorly.data.repo.room

import com.tutorly.data.db.dao.LessonCountTuple
import com.tutorly.data.db.dao.LessonDao
import com.tutorly.data.db.dao.PaymentDao
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomLessonsRepository(
    private val lessonDao: LessonDao,
    private val paymentDao: PaymentDao
) : LessonsRepository {
    override fun observeLessons(from: Instant, to: Instant): Flow<List<LessonDetails>> =
        lessonDao.observeInRange(from, to).map { lessons -> lessons.map { it.toLessonDetails() } }

    override fun observeTodayLessons(dayStart: Instant, dayEnd: Instant): Flow<List<LessonForToday>> =
        lessonDao.observeInRange(dayStart, dayEnd).map { lessons -> lessons.map { it.toLessonForToday() } }

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

    override suspend fun upsert(lesson: Lesson): Long = lessonDao.upsert(lesson)

    override suspend fun create(request: LessonCreateRequest): Long {
        val now = Instant.now()
        val lesson = Lesson(
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
        return lessonDao.upsert(lesson)
    }

    override suspend fun findConflicts(start: Instant, end: Instant): List<LessonDetails> {
        return lessonDao.findOverlapping(start, end).map { it.toLessonDetails() }
    }

    override suspend fun latestLessonForStudent(studentId: Long): Lesson? {
        return lessonDao.findLatestForStudent(studentId)?.lesson
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
}
