package com.tutorly.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant

@Composable
fun LessonEditorScreen(
    startTime: Instant?,
    studentId: Long?,
    onClose: () -> Unit
) {
    LessonPlaceholder(
        title = "Создать урок",
        primary = startTime?.toString() ?: "—",
        secondary = studentId?.toString() ?: "—",
        actionLabel = "Закрыть",
        onAction = onClose
    )
}

@Composable
fun LessonDetailsScreen(
    lessonId: Long,
    studentId: Long?,
    startTime: Instant?,
    onBack: () -> Unit
) {
    LessonPlaceholder(
        title = "Урок #$lessonId",
        primary = startTime?.toString() ?: "—",
        secondary = studentId?.toString() ?: "—",
        actionLabel = "Назад",
        onAction = onBack
    )
}

@Composable
private fun LessonPlaceholder(
    title: String,
    primary: String,
    secondary: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(text = "startTime: $primary", style = MaterialTheme.typography.bodyMedium)
        Text(text = "studentId: $secondary", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onAction) {
            Text(text = actionLabel)
        }
    }
}
