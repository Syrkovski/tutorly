package com.tutorly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.PaymentsRepository
import com.tutorly.models.Payment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
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
        val temporalContext = FinanceTemporalContext(
            now = now,
            zoneId = zoneId,
            weekFields = weekFields
        )
        val currentOutstanding = outstandingLessons.filter { lesson ->
            !lesson.startAt.isAfter(now.toInstant())
        }
        val accountsReceivableRubles = centsToRubles(
            currentOutstanding.sumOf { lesson -> lesson.outstandingAmountCents() }
        )
        val prepaymentsRubles = centsToRubles(prepaymentBalanceCents.coerceAtLeast(0))

        val debtors = computeDebtors(currentOutstanding)

        FinanceUiState.Content(
            lessons = lessons,
            payments = payments,
            debtors = debtors,
            accountsReceivable = accountsReceivableRubles,
            prepayments = prepaymentsRubles,
            temporalContext = temporalContext
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceUiState.Loading)

    private fun computeDebtors(outstandingLessons: List<LessonDetails>): List<FinanceDebtor> {
        return outstandingLessons
            .groupBy { it.studentId }
            .mapNotNull { (studentId, lessons) ->
                val totalOutstandingCents = lessons.sumOf { it.outstandingAmountCents() }
                if (totalOutstandingCents <= 0) return@mapNotNull null
                val name = lessons.firstOrNull()?.studentName.orEmpty()
                val lastDate = lessons.maxOfOrNull { it.startAt.atZone(zoneId).toLocalDate() }
                    ?: ZonedDateTime.now(zoneId).toLocalDate()
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

    private fun buildObservationBounds(): ObservationBounds {
        val now = ZonedDateTime.now(zoneId)
        val temporalContext = FinanceTemporalContext(
            now = now,
            zoneId = zoneId,
            weekFields = weekFields
        )
        val periodBounds = FinancePeriod.entries.map { it.bounds(temporalContext) }
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
}

sealed interface FinanceUiState {
    data object Loading : FinanceUiState
    data class Content(
        val lessons: List<LessonDetails>,
        val payments: List<Payment>,
        val debtors: List<FinanceDebtor>,
        val accountsReceivable: Long,
        val prepayments: Long,
        val temporalContext: FinanceTemporalContext
    ) : FinanceUiState
}

private data class ObservationBounds(
    val start: Instant,
    val end: Instant
)
