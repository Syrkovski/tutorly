package com.tutorly.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.ui.CalendarEvent
import com.tutorly.domain.model.LessonsRangeStats
import com.tutorly.domain.model.PaymentStatusIcon
import com.tutorly.models.PaymentStatus
import com.tutorly.ui.components.LessonBrief
import com.tutorly.ui.components.WeekMosaic
import com.tutorly.ui.components.WeeklyStats
import com.tutorly.ui.theme.NowRed
import com.tutorly.ui.lessoncreation.LessonCreationConfig
import com.tutorly.ui.lessoncreation.LessonCreationOrigin
import com.tutorly.ui.lessoncreation.LessonCreationSheet
import com.tutorly.ui.lessoncreation.LessonCreationViewModel
import com.tutorly.ui.lessoncard.LessonCardExitAction
import com.tutorly.ui.lessoncard.LessonCardSheet
import com.tutorly.ui.lessoncard.LessonCardViewModel
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class CalendarMode { DAY, WEEK, MONTH }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    onLessonDetails: (Long, Long, ZonedDateTime) -> Unit = { _, _, _ -> },
    onAddStudent: () -> Unit = {},
    creationViewModel: LessonCreationViewModel,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val creationState by creationViewModel.uiState.collectAsState()
    val lessonCardViewModel: LessonCardViewModel = hiltViewModel()
    val lessonCardState by lessonCardViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var direction by remember { mutableStateOf(0) } // -1 назад, +1 вперёд
    val anchor = uiState.anchor
    val mode = uiState.mode
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
                onLessonDetails(details.id, details.studentId, details.startAt.atZone(zoneId))
                lessonCardViewModel.consumeExitAction()
            }
            LessonCardExitAction.Close -> {
                lessonCardViewModel.consumeExitAction()
            }
            null -> Unit
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CalendarEvent.CreateLesson -> creationViewModel.start(
                    LessonCreationConfig(
                        start = event.start,
                        duration = event.duration,
                        studentId = event.studentId,
                        zoneId = event.start.zone,
                        origin = LessonCreationOrigin.CALENDAR
                    )
                )
                is CalendarEvent.OpenLesson -> lessonCardViewModel.open(event.lessonId)
            }
        }
    }

    LaunchedEffect(creationState.snackbarMessage) {
        creationState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            creationViewModel.consumeSnackbar()
        }
    }

    LessonCreationSheet(
        state = creationState,
        onDismiss = { creationViewModel.dismiss() },
        onStudentQueryChange = creationViewModel::onStudentQueryChange,
        onStudentSelect = creationViewModel::onStudentSelected,
        onAddStudent = {
            creationViewModel.prepareForStudentCreation()
            creationViewModel.dismiss()
            onAddStudent()
        },
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

    val prevPeriod = {
        direction = -1
        viewModel.goToPreviousPeriod()
    }
    val nextPeriod = {
        direction = +1
        viewModel.goToNextPeriod()
    }

    val swipeModifier = Modifier.pointerInput(mode) {
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
                    if (totalDrag < 0) nextPeriod() else prevPeriod()
                    handled = true
                    change.consume()
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val start = uiState.currentDateTime
                creationViewModel.start(
                    LessonCreationConfig(
                        start = start,
                        zoneId = uiState.zoneId,
                        origin = LessonCreationOrigin.CALENDAR
                    )
                )
            }) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
        // Хедер: тут же свайп (чтобы не конфликтовал со скроллом списка)
        PlanScreenHeader(
            anchor = anchor,
            mode = mode,
            onModeChange = {
                direction = 0
                viewModel.setMode(it)
            },
            onPrevPeriod = prevPeriod,
            onNextPeriod = nextPeriod,
            onAddClick = {
                val start = uiState.currentDateTime
                creationViewModel.start(
                    LessonCreationConfig(
                        start = start,
                        zoneId = uiState.zoneId,
                        origin = LessonCreationOrigin.CALENDAR
                    )
                )
            },
            onSelectDate = { selected ->
                direction = when {
                    selected.isAfter(anchor) -> 1
                    selected.isBefore(anchor) -> -1
                    else -> 0
                }
                viewModel.selectDate(selected)
            },
            onSwipeLeft = nextPeriod,
            onSwipeRight = prevPeriod
        )

        // Контент занимает остаток экрана и скроллится внутри
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .then(swipeModifier)   // 👈 свайп теперь работает по всему экрану
        ) {
            AnimatedContent(
                targetState = anchor,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    if (direction > 0) {
                        // вперёд (влево)
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(durationMillis = 250)
                        ) + fadeIn(animationSpec = tween(250))) togetherWith
                                (slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -fullWidth / 2 },
                                    animationSpec = tween(durationMillis = 250)
                                ) + fadeOut(animationSpec = tween(250)))
                    } else {
                        // назад (вправо)
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(durationMillis = 250)
                        ) + fadeIn(animationSpec = tween(250))) togetherWith
                                (slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth / 2 },
                                    animationSpec = tween(durationMillis = 250)
                                ) + fadeOut(animationSpec = tween(250)))
                    }
                }
                ,
                label = "day-switch"
            ) { currentDate ->
                val lessonsForCurrent = remember(currentDate, uiState.lessonsByDate) {
                    uiState.lessonsByDate[currentDate].orEmpty()
                }
                when (mode) {
                    CalendarMode.DAY -> DayTimeline(
                        date = currentDate,
                        lessons = lessonsForCurrent,
                        currentDateTime = uiState.currentDateTime,
                        onLessonClick = { lesson ->
                            lessonCardViewModel.open(lesson.id)
                        },
                        onEmptySlot = { startTime ->
                            viewModel.onEmptySlotSelected(currentDate, startTime, DefaultSlotDuration)
                        }
                    )
                    CalendarMode.WEEK -> WeekMosaic(
                        anchor = currentDate,
                        onOpenDay = { selected ->
                            direction = when {
                                selected.isAfter(anchor) -> 1
                                selected.isBefore(anchor) -> -1
                                else -> 0
                            }
                            viewModel.setMode(CalendarMode.DAY)
                            viewModel.selectDate(selected)
                        },
                        dayDataProvider = { date ->
                            uiState.lessonsByDate[date].orEmpty().map { it.toLessonBrief() }
                        },
                        stats = uiState.stats.toWeeklyStats(),
                        currentDateTime = uiState.currentDateTime,
                        onLessonClick = { brief -> lessonCardViewModel.open(brief.id) }
                    )
                    CalendarMode.MONTH -> MonthPlaceholder(currentDate)
                }
            }
        }
    }
    }
}

private fun CalendarLesson.toLessonBrief(): LessonBrief {
    return LessonBrief(
        id = id,
        time = start.format(timeFormatter),
        end = end.format(timeFormatter),
        student = studentName,
        priceCents = priceCents.toLong(),
        paid = paymentStatus == PaymentStatus.PAID
    )
}

private fun LessonsRangeStats.toWeeklyStats(): WeeklyStats = WeeklyStats(
    totalLessons = totalLessons,
    paidCount = paidLessons,
    debtCount = debtLessons,
    earnedCents = earnedCents
)

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")


/* ----------------------------- HEADER ----------------------------------- */

@Composable
fun PlanScreenHeader(
    anchor: LocalDate,
    mode: CalendarMode,
    onModeChange: (CalendarMode) -> Unit,
    onPrevPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onAddClick: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            // свайп только на хедере — не мешает вертикальному скроллу списка
            .pointerInput(mode) {
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
                            if (totalDrag < 0) onSwipeLeft() else onSwipeRight()
                            handled = true
                            change.consume()
                        }
                    }
                )
            }
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = anchor.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru")))
                    .replaceFirstChar { it.titlecase(Locale("ru")) },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Добавить занятие")
            }
        }
        TabRow(
            selectedTabIndex = mode.ordinal,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            listOf("День", "Неделя", "Месяц").forEachIndexed { i, label ->
                Tab(
                    selected = i == mode.ordinal,
                    onClick = { onModeChange(CalendarMode.values()[i]) },
                    text = { Text(label) }
                )
            }
        }
        if (mode == CalendarMode.DAY) {
            DayWeekStrip(
                anchor = anchor,
                onSelect = onSelectDate,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/* --------------------------- DAY TIMELINE -------------------------------- */

private val GridColor = Color(0xFFE9F0FF)
private val SpineColor = Color(0xFF2D7FF9).copy(alpha = 0.6f)
private val LabelWidth = 64.dp
private val HourHeight = 64.dp
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DefaultSlotDuration: Duration = Duration.ofMinutes(60)
private const val MinutesPerHour: Int = 60
private const val LAST_TIMELINE_MINUTE: Int = 23 * MinutesPerHour + 30
private const val SlotIncrementMinutes: Int = 30

@Composable
private fun DayTimeline(
    date: LocalDate,
    lessons: List<CalendarLesson>,
    currentDateTime: ZonedDateTime,
    onLessonClick: (CalendarLesson) -> Unit,
    onEmptySlot: (LocalTime) -> Unit
) {
    val dayLessons = remember(date, lessons) { lessons }
    val isToday = remember(date, currentDateTime) { currentDateTime.toLocalDate() == date }
    val (startHour, endHourExclusive) = remember(dayLessons) {
        computeTimelineBounds(dayLessons)
    }
    val hours = remember(startHour, endHourExclusive) {
        (startHour until endHourExclusive).map { "%02d:00".format(it) }
    }
    val totalHeight: Dp = HourHeight * hours.size
    val totalMinutes = remember(startHour, endHourExclusive) {
        (endHourExclusive - startHour) * MinutesPerHour
    }

    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val hourHeightPx = remember(density) { with(density) { HourHeight.toPx() } }
    val labelWidthPx = remember(density) { with(density) { LabelWidth.toPx() } }
    val cardInsetPx = remember(density) { with(density) { 8.dp.toPx() } }
    val totalHeightPx = remember(totalHeight, density) { with(density) { totalHeight.toPx() } }
    val minuteHeight = remember { HourHeight / MinutesPerHour }
    val nowMinutesFromStart = remember(isToday, currentDateTime, startHour) {
        if (!isToday) null else {
            val currentMinutes = currentDateTime.hour * MinutesPerHour + currentDateTime.minute
            currentMinutes - startHour * MinutesPerHour
        }
    }
    val nowBadgeOffset = remember(nowMinutesFromStart, totalMinutes) {
        nowMinutesFromStart?.takeIf { it in 0..totalMinutes }?.let { minutes ->
            minuteHeight * minutes.toFloat()
        }
    }

    val lessonRegions = remember(dayLessons, startHour, hourHeightPx, labelWidthPx, cardInsetPx) {
        val baseMin = startHour * MinutesPerHour
        dayLessons.map { lesson ->
            val startTime = lesson.start.toLocalTime()
            val startMin = startTime.hour * MinutesPerHour + startTime.minute
            val durationMinutes = lesson.duration.toMinutes().coerceAtLeast(SlotIncrementMinutes.toLong())
            val topPx = ((startMin - baseMin).coerceAtLeast(0)) * hourHeightPx / MinutesPerHour
            val heightPx = durationMinutes.toFloat() * hourHeightPx / MinutesPerHour
            TimelineLessonRegion(
                topPx = topPx,
                bottomPx = topPx + heightPx,
                leftPx = labelWidthPx + cardInsetPx
            )
        }
    }

    // ВЕСЬ день = одна большая "простыня" высотой totalHeight; она вертикально скроллится
    Box(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
    ) {
        // Внутренний контейнер фиксированной высоты = весь день
        Box(
            Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .padding(horizontal = 8.dp)
                .pointerInput(dayLessons, startHour, endHourExclusive, lessonRegions) {
                    detectTapGestures { offset ->
                        if (offset.x < labelWidthPx) return@detectTapGestures
                        if (lessonRegions.any { region -> offset.x >= region.leftPx && offset.y in region.topPx..region.bottomPx }) {
                            return@detectTapGestures
                        }

                        val clampedY = offset.y.coerceIn(0f, totalHeightPx)
                        val minutesWithin = (clampedY / hourHeightPx) * MinutesPerHour
                        val candidate = (startHour * MinutesPerHour) + minutesWithin.roundToInt()
                        val normalized = candidate.coerceIn(0, LAST_TIMELINE_MINUTE)
                        val rounded = (normalized / SlotIncrementMinutes) * SlotIncrementMinutes
                        val hour = rounded / MinutesPerHour
                        val minute = rounded % MinutesPerHour
                        onEmptySlot(LocalTime.of(hour, minute))
                    }
                }
        ) {
            // 1) Сетка фоном
            Canvas(Modifier.matchParentSize()) {
                val rowH = HourHeight.toPx()
                val leftPad = LabelWidth.toPx()
                val spineW = 2.dp.toPx()

                repeat(hours.size + 1) { i ->
                    val y = i * rowH
                    drawLine(
                        color = GridColor,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                drawRect(
                    color = SpineColor,
                    topLeft = androidx.compose.ui.geometry.Offset(leftPad, 0f),
                    size = androidx.compose.ui.geometry.Size(spineW, size.height)
                )
                nowMinutesFromStart?.takeIf { it in 0..totalMinutes }?.let { minutes ->
                    val y = minutes * rowH / MinutesPerHour
                    drawLine(
                        color = NowRed,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // 2) Уроки — точное позиционирование по времени, до правого края
            dayLessons.forEach { lesson ->
                LessonBlock(
                    lesson = lesson,
                    baseHour = startHour,
                    hourHeight = HourHeight,
                    onLessonClick = onLessonClick
                )
            }

            // 3) Метки времени слева
            Column(
                Modifier
                    .fillMaxHeight()
                    .width(LabelWidth)
            ) {
                hours.forEach {
                    Box(
                        Modifier
                            .height(HourHeight),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(it, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            nowBadgeOffset?.let { offset ->
                val centered = offset - 12.dp
                val badgeOffset = if (centered < 0.dp) 0.dp else centered
                Box(
                    Modifier
                        .fillMaxWidth()
                        .offset(y = badgeOffset)
                ) {
                    NowBadge(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = LabelWidth + 16.dp)
                    )
                }
            }
        }
    }
}

private data class TimelineLessonRegion(
    val topPx: Float,
    val bottomPx: Float,
    val leftPx: Float
)

@Composable
private fun LessonBlock(
    lesson: CalendarLesson,
    baseHour: Int,
    hourHeight: Dp,
    onLessonClick: (CalendarLesson) -> Unit
) {
    val startTime = lesson.start.toLocalTime()
    val endTime = lesson.end.toLocalTime()
    val startMin = startTime.hour * 60 + startTime.minute
    val baseMin = baseHour * 60
    val durationMinutes = lesson.duration.toMinutes().coerceAtLeast(SlotIncrementMinutes.toLong())

    // Переводим минуты в dp (1 мин = hourHeight/60)
    val minuteDp = hourHeight / 60f
    val top = minuteDp * (startMin - baseMin)
    val height = minuteDp * durationMinutes.toInt()

    val timeRange = buildString {
        append(TimeFormatter.format(startTime))
        append("–")
        append(TimeFormatter.format(endTime))
    }
    val subtitle = lesson.subtitleText()
    val detailLine = listOfNotNull(timeRange, subtitle).joinToString(" • ")
    val statusText = lesson.paymentStatusLabel()
    val statusColor = lesson.paymentStatusColor()
    val statusIcon = lesson.paymentStatusIconVector()
    val containerColor = lesson.subjectColorArgb?.let { Color(it).copy(alpha = 0.12f) }
        ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    Box(
        Modifier
            .fillMaxWidth()
            .offset(y = top)
            .height(height)
            .padding(start = LabelWidth + 8.dp, end = 8.dp) // от оси до правого края
    ) {
        Card(
            onClick = { onLessonClick(lesson) },
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(lesson.studentName, style = MaterialTheme.typography.titleMedium)
                if (detailLine.isNotBlank()) {
                    Text(
                        detailLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current.copy(alpha = 0.7f)
                    )
                }
                if (statusText != null) {
                    StatusBadge(text = statusText, color = statusColor, icon = statusIcon)
                }
            }
        }
    }
}

private fun CalendarLesson.subtitleText(): String? {
    val parts = listOfNotNull(
        subjectName?.takeIf { it.isNotBlank() }?.trim(),
        lessonNote?.takeIf { it.isNotBlank() },
        studentNote?.takeIf { it.isNotBlank() },
        lessonTitle?.takeIf { it.isNotBlank() }
    ).distinct()
    return if (parts.isEmpty()) null else parts.joinToString(" • ")
}

@Composable
private fun CalendarLesson.paymentStatusLabel(): String? = when (paymentStatus) {
    PaymentStatus.PAID -> stringResource(R.string.lesson_status_paid)
    PaymentStatus.DUE -> stringResource(R.string.lesson_status_due)
    PaymentStatus.UNPAID -> stringResource(R.string.lesson_status_unpaid)
    PaymentStatus.CANCELLED -> stringResource(R.string.lesson_status_cancelled)
}

@Composable
private fun CalendarLesson.paymentStatusColor(): Color = when (paymentStatus) {
    PaymentStatus.PAID -> MaterialTheme.colorScheme.tertiary
    PaymentStatus.DUE, PaymentStatus.UNPAID -> MaterialTheme.colorScheme.error
    PaymentStatus.CANCELLED -> MaterialTheme.colorScheme.outline
}

@Composable
private fun CalendarLesson.paymentStatusIconVector(): ImageVector? = when (paymentStatusIcon) {
    PaymentStatusIcon.PAID -> Icons.Filled.Check
    PaymentStatusIcon.OUTSTANDING -> Icons.Filled.Warning
    PaymentStatusIcon.CANCELLED -> Icons.Filled.Close
}

@Composable
private fun StatusBadge(text: String, color: Color, icon: ImageVector?) {
    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun NowBadge(modifier: Modifier = Modifier) {
    Surface(
        color = NowRed,
        contentColor = Color.White,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.calendar_now_badge),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun computeTimelineBounds(lessons: List<CalendarLesson>): Pair<Int, Int> {
    val defaultStart = 9
    val defaultEnd = 21
    if (lessons.isEmpty()) return defaultStart to defaultEnd

    val earliestMinutes = lessons.minOf { it.start.hour * 60 + it.start.minute }
    val latestMinutes = lessons.maxOf { it.end.hour * 60 + it.end.minute }
    val startHour = min(defaultStart, earliestMinutes / 60)
    val endHourExclusive = max(defaultEnd, ((latestMinutes + 59) / 60))
    return startHour to max(endHourExclusive, startHour + 1)
}

/* ---------------------- WEEK / MONTH PLACEHOLDERS ------------------------ */

@Composable private fun WeekPlaceholder(anchor: LocalDate) {
    Box(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Неделя ${weekRange(anchor)} (заглушка)", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun MonthPlaceholder(anchor: LocalDate) {
    Box(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("${anchor.monthValue}.${anchor.year} (месяц — заглушка)", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun weekRange(d: LocalDate): String {
    val start = d.with(DayOfWeek.MONDAY)
    val end = start.plusDays(6)
    return "${start.dayOfMonth}.${start.monthValue} — ${end.dayOfMonth}.${end.monthValue}"
}

/* --------------------------- DAY/WEEK STRIP ------------------------------ */

@Composable
fun DayWeekStrip(
    anchor: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val monday = anchor.with(DayOfWeek.MONDAY)
    val days = remember(monday) { (0..6).map { monday.plusDays(it.toLong()) } }

    Row(modifier.padding(top = 8.dp)) {
        days.forEachIndexed { idx, d ->
            val selected = d == anchor
            DayTwoLineChip(
                date = d,
                selected = selected,
                onClick = { onSelect(d) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .then(if (idx < days.lastIndex) Modifier.padding(end = 8.dp) else Modifier)
            )
        }
    }
}

@Composable
private fun DayTwoLineChip(
    date: LocalDate,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Color(0x1A2D7FF9) else Color(0xFFF2F3F7)
    val fg = if (selected) Color(0xFF2D7FF9) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    Surface(
        color = bg,
        shape = MaterialTheme.shapes.medium,
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ru"))
                    .replaceFirstChar { it.titlecase(Locale("ru")) },
                style = MaterialTheme.typography.labelSmall,
                color = fg
            )
            Text(
                "${date.dayOfMonth}",
                style = MaterialTheme.typography.labelLarge,
                color = fg
            )
        }
    }
}
//    LessonCardSheet(
//        state = lessonCardState,
//        zoneId = uiState.zoneId,
//        onDismissRequest = lessonCardViewModel::requestDismiss,
//        onCancelDismiss = lessonCardViewModel::cancelDismiss,
//        onConfirmDismiss = lessonCardViewModel::confirmDismiss,
//        onNoteChange = lessonCardViewModel::onNoteChange,
//        onSaveNote = lessonCardViewModel::saveNote,
//        onMarkPaid = lessonCardViewModel::markPaid,
//        onRequestMarkDue = lessonCardViewModel::requestMarkDue,
//        onDismissMarkDue = lessonCardViewModel::dismissMarkDueDialog,
//        onConfirmMarkDue = lessonCardViewModel::confirmMarkDue,
//        onRequestEdit = lessonCardViewModel::requestEdit,
//        onSnackbarConsumed = lessonCardViewModel::consumeSnackbar
//    )
//
//    val pendingExit = lessonCardState.pendingExitAction
//    LaunchedEffect(pendingExit) {
//        when (pendingExit) {
//            is LessonCardExitAction.NavigateToEdit -> {
//                val details = pendingExit.details
//                onLessonDetails(
//                    details.id,
//                    details.studentId,
//                    details.startAt.atZone(uiState.zoneId)
//                )
//                lessonCardViewModel.consumeExitAction()
//            }
//            LessonCardExitAction.Close -> {
//                lessonCardViewModel.consumeExitAction()
//            }
//            null -> Unit
//        }
//    }
