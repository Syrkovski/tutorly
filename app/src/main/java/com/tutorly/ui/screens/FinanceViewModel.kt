package com.tutorly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.models.PaymentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.WeekFields

@HiltViewModel
class FinanceViewModel @Inject constructor(
    lessonsRepository: LessonsRepository
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val weekFields: WeekFields = WeekFields.ISO

    private val observationBounds: ObservationBounds = buildObservationBounds()

    val uiState: StateFlow<FinanceUiState> = lessonsRepository
        .observeLessons(observationBounds.start, observationBounds.end)
        .map { lessons ->
            val now = ZonedDateTime.now(zoneId)
            val periodBounds = FinancePeriod.entries.associateWith { it.bounds(now, weekFields) }
            val summaries = periodBounds.mapValues { (_, bounds) ->
                computeSummary(lessons, bounds)
            }
            val averages = computeAverages(lessons)
            FinanceUiState.Content(summaries = summaries, averages = averages)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceUiState.Loading)

    private fun computeSummary(
        lessons: List<LessonDetails>,
        bounds: FinancePeriodBounds
    ): FinanceSummary {
        val currentLessons = lessons.filter { it.isWithin(bounds.currentStart, bounds.currentEnd) }
        val previousLessons = lessons.filter { it.isWithin(bounds.previousStart, bounds.previousEnd) }

        val incomeCurrentCents = currentLessons.sumOf { it.paidCents.toLong() }
        val incomePreviousCents = previousLessons.sumOf { it.paidCents.toLong() }
        val debtCurrentCents = currentLessons.sumOf { lesson ->
            if (lesson.paymentStatus in PaymentStatus.outstandingStatuses) {
                (lesson.priceCents - lesson.paidCents).coerceAtLeast(0).toLong()
            } else {
                0L
            }
        }
        val debtPreviousCents = previousLessons.sumOf { lesson ->
            if (lesson.paymentStatus in PaymentStatus.outstandingStatuses) {
                (lesson.priceCents - lesson.paidCents).coerceAtLeast(0).toLong()
            } else {
                0L
            }
        }

        val totalMinutes = currentLessons.sumOf { it.duration.toMinutes() }
        val hours = totalMinutes.toDouble() / 60.0
        val topStudents = currentLessons
            .groupBy { it.studentId }
            .map { (id, items) ->
                val name = items.firstOrNull()?.studentName.orEmpty()
                val amountCents = items.sumOf { it.paidCents.toLong() }
                StudentEarning(
                    studentId = id,
                    name = name,
                    amount = centsToRubles(amountCents)
                )
            }
            .filter { it.amount > 0 }
            .sortedWith(
                compareByDescending<StudentEarning> { it.amount }
                    .thenBy { it.name }
            )

        return FinanceSummary(
            income = centsToRubles(incomeCurrentCents),
            incomeChange = FinanceChange.from(incomeCurrentCents, incomePreviousCents),
            debt = centsToRubles(debtCurrentCents),
            debtChange = FinanceChange.from(debtCurrentCents, debtPreviousCents),
            hours = hours,
            lessons = currentLessons.size,
            topStudents = topStudents
        )
    }

    private fun computeAverages(lessons: List<LessonDetails>): FinanceAverages {
        if (lessons.isEmpty()) return FinanceAverages.ZERO

        val paidByDay = lessons
            .groupBy { it.startAt.atZone(zoneId).toLocalDate() }
            .mapValues { (_, items) -> items.sumOf { it.paidCents.toLong() } }
        val paidByWeek = lessons
            .groupBy { lesson ->
                val date = lesson.startAt.atZone(zoneId).toLocalDate()
                val week = date.get(weekFields.weekOfWeekBasedYear())
                val year = date.get(weekFields.weekBasedYear())
                year to week
            }
            .mapValues { (_, items) -> items.sumOf { it.paidCents.toLong() } }
        val paidByMonth = lessons
            .groupBy { lesson -> YearMonth.from(lesson.startAt.atZone(zoneId)) }
            .mapValues { (_, items) -> items.sumOf { it.paidCents.toLong() } }

        return FinanceAverages(
            day = averageRubles(paidByDay.values),
            week = averageRubles(paidByWeek.values),
            month = averageRubles(paidByMonth.values)
        )
    }

    private fun averageRubles(values: Collection<Long>): Long {
        if (values.isEmpty()) return 0L
        val avgCents = values.map { it.toDouble() }.average()
        return centsToRubles(avgCents)
    }

    private fun centsToRubles(value: Long): Long = centsToRubles(value.toDouble())

    private fun centsToRubles(value: Double): Long = (value / 100.0).roundToLong()

    private fun LessonDetails.isWithin(start: Instant, end: Instant): Boolean {
        val instant = startAt
        return !instant.isBefore(start) && instant.isBefore(end)
    }

    private fun FinancePeriod.bounds(
        now: ZonedDateTime,
        weekFields: WeekFields
    ): FinancePeriodBounds {
        val zone = now.zone
        return when (this) {
            FinancePeriod.DAY -> {
                val currentStart = now.toLocalDate().atStartOfDay(zone)
                FinancePeriodBounds(
                    previousStart = currentStart.minusDays(1).toInstant(),
                    previousEnd = currentStart.toInstant(),
                    currentStart = currentStart.toInstant(),
                    currentEnd = currentStart.plusDays(1).toInstant()
                )
            }
            FinancePeriod.WEEK -> {
                val currentStartDate = now.toLocalDate().with(weekFields.dayOfWeek(), 1)
                val currentStart = currentStartDate.atStartOfDay(zone)
                FinancePeriodBounds(
                    previousStart = currentStart.minusWeeks(1).toInstant(),
                    previousEnd = currentStart.toInstant(),
                    currentStart = currentStart.toInstant(),
                    currentEnd = currentStart.plusWeeks(1).toInstant()
                )
            }
            FinancePeriod.MONTH -> {
                val currentStartDate = now.toLocalDate().withDayOfMonth(1)
                val currentStart = currentStartDate.atStartOfDay(zone)
                FinancePeriodBounds(
                    previousStart = currentStart.minusMonths(1).toInstant(),
                    previousEnd = currentStart.toInstant(),
                    currentStart = currentStart.toInstant(),
                    currentEnd = currentStart.plusMonths(1).toInstant()
                )
            }
        }
    }

    private fun buildObservationBounds(): ObservationBounds {
        val now = ZonedDateTime.now(zoneId)
        val periodBounds = FinancePeriod.entries.map { it.bounds(now, weekFields) }
        val earliestPeriodStart = periodBounds.minOf { it.previousStart }
        val latestPeriodEnd = periodBounds.maxOf { it.currentEnd }

        val oneYearAgoStart = now
            .minusMonths(12)
            .toLocalDate()
            .withDayOfMonth(1)
            .atStartOfDay(zoneId)
            .toInstant()

        val start = if (oneYearAgoStart.isBefore(earliestPeriodStart)) {
            oneYearAgoStart
        } else {
            earliestPeriodStart
        }

        return ObservationBounds(start = start, end = latestPeriodEnd)
    }
}

sealed interface FinanceUiState {
    data object Loading : FinanceUiState
    data class Content(
        val summaries: Map<FinancePeriod, FinanceSummary>,
        val averages: FinanceAverages
    ) : FinanceUiState
}

private data class FinancePeriodBounds(
    val previousStart: Instant,
    val previousEnd: Instant,
    val currentStart: Instant,
    val currentEnd: Instant
)

private data class ObservationBounds(
    val start: Instant,
    val end: Instant
)

