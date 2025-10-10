package com.tutorly.domain.repo

import com.tutorly.domain.model.LessonCreateRequest
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.model.LessonsRangeStats
import com.tutorly.models.Lesson
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface LessonsRepository {
    fun observeLessons(from: Instant, to: Instant): Flow<List<LessonDetails>>
    fun observeWeekStats(from: Instant, to: Instant): Flow<LessonsRangeStats>
    fun observeLessonDetails(id: Long): Flow<LessonDetails?>
    fun observeByStudent(studentId: Long): Flow<List<Lesson>>
    suspend fun upsert(lesson: Lesson): Long
    suspend fun create(request: LessonCreateRequest): Long
    suspend fun findConflicts(start: Instant, end: Instant): List<LessonDetails>
    suspend fun latestLessonForStudent(studentId: Long): Lesson?
    suspend fun markPaid(id: Long)
    suspend fun markDue(id: Long)
    suspend fun saveNote(id: Long, note: String?)
}
