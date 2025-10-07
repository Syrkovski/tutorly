package com.tutorly.domain.repo

import com.tutorly.models.Payment
import kotlinx.coroutines.flow.Flow

interface PaymentsRepository {
    fun observePaymentsByStudent(studentId: Long): Flow<List<Payment>>
    fun observeHasDebt(studentId: Long): Flow<Boolean>
    fun observeTotalDebt(studentId: Long): Flow<Long>

    suspend fun insert(payment: Payment): Long
    suspend fun update(payment: Payment)
    suspend fun delete(payment: Payment)
}