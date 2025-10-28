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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.layout.wrapContentSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import android.app.DatePickerDialog
import com.tutorly.R
import com.tutorly.ui.CalendarEvent
import com.tutorly.models.PaymentStatus
import com.tutorly.ui.components.LessonBrief
import com.tutorly.ui.components.StatusChipData
import com.tutorly.ui.components.TopBarContainer
import com.tutorly.ui.components.WeekMosaic
import com.tutorly.ui.components.statusChipData
import com.tutorly.ui.screens.normalizeGrade
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
import java.time.format.TextStyle
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

enum class CalendarMode { DAY, WEEK }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    onAddStudent: () -> Unit = {},
    onEditStudent: (Long) -> Unit = {},
    onOpenStudentProfile: (Long) -> Unit = {},
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
        onAddStudent = {
            lessonCardViewModel.dismiss()
            creationViewModel.prepareForStudentCreation()
            onAddStudent()
        },
        onEditStudent = onEditStudent,
        onOpenStudentProfile = onOpenStudentProfile,
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
        onRecurrenceToggle = creationViewModel::onRecurrenceEnabledChanged,
        onRecurrenceModeChange = creationViewModel::onRecurrenceModeSelected,
        onRecurrenceDayToggle = creationViewModel::onRecurrenceDayToggled,
        onRecurrenceIntervalChange = creationViewModel::onRecurrenceIntervalChanged,
        onRecurrenceEndToggle = creationViewModel::onRecurrenceEndEnabledChanged,
        onRecurrenceEndDateSelect = creationViewModel::onRecurrenceEndDateSelected,
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

    val handleSelectDate: (LocalDate) -> Unit = { selected ->
        direction = when {
            selected.isAfter(anchor) -> 1
            selected.isBefore(anchor) -> -1
            else -> 0
        }
        viewModel.selectDate(selected)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CalendarTopBar(
                anchor = anchor,
                onSelectDate = handleSelectDate,
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

//            containerColor = Color.Transparent
        }
    ) { padding ->
        val dayProgressMessage = if (mode == CalendarMode.DAY) {
            val lessonsForDay = uiState.lessonsWithinBoundsByDate[anchor].orEmpty()
            val completedCount = lessonsForDay.count { lesson ->
                !lesson.end.isAfter(uiState.currentDateTime)
            }
            val remainingCount = (lessonsForDay.size - completedCount).coerceAtLeast(0)
            val completedText = pluralStringResource(
                id = R.plurals.calendar_day_completed_lessons,
                count = completedCount,
                completedCount
            )
            val remainingText = pluralStringResource(
                id = R.plurals.calendar_day_remaining_lessons,
                count = remainingCount,
                remainingCount
            )
            stringResource(
                id = R.string.calendar_day_progress_summary,
                completedText,
                remainingText
            )
        } else {
            null
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CalendarTimelineHeader(
                anchor = anchor,
                currentDateTime = uiState.currentDateTime,
                mode = mode,
                onSelectDate = handleSelectDate,
                onSelectMode = { newMode ->
                    direction = 0
                    viewModel.setMode(newMode)
                },
                onSwipeLeft = nextPeriod,
                onSwipeRight = prevPeriod,
                progressMessage = dayProgressMessage
            )

            // Контент занимает остаток экрана и скроллится внутри
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .then(swipeModifier)   // 👈 свайп теперь работает по всему экрану
            ) {
                val workdayBounds =
                    remember(uiState.workDayStartMinutes, uiState.workDayEndMinutes) {
                        sanitizeWorkdayBounds(
                            startMinutes = uiState.workDayStartMinutes,
                            endMinutes = uiState.workDayEndMinutes
                        )
                    }

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
                    },
                    label = "day-switch"
                ) { currentDate ->
                    when (mode) {
                        CalendarMode.DAY -> {
                            val lessonsForCurrent = remember(
                                currentDate,
                                uiState.lessonsWithinBoundsByDate
                            ) {
                                uiState.lessonsWithinBoundsByDate[currentDate]
                                    .orEmpty()
                            }
                            DayTimeline(
                                date = currentDate,
                                lessons = lessonsForCurrent,
                                currentDateTime = uiState.currentDateTime,
                                workDayStartMinutes = workdayBounds.startMinutes,
                                workDayEndMinutes = workdayBounds.endMinutes,
                                onLessonClick = { lesson ->
                                    lessonCardViewModel.open(lesson.id)
                                },
                                onEmptySlot = { startTime ->
                                    viewModel.onEmptySlotSelected(
                                        currentDate,
                                        startTime,
                                        DefaultSlotDuration
                                    )
                                },
                                onLessonMoved = viewModel::onLessonMoved
                            )
                        }

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
                                uiState.lessonsByDate[date]
                                    .orEmpty()
                                    .map { it.toLessonBrief() }
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
fun CalendarTopBar(
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

    TopBarContainer {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .align(Alignment.Center)
//                    .padding(horizontal = 96.dp)
                    .clickable { showDatePicker = true },
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.CenterEnd),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(id = R.string.settings_title)
                )
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
private fun CalendarTimelineHeader(
    anchor: LocalDate,
    currentDateTime: ZonedDateTime,
    mode: CalendarMode,
    onSelectDate: (LocalDate) -> Unit,
    onSelectMode: (CalendarMode) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    progressMessage: String?,
    modifier: Modifier = Modifier
) {
    val locale = remember { Locale("ru") }
    val weekStart = remember(anchor) { anchor.with(DayOfWeek.MONDAY) }
    val weekDays = remember(weekStart) { (0 until 7).map { weekStart.plusDays(it.toLong()) } }
    val today = remember(currentDateTime) { currentDateTime.toLocalDate() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFEFEFE))
    ) {
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            weekDays.forEach { day ->
                WeekDayCell(
                    modifier = Modifier.weight(1f),
                    date = day,
                    isSelected = day == anchor,
                    isToday = day == today,
                    locale = locale,
                    onClick = { onSelectDate(day) }
                )
            }
        }

        CalendarModeToggle(
            selected = mode,
            onSelect = onSelectMode,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
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
        )

        progressMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
            )
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
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val accent = MaterialTheme.extendedColors.accent
    val inactiveColor = Color(0xFFB9BCC7)
    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
//        containerColor = Color(0xFFFEFEFE),
        contentColor = MaterialTheme.colorScheme.onSurface,
        divider = {},
        indicator = { tabPositions ->
            if (selectedIndex in tabPositions.indices) {
                val position = tabPositions[selectedIndex]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.BottomStart)
                ) {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(position)
                            .padding(horizontal = 38.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(accent)
                    )
                }
            }
        }
    ) {
        options.forEachIndexed { index, option ->
            val labelRes = if (option == CalendarMode.DAY) {
                R.string.calendar_mode_day
            } else {
                R.string.calendar_mode_week
            }
            val isSelected = index == selectedIndex
            Tab(
                selected = isSelected,
                onClick = {
                    if (!isSelected) onSelect(option)
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            inactiveColor
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun WeekDayCell(
    modifier: Modifier = Modifier,
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    locale: Locale,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.extendedColors.accent
    val label = remember(date, locale) {
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    val number = remember(date) { date.dayOfMonth.toString() }
    val circleColor = if (isSelected) accent else Color.Transparent
    val numberColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    val labelColor = Color(0xFFB9BCC7)

    Column(
        modifier = modifier
            .width(40.dp)

            .clickable(onClick = onClick)
            .padding(vertical = 0.dp, horizontal = 4.dp),
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
                .height(40.dp)
                .width(40.dp)
                .clip(CircleShape)
                .shadow(if (isSelected) 6.dp else 0.dp, CircleShape, clip = false)
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
private const val SlotIncrementMinutes: Int = 30
private const val MINUTES_IN_DAY: Int = MinutesPerHour * 24
private const val MAX_END_MINUTE: Int = MINUTES_IN_DAY
private const val MAX_START_MINUTE: Int = MAX_END_MINUTE - SlotIncrementMinutes

@Composable
private fun DayTimeline(
    date: LocalDate,
    lessons: List<CalendarLesson>,
    currentDateTime: ZonedDateTime,
    workDayStartMinutes: Int,
    workDayEndMinutes: Int,
    onLessonClick: (CalendarLesson) -> Unit,
    onEmptySlot: (LocalTime) -> Unit,
    onLessonMoved: (CalendarLesson, ZonedDateTime) -> Unit
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
    var dragState by remember { mutableStateOf<LessonDragState?>(null) }
    LaunchedEffect(dayLessons) {
        dragState?.let { state ->
            if (dayLessons.none { it.id == state.lesson.id }) {
                dragState = null
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
//            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 0.dp, vertical = 0.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
//            shape = RoundedCornerShape(28.dp),
//            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
//            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(totalHeight)
                        .padding(horizontal = 0.dp, vertical = 0.dp)
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
                    val circleColor = MaterialTheme.colorScheme.background
                    Canvas(Modifier.matchParentSize()) {
                        val leftPad = LabelWidth.toPx()
                        val spineW = 2.dp.toPx()
                        hourMarks.forEach { minute ->
                            val y = (minute - startMinutes) * minuteHeightPx
                            drawLine(
                                color = gridLineColor,
                                start = Offset(20f, y),
                                end = Offset(size.width-20, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        drawRect(
                            color = accent,
                            topLeft = Offset(leftPad - spineW / 2f, 0f),
                            size = Size(spineW, size.height)
                        )
//                        nowMinutesFromStart?.let { minutes ->
//                            val y = minutes * minuteHeightPx
//                            drawLine(
//                                color = accent,
//                                start = Offset(leftPad, y),
//                                end = Offset(size.width, y),
//                                strokeWidth = 2.dp.toPx()
//                            )
//                        }
                        nowMinutesFromStart?.let { minutes ->
                            val y = minutes * minuteHeightPx

                            val lineWidth = 2.dp.toPx()
                            val ringStroke = 2.dp.toPx()
                            val circleRadiusWhite = 12.dp.toPx()
                            val circleRadiusAccentRing = 8.dp.toPx()
                            val circleRadiusAccent = 5.dp.toPx()// подберёшь по вкусу
                            val circleCx = leftPad                   // центр кружка на оси времени

                            // 1) Линия от кружка вправо (чтобы не «перечёркивала» центр)
//                            drawLine(
//                                color = accent,
//                                start = Offset(circleCx + circleRadius, y),
//                                end   = Offset(size.width, y),
//                                strokeWidth = lineWidth
//                            )

                            // 2) Белый «фон» кружка (чтобы перекрыть вертикальную ось/линии под ним)
                            drawCircle(
                                color = circleColor,
                                radius = circleRadiusWhite,
                                center = Offset(circleCx, y)
                            )

                            // 3) Акцентное кольцо (как на скрине)
                            drawCircle(
                                color = accent,
                                radius = circleRadiusAccentRing,
                                center = Offset(circleCx, y),
                                style = Stroke(width = ringStroke)
                            )

                            drawCircle(
                                color = accent,
                                radius = circleRadiusAccent,
                                center = Offset(circleCx, y),
//                                style = Stroke(width = ringStroke)
                            )
                        }
                    }

                    dayLessons.forEach { lesson ->
                        val lessonStartTime = lesson.start.toLocalTime()
                        val lessonStartMinutes = lessonStartTime.hour * MinutesPerHour + lessonStartTime.minute
                        val offsetMinutes = (lessonStartMinutes - startMinutes).coerceAtLeast(0)
                        val durationMinutes = lesson.duration
                            .toMinutes()
                            .toInt()
                            .coerceAtLeast(SlotIncrementMinutes)
                        val baseTopPx = offsetMinutes * minuteHeightPx
                        val baseHeightPx = durationMinutes * minuteHeightPx
                        val isDragging = dragState?.lesson?.id == lesson.id
                        val translationPx = if (isDragging) dragState!!.translationPx else 0f

                        LessonBlock(
                            lesson = lesson,
                            baseMinutes = startMinutes,
                            minuteHeight = minuteHeight,
                            now = currentDateTime,
                            onLessonClick = onLessonClick,
                            onDragStart = {
                                dragState = LessonDragState(
                                    lesson = lesson,
                                    baseTopPx = baseTopPx,
                                    baseHeightPx = baseHeightPx
                                )
                            },
                            onDrag = { _, delta ->
                                dragState?.takeIf { it.lesson.id == lesson.id }?.let { state ->
                                    val minTranslation = -state.baseTopPx
                                    val maxTranslation = totalHeightPx - state.baseTopPx - state.baseHeightPx
                                    val newTranslation = (state.translationPx + delta)
                                        .coerceIn(minTranslation, maxTranslation)
                                    dragState = state.copy(translationPx = newTranslation)
                                }
                            },
                            onDragEnd = {
                                val state = dragState?.takeIf { it.lesson.id == lesson.id } ?: run {
                                    dragState = null
                                    return@LessonBlock
                                }
                                val stateLesson = state.lesson
                                val targetTopPx = (state.baseTopPx + state.translationPx)
                                    .coerceIn(0f, totalHeightPx - state.baseHeightPx)
                                val minutesWithin = (targetTopPx / minuteHeightPx).roundToInt()
                                val candidate = startMinutes + minutesWithin
                                val lessonDurationMinutes = stateLesson.duration
                                    .toMinutes()
                                    .toInt()
                                    .coerceAtLeast(SlotIncrementMinutes)
                                val maxStart = (endMinutes - lessonDurationMinutes).coerceAtLeast(startMinutes)
                                val normalized = candidate.coerceIn(startMinutes, maxStart)
                                val slots = (normalized - startMinutes) / SlotIncrementMinutes
                                val snapped = (startMinutes + slots * SlotIncrementMinutes)
                                    .coerceIn(startMinutes, maxStart)
                                val hour = snapped / MinutesPerHour
                                val minute = snapped % MinutesPerHour
                                val zone = stateLesson.start.zone
                                val baseLocal = date.atTime(hour, minute)
                                val adjustedLocal = baseLocal
                                    .withSecond(stateLesson.start.second)
                                    .withNano(stateLesson.start.nano)
                                val newStart = adjustedLocal.atZone(zone)
                                onLessonMoved(stateLesson, newStart)
                                dragState = null
                            },
                            onDragCancel = { dragState = null },
                            isDragging = isDragging,
                            dragTranslationPx = translationPx
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
                                    .offset(y = minuteHeight * (minute - startMinutes).toFloat()+8.dp, x=8.dp)
                            )
                        }
                    }
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

private data class LessonDragState(
    val lesson: CalendarLesson,
    val baseTopPx: Float,
    val baseHeightPx: Float,
    val translationPx: Float = 0f
)

@Composable
private fun LessonBlock(
    lesson: CalendarLesson,
    baseMinutes: Int,
    minuteHeight: Dp,
    now: ZonedDateTime,
    onLessonClick: (CalendarLesson) -> Unit,
    onDragStart: (CalendarLesson) -> Unit,
    onDrag: (CalendarLesson, Float) -> Unit,
    onDragEnd: (CalendarLesson) -> Unit,
    onDragCancel: () -> Unit,
    isDragging: Boolean,
    dragTranslationPx: Float
) {
    val startTime = lesson.start.toLocalTime()
    val startMin = startTime.hour * MinutesPerHour + startTime.minute
    val offsetMinutes = (startMin - baseMinutes).coerceAtLeast(0)
    val durationMinutes = lesson.duration.toMinutes().coerceAtLeast(SlotIncrementMinutes.toLong())

    val top = minuteHeight * offsetMinutes.toFloat()
    val height = minuteHeight * durationMinutes.toInt().toFloat()

    val lessonUi = lesson.toLessonUi(now)

    Box(
        Modifier
            .fillMaxWidth()
            .offset(y = top+4.dp)
            .height(height-8.dp)
            .padding(start = LabelWidth + 16.dp, end = 20.dp)
            .graphicsLayer { translationY = dragTranslationPx }
            .zIndex(if (isDragging) 1f else 0f)
            .pointerInput(lesson.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart(lesson) },
                    onDragEnd = { onDragEnd(lesson) },
                    onDragCancel = onDragCancel,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(lesson, dragAmount.y)
                    }
                )
            }
            .clip(RoundedCornerShape(12.dp))
    ) {
        val cardShape = RoundedCornerShape(12.dp)
        val innerStrokeWidth = 1.dp
        Card(
            modifier = Modifier.fillMaxSize(),
            onClick = { onLessonClick(lesson) },
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            enabled = !isDragging
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val strokeWidth = innerStrokeWidth.toPx()
                        val radius = 12.dp.toPx().coerceAtLeast(0f)
                        val adjustedRadius = (radius - strokeWidth / 2f).coerceAtLeast(0f)
                        drawRoundRect(
                            color = Color(0x14000000),
                            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                            size = Size(size.width - strokeWidth+20, size.height - strokeWidth),
                            cornerRadius = CornerRadius(adjustedRadius, adjustedRadius),
                            style = Stroke(width = strokeWidth)
                        )
                    }
            ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
//                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = lessonUi.studentName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (lessonUi.isRecurring) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = lessonUi.recurrenceLabel ?: stringResource(id = R.string.lesson_recurring_short),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    lessonUi.secondaryLine?.let { secondaryLine ->
                        Text(
                            text = secondaryLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
//                    lessonUi.note?.let { note ->
//                        Text(
//                            text = note,
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            maxLines = 1,
//                            overflow = TextOverflow.Ellipsis
//                        )
//                    }
//                    Text(
//                        text = lessonUi.statusDescription,
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
//                .offset(x = 12.dp)
                .fillMaxHeight()
                .width(12.dp)
                .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                .background(lessonUi.statusColor),

        )
    }
}

@Composable
private fun CalendarLesson.statusPresentation(now: ZonedDateTime): StatusChipData =
    statusChipData(paymentStatus, start, end, now)

private data class LessonUi(
    val studentName: String,
    val secondaryLine: String?,
    val note: String?,
    val statusDescription: String,
    val statusColor: Color,
    val isRecurring: Boolean,
    val recurrenceLabel: String?
)

@Composable
private fun CalendarLesson.toLessonUi(now: ZonedDateTime): LessonUi {
    val status = statusPresentation(now)
    val grade = normalizeGrade(studentGrade)
    val subject = subjectName?.takeIf { it.isNotBlank() }?.trim()
    val secondaryLine = listOfNotNull(grade, subject)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(separator = " • ")
    val note = lessonNote?.takeIf { it.isNotBlank() }?.trim()
    val recurrence = recurrenceLabel?.takeIf { isRecurring }

    return LessonUi(
        studentName = studentName,
        secondaryLine = secondaryLine,
        note = note,
        statusDescription = status.description,
        statusColor = status.background,
        isRecurring = isRecurring,
        recurrenceLabel = recurrence
    )
}

