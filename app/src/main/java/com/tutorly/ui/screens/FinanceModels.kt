package com.tutorly.ui.screens

import androidx.annotation.StringRes
import com.tutorly.R

enum class FinancePeriod(
    @StringRes val tabLabelRes: Int,
    @StringRes val periodLabelRes: Int,
    @StringRes val previousLabelRes: Int
) {
    DAY(
        tabLabelRes = R.string.finance_period_day,
        periodLabelRes = R.string.finance_period_day_accusative,
        previousLabelRes = R.string.finance_previous_day
    ),
    WEEK(
        tabLabelRes = R.string.finance_period_week,
        periodLabelRes = R.string.finance_period_week_accusative,
        previousLabelRes = R.string.finance_previous_week
    ),
    MONTH(
        tabLabelRes = R.string.finance_period_month,
        periodLabelRes = R.string.finance_period_month_accusative,
        previousLabelRes = R.string.finance_previous_month
    )
}

data class FinanceSummary(
    val income: Long,
    val incomeChange: FinanceChange,
    val debt: Long,
    val debtChange: FinanceChange,
    val hours: Double,
    val lessons: Int,
    val topStudents: List<StudentEarning>
) {
    companion object {
        val EMPTY = FinanceSummary(
            income = 0,
            incomeChange = FinanceChange.ZERO,
            debt = 0,
            debtChange = FinanceChange.ZERO,
            hours = 0.0,
            lessons = 0,
            topStudents = emptyList()
        )
    }
}

data class FinanceChange(
    val percent: Double,
    val deltaCents: Long
) {
    companion object {
        val ZERO = FinanceChange(percent = 0.0, deltaCents = 0L)

        fun from(currentCents: Long, previousCents: Long): FinanceChange {
            val percent = when {
                previousCents == 0L && currentCents == 0L -> 0.0
                previousCents == 0L -> 1.0
                else -> (currentCents - previousCents).toDouble() / previousCents.toDouble()
            }
            val delta = currentCents - previousCents
            return FinanceChange(percent = percent, deltaCents = delta)
        }
    }
}

data class FinanceAverages(
    val day: Long,
    val week: Long,
    val month: Long
) {
    companion object {
        val ZERO = FinanceAverages(day = 0, week = 0, month = 0)
    }
}

data class StudentEarning(
    val studentId: Long,
    val name: String,
    val amount: Long
)

