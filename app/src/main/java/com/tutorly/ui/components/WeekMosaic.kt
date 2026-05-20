package com.tutorly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tutorly.models.PaymentStatus
import com.tutorly.ui.theme.TutorlyColors
import com.tutorly.ui.theme.extendedColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.TextStyle
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
    contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp),
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
        modifier = Modifier.fillMaxSize().fillMaxWidth(),
        contentPadding = contentPadding,
//        verticalArrangement = Arrangement.spacedBy(itemSpacing),
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
    val bg = Color.Transparent

    val dayShape = MaterialTheme.shapes.medium
    Surface(
        color = bg,
//        shape = dayShape,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        if (model.totalLessons != 0) {
//                    EmptyDayPlaceholder()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
//                .heightIn(min = DayTileMinHeight)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                val locale = remember { Locale("ru") }
                val dayOfWeekLabel = remember(model.date, locale) {
                    model.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                        .replaceFirstChar { it.titlecase(locale) }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
//                    Spacer(Modifier.height(10.dp))
                    if (isToday) {
                        MiniBadge()
                    }
                    Text(
                        text = dayOfWeekLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = model.date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val toShow = if (isToday) others else model.brief

                    if (model.totalLessons == 0) {
//                    EmptyDayPlaceholder()
                    } else {
                        if (isToday && ongoing.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                            Text(
//                                text = "Идут сейчас",
//                                style = MaterialTheme.typography.labelSmall,
//                                color = MaterialTheme.colorScheme.primary
//                            )
                                ongoing.forEach {
                                    LessonRowCompact(
                                        lesson = it,
                                        tone = LessonTone.Default,
                                        currentDateTime = now,
                                        onClick = { onLessonClick(it) }
                                    )
                                }
                            }
                        }

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
                                text = "+${model.totalLessons - shown} ещё…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
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
    val status = statusChipData(
        paymentStatus = lesson.paymentStatus,
        start = lesson.start,
        end = lesson.end,
        now = currentDateTime
    )
    val isFutureLesson = lesson.start.isAfter(currentDateTime)

    val hasSubject = !lesson.subjectName.isNullOrBlank()
    val title = lesson.student

    val subtitleParts = mutableListOf<String>()
    if (hasSubject) {
        lesson.grade?.takeIf { it.isNotBlank() }?.let { subtitleParts += it.trim() }
        subtitleParts += lesson.subjectName!!.trim()
    } else {
        lesson.grade?.takeIf { it.isNotBlank() }?.let { subtitleParts += it.trim() }
    }
    val subtitle = subtitleParts.takeIf { it.isNotEmpty() }?.joinToString(separator = " • ")

    val (statusIcon, statusLabel, badgeContainer, badgeContent) = when {
        lesson.paymentStatus == PaymentStatus.PAID && isFutureLesson -> Quadruple("∞", "Абонемент", Color(0xFFE9ECFF), Color(0xFF4A56D9))
        lesson.paymentStatus == PaymentStatus.PAID -> Quadruple("✓", "Оплачено", Color(0xFFE6F7EC), Color(0xFF2DA45A))
        lesson.paymentStatus == PaymentStatus.UNPAID && !isFutureLesson -> Quadruple("!", "Долг", Color(0xFFFFE9EC), Color(0xFFD64258))
        lesson.paymentStatus == PaymentStatus.CANCELLED -> Quadruple("×", "Отменено", Color(0xFFEDF1F6), Color(0xFF667085))
        else -> Quadruple("◷", "Ожидает оплаты", Color(0xFFFFF2E6), Color(0xFFE08A22))
    }

    ScheduleLessonCard(
        studentName = title,
        subtitle = subtitle,
        statusIcon = statusIcon,
        statusLabel = statusLabel,
        badgeContainerColor = badgeContainer,
        badgeContentColor = badgeContent,
        amountText = formatRubles(lesson.priceCents),
        statusStripeColor = status.background,
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
        onClick = onClick
    )
}

private fun formatRubles(amountCents: Int): String {
    val rubles = (amountCents / 100.0)
    val formatted = java.text.DecimalFormat("#,##0").format(rubles).replace(',', ' ')
    return "$formatted ₽"
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun MiniBadge() {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.extendedColors.accent)
    )
}

@Composable
private fun EmptyDayPlaceholder() {
    Surface(
//        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
//            .compositeOver(MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Занятий нет",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
    val priceCents: Int,
    val paymentStatus: PaymentStatus
)

private data class DayCardModel(
    val date: LocalDate,
    val brief: List<LessonBrief>,
    val totalLessons: Int
)

/* ------------------------------ Helpers --------------------------------- */

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
                priceCents = 150_000,
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
                priceCents = 180_000,
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
                priceCents = 200_000,
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
                priceCents = 170_000,
                paymentStatus = PaymentStatus.PAID
            )
        )
        else -> emptyList()
    }
}
