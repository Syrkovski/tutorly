package com.tutorly.data.db.dao

import androidx.room.*
import com.tutorly.models.Payment
import com.tutorly.models.PaymentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE studentId = :studentId ORDER BY at DESC")
    fun observePaymentsByStudent(studentId: Long): Flow<List<Payment>>

    @Query("""
        SELECT EXISTS(
          SELECT 1 FROM payments
          WHERE studentId = :studentId AND status IN (:statuses)
        )
    """)
    suspend fun hasDebt(studentId: Long, statuses: List<PaymentStatus>): Boolean

    @Query("""
        SELECT EXISTS(
          SELECT 1 FROM payments
          WHERE studentId = :studentId AND status IN (:statuses)
        )
    """)
    fun observeHasDebt(studentId: Long, statuses: List<PaymentStatus>): Flow<Boolean>

    @Query("""
        SELECT COALESCE(SUM(amountCents), 0)
        FROM payments
        WHERE studentId = :studentId AND status IN (:statuses)
    """)
    suspend fun totalDebt(studentId: Long, statuses: List<PaymentStatus>): Long

    @Query("""
        SELECT COALESCE(SUM(amountCents), 0)
        FROM payments
        WHERE studentId = :studentId AND status IN (:statuses)
    """)
    fun observeTotalDebt(studentId: Long, statuses: List<PaymentStatus>): Flow<Long>

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
