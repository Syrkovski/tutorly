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
import com.tutorly.ui.theme.NowAccent
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
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

enum class CalendarMode { DAY, WEEK, MONTH }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
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
            AppTopBar(title = calendarTitle)
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Outlined.DateRange,
                    contentDescription = stringResource(id = R.string.lesson_create_title)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
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
    val (startHour, endHourExclusive) = remember { computeTimelineBounds() }
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
    val nowMinutesFromStart = remember(isToday, currentDateTime, startHour) {
        if (!isToday) null else {
            val currentMinutes = currentDateTime.hour * MinutesPerHour + currentDateTime.minute
            currentMinutes - startHour * MinutesPerHour
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
            val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            val spineColor = MaterialTheme.colorScheme.primary
            Canvas(Modifier.matchParentSize()) {
                val rowH = HourHeight.toPx()
                val leftPad = LabelWidth.toPx()
                val spineW = 2.dp.toPx()

                repeat(hours.size + 1) { i ->
                    val y = i * rowH
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
                nowMinutesFromStart?.takeIf { it in 0..totalMinutes }?.let { minutes ->
                    val y = minutes * rowH / MinutesPerHour
                    drawLine(
                        color = NowAccent,
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
                    now = currentDateTime,
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
    now: ZonedDateTime,
    onLessonClick: (CalendarLesson) -> Unit
) {
    val startTime = lesson.start.toLocalTime()
    val startMin = startTime.hour * 60 + startTime.minute
    val baseMin = baseHour * 60
    val durationMinutes = lesson.duration.toMinutes().coerceAtLeast(SlotIncrementMinutes.toLong())

    // Переводим минуты в dp (1 мин = hourHeight/60)
    val minuteDp = hourHeight / 60f
    val top = minuteDp * (startMin - baseMin)
    val height = minuteDp * durationMinutes.toInt()

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
            .padding(start = LabelWidth + 8.dp, end = 8.dp) // от оси до правого края
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

private fun computeTimelineBounds(): Pair<Int, Int> = 6 to 24

/* ----------------------------- MONTH GRID -------------------------------- */

@Composable
private fun MonthCalendar(
    anchor: LocalDate,
    currentDateTime: ZonedDateTime,
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
                        isWeekend(dayOfWeek) -> NowAccent
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = inCurrentMonth
    val containerColor = when {
        !enabled -> Color.Transparent
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val border = if (isToday) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val dayNumberColor = when {
        !enabled -> contentColor.copy(alpha = 0.3f)
        isWeekend(date.dayOfWeek) -> NowAccent
        else -> contentColor
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        border = border,
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

private fun isWeekend(dayOfWeek: DayOfWeek): Boolean {
    return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
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
    val background = if (selected) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val foreground = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
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
