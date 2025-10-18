package com.tutorly.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.ui.CalendarEvent
import com.tutorly.domain.model.PaymentStatusIcon
import com.tutorly.models.PaymentStatus
import com.tutorly.ui.components.AppTopBar
import com.tutorly.ui.components.LessonBrief
import com.tutorly.ui.components.StatusChip
import com.tutorly.ui.components.StatusChipData
import com.tutorly.ui.components.WeekMosaic
import com.tutorly.ui.components.statusChipData
import com.tutorly.ui.theme.extendedColors
import com.tutorly.ui.lessoncreation.LessonCreationConfig
import com.tutorly.ui.lessoncreation.LessonCreationOrigin
import com.tutorly.ui.lessoncreation.LessonCreationSheet
import com.tutorly.ui.lessoncreation.LessonCreationViewModel
import com.tutorly.ui.lessoncard.LessonCardSheet
import com.tutorly.ui.lessoncard.LessonCardViewModel
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

enum class CalendarMode { DAY, WEEK, MONTH }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    onAddStudent: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
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

    val calendarTitle = remember(anchor) {
        anchor.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru")))
            .replaceFirstChar { it.titlecase(Locale("ru")) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = calendarTitle,
                actions = {
                    IconButton(
                        onClick = onOpenSettings
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(id = R.string.calendar_open_settings)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val start = uiState.currentDateTime
                    creationViewModel.start(
                        LessonCreationConfig(
                            start = start,
                            zoneId = uiState.zoneId,
                            origin = LessonCreationOrigin.CALENDAR
                        )
                    )
                },
                containerColor = MaterialTheme.extendedColors.accent,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Outlined.DateRange,
                    contentDescription = stringResource(id = R.string.lesson_create_title)
                )
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        // Хедер: тут же свайп (чтобы не конфликтовал со скроллом списка)
        PlanScreenHeader(
            anchor = anchor,
            mode = mode,
            weekendDays = uiState.weekendDays,
            onModeChange = {
                direction = 0
                viewModel.setMode(it)
            },
            onPrevPeriod = prevPeriod,
            onNextPeriod = nextPeriod,
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
                        workDayStartMinutes = uiState.workDayStartMinutes,
                        workDayEndMinutes = uiState.workDayEndMinutes,
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
                        currentDateTime = uiState.currentDateTime,
                        onLessonClick = { brief -> lessonCardViewModel.open(brief.id) }
                    )
                    CalendarMode.MONTH -> MonthCalendar(
                        anchor = currentDate,
                        currentDateTime = uiState.currentDateTime,
                        weekendDays = uiState.weekendDays,
                        onDaySelected = { selected ->
                            direction = when {
                                selected.isAfter(anchor) -> 1
                                selected.isBefore(anchor) -> -1
                                else -> 0
                            }
                            viewModel.setMode(CalendarMode.DAY)
                            viewModel.selectDate(selected)
                        }
                    )
                }
            }
        }
    }
    }
}

private fun CalendarLesson.toLessonBrief(): LessonBrief {
    return LessonBrief(
        id = id,
        start = start,
        end = end,
        student = studentName,
        grade = studentGrade,
        subjectName = subjectName,
        subjectColorArgb = subjectColorArgb,
        paymentStatus = paymentStatus
    )
}


/* ----------------------------- HEADER ----------------------------------- */

@Composable
fun PlanScreenHeader(
    anchor: LocalDate,
    mode: CalendarMode,
    weekendDays: Set<DayOfWeek>,
    onModeChange: (CalendarMode) -> Unit,
    onPrevPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
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
                weekendDays = weekendDays,
                onSelect = onSelectDate,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/* --------------------------- DAY TIMELINE -------------------------------- */

private val LabelWidth = 64.dp
private val HourHeight = 80.dp
private val DefaultSlotDuration: Duration = Duration.ofMinutes(60)
private const val MinutesPerHour: Int = 60
private const val LAST_TIMELINE_MINUTE: Int = 23 * MinutesPerHour + 30
private const val SlotIncrementMinutes: Int = 30
private const val MAX_START_MINUTE: Int = LAST_TIMELINE_MINUTE - SlotIncrementMinutes
private const val MAX_END_MINUTE: Int = LAST_TIMELINE_MINUTE

@Composable
private fun DayTimeline(
    date: LocalDate,
    lessons: List<CalendarLesson>,
    currentDateTime: ZonedDateTime,
    workDayStartMinutes: Int,
    workDayEndMinutes: Int,
    onLessonClick: (CalendarLesson) -> Unit,
    onEmptySlot: (LocalTime) -> Unit
) {
    val dayLessons = remember(date, lessons) { lessons }
    val isToday = remember(date, currentDateTime) { currentDateTime.toLocalDate() == date }
    val startMinutes = remember(workDayStartMinutes) {
        workDayStartMinutes.coerceIn(0, MAX_START_MINUTE)
    }
    val endMinutes = remember(workDayEndMinutes, startMinutes) {
        workDayEndMinutes.coerceIn(startMinutes + SlotIncrementMinutes, MAX_END_MINUTE)
    }
    val totalMinutes = remember(startMinutes, endMinutes) {
        (endMinutes - startMinutes).coerceAtLeast(SlotIncrementMinutes)
    }
    val minuteHeight = remember { HourHeight / MinutesPerHour }
    val totalHeight = remember(totalMinutes) { minuteHeight * totalMinutes.toFloat() }

    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val minuteHeightPx = remember(density) { with(density) { minuteHeight.toPx() } }
    val labelWidthPx = remember(density) { with(density) { LabelWidth.toPx() } }
    val cardInsetPx = remember(density) { with(density) { 8.dp.toPx() } }
    val totalHeightPx = remember(totalHeight, density) { with(density) { totalHeight.toPx() } }
    val nowMinutesFromStart = remember(isToday, currentDateTime, startMinutes, totalMinutes) {
        if (!isToday) {
            null
        } else {
            val currentMinutes = currentDateTime.hour * MinutesPerHour + currentDateTime.minute
            val within = currentMinutes - startMinutes
            within.takeIf { it in 0..totalMinutes }
        }
    }
    val hourMarks = remember(startMinutes, endMinutes) {
        buildList {
            add(startMinutes)
            var next = ((startMinutes / MinutesPerHour) + 1) * MinutesPerHour
            while (next < endMinutes) {
                add(next)
                next += MinutesPerHour
            }
            add(endMinutes)
        }.distinct()
    }
    val labelMinutes = remember(startMinutes, endMinutes) {
        buildList {
            add(startMinutes)
            var next = ((startMinutes / MinutesPerHour) + 1) * MinutesPerHour
            while (next < endMinutes) {
                add(next)
                next += MinutesPerHour
            }
        }
    }
    val lessonRegions = remember(dayLessons, startMinutes, minuteHeightPx, labelWidthPx, cardInsetPx) {
        val baseMin = startMinutes
        dayLessons.map { lesson ->
            val startTime = lesson.start.toLocalTime()
            val lessonStart = startTime.hour * MinutesPerHour + startTime.minute
            val offsetMinutes = (lessonStart - baseMin).coerceAtLeast(0)
            val durationMinutes = lesson.duration.toMinutes().coerceAtLeast(SlotIncrementMinutes.toLong())
            val topPx = offsetMinutes * minuteHeightPx
            val heightPx = durationMinutes.toFloat() * minuteHeightPx
            TimelineLessonRegion(
                topPx = topPx,
                bottomPx = topPx + heightPx,
                leftPx = labelWidthPx + cardInsetPx
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFAFC))
            .verticalScroll(scroll)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .padding(horizontal = 8.dp)
                .pointerInput(dayLessons, startMinutes, endMinutes, lessonRegions) {
                    detectTapGestures { offset ->
                        if (offset.x < labelWidthPx) return@detectTapGestures
                        if (lessonRegions.any { region ->
                                offset.x >= region.leftPx && offset.y in region.topPx..region.bottomPx
                            }
                        ) {
                            return@detectTapGestures
                        }

                        val clampedY = offset.y.coerceIn(0f, totalHeightPx)
                        val minutesWithin = (clampedY / minuteHeightPx).roundToInt()
                        val candidate = startMinutes + minutesWithin
                        val maxSelectable = (endMinutes - SlotIncrementMinutes).coerceAtLeast(startMinutes)
                        val normalized = candidate.coerceIn(startMinutes, maxSelectable)
                        val slots = (normalized - startMinutes) / SlotIncrementMinutes
                        val rounded = startMinutes + slots * SlotIncrementMinutes
                        val hour = rounded / MinutesPerHour
                        val minute = rounded % MinutesPerHour
                        onEmptySlot(LocalTime.of(hour, minute))
                    }
                }
        ) {
            val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            val spineColor = MaterialTheme.colorScheme.primary
            val accent = MaterialTheme.extendedColors.accent
            Canvas(Modifier.matchParentSize()) {
                val leftPad = LabelWidth.toPx()
                val spineW = 2.dp.toPx()
                hourMarks.forEach { minute ->
                    val y = (minute - startMinutes) * minuteHeightPx
                    drawLine(
                        color = gridLineColor,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                drawRect(
                    color = spineColor,
                    topLeft = androidx.compose.ui.geometry.Offset(leftPad, 0f),
                    size = androidx.compose.ui.geometry.Size(spineW, size.height)
                )
                nowMinutesFromStart?.let { minutes ->
                    val y = minutes * minuteHeightPx
                    drawLine(
                        color = accent,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            dayLessons.forEach { lesson ->
                LessonBlock(
                    lesson = lesson,
                    baseMinutes = startMinutes,
                    minuteHeight = minuteHeight,
                    now = currentDateTime,
                    onLessonClick = onLessonClick
                )
            }

            Box(
                Modifier
                    .fillMaxHeight()
                    .width(LabelWidth)
            ) {
                labelMinutes.forEach { minute ->
                    val label = "%02d:%02d".format(minute / MinutesPerHour, minute % MinutesPerHour)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(y = minuteHeight * (minute - startMinutes).toFloat())
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
    baseMinutes: Int,
    minuteHeight: Dp,
    now: ZonedDateTime,
    onLessonClick: (CalendarLesson) -> Unit
) {
    val startTime = lesson.start.toLocalTime()
    val startMin = startTime.hour * MinutesPerHour + startTime.minute
    val offsetMinutes = (startMin - baseMinutes).coerceAtLeast(0)
    val durationMinutes = lesson.duration.toMinutes().coerceAtLeast(SlotIncrementMinutes.toLong())

    val top = minuteHeight * offsetMinutes.toFloat()
    val height = minuteHeight * durationMinutes.toInt().toFloat()

    val statusInfo = lesson.statusPresentation(now)
    val secondaryLine = remember(lesson.studentGrade, lesson.subjectName) {
        listOfNotNull(lesson.studentGrade, lesson.subjectName)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " • ")
    }

    Box(
        Modifier
            .fillMaxWidth()
            .offset(y = top)
            .height(height)
            .padding(start = LabelWidth + 8.dp, end = 8.dp)
    ) {
        Card(
            onClick = { onLessonClick(lesson) },
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp,
                focusedElevation = 2.dp,
                hoveredElevation = 2.dp,
                pressedElevation = 1.dp,
                draggedElevation = 3.dp,
                disabledElevation = 0.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = if (secondaryLine.isNullOrBlank()) {
                    Arrangement.Center
                } else {
                    Arrangement.spacedBy(4.dp)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lesson.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    StatusChip(
                        data = statusInfo
                    )
                }
                if (!secondaryLine.isNullOrBlank()) {
                    Text(
                        text = secondaryLine,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarLesson.statusPresentation(now: ZonedDateTime): StatusChipData =
    statusChipData(paymentStatus, start, end, now)

/* ----------------------------- MONTH GRID -------------------------------- */

@Composable
private fun MonthCalendar(
    anchor: LocalDate,
    currentDateTime: ZonedDateTime,
    weekendDays: Set<DayOfWeek>,
    onDaySelected: (LocalDate) -> Unit
) {
    val month = remember(anchor) { YearMonth.from(anchor) }
    val firstDay = remember(month) { month.atDay(1) }
    val firstVisibleDay = remember(firstDay) {
        firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val totalCells = remember { 6 * 7 }
    val days = remember(firstVisibleDay, totalCells) {
        generateSequence(firstVisibleDay) { it.plusDays(1) }
            .take(totalCells)
            .toList()
    }
    val today = remember(currentDateTime) { currentDateTime.toLocalDate() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val headerHeight = 36.dp
        val gridHeight = remember(maxHeight) { (maxHeight - headerHeight).coerceAtLeast(0.dp) }
        val cellHeight = remember(gridHeight) { if (gridHeight == 0.dp) 0.dp else gridHeight / 6 }
        val weekdays = remember {
            listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
            )
        }

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekdays.forEach { dayOfWeek ->
                    val label = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ru"))
                        .replaceFirstChar { it.titlecase(Locale("ru")) }
                    val labelColor = when {
                        dayOfWeek in weekendDays -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            color = labelColor
                        )
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                userScrollEnabled = false
            ) {
                items(days) { date ->
                    MonthDayCell(
                        date = date,
                        inCurrentMonth = date.month == month.month,
                        isToday = date == today,
                        isWeekend = date.dayOfWeek in weekendDays,
                        onClick = {
                            if (date.month == month.month) {
                                onDaySelected(date)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cellHeight)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    date: LocalDate,
    inCurrentMonth: Boolean,
    isToday: Boolean,
    isWeekend: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = inCurrentMonth
    val isMonday = date.dayOfWeek == DayOfWeek.MONDAY
    val weekNumber = remember(date) { date.get(WeekFields.ISO.weekOfWeekBasedYear()) }
    val containerColor = when {
        !enabled -> Color.Transparent
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else -> Color.Transparent
    }
    val border = if (isToday) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val dayNumberColor = when {
        !enabled -> contentColor.copy(alpha = 0.3f)
        isToday -> MaterialTheme.colorScheme.primary
        isWeekend -> MaterialTheme.colorScheme.primary
        else -> contentColor
    }
    val weekNumberColor = when {
        !enabled -> contentColor.copy(alpha = 0.2f)
        isWeekend -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        border = border,
        tonalElevation = if (isToday && enabled) 2.dp else 0.dp,
        shadowElevation = if (isToday && enabled) 4.dp else 0.dp,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                DayNumberBadge(
                    day = date.dayOfMonth,
                    isToday = isToday,
                    color = dayNumberColor,
                    enabled = enabled
                )
            }
            Spacer(modifier = Modifier.weight(1f, fill = true))
            if (isMonday) {
                Text(
                    text = weekNumber.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = weekNumberColor,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
        }
    }
}

@Composable
private fun DayNumberBadge(
    day: Int,
    isToday: Boolean,
    color: Color,
    enabled: Boolean
) {
    val badgeColor = when {
        !enabled -> Color.Transparent
        isToday -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val textColor = when {
        isToday && enabled -> MaterialTheme.colorScheme.onPrimary
        else -> color
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(badgeColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

/* --------------------------- DAY/WEEK STRIP ------------------------------ */

@Composable
fun DayWeekStrip(
    anchor: LocalDate,
    weekendDays: Set<DayOfWeek>,
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
                isWeekend = d.dayOfWeek in weekendDays,
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
    isWeekend: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val foreground = when {
        selected -> MaterialTheme.colorScheme.onSurface
        isWeekend -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = background,
        shape = MaterialTheme.shapes.medium,
        onClick = onClick,
        modifier = modifier,
        shadowElevation = if (selected) 6.dp else 4.dp
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
                color = foreground
            )
            Text(
                "${date.dayOfMonth}",
                style = MaterialTheme.typography.labelLarge,
                color = foreground
            )
        }
    }
}
