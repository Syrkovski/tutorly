package com.tutorly.ui.lessoncreation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import com.tutorly.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LessonCreationSheet(
    state: LessonCreationUiState,
    onDismiss: () -> Unit,
    onStudentQueryChange: (String) -> Unit,
    onStudentSelect: (Long) -> Unit,
    onAddStudent: () -> Unit,
    onSubjectSelect: (Long?) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onTimeSelect: (LocalTime) -> Unit,
    onDurationChange: (Int) -> Unit,
    onPriceChange: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onConfirmConflict: () -> Unit,
    onDismissConflict: () -> Unit
) {
    if (!state.isVisible) return

    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val minHeight = remember(configuration) { configuration.screenHeightDp.dp * 0.6f }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SheetHeader(onDismiss)
            StudentSection(
                state = state,
                onQueryChange = onStudentQueryChange,
                onStudentSelect = onStudentSelect,
                onAddStudent = onAddStudent
            )
            SubjectSection(state = state, onSubjectSelect = onSubjectSelect)
            TimeSection(state = state, onDateSelect = onDateSelect, onTimeSelect = onTimeSelect)
            DurationPriceSection(state = state, onDurationChange = onDurationChange, onPriceChange = onPriceChange)
            NoteSection(state = state, onNoteChange = onNoteChange)
            ActionButtons(state = state, onSubmit = onSubmit)
        }
    }

    val conflict = state.showConflictDialog
    if (conflict != null) {
        ConflictDialog(conflict, onConfirm = onConfirmConflict, onDismiss = onDismissConflict)
    }
}

@Composable
private fun SheetHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.lesson_create_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = null)
        }
    }
}

@Composable
private fun StudentSection(
    state: LessonCreationUiState,
    onQueryChange: (String) -> Unit,
    onStudentSelect: (Long) -> Unit,
    onAddStudent: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(id = R.string.lesson_create_student_label), fontWeight = FontWeight.Medium)
        val selectedName = state.selectedStudent?.name ?: state.studentQuery
        var query by remember(selectedName) { mutableStateOf(selectedName) }
        var expanded by remember { mutableStateOf(false) }
        var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
        val dropdownWidth = with(LocalDensity.current) { textFieldSize.width.toDp() }
        val dropdownModifier = if (dropdownWidth > 0.dp) Modifier.width(dropdownWidth) else Modifier
        Box {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    expanded = true
                    onQueryChange(it)
                },
                placeholder = { Text(text = stringResource(id = R.string.lesson_create_student_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { textFieldSize = it.size }
                    .onFocusChanged { focusState -> expanded = focusState.isFocused },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                },
                isError = state.errors.containsKey(LessonCreationField.STUDENT)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = dropdownModifier
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.lesson_create_new_student)) },
                    onClick = {
                        expanded = false
                        onAddStudent()
                    }
                )
                state.students.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.name,
                                style = LocalTextStyle.current
                            )
                        },
                        trailingIcon = {
                            if (state.selectedStudent?.id == option.id) {
                                Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            query = option.name
                            expanded = false
                            onStudentSelect(option.id)
                        }
                    )
                }
            }
        }
        state.errors[LessonCreationField.STUDENT]?.let { message ->
            ErrorText(message)
        }
    }
}

@Composable
private fun SubjectSection(state: LessonCreationUiState, onSubjectSelect: (Long?) -> Unit) {
    if (state.subjects.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(id = R.string.lesson_create_subject_label), fontWeight = FontWeight.Medium)
        FlowSubjectChips(state = state, onSubjectSelect = onSubjectSelect)
    }
}

@Composable
private fun FlowSubjectChips(state: LessonCreationUiState, onSubjectSelect: (Long?) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.subjects.forEach { subject ->
            val selected = state.selectedSubjectId == subject.id
            FilterChip(
                onClick = { onSubjectSelect(subject.id) },
                label = { Text(text = subject.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                ),
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(subject.colorArgb), CircleShape)
                    )
                },
                selected = selected
            )
        }
    }
}

@Composable
private fun TimeSection(
    state: LessonCreationUiState,
    onDateSelect: (LocalDate) -> Unit,
    onTimeSelect: (LocalTime) -> Unit
) {
    val context = LocalContext.current
    val locale = state.locale
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onDateSelect(LocalDate.of(year, month + 1, day))
                    },
                    state.date.year,
                    state.date.monthValue - 1,
                    state.date.dayOfMonth
                ).show()
            },
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Outlined.CalendarMonth, contentDescription = null)
                Text(text = state.date.format(dateFormatter))
            }
        }
        OutlinedButton(
            onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> onTimeSelect(LocalTime.of(hour, minute)) },
                    state.time.hour,
                    state.time.minute,
                    true
                ).show()
            },
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Outlined.Schedule, contentDescription = null)
                Text(text = state.time.format(timeFormatter))
            }
        }
    }
    state.errors[LessonCreationField.TIME]?.let { ErrorText(it) }
}

@Composable
private fun DurationPriceSection(
    state: LessonCreationUiState,
    onDurationChange: (Int) -> Unit,
    onPriceChange: (Int) -> Unit
) {
    val presets = listOf(45, 60, 90, 120)
    var customDurationInput by remember(state.durationMinutes) {
        val value = state.durationMinutes.takeIf { it > 0 && it !in presets }?.toString().orEmpty()
        mutableStateOf(value)
    }
    var priceInput by remember(state.priceCents) {
        mutableStateOf(state.priceCents.takeIf { it >= 0 }?.let { (it / 100).toString() } ?: "")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(id = R.string.lesson_create_duration_label), fontWeight = FontWeight.Medium)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { minutes ->
                    val selected = state.durationMinutes == minutes
                    FilterChip(
                        selected = selected,
                        onClick = {
                            customDurationInput = ""
                            onDurationChange(minutes)
                        },
                        label = { Text(text = stringResource(id = R.string.lesson_create_duration_chip, minutes)) }
                    )
                }
            }
            OutlinedTextField(
                value = customDurationInput,
                onValueChange = { value ->
                    val digits = value.filter { it.isDigit() }
                    customDurationInput = digits
                    onDurationChange(digits.toIntOrNull() ?: 0)
                },
                label = { Text(text = stringResource(id = R.string.lesson_create_duration_custom_label)) },
                suffix = { Text(text = stringResource(id = R.string.lesson_create_minutes_suffix)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = state.errors.containsKey(LessonCreationField.DURATION)
            )
            state.errors[LessonCreationField.DURATION]?.let { ErrorText(it) }
        }

        OutlinedTextField(
            value = priceInput,
            onValueChange = { value ->
                val digits = value.filter { it.isDigit() }
                priceInput = digits
                onPriceChange((digits.toIntOrNull() ?: 0) * 100)
            },
            label = { Text(text = stringResource(id = R.string.lesson_create_price_label, state.currencySymbol)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = state.errors.containsKey(LessonCreationField.PRICE)
        )
        state.errors[LessonCreationField.PRICE]?.let { ErrorText(it) }
    }
}

@Composable
private fun NoteSection(state: LessonCreationUiState, onNoteChange: (String) -> Unit) {
    OutlinedTextField(
        value = state.note,
        onValueChange = onNoteChange,
        label = { Text(text = stringResource(id = R.string.lesson_create_note_label)) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
    )
}

@Composable
private fun ActionButtons(
    state: LessonCreationUiState,
    onSubmit: () -> Unit
) {
    androidx.compose.material3.Button(
        onClick = onSubmit,
        enabled = !state.isSubmitting && state.selectedStudent != null,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (state.isSubmitting) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.height(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(text = stringResource(id = R.string.lesson_create_submit))
        }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun ConflictDialog(conflict: ConflictInfo, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val timeFormatter = remember(conflict.start.zone) {
        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.lesson_create_conflict_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(id = R.string.lesson_create_conflict_body))
                conflict.conflicts.forEach { lesson ->
                    Text(
                        text = "${lesson.studentName} • ${lesson.start.format(timeFormatter)}–${lesson.end.format(timeFormatter)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.lesson_create_conflict_proceed))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.lesson_create_conflict_back))
            }
        }
    )
}
