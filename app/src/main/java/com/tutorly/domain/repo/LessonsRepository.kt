package com.tutorly.domain.repo

import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.model.LessonsRangeStats
import com.tutorly.models.Lesson
import com.tutorly.models.PaymentStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface LessonsRepository {
    fun observeInRange(from: Instant, to: Instant): Flow<List<LessonDetails>>
    fun observeStatsInRange(from: Instant, to: Instant): Flow<LessonsRangeStats>
    fun observeByStudent(studentId: Long): Flow<List<Lesson>>
    suspend fun upsert(lesson: Lesson): Long
    suspend fun markPayment(id: Long, status: PaymentStatus, paidCents: Int)
    suspend fun saveNote(id: Long, note: String?)
}
