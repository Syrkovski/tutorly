package com.tutorly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.PaymentsRepository
import com.tutorly.models.LessonStatus
import com.tutorly.models.Payment
import com.tutorly.models.PaymentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.WeekFields

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val lessonsRepository: LessonsRepository,
    private val paymentsRepository: PaymentsRepository
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val weekFields: WeekFields = WeekFields.ISO

    private val observationBounds: ObservationBounds = buildObservationBounds()

    private val lessonsFlow = lessonsRepository.observeLessons(observationBounds.start, observationBounds.end)
    private val outstandingLessonsFlow = lessonsRepository.observeOutstandingLessonDetails(observationBounds.end)
    private val paymentsFlow = paymentsRepository.observePaymentsInRange(observationBounds.start, observationBounds.end)
    private val prepaymentsFlow = paymentsRepository.observePrepaymentBalance()

    val uiState: StateFlow<FinanceUiState> = combine(
        lessonsFlow,
        outstandingLessonsFlow,
        paymentsFlow,
        prepaymentsFlow
    ) { lessons, outstandingLessons, payments, prepaymentBalanceCents ->
        val now = ZonedDateTime.now(zoneId)
        val periodBounds = FinancePeriod.entries.associateWith { it.bounds(now, weekFields) }
        val currentOutstanding = outstandingLessons.filter { lesson ->
            !lesson.startAt.isAfter(now.toInstant())
        }
        val accountsReceivableRubles = centsToRubles(
            currentOutstanding.sumOf { lesson -> lesson.outstandingAmountCents() }
        )
        val prepaymentsRubles = centsToRubles(prepaymentBalanceCents.coerceAtLeast(0))

        val summaries = periodBounds.mapValues { (_, bounds) ->
            computeSummary(
                lessons = lessons,
                payments = payments,
                bounds = bounds,
                accountsReceivableRubles = accountsReceivableRubles,
                prepaymentsRubles = prepaymentsRubles
            )
        }

        val chart = periodBounds.mapValues { (period, bounds) ->
            buildChart(payments, bounds, period)
        }

        val debtors = computeDebtors(currentOutstanding)

        FinanceUiState.Content(
            summaries = summaries,
            chart = chart,
            debtors = debtors
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceUiState.Loading)

    private fun computeSummary(
        lessons: List<LessonDetails>,
        payments: List<Payment>,
        bounds: FinancePeriodBounds,
        accountsReceivableRubles: Long,
        prepaymentsRubles: Long
    ): FinanceSummary {
        val periodLessons = lessons.filter { it.isWithin(bounds.start, bounds.end) }
        val accruedCents = periodLessons
            .filter { it.paymentStatus != PaymentStatus.CANCELLED }
            .sumOf { it.priceCents.toLong() }

        val cashInCents = payments
            .filter { payment -> payment.at.isWithin(bounds.start, bounds.end) && payment.status == PaymentStatus.PAID }
            .sumOf { it.amountCents.toLong() }

        val totalLessons = periodLessons.size
        val conducted = periodLessons.count { it.lessonStatus == LessonStatus.DONE }
        val cancelled = periodLessons.count { it.lessonStatus == LessonStatus.CANCELED }

        return FinanceSummary(
            cashIn = centsToRubles(cashInCents),
            accrued = centsToRubles(accruedCents),
            accountsReceivable = accountsReceivableRubles,
            prepayments = prepaymentsRubles,
            lessons = FinanceLessonsSummary(
                total = totalLessons,
                conducted = conducted,
                cancelled = cancelled
            )
        )
    }

    private fun buildChart(
        payments: List<Payment>,
        bounds: FinancePeriodBounds,
        period: FinancePeriod
    ): List<FinanceChartPoint> {
        val paidByDate = payments
            .filter { it.status == PaymentStatus.PAID && it.at.isWithin(bounds.start, bounds.end) }
            .groupBy { payment -> payment.at.atZone(zoneId).toLocalDate() }
            .mapValues { (_, items) -> centsToRubles(items.sumOf { it.amountCents.toLong() }) }

        val startDate = bounds.start.atZone(zoneId).toLocalDate()
        val endExclusive = bounds.end.atZone(zoneId).toLocalDate()

        return when (period) {
            FinancePeriod.WEEK -> {
                val dates = mutableListOf<LocalDate>()
                var cursor = startDate
                while (cursor.isBefore(endExclusive)) {
                    dates.add(cursor)
                    cursor = cursor.plusDays(1)
                }
                if (dates.isEmpty()) {
                    dates.add(startDate)
                }
                dates.map { date ->
                    FinanceChartPoint(
                        date = date,
                        amount = paidByDate[date] ?: 0
                    )
                }
            }

            FinancePeriod.MONTH -> {
                val dates = mutableListOf<LocalDate>()
                var cursor = startDate
                while (cursor.isBefore(endExclusive)) {
                    dates.add(cursor)
                    cursor = cursor.plusDays(1)
                }
                if (dates.isEmpty()) {
                    dates.add(startDate)
                }

                dates.map { date ->
                    FinanceChartPoint(
                        date = date,
                        amount = paidByDate[date] ?: 0
                    )
                }
            }
        }
    }

    private fun computeDebtors(outstandingLessons: List<LessonDetails>): List<FinanceDebtor> {
        return outstandingLessons
            .groupBy { it.studentId }
            .mapNotNull { (studentId, lessons) ->
                val totalOutstandingCents = lessons.sumOf { it.outstandingAmountCents() }
                if (totalOutstandingCents <= 0) return@mapNotNull null
                val name = lessons.firstOrNull()?.studentName.orEmpty()
                val lastDate = lessons.maxOfOrNull { it.startAt.atZone(zoneId).toLocalDate() } ?: LocalDate.now(zoneId)
                FinanceDebtor(
                    studentId = studentId,
                    name = name,
                    amount = centsToRubles(totalOutstandingCents),
                    lastDueDate = lastDate
                )
            }
            .sortedWith(
                compareByDescending<FinanceDebtor> { it.amount }
                    .thenByDescending { it.lastDueDate }
                    .thenBy { it.name }
            )
            .take(5)
    }

    private fun LessonDetails.outstandingAmountCents(): Long {
        if (paymentStatus !in PaymentStatus.outstandingStatuses) return 0
        return (priceCents - paidCents).coerceAtLeast(0).toLong()
    }

    private fun LessonDetails.isWithin(start: Instant, end: Instant): Boolean {
        return !startAt.isBefore(start) && startAt.isBefore(end)
    }

    private fun Instant.isWithin(start: Instant, end: Instant): Boolean {
        return !isBefore(start) && isBefore(end)
    }

    private fun FinancePeriod.bounds(
        now: ZonedDateTime,
        weekFields: WeekFields
    ): FinancePeriodBounds {
        val zone = now.zone
        return when (this) {
            FinancePeriod.WEEK -> {
                val startDate = now.toLocalDate().with(weekFields.dayOfWeek(), 1)
                val start = startDate.atStartOfDay(zone)
                FinancePeriodBounds(
                    start = start.toInstant(),
                    end = start.plusWeeks(1).toInstant()
                )
            }

            FinancePeriod.MONTH -> {
                val startDate = now.toLocalDate().withDayOfMonth(1)
                val start = startDate.atStartOfDay(zone)
                FinancePeriodBounds(
                    start = start.toInstant(),
                    end = start.plusMonths(1).toInstant()
                )
            }
        }
    }

    private fun buildObservationBounds(): ObservationBounds {
        val now = ZonedDateTime.now(zoneId)
        val periodBounds = FinancePeriod.entries.map { it.bounds(now, weekFields) }
        val earliestStart = periodBounds.minOf { it.start }
        val latestEnd = periodBounds.maxOf { it.end }

        val oneYearAgoStart = now
            .minusMonths(12)
            .toLocalDate()
            .withDayOfMonth(1)
            .atStartOfDay(zoneId)
            .toInstant()

        val start = if (oneYearAgoStart.isBefore(earliestStart)) {
            oneYearAgoStart
        } else {
            earliestStart
        }

        return ObservationBounds(start = start, end = latestEnd)
    }

    private fun centsToRubles(value: Long): Long = centsToRubles(value.toDouble())

    private fun centsToRubles(value: Double): Long = (value / 100.0).roundToLong()
}

sealed interface FinanceUiState {
    data object Loading : FinanceUiState
    data class Content(
        val summaries: Map<FinancePeriod, FinanceSummary>,
        val chart: Map<FinancePeriod, List<FinanceChartPoint>>,
        val debtors: List<FinanceDebtor>
    ) : FinanceUiState
}

private data class FinancePeriodBounds(
    val start: Instant,
    val end: Instant
)

private data class ObservationBounds(
    val start: Instant,
    val end: Instant
)
