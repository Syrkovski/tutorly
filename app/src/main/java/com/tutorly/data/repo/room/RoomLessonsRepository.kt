package com.tutorly.data.repo.room

import com.tutorly.data.db.projections.LessonWithStudent
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.data.db.dao.LessonDao
import com.tutorly.models.Lesson
import com.tutorly.models.PaymentStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomLessonsRepository(
    private val dao: LessonDao
) : LessonsRepository {
    override fun observeInRange(from: Instant, to: Instant): Flow<List<LessonDetails>> =
        dao.observeInRange(from, to).map { lessons -> lessons.map { it.toLessonDetails() } }
    override fun observeByStudent(studentId: Long): Flow<List<Lesson>> = dao.observeByStudent(studentId)
    override suspend fun upsert(lesson: Lesson): Long = dao.upsert(lesson)
    override suspend fun markPayment(id: Long, status: PaymentStatus, paidCents: Int) =
        dao.updatePayment(id, status, paidCents, Instant.now())

    override suspend fun saveNote(id: Long, note: String?) =
        dao.updateNote(id, note, Instant.now())
}

private fun LessonWithStudent.toLessonDetails(): LessonDetails =
    LessonDetails(lesson = lesson, student = student, subject = subject, payments = payments)
