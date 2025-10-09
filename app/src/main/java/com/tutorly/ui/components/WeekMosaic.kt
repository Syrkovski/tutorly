package com.tutorly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/* =====================  WEEK MOSAIC (compact, 2×4)  ===================== */

@Composable
fun WeekMosaic(
    anchor: LocalDate,
    onOpenDay: (LocalDate) -> Unit,
    dayDataProvider: (LocalDate) -> List<LessonBrief> = { demoLessonsFor(it) },
    stats: WeeklyStats? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    hSpacing: Dp = 8.dp,
    vSpacing: Dp = 8.dp
) {
    val monday = anchor.with(DayOfWeek.MONDAY)
    val days = remember(monday) { (0..6).map { monday.plusDays(it.toLong()) } }

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

    val weekly = remember(days, dayDataProvider, stats) {
        stats ?: run {
            var total = 0
            var paid = 0
            var debts = 0
            var earned = 0L
            days.forEach { d ->
                val ls = dayDataProvider(d)
                total += ls.size
                paid += ls.count { it.paid }
                debts += ls.count { !it.paid }
                earned += ls.filter { it.paid }.sumOf { it.priceCents }
            }
            WeeklyStats(totalLessons = total, paidCount = paid, debtCount = debts, earnedCents = earned)
        }
    }

    val tiles = remember(dayCards, weekly) {
        dayCards.map { WeekTile.Day(it) } + WeekTile.Stats(weekly)
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
            items(tiles) { tile ->
                when (tile) {
                    is WeekTile.Day -> DayTile(
                        model = tile.model,
                        height = cellH,
                        onClick = { onOpenDay(tile.model.date) }
                    )
                    is WeekTile.Stats -> StatsTile(
                        stats = tile.stats,
                        height = cellH
                    )
                }
            }
        }
    }
}

/* -------------------------------- UI ----------------------------------- */

@Composable
private fun DayTile(
    model: DayCardModel,
    height: Dp,
    onClick: () -> Unit
) {
    val today = LocalDate.now()
    val isToday = model.date == today
    val hasLessons = model.totalLessons > 0

    val (ongoing, others) = remember(model, isToday) {
        if (!isToday) emptyList<LessonBrief>() to model.brief
        else model.brief.partition { it.isOngoingOn(model.date) }
    }

    // лёгкая подложка только если есть занятия
    val bg: Color = if (hasLessons)
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    else
        Color.Transparent

    Surface(
        color = bg,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .height(height)
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
                        time = timeRangeText(it),
                        who = it.student,
                        tone = LessonTone.Ongoing
                    )
                }
            }

            // Остальные (или все для не-сегодня)
            val toShow = if (isToday) others else model.brief
            toShow.forEach {
                LessonRowCompact(
                    time = timeRangeText(it),
                    who = it.student,
                    tone = if (it.paid) LessonTone.Paid else LessonTone.Default
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

private enum class LessonTone { Default, Paid, Ongoing }

@Composable
private fun LessonRowCompact(time: String, who: String, tone: LessonTone) {
    val (bg, fg) = when (tone) {
        LessonTone.Default -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) to
                MaterialTheme.colorScheme.onSurface
        LessonTone.Paid -> MaterialTheme.colorScheme.secondaryContainer to
                MaterialTheme.colorScheme.onSecondaryContainer
        LessonTone.Ongoing -> MaterialTheme.colorScheme.primaryContainer to
                MaterialTheme.colorScheme.onPrimaryContainer
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$time · $who",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = fg
        )
        when (tone) {
            LessonTone.Default -> {}
            LessonTone.Paid    -> Text("✓", color = fg, style = MaterialTheme.typography.labelSmall)
            LessonTone.Ongoing -> Text("Идёт", color = fg, style = MaterialTheme.typography.labelSmall)
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

@Composable
private fun StatsTile(
    stats: WeeklyStats,
    height: Dp
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.height(height)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(10.dp), // компакт
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Итоги недели",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatRow("Всего занятий", stats.totalLessons.toString())
                StatRow("Оплачено", stats.paidCount.toString())
                StatRow("Долгов", stats.debtCount.toString())
                StatRow("Заработано", formatMoney(stats.earnedCents))
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

/* ------------------------------- Models --------------------------------- */

data class LessonBrief(
    val time: String,    // "09:30"
    val end: String?,    // "10:30" (null — неизвестно)
    val student: String,
    val priceCents: Long,
    val paid: Boolean
)

private data class DayCardModel(
    val date: LocalDate,
    val brief: List<LessonBrief>,
    val totalLessons: Int
)

data class WeeklyStats(
    val totalLessons: Int,
    val paidCount: Int,
    val debtCount: Int,
    val earnedCents: Long
)

private sealed interface WeekTile {
    data class Day(val model: DayCardModel) : WeekTile
    data class Stats(val stats: WeeklyStats) : WeekTile
}

/* ------------------------------ Helpers --------------------------------- */

private fun dayTitle(d: LocalDate): String {
    val dow = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ru"))
        .replaceFirstChar { it.titlecase(Locale("ru")) }
    return "$dow ${d.dayOfMonth}"
}

private fun timeRangeText(l: LessonBrief): String =
    if (l.end.isNullOrBlank()) l.time else "${l.time}–${l.end}"

private fun LessonBrief.isOngoingOn(day: LocalDate): Boolean {
    val now = LocalDateTime.now()
    if (now.toLocalDate() != day) return false
    val start = parseLocalTime(time) ?: return false
    val endT  = parseLocalTime(end) ?: return false
    val nowT = now.toLocalTime()
    return nowT >= start && nowT < endT
}

private fun parseLocalTime(hhmm: String?): LocalTime? = try {
    if (hhmm == null) null else LocalTime.parse(hhmm)
} catch (_: Throwable) { null }

private fun formatMoney(amountCents: Long): String {
    val rubles = amountCents / 100
    val kopecks = (amountCents % 100).toInt()
    val nf = NumberFormat.getInstance(Locale("ru", "RU"))
    val base = nf.format(rubles)
    return if (kopecks == 0) "$base ₽"
    else String.format(Locale("ru", "RU"), "%s,%02d ₽", base, kopecks)
}

/* ----------------------------- Demo data -------------------------------- */

private fun demoLessonsFor(date: LocalDate): List<LessonBrief> {
    val seed = (date.dayOfMonth + date.monthValue) % 3
    return when (seed) {
        0 -> listOf(
            LessonBrief("09:30", "10:30", "Анна", 1500_00, true),
            LessonBrief("13:00", "14:30", "Иван", 2000_00, false),
            LessonBrief("18:00", "19:00", "Олег", 1500_00, true)
        )
        1 -> listOf(LessonBrief("16:00", "17:30", "Мария", 1800_00, true))
        else -> emptyList()
    }
}
