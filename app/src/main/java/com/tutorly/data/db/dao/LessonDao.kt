package com.tutorly.data.db.dao

import androidx.room.*
import com.tutorly.data.db.projections.LessonWithStudent
import com.tutorly.models.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface LessonDao {
    @Transaction
    @Query("SELECT * FROM lessons WHERE startAt >= :from AND startAt < :to ORDER BY startAt")
    fun observeInRange(from: Instant, to: Instant): Flow<List<LessonWithStudent>>

    @Query(
        """
        SELECT
            COUNT(*) AS totalLessons,
            COALESCE(SUM(CASE WHEN paymentStatus = :paidStatus THEN 1 ELSE 0 END), 0) AS paidLessons,
            COALESCE(SUM(CASE WHEN paymentStatus IN (:outstandingStatuses) THEN 1 ELSE 0 END), 0) AS debtLessons
        FROM lessons
        WHERE startAt >= :from AND startAt < :to
        """
    )
    fun observeLessonCounts(
        from: Instant,
        to: Instant,
        paidStatus: PaymentStatus,
        outstandingStatuses: List<PaymentStatus>
    ): Flow<LessonCountTuple>

    @Query("SELECT * FROM lessons WHERE studentId = :studentId ORDER BY startAt DESC")
    fun observeByStudent(studentId: Long): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Lesson?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(lesson: Lesson): Long

    @Query("UPDATE lessons SET paymentStatus=:status, paidCents=:paid, updatedAt=:now WHERE id=:id")
    suspend fun updatePayment(id: Long, status: PaymentStatus, paid: Int, now: Instant)

    @Query("UPDATE lessons SET note=:note, updatedAt=:now WHERE id=:id")
    suspend fun updateNote(id: Long, note: String?, now: Instant)
}

data class LessonCountTuple(
    val totalLessons: Int,
    val paidLessons: Int,
    val debtLessons: Int
)


//@Dao
//interface LessonDao {
//    @Transaction
//    @Query("SELECT * FROM lessons WHERE startAt >= :from AND startAt < :to ORDER BY startAt")
//    suspend fun lessonsInRange(from: Instant, to: Instant): List<LessonWithStudent>
//
//    @Transaction
//    @Query("SELECT * FROM lessons WHERE DATE(startAt/1000,'unixepoch') = DATE(:day/1000,'unixepoch') ORDER BY startAt")
//    suspend fun lessonsOnDay(day: Instant): List<LessonWithStudent>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun upsert(lesson: Lesson): Long
//
//    @Query("UPDATE lessons SET paymentStatus=:status, paidCents=:paid, updatedAt=:now WHERE id=:lessonId")
//    suspend fun updatePayment(lessonId: Long, status: PaymentStatus, paid: Int, now: Instant)
//}