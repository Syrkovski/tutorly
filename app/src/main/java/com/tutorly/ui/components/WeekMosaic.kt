package com.tutorly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

/* =====================  WEEK MOSAIC (compact, 2×4)  ===================== */

@Composable
fun WeekMosaic(
    anchor: LocalDate,
    onOpenDay: (LocalDate) -> Unit,
    dayDataProvider: (LocalDate) -> List<LessonBrief> = { demoLessonsFor(it) },
    currentDateTime: ZonedDateTime,
    onLessonClick: (LessonBrief) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    hSpacing: Dp = 8.dp,
    vSpacing: Dp = 8.dp
) {
    val monday = anchor.with(DayOfWeek.MONDAY)
    val days = remember(monday) { (0..6).map { monday.plusDays(it.toLong()) } }
    val today = remember(currentDateTime) { currentDateTime.toLocalDate() }
    val now = remember(currentDateTime) { currentDateTime.toLocalDateTime() }

    val dayCards = remember(days, dayDataProvider) {
        days.map { d ->
            val lessons = dayDataProvider(d)
            DayCardModel(
                date = d,
                brief = lessons.take(6), // больше строк вмещаем
                totalLessons = lessons.size
            )
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val gridH =
            maxHeight - contentPadding.calculateTopPadding() - contentPadding.calculateBottomPadding()
        val cellH = (gridH - vSpacing * 3f) / 4f

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(vSpacing),
            horizontalArrangement = Arrangement.spacedBy(hSpacing)
        ) {
            items(dayCards) { model ->
                DayTile(
                    model = model,
                    height = cellH,
                    onClick = { onOpenDay(model.date) },
                    today = today,
                    now = now,
                    onLessonClick = onLessonClick
                )
            }
        }
    }
}

/* -------------------------------- UI ----------------------------------- */

@Composable
private fun DayTile(
    model: DayCardModel,
    height: Dp,
    onClick: () -> Unit,
    today: LocalDate,
    now: LocalDateTime,
    onLessonClick: (LessonBrief) -> Unit
) {
    val isToday = model.date == today
    val hasLessons = model.totalLessons > 0

    val (ongoing, others) = remember(model, isToday, now) {
        if (!isToday) emptyList<LessonBrief>() to model.brief
        else model.brief.partition { it.isOngoingOn(model.date, now) }
    }

    // лёгкая подложка только если есть занятия
    val bg: Color = if (hasLessons)
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    else
        Color.Transparent

    val dayShape = MaterialTheme.shapes.medium
    Surface(
        color = bg,
        shape = dayShape,
        modifier = Modifier
            .height(height)
            .clip(dayShape)
            .clickable(onClick = onClick)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp), // компакт
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Заголовок дня + маленький бейдж "Сегодня"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    dayTitle(model.date),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (isToday) {
                    MiniBadge(text = "Сегодня")
                }
            }

            // Для "сегодня" — блок "Идут сейчас"
            if (isToday && ongoing.isNotEmpty()) {
                Text(
                    "Идут сейчас",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                ongoing.forEach {
                    LessonRowCompact(
                        lesson = it,
                        tone = LessonTone.Ongoing,
                        onClick = { onLessonClick(it) }
                    )
                }
            }

            // Остальные (или все для не-сегодня)
            val toShow = if (isToday) others else model.brief
            toShow.forEach {
                LessonRowCompact(
                    lesson = it,
                    tone = LessonTone.Default,
                    onClick = { onLessonClick(it) }
                )
            }

            val shown = (if (isToday) ongoing.size else 0) + toShow.size
            if (model.totalLessons > shown) {
                Text(
                    "+${model.totalLessons - shown} ещё…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // если нет занятий — внутри ничего, только заголовок
        }
    }
}

private enum class LessonTone { Default, Ongoing }

@Composable
private fun LessonRowCompact(lesson: LessonBrief, tone: LessonTone, onClick: () -> Unit) {
    val (bg, fg) = when (tone) {
        LessonTone.Default -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) to
                MaterialTheme.colorScheme.onSurface
        LessonTone.Ongoing -> MaterialTheme.colorScheme.primaryContainer to
                MaterialTheme.colorScheme.onPrimaryContainer
    }

    val shape = RoundedCornerShape(8.dp)
    Surface(
        color = bg,
        contentColor = fg,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubjectAvatar(name = lesson.subjectName, colorArgb = lesson.subjectColorArgb)
            Text(
                text = buildLessonTitle(lesson),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SubjectAvatar(name: String?, colorArgb: Int?): Unit {
    val label = name?.firstOrNull()?.uppercaseChar()?.toString() ?: "•"
    val background = colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.secondaryContainer
    val content = if (colorArgb != null) Color.White else MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content
        )
    }
}

private fun buildLessonTitle(lesson: LessonBrief): String {
    val extras = lesson.grade?.takeIf { it.isNotBlank() }
    return listOfNotNull(lesson.student, extras).joinToString(" • ")
}

@Composable
private fun MiniBadge(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/* ------------------------------- Models --------------------------------- */

data class LessonBrief(
    val id: Long,
    val start: LocalTime,
    val end: LocalTime?,
    val student: String,
    val grade: String?,
    val subjectName: String?,
    val subjectColorArgb: Int?
)

private data class DayCardModel(
    val date: LocalDate,
    val brief: List<LessonBrief>,
    val totalLessons: Int
)

/* ------------------------------ Helpers --------------------------------- */

private fun dayTitle(d: LocalDate): String {
    val dow = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ru"))
        .replaceFirstChar { it.titlecase(Locale("ru")) }
    return "$dow ${d.dayOfMonth}"
}

private fun LessonBrief.isOngoingOn(day: LocalDate, now: LocalDateTime): Boolean {
    if (now.toLocalDate() != day) return false
    val endTime = end ?: return false
    val nowTime = now.toLocalTime()
    return nowTime >= start && nowTime < endTime
}

/* ----------------------------- Demo data -------------------------------- */

private fun demoLessonsFor(date: LocalDate): List<LessonBrief> {
    val seed = (date.dayOfMonth + date.monthValue) % 3
    return when (seed) {
        0 -> listOf(
            LessonBrief(1L, LocalTime.of(9, 30), LocalTime.of(10, 30), "Анна", null, "Математика", null),
            LessonBrief(2L, LocalTime.of(13, 0), LocalTime.of(14, 30), "Иван", "7 класс", "Физика", null),
            LessonBrief(3L, LocalTime.of(18, 0), LocalTime.of(19, 0), "Олег", null, "Русский", null)
        )
        1 -> listOf(LessonBrief(4L, LocalTime.of(16, 0), LocalTime.of(17, 30), "Мария", "10 класс", "Химия", null))
        else -> emptyList()
    }
}
