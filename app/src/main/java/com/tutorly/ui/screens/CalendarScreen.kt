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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import kotlin.math.abs
import com.tutorly.ui.components.WeekMosaic

enum class CalendarMode { DAY, WEEK, MONTH }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null
) {
    var mode by rememberSaveable { mutableStateOf(CalendarMode.DAY) }
    var anchor by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var direction by remember { mutableStateOf(0) } // -1 назад, +1 вперёд

    val prevPeriod = {
        direction = -1
        anchor = when (mode) {
            CalendarMode.DAY   -> anchor.minusDays(1)
            CalendarMode.WEEK  -> anchor.minusWeeks(1)
            CalendarMode.MONTH -> anchor.minusMonths(1)
        }
    }
    val nextPeriod = {
        direction = +1
        anchor = when (mode) {
            CalendarMode.DAY   -> anchor.plusDays(1)
            CalendarMode.WEEK  -> anchor.plusWeeks(1)
            CalendarMode.MONTH -> anchor.plusMonths(1)
        }
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

    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Хедер: тут же свайп (чтобы не конфликтовал со скроллом списка)
        PlanScreenHeader(
            anchor = anchor,
            mode = mode,
            onModeChange = { mode = it },
            onPrevPeriod = prevPeriod,
            onNextPeriod = nextPeriod,
            onAddClick = { onAddClick?.invoke() },
            onSelectDate = { anchor = it },
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
                when (mode) {
                    CalendarMode.DAY   -> DayTimeline(currentDate)
                    CalendarMode.WEEK  -> WeekMosaic(
                        anchor = currentDate, // внутри он сам возьмёт monday = anchor.with(MONDAY)
                        onOpenDay = { selected ->
                            // Вариант A: остаёмся в режиме WEEK и просто выставляем выбранный день якорём
//                            anchor = selected.with(DayOfWeek.MONDAY)

                            // Вариант B (если когда-нибудь решишь): перейти в DAY на выбранную дату
                             mode = CalendarMode.DAY
                             anchor = selected
                        })
                    CalendarMode.MONTH -> MonthPlaceholder(currentDate)
                }
            }
        }
    }
}


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

private data class LessonUi(
    val start: String,   // "HH:mm"
    val end: String,     // "HH:mm"
    val student: String,
    val subtitle: String,
    val paid: Boolean
)

@Composable
private fun DayTimeline(date: LocalDate) {
    // Пример данных
    val lessons = remember(date) {
        listOf(
            LessonUi("09:30", "10:30", "Анна Петрова", "Математика • 8 класс", paid = true),
            LessonUi("13:00", "14:30", "Иван Сидоров", "Английский • 7 класс", paid = false),
        )
    }

    val startHour = 9
    val endHourExclusive = 21
    val hours = (startHour until endHourExclusive).map { "%02d:00".format(it) }
    val hoursCount = hours.size
    val totalHeight: Dp = HourHeight * hoursCount

    val scroll = rememberScrollState()

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
        ) {
            // 1) Сетка фоном
            Canvas(Modifier.matchParentSize()) {
                val rowH = HourHeight.toPx()
                val leftPad = LabelWidth.toPx()
                val spineW = 2.dp.toPx()

                repeat(hoursCount + 1) { i ->
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
            }

            // 2) Уроки — точное позиционирование по времени, до правого края
            lessons.forEach { l ->
                LessonBlock(
                    lesson = l,
                    baseHour = startHour,
                    hourHeight = HourHeight
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

@Composable
private fun LessonBlock(
    lesson: LessonUi,
    baseHour: Int,
    hourHeight: Dp
) {
    // Парсим время
    val (sh, sm) = lesson.start.split(":").map { it.toInt() }
    val (eh, em) = lesson.end.split(":").map { it.toInt() }
    val startMin = sh * 60 + sm
    val endMin = eh * 60 + em
    val baseMin = baseHour * 60
    val durMin = (endMin - startMin).coerceAtLeast(30)

    // Переводим минуты в dp (1 мин = hourHeight/60)
    val minuteDp = hourHeight / 60f
    val top = minuteDp * (startMin - baseMin)
    val height = minuteDp * durMin

    Box(
        Modifier
            .fillMaxWidth()
            .offset(y = top)
            .height(height)
            .padding(start = LabelWidth + 8.dp, end = 8.dp) // от оси до правого края
    ) {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(lesson.student, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${lesson.start}–${lesson.end} • ${lesson.subtitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.7f)
                )
            }
        }
    }
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
