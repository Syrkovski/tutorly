package com.tutorly.data.repo.room

import com.tutorly.data.db.dao.PaymentDao
import com.tutorly.domain.repo.PaymentsRepository
import com.tutorly.models.Payment
import com.tutorly.models.PaymentStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant

@Singleton
class RoomPaymentsRepository @Inject constructor(
    private val paymentDao: PaymentDao,
    private val prepaymentAllocator: StudentPrepaymentAllocator
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

    override fun observePaymentsInRange(from: Instant, to: Instant): Flow<List<Payment>> =
        paymentDao.observePaymentsInRange(from, to, PaymentStatus.PAID)

    override fun observePrepaymentBalance(): Flow<Long> =
        combine(
            paymentDao.observeTotalPrepaymentDeposits(PaymentStatus.PAID),
            paymentDao.observeTotalPrepaymentAllocations(PaymentStatus.PAID, PREPAYMENT_METHOD)
        ) { deposits, allocations -> deposits - allocations }

    override suspend fun insert(payment: Payment): Long =
        paymentDao.insert(payment)

    override suspend fun update(payment: Payment) =
        paymentDao.update(payment)

    override suspend fun delete(payment: Payment) =
        paymentDao.delete(payment)

    override suspend fun applyPrepayment(studentId: Long) {
        prepaymentAllocator.sync(studentId)
    }
}
