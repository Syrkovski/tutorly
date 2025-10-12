package com.tutorly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.domain.model.LessonForToday
import com.tutorly.models.PaymentStatus
import com.tutorly.ui.lessoncard.LessonCardSheet
import com.tutorly.ui.lessoncard.LessonCardViewModel
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lessonCardViewModel: LessonCardViewModel = hiltViewModel()
    val lessonCardState by lessonCardViewModel.uiState.collectAsState()

    LessonCardSheet(
        state = lessonCardState,
        onDismissRequest = lessonCardViewModel::dismiss,
        onStudentSelect = lessonCardViewModel::onStudentSelected,
        onDateSelect = lessonCardViewModel::onDateSelected,
        onTimeSelect = lessonCardViewModel::onTimeSelected,
        onDurationSelect = lessonCardViewModel::onDurationSelected,
        onPriceChange = lessonCardViewModel::onPriceChanged,
        onStatusSelect = lessonCardViewModel::onPaymentStatusSelected,
        onNoteChange = lessonCardViewModel::onNoteChanged,
        onSnackbarConsumed = lessonCardViewModel::consumeSnackbar
    )

    LaunchedEffect(snackbarMessage) {
        val message = snackbarMessage ?: return@LaunchedEffect
        val text = when (message.status) {
            PaymentStatus.PAID -> context.getString(R.string.today_snackbar_paid)
            PaymentStatus.DUE -> context.getString(R.string.today_snackbar_due)
            else -> context.getString(R.string.today_snackbar_marked)
        }
        val action = context.getString(R.string.today_snackbar_action_undo)
        val result = snackbarHostState.showSnackbar(
            message = text,
            actionLabel = action,
            duration = SnackbarDuration.Short,
            withDismissAction = true
        )
        viewModel.onSnackbarShown()
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.onUndo(message.lessonId)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TodayTopBar(state = uiState) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                TodayUiState.Loading -> LoadingState()
                TodayUiState.Empty -> EmptyState()
                is TodayUiState.Content -> TodayContent(
                    state = state,
                    onSwipeRight = viewModel::onSwipeRight,
                    onSwipeLeft = viewModel::onSwipeLeft,
                    onLessonClick = lessonCardViewModel::open
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.today_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.today_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayContent(
    state: TodayUiState.Content,
    onSwipeRight: (Long) -> Unit,
    onSwipeLeft: (Long) -> Unit,
    onLessonClick: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "stats") {
            TodayStatsRow(stats = state.stats)
        }
        if (state.isAllMarked) {
            item(key = "all_done") {
                AllMarkedMessage()
            }
        }
        state.sections.forEachIndexed { index, section ->
            item(key = "header_${section.date}") {
                DaySectionHeader(
                    section = section,
                    addTopSpacing = index > 0 || state.isAllMarked
                )
            }
            items(section.pending, key = { it.id }) { lesson ->
                TodayLessonRow(
                    lesson = lesson,
                    onSwipeRight = onSwipeRight,
                    onSwipeLeft = onSwipeLeft,
                    onClick = { onLessonClick(lesson.id) }
                )
            }
            if (section.upcoming.isNotEmpty()) {
                item(key = "upcoming_header_${section.date}") {
                    UpcomingSectionHeader()
                }
                items(section.upcoming, key = { it.id }) { lesson ->
                    TodayLessonRow(
                        lesson = lesson,
                        onSwipeRight = onSwipeRight,
                        onSwipeLeft = onSwipeLeft,
                        onClick = { onLessonClick(lesson.id) }
                    )
                }
            }
            if (section.marked.isNotEmpty()) {
                item(key = "marked_header_${section.date}") {
                    MarkedSectionHeader()
                }
                items(section.marked, key = { it.id }) { lesson ->
                    TodayLessonRow(
                        lesson = lesson,
                        onSwipeRight = onSwipeRight,
                        onSwipeLeft = onSwipeLeft,
                        onClick = { onLessonClick(lesson.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySectionHeader(section: TodayLessonSection, addTopSpacing: Boolean) {
    val locale = remember { Locale.getDefault() }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM, EEEE", locale) }
    val formatted = remember(section.date, locale) {
        dateFormatter.format(section.date).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (addTopSpacing) 12.dp else 0.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatted,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (section.isToday) {
            TodayBadge()
        }
    }
}

@Composable
private fun TodayStatsRow(stats: TodayStats) {
    val currencyFormatter = rememberCurrencyFormatter()
    val totalAmount = remember(stats.totalAmountCents, currencyFormatter) {
        formatCurrency(stats.totalAmountCents, currencyFormatter)
    }
    val paidAmount = remember(stats.paidAmountCents, currencyFormatter) {
        formatCurrency(stats.paidAmountCents, currencyFormatter)
    }
    val incomeValue = stringResource(
        R.string.today_stats_income_value,
        paidAmount,
        totalAmount
    )
    val lessonsValue = stringResource(
        R.string.today_stats_lessons_value,
        stats.paidLessons,
        stats.totalLessons
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TodayStatsCard(
            label = stringResource(R.string.today_stats_income_label),
            value = incomeValue,
            modifier = Modifier.weight(1f)
        )
        TodayStatsCard(
            label = stringResource(R.string.today_stats_lessons_label),
            value = lessonsValue,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TodayStatsCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TodayBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = stringResource(R.string.today_title),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MarkedSectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Text(
            text = stringResource(R.string.today_section_marked),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

@Composable
private fun UpcomingSectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Text(
            text = stringResource(R.string.today_section_upcoming),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

@Composable
private fun AllMarkedMessage() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.today_all_marked_title),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.today_all_marked_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayLessonRow(
    lesson: LessonForToday,
    onSwipeRight: (Long) -> Unit,
    onSwipeLeft: (Long) -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        when (value) {
            SwipeToDismissBoxValue.StartToEnd -> {
                onSwipeRight(lesson.id)
                false
            }
            SwipeToDismissBoxValue.EndToStart -> {
                onSwipeLeft(lesson.id)
                false
            }
            else -> false
        }
    })

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier
            .fillMaxWidth(),
        backgroundContent = { DismissBackground(state = dismissState) }
    ) {
        LessonCard(
            lesson = lesson,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        )
    }
}

@Composable
private fun DismissBackground(state: androidx.compose.material3.SwipeToDismissBoxState) {
    val target = state.targetValue
    if (target == SwipeToDismissBoxValue.Settled) {
        return
    }
    val color: Color
    val icon: ImageVector
    val tint: Color
    val alignment: Alignment
    if (target == SwipeToDismissBoxValue.StartToEnd) {
        color = MaterialTheme.colorScheme.tertiaryContainer
        icon = Icons.Filled.Check
        tint = MaterialTheme.colorScheme.onTertiaryContainer
        alignment = Alignment.CenterStart
    } else {
        color = MaterialTheme.colorScheme.errorContainer
        icon = Icons.Outlined.WarningAmber
        tint = MaterialTheme.colorScheme.onErrorContainer
        alignment = Alignment.CenterEnd
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color),
        contentAlignment = alignment
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .size(28.dp)
        )
    }
}

@Composable
private fun LessonCard(
    lesson: LessonForToday,
    modifier: Modifier = Modifier
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val currencyFormatter = rememberCurrencyFormatter()
    val startTime = remember(lesson.startAt) { lesson.startAt.atZone(zoneId).toLocalTime() }
    val timeText = remember(startTime) { timeFormatter.format(startTime) }
    val durationMinutes = remember(lesson.duration) { lesson.duration.toMinutes().toInt().coerceAtLeast(0) }
    val amount = remember(lesson.priceCents) { formatCurrency(lesson.priceCents.toLong(), currencyFormatter) }
    val subjectTitle = lesson.lessonTitle?.takeIf { it.isNotBlank() }?.trim()
        ?: lesson.subjectName?.takeIf { it.isNotBlank() }?.trim()
        ?: stringResource(id = R.string.lesson_card_subject_placeholder)
    val durationLabel = stringResource(R.string.today_duration_format, durationMinutes)
    val metaParts = listOf(subjectTitle, timeText, durationLabel, amount)

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lesson.studentName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                PaymentStatusChip(status = lesson.paymentStatus)
            }
            Text(
                text = metaParts.joinToString(separator = " • "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val note = lesson.note?.takeIf { it.isNotBlank() }
            if (note != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.StickyNote2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentStatusChip(status: PaymentStatus) {
    if (status == PaymentStatus.UNPAID) return
    val (label, container, content) = when (status) {
        PaymentStatus.PAID -> Triple(
            stringResource(R.string.lesson_status_paid),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        PaymentStatus.DUE -> Triple(
            stringResource(R.string.lesson_status_due),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        PaymentStatus.CANCELLED -> Triple(
            stringResource(R.string.lesson_status_cancelled),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        else -> Triple(
            stringResource(R.string.lesson_status_unpaid),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayTopBar(state: TodayUiState) {
    TopAppBar(
        title = { Text(text = stringResource(R.string.today_title)) },
        actions = {
            if (state is TodayUiState.Content && !state.isAllMarked) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(text = stringResource(R.string.today_remaining_count, state.pendingCount))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
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

private fun formatCurrency(amountCents: Long, formatter: NumberFormat): String {
    return formatter.format(amountCents / 100.0)
}
