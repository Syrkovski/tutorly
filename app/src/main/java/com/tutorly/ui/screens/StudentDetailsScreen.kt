package com.tutorly.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.tutorly.models.PaymentStatus
import com.tutorly.models.Student
import com.tutorly.ui.lessoncard.LessonCardExitAction
import com.tutorly.ui.lessoncard.LessonCardSheet
import com.tutorly.ui.lessoncard.LessonCardViewModel
import java.time.ZoneId
import java.time.ZonedDateTime

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

