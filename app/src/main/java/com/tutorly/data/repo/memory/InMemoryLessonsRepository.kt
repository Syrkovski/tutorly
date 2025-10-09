package com.tutorly.data.repo.memory

import com.tutorly.domain.model.LessonCreateRequest
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.model.LessonsRangeStats
import com.tutorly.domain.model.asIcon
import com.tutorly.domain.model.resolveDuration
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.models.Lesson
import com.tutorly.models.Payment
import com.tutorly.models.PaymentStatus
import com.tutorly.models.LessonStatus
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
    private val payments = ConcurrentHashMap<Long, Payment>()

    override fun observeLessons(from: Instant, to: Instant): Flow<List<LessonDetails>> =
        lessonsFlow.map { lessons ->
            lessons.filter { it.startAt >= from && it.startAt < to }
                .sortedBy { it.startAt }
                .map { it.toDetailsStub() }
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

    override suspend fun upsert(lesson: Lesson): Long {
        val id = if (lesson.id == 0L) seq.getAndIncrement() else lesson.id
        store[id] = lesson.copy(id = id)
        emit()
        return id
    }

    override suspend fun create(request: LessonCreateRequest): Long {
        val id = seq.getAndIncrement()
        val now = Instant.now()
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
            status = LessonStatus.PLANNED,
            note = request.note,
            createdAt = now,
            updatedAt = now
        )
        store[id] = newLesson
        emit()
        return id
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

    override suspend fun markPaid(id: Long) {
        store[id]?.let { lesson ->
            val updatedLesson = lesson.copy(
                paymentStatus = PaymentStatus.PAID,
                paidCents = lesson.priceCents
            )
            store[id] = updatedLesson
            val now = Instant.now()
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
            val updatedLesson = lesson.copy(
                paymentStatus = PaymentStatus.DUE,
                paidCents = 0
            )
            store[id] = updatedLesson
            val now = Instant.now()
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

    private fun emit() {
        lessonsFlow.value = store.values.sortedByDescending { it.startAt }
    }
}

private fun Lesson.toDetailsStub(): LessonDetails {
    val duration = resolveDuration(startAt, endAt, null)
    val normalizedEnd = startAt.plus(duration)

    return LessonDetails(
        id = id,
        studentId = studentId,
        startAt = startAt,
        endAt = normalizedEnd,
        duration = duration,
        studentName = "Student #$studentId",
        studentNote = null,
        subjectName = null,
        subjectColorArgb = null,
        paymentStatus = paymentStatus,
        paymentStatusIcon = paymentStatus.asIcon(),
        priceCents = priceCents,
        paidCents = paidCents,
        lessonTitle = title,
        lessonNote = note
    )
}
