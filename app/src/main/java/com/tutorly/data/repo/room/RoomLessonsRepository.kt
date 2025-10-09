package com.tutorly.data.repo.room

import com.tutorly.data.db.dao.LessonDao
import com.tutorly.data.db.dao.PaymentDao
import com.tutorly.data.db.dao.LessonCountTuple
import com.tutorly.data.db.projections.LessonWithStudent
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.model.LessonsRangeStats
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.models.Lesson
import com.tutorly.models.PaymentStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomLessonsRepository(
    private val lessonDao: LessonDao,
    private val paymentDao: PaymentDao
) : LessonsRepository {
    override fun observeInRange(from: Instant, to: Instant): Flow<List<LessonDetails>> =
        lessonDao.observeInRange(from, to).map { lessons -> lessons.map { it.toLessonDetails() } }

    override fun observeStatsInRange(from: Instant, to: Instant): Flow<LessonsRangeStats> =
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

    override fun observeByStudent(studentId: Long): Flow<List<Lesson>> = lessonDao.observeByStudent(studentId)

    override suspend fun upsert(lesson: Lesson): Long = lessonDao.upsert(lesson)
    override suspend fun markPayment(id: Long, status: PaymentStatus, paidCents: Int) =
        lessonDao.updatePayment(id, status, paidCents, Instant.now())

    override suspend fun saveNote(id: Long, note: String?) =
        lessonDao.updateNote(id, note, Instant.now())
}

private fun LessonWithStudent.toLessonDetails(): LessonDetails =
    LessonDetails(lesson = lesson, student = student, subject = subject, payments = payments)
