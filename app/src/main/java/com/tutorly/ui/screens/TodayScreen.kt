package com.tutorly.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.CurrencyRuble
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.domain.model.LessonForToday
import com.tutorly.models.PaymentStatus
import com.tutorly.ui.components.GradientTopBarContainer
import com.tutorly.ui.components.statusChipData
import com.tutorly.ui.lessoncard.LessonCardSheet
import com.tutorly.ui.lessoncard.LessonCardViewModel
import com.tutorly.ui.theme.DebtChipContent
import com.tutorly.ui.theme.DebtChipFill
import com.tutorly.ui.theme.PaidChipContent
import com.tutorly.ui.theme.extendedColors
import com.tutorly.ui.theme.TutorlyCardDefaults
import java.text.NumberFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
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
        topBar = { TodayTopBar(state = uiState, onReopenDay = viewModel::onReopenDay) },
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
                is TodayUiState.Empty -> EmptyState(
                    state = state,
                    onSwipeRight = viewModel::onSwipeRight,
                    onSwipeLeft = viewModel::onSwipeLeft,
                    onLessonOpen = { lessonId ->
                        lessonCardViewModel.open(lessonId)
                    },
                    onOpenStudentProfile = onOpenStudentProfile,
                    onOpenDebtors = onOpenDebtors
                )
                is TodayUiState.ReviewPending -> ReviewPendingContent(
                    state = state,
                    onSwipeRight = viewModel::onSwipeRight,
                    onSwipeLeft = viewModel::onSwipeLeft,
                    onLessonOpen = { lessonId ->
                        lessonCardViewModel.open(lessonId)
                    },
                    onOpenStudentProfile = onOpenStudentProfile,
                    onOpenDebtors = onOpenDebtors,
                    onRequestCloseDay = { showCloseDayDialog = true }
                )
                is TodayUiState.DayInProgress -> DayInProgressContent(
                    state = state,
                    onSwipeRight = viewModel::onSwipeRight,
                    onSwipeLeft = viewModel::onSwipeLeft,
                    onLessonOpen = { lessonId ->
                        lessonCardViewModel.open(lessonId)
                    },
                    onOpenStudentProfile = onOpenStudentProfile,
                    onOpenDebtors = onOpenDebtors,
                    onRequestCloseDay = { showCloseDayDialog = true }
                )
                is TodayUiState.DayClosed -> DayClosedContent(
                    state = state,
                    onLessonOpen = { lessonId ->
                        lessonCardViewModel.open(lessonId)
                    },
                    onSwipeRight = viewModel::onSwipeRight,
                    onSwipeLeft = viewModel::onSwipeLeft,
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
private fun EmptyState(
    state: TodayUiState.Empty,
    onSwipeRight: (Long) -> Unit,
    onSwipeLeft: (Long) -> Unit,
    onLessonOpen: (Long) -> Unit,
    onOpenStudentProfile: (Long) -> Unit,
    onOpenDebtors: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "empty_state_header") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = TutorlyCardDefaults.colors(containerColor = Color.White),
                elevation = TutorlyCardDefaults.elevation()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.vacation),
                        contentDescription = null,
                        modifier = Modifier.size(width = 256.dp, height = 192.dp)
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
        }
        item(key = "past_debtors") {
            PastDebtorsCollapsible(
                lessons = state.pastDueLessonsPreview,
                onSwipeRight = onSwipeRight,
                onSwipeLeft = onSwipeLeft,
                onLessonOpen = onLessonOpen,
                onOpenStudentProfile = onOpenStudentProfile,
                onOpenDebtors = onOpenDebtors,
                hasMore = state.hasMorePastDueLessons,
                titleTextAlign = TextAlign.Start
            )
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
    onOpenStudentProfile: (Long) -> Unit,
    onOpenDebtors: () -> Unit,
    onRequestCloseDay: () -> Unit
) {
    val listState = rememberLazyListState()
    val (pendingLessons, markedLessons) = remember(state.lessons) {
        state.lessons.partition { it.paymentStatus == PaymentStatus.UNPAID }
    }
    val allLessonsCompleted = remember(state.completedLessons, state.totalLessons) {
        state.totalLessons > 0 && state.completedLessons == state.totalLessons
    }
    val showProgressSummary = !state.showCloseDayCallout
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showProgressSummary) {
            item(key = "summary") {
                DayProgressSummary(
                    completed = state.completedLessons,
                    total = state.totalLessons,
                    remaining = state.remainingLessons,
                    allLessonsCompleted = allLessonsCompleted
                )
            }
        }
        if (showProgressSummary && (pendingLessons.isNotEmpty() || markedLessons.isNotEmpty())) {
            item(key = "lessons_header") {
                Text(
                    text = stringResource(id = R.string.today_lessons_header),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )
            }
        }
        if (state.showCloseDayCallout) {
            item(key = "close_day_callout") {
                CloseDayCallout(onRequestCloseDay = onRequestCloseDay)
            }
        }
        if (pendingLessons.isNotEmpty()) {
            item(key = "pending_lessons") {
                LessonsList(
                    lessons = pendingLessons,
                    onSwipeRight = onSwipeRight,
                    onSwipeLeft = onSwipeLeft,
                    onLessonOpen = onLessonOpen,
                    onOpenStudentProfile = onOpenStudentProfile,
                    onLessonLongPress = { lesson -> onLessonOpen(lesson.id) }
                )
            }
        }
        if (markedLessons.isNotEmpty()) {
            item(key = "marked_header") {
                SectionHeader(text = stringResource(id = R.string.today_marked_section_title))
            }
            item(key = "marked_lessons") {
                LessonsList(
                    lessons = markedLessons,
                    onSwipeRight = onSwipeRight,
                    onSwipeLeft = onSwipeLeft,
                    onLessonOpen = onLessonOpen,
                    onOpenStudentProfile = onOpenStudentProfile,
                    onLessonLongPress = { lesson -> onLessonOpen(lesson.id) }
                )
            }
        }
        item(key = "past_debtors") {
            PastDebtorsCollapsible(
                lessons = state.pastDueLessonsPreview,
                onSwipeRight = onSwipeRight,
                onSwipeLeft = onSwipeLeft,
                onLessonOpen = onLessonOpen,
                onOpenStudentProfile = onOpenStudentProfile,
                onOpenDebtors = onOpenDebtors,
                hasMore = state.hasMorePastDueLessons,
                titleTextAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun ReviewPendingContent(
    state: TodayUiState.ReviewPending,
    onSwipeRight: (Long) -> Unit,
    onSwipeLeft: (Long) -> Unit,
    onLessonOpen: (Long) -> Unit,
    onOpenStudentProfile: (Long) -> Unit,
    onOpenDebtors: () -> Unit,
    onRequestCloseDay: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "review_summary") {
            ReviewSummaryCard(
                remaining = state.reviewLessons.size,
                total = state.totalLessons,
                showCloseDayButton = state.showCloseDayButton,
                onRequestCloseDay = onRequestCloseDay
            )
        }
        if (state.reviewLessons.isNotEmpty()) {
            item(key = "review_carousel") {
                LessonsReviewCarousel(
                    lessons = state.reviewLessons,
                    onSwipeRight = onSwipeRight,
                    onSwipeLeft = onSwipeLeft,
                    onLessonOpen = onLessonOpen,
                    onOpenStudentProfile = onOpenStudentProfile
                )
            }
        }
        item(key = "review_marked_header") {
            SectionHeader(text = stringResource(id = R.string.today_review_marked_section))
        }
        item(key = "review_marked_list") {
            if (state.markedLessons.isEmpty()) {
                ReviewEmptyMarkedCard()
            } else {
                LessonsList(
                    lessons = state.markedLessons,
                    onSwipeRight = onSwipeRight,
                    onSwipeLeft = onSwipeLeft,
                    onLessonOpen = onLessonOpen,
                    onOpenStudentProfile = onOpenStudentProfile,
                )
            }
        }
        item(key = "past_debtors") {
            PastDebtorsCollapsible(
                lessons = state.pastDueLessonsPreview,
                onSwipeRight = onSwipeRight,
                onSwipeLeft = onSwipeLeft,
                onLessonOpen = onLessonOpen,
                onOpenStudentProfile = onOpenStudentProfile,
                onOpenDebtors = onOpenDebtors,
                hasMore = state.hasMorePastDueLessons,
                titleTextAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun ReviewSummaryCard(
    remaining: Int,
    total: Int,
    showCloseDayButton: Boolean,
    onRequestCloseDay: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.undraw_to_do_list_o3jf),
                    contentDescription = null,
                    modifier = Modifier.size(width = 240.dp, height = 148.dp)
                )

                val titleRes = if (showCloseDayButton) {
                    R.string.today_review_ready_title
                } else {
                    R.string.today_review_title
                }
                Text(
                    text = stringResource(id = titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                if (showCloseDayButton) {
//                    Text(
//                        text = stringResource(id = R.string.today_review_ready_subtitle),
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
                } else if (total > 0 && remaining > 0) {
                    Text(
                        text = stringResource(id = R.string.today_review_progress, remaining, total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (showCloseDayButton) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRequestCloseDay
                    ) {
                        Text(text = stringResource(id = R.string.today_close_day_action))
                    }
                }

            }
        }
//        if (!showCloseDayButton) {
//            Text(
//                text = stringResource(id = R.string.today_review_subtitle),
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant,
//                textAlign = TextAlign.Center,
//                modifier = Modifier.fillMaxWidth()
//            )
//        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun LessonsReviewCarousel(
    lessons: List<LessonForToday>,
    onSwipeRight: (Long) -> Unit,
    onSwipeLeft: (Long) -> Unit,
    onLessonOpen: (Long) -> Unit,
    onOpenStudentProfile: (Long) -> Unit
) {
    val currentLesson = lessons.firstOrNull()
    if (currentLesson == null) {
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.today_review_hint_due),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = stringResource(id = R.string.today_review_hint_paid),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        TodayLessonRow(
            lesson = currentLesson,
            onSwipeRight = onSwipeRight,
            onSwipeLeft = onSwipeLeft,
            onClick = { onLessonOpen(currentLesson.id) },
            onLongPress = { onOpenStudentProfile(currentLesson.studentId) },
            cardElevation = TutorlyCardDefaults.elevation()
        )
    }
}

@Composable
private fun ReviewEmptyMarkedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = TutorlyCardDefaults.colors(containerColor = Color.White),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.today_review_empty_marked),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun DayProgressSummary(
    completed: Int,
    total: Int,
    remaining: Int,
    allLessonsCompleted: Boolean
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        )  {
            Image(
                painter = painterResource(id = R.drawable.focused),
                contentDescription = null,
                modifier = Modifier.size(width = 178.dp, height = 154.dp)
            )
            val summaryText = if (allLessonsCompleted) {
                stringResource(id = R.string.today_progress_all_done)
            } else {
                stringResource(R.string.today_progress_summary, completed, total)
            }
            Text(
                text = summaryText,
                style = MaterialTheme.typography.titleMedium
            )
            if (!allLessonsCompleted) {
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
    onLessonOpen: (Long) -> Unit,
    onSwipeRight: (Long) -> Unit,
    onSwipeLeft: (Long) -> Unit,
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
                lessons = state.todayDueLessons,
                onSwipeRight = onSwipeRight,
                onSwipeLeft = onSwipeLeft,
                onLessonOpen = onLessonOpen,
                onOpenStudentProfile = onOpenStudentProfile
            )
        }
        if (state.lessons.isNotEmpty()) {
            item(key = "closed_lessons") {
                ClosedDayLessonsSection(
                    lessons = state.lessons,
                    onLessonOpen = onLessonOpen
                )
            }
        }
        item(key = "past_debtors") {
            PastDebtorsCollapsible(
                lessons = state.pastDueLessonsPreview,
                onSwipeRight = onSwipeRight,
                onSwipeLeft = onSwipeLeft,
                onLessonOpen = onLessonOpen,
                onOpenStudentProfile = onOpenStudentProfile,
                onOpenDebtors = onOpenDebtors,
                hasMore = state.hasMorePastDueLessons,
                titleTextAlign = TextAlign.Start
            )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
    lessons: List<LessonForToday>,
    onSwipeRight: (Long) -> Unit,
    onSwipeLeft: (Long) -> Unit,
    onLessonOpen: (Long) -> Unit,
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
        if (lessons.isEmpty()) {
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
            LessonsList(
                lessons = lessons,
                onSwipeRight = onSwipeRight,
                onSwipeLeft = onSwipeLeft,
                onLessonOpen = onLessonOpen,
                onOpenStudentProfile = onOpenStudentProfile,
                showLessonDate = false,
            )
        }
    }
}

@Composable
private fun ClosedDayLessonsSection(
    lessons: List<LessonForToday>,
    onLessonOpen: (Long) -> Unit
) {
    val subtitle = stringResource(
        R.string.today_closed_lessons_section_subtitle,
        lessons.size
    )
    CollapsibleSection(
        title = stringResource(R.string.today_closed_lessons_section_title),
        subtitle = subtitle
    ) {
        lessons.forEach { lesson ->
            LessonCard(
                lesson = lesson,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLessonOpen(lesson.id) },
                cardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            )
        }
    }
}

@Composable
private fun PastDebtorsCollapsible(
    lessons: List<LessonForToday>,
    onSwipeRight: (Long) -> Unit,
    onSwipeLeft: (Long) -> Unit,
    onLessonOpen: (Long) -> Unit,
    onOpenStudentProfile: (Long) -> Unit,
    onOpenDebtors: () -> Unit,
    hasMore: Boolean,
    titleTextAlign: TextAlign = TextAlign.Center
) {
    val subtitle = if (hasMore) {
        stringResource(R.string.today_debtors_past_subtitle_more, lessons.size)
    } else {
        stringResource(R.string.today_debtors_past_subtitle, lessons.size)
    }
    CollapsibleSection(
        title = stringResource(R.string.today_debtors_past_title),
        titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
        titleTextAlign = titleTextAlign,
        subtitle = subtitle,
//        inlineIndicator = true
    ) {
        if (lessons.isEmpty()) {
            Text(
                text = stringResource(R.string.today_debtors_past_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LessonsList(
                lessons = lessons,
                onSwipeRight = onSwipeRight,
                onSwipeLeft = onSwipeLeft,
                onLessonOpen = onLessonOpen,
                onOpenStudentProfile = onOpenStudentProfile,
                cardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                onLessonLongPress = { lesson -> onOpenStudentProfile(lesson.studentId) },
                showLessonDate = true,
            )
        }
        if (hasMore) {
            Button(onClick = onOpenDebtors) {
                Text(text = stringResource(R.string.today_debtors_more_cta))
            }
        }
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    titleTextAlign: TextAlign = TextAlign.Start,
    inlineIndicator: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        if (inlineIndicator) {
            val boxAlignment = when (titleTextAlign) {
                TextAlign.Center -> Alignment.Center
                TextAlign.End -> Alignment.CenterEnd
                else -> Alignment.CenterStart
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.align(boxAlignment),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = titleColor,
                            textAlign = titleTextAlign
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = titleColor,
                        textAlign = titleTextAlign,
                        modifier = Modifier.fillMaxWidth()
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
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun LessonsList(
    lessons: List<LessonForToday>,
    onSwipeRight: (Long) -> Unit,
    onSwipeLeft: (Long) -> Unit,
    onLessonOpen: (Long) -> Unit,
    onOpenStudentProfile: (Long) -> Unit,
    cardElevation: CardElevation = TutorlyCardDefaults.elevation(),
    onLessonLongPress: (LessonForToday) -> Unit = { lesson ->
        onOpenStudentProfile(lesson.studentId)
    },
    showLessonDate: Boolean = false,
) {
    if (lessons.isEmpty()) {
        return
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        lessons.forEachIndexed { index, lesson ->
            TodayLessonRow(
                lesson = lesson,
                onSwipeRight = onSwipeRight,
                onSwipeLeft = onSwipeLeft,
                onClick = { onLessonOpen(lesson.id) },
                onLongPress = { onLessonLongPress(lesson) },
                cardElevation = cardElevation,
                showLessonDate = showLessonDate,
            )
            Spacer(modifier = Modifier.height(12.dp))
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
    onLongPress: () -> Unit,
    cardElevation: CardElevation = TutorlyCardDefaults.elevation(),
    showLessonDate: Boolean = false,
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


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier.weight(1f),
            backgroundContent = { DismissBackground(state = dismissState) }
        ) {
            LessonCard(
                lesson = lesson,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongPress
                    ),
                cardElevation = cardElevation,
                showLessonDate = showLessonDate,
            )
        }
    }
}

@Composable
private fun DismissBackground(state: androidx.compose.material3.SwipeToDismissBoxState) {
    val offset by rememberDismissOffset(state)
    val dismissValue = when (state.targetValue) {
        SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart -> state.targetValue
        else -> when {
            (offset ?: 0f) > 0f -> SwipeToDismissBoxValue.StartToEnd
            (offset ?: 0f) < 0f -> SwipeToDismissBoxValue.EndToStart
            else -> null
        }
    } ?: return

    val (color, alignment, tint) = when (dismissValue) {
        SwipeToDismissBoxValue.StartToEnd -> Triple(
            MaterialTheme.extendedColors.accent,
            Alignment.CenterStart,
            PaidChipContent
        )

        SwipeToDismissBoxValue.EndToStart -> Triple(
            DebtChipFill,
            Alignment.CenterEnd,
            DebtChipContent
        )

        SwipeToDismissBoxValue.Settled -> return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(color),
        contentAlignment = alignment
    ) {
        Icon(
            imageVector = Icons.Outlined.CurrencyRuble,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .size(28.dp)
        )
    }
}

@Composable
private fun rememberDismissOffset(
    state: androidx.compose.material3.SwipeToDismissBoxState
): State<Float?> {
    val offsetState = remember(state) { mutableStateOf<Float?>(null) }
    LaunchedEffect(state) {
        snapshotFlow {
            try {
                state.requireOffset()
            } catch (_: IllegalStateException) {
                null
            }
        }
            .distinctUntilChanged()
            .collect { value ->
                offsetState.value = value
            }
    }
    return offsetState
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LessonCard(
    lesson: LessonForToday,
    modifier: Modifier = Modifier,
    cardColors: CardColors = TutorlyCardDefaults.colors(containerColor = Color.White),
    cardElevation: CardElevation = TutorlyCardDefaults.elevation(),
    showLessonDate: Boolean = false,
) {
    val context = LocalContext.current
    val zoneId = remember { ZoneId.systemDefault() }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val currencyFormatter = rememberCurrencyFormatter()
    val start = remember(lesson.startAt) { lesson.startAt.atZone(zoneId) }
    val end = remember(lesson.endAt) { lesson.endAt.atZone(zoneId) }
    val startTime = remember(start) { start.toLocalTime() }
    val timeText = remember(startTime) { timeFormatter.format(startTime) }
    val durationMinutes = remember(lesson.duration) { lesson.duration.toMinutes().toInt().coerceAtLeast(0) }
    val amount = remember(lesson.priceCents) { formatCurrency(lesson.priceCents.toLong(), currencyFormatter) }
    val studentName = remember(lesson.studentName) { lesson.studentName }
    val normalizedLessonTitle = lesson.lessonTitle
        ?.takeIf { it.isNotBlank() }
        ?.trim()
    val normalizedSubjectName = lesson.subjectName
        ?.takeIf { it.isNotBlank() }
        ?.trim()
    val subjectTitle = normalizedLessonTitle
        ?: normalizedSubjectName
        ?: stringResource(id = R.string.lesson_card_subject_placeholder)
    val grade = normalizeGrade(lesson.studentGrade)
    val subtitle = listOfNotNull(grade, subjectTitle).joinToString(separator = " • ")
    val durationLabel = stringResource(R.string.today_duration_format, durationMinutes)
    val locale = remember(context) {
        val locales = context.resources.configuration.locales
        if (locales.isEmpty) Locale.getDefault() else locales[0]
    }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM yyyy", locale) }
    val lessonDateText = remember(start, locale) {
        dateFormatter.format(start.toLocalDate()).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
    }
    val statusData = statusChipData(
        paymentStatus = lesson.paymentStatus,
        start = start,
        end = end,
        now = ZonedDateTime.now(zoneId)
    )

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = cardColors,
        elevation = cardElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (showLessonDate) {
                        Text(
                            text = lessonDateText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = studentName,
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
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(16.dp)
                    .background(statusData.background)
            )
        }
    }
}

@Composable
private fun LessonMetaPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
private fun TodayTopBar(state: TodayUiState, onReopenDay: () -> Unit) {
    GradientTopBarContainer {
        val titleRes = when (state) {
            is TodayUiState.DayClosed -> R.string.today_topbar_closed
            else -> R.string.today_title
        }
        val canReopen = (state as? TodayUiState.DayClosed)?.canReopen == true
        CenterAlignedTopAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp, bottom = 0.dp),
//                .height(80.dp),

            title = {
                Text(
                    text = stringResource(titleRes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            actions = {
                if (canReopen) {
                    IconButton(onClick = onReopenDay) {
                        Icon(
                            imageVector = Icons.Outlined.LockOpen,
                            contentDescription = stringResource(R.string.today_reopen_day_action),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
