package com.tutorly.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.domain.model.LessonForToday
import com.tutorly.models.PaymentStatus
import com.tutorly.ui.components.GradientTopBarContainer
import com.tutorly.ui.lessoncard.LessonCardSheet
import com.tutorly.ui.lessoncard.LessonCardViewModel
import com.tutorly.ui.theme.TutorlyCardDefaults
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
    onAddLesson: () -> Unit = {},
    onAddStudent: () -> Unit = {},
    onOpenStudentProfile: (Long) -> Unit = {},
    onOpenDebtors: () -> Unit = {},
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lessonCardViewModel: LessonCardViewModel = hiltViewModel()
    val lessonCardState by lessonCardViewModel.uiState.collectAsState()
    var showCloseDayDialog by rememberSaveable { mutableStateOf(false) }

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

    if (showCloseDayDialog) {
        ConfirmCloseDayDialog(
            onConfirm = {
                showCloseDayDialog = false
                viewModel.onDayCloseConfirmed()
            },
            onDismiss = { showCloseDayDialog = false }
        )
    }

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
                TodayUiState.Empty -> EmptyState(onAddLesson = onAddLesson)
                is TodayUiState.DayInProgress -> DayInProgressContent(
                    state = state,
                    onSwipeRight = viewModel::onSwipeRight,
                    onSwipeLeft = viewModel::onSwipeLeft,
                    onLessonOpen = { lessonId ->
                        lessonCardViewModel.open(lessonId)
                    },
                    onRequestCloseDay = { showCloseDayDialog = true }
                )
                is TodayUiState.DayClosed -> DayClosedContent(
                    state = state,
                    onOpenStudentProfile = onOpenStudentProfile,
                    onOpenDebtors = onOpenDebtors
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
private fun EmptyState(onAddLesson: () -> Unit) {
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
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onAddLesson) {
            Text(text = stringResource(R.string.today_empty_add_button))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DayInProgressContent(
    state: TodayUiState.DayInProgress,
    onSwipeRight: (Long) -> Unit,
    onSwipeLeft: (Long) -> Unit,
    onLessonOpen: (Long) -> Unit,
    onRequestCloseDay: () -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "summary") {
            DayProgressSummary(
                completed = state.completedLessons,
                total = state.totalLessons,
                remaining = state.remainingLessons
            )
        }
        if (state.canCloseDay) {
            item(key = "close_day_callout") {
                CloseDayCallout(onRequestCloseDay = onRequestCloseDay)
            }
        }
        items(state.lessons, key = { it.id }) { lesson ->
            TodayLessonRow(
                lesson = lesson,
                onSwipeRight = onSwipeRight,
                onSwipeLeft = onSwipeLeft,
                onClick = {},
                onLongPress = { onLessonOpen(lesson.id) }
            )
        }
    }
}

@Composable
private fun DayProgressSummary(
    completed: Int,
    total: Int,
    remaining: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Text(
                text = stringResource(R.string.today_progress_summary, completed, total),
                style = MaterialTheme.typography.titleMedium
            )
            val remainingText = pluralStringResource(
                id = R.plurals.today_progress_remaining,
                count = remaining,
                remaining
            )
            Text(
                text = remainingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CloseDayCallout(onRequestCloseDay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Text(
                text = stringResource(R.string.today_close_day_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.today_close_day_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRequestCloseDay) {
                Text(text = stringResource(R.string.today_close_day_action))
            }
        }
    }
}

@Composable
private fun ConfirmCloseDayDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.today_close_day_dialog_title)) },
        text = { Text(text = stringResource(R.string.today_close_day_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.today_close_day_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.today_close_day_dialog_dismiss))
            }
        }
    )
}

@Composable
private fun DayClosedContent(
    state: TodayUiState.DayClosed,
    onOpenStudentProfile: (Long) -> Unit,
    onOpenDebtors: () -> Unit
) {
    val currencyFormatter = rememberCurrencyFormatter()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "summary") {
            DayClosedSummary(
                paidAmountCents = state.paidAmountCents,
                dueAmountCents = state.todayDueAmountCents,
                formatter = currencyFormatter
            )
        }
        item(key = "today_debtors") {
            TodayDebtorsSection(
                debtors = state.todayDebtors,
                formatter = currencyFormatter,
                onOpenStudentProfile = onOpenStudentProfile
            )
        }
        if (state.lessons.isNotEmpty()) {
            item(key = "closed_lessons") {
                ClosedDayLessonsSection(lessons = state.lessons)
            }
        }
        if (state.pastDebtorsPreview.isNotEmpty()) {
            item(key = "past_debtors") {
                PastDebtorsCollapsible(
                    debtors = state.pastDebtorsPreview,
                    formatter = currencyFormatter,
                    onOpenStudentProfile = onOpenStudentProfile,
                    onOpenDebtors = onOpenDebtors,
                    hasMore = state.hasMorePastDebtors
                )
            }
        }
    }
}

@Composable
private fun DayClosedSummary(
    paidAmountCents: Long,
    dueAmountCents: Long,
    formatter: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = TutorlyCardDefaults.colors(containerColor = Color.White),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.today_closed_summary_title),
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.today_closed_income_label),
                    value = formatCurrency(paidAmountCents, formatter),
                    valueColor = MaterialTheme.colorScheme.tertiary
                )
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.today_closed_debt_label),
                    value = formatCurrency(dueAmountCents, formatter),
                    valueColor = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    modifier: Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TodayDebtorsSection(
    debtors: List<TodayDebtor>,
    formatter: NumberFormat,
    onOpenStudentProfile: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.today_debtors_today_title),
            style = MaterialTheme.typography.titleMedium
        )
        if (debtors.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Text(
                    text = stringResource(R.string.today_debtors_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                debtors.forEach { debtor ->
                    DebtorCard(
                        debtor = debtor,
                        formatter = formatter,
                        onOpenStudentProfile = onOpenStudentProfile
                    )
                }
            }
        }
    }
}

@Composable
private fun ClosedDayLessonsSection(lessons: List<LessonForToday>) {
    val subtitle = stringResource(
        R.string.today_closed_lessons_section_subtitle,
        lessons.size
    )
    CollapsibleCard(
        title = stringResource(R.string.today_closed_lessons_section_title),
        subtitle = subtitle
    ) {
        lessons.forEach { lesson ->
            LessonCard(
                lesson = lesson,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PastDebtorsCollapsible(
    debtors: List<TodayDebtor>,
    formatter: NumberFormat,
    onOpenStudentProfile: (Long) -> Unit,
    onOpenDebtors: () -> Unit,
    hasMore: Boolean
) {
    val subtitle = if (hasMore) {
        stringResource(R.string.today_debtors_past_subtitle_more, debtors.size)
    } else {
        stringResource(R.string.today_debtors_past_subtitle, debtors.size)
    }
    CollapsibleCard(
        title = stringResource(R.string.today_debtors_past_title),
        subtitle = subtitle
    ) {
        debtors.forEach { debtor ->
            DebtorCard(
                debtor = debtor,
                formatter = formatter,
                onOpenStudentProfile = onOpenStudentProfile
            )
        }
        if (hasMore) {
            FilledTonalButton(onClick = onOpenDebtors) {
                Text(text = stringResource(R.string.today_debtors_more_cta))
            }
        }
    }
}

@Composable
private fun CollapsibleCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = TutorlyCardDefaults.colors(containerColor = Color.White),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DebtorCard(
    debtor: TodayDebtor,
    formatter: NumberFormat,
    onOpenStudentProfile: (Long) -> Unit
) {
    val lessonCountText = pluralStringResource(
        id = R.plurals.today_debtor_lessons,
        count = debtor.lessonCount,
        debtor.lessonCount
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onOpenStudentProfile(debtor.studentId) },
                onLongClick = { onOpenStudentProfile(debtor.studentId) }
            ),
        shape = MaterialTheme.shapes.large,
        colors = TutorlyCardDefaults.colors(containerColor = Color.White),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = debtor.studentName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lessonCountText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrency(debtor.amountCents, formatter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TodayLessonRow(
    lesson: LessonForToday,
    onSwipeRight: (Long) -> Unit,
    onSwipeLeft: (Long) -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit
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
        modifier = Modifier.fillMaxWidth(),
        backgroundContent = { DismissBackground(state = dismissState) }
    ) {
        LessonCard(
            lesson = lesson,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongPress
                )
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

@OptIn(ExperimentalFoundationApi::class)
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
            androidx.compose.foundation.layout.FlowRow(
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
                when (state) {
                    is TodayUiState.DayInProgress -> {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = stringResource(
                                        R.string.today_remaining_count,
                                        state.remainingLessons
                                    )
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    is TodayUiState.DayClosed -> {
                        AssistChip(
                            onClick = {},
                            label = { Text(text = stringResource(R.string.today_topbar_closed)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    else -> Unit
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = Color.White
            ),
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
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
