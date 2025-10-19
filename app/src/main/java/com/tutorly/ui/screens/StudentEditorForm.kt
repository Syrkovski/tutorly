package com.tutorly.ui.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.CurrencyRuble
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tutorly.R
import com.tutorly.ui.theme.extendedColors
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
    val gradeNumbers = remember { (9..11).toList() }
    val gradeOptions = gradeNumbers.map { number ->
        stringResource(id = R.string.student_editor_grade_option, number)
    }

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
                onNameChange = onNameChange,
                onSubjectChange = onSubjectChange,
                onGradeChange = onGradeChange,
                enabled = enabled,
                nameFocusRequester = nameFocusRequester,
                gradeFocusRequester = gradeFocusRequester,
                gradeOptions = gradeOptions,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectInputField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    label: @Composable (() -> Unit)?,
    placeholder: @Composable (() -> Unit)?,
    supportingText: @Composable (() -> Unit)?,
    leadingIcon: @Composable (() -> Unit)?,
    onSubmit: (() -> Unit)?,
    locale: Locale,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val parts = remember(value, locale) { parseSubjectInput(value, locale) }
    val tokens = parts.tokens
    val query = parts.query
    val interactionSource = remember { MutableInteractionSource() }
    val colors = editorFieldColors()
    val textColor = MaterialTheme.colorScheme.onSurface
    val textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = textColor))

    fun updateValue(updatedTokens: List<String>, updatedQuery: String, keepSeparator: Boolean = false) {
        val sanitizedTokens = updatedTokens.filter { it.isNotBlank() }
        val normalizedQuery = enforceCapitalized(updatedQuery, locale)
        onValueChange(
            buildSubjectInput(
                sanitizedTokens,
                normalizedQuery,
                forceSeparator = keepSeparator || (parts.hasSeparator && normalizedQuery.isEmpty() && sanitizedTokens.isNotEmpty())
            )
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicTextField(
            value = query,
            onValueChange = { raw ->
                val sanitized = enforceCapitalized(raw, locale)
                val commaIndex = sanitized.indexOf(',')
                if (commaIndex >= 0) {
                    val newToken = sanitized.substring(0, commaIndex).trim()
                    val remainder = sanitized.substring(commaIndex + 1).trimStart()
                    val merged = mergeSubjectToken(tokens, newToken, locale)
                    updateValue(merged, remainder, keepSeparator = remainder.isEmpty())
                } else {
                    updateValue(tokens, sanitized)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.extendedColors.accent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { onSubmit?.invoke() }),
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = if (tokens.isNotEmpty() || query.isNotEmpty()) " " else "",
                    innerTextField = {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tokens.forEach { token ->
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        val updatedTokens = tokens.filterNot { it.equals(token, ignoreCase = true) }
                                        updateValue(updatedTokens, query, keepSeparator = updatedTokens.isNotEmpty())
                                    },
                                    label = { Text(text = token) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = stringResource(
                                                id = R.string.student_editor_subject_remove,
                                                token
                                            )
                                        )
                                    },
                                    enabled = enabled
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 24.dp)
                                    .padding(vertical = 8.dp)
                            ) {
                                innerTextField()
                            }
                        }
                    },
                    label = label,
                    placeholder = if (tokens.isEmpty() && query.isEmpty()) placeholder else null,
                    leadingIcon = leadingIcon,
                    supportingText = supportingText,
                    trailingIcon = null,
                    singleLine = true,
                    enabled = enabled,
                    isError = false,
                    interactionSource = interactionSource,
                    colors = colors,
                    contentPadding = OutlinedTextFieldDefaults.contentPadding()
                )
            }
        )

        if (suggestions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onSuggestionSelected(suggestion) },
                        label = { Text(text = suggestion) },
                        enabled = enabled
                    )
                }
            }
        }
    }
}

private data class SubjectInputParts(
    val tokens: List<String>,
    val query: String,
    val hasSeparator: Boolean
)

private fun parseSubjectInput(raw: String, locale: Locale): SubjectInputParts {
    if (raw.isBlank()) {
        return SubjectInputParts(emptyList(), "", false)
    }
    val hasSeparator = raw.trimEnd().endsWith(',')
    val segments = raw.split(',').map { it.trim() }
    val baseTokens = if (hasSeparator) {
        segments
    } else {
        segments.dropLast(1)
    }
    val tokens = baseTokens.fold(mutableListOf<String>()) { acc, item ->
        val normalized = enforceCapitalized(item, locale)
        if (normalized.isNotEmpty() && acc.none { it.equals(normalized, ignoreCase = true) }) {
            acc.add(normalized)
        }
        acc
    }
    val query = if (hasSeparator) "" else segments.lastOrNull().orEmpty()
    return SubjectInputParts(tokens, enforceCapitalized(query, locale), hasSeparator)
}

private fun buildSubjectInput(tokens: List<String>, query: String, forceSeparator: Boolean = false): String {
    val base = tokens.filter { it.isNotBlank() }.joinToString(separator = ", ")
    val normalizedQuery = query.trim()
    return when {
        normalizedQuery.isNotEmpty() -> if (base.isEmpty()) normalizedQuery else "$base, $normalizedQuery"
        forceSeparator -> if (base.isEmpty()) "" else "$base, "
        else -> base
    }
}

private fun mergeSubjectToken(tokens: List<String>, token: String, locale: Locale): List<String> {
    val normalized = enforceCapitalized(token, locale)
    if (normalized.isBlank()) return tokens
    return if (tokens.any { it.equals(normalized, ignoreCase = true) }) {
        tokens
    } else {
        tokens + normalized
    }
}

private fun enforceCapitalized(value: String, locale: Locale): String {
    val trimmed = value.trimStart()
    if (trimmed.isEmpty()) return ""
    val first = trimmed.first().uppercase(locale)
    return first + trimmed.drop(1)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileSection(
    state: StudentEditorFormState,
    onNameChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onGradeChange: (String) -> Unit,
    enabled: Boolean,
    nameFocusRequester: FocusRequester,
    gradeFocusRequester: FocusRequester,
    gradeOptions: List<String>,
    isStandalone: Boolean,
    onSubmit: (() -> Unit)?,
) {
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val locale = remember { Locale.getDefault() }
    val subjectSuggestions = remember { stringArrayResource(id = R.array.student_editor_subject_suggestions).toList() }
    val inputParts = remember(state.subject) { parseSubjectInput(state.subject, locale) }
    val normalizedSelected = remember(inputParts.tokens, locale) {
        inputParts.tokens.associateBy { it.lowercase(locale) }
    }
    val filteredSuggestions = remember(subjectSuggestions, inputParts, locale) {
        val query = inputParts.query
        subjectSuggestions.filter { suggestion ->
            val normalized = suggestion.lowercase(locale)
            if (normalizedSelected.containsKey(normalized)) {
                false
            } else {
                query.isBlank() || suggestion.contains(query, ignoreCase = true)
            }
        }
    }
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

        SubjectInputField(
            value = state.subject,
            onValueChange = onSubjectChange,
            enabled = enabled,
            label = { Text(text = stringResource(id = R.string.student_editor_subject)) },
            placeholder = { Text(text = stringResource(id = R.string.student_editor_subject_placeholder)) },
            supportingText = { Text(text = stringResource(id = R.string.student_editor_subject_support)) },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Book, contentDescription = null, tint = iconTint)
            },
            onSubmit = { gradeFocusRequester.tryRequestFocus() },
            locale = locale,
            suggestions = filteredSuggestions,
            onSuggestionSelected = { suggestion ->
                val updated = buildSubjectInput(
                    mergeSubjectToken(inputParts.tokens, suggestion, locale),
                    query = "",
                    forceSeparator = true
                )
                onSubjectChange(updated)
            },
            modifier = Modifier.fillMaxWidth()
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

        if (gradeOptions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gradeOptions.forEach { option ->
                    val selected = state.grade.equals(option, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val value = if (selected) "" else option
                            onGradeChange(value)
                        },
                        label = { Text(text = option) },
                        enabled = enabled,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.extendedColors.accent.copy(alpha = 0.16f)
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    val rateOptions = remember { listOf(1500, 2000, 2500, 3000) }
    val textFieldColors = editorFieldColors()
    val normalizedRate = remember(rate) { rate.filter { it.isDigit() } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rateOptions.forEach { option ->
                val formatted = stringResource(id = R.string.student_editor_rate_option_value, option)
                val selected = normalizedRate == option.toString()
                FilterChip(
                    selected = selected,
                    onClick = {
                        val newValue = if (selected) "" else option.toString()
                        onRateChange(newValue)
                    },
                    label = { Text(text = formatted) },
                    enabled = enabled,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.extendedColors.accent.copy(alpha = 0.16f)
                    )
                )
            }
        }
    }
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
