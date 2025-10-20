package com.tutorly.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.ui.components.GradientTopBarContainer
import com.tutorly.ui.theme.TutorlyCardDefaults
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

@Composable
fun FinanceTopBar(
    selectedPeriod: FinancePeriod,
    onSelectPeriod: (FinancePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    GradientTopBarContainer {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.finance_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            FinancePeriodToggle(
                selected = selectedPeriod,
                onSelect = onSelectPeriod
            )
        }
    }
}

@Composable
private fun FinancePeriodToggle(
    selected: FinancePeriod,
    onSelect: (FinancePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .selectableGroup()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FinancePeriod.entries.forEach { period ->
            val isSelected = period == selected
            val segmentShape = RoundedCornerShape(20.dp)
            val background = if (isSelected) Color.White else Color.White.copy(alpha = 0.12f)
            val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary

            Surface(
                onClick = { onSelect(period) },
                shape = segmentShape,
                color = background,
                contentColor = contentColor,
                tonalElevation = 0.dp,
                shadowElevation = if (isSelected) 2.dp else 0.dp
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    text = stringResource(period.tabLabelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun FinanceScreen(
    selectedPeriod: FinancePeriod,
    periodOffset: Int,
    onPeriodOffsetChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FinanceViewModel = hiltViewModel(),
    onOpenStudent: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    when (val uiState = state) {
        FinanceUiState.Loading -> FinanceLoading(modifier)
        is FinanceUiState.Content -> FinanceContent(
            modifier = modifier,
            selectedPeriod = selectedPeriod,
            periodOffset = periodOffset,
            state = uiState,
            onPeriodOffsetChange = onPeriodOffsetChange,
            onOpenStudent = onOpenStudent
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
    periodOffset: Int,
    state: FinanceUiState.Content,
    onPeriodOffsetChange: (Int) -> Unit,
    onOpenStudent: (Long) -> Unit
) {
    val currencyFormatter = rememberCurrencyFormatter()
    val dateFormatter = rememberDateFormatter()
    val scrollState = rememberScrollState()

    val temporalContext = state.temporalContext
    val bounds = remember(temporalContext, selectedPeriod, periodOffset) {
        selectedPeriod.bounds(temporalContext, periodOffset)
    }
    val summary = remember(state, bounds) {
        calculateFinanceSummary(
            lessons = state.lessons,
            payments = state.payments,
            bounds = bounds,
            accountsReceivableRubles = state.accountsReceivable,
            prepaymentsRubles = state.prepayments
        )
    }
    val chartPoints = remember(state, bounds) {
        calculateFinanceChart(
            lessons = state.lessons,
            bounds = bounds,
            now = temporalContext.now.toInstant(),
            zoneId = temporalContext.zoneId
        )
    }
    val debtors = state.debtors

    val periodLabel = stringResource(selectedPeriod.periodLabelRes)
    val periodText = stringResource(R.string.finance_metric_period, periodLabel)
    val cashInValue = currencyFormatter.format(summary.cashIn)
    val accruedValue = currencyFormatter.format(summary.accrued)
    val debtValue = currencyFormatter.format(summary.accountsReceivable)
    val prepaymentValue = currencyFormatter.format(summary.prepayments)
    val lessonsValue = summary.lessons.total.toString()

    val swipeModifier = Modifier.pointerInput(selectedPeriod, periodOffset) {
        val threshold = 48.dp.toPx()
        var totalDrag = 0f
        var handled = false
        detectHorizontalDragGestures(
            onDragStart = {
                totalDrag = 0f
                handled = false
            },
            onDragEnd = {
                totalDrag = 0f
                handled = false
            },
            onDragCancel = {
                totalDrag = 0f
                handled = false
            },
            onHorizontalDrag = { change, dragAmount ->
                if (handled) return@detectHorizontalDragGestures

                totalDrag += dragAmount
                if (abs(totalDrag) > threshold) {
                    val newOffset = if (totalDrag < 0) periodOffset + 1 else periodOffset - 1
                    onPeriodOffsetChange(newOffset)
                    handled = true
                    change.consume()
                }
            }
        )
    }

    val zoneId = temporalContext.zoneId
    val periodStartDate = remember(bounds, zoneId) {
        bounds.start.atZone(zoneId).toLocalDate()
    }
    val periodEndDate = remember(bounds, zoneId, periodStartDate) {
        val exclusive = bounds.end.atZone(zoneId).toLocalDate()
        if (exclusive.isAfter(periodStartDate)) exclusive.minusDays(1) else periodStartDate
    }
    val periodRange = stringResource(
        R.string.finance_period_range,
        dateFormatter.format(periodStartDate),
        dateFormatter.format(periodEndDate)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(swipeModifier)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = periodRange,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            subtitle = periodText
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
            onOpenStudent = onOpenStudent
        )

        Spacer(modifier = Modifier.height(8.dp))
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
        val density = LocalDensity.current
        val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        val textSizePx = with(density) { 12.sp.toPx() }
        val labelPaint = remember(labelColor, textSizePx) {
            android.graphics.Paint().apply {
                isAntiAlias = true
                color = labelColor.toArgb()
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = textSizePx
            }
        }
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
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            if (points.isEmpty()) return@Canvas
            val bars = points.size
            val width = size.width
            val stepX = width / bars
            val baseline = size.height - labelPaint.fontMetrics.descent

            labels.forEachIndexed { index, label ->
                if (label.isNotBlank()) {
                    val x = stepX * index + stepX / 2f
                    drawContext.canvas.nativeCanvas.drawText(label, x, baseline, labelPaint)
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
    onOpenStudent: (Long) -> Unit
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
                text = stringResource(R.string.finance_debtors_title),
                style = MaterialTheme.typography.titleMedium
            )

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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    modifier = Modifier.clickable { onOpenStudent(debtor.studentId) },
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
    return when (period) {
        FinancePeriod.WEEK -> points.map { point ->
            point.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        }

        FinancePeriod.MONTH -> {
            val lastDay = points.maxOfOrNull { it.date.dayOfMonth } ?: 1
            val labeledDays = setOf(1, 7, 14, 21, lastDay)
            points.map { point ->
                val day = point.date.dayOfMonth
                if (day in labeledDays) day.toString() else ""
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
