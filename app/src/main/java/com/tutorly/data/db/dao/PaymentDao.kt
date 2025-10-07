package com.tutorly.data.db.dao

import androidx.room.*
import com.tutorly.models.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE studentId = :studentId ORDER BY at DESC")
    fun observePaymentsByStudent(studentId: Long): Flow<List<Payment>>

    @Query("""
        SELECT EXISTS(
          SELECT 1 FROM payments
          WHERE studentId = :studentId AND status = 'DUE'
        )
    """)
    fun observeHasDebt(studentId: Long): Flow<Boolean>

    @Query("""
        SELECT COALESCE(SUM(amountCents), 0)
        FROM payments
        WHERE studentId = :studentId AND status = 'DUE'
    """)
    fun observeTotalDebt(studentId: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: Payment): Long

    @Update
    suspend fun update(payment: Payment)

    @Delete
    suspend fun delete(payment: Payment)
}




//@Dao
//interface PaymentDao {
//
//    @Query("SELECT * FROM payments WHERE studentId = :studentId ORDER BY at DESC")
//    suspend fun getByStudent(studentId: Long): List<Payment>
//
//    @Query("SELECT * FROM payments WHERE lessonId = :lessonId")
//    suspend fun getByLesson(lessonId: Long): List<Payment>
//
//    @Query("SELECT SUM(amountCents) FROM payments WHERE at >= :from AND at < :to")
//    suspend fun totalAmountInRange(from: Instant, to: Instant): Int?
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun upsert(payment: Payment): Long
//
//    @Delete
//    suspend fun delete(payment: Payment)
//}
