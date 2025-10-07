package com.tutorly.data.db.dao

import androidx.room.*
import com.tutorly.models.*
import java.time.Instant

@Dao
interface LessonDao {
    @Transaction
    @Query("SELECT * FROM lessons WHERE startAt >= :from AND startAt < :to ORDER BY startAt")
    suspend fun inRange(from: Instant, to: Instant): List<Lesson>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(lesson: Lesson): Long

    @Query("UPDATE lessons SET paymentStatus=:status, paidCents=:paid, updatedAt=:now WHERE id=:id")
    suspend fun updatePayment(id: Long, status: PaymentStatus, paid: Int, now: Instant)

    @Query("UPDATE lessons SET note=:note, updatedAt=:now WHERE id=:id")
    suspend fun updateNote(id: Long, note: String?, now: Instant)
}


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