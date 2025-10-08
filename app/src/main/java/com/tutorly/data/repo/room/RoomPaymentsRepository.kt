package com.tutorly.data.repo.room

import com.tutorly.data.db.dao.PaymentDao
import com.tutorly.domain.repo.PaymentsRepository
import com.tutorly.models.Payment
import com.tutorly.models.PaymentStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class RoomPaymentsRepository @Inject constructor(
    private val paymentDao: PaymentDao
) : PaymentsRepository {

    override fun observePaymentsByStudent(studentId: Long): Flow<List<Payment>> =
        paymentDao.observePaymentsByStudent(studentId)

    override suspend fun hasDebt(studentId: Long): Boolean =
        paymentDao.hasDebt(studentId, PaymentStatus.outstandingStatuses)

    override fun observeHasDebt(studentId: Long): Flow<Boolean> =
        paymentDao.observeHasDebt(studentId, PaymentStatus.outstandingStatuses)

    override suspend fun totalDebt(studentId: Long): Long =
        paymentDao.totalDebt(studentId, PaymentStatus.outstandingStatuses)

    override fun observeTotalDebt(studentId: Long): Flow<Long> =
        paymentDao.observeTotalDebt(studentId, PaymentStatus.outstandingStatuses)

    override suspend fun insert(payment: Payment): Long =
        paymentDao.insert(payment)

    override suspend fun update(payment: Payment) =
        paymentDao.update(payment)

    override suspend fun delete(payment: Payment) =
        paymentDao.delete(payment)
}
