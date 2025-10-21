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
                anchor = anchor,
                onSelectDate = { selected ->
                    direction = when {
                        selected.isAfter(anchor) -> 1
                        selected.isBefore(anchor) -> -1
                        else -> 0
                    }
                    viewModel.selectDate(selected)
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
            onSwipeRight = prevPeriod,
            onSelectMode = { newMode ->
                direction = 0
                viewModel.setMode(newMode)
            },
            onTodayClick = {
                val today = uiState.currentDateTime.toLocalDate()
                direction = when {
                    today.isAfter(anchor) -> 1
                    today.isBefore(anchor) -> -1
                    else -> 0
                }
                viewModel.selectDate(today)
            }
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
    anchor: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onOpenSettings: () -> Unit
) {
    val locale = remember { Locale("ru") }
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("LLLL yyyy", locale) }
    val monthLabel = remember(anchor, locale) {
        val raw = monthFormatter.format(anchor)
        raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    GradientTopBarContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = stringResource(id = R.string.calendar_open_day_picker),
                        tint = Color.White
                    )
                }
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .clickable { showDatePicker = true },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

    if (showDatePicker) {
        DisposableEffect(anchor) {
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

@Composable
private fun CalendarModeToggle(
    selected: CalendarMode,
    onSelect: (CalendarMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember { listOf(CalendarMode.DAY, CalendarMode.WEEK) }
    Row(
        modifier = modifier
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val segmentShape = RoundedCornerShape(20.dp)
            val accent = MaterialTheme.extendedColors.accent
            val background = if (isSelected) {
                accent
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
            val contentColor = if (isSelected) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                onClick = {
                    if (!isSelected) onSelect(option)
                },
                shape = segmentShape,
                color = background,
                contentColor = contentColor,
                tonalElevation = 0.dp,
                shadowElevation = if (isSelected) 6.dp else 0.dp
            ) {
                val labelRes = if (option == CalendarMode.DAY) {
                    R.string.calendar_mode_day
                } else {
                    R.string.calendar_mode_week
                }
                Text(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
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
    onSwipeRight: () -> Unit,
    onSelectMode: (CalendarMode) -> Unit,
    onTodayClick: () -> Unit
) {
    val locale = remember { Locale("ru") }
    val weekStart = remember(anchor) { anchor.with(DayOfWeek.MONDAY) }
    val weekDays = remember(weekStart) { (0 until 7).map { weekStart.plusDays(it.toLong()) } }
    val today = remember(currentDateTime) { currentDateTime.toLocalDate() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                CalendarModeToggle(
                    selected = mode,
                    onSelect = onSelectMode,
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                )
            }
            FilledTonalButton(
                onClick = onTodayClick,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(text = stringResource(id = R.string.calendar_today_button))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevPeriod) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = stringResource(id = R.string.calendar_prev_period)
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    weekDays.forEach { day ->
                        WeekDayCell(
                            date = day,
                            isSelected = day == anchor,
                            isToday = day == today,
                            isWeekend = day.dayOfWeek in weekendDays,
                            locale = locale,
                            onClick = { onSelectDate(day) }
                        )
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
}

@Composable
private fun WeekDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isWeekend: Boolean,
    locale: Locale,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.extendedColors.accent
    val label = remember(date, locale) {
        date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    val number = remember(date) { date.dayOfMonth.toString() }
    val circleColor = if (isSelected) accent else Color.Transparent
    val numberColor = when {
        isSelected -> Color.White
        isWeekend -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val labelColor = when {
        isSelected -> Color.White
        isWeekend -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .widthIn(min = 44.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.titleMedium,
                color = numberColor
            )
        }
        if (isToday && !isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accent)
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
    val cardInsetPx = remember(density) { with(density) { 16.dp.toPx() } }
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
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(totalHeight)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    val accent = MaterialTheme.extendedColors.accent
                    Canvas(Modifier.matchParentSize()) {
                        val leftPad = LabelWidth.toPx()
                        val spineW = 2.dp.toPx()
                        hourMarks.forEach { minute ->
                            val y = (minute - startMinutes) * minuteHeightPx
                            drawLine(
                                color = gridLineColor,
                                start = androidx.compose.ui.geometry.Offset(leftPad, y),
                                end = androidx.compose.ui.geometry.Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        drawRect(
                            color = accent,
                            topLeft = androidx.compose.ui.geometry.Offset(leftPad - spineW / 2f, 0f),
                            size = androidx.compose.ui.geometry.Size(spineW, size.height)
                        )
                        nowMinutesFromStart?.let { minutes ->
                            val y = minutes * minuteHeightPx
                            drawLine(
                                color = accent,
                                start = androidx.compose.ui.geometry.Offset(leftPad, y),
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

                Spacer(modifier = Modifier.height(24.dp))
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
    val subjectColor = lesson.subjectColorArgb?.let { Color(it) } ?: MaterialTheme.extendedColors.accent
    val statusColor = statusInfo.background
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val timeRange = remember(lesson.start, lesson.end) {
        val startLabel = timeFormatter.format(lesson.start.toLocalTime())
        val endLabel = timeFormatter.format(lesson.end.toLocalTime())
        "$startLabel – $endLabel"
    }
    val secondaryLine = remember(lesson.studentGrade, lesson.subjectName) {
        val grade = normalizeGrade(lesson.studentGrade)
        val subject = lesson.subjectName?.takeIf { it.isNotBlank() }?.trim()
        listOfNotNull(grade, subject)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " • ")
    }

    Box(
        Modifier
            .fillMaxWidth()
            .offset(y = top)
            .height(height)
            .padding(start = LabelWidth + 16.dp, end = 16.dp)
    ) {
        Card(
            onClick = { onLessonClick(lesson) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                focusedElevation = 3.dp,
                hoveredElevation = 3.dp,
                pressedElevation = 2.dp,
                draggedElevation = 4.dp,
                disabledElevation = 0.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                        .background(subjectColor)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeRange,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatusChip(data = statusInfo)
                    }
                    Text(
                        text = lesson.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!secondaryLine.isNullOrBlank()) {
                        Text(
                            text = secondaryLine,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    lesson.lessonNote?.takeIf { it.isNotBlank() }?.let { note ->
                        Text(
                            text = note.trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                        .background(statusColor)
                )
            }
        }
    }
}

@Composable
private fun CalendarLesson.statusPresentation(now: ZonedDateTime): StatusChipData =
    statusChipData(paymentStatus, start, end, now)
