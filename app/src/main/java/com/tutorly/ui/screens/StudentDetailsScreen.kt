package com.tutorly.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.sharedBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.domain.model.StudentProfile
import com.tutorly.domain.model.StudentProfileLesson
import com.tutorly.models.PaymentStatus
import com.tutorly.ui.components.GradientTopBarContainer
import com.tutorly.ui.components.PaymentBadge
import com.tutorly.ui.components.PaymentBadgeStatus
import com.tutorly.ui.lessoncard.LessonCardSheet
import com.tutorly.ui.lessoncard.LessonCardViewModel
import com.tutorly.ui.lessoncreation.LessonCreationConfig
import com.tutorly.ui.lessoncreation.LessonCreationOrigin
import com.tutorly.ui.lessoncreation.LessonCreationSheet
import com.tutorly.ui.lessoncreation.LessonCreationViewModel
import com.tutorly.ui.theme.PrimaryTextColor
import com.tutorly.ui.theme.TutorlyCardDefaults
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun StudentDetailsScreen(
    onBack: () -> Unit,
    onEdit: (Long, StudentEditTarget) -> Unit,
    onAddStudentFromCreation: () -> Unit = {},
    modifier: Modifier = Modifier,
    vm: StudentDetailsViewModel = hiltViewModel(),
    creationViewModel: LessonCreationViewModel,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val state by vm.uiState.collectAsState()
    val lessonCardViewModel: LessonCardViewModel = hiltViewModel()
    val lessonCardState by lessonCardViewModel.uiState.collectAsState()
    val creationState by creationViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showPrepaymentDialog by rememberSaveable { mutableStateOf(false) }
    var isArchiving by remember { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    LessonCardSheet(
        state = lessonCardState,
        onDismissRequest = lessonCardViewModel::dismiss,
        onStudentSelect = lessonCardViewModel::onStudentSelected,
        onAddStudent = {
            lessonCardViewModel.dismiss()
            onAddStudentFromCreation()
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

    LessonCreationSheet(
        state = creationState,
        onDismiss = { creationViewModel.dismiss() },
        onStudentQueryChange = creationViewModel::onStudentQueryChange,
        onStudentSelect = creationViewModel::onStudentSelected,
        onAddStudent = {
            creationViewModel.prepareForStudentCreation()
            creationViewModel.dismiss()
            onAddStudentFromCreation()
        },
        onSubjectInputChange = creationViewModel::onSubjectInputChanged,
        onSubjectSelect = creationViewModel::onSubjectSelected,
        onDateSelect = creationViewModel::onDateSelected,
        onTimeSelect = creationViewModel::onTimeSelected,
        onDurationChange = creationViewModel::onDurationChanged,
        onPriceChange = creationViewModel::onPriceChanged,
        onNoteChange = creationViewModel::onNoteChanged,
        onSubmit = creationViewModel::submit,
        onConfirmConflict = creationViewModel::confirmConflict,
        onDismissConflict = creationViewModel::dismissConflict
    )

    LaunchedEffect(creationState.snackbarMessage) {
        val message = creationState.snackbarMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            creationViewModel.consumeSnackbar()
        }
    }

    if (showPrepaymentDialog) {
        StudentPrepaymentDialog(
            onDismiss = { showPrepaymentDialog = false },
            onSaved = { result ->
                showPrepaymentDialog = false
                val amountText = formatMoneyInput(result.depositedCents)
                val message = if (result.debtCoveredCents > 0) {
                    val debtText = formatMoneyInput(result.debtCoveredCents)
                    context.getString(R.string.student_prepayment_success_with_debt, amountText, debtText)
                } else {
                    context.getString(R.string.student_prepayment_success, amountText)
                }
                coroutineScope.launch { snackbarHostState.showSnackbar(message) }
            }
        )
    }

    LaunchedEffect(showDeleteDialog) {
        if (!showDeleteDialog) {
            isDeleting = false
        }
    }

    val contentState = state as? StudentProfileUiState.Content

    val title = contentState?.profile?.student?.name
        ?: stringResource(id = R.string.student_details_title_placeholder)

    val openLessonCreation: (Long) -> Unit = { id ->
        creationViewModel.start(
            LessonCreationConfig(
                studentId = id,
                zoneId = ZonedDateTime.now().zone,
                origin = LessonCreationOrigin.STUDENT
            )
        )
    }

    Scaffold(
        topBar = {
            StudentProfileTopBar(
                title = title,
                isArchived = contentState?.profile?.student?.isArchived,
                onBack = onBack,
                onArchiveClick = contentState?.let {
                    {
                        if (!isArchiving) {
                            isArchiving = true
                            vm.toggleArchive(
                                onComplete = { isArchiving = false },
                                onError = { throwable ->
                                    val message = throwable.message?.takeIf { it.isNotBlank() }
                                        ?: context.getString(R.string.student_details_archive_error)
                                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                                }
                            )
                        }
                    }
                },
                onDeleteClick = contentState?.let { { showDeleteDialog = true } },
                archiveEnabled = !isArchiving,
                deleteEnabled = !isDeleting
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            contentState?.let { current ->
                val profile = current.profile
                FloatingActionButton(
                    onClick = { openLessonCreation(profile.student.id) },
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Outlined.CalendarToday, contentDescription = null)
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        when (val currentState = state) {
            StudentProfileUiState.Hidden, StudentProfileUiState.Loading -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            StudentProfileUiState.Error -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.student_profile_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is StudentProfileUiState.Content -> {
                StudentProfileContent(
                    profile = currentState.profile,
                    onEdit = onEdit,
                    onAddLesson = openLessonCreation,
                    onPrepaymentClick = { showPrepaymentDialog = true },
                    onLessonClick = lessonCardViewModel::open,
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
    }
}

    if (showDeleteDialog && contentState != null) {
        val studentName = contentState.profile.student.name
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) {
                    showDeleteDialog = false
                }
            },
            title = { Text(text = stringResource(id = R.string.student_details_delete_title)) },
            text = {
                Text(text = stringResource(id = R.string.student_details_delete_message, studentName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isDeleting) {
                            isDeleting = true
                            vm.deleteStudent(
                                onSuccess = {
                                    isDeleting = false
                                    showDeleteDialog = false
                                    onBack()
                                },
                                onError = { throwable ->
                                    isDeleting = false
                                    val message = throwable.message?.takeIf { it.isNotBlank() }
                                        ?: context.getString(R.string.student_details_delete_error)
                                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                                }
                            )
                        }
                    },
                    enabled = !isDeleting
                ) {
                    Text(text = stringResource(id = R.string.student_details_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeleting
                ) {
                    Text(text = stringResource(id = R.string.student_details_delete_cancel))
                }
            }
        )
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentProfileTopBar(
    title: String,
    isArchived: Boolean?,
    onBack: () -> Unit,
    onArchiveClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    archiveEnabled: Boolean = true,
    deleteEnabled: Boolean = true,
) {
    GradientTopBarContainer {
        TopAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            title = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = PrimaryTextColor
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
                if (onArchiveClick != null && isArchived != null) {
                    IconButton(onClick = onArchiveClick, enabled = archiveEnabled) {
                        val (icon, description) = if (isArchived) {
                            Icons.Outlined.Unarchive to stringResource(id = R.string.student_details_unarchive)
                        } else {
                            Icons.Outlined.Archive to stringResource(id = R.string.student_details_archive)
                        }
                        Icon(imageVector = icon, contentDescription = description)
                    }
                }
                if (onDeleteClick != null) {
                    IconButton(onClick = onDeleteClick, enabled = deleteEnabled) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(id = R.string.student_details_delete)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = PrimaryTextColor,
                navigationIconContentColor = PrimaryTextColor,
                actionIconContentColor = PrimaryTextColor
            ),
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}

@Composable
private fun StudentProfileContent(
    profile: StudentProfile,
    onEdit: (Long, StudentEditTarget) -> Unit,
    onAddLesson: (Long) -> Unit,
    onPrepaymentClick: (Long) -> Unit,
    onLessonClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val locale = remember { Locale("ru", "RU") }
    val numberFormatter = remember(locale) {
        NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    }
    val currencyFormatter = remember(locale) {
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance("RUB")
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    }
    val zoneId = remember { ZoneId.systemDefault() }
    val referenceTime = remember(profile) { Instant.now() }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM yyyy", locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("LLLL yyyy", locale) }

    val groupedLessons = remember(profile.recentLessons, zoneId) {
        val sorted = profile.recentLessons.sortedByDescending { it.startAt }
        val groups = linkedMapOf<YearMonth, MutableList<StudentProfileLesson>>()
        sorted.forEach { lesson ->
            val key = YearMonth.from(lesson.startAt.atZone(zoneId))
            groups.getOrPut(key) { mutableListOf() }.add(lesson)
        }
        groups.map { it.key to it.value.toList() }
    }

    val listState = rememberLazyListState()

    val sharedElementKey = "student-card-${profile.student.id}"

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            StudentProfileHeader(
                profile = profile,
                onEdit = { target -> onEdit(profile.student.id, target) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                sharedContentKey = sharedElementKey
            )
        }

        item {
            StudentProfileMetricsSection(
                profile = profile,
                numberFormatter = numberFormatter,
                onRateClick = { onEdit(profile.student.id, StudentEditTarget.RATE) },
                onPrepaymentClick = { onPrepaymentClick(profile.student.id) }
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ProfileContactsCard(
                    phone = profile.student.phone,
                    messenger = profile.student.messenger,
                    onPhoneClick = { onEdit(profile.student.id, StudentEditTarget.PHONE) },
                    onMessengerClick = { onEdit(profile.student.id, StudentEditTarget.MESSENGER) }
                )
                ProfileInfoCard(
                    icon = Icons.Outlined.StickyNote2,
                    label = stringResource(id = R.string.student_details_notes_title),
                    value = profile.student.note,
                    onClick = { onEdit(profile.student.id, StudentEditTarget.NOTES) },
                    valueMaxLines = 4
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(id = R.string.student_details_history_title),
                    style = MaterialTheme.typography.titleMedium
                )

                if (groupedLessons.isEmpty()) {
                    StudentProfileEmptyHistory(
                        onAddLesson = { onAddLesson(profile.student.id) }
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        groupedLessons.forEach { (month, lessons) ->
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = monthFormatter.format(month),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                lessons.forEach { lesson ->
                                    StudentProfileLessonCard(
                                        lesson = lesson,
                                        fallbackSubject = profile.subject,
                                        currencyFormatter = currencyFormatter,
                                        zoneId = zoneId,
                                        dateFormatter = dateFormatter,
                                        timeFormatter = timeFormatter,
                                        referenceTime = referenceTime,
                                        onClick = { onLessonClick(lesson.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun StudentProfileHeader(
    profile: StudentProfile,
    onEdit: (StudentEditTarget) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedContentKey: String? = null,
) {
    val sharedModifier = if (
        sharedTransitionScope != null &&
        animatedVisibilityScope != null &&
        sharedContentKey != null
    ) {
        with(sharedTransitionScope) {
            val sharedState = rememberSharedContentState(key = sharedContentKey)
            Modifier.sharedBounds(
                sharedContentState = sharedState,
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> tween(durationMillis = 450, easing = FastOutSlowInEasing) }
            )
        }
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(sharedModifier)
            .clickable { onEdit(StudentEditTarget.PROFILE) },
        shape = MaterialTheme.shapes.extraLarge,
        colors = TutorlyCardDefaults.colors(),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StudentAvatar(name = profile.student.name, size = 64.dp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = profile.student.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subject = profile.subject?.takeIf { it.isNotBlank() }?.trim()
                val grade = profile.grade?.takeIf { it.isNotBlank() }?.trim()
                val details = listOfNotNull(grade, subject).joinToString(separator = " • ")
                if (details.isNotEmpty()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(
    icon: ImageVector,
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    valueMaxLines: Int = 2,
) {
    val hasValue = !value.isNullOrBlank()
    val displayValue = value?.takeIf { it.isNotBlank() }
        ?: stringResource(id = R.string.student_profile_contact_placeholder)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = TutorlyCardDefaults.colors(),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasValue) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = valueMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StudentProfileMetricsSection(
    profile: StudentProfile,
    numberFormatter: NumberFormat,
    onRateClick: () -> Unit,
    onPrepaymentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lessonsCount = profile.metrics.totalLessons.toString()
    val baseRateCents = profile.student.rateCents?.takeIf { it > 0 }
    val recentRateCents = profile.rate?.let { rate ->
        if (rate.durationMinutes > 0) {
            ((rate.priceCents.toDouble() * 60) / rate.durationMinutes).roundToInt()
        } else {
            null
        }
    }
    val rateCents = baseRateCents ?: recentRateCents
    val rateValue = rateCents?.let { cents ->
        numberFormatter.format(cents / 100.0)
    } ?: stringResource(id = R.string.students_rate_placeholder)
    val earnedValue = numberFormatter.format(profile.metrics.totalPaidCents / 100.0)
    val prepaymentValue = numberFormatter.format(profile.metrics.prepaymentCents / 100.0)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileMetricTile(
                    icon = Icons.Outlined.CalendarToday,
                    value = lessonsCount,
                    label = stringResource(id = R.string.student_profile_metrics_lessons_label)
                )
                ProfileMetricTile(
                    icon = Icons.Outlined.Schedule,
                    value = rateValue,
                    label = stringResource(id = R.string.student_profile_metrics_rate_label),
                    onClick = onRateClick
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileMetricTile(
                    icon = Icons.Outlined.CreditCard,
                    value = earnedValue,
                    label = stringResource(id = R.string.student_profile_metrics_earned_label)
                )
                ProfileMetricTile(
                    icon = Icons.Outlined.Savings,
                    value = prepaymentValue,
                    label = stringResource(id = R.string.student_profile_metrics_prepayment_label),
                    onClick = onPrepaymentClick
                )
            }
        }
    }
}

@Composable
private fun ProfileMetricTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = modifier.fillMaxWidth()
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = MaterialTheme.shapes.large,
            colors = TutorlyCardDefaults.colors(),
            elevation = TutorlyCardDefaults.elevation()
        ) { content() }
    } else {
        Card(
            modifier = cardModifier,
            shape = MaterialTheme.shapes.large,
            colors = TutorlyCardDefaults.colors(),
            elevation = TutorlyCardDefaults.elevation()
        ) { content() }
    }
}

@Composable
private fun ProfileContactsCard(
    phone: String?,
    messenger: String?,
    onPhoneClick: () -> Unit,
    onMessengerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = TutorlyCardDefaults.colors(),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            ProfileContactRow(
                icon = Icons.Outlined.Phone,
                label = stringResource(id = R.string.student_details_phone_label),
                value = phone,
                placeholder = stringResource(id = R.string.student_profile_contact_placeholder),
                onClick = onPhoneClick
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ProfileContactRow(
                icon = Icons.Outlined.Email,
                label = stringResource(id = R.string.student_details_messenger_label),
                value = messenger,
                placeholder = stringResource(id = R.string.student_profile_contact_placeholder),
                onClick = onMessengerClick,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun ProfileContactRow(
    icon: ImageVector,
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasValue = !value.isNullOrBlank()
    val displayValue = value?.takeIf { it.isNotBlank() } ?: placeholder
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasValue) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StudentProfileEmptyHistory(
    onAddLesson: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = TutorlyCardDefaults.colors(),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.student_details_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(onClick = onAddLesson) {
                Text(text = stringResource(id = R.string.student_details_create_lesson))
            }
        }
    }
}

@Composable
private fun StudentProfileLessonCard(
    lesson: StudentProfileLesson,
    fallbackSubject: String?,
    currencyFormatter: NumberFormat,
    zoneId: ZoneId,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    referenceTime: Instant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val start = remember(lesson.startAt, zoneId) { lesson.startAt.atZone(zoneId) }
    val end = remember(lesson.endAt, zoneId) { lesson.endAt.atZone(zoneId) }
    val dateText = remember(start) { dateFormatter.format(start) }
    val timeText = stringResource(
        id = R.string.student_details_history_time_range,
        timeFormatter.format(start),
        timeFormatter.format(end),
        lesson.durationMinutes
    )
    val fallbackSubjectText = fallbackSubject?.takeIf { it.isNotBlank() }?.trim()
    val title = lesson.title?.takeIf { it.isNotBlank() }?.trim()
        ?: lesson.subjectName?.takeIf { it.isNotBlank() }?.trim()
        ?: fallbackSubjectText
        ?: stringResource(id = R.string.lesson_card_subject_placeholder)
    val amount = currencyFormatter.format(lesson.priceCents / 100.0)
    val isPaid = lesson.paymentStatus == PaymentStatus.PAID
    val isUpcoming = lesson.startAt.isAfter(referenceTime)
    val badgeStatus = when {
        !isPaid -> PaymentBadgeStatus.DEBT
        isUpcoming -> PaymentBadgeStatus.PREPAID
        else -> PaymentBadgeStatus.PAID
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = TutorlyCardDefaults.colors(),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleMedium
                )
                PaymentBadge(status = badgeStatus)
            }
        }
    }
}

@Composable
private fun StudentAvatar(
    name: String,
    size: androidx.compose.ui.unit.Dp = 48.dp,
) {
    val initials = remember(name) {
        name
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString(separator = "") { it.first().uppercaseChar().toString() }
            .ifEmpty { "?" }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
