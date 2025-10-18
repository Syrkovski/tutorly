package com.tutorly.ui.lessoncard

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tutorly.R
import com.tutorly.models.PaymentStatus
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import com.tutorly.ui.components.TutorlyBottomSheetContainer
import com.tutorly.ui.theme.TutorlyCardDefaults
import com.tutorly.ui.theme.extendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LessonCardSheet(
    state: LessonCardUiState,
    onDismissRequest: () -> Unit,
    onStudentSelect: (Long) -> Unit,
    onAddStudent: () -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onTimeSelect: (LocalTime) -> Unit,
    onDurationSelect: (Int) -> Unit,
    onPriceChange: (Int) -> Unit,
    onStatusSelect: (PaymentStatus) -> Unit,
    onNoteChange: (String) -> Unit,
    onDeleteLesson: () -> Unit,
    onSnackbarConsumed: () -> Unit,
) {
    if (!state.isVisible) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showStudentPicker by remember { mutableStateOf(false) }
    var showDurationDialog by remember { mutableStateOf(false) }
    var showPriceDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val locale = state.locale
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM", locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }
    val currencyFormatter = remember(locale, state.currencyCode) {
        runCatching {
            NumberFormat.getCurrencyInstance(locale).apply {
                currency = java.util.Currency.getInstance(state.currencyCode)
                maximumFractionDigits = 0
                minimumFractionDigits = 0
            }
        }.getOrElse {
            NumberFormat.getCurrencyInstance(locale).apply {
                maximumFractionDigits = 0
                minimumFractionDigits = 0
            }
        }
    }
    val priceText = remember(state.priceCents, currencyFormatter) {
        currencyFormatter.format(state.priceCents / 100)
    }
    val formattedDate = remember(state.date, locale) {
        dateFormatter.format(state.date).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
    }
    val snackbarText = when (val message = state.snackbarMessage) {
        is LessonCardMessage.Error -> message.message ?: stringResource(R.string.lesson_card_snackbar_error)
        null -> null
    }

    LaunchedEffect(snackbarText) {
        if (snackbarText != null) {
            snackbarHostState.showSnackbar(snackbarText)
            onSnackbarConsumed()
        }
    }

    val onStudentClick: () -> Unit = { showStudentPicker = true }
    val onDateClick: () -> Unit = {
        DatePickerDialog(
            context,
            { _, year, month, day -> onDateSelect(LocalDate.of(year, month + 1, day)) },
            state.date.year,
            state.date.monthValue - 1,
            state.date.dayOfMonth
        ).show()
    }
    val onTimeClick: () -> Unit = {
        TimePickerDialog(
            context,
            { _, hour, minute -> onTimeSelect(LocalTime.of(hour, minute)) },
            state.time.hour,
            state.time.minute,
            true
        ).show()
    }
    val onDurationClick: () -> Unit = { showDurationDialog = true }
    val onPriceClick: () -> Unit = { showPriceDialog = true }
    val onNoteClick: () -> Unit = { showNoteDialog = true }

    ModalBottomSheet(
        onDismissRequest = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
        },
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Unspecified,
        scrimColor = Color.Black.copy(alpha = 0.32f),
    ) {
        TutorlyBottomSheetContainer {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        if (state.isSaving) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        LessonHeader(
                            name = state.studentName,
                            grade = state.studentGrade,
                            subject = state.subjectName,
                            onClick = onStudentClick
                        )

                        DateRow(
                            dateText = formattedDate,
                            onClick = onDateClick
                        )

                        TimeDurationRow(
                            timeLabel = stringResource(id = R.string.lesson_details_time_label),
                            timeText = state.time.format(timeFormatter),
                            durationLabel = stringResource(id = R.string.lesson_details_duration_label),
                            durationText = stringResource(id = R.string.lesson_card_duration_value, state.durationMinutes),
                            onTimeClick = onTimeClick,
                            onDurationClick = onDurationClick
                        )

                        PriceRow(
                            price = priceText,
                            paymentStatus = state.paymentStatus,
                            onPriceClick = onPriceClick,
                            onStatusSelect = onStatusSelect,
                            isStatusBusy = state.isPaymentActionRunning
                        )

                        NoteRow(
                            note = state.note,
                            onClick = onNoteClick
                        )

                        if (state.lessonId != null) {
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isSaving && !state.isDeleting && !state.isLoading,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                if (state.isDeleting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text(text = stringResource(id = R.string.lesson_card_delete))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }
        }
    }

    if (showStudentPicker) {
        StudentPickerDialog(
            options = state.studentOptions,
            onSelect = {
                onStudentSelect(it)
                showStudentPicker = false
            },
            onAddStudent = {
                showStudentPicker = false
                onAddStudent()
            },
            onDismiss = { showStudentPicker = false }
        )
    }

    if (showDurationDialog) {
        DurationDialog(
            currentMinutes = state.durationMinutes,
            onDismiss = { showDurationDialog = false },
            onConfirm = {
                onDurationSelect(it)
                showDurationDialog = false
            }
        )
    }

    if (showPriceDialog) {
        PriceDialog(
            currentPriceCents = state.priceCents,
            currencySymbol = state.currencySymbol,
            onDismiss = { showPriceDialog = false },
            onConfirm = {
                onPriceChange(it)
                showPriceDialog = false
            }
        )
    }

    if (showNoteDialog) {
        NoteDialog(
            currentNote = state.note.orEmpty(),
            onDismiss = { showNoteDialog = false },
            onConfirm = {
                onNoteChange(it)
                showNoteDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        DeleteLessonDialog(
            onConfirm = {
                showDeleteDialog = false
                onDeleteLesson()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun LessonHeader(
    name: String,
    grade: String?,
    subject: String?,
    onClick: () -> Unit,
) {
    val initials = remember(name) {
        name.split(" ").filter { it.isNotBlank() }.take(2).map { it.first().uppercase() }.joinToString("")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.extendedColors.chipSelected
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials.ifBlank { "?" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name.ifBlank { stringResource(id = R.string.lesson_card_student_placeholder) },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = listOfNotNull(grade, subject).filter { it.isNotBlank() }.joinToString(" • ")
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DateRow(
    dateText: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = TutorlyCardDefaults.colors(),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = stringResource(id = R.string.lesson_card_date_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun TimeDurationRow(
    timeLabel: String,
    timeText: String,
    durationLabel: String,
    durationText: String,
    onTimeClick: () -> Unit,
    onDurationClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TimeCard(
            label = timeLabel,
            value = timeText,
            icon = Icons.Outlined.Schedule,
            onClick = onTimeClick,
            modifier = Modifier.weight(1f)
        )
        TimeCard(
            label = durationLabel,
            value = durationText,
            icon = Icons.Outlined.Timelapse,
            onClick = onDurationClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimeCard(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = TutorlyCardDefaults.colors(),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun PriceRow(
    price: String,
    paymentStatus: PaymentStatus,
    onPriceClick: () -> Unit,
    onStatusSelect: (PaymentStatus) -> Unit,
    isStatusBusy: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onPriceClick)
        ) {
            Text(
                text = stringResource(id = R.string.lesson_card_price_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = price,
                style = MaterialTheme.typography.titleLarge
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.lesson_card_payment_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val isPaid = paymentStatus == PaymentStatus.PAID
            Box(contentAlignment = Alignment.Center) {
                Switch(
                    checked = isPaid,
                    onCheckedChange = { checked ->
                        val newStatus = if (checked) PaymentStatus.PAID else PaymentStatus.DUE
                        onStatusSelect(newStatus)
                    },
                    enabled = !isStatusBusy,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onError,
                        uncheckedTrackColor = MaterialTheme.colorScheme.error,
                        uncheckedBorderColor = Color.Transparent,
                        checkedBorderColor = Color.Transparent,
                        uncheckedIconColor = MaterialTheme.colorScheme.onError,
                        checkedIconColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                if (isStatusBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteRow(
    note: String?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = TutorlyCardDefaults.colors(),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.lesson_card_note_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val content = note?.takeIf { it.isNotBlank() }
            if (content == null) {
                Text(
                    text = stringResource(id = R.string.lesson_card_note_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StudentPickerDialog(
    options: List<LessonStudentOption>,
    onSelect: (Long) -> Unit,
    onAddStudent: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.lesson_card_student_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (options.isEmpty()) {
                    Text(text = stringResource(id = R.string.lesson_card_student_picker_empty))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(options, key = { it.id }) { option ->
                            Card(
                                onClick = { onSelect(option.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = TutorlyCardDefaults.colors(),
                                elevation = TutorlyCardDefaults.elevation()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    val subtitle = listOfNotNull(option.grade, option.subject)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" • ")
                                    if (subtitle.isNotBlank()) {
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onAddStudent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.add_student))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.lesson_create_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun DurationDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var customInput by remember(currentMinutes) {
        mutableStateOf(currentMinutes.takeIf { it > 0 }?.toString().orEmpty())
    }
    val presets = listOf(45, 60, 90, 120)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.lesson_card_duration_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { value ->
                        customInput = value.filter { it.isDigit() }
                    },
                    label = { Text(text = stringResource(id = R.string.lesson_card_duration_hint)) },
                    suffix = { Text(text = stringResource(id = R.string.lesson_create_minutes_suffix)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        SuggestionChip(
                            onClick = {
                                customInput = preset.toString()
                            },
                            label = { Text(text = preset.toString()) },
                            colors = SuggestionChipDefaults.suggestionChipColors()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                customInput.toIntOrNull()?.takeIf { it > 0 }?.let(onConfirm)
            }) {
                Text(text = stringResource(id = R.string.lesson_details_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.lesson_create_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun PriceDialog(
    currentPriceCents: Int,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var input by remember(currentPriceCents) {
        mutableStateOf(currentPriceCents.takeIf { it >= 0 }?.let { (it / 100).toString() } ?: "")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.lesson_card_price_title)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { value ->
                    input = value.filter { it.isDigit() }
                },
                label = { Text(text = stringResource(id = R.string.lesson_card_price_hint, currencySymbol)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                input.toIntOrNull()?.let { onConfirm(it * 100) }
            }) {
                Text(text = stringResource(id = R.string.lesson_details_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.lesson_create_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun NoteDialog(
    currentNote: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var noteInput by remember(currentNote) { mutableStateOf(currentNote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.lesson_card_note_title)) },
        text = {
            OutlinedTextField(
                value = noteInput,
                onValueChange = { value ->
                    noteInput = value.take(LESSON_CARD_NOTE_LIMIT)
                },
                label = { Text(text = stringResource(id = R.string.lesson_card_note_hint)) },
                modifier = Modifier.height(120.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(noteInput) }) {
                Text(text = stringResource(id = R.string.lesson_details_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.lesson_create_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun DeleteLessonDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.lesson_card_delete_title)) },
        text = { Text(text = stringResource(id = R.string.lesson_card_delete_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(text = stringResource(id = R.string.lesson_card_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.lesson_create_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

