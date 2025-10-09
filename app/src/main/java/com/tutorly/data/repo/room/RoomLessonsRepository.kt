package com.tutorly.data.repo.room

import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.data.db.dao.LessonDao
import com.tutorly.models.Lesson
import com.tutorly.models.PaymentStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class RoomLessonsRepository(
    private val dao: LessonDao
) : LessonsRepository {
    override suspend fun inRange(from: Instant, to: Instant): List<Lesson> = dao.inRange(from, to)
    override fun observeByStudent(studentId: Long): Flow<List<Lesson>> = dao.observeByStudent(studentId)
    override suspend fun upsert(lesson: Lesson): Long = dao.upsert(lesson)
    override suspend fun markPayment(id: Long, status: PaymentStatus, paidCents: Int) =
        dao.updatePayment(id, status, paidCents, Instant.now())

    override suspend fun saveNote(id: Long, note: String?) =
        dao.updateNote(id, note, Instant.now())
}
