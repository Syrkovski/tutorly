package com.tutorly.ui.screens

import androidx.annotation.StringRes
import com.tutorly.R
import java.time.LocalDate

enum class FinancePeriod(
    @StringRes val tabLabelRes: Int,
    @StringRes val periodLabelRes: Int
) {
    WEEK(
        tabLabelRes = R.string.finance_period_week,
        periodLabelRes = R.string.finance_period_week_accusative
    ),
    MONTH(
        tabLabelRes = R.string.finance_period_month,
        periodLabelRes = R.string.finance_period_month_accusative
    )
}

data class FinanceSummary(
    val cashIn: Long,
    val accountsReceivable: Long,
    val prepayments: Long,
    val lessons: FinanceLessonsSummary,
    val totalDurationMinutes: Int
) {
    companion object {
        val EMPTY = FinanceSummary(
            cashIn = 0,
            accountsReceivable = 0,
            prepayments = 0,
            lessons = FinanceLessonsSummary.EMPTY,
            totalDurationMinutes = 0
        )
    }
}

data class FinanceLessonsSummary(
    val total: Int,
    val conducted: Int,
    val cancelled: Int
) {
    companion object {
        val EMPTY = FinanceLessonsSummary(total = 0, conducted = 0, cancelled = 0)
    }
}

data class FinanceDebtor(
    val studentId: Long,
    val name: String,
    val amount: Long,
    val lastDueDate: LocalDate
)

data class FinanceChartPoint(
    val date: LocalDate,
    val amount: Long
)
