package com.tutorly.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.tutorly.models.PaymentStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.time.ZoneId
import java.util.Locale

private val DayTileMinHeight = 112.dp

/* =====================  WEEK MOSAIC (single column list)  ===================== */

@Composable
fun WeekMosaic(
    anchor: LocalDate,
    onOpenDay: (LocalDate) -> Unit,
    dayDataProvider: (LocalDate) -> List<LessonBrief> = { demoLessonsFor(it) },
    currentDateTime: ZonedDateTime,
    onLessonClick: (LessonBrief) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    itemSpacing: Dp = 12.dp
) {
    val monday = anchor.with(DayOfWeek.MONDAY)
    val days = remember(monday) { (0..6).map { monday.plusDays(it.toLong()) } }
    val today = remember(currentDateTime) { currentDateTime.toLocalDate() }
    val now = remember(currentDateTime) { currentDateTime }

    val dayCards = days.map { d ->
        val lessons = dayDataProvider(d)
        DayCardModel(
            date = d,
            brief = lessons,
            totalLessons = lessons.size
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        items(dayCards, key = { it.date }) { model ->
            DayTile(
                model = model,
                onClick = { onOpenDay(model.date) },
                today = today,
                now = now,
                onLessonClick = onLessonClick
            )
        }
    }
}

/* -------------------------------- UI ----------------------------------- */

@Composable
private fun DayTile(
    model: DayCardModel,
    onClick: () -> Unit,
    today: LocalDate,
    now: ZonedDateTime,
    onLessonClick: (LessonBrief) -> Unit
) {
    val isToday = model.date == today
    val (ongoing, others) = remember(model, isToday, now) {
        if (!isToday) emptyList<LessonBrief>() to model.brief
        else model.brief.partition { it.isOngoingOn(model.date, now) }
    }

    // Белая подложка только для сегодняшнего дня
    val bg: Color = Color.White

    val dayShape = MaterialTheme.shapes.medium
    Surface(
        color = bg,
        shape = dayShape,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = DayTileMinHeight)
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
                        currentDateTime = now,
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
                    currentDateTime = now,
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
private fun LessonRowCompact(
    lesson: LessonBrief,
    tone: LessonTone,
    currentDateTime: ZonedDateTime,
    onClick: () -> Unit
) {
    val (bg, fg, border) = when (tone) {
        LessonTone.Default -> Triple(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.onSurface,
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        )
        LessonTone.Ongoing -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        )
    }

    val shape = RoundedCornerShape(8.dp)
    Surface(
        color = bg,
        contentColor = fg,
        shape = shape,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 40.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = lesson.student,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            StatusChip(
                data = statusChipData(
                    paymentStatus = lesson.paymentStatus,
                    start = lesson.start,
                    end = lesson.end,
                    now = currentDateTime
                ),
                modifier = Modifier.size(24.dp)
            )
        }
    }
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
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val student: String,
    val grade: String?,
    val subjectName: String?,
    val subjectColorArgb: Int?,
    val paymentStatus: PaymentStatus
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

private fun LessonBrief.isOngoingOn(day: LocalDate, now: ZonedDateTime): Boolean {
    val current = now.withZoneSameInstant(start.zone)
    if (current.toLocalDate() != day) return false
    return !current.isBefore(start) && current.isBefore(end)
}

/* ----------------------------- Demo data -------------------------------- */

private fun demoLessonsFor(date: LocalDate): List<LessonBrief> {
    val zone = ZoneId.systemDefault()
    val seed = (date.dayOfMonth + date.monthValue) % 3
    return when (seed) {
        0 -> listOf(
            LessonBrief(
                id = 1L,
                start = date.atTime(9, 30).atZone(zone),
                end = date.atTime(10, 30).atZone(zone),
                student = "Анна",
                grade = null,
                subjectName = "Математика",
                subjectColorArgb = null,
                paymentStatus = PaymentStatus.PAID
            ),
            LessonBrief(
                id = 2L,
                start = date.atTime(13, 0).atZone(zone),
                end = date.atTime(14, 30).atZone(zone),
                student = "Иван",
                grade = "7 класс",
                subjectName = "Физика",
                subjectColorArgb = null,
                paymentStatus = PaymentStatus.UNPAID
            ),
            LessonBrief(
                id = 3L,
                start = date.atTime(18, 0).atZone(zone),
                end = date.atTime(19, 0).atZone(zone),
                student = "Олег",
                grade = null,
                subjectName = "Русский",
                subjectColorArgb = null,
                paymentStatus = PaymentStatus.DUE
            )
        )
        1 -> listOf(
            LessonBrief(
                id = 4L,
                start = date.atTime(16, 0).atZone(zone),
                end = date.atTime(17, 30).atZone(zone),
                student = "Мария",
                grade = "10 класс",
                subjectName = "Химия",
                subjectColorArgb = null,
                paymentStatus = PaymentStatus.PAID
            )
        )
        else -> emptyList()
    }
}
