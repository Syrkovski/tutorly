package com.tutorly.domain.repo

import com.tutorly.models.Payment
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface PaymentsRepository {
    fun observePaymentsByStudent(studentId: Long): Flow<List<Payment>>
    suspend fun hasDebt(studentId: Long): Boolean
    fun observeHasDebt(studentId: Long): Flow<Boolean>
    suspend fun totalDebt(studentId: Long): Long
    fun observeTotalDebt(studentId: Long): Flow<Long>

    fun observePaymentsInRange(from: Instant, to: Instant): Flow<List<Payment>>
    fun observePrepaymentBalance(): Flow<Long>

    suspend fun insert(payment: Payment): Long
    suspend fun update(payment: Payment)
    suspend fun delete(payment: Payment)

    suspend fun applyPrepayment(studentId: Long)
}
