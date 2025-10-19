package com.tutorly.ui.lessoncreation

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CurrencyRuble
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tutorly.R
import com.tutorly.ui.components.TutorlyBottomSheetContainer
import com.tutorly.ui.theme.extendedColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SectionSpacing = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonCreationSheet(
    state: LessonCreationUiState,
    onDismiss: () -> Unit,
    onStudentQueryChange: (String) -> Unit,
    onStudentSelect: (Long) -> Unit,
    onSubjectInputChange: (String) -> Unit,
    onSubjectSelect: (Long?) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onTimeInputChange: (String) -> Unit,
    onDurationChange: (Int) -> Unit,
    onPriceChange: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onConfirmConflict: () -> Unit,
    onDismissConflict: () -> Unit,
    onUseDuplicateStudent: () -> Unit,
    onCreateDuplicateStudent: () -> Unit,
    onDismissDuplicateStudent: () -> Unit
) {
    if (!state.isVisible) return

    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val minHeight = remember(configuration) { configuration.screenHeightDp.dp * 0.6f }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Unspecified,
        scrimColor = Color.Black.copy(alpha = 0.32f)
    ) {
        TutorlyBottomSheetContainer(dragHandle = null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(SectionSpacing)
            ) {
                SheetHeader(onDismiss)
                StudentSection(
                    state = state,
                    onQueryChange = onStudentQueryChange,
                    onStudentSelect = onStudentSelect
                )
                SubjectSection(
                    state = state,
                    onSubjectInputChange = onSubjectInputChange,
                    onSubjectSelect = onSubjectSelect
                )
                TimeSection(
                    state = state,
                    onDateSelect = onDateSelect,
                    onTimeInputChange = onTimeInputChange
                )
                DurationSection(state = state, onDurationChange = onDurationChange)
                PriceSection(state = state, onPriceChange = onPriceChange)
                NoteSection(state = state, onNoteChange = onNoteChange)
                ActionButtons(state = state, onSubmit = onSubmit)
            }
        }
    }

    val conflict = state.showConflictDialog
    if (conflict != null) {
        ConflictDialog(conflict, onConfirm = onConfirmConflict, onDismiss = onDismissConflict)
    }
    state.studentDuplicatePrompt?.let { prompt ->
        DuplicateStudentDialog(
            prompt = prompt,
            onUseExisting = onUseDuplicateStudent,
            onCreateNew = onCreateDuplicateStudent,
            onDismiss = onDismissDuplicateStudent
        )
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
    onStudentSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
        var expanded by remember { mutableStateOf(false) }
        var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
        val dropdownWidth = with(LocalDensity.current) { textFieldSize.width.toDp() }
        val dropdownModifier = if (dropdownWidth > 0.dp) Modifier.width(dropdownWidth) else Modifier
        LaunchedEffect(state.students, state.studentQuery, state.selectedStudent?.id) {
            if (state.selectedStudent != null || state.students.isEmpty() || state.studentQuery.isBlank()) {
                expanded = false
            }
        }
        Box {
            OutlinedTextField(
                value = state.studentQuery,
                onValueChange = {
                    onQueryChange(it)
                    expanded = true
                },
                label = { Text(text = stringResource(id = R.string.lesson_create_student_label)) },
                placeholder = { Text(text = stringResource(id = R.string.lesson_create_student_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { textFieldSize = it.size }
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && state.students.isNotEmpty()) {
                            expanded = true
                        }
                        if (!focusState.isFocused) {
                            expanded = false
                        }
                },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Person, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                isError = state.errors.containsKey(LessonCreationField.STUDENT),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    errorContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )
            DropdownMenu(
                expanded = expanded && state.students.isNotEmpty(),
                onDismissRequest = { expanded = false },
                modifier = dropdownModifier,
                containerColor = MaterialTheme.colorScheme.surface,
                properties = PopupProperties(focusable = false)
            ) {
                state.students.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = option.name,
                                    style = LocalTextStyle.current,
                                    maxLines = 1
                                )
                                if (option.subjects.isNotEmpty()) {
                                    Text(
                                        text = option.subjects.joinToString(separator = ", "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                            }
                        },
                        leadingIcon = {
                            StudentAvatar(name = option.name, size = 32.dp)
                        },
                        trailingIcon = {
                            if (state.selectedStudent?.id == option.id) {
                                Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            expanded = false
                            onStudentSelect(option.id)
                        }
                    )
                }
            }
        }
        if (state.selectedStudent == null && state.studentQuery.isNotBlank()) {
            Text(
                text = stringResource(id = R.string.lesson_create_student_new_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        state.errors[LessonCreationField.STUDENT]?.let { message ->
            ErrorText(message)
        }
    }
}

@Composable
private fun StudentAvatar(
    name: String,
    size: Dp = 32.dp,
) {
    val initials = remember(name) {
        name
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString(separator = "") { it.first().uppercaseChar().toString() }
            .ifEmpty { "?" }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.extendedColors.chipSelected),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectSection(
    state: LessonCreationUiState,
    onSubjectInputChange: (String) -> Unit,
    onSubjectSelect: (Long?) -> Unit
) {
    val locale = state.locale
    val availableSubjects = state.availableSubjects
    val studentSubjectNames = state.selectedStudent?.subjects.orEmpty()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    val additionalNames = studentSubjectNames.filter { name ->
        availableSubjects.none { option -> option.name.equals(name, ignoreCase = true) }
    }
    val query = state.subjectInput
    val filteredSubjects = if (query.isBlank()) {
        availableSubjects
    } else {
        availableSubjects.filter { it.name.contains(query, ignoreCase = true) }
    }
    val filteredAdditional = if (query.isBlank()) {
        additionalNames
    } else {
        additionalNames.filter { it.contains(query, ignoreCase = true) }
    }
    val popularSubjects = stringArrayResource(id = R.array.student_editor_subject_suggestions).toList()
    val normalizedExisting = remember(availableSubjects, additionalNames, locale) {
        (availableSubjects.map { it.name } + additionalNames)
            .map { it.lowercase(locale) }
            .toSet()
    }
    val filteredPopular = remember(popularSubjects, normalizedExisting, query) {
        popularSubjects.filter { subject ->
            val normalized = subject.lowercase(locale)
            val matchesQuery = query.isBlank() || subject.contains(query, ignoreCase = true)
            matchesQuery && !normalizedExisting.contains(normalized)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
        OutlinedTextField(
            value = state.subjectInput,
            onValueChange = onSubjectInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.lesson_create_subject_label)) },
            placeholder = {
                Text(text = stringResource(id = R.string.lesson_create_subject_placeholder))
            },
            singleLine = true,
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Book, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
        if (filteredSubjects.isNotEmpty() || filteredAdditional.isNotEmpty() || filteredPopular.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filteredSubjects.forEach { subject ->
                    AssistChip(
                        onClick = { onSubjectSelect(subject.id) },
                        label = { Text(text = subject.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(subject.colorArgb), CircleShape)
                            )
                        }
                    )
                }
                filteredAdditional.forEach { subjectName ->
                    AssistChip(
                        onClick = { onSubjectInputChange(subjectName) },
                        label = { Text(text = subjectName) }
                    )
                }
                filteredPopular.forEach { subjectName ->
                    SuggestionChip(
                        onClick = { onSubjectInputChange(subjectName) },
                        label = { Text(text = subjectName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeSection(
    state: LessonCreationUiState,
    onDateSelect: (LocalDate) -> Unit,
    onTimeInputChange: (String) -> Unit
) {
    val context = LocalContext.current
    val locale = state.locale
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }

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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = Icons.Outlined.CalendarMonth, contentDescription = null)
                Text(text = state.date.format(dateFormatter), textAlign = TextAlign.Center)
            }
        }
        OutlinedTextField(
            value = state.timeInput,
            onValueChange = onTimeInputChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text(text = stringResource(id = R.string.lesson_create_time_label)) },
            placeholder = { Text(text = stringResource(id = R.string.lesson_create_time_placeholder)) },
            leadingIcon = { Icon(imageVector = Icons.Filled.Schedule, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            isError = state.errors.containsKey(LessonCreationField.TIME),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
    }
    state.errors[LessonCreationField.TIME]?.let { ErrorText(it) }
}

@Composable
private fun DurationSection(
    state: LessonCreationUiState,
    onDurationChange: (Int) -> Unit
) {
    val presets = listOf(45, 60, 90, 120)
    var customDurationInput by remember(state.durationMinutes) {
        mutableStateOf(state.durationMinutes.takeIf { it > 0 }?.toString().orEmpty())
    }

    Column {
        OutlinedTextField(
            value = customDurationInput,
            onValueChange = { value ->
                val digits = value.filter { it.isDigit() }
                customDurationInput = digits
                onDurationChange(digits.toIntOrNull() ?: 0)
            },
            label = { Text(text = stringResource(id = R.string.lesson_create_duration_label)) },
            suffix = { Text(text = stringResource(id = R.string.lesson_create_minutes_suffix)) },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Schedule, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = state.errors.containsKey(LessonCreationField.DURATION),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { minutes ->
                val selected = state.durationMinutes == minutes
                FilterChip(
                    selected = selected,
                    onClick = {
                        customDurationInput = minutes.toString()
                        onDurationChange(minutes)
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.lesson_create_duration_chip, minutes),
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.extendedColors.chipSelected,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
        state.errors[LessonCreationField.DURATION]?.let { ErrorText(it) }
    }
}

@Composable
private fun PriceSection(
    state: LessonCreationUiState,
    onPriceChange: (Int) -> Unit
) {
    var priceInput by remember(state.priceCents) {
        mutableStateOf(state.priceCents.takeIf { it >= 0 }?.let { (it / 100).toString() } ?: "")
    }
    val priceFormatter = remember(state.locale) {
        NumberFormat.getIntegerInstance(state.locale).apply { maximumFractionDigits = 0 }
    }

    Column {
        OutlinedTextField(
            value = priceInput,
            onValueChange = { value ->
                val digits = value.filter { it.isDigit() }
                priceInput = digits
                onPriceChange((digits.toIntOrNull() ?: 0) * 100)
            },
            label = { Text(text = stringResource(id = R.string.lesson_create_price_label)) },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.CurrencyRuble, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = state.errors.containsKey(LessonCreationField.PRICE),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
        if (state.pricePresets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.pricePresets.take(4).forEach { preset ->
                    val formatted = priceFormatter.format(preset / 100)
                    FilterChip(
                        selected = state.priceCents == preset,
                        onClick = {
                            priceInput = (preset / 100).toString()
                            onPriceChange(preset)
                        },
                        label = {
                            Text(
                                text = "$formatted ${state.currencySymbol}",
                                textAlign = TextAlign.Center
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.extendedColors.chipSelected,
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
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
            .heightIn(min = 80.dp),
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Description, contentDescription = null)
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            errorContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            errorBorderColor = MaterialTheme.colorScheme.error
        )
    )
}

@Composable
private fun ActionButtons(
    state: LessonCreationUiState,
    onSubmit: () -> Unit
) {
    val canSubmit = !state.isSubmitting && (state.selectedStudent != null || state.studentQuery.isNotBlank())
    androidx.compose.material3.Button(
        onClick = onSubmit,
        enabled = canSubmit,
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


@Composable
private fun DuplicateStudentDialog(
    prompt: StudentDuplicatePrompt,
    onUseExisting: () -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.lesson_create_duplicate_title)) },
        text = {
            Text(
                text = stringResource(
                    id = R.string.lesson_create_duplicate_message,
                    prompt.existingStudent.name,
                    prompt.enteredName
                )
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onUseExisting) {
                    Text(text = stringResource(id = R.string.lesson_create_duplicate_use_existing))
                }
                TextButton(onClick = onCreateNew) {
                    Text(text = stringResource(id = R.string.lesson_create_duplicate_create_new))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.lesson_create_duplicate_cancel))
            }
        }
    )
}
