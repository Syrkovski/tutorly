package com.tutorly.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.ui.theme.TutorlyCardDefaults
import com.tutorly.ui.theme.extendedColors
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Currency
import java.util.Locale

@Composable
fun FinanceScreen(
    modifier: Modifier = Modifier,
    viewModel: FinanceViewModel = hiltViewModel(),
    onOpenStudent: (Long) -> Unit = {},
    onViewAllDebtors: () -> Unit = {}
) {
    var selectedPeriod by rememberSaveable { mutableStateOf(FinancePeriod.WEEK) }
    val state by viewModel.uiState.collectAsState()

    when (val uiState = state) {
        FinanceUiState.Loading -> FinanceLoading(modifier)
        is FinanceUiState.Content -> FinanceContent(
            modifier = modifier,
            selectedPeriod = selectedPeriod,
            onSelectPeriod = { selectedPeriod = it },
            state = uiState,
            onOpenStudent = onOpenStudent,
            onViewAllDebtors = onViewAllDebtors
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
    state: FinanceUiState.Content,
    onOpenStudent: (Long) -> Unit,
    onViewAllDebtors: () -> Unit
) {
    val currencyFormatter = rememberCurrencyFormatter()
    val dateFormatter = rememberDateFormatter()
    val scrollState = rememberScrollState()

    val summary = state.summaries[selectedPeriod] ?: FinanceSummary.EMPTY
    val chartPoints = state.chart[selectedPeriod].orEmpty()
    val debtors = state.debtors

    val periodLabel = stringResource(selectedPeriod.periodLabelRes)
    val periodText = stringResource(R.string.finance_metric_period, periodLabel)
    val cashInValue = currencyFormatter.format(summary.cashIn)
    val accruedValue = currencyFormatter.format(summary.accrued)
    val debtValue = currencyFormatter.format(summary.accountsReceivable)
    val prepaymentValue = currencyFormatter.format(summary.prepayments)
    val lessonsValue = summary.lessons.total.toString()
    val conductedText = stringResource(R.string.finance_lessons_badge_conducted, summary.lessons.conducted)
    val cancelledText = stringResource(R.string.finance_lessons_badge_cancelled, summary.lessons.cancelled)

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
                title = stringResource(R.string.finance_cash_in_label),
                value = cashInValue,
                subtitle = periodText
            )
            FinanceMetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.finance_accrued_label),
                value = accruedValue,
                subtitle = periodText
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FinanceMetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.finance_ar_label),
                value = debtValue
            )
            FinanceMetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.finance_prepayments_label),
                value = prepaymentValue
            )
        }

        FinanceMetricCard(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.finance_lessons_label),
            value = lessonsValue,
            subtitle = periodText,
            footer = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinanceBadge(
                        text = conductedText,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    FinanceBadge(
                        text = cancelledText,
                        color = Color(0xFFD05E6E),
                        contentColor = Color.White
                    )
                }
            }
        )

        FinanceChartCard(
            modifier = Modifier.fillMaxWidth(),
            points = chartPoints,
            period = selectedPeriod,
            currencyFormatter = currencyFormatter,
            dateFormatter = dateFormatter
        )

        FinanceDebtorsSection(
            modifier = Modifier.fillMaxWidth(),
            debtors = debtors,
            currencyFormatter = currencyFormatter,
            dateFormatter = dateFormatter,
            onOpenStudent = onOpenStudent,
            onViewAllDebtors = onViewAllDebtors
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
    footer: (@Composable () -> Unit)? = null
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
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            footer?.let {
                it()
            }
        }
    }
}

@Composable
private fun FinanceBadge(
    text: String,
    color: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color,
        contentColor = contentColor
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FinanceChartCard(
    modifier: Modifier,
    points: List<FinanceChartPoint>,
    period: FinancePeriod,
    currencyFormatter: NumberFormat,
    dateFormatter: DateTimeFormatter
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
                text = stringResource(R.string.finance_chart_title),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
            if (points.isEmpty()) {
                Text(
                    text = stringResource(R.string.finance_chart_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FinanceBarChart(
                    points = points,
                    period = period,
                    dateFormatter = dateFormatter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
                val total = points.sumOf { it.amount }
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(
                        R.string.finance_chart_total,
                        currencyFormatter.format(total)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FinanceBarChart(
    points: List<FinanceChartPoint>,
    period: FinancePeriod,
    dateFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    val maxValue = remember(points) { points.maxOfOrNull { it.amount } ?: 0L }
    val barColor = MaterialTheme.colorScheme.primary
    val labels = remember(points, period, dateFormatter) {
        buildChartLabels(points, period, dateFormatter)
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            if (points.isEmpty()) return@Canvas
            val max = maxValue.toFloat().coerceAtLeast(1f)
            val height = size.height
            val width = size.width
            val bars = points.size
            val barWidth = width / (bars * 1.6f)
            val stepX = width / bars

            points.forEachIndexed { index, point ->
                val ratio = point.amount.toFloat() / max
                val barHeight = ratio * height
                val left = stepX * index + (stepX - barWidth) / 2f
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x = left, y = height - barHeight),
                    size = Size(width = barWidth, height = barHeight),
                    cornerRadius = CornerRadius(x = 12.dp.toPx(), y = 12.dp.toPx())
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}
}

@Composable
private fun FinanceDebtorsSection(
    modifier: Modifier,
    debtors: List<FinanceDebtor>,
    currencyFormatter: NumberFormat,
    dateFormatter: DateTimeFormatter,
    onOpenStudent: (Long) -> Unit,
    onViewAllDebtors: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.finance_debtors_title),
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onViewAllDebtors) {
                    Text(text = stringResource(R.string.finance_debtors_all))
                }
            }

            if (debtors.isEmpty()) {
                Text(
                    text = stringResource(R.string.finance_debtors_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    debtors.forEach { debtor ->
                        FinanceDebtorRow(
                            debtor = debtor,
                            currencyFormatter = currencyFormatter,
                            dateFormatter = dateFormatter,
                            onOpenStudent = onOpenStudent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceDebtorRow(
    debtor: FinanceDebtor,
    currencyFormatter: NumberFormat,
    dateFormatter: DateTimeFormatter,
    onOpenStudent: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenStudent(debtor.studentId) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = debtor.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.finance_debtors_last_debt, dateFormatter.format(debtor.lastDueDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = currencyFormatter.format(debtor.amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun buildChartLabels(
    points: List<FinanceChartPoint>,
    period: FinancePeriod,
    dateFormatter: DateTimeFormatter
): List<String> {
    if (points.isEmpty()) return emptyList()
    val locale = Locale.getDefault()
    val firstDayOfWeek = WeekFields.ISO.firstDayOfWeek
    return when (period) {
        FinancePeriod.WEEK -> points.map { point ->
            point.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        }

        FinancePeriod.MONTH -> points.mapIndexed { index, point ->
            if (point.date.dayOfWeek == firstDayOfWeek || index == 0) {
                dateFormatter.format(point.date)
            } else {
                ""
            }
        }
    }
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
private fun rememberDateFormatter(): DateTimeFormatter {
    return remember {
        DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    }
}
