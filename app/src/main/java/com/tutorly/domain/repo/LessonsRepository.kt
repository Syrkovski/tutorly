package com.tutorly.domain.repo

import com.tutorly.models.Lesson
import com.tutorly.models.PaymentStatus
import java.time.Instant

interface LessonsRepository {
    suspend fun inRange(from: Instant, to: Instant): List<Lesson>
    suspend fun upsert(lesson: Lesson): Long
    suspend fun markPayment(id: Long, status: PaymentStatus, paidCents: Int)
    suspend fun saveNote(id: Long, note: String?)
}
