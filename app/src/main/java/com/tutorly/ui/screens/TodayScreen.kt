package com.tutorly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import com.tutorly.ui.components.GradientTopBarContainer
import com.tutorly.ui.theme.PrimaryTextColor
import com.tutorly.ui.theme.SuccessGreen
import com.tutorly.ui.theme.TutorlyCardDefaults
import com.tutorly.R
import com.tutorly.domain.model.LessonForToday
import com.tutorly.models.PaymentStatus
import com.tutorly.ui.lessoncard.LessonCardSheet
import com.tutorly.ui.lessoncard.LessonCardViewModel
import com.tutorly.ui.icons.AppIcons
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    onAddStudent: () -> Unit = {},
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
        onAddStudent = {
            lessonCardViewModel.dismiss()
            onAddStudent()
        },
        onDateSelect = lessonCardViewModel::onDateSelected,
        onTimeSelect = lessonCardViewModel::onTimeSelected,
        onDurationSelect = lessonCardViewModel::onDurationSelected,
        onPriceChange = lessonCardViewModel::onPriceChanged,
        onStatusSelect = lessonCardViewModel::onPaymentStatusSelected,
        onNoteChange = lessonCardViewModel::onNoteChanged,
        onDeleteLesson = lessonCardViewModel::deleteLesson,
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent
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
            imageVector = AppIcons.Calendar,
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
            if (section.pending.isNotEmpty()) {
                item(key = "pending_header_${section.date}") {
                    PastSectionHeader()
                }
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
private fun PastSectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Text(
            text = stringResource(R.string.today_section_past),
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
private fun TodayStatsRow(stats: TodayStats) {
    val currencyFormatter = rememberCurrencyFormatter()
    val passedLessons = remember(stats.passedLessons) { stats.passedLessons.toString() }
    val remainingLessons = remember(stats.remainingLessons) { stats.remainingLessons.toString() }
    val paidAmount = remember(stats.paidAmountCents, currencyFormatter) {
        formatCurrency(stats.paidAmountCents, currencyFormatter)
    }
    val dueAmount = remember(stats.dueAmountCents, currencyFormatter) {
        formatCurrency(stats.dueAmountCents, currencyFormatter)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TodayStatsTile(
                label = stringResource(R.string.today_stats_passed_label),
                value = passedLessons,
                modifier = Modifier.weight(1f)
            )
            TodayStatsTile(
                label = stringResource(R.string.today_stats_paid_label),
                value = paidAmount,
                valueColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TodayStatsTile(
                label = stringResource(R.string.today_stats_remaining_label),
                value = remainingLessons,
                modifier = Modifier.weight(1f)
            )
            TodayStatsTile(
                label = stringResource(R.string.today_stats_due_label),
                value = dueAmount,
                valueColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TodayStatsTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
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
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = valueColor,
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
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val successColor = SuccessGreen
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(32.dp),
                color = successColor.copy(alpha = 0.12f),
                contentColor = successColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AppIcons.CheckCircle,
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
        icon = AppIcons.CheckCircleBold
        tint = MaterialTheme.colorScheme.onTertiaryContainer
        alignment = Alignment.CenterStart
    } else {
        color = MaterialTheme.colorScheme.errorContainer
        icon = AppIcons.Warning
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

@OptIn(ExperimentalLayoutApi::class)
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
    val grade = lesson.studentGrade?.takeIf { it.isNotBlank() }?.trim()
    val subtitle = listOfNotNull(grade, subjectTitle).joinToString(separator = " • ")
    val durationLabel = stringResource(R.string.today_duration_format, durationMinutes)
    val isFutureLesson = remember(lesson.startAt) { lesson.startAt.isAfter(Instant.now()) }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = TutorlyCardDefaults.colors(containerColor = Color.White),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = lesson.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                PaymentStatusChip(
                    status = lesson.paymentStatus,
                    isFutureLesson = isFutureLesson
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LessonMetaPill(text = timeText)
                LessonMetaPill(text = durationLabel)
                LessonMetaPill(text = amount)
            }
            val note = lesson.note?.takeIf { it.isNotBlank() }
            if (note != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.StickyNote,
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
private fun PaymentStatusChip(
    status: PaymentStatus,
    isFutureLesson: Boolean,
    modifier: Modifier = Modifier
) {
    if (status == PaymentStatus.UNPAID) return
    val label = when (status) {
        PaymentStatus.PAID -> stringResource(
            if (isFutureLesson) R.string.lesson_card_status_prepaid else R.string.lesson_status_paid
        )
        PaymentStatus.DUE -> stringResource(R.string.lesson_status_due)
        PaymentStatus.CANCELLED -> stringResource(R.string.lesson_status_cancelled)
        PaymentStatus.UNPAID -> return
    }
    val container: Color
    val content: Color
    when (status) {
        PaymentStatus.PAID -> {
            container = MaterialTheme.colorScheme.tertiaryContainer
            content = MaterialTheme.colorScheme.onTertiaryContainer
        }
        PaymentStatus.DUE -> {
            container = MaterialTheme.colorScheme.errorContainer
            content = MaterialTheme.colorScheme.onErrorContainer
        }
        else -> {
            container = MaterialTheme.colorScheme.surfaceVariant
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun LessonMetaPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayTopBar(state: TodayUiState) {
    GradientTopBarContainer {
        TopAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(135.dp),
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .padding(start = 30.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = stringResource(R.string.today_title),
                        color = Color.White
                    )
                }
            },
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
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = Color.White
            ),
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
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

private fun formatCurrency(amountCents: Long, formatter: NumberFormat): String {
    return formatter.format(amountCents / 100.0)
}
