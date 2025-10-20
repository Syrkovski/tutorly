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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.app.DatePickerDialog
import com.tutorly.R
import com.tutorly.ui.CalendarEvent
import com.tutorly.domain.model.PaymentStatusIcon
import com.tutorly.models.PaymentStatus
import com.tutorly.ui.components.GradientTopBarContainer
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
import com.tutorly.ui.theme.CardSurface
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

enum class CalendarMode { DAY, WEEK }

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

    LaunchedEffect(mode) {
    }

    LessonCardSheet(
        state = lessonCardState,
        onDismissRequest = lessonCardViewModel::dismiss,
        onStudentSelect = lessonCardViewModel::onStudentSelected,
        onAddStudent = {
            lessonCardViewModel.dismiss()
            creationViewModel.prepareForStudentCreation()
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
        onStudentGradeChange = creationViewModel::onStudentGradeChanged,
        onStudentSelect = creationViewModel::onStudentSelected,
        onSubjectInputChange = creationViewModel::onSubjectInputChanged,
        onSubjectSelect = creationViewModel::onSubjectSelected,
        onSubjectSuggestionToggle = creationViewModel::onSubjectSuggestionToggled,
        onSubjectChipRemove = creationViewModel::onSubjectChipRemoved,
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
        topBar = {
            CalendarTopBar(
                selectedMode = mode,
                onSelectMode = { newMode ->
                    direction = 0
                    viewModel.setMode(newMode)
                },
                onOpenSettings = onOpenSettings
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
                    imageVector = Icons.Outlined.Add,
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
            currentDateTime = uiState.currentDateTime,
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


/* ----------------------------- TOP BAR ----------------------------------- */

@Composable
private fun CalendarTopBar(
    selectedMode: CalendarMode,
    onSelectMode: (CalendarMode) -> Unit,
    onOpenSettings: () -> Unit
) {
    val displayMode = remember(selectedMode) { selectedMode }

    GradientTopBarContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalendarModeToggle(
                selected = displayMode,
                onSelect = onSelectMode
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(id = R.string.calendar_open_settings),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun CalendarModeToggle(
    selected: CalendarMode,
    onSelect: (CalendarMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember { listOf(CalendarMode.DAY, CalendarMode.WEEK) }
    Row(
        modifier = modifier
            .selectableGroup()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val segmentShape = RoundedCornerShape(20.dp)
            val background = if (isSelected) {
                Color.White
            } else {
                Color.White.copy(alpha = 0.12f)
            }
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onPrimary
            }

            Surface(
                onClick = {
                    if (!isSelected) onSelect(option)
                },
                shape = segmentShape,
                color = background,
                contentColor = contentColor,
                tonalElevation = 0.dp,
                shadowElevation = if (isSelected) 2.dp else 0.dp
            ) {
                val labelRes = if (option == CalendarMode.DAY) {
                    R.string.calendar_mode_day
                } else {
                    R.string.calendar_mode_week
                }
                Text(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    text = stringResource(id = labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor
                )
            }
        }
    }
}


/* ----------------------------- HEADER ----------------------------------- */

@Composable
fun PlanScreenHeader(
    anchor: LocalDate,
    mode: CalendarMode,
    weekendDays: Set<DayOfWeek>,
    currentDateTime: ZonedDateTime,
    onPrevPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val locale = remember { Locale("ru") }
    val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM yyyy", locale) }
    val dayMonthFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
    val periodLabel = remember(anchor, mode) {
        when (mode) {
            CalendarMode.WEEK -> {
                val weekStart = anchor.with(DayOfWeek.MONDAY)
                val weekEnd = weekStart.plusDays(6)
                when {
                    weekStart.month == weekEnd.month && weekStart.year == weekEnd.year -> {
                        val endText = dayFormatter.format(weekEnd)
                        "${weekStart.dayOfMonth} – $endText"
                    }
                    weekStart.year == weekEnd.year -> {
                        val startText = dayMonthFormatter.format(weekStart)
                        val endText = dayFormatter.format(weekEnd)
                        "$startText – $endText"
                    }
                    else -> {
                        val startText = dayFormatter.format(weekStart)
                        val endText = dayFormatter.format(weekEnd)
                        "$startText – $endText"
                    }
                }
            }
            else -> dayFormatter.format(anchor)
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onPrevPeriod) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = stringResource(id = R.string.calendar_prev_period)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) {
                Surface(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(24.dp),
                    color = CardSurface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
//                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null
                        )
                        Text(
                            text = periodLabel,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Outlined.ExpandMore,
                            contentDescription = stringResource(id = R.string.calendar_open_day_picker)
                        )
                    }
                }

                if (showDatePicker) {
                    DisposableEffect(Unit) {
                        val picker = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                showDatePicker = false
                                onSelectDate(LocalDate.of(year, month + 1, dayOfMonth))
                            },
                            anchor.year,
                            anchor.monthValue - 1,
                            anchor.dayOfMonth
                        )
                        picker.setOnDismissListener { showDatePicker = false }
                        picker.show()
                        onDispose {
                            picker.setOnDismissListener(null)
                            if (picker.isShowing) {
                                picker.dismiss()
                            }
                        }
                    }
                }
            }

            IconButton(onClick = onNextPeriod) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = stringResource(id = R.string.calendar_next_period)
                )
            }
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
            .background(Color(0xFFFFFFFF))
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
