package com.tutorly.ui.screens

import androidx.compose.foundation.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CurrencyRuble
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tutorly.R
import com.tutorly.models.SubjectPreset
import com.tutorly.ui.subject.SubjectSuggestionDefaults
import com.tutorly.ui.theme.extendedColors
import java.util.LinkedHashSet
import java.util.Locale

@Composable
private fun editorFieldColors(): TextFieldColors {
    val colorScheme = MaterialTheme.colorScheme
    val accent = MaterialTheme.extendedColors.accent
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = colorScheme.surface,
        unfocusedContainerColor = colorScheme.surface,
        disabledContainerColor = colorScheme.surface,
        errorContainerColor = colorScheme.surface,
        focusedBorderColor = accent,
        unfocusedBorderColor = accent.copy(alpha = 0.4f),
        disabledBorderColor = accent.copy(alpha = 0.24f),
        errorBorderColor = colorScheme.error
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentEditorForm(
    state: StudentEditorFormState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onMessengerChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    subjectPresets: List<SubjectPreset> = emptyList(),
    onSubjectChange: (String) -> Unit,
    onGradeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    editTarget: StudentEditTarget? = null,
    initialFocus: StudentEditTarget? = null,
    enableScrolling: Boolean = true,
    enabled: Boolean = true,
    onSubmit: (() -> Unit)? = null,
) {
    val nameFocusRequester = remember { FocusRequester() }
    val gradeFocusRequester = remember { FocusRequester() }
    val rateFocusRequester = remember { FocusRequester() }
    val phoneFocusRequester = remember { FocusRequester() }
    val messengerFocusRequester = remember { FocusRequester() }
    val noteFocusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    LaunchedEffect(initialFocus, enabled) {
        if (enabled) {
            when (initialFocus) {
                StudentEditTarget.PROFILE -> nameFocusRequester.safeRequestFocus()
                StudentEditTarget.RATE -> rateFocusRequester.safeRequestFocus()
                StudentEditTarget.PHONE -> phoneFocusRequester.safeRequestFocus()
                StudentEditTarget.MESSENGER -> messengerFocusRequester.safeRequestFocus()
                StudentEditTarget.NOTES -> noteFocusRequester.safeRequestFocus()
                null -> Unit
            }
        }
    }

    val showFullForm = editTarget == null
    val isNewStudent = state.studentId == null
    var showAdditionalData by rememberSaveable(state.studentId) {
        mutableStateOf(
            !isNewStudent ||
                state.phone.isNotBlank() ||
                state.messenger.isNotBlank() ||
                state.note.isNotBlank()
        )
    }

    LaunchedEffect(editTarget) {
        if (editTarget == StudentEditTarget.PHONE ||
            editTarget == StudentEditTarget.MESSENGER ||
            editTarget == StudentEditTarget.NOTES
        ) {
            showAdditionalData = true
        }
    }

    val columnModifier = if (enableScrolling) {
        modifier.verticalScroll(scrollState)
    } else {
        modifier
    }

    val textFieldColors = editorFieldColors()

    Column(
        modifier = columnModifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showFullForm || editTarget == StudentEditTarget.PROFILE) {
            ProfileSection(
                state = state,
                subjectPresets = subjectPresets,
                onNameChange = onNameChange,
                onSubjectChange = onSubjectChange,
                onGradeChange = onGradeChange,
                enabled = enabled,
                nameFocusRequester = nameFocusRequester,
                gradeFocusRequester = gradeFocusRequester,
                isStandalone = !showFullForm && editTarget == StudentEditTarget.PROFILE,
                onSubmit = onSubmit
            )
        }

        if (showFullForm || editTarget == StudentEditTarget.RATE) {
            RateSection(
                rate = state.rate,
                onRateChange = onRateChange,
                enabled = enabled,
                focusRequester = rateFocusRequester,
                isStandalone = !showFullForm && editTarget == StudentEditTarget.RATE,
                onSubmit = onSubmit
            )
        }

        val shouldShowAdditionalSections = !isNewStudent || showAdditionalData

        if (showFullForm && isNewStudent) {
            AdditionalDataToggle(
                expanded = showAdditionalData,
                onToggle = { showAdditionalData = !showAdditionalData },
                enabled = enabled,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        if ((showFullForm && shouldShowAdditionalSections) || editTarget == StudentEditTarget.PHONE) {
            PhoneSection(
                phone = state.phone,
                onPhoneChange = onPhoneChange,
                enabled = enabled,
                focusRequester = phoneFocusRequester,
                isStandalone = !showFullForm && editTarget == StudentEditTarget.PHONE,
                onSubmit = onSubmit
            )
        }

        if ((showFullForm && shouldShowAdditionalSections) || editTarget == StudentEditTarget.MESSENGER) {
            MessengerSection(
                messenger = state.messenger,
                onMessengerChange = onMessengerChange,
                enabled = enabled,
                focusRequester = messengerFocusRequester,
                isStandalone = !showFullForm && editTarget == StudentEditTarget.MESSENGER,
                onSubmit = onSubmit
            )
        }

        if ((showFullForm && shouldShowAdditionalSections) || editTarget == StudentEditTarget.NOTES) {
            NotesSection(
                note = state.note,
                onNoteChange = onNoteChange,
                enabled = enabled,
                focusRequester = noteFocusRequester,
                onSubmit = onSubmit
            )
        }

    }
}

@Composable
private fun ProfileSection(
    state: StudentEditorFormState,
    subjectPresets: List<SubjectPreset>,
    onNameChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onGradeChange: (String) -> Unit,
    enabled: Boolean,
    nameFocusRequester: FocusRequester,
    gradeFocusRequester: FocusRequester,
    isStandalone: Boolean,
    onSubmit: (() -> Unit)?,
) {
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val textFieldColors = editorFieldColors()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text(text = stringResource(id = R.string.student_editor_name)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocusRequester),
            singleLine = true,
            enabled = enabled,
            isError = state.nameError,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = iconTint
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            colors = textFieldColors
        )
        if (state.nameError) {
            Text(
                text = stringResource(id = R.string.student_editor_name_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        SubjectSelector(
            studentId = state.studentId,
            subjectValue = state.subject,
            subjectPresets = subjectPresets,
            onSubjectChange = onSubjectChange,
            enabled = enabled,
            isStandalone = isStandalone,
            onSubmit = onSubmit
        )

        OutlinedTextField(
            value = state.grade,
            onValueChange = onGradeChange,
            label = { Text(text = stringResource(id = R.string.student_editor_grade)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(gradeFocusRequester),
            singleLine = true,
            enabled = enabled,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = iconTint
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = if (isStandalone) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = if (isStandalone) {
                KeyboardActions(onDone = { onSubmit?.invoke() })
            } else {
                KeyboardActions.Default
            },
            colors = textFieldColors
        )
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectSelector(
    studentId: Long?,
    subjectValue: String,
    subjectPresets: List<SubjectPreset>,
    onSubjectChange: (String) -> Unit,
    enabled: Boolean,
    isStandalone: Boolean,
    onSubmit: (() -> Unit)?,
) {
    val locale = remember { Locale.getDefault() }
    var subjectInput by remember { mutableStateOf("") }
    var selectedChips by remember { mutableStateOf<List<StudentSubjectChip>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    val dropdownWidth = with(LocalDensity.current) { textFieldSize.width.toDp() }
    val dropdownModifier = if (dropdownWidth > 0.dp) Modifier.width(dropdownWidth) else Modifier
    val interactionSource = remember { MutableInteractionSource() }
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.surface,
        errorContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        errorBorderColor = MaterialTheme.colorScheme.error
    )
    val presetLookup = remember(subjectPresets, locale) {
        subjectPresets.associateBy { it.name.lowercase(locale) }
    }

    LaunchedEffect(studentId) { subjectInput = "" }

    LaunchedEffect(subjectValue, subjectPresets) {
        val tokens = subjectValue.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val chips = tokens.map { token ->
            val preset = presetLookup[token.lowercase(locale)]
            if (preset != null) {
                preset.toChip()
            } else {
                StudentSubjectChip(id = null, name = titleCaseWords(token), colorArgb = null)
            }
        }
        selectedChips = chips
        subjectInput = ""
    }

    val trimmedQuery = subjectInput.trim()
    val normalizedQuery = trimmedQuery.lowercase(locale)
    val hasQuery = normalizedQuery.isNotEmpty()
    val matchingPresets = if (hasQuery) {
        subjectPresets.filter { option ->
            option.name.lowercase(locale).startsWith(normalizedQuery)
        }
    } else {
        emptyList()
    }
    val matchingDefaults = if (hasQuery) {
        SubjectSuggestionDefaults.filter { suggestion ->
            val normalized = suggestion.lowercase(locale)
            normalized.startsWith(normalizedQuery) &&
                subjectPresets.none { it.name.equals(suggestion, ignoreCase = true) }
        }
    } else {
        emptyList()
    }
    val hasSuggestions = matchingPresets.isNotEmpty() || matchingDefaults.isNotEmpty()

    fun updateChips(updated: List<StudentSubjectChip>) {
        selectedChips = updated
        onSubjectChange(buildSubjectValue(updated))
    }

    fun addPreset(option: SubjectPreset) {
        if (selectedChips.any { it.id == option.id }) {
            subjectInput = ""
            expanded = false
            return
        }
        updateChips(selectedChips + option.toChip())
        subjectInput = ""
        expanded = false
    }

    fun toggleSuggestion(name: String) {
        val normalized = name.trim()
        if (normalized.isEmpty()) {
            subjectInput = ""
            expanded = false
            return
        }
        val existingIndex = selectedChips.indexOfFirst { chip ->
            chip.id == null && chip.name.equals(normalized, ignoreCase = true)
        }
        val updated = if (existingIndex >= 0) {
            selectedChips.toMutableList().also { it.removeAt(existingIndex) }
        } else {
            val displayName = titleCaseWords(normalized)
            selectedChips + StudentSubjectChip(id = null, name = displayName, colorArgb = null)
        }
        updateChips(updated)
        subjectInput = ""
        expanded = false
    }

    fun removeChip(chip: StudentSubjectChip) {
        val updated = if (chip.id != null) {
            selectedChips.filterNot { it.id == chip.id }
        } else {
            selectedChips.filterNot {
                it.id == null && it.name.equals(chip.name, ignoreCase = true)
            }
        }
        updateChips(updated)
    }

    fun commitInput(): Boolean {
        if (trimmedQuery.isEmpty()) {
            return false
        }
        if (selectedChips.any { it.name.equals(trimmedQuery, ignoreCase = true) }) {
            subjectInput = ""
            expanded = false
            return true
        }
        val displayName = titleCaseWords(trimmedQuery)
        updateChips(
            selectedChips + StudentSubjectChip(id = null, name = displayName, colorArgb = null)
        )
        subjectInput = ""
        expanded = false
        return true
    }

    LaunchedEffect(hasSuggestions) {
        if (!hasSuggestions) {
            expanded = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            BasicTextField(
                value = subjectInput,
                onValueChange = {
                    subjectInput = it
                    expanded = enabled && it.trim().isNotEmpty()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { textFieldSize = it.size }
                    .onFocusChanged { focusState ->
                        expanded = enabled && focusState.isFocused && hasSuggestions
                    },
                singleLine = true,
                enabled = enabled,
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = if (isStandalone) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = if (isStandalone) {
                    KeyboardActions(onDone = {
                        val consumed = commitInput()
                        if (!consumed) {
                            defaultKeyboardAction(ImeAction.Done)
                        }
                        onSubmit?.invoke()
                    })
                } else {
                    KeyboardActions(onNext = {
                        val consumed = commitInput()
                        if (!consumed) {
                            defaultKeyboardAction(ImeAction.Next)
                        }
                    })
                },
                decorationBox = { innerTextField ->
                    val labelValue = remember(subjectInput, selectedChips) {
                        when {
                            subjectInput.isNotEmpty() -> subjectInput
                            selectedChips.isNotEmpty() -> " "
                            else -> ""
                        }
                    }
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = labelValue,
                        visualTransformation = VisualTransformation.None,
                        innerTextField = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedChips.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        selectedChips.forEach { chip ->
                                            FilterChip(
                                                selected = true,
                                                onClick = {
                                                    if (enabled) {
                                                        removeChip(chip)
                                                    }
                                                },
                                                label = { Text(text = chip.name) },
                                                leadingIcon = {
                                                    chip.colorArgb?.let { color ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(12.dp)
                                                                .background(Color(color), CircleShape)
                                                        )
                                                    }
                                                },
                                                trailingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Filled.Close,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.extendedColors.chipSelected,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                                ),
                                                enabled = enabled
                                            )
                                        }
                                    }
                                }
                                Box(modifier = Modifier.weight(1f, fill = true)) {
                                    innerTextField()
                                }
                            }
                        },
                        label = { Text(text = stringResource(id = R.string.student_editor_subject)) },
                        placeholder = null,
                        leadingIcon = { Icon(imageVector = Icons.Filled.Book, contentDescription = null) },
                        trailingIcon = null,
                        supportingText = null,
                        singleLine = true,
                        enabled = enabled,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = textFieldColors,
                        contentPadding = OutlinedTextFieldDefaults.contentPadding()
                    )
                }
            )

            DropdownMenu(
                expanded = expanded && hasSuggestions,
                onDismissRequest = { expanded = false },
                modifier = dropdownModifier,
                containerColor = MaterialTheme.colorScheme.surface,
                properties = PopupProperties(focusable = false)
            ) {
                matchingPresets.forEach { option ->
                    val isSelected = selectedChips.any { it.id == option.id }
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color(option.colorArgb), CircleShape)
                                )
                                Text(text = option.name)
                            }
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        onClick = { addPreset(option) }
                    )
                }
                matchingDefaults.forEach { suggestion ->
                    val isSelected = selectedChips.any {
                        it.id == null && it.name.equals(suggestion, ignoreCase = true)
                    }
                    DropdownMenuItem(
                        text = { Text(text = suggestion) },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        onClick = { toggleSuggestion(suggestion) }
                    )
                }
            }
        }
    }
}

private data class StudentSubjectChip(
    val id: Long?,
    val name: String,
    val colorArgb: Int?
)

private fun SubjectPreset.toChip(): StudentSubjectChip =
    StudentSubjectChip(id = id, name = name, colorArgb = colorArgb)

private fun buildSubjectValue(chips: List<StudentSubjectChip>): String {
    val seen = LinkedHashSet<String>()
    val ordered = mutableListOf<String>()
    chips.forEach { chip ->
        val trimmed = chip.name.trim()
        if (trimmed.isNotEmpty()) {
            val normalized = trimmed.lowercase(Locale.getDefault())
            if (seen.add(normalized)) {
                ordered.add(titleCaseWords(trimmed))
            }
        }
    }
    return ordered.joinToString(separator = ", ")
}

@Composable
private fun RateSection(
    rate: String,
    onRateChange: (String) -> Unit,
    enabled: Boolean,
    focusRequester: FocusRequester,
    isStandalone: Boolean,
    onSubmit: (() -> Unit)?,
) {
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val textFieldColors = editorFieldColors()

    OutlinedTextField(
        value = rate,
        onValueChange = onRateChange,
        label = { Text(text = stringResource(id = R.string.student_editor_rate)) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        enabled = enabled,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.CurrencyRuble,
                contentDescription = null,
                tint = iconTint
            )
        },
        supportingText = {
            Text(text = stringResource(id = R.string.student_editor_rate_support))
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Decimal,
            imeAction = if (isStandalone) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = if (isStandalone) {
            KeyboardActions(onDone = { onSubmit?.invoke() })
        } else {
            KeyboardActions.Default
        },
        colors = textFieldColors
    )
}

@Composable
private fun AdditionalDataToggle(
    expanded: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(id = R.string.student_editor_additional_data_title)
    val description = stringResource(
        id = if (expanded) {
            R.string.student_editor_additional_data_hide
        } else {
            R.string.student_editor_additional_data_show
        }
    )
    TextButton(
        onClick = onToggle,
        modifier = modifier,
        enabled = enabled
    ) {
        Text(text = title)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
            contentDescription = description
        )
    }
}

@Composable
private fun PhoneSection(
    phone: String,
    onPhoneChange: (String) -> Unit,
    enabled: Boolean,
    focusRequester: FocusRequester,
    isStandalone: Boolean,
    onSubmit: (() -> Unit)?,
) {
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val textFieldColors = editorFieldColors()
    OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text(text = stringResource(id = R.string.student_editor_phone)) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        enabled = enabled,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Phone,
                contentDescription = null,
                tint = iconTint
            )
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = if (isStandalone) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = if (isStandalone) {
            KeyboardActions(onDone = { onSubmit?.invoke() })
        } else {
            KeyboardActions.Default
        },
        colors = textFieldColors
    )
}

@Composable
private fun MessengerSection(
    messenger: String,
    onMessengerChange: (String) -> Unit,
    enabled: Boolean,
    focusRequester: FocusRequester,
    isStandalone: Boolean,
    onSubmit: (() -> Unit)?,
) {
    val messengerOptions = remember { StudentMessengerType.values().toList() }
    var selectedType by remember { mutableStateOf(StudentMessengerType.TELEGRAM) }
    var customLabel by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") }
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val textFieldColors = editorFieldColors()

    LaunchedEffect(messenger) {
        val parsed = messenger.parseMessengerValue()
        selectedType = parsed.type
        customLabel = parsed.customLabel
        identifier = parsed.identifier
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        var isDropdownExpanded by remember { mutableStateOf(false) }
        Box {
            OutlinedTextField(
                value = if (selectedType == StudentMessengerType.OTHER) customLabel else stringResource(id = selectedType.labelRes),
                onValueChange = {
                    if (selectedType == StudentMessengerType.OTHER) {
                        customLabel = it
                        onMessengerChange(buildMessengerValue(selectedType, customLabel, identifier))
                    }
                },
                label = { Text(text = stringResource(id = R.string.student_editor_messenger_type)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (enabled && focusState.isFocused) {
                            isDropdownExpanded = true
                        }
                    },
                enabled = enabled,
                readOnly = selectedType != StudentMessengerType.OTHER,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Message,
                        contentDescription = null,
                        tint = iconTint
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }, enabled = enabled) {
                        Icon(
                            imageVector = if (isDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = iconTint
                        )
                    }
                },
                colors = textFieldColors
            )

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                messengerOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = option.labelRes)) },
                        onClick = {
                            isDropdownExpanded = false
                            selectedType = option
                            if (option != StudentMessengerType.OTHER) {
                                customLabel = ""
                            }
                            onMessengerChange(buildMessengerValue(selectedType, customLabel, identifier))
                        },
                        enabled = enabled
                    )
                }
            }
        }

        OutlinedTextField(
            value = identifier,
            onValueChange = {
                identifier = it
                onMessengerChange(buildMessengerValue(selectedType, customLabel, identifier))
            },
            label = { Text(text = stringResource(id = R.string.student_editor_messenger_id)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            enabled = enabled,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.AlternateEmail,
                    contentDescription = null,
                    tint = iconTint
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = if (isStandalone) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = if (isStandalone) {
                KeyboardActions(onDone = { onSubmit?.invoke() })
            } else {
                KeyboardActions.Default
            },
            colors = textFieldColors
        )
    }
}

@Composable
private fun NotesSection(
    note: String,
    onNoteChange: (String) -> Unit,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onSubmit: (() -> Unit)?
) {
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val textFieldColors = editorFieldColors()
    OutlinedTextField(
        value = note,
        onValueChange = onNoteChange,
        label = { Text(text = stringResource(id = R.string.student_editor_notes)) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        minLines = 3,
        enabled = enabled,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                tint = iconTint
            )
        },
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onSubmit?.invoke()
        }),
        colors = textFieldColors
    )
}

private suspend fun FocusRequester.safeRequestFocus() {
    repeat(5) {
        if (tryRequestFocus()) {
            return
        }
        // When the dialog is first shown the focus target might not yet be attached.
        // Wait for the next frame so Compose has a chance to attach the node before retrying.
        withFrameNanos { }
    }
    tryRequestFocus()
}

private fun FocusRequester.tryRequestFocus(): Boolean =
    runCatching { requestFocus() }.isSuccess

private data class MessengerValue(
    val type: StudentMessengerType,
    val customLabel: String,
    val identifier: String
)

private fun String.parseMessengerValue(): MessengerValue {
    val raw = trim()
    if (raw.isEmpty()) {
        return MessengerValue(StudentMessengerType.TELEGRAM, "", "")
    }

    val delimiterIndex = raw.indexOf(':')
    if (delimiterIndex == -1) {
        return MessengerValue(StudentMessengerType.OTHER, "", raw)
    }

    val label = raw.substring(0, delimiterIndex).trim()
    val value = raw.substring(delimiterIndex + 1).trim()
    val type = StudentMessengerType.fromLabel(label)
    return if (type == StudentMessengerType.OTHER) {
        MessengerValue(type, label, value)
    } else {
        MessengerValue(type, "", value)
    }
}

private fun buildMessengerValue(
    type: StudentMessengerType,
    customLabel: String,
    identifier: String
): String {
    val trimmedIdentifier = identifier.trim()
    if (trimmedIdentifier.isEmpty()) {
        return ""
    }

    val label = when (type) {
        StudentMessengerType.OTHER -> customLabel.trim()
        else -> type.label
    }

    return if (label.isNotEmpty()) {
        "$label: $trimmedIdentifier"
    } else {
        trimmedIdentifier
    }
}



private enum class StudentMessengerType(
    val label: String,
    val labelRes: Int,
) {
    TELEGRAM(label = "Telegram", labelRes = R.string.student_editor_messenger_type_telegram),
    WHATSAPP(label = "WhatsApp", labelRes = R.string.student_editor_messenger_type_whatsapp),
    VIBER(label = "Viber", labelRes = R.string.student_editor_messenger_type_viber),
    VK(label = "VK", labelRes = R.string.student_editor_messenger_type_vk),
    OTHER(label = "", labelRes = R.string.student_editor_messenger_type_other);

    companion object {
        fun fromLabel(label: String): StudentMessengerType {
            val normalized = label.lowercase(Locale.getDefault())
            return values().firstOrNull { option ->
                option.label.lowercase(Locale.getDefault()) == normalized && option != OTHER
            } ?: OTHER
        }
    }
}
