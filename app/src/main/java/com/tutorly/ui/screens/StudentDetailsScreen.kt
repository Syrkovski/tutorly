package com.tutorly.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.models.Lesson
import com.tutorly.models.PaymentStatus
import com.tutorly.models.Student
import com.tutorly.ui.components.PaymentBadge
import com.tutorly.ui.lessoncard.LessonCardExitAction
import com.tutorly.ui.lessoncard.LessonCardSheet
import com.tutorly.ui.lessoncard.LessonCardViewModel
import java.text.NumberFormat
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailsScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCreateLesson: (Student) -> Unit = {},
    onLessonEdit: (Long, Long, ZonedDateTime) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
    vm: StudentDetailsViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val lessonCardViewModel: LessonCardViewModel = hiltViewModel()
    val lessonCardState by lessonCardViewModel.uiState.collectAsState()
    val zoneId = remember { ZoneId.systemDefault() }

    LessonCardSheet(
        state = lessonCardState,
        zoneId = zoneId,
        onDismissRequest = lessonCardViewModel::requestDismiss,
        onCancelDismiss = lessonCardViewModel::cancelDismiss,
        onConfirmDismiss = lessonCardViewModel::confirmDismiss,
        onNoteChange = lessonCardViewModel::onNoteChange,
        onSaveNote = lessonCardViewModel::saveNote,
        onMarkPaid = lessonCardViewModel::markPaid,
        onRequestMarkDue = lessonCardViewModel::requestMarkDue,
        onDismissMarkDue = lessonCardViewModel::dismissMarkDueDialog,
        onConfirmMarkDue = lessonCardViewModel::confirmMarkDue,
        onRequestEdit = lessonCardViewModel::requestEdit,
        onSnackbarConsumed = lessonCardViewModel::consumeSnackbar
    )

    val pendingExit = lessonCardState.pendingExitAction
    LaunchedEffect(pendingExit) {
        when (pendingExit) {
            is LessonCardExitAction.NavigateToEdit -> {
                val details = pendingExit.details
                onLessonEdit(
                    details.id,
                    details.studentId,
                    details.startAt.atZone(zoneId)
                )
                lessonCardViewModel.consumeExitAction()
            }
            LessonCardExitAction.Close -> {
                lessonCardViewModel.consumeExitAction()
            }
            null -> Unit
        }
    }

    Scaffold(
        topBar = {
            StudentDetailsTopBar(
                title = state.student?.name ?: stringResource(id = R.string.student_details_title_placeholder),
                onBack = onBack,
                onEdit = if (state.student != null) onEdit else null
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.student == null -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.student_details_missing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                val student = requireNotNull(state.student)
                val lessons = remember(state.lessons) {
                    state.lessons.sortedByDescending { it.startAt }
                }
                val locale = remember { Locale("ru", "RU") }
                val currencyFormatter = remember(locale) { NumberFormat.getCurrencyInstance(locale) }
                val totalEarnedCents = remember(lessons) { lessons.sumOf { it.paidCents.toLong() } }
                val formattedTotalEarned = remember(totalEarnedCents) {
                    currencyFormatter.format(totalEarnedCents / 100.0)
                }
                val formattedDebt = remember(state.totalDebtCents) {
                    currencyFormatter.format(state.totalDebtCents / 100.0)
                }
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    StudentSummaryCard(
                        student = student,
                        hasDebt = state.hasDebt,
                        totalDebt = formattedDebt
                    )
                    Button(
                        onClick = { onCreateLesson(student) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.student_details_create_lesson))
                    }
                    StudentContactCard(student = student)
                    StudentStatsRow(
                        totalLessons = lessons.size,
                        formattedTotalEarned = formattedTotalEarned,
                        paidLessons = lessons.count { it.paymentStatus == PaymentStatus.PAID },
                        hasDebt = state.hasDebt,
                        formattedDebt = formattedDebt
                    )
                    if (!student.note.isNullOrBlank()) {
                        StudentNotesCard(note = student.note!!)
                    }
                    LessonsHistoryCard(
                        lessons = lessons,
                        currencyFormatter = currencyFormatter,
                        locale = locale,
                        onLessonClick = { lesson -> lessonCardViewModel.open(lesson.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentDetailsTopBar(
    title: String,
    onBack: () -> Unit,
    onEdit: (() -> Unit)?
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.student_details_back)
                )
            }
        },
        actions = {
            onEdit?.let {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(id = R.string.student_details_edit)
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
private fun StudentSummaryCard(
    student: Student,
    hasDebt: Boolean,
    totalDebt: String,
    modifier: Modifier = Modifier
) {
    val subtitle = remember(student.note) {
        student.note
            ?.lineSequence()
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = student.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = subtitle ?: stringResource(id = R.string.student_details_header_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaymentBadge(paid = !hasDebt)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (hasDebt) {
                        stringResource(id = R.string.student_details_debt_amount, totalDebt)
                    } else {
                        stringResource(id = R.string.student_details_no_debt)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun StudentContactCard(
    student: Student,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.student_details_contact_title),
                style = MaterialTheme.typography.titleMedium
            )
            ContactInfoRow(
                icon = Icons.Outlined.Phone,
                label = stringResource(id = R.string.student_details_phone_label),
                value = student.phone,
                placeholder = stringResource(id = R.string.student_details_phone_placeholder)
            )
            ContactInfoRow(
                icon = Icons.Outlined.Message,
                label = stringResource(id = R.string.student_details_messenger_label),
                value = student.messenger,
                placeholder = stringResource(id = R.string.student_details_messenger_placeholder)
            )
        }
    }
}

@Composable
private fun ContactInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String?,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = CircleShape,
            tonalElevation = 2.dp
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(12.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value?.takeIf { it.isNotBlank() } ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value.isNullOrBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StudentStatsRow(
    totalLessons: Int,
    formattedTotalEarned: String,
    paidLessons: Int,
    hasDebt: Boolean,
    formattedDebt: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryStatCard(
            modifier = Modifier.weight(1f),
            value = totalLessons.toString(),
            label = stringResource(id = R.string.student_details_stats_lessons)
        )
        SummaryStatCard(
            modifier = Modifier.weight(1f),
            value = formattedTotalEarned,
            label = stringResource(id = R.string.student_details_stats_earned)
        )
        val (thirdValue, thirdLabel) = if (hasDebt) {
            formattedDebt to stringResource(id = R.string.student_details_stats_debt)
        } else {
            paidLessons.toString() to stringResource(id = R.string.student_details_stats_paid)
        }
        SummaryStatCard(
            modifier = Modifier.weight(1f),
            value = thirdValue,
            label = thirdLabel
        )
    }
}

@Composable
private fun SummaryStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StudentNotesCard(
    note: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.student_details_notes_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LessonsHistoryCard(
    lessons: List<Lesson>,
    currencyFormatter: NumberFormat,
    locale: Locale,
    onLessonClick: (Lesson) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("d MMMM", locale)
    }
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("HH:mm", locale)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.student_details_history_title),
                style = MaterialTheme.typography.titleMedium
            )
            if (lessons.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.student_details_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    lessons.forEachIndexed { index, lesson ->
                        LessonHistoryRow(
                            lesson = lesson,
                            currencyFormatter = currencyFormatter,
                            dateFormatter = dateFormatter,
                            timeFormatter = timeFormatter,
                            onClick = onLessonClick
                        )
                        if (index != lessons.lastIndex) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonHistoryRow(
    lesson: Lesson,
    currencyFormatter: NumberFormat,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    onClick: (Lesson) -> Unit,
    modifier: Modifier = Modifier
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val start = remember(lesson.startAt) { lesson.startAt.atZone(zoneId) }
    val end = remember(lesson.endAt) { lesson.endAt.atZone(zoneId) }
    val durationMinutes = remember(lesson.startAt, lesson.endAt) {
        Duration.between(lesson.startAt, lesson.endAt).toMinutes().toInt().coerceAtLeast(0)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(lesson) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dateFormatter.format(start),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    id = R.string.student_details_history_time_range,
                    timeFormatter.format(start),
                    timeFormatter.format(end),
                    durationMinutes
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = currencyFormatter.format(lesson.priceCents / 100.0),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            LessonPaymentStatusBadge(status = lesson.paymentStatus)
        }
    }
}

@Composable
private fun LessonPaymentStatusBadge(
    status: PaymentStatus,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val (labelRes, container, content) = when (status) {
        PaymentStatus.PAID -> Triple(
            R.string.lesson_status_paid,
            colorScheme.tertiaryContainer,
            colorScheme.onTertiaryContainer
        )
        PaymentStatus.DUE, PaymentStatus.UNPAID -> Triple(
            R.string.lesson_status_due,
            colorScheme.errorContainer,
            colorScheme.onErrorContainer
        )
        PaymentStatus.CANCELLED -> Triple(
            R.string.lesson_status_cancelled,
            colorScheme.surfaceVariant,
            colorScheme.onSurfaceVariant
        )
    }
    Surface(
        modifier = modifier,
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 1.dp
    ) {
        Text(
            text = stringResource(id = labelRes),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
