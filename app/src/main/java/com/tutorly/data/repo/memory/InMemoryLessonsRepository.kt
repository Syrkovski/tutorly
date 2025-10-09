package com.tutorly.data.repo.memory

import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.models.Lesson
import com.tutorly.models.PaymentStatus
import com.tutorly.models.Student
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class InMemoryLessonsRepository : LessonsRepository {
    private val seq = AtomicLong(1)
    private val store = ConcurrentHashMap<Long, Lesson>()
    private val lessonsFlow = MutableStateFlow<List<Lesson>>(emptyList())

    override fun observeInRange(from: Instant, to: Instant): Flow<List<LessonDetails>> =
        lessonsFlow.map { lessons ->
            lessons.filter { it.startAt >= from && it.startAt < to }
                .sortedBy { it.startAt }
                .map { it.toDetailsStub() }
        }

    override fun observeByStudent(studentId: Long): Flow<List<Lesson>> =
        lessonsFlow.map { lessons -> lessons.filter { it.studentId == studentId } }

    override suspend fun upsert(lesson: Lesson): Long {
        val id = if (lesson.id == 0L) seq.getAndIncrement() else lesson.id
        store[id] = lesson.copy(id = id)
        emit()
        return id
    }

    override suspend fun markPayment(id: Long, status: PaymentStatus, paidCents: Int) {
        store[id]?.let {
            store[id] = it.copy(paymentStatus = status, paidCents = paidCents)
            emit()
        }
    }

    override suspend fun saveNote(id: Long, note: String?) {
        store[id]?.let {
            store[id] = it.copy(note = note)
            emit()
        }
    }

    private fun emit() {
        lessonsFlow.value = store.values.sortedByDescending { it.startAt }
    }
}

private fun Lesson.toDetailsStub(): LessonDetails = LessonDetails(
    lesson = this,
    student = Student(
        id = studentId,
        name = "Student #$studentId",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH
    ),
    subject = null,
    payments = emptyList()
)
