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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CurrencyRuble
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.stringResource
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SectionSpacing = 12.dp

private val SubjectSuggestionDefaults = listOf(
    "Математика",
    "Русский язык",
    "Английский язык",
    "Физика",
    "Химия",
    "Биология",
    "История",
    "Обществознание",
    "Литература",
    "География",
    "Информатика",
    "Алгебра",
    "Геометрия",
    "Экономика",
    "Право",
    "Немецкий язык",
    "Французский язык",
    "Испанский язык",
    "Китайский язык",
    "Итальянский язык",
    "Турецкий язык",
    "Программирование",
    "Шахматы",
    "Музыка",
    "Вокал",
    "Фортепиано",
    "Гитара",
    "Логопед",
    "Подготовка к школе",
    "Начальная школа",
    "ЕГЭ",
    "ОГЭ",
    "Олимпиады"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonCreationSheet(
    state: LessonCreationUiState,
    onDismiss: () -> Unit,
    onStudentQueryChange: (String) -> Unit,
    onStudentSelect: (Long) -> Unit,
    onAddStudent: () -> Unit,
    onSubjectInputChange: (String) -> Unit,
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
                    onStudentSelect = onStudentSelect,
                    onAddStudent = onAddStudent
                )
                TimeSection(state = state, onDateSelect = onDateSelect, onTimeSelect = onTimeSelect)
                DurationSection(state = state, onDurationChange = onDurationChange)
                SubjectSection(
                    state = state,
                    onSubjectInputChange = onSubjectInputChange,
                    onSubjectSelect = onSubjectSelect
                )
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
    Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
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
                label = { Text(text = stringResource(id = R.string.lesson_create_student_label)) },
                placeholder = { Text(text = stringResource(id = R.string.lesson_create_student_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { textFieldSize = it.size }
                    .onFocusChanged { focusState -> expanded = focusState.isFocused },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Person, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                },
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
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = dropdownModifier,
                containerColor = MaterialTheme.colorScheme.surface
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

@Composable
private fun SubjectSection(
    state: LessonCreationUiState,
    onSubjectInputChange: (String) -> Unit,
    onSubjectSelect: (Long?) -> Unit
) {
    val availableSubjects = state.availableSubjects
    val studentSubjectNames = state.selectedStudent?.subjects.orEmpty()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    val additionalNames = studentSubjectNames.filter { name ->
        availableSubjects.none { option -> option.name.equals(name, ignoreCase = true) }
    }
    val locale = state.locale
    val query = state.subjectInput
    val normalizedQuery = query.trim().lowercase(locale)
    val hasQuery = normalizedQuery.isNotEmpty()
    val matchingSubjects = availableSubjects.filter { option ->
        hasQuery && option.name.lowercase(locale).startsWith(normalizedQuery)
    }
    val matchingAdditional = additionalNames.filter { name ->
        hasQuery && name.lowercase(locale).startsWith(normalizedQuery)
    }
    val defaultSuggestions = SubjectSuggestionDefaults.filter { name ->
        hasQuery && name.lowercase(locale).startsWith(normalizedQuery)
    }.filterNot { suggestion ->
        matchingSubjects.any { it.name.equals(suggestion, ignoreCase = true) } ||
            matchingAdditional.any { it.equals(suggestion, ignoreCase = true) }
    }
    val hasSuggestions = matchingSubjects.isNotEmpty() || matchingAdditional.isNotEmpty() || defaultSuggestions.isNotEmpty()
    var expanded by remember(state.selectedStudent, availableSubjects, additionalNames) { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    val dropdownWidth = with(LocalDensity.current) { textFieldSize.width.toDp() }
    val dropdownModifier = if (dropdownWidth > 0.dp) Modifier.width(dropdownWidth) else Modifier

    Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
        Box {
            OutlinedTextField(
                value = state.subjectInput,
                onValueChange = {
                    onSubjectInputChange(it)
                    expanded = it.trim().isNotEmpty()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { textFieldSize = it.size }
                    .onFocusChanged { focusState ->
                        expanded = focusState.isFocused && hasSuggestions
                    },
                label = { Text(text = stringResource(id = R.string.lesson_create_subject_label)) },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Book, contentDescription = null)
                },
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
                expanded = expanded && hasSuggestions,
                onDismissRequest = { expanded = false },
                modifier = dropdownModifier,
                containerColor = MaterialTheme.colorScheme.surface,
                properties = PopupProperties(focusable = false)
            ) {
                matchingSubjects.forEach { subject ->
                    DropdownMenuItem(
                        text = { Text(text = subject.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(subject.colorArgb), CircleShape)
                            )
                        },
                        trailingIcon = {
                            if (state.selectedSubjectId == subject.id) {
                                Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            expanded = false
                            onSubjectSelect(subject.id)
                        }
                    )
                }
                matchingAdditional.forEach { subjectName ->
                    DropdownMenuItem(
                        text = { Text(text = subjectName) },
                        onClick = {
                            expanded = false
                            onSubjectInputChange(subjectName)
                        }
                    )
                }
                defaultSuggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(text = suggestion) },
                        onClick = {
                            expanded = false
                            onSubjectInputChange(suggestion)
                        }
                    )
                }
            }
        }
    }

    LaunchedEffect(hasSuggestions) {
        if (!hasSuggestions) {
            expanded = false
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = Icons.Outlined.CalendarMonth, contentDescription = null)
                Text(text = state.date.format(dateFormatter), textAlign = TextAlign.Center)
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = Icons.Outlined.Schedule, contentDescription = null)
                Text(text = state.time.format(timeFormatter), textAlign = TextAlign.Center)
            }
        }
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

    Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { minutes ->
                val selected = state.durationMinutes == minutes
                Box(modifier = Modifier.weight(1f)) {
                    FilterChip(
                        selected = selected,
                        onClick = {
                            customDurationInput = minutes.toString()
                            onDurationChange(minutes)
                        },
                        label = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.lesson_create_duration_chip, minutes),
                                    textAlign = TextAlign.Center
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.extendedColors.chipSelected,
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
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

    Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.pricePresets.take(4).forEach { preset ->
                    val formatted = priceFormatter.format(preset / 100)
                    Box(modifier = Modifier.weight(1f)) {
                        FilterChip(
                            selected = state.priceCents == preset,
                            onClick = {
                                priceInput = (preset / 100).toString()
                                onPriceChange(preset)
                            },
                            label = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$formatted ${state.currencySymbol}",
                                        textAlign = TextAlign.Center
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.extendedColors.chipSelected,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
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
