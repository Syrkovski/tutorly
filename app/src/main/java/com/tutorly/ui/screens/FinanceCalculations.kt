package com.tutorly.ui.screens

import com.tutorly.domain.model.LessonDetails
import com.tutorly.models.LessonStatus
import com.tutorly.models.Payment
import com.tutorly.models.PaymentStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.WeekFields
import kotlin.math.roundToLong

data class FinanceTemporalContext(
    val now: ZonedDateTime,
    val zoneId: ZoneId,
    val weekFields: WeekFields
)

internal data class FinancePeriodBounds(
    val start: Instant,
    val end: Instant
)

internal fun FinancePeriod.bounds(
    context: FinanceTemporalContext,
    offset: Int = 0
): FinancePeriodBounds {
    val zone = context.zoneId
    return when (this) {
        FinancePeriod.WEEK -> {
            val baseDate = context.now.toLocalDate().with(context.weekFields.dayOfWeek(), 1)
            val startDate = baseDate.plusWeeks(offset.toLong())
            val start = startDate.atStartOfDay(zone)
            FinancePeriodBounds(start = start.toInstant(), end = start.plusWeeks(1).toInstant())
        }

        FinancePeriod.MONTH -> {
            val baseDate = context.now.toLocalDate().withDayOfMonth(1)
            val startDate = baseDate.plusMonths(offset.toLong())
            val start = startDate.atStartOfDay(zone)
            FinancePeriodBounds(start = start.toInstant(), end = start.plusMonths(1).toInstant())
        }
    }
}

internal fun calculateFinanceSummary(
    lessons: List<LessonDetails>,
    payments: List<Payment>,
    bounds: FinancePeriodBounds,
    accountsReceivableRubles: Long,
    prepaymentsRubles: Long
): FinanceSummary {
    val periodLessons = lessons.filter { lesson ->
        !lesson.startAt.isBefore(bounds.start) && lesson.startAt.isBefore(bounds.end)
    }

    val cashInCents = payments
        .filter { payment ->
            !payment.at.isBefore(bounds.start) && payment.at.isBefore(bounds.end) &&
                payment.status == PaymentStatus.PAID
        }
        .sumOf { it.amountCents.toLong() }

    val totalDurationMinutes = periodLessons.fold(0L) { acc, lesson ->
        val lessonMinutes = lesson.duration.toMinutes().coerceAtLeast(0)
        acc + lessonMinutes
    }.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()

    val totalLessons = periodLessons.size
    val conducted = periodLessons.count { it.lessonStatus == LessonStatus.DONE }
    val cancelled = periodLessons.count { it.lessonStatus == LessonStatus.CANCELED }

    return FinanceSummary(
        cashIn = centsToRubles(cashInCents),
        accountsReceivable = accountsReceivableRubles,
        prepayments = prepaymentsRubles,
        lessons = FinanceLessonsSummary(
            total = totalLessons,
            conducted = conducted,
            cancelled = cancelled
        ),
        totalDurationMinutes = totalDurationMinutes
    )
}

internal fun calculateFinanceChart(
    lessons: List<LessonDetails>,
    bounds: FinancePeriodBounds,
    now: Instant,
    zoneId: ZoneId
): List<FinanceChartPoint> {
    val accruedByDate = lessons
        .asSequence()
        .filter { lesson ->
            !lesson.startAt.isBefore(bounds.start) &&
                lesson.startAt.isBefore(bounds.end) &&
                !lesson.startAt.isAfter(now) &&
                lesson.paymentStatus != PaymentStatus.CANCELLED &&
                lesson.lessonStatus != LessonStatus.CANCELED
        }
        .groupBy { lesson -> lesson.startAt.atZone(zoneId).toLocalDate() }
        .mapValues { (_, items) -> centsToRubles(items.sumOf { it.priceCents.toLong() }) }

    val startDate = bounds.start.atZone(zoneId).toLocalDate()
    val endExclusive = bounds.end.atZone(zoneId).toLocalDate()

    val dates = mutableListOf<LocalDate>()
    var cursor = startDate
    while (cursor.isBefore(endExclusive)) {
        dates.add(cursor)
        cursor = cursor.plusDays(1)
    }

    if (dates.isEmpty()) {
        dates.add(startDate)
    }

    return dates.map { date ->
        FinanceChartPoint(
            date = date,
            amount = accruedByDate[date] ?: 0
        )
    }
}

internal fun centsToRubles(value: Long): Long = (value / 100.0).roundToLong()

internal fun LessonDetails.outstandingAmountCents(): Long {
    if (paymentStatus !in PaymentStatus.outstandingStatuses) return 0
    return (priceCents - paidCents).coerceAtLeast(0).toLong()
}
