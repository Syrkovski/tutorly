package com.tutorly.data.repo.room

import com.tutorly.data.db.dao.LessonDao
import com.tutorly.data.db.dao.PaymentDao
import com.tutorly.models.Lesson
import com.tutorly.models.LessonStatus
import com.tutorly.models.Payment
import com.tutorly.models.PaymentStatus
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

internal const val PREPAYMENT_METHOD = "prepayment"

@Singleton
internal class StudentPrepaymentAllocator @Inject constructor(
    private val lessonDao: LessonDao,
    private val paymentDao: PaymentDao
) {
    suspend fun sync(studentId: Long) {
        val lessons = lessonDao.listByStudentAscending(studentId)
        if (lessons.isEmpty()) return

        val payments = paymentDao.getByStudent(studentId)
        if (payments.isEmpty()) {
            allocateFromDeposits(studentId, lessons, emptyMap())
        } else {
            val paymentsByLesson = payments
                .filter { it.lessonId != null }
                .associateBy { it.lessonId!! }
            allocateFromDeposits(studentId, lessons, paymentsByLesson)
        }
    }

    private suspend fun allocateFromDeposits(
        studentId: Long,
        lessons: List<Lesson>,
        paymentsByLesson: Map<Long, Payment>
    ) {
        val depositTotal = paymentDao.totalPrepayment(studentId, PaymentStatus.PAID)
        var remaining = depositTotal
        val referenceTime = Instant.now()

        lessons.forEach { lesson ->
            val payment = paymentsByLesson[lesson.id]

            if (lesson.status == LessonStatus.CANCELED) {
                if (payment?.method == PREPAYMENT_METHOD && payment.status == PaymentStatus.PAID) {
                    revertPrepayment(lesson, payment, referenceTime)
                }
                return@forEach
            }

            if (payment != null && payment.status == PaymentStatus.PAID && payment.method != PREPAYMENT_METHOD) {
                return@forEach
            }

            val price = lesson.priceCents
            if (price <= 0) {
                if (payment?.method == PREPAYMENT_METHOD && payment.status == PaymentStatus.PAID) {
                    revertPrepayment(lesson, payment, referenceTime)
                }
                return@forEach
            }

            val priceLong = price.toLong()
            if (remaining >= priceLong) {
                remaining -= priceLong
                ensurePrepaid(lesson, payment, referenceTime)
            } else {
                if (payment?.method == PREPAYMENT_METHOD && payment.status == PaymentStatus.PAID) {
                    revertPrepayment(lesson, payment, referenceTime)
                }
            }
        }
    }

    private suspend fun ensurePrepaid(
        lesson: Lesson,
        payment: Payment?,
        referenceTime: Instant
    ) {
        if (lesson.paymentStatus != PaymentStatus.PAID || lesson.paidCents != lesson.priceCents) {
            lessonDao.updatePayment(
                id = lesson.id,
                status = PaymentStatus.PAID,
                paid = lesson.priceCents,
                now = referenceTime,
                markedAt = referenceTime
            )
        }

        val updated = (payment ?: Payment(
            lessonId = lesson.id,
            studentId = lesson.studentId,
            amountCents = lesson.priceCents,
            status = PaymentStatus.PAID
        )).copy(
            amountCents = lesson.priceCents,
            status = PaymentStatus.PAID,
            method = PREPAYMENT_METHOD,
            at = referenceTime
        )

        if (payment == null) {
            paymentDao.insert(updated)
        } else {
            paymentDao.update(updated)
        }
    }

    private suspend fun revertPrepayment(
        lesson: Lesson,
        payment: Payment,
        referenceTime: Instant
    ) {
        val isPastLesson = lesson.startAt.isBefore(referenceTime)
        val status = if (isPastLesson) PaymentStatus.DUE else PaymentStatus.UNPAID
        val markedAt = if (status == PaymentStatus.UNPAID) null else referenceTime

        if (lesson.paymentStatus != status || lesson.paidCents != 0) {
            lessonDao.updatePayment(
                id = lesson.id,
                status = status,
                paid = 0,
                now = referenceTime,
                markedAt = markedAt
            )
        }

        paymentDao.delete(payment)
    }
}
