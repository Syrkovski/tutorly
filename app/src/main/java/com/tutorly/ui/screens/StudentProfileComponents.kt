package com.tutorly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tutorly.R
import com.tutorly.models.Lesson
import com.tutorly.models.PaymentStatus
import com.tutorly.models.Student
import com.tutorly.ui.components.PaymentBadge
import java.text.NumberFormat
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StudentSummaryCard(
    student: Student,
    hasDebt: Boolean,
    totalDebt: String,
    modifier: Modifier = Modifier
) {
    val subtitle = remember(student.note) {
        student.note
            ?.lineSequence()
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = student.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = subtitle ?: stringResource(id = R.string.student_details_header_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaymentBadge(paid = !hasDebt)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (hasDebt) {
                        stringResource(id = R.string.student_details_debt_amount, totalDebt)
                    } else {
                        stringResource(id = R.string.student_details_no_debt)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun StudentContactCard(
    student: Student,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.student_details_contact_title),
                style = MaterialTheme.typography.titleMedium
            )
            ContactInfoRow(
                icon = Icons.Outlined.Phone,
                label = stringResource(id = R.string.student_details_phone_label),
                value = student.phone,
                placeholder = stringResource(id = R.string.student_details_phone_placeholder)
            )
            ContactInfoRow(
                icon = Icons.Outlined.Message,
                label = stringResource(id = R.string.student_details_messenger_label),
                value = student.messenger,
                placeholder = stringResource(id = R.string.student_details_messenger_placeholder)
            )
        }
    }
}

@Composable
fun ContactInfoRow(
    icon: ImageVector,
    label: String,
    value: String?,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = CircleShape,
            tonalElevation = 2.dp
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(12.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value?.takeIf { it.isNotBlank() } ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value.isNullOrBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StudentStatsRow(
    totalLessons: Int,
    formattedTotalEarned: String,
    paidLessons: Int,
    hasDebt: Boolean,
    formattedDebt: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryStatCard(
            modifier = Modifier.weight(1f),
            value = totalLessons.toString(),
            label = stringResource(id = R.string.student_details_stats_lessons)
        )
        SummaryStatCard(
            modifier = Modifier.weight(1f),
            value = formattedTotalEarned,
            label = stringResource(id = R.string.student_details_stats_earned)
        )
        val (thirdValue, thirdLabel) = if (hasDebt) {
            formattedDebt to stringResource(id = R.string.student_details_stats_debt)
        } else {
            paidLessons.toString() to stringResource(id = R.string.student_details_stats_paid)
        }
        SummaryStatCard(
            modifier = Modifier.weight(1f),
            value = thirdValue,
            label = thirdLabel
        )
    }
}

@Composable
fun SummaryStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StudentNotesCard(
    note: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.student_details_notes_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LessonsHistoryCard(
    lessons: List<Lesson>,
    currencyFormatter: NumberFormat,
    locale: Locale,
    onLessonClick: (Lesson) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("d MMMM", locale)
    }
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("HH:mm", locale)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.student_details_history_title),
                style = MaterialTheme.typography.titleMedium
            )
            if (lessons.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.student_details_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    lessons.forEachIndexed { index, lesson ->
                        LessonHistoryRow(
                            lesson = lesson,
                            currencyFormatter = currencyFormatter,
                            dateFormatter = dateFormatter,
                            timeFormatter = timeFormatter,
                            onClick = onLessonClick
                        )
                        if (index != lessons.lastIndex) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonHistoryRow(
    lesson: Lesson,
    currencyFormatter: NumberFormat,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    onClick: (Lesson) -> Unit,
    modifier: Modifier = Modifier
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val start = remember(lesson.startAt) { lesson.startAt.atZone(zoneId) }
    val end = remember(lesson.endAt) { lesson.endAt.atZone(zoneId) }
    val durationMinutes = remember(lesson.startAt, lesson.endAt) {
        Duration.between(lesson.startAt, lesson.endAt).toMinutes().toInt().coerceAtLeast(0)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(lesson) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dateFormatter.format(start),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    id = R.string.student_details_history_time_range,
                    timeFormatter.format(start),
                    timeFormatter.format(end),
                    durationMinutes
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = currencyFormatter.format(lesson.priceCents / 100.0),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            LessonPaymentStatusBadge(status = lesson.paymentStatus)
        }
    }
}

@Composable
fun LessonPaymentStatusBadge(
    status: PaymentStatus,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val (labelRes, container, content) = when (status) {
        PaymentStatus.PAID -> Triple(
            R.string.lesson_status_paid,
            colorScheme.tertiaryContainer,
            colorScheme.onTertiaryContainer
        )
        PaymentStatus.DUE, PaymentStatus.UNPAID -> Triple(
            R.string.lesson_status_due,
            colorScheme.errorContainer,
            colorScheme.onErrorContainer
        )
        PaymentStatus.CANCELLED -> Triple(
            R.string.lesson_status_cancelled,
            colorScheme.surfaceVariant,
            colorScheme.onSurfaceVariant
        )
    }
    Surface(
        modifier = modifier,
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 1.dp
    ) {
        Text(
            text = stringResource(id = labelRes),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

