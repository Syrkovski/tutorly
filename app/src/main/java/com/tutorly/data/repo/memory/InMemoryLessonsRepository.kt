package com.tutorly.data.repo.memory

import com.tutorly.models.Lesson
import com.tutorly.models.PaymentStatus
import com.tutorly.domain.repo.LessonsRepository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class InMemoryLessonsRepository : LessonsRepository {
    private val seq = AtomicLong(1)
    private val store = ConcurrentHashMap<Long, Lesson>()

    override suspend fun inRange(from: Instant, to: Instant) =
        store.values.filter { it.startAt >= from && it.startAt < to }.sortedBy { it.startAt }

    override suspend fun upsert(lesson: Lesson): Long {
        val id = if (lesson.id == 0L) seq.getAndIncrement() else lesson.id
        store[id] = lesson.copy(id = id)
        return id
    }

    override suspend fun markPayment(id: Long, status: PaymentStatus, paidCents: Int) {
        store[id]?.let { store[id] = it.copy(paymentStatus = status, paidCents = paidCents) }
    }

    override suspend fun saveNote(id: Long, note: String?) {
        store[id]?.let { store[id] = it.copy(note = note) }
    }
}
