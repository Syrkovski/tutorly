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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.domain.model.LessonForToday
import com.tutorly.models.PaymentStatus
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

    var noteLesson by remember { mutableStateOf<LessonForToday?>(null) }
    var noteDraft by rememberSaveable(noteLesson?.id) { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    onLessonClick = { lesson ->
                        noteLesson = lesson
                        noteDraft = lesson.note.orEmpty()
                    }
                )
            }
        }
    }

    noteLesson?.let { lesson ->
        ModalBottomSheet(
            onDismissRequest = { noteLesson = null },
            sheetState = sheetState
        ) {
            LessonNoteSheet(
                lesson = lesson,
                text = noteDraft,
                onTextChange = { noteDraft = it },
                onCancel = { noteLesson = null },
                onSave = { value ->
                    viewModel.onNoteSave(lesson.id, value.ifBlank { null })
                    noteLesson = null
                }
            )
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
    onLessonClick: (LessonForToday) -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(state.lessons, key = { _, item -> item.id }) { index, lesson ->
            if (state.remainingCount > 0 && index == state.remainingCount) {
                MarkedSectionHeader()
            }
            TodayLessonRow(
                lesson = lesson,
                onSwipeRight = onSwipeRight,
                onSwipeLeft = onSwipeLeft,
                onClick = { onLessonClick(lesson) }
            )
        }
    }
}

@Composable
private fun MarkedSectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
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
                .clickable { onClick() }
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

@Composable
private fun LessonNoteSheet(
    lesson: LessonForToday,
    text: String,
    onTextChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: (String) -> Unit
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val start = remember(lesson.startAt) { lesson.startAt.atZone(zoneId).toLocalTime() }
    val durationMinutes = remember(lesson.duration) { lesson.duration.toMinutes().toInt().coerceAtLeast(0) }
    val subtitleParts = buildList {
        add(stringResource(R.string.today_note_time, timeFormatter.format(start)))
        add(stringResource(R.string.today_duration_format, durationMinutes))
        lesson.subjectName?.takeIf { it.isNotBlank() }?.trim()?.let { add(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.today_note_title, lesson.studentName),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = subtitleParts.joinToString(separator = " • "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            placeholder = { Text(stringResource(R.string.today_note_placeholder)) }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.today_note_cancel))
            }
            Button(onClick = { onSave(text) }, modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.today_note_save))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayTopBar(state: TodayUiState) {
    TopAppBar(
        title = { Text(text = stringResource(R.string.today_title)) },
        actions = {
            if (state is TodayUiState.Content) {
                if (state.isAllMarked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.today_all_marked),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(text = stringResource(R.string.today_remaining_count, state.remainingCount))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
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
