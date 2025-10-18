package com.tutorly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingFlat
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.ui.theme.TutorlyCardDefaults
import com.tutorly.ui.theme.extendedColors
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

@Composable
fun FinanceScreen(
    modifier: Modifier = Modifier,
    viewModel: FinanceViewModel = hiltViewModel()
) {
    var selectedPeriod by rememberSaveable { mutableStateOf(FinancePeriod.DAY) }
    val state by viewModel.uiState.collectAsState()

    when (val uiState = state) {
        FinanceUiState.Loading -> FinanceLoading(modifier)
        is FinanceUiState.Content -> FinanceContent(
            modifier = modifier,
            selectedPeriod = selectedPeriod,
            onSelectPeriod = { selectedPeriod = it },
            state = uiState
        )
    }
}

@Composable
private fun FinanceLoading(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FinanceContent(
    modifier: Modifier,
    selectedPeriod: FinancePeriod,
    onSelectPeriod: (FinancePeriod) -> Unit,
    state: FinanceUiState.Content
) {
    val currencyFormatter = rememberCurrencyFormatter()
    val percentFormatter = rememberPercentFormatter()
    val numberFormatter = rememberNumberFormatter()
    val scrollState = rememberScrollState()

    val summary = state.summaries[selectedPeriod] ?: FinanceSummary.EMPTY
    val averages = state.averages

    val periodLabel = stringResource(selectedPeriod.periodLabelRes)
    val periodText = stringResource(R.string.finance_metric_period, periodLabel)
    val incomeValue = currencyFormatter.format(summary.income)
    val debtValue = currencyFormatter.format(summary.debt)
    val hoursValue = numberFormatter.format(summary.hours)
    val lessonsValue = summary.lessons.toString()
    val averageDay = currencyFormatter.format(averages.day)
    val averageWeek = currencyFormatter.format(averages.week)
    val averageMonth = currencyFormatter.format(averages.month)

    val incomeChangeDisplay = summary.incomeChange.toDisplay(
        period = selectedPeriod,
        percentFormatter = percentFormatter,
        increaseIsGood = true
    )
    val debtChangeDisplay = summary.debtChange.toDisplay(
        period = selectedPeriod,
        percentFormatter = percentFormatter,
        increaseIsGood = false
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FinancePeriodSelector(
            selected = selectedPeriod,
            onSelect = onSelectPeriod
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FinanceMetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.finance_income_label),
                value = incomeValue,
                change = incomeChangeDisplay
            )
            FinanceMetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.finance_debt_label),
                value = debtValue,
                change = debtChangeDisplay
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FinanceMetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.finance_hours_label),
                value = stringResource(R.string.finance_hours_value_format, hoursValue),
                subtitle = periodText
            )
            FinanceMetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.finance_lessons_label),
                value = lessonsValue,
                subtitle = periodText
            )
        }

        FinanceAveragesCard(
            modifier = Modifier.fillMaxWidth(),
            averageDay = averageDay,
            averageWeek = averageWeek,
            averageMonth = averageMonth,
            topStudents = summary.topStudents,
            currencyFormatter = currencyFormatter
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FinancePeriodSelector(
    selected: FinancePeriod,
    onSelect: (FinancePeriod) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FinancePeriod.entries.forEach { period ->
            val isSelected = period == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(period) },
                label = {
                    Text(text = stringResource(period.tabLabelRes))
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null
                        )
                    }
                } else {
                    null
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.extendedColors.chipSelected,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun FinanceMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    change: ChangeDisplay? = null
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = TutorlyCardDefaults.colors(containerColor = Color.White),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            when {
                change != null -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = change.icon,
                            contentDescription = null,
                            tint = change.tint,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = change.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = change.tint
                        )
                    }
                }

                subtitle != null -> {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FinanceAveragesCard(
    modifier: Modifier,
    averageDay: String,
    averageWeek: String,
    averageMonth: String,
    topStudents: List<StudentEarning>,
    currencyFormatter: NumberFormat
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = TutorlyCardDefaults.colors(containerColor = Color.White),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.finance_average_title),
                style = MaterialTheme.typography.titleMedium
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FinanceAverageRow(
                    label = stringResource(R.string.finance_average_day),
                    value = averageDay
                )
                FinanceAverageRow(
                    label = stringResource(R.string.finance_average_week),
                    value = averageWeek
                )
                FinanceAverageRow(
                    label = stringResource(R.string.finance_average_month),
                    value = averageMonth
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = stringResource(R.string.finance_top_students_title),
                style = MaterialTheme.typography.titleSmall
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                topStudents.take(3).forEachIndexed { index, student ->
                    FinanceTopStudentRow(
                        position = index + 1,
                        student = student,
                        currencyFormatter = currencyFormatter
                    )
                }
            }
        }
    }
}

@Composable
private fun FinanceAverageRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun FinanceTopStudentRow(
    position: Int,
    student: StudentEarning,
    currencyFormatter: NumberFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.finance_top_student_item, position, student.name),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = currencyFormatter.format(student.amount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class ChangeDisplay(
    val icon: ImageVector,
    val tint: Color,
    val text: String
)

@Composable
private fun FinanceChange.toDisplay(
    period: FinancePeriod,
    percentFormatter: NumberFormat,
    increaseIsGood: Boolean
): ChangeDisplay {
    val previousLabel = period.previousLabelRes
    val previousText = stringResource(previousLabel)
    val percentValue = abs(percent)
    val formattedPercent = percentFormatter.format(percentValue)
    val text = when {
        percent > 0 -> stringResource(
            R.string.finance_change_positive,
            formattedPercent,
            previousText
        )

        percent < 0 -> stringResource(
            R.string.finance_change_negative,
            formattedPercent,
            previousText
        )

        else -> stringResource(R.string.finance_change_neutral, previousText)
    }

    val (icon, color) = when {
        percent > 0 -> Icons.Outlined.TrendingUp to if (increaseIsGood) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        }

        percent < 0 -> Icons.Outlined.TrendingDown to if (increaseIsGood) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }

        else -> Icons.Outlined.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
    }

    return ChangeDisplay(icon = icon, tint = color, text = text)
}

@Composable
private fun rememberCurrencyFormatter(): NumberFormat {
    return remember {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance("RUB")
            maximumFractionDigits = 0
        }
    }
}

@Composable
private fun rememberPercentFormatter(): NumberFormat {
    return remember {
        NumberFormat.getPercentInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 1
            minimumFractionDigits = 0
        }
    }
}

@Composable
private fun rememberNumberFormatter(): NumberFormat {
    return remember {
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 1
            minimumFractionDigits = 0
        }
    }
}
