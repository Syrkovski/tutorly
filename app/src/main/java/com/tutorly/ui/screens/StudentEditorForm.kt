package com.tutorly.ui.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.CurrencyRuble
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
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

private val SubjectSuggestions = listOf(
    "Математика",
    "Алгебра",
    "Геометрия",
    "Русский язык",
    "Литература",
    "Английский язык",
    "Немецкий язык",
    "Французский язык",
    "Испанский язык",
    "Китайский язык",
    "Информатика",
    "Программирование",
    "Физика",
    "Химия",
    "Биология",
    "География",
    "История",
    "Обществознание",
    "Экономика",
    "Право",
    "Музыка",
    "Изобразительное искусство",
    "Технология",
    "Черчение",
    "Физкультура",
    "ЕГЭ",
    "ОГЭ",
    "Олимпиады",
    "Подготовка к ВПР",
    "Подготовка к ДВИ",
    "Подготовка к собеседованию",
    "Подготовка к колледжу"
)

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
    modifier: Modifier = Modifier
) {
    var tokens by remember { mutableStateOf(parseSubjectInput(value, locale).tokens) }
    var query by remember { mutableStateOf(parseSubjectInput(value, locale).query) }
    var expanded by remember { mutableStateOf(false) }
    var dropdownWidth by remember { mutableStateOf(0f) }
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val textStyle = MaterialTheme.typography.bodyLarge
    val textColor = MaterialTheme.colorScheme.onSurface
    val colors = editorFieldColors()

    LaunchedEffect(value, locale) {
        val current = buildSubjectInput(tokens, query)
        if (current != value) {
            val parsed = parseSubjectInput(value, locale)
            tokens = parsed.tokens
            query = parsed.query
        }
    }

    val suggestions = remember(tokens, query) {
        resolveSubjectSuggestions(tokens, query)
    }
    val shouldShowDropdown = expanded && enabled && suggestions.isNotEmpty()

    fun emit(updatedTokens: List<String>, updatedQuery: String, collapseDropdown: Boolean = false) {
        tokens = updatedTokens
        query = updatedQuery
        val nextValue = buildSubjectInput(updatedTokens, updatedQuery)
        if (nextValue != value) {
            onValueChange(nextValue)
        }
        expanded = when {
            collapseDropdown -> false
            updatedQuery.isBlank() -> false
            else -> resolveSubjectSuggestions(updatedTokens, updatedQuery).isNotEmpty()
        }
    }

    fun commitToken(rawToken: String) {
        val trimmed = rawToken.trim()
        if (trimmed.isEmpty()) {
            emit(tokens, "", collapseDropdown = true)
            return
        }
        val mergedTokens = mergeSubjectToken(tokens, trimmed, locale)
        emit(mergedTokens, "", collapseDropdown = true)
    }

    Column(modifier = modifier) {
        Box {
            BasicTextField(
                value = query,
                onValueChange = { raw ->
                    val sanitized = enforceCapitalized(raw, locale)
                    val delimiterIndex = sanitized.indexOf(',')
                    if (delimiterIndex >= 0) {
                        val tokenPart = sanitized.substring(0, delimiterIndex)
                        val remainder = sanitized.substring(delimiterIndex + 1)
                        val mergedTokens = mergeSubjectToken(tokens, tokenPart.trim(), locale)
                        val nextQuery = enforceCapitalized(remainder.trimStart(), locale)
                        emit(mergedTokens, nextQuery)
                    } else {
                        emit(tokens, sanitized)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onGloballyPositioned { coordinates ->
                        dropdownWidth = coordinates.size.toSize().width
                    }
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            expanded = suggestions.isNotEmpty()
                        } else {
                            if (query.isNotBlank()) {
                                commitToken(query)
                            } else {
                                emit(tokens, "", collapseDropdown = true)
                            }
                        }
                    },
                enabled = enabled,
                singleLine = true,
                textStyle = textStyle.copy(color = textColor),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        if (query.isNotBlank()) {
                            commitToken(query)
                        } else {
                            emit(tokens, "", collapseDropdown = true)
                        }
                        onSubmit?.invoke()
                    }
                ),
                cursorBrush = SolidColor(MaterialTheme.extendedColors.accent),
                interactionSource = interactionSource,
                decorationBox = { innerTextField ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = query,
                        innerTextField = {
                            FlowRow(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 56.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                tokens.forEach { token ->
                                    AssistChip(
                                        onClick = {
                                            if (!enabled) return@AssistChip
                                            val remainingTokens = tokens.filterNot {
                                                it.equals(token, ignoreCase = true)
                                            }
                                            emit(remainingTokens, query)
                                            focusRequester.tryRequestFocus()
                                        },
                                        label = { Text(token) },
                                        enabled = enabled,
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 24.dp)
                                ) {
                                    if (tokens.isEmpty() && query.isEmpty()) {
                                        CompositionLocalProvider(
                                            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                                            LocalTextStyle provides textStyle
                                        ) {
                                            placeholder?.invoke()
                                        }
                                    }
                                    innerTextField()
                                }
                            }
                        },
                        label = label,
                        placeholder = null,
                        leadingIcon = leadingIcon,
                        trailingIcon = null,
                        prefix = null,
                        suffix = null,
                        supportingText = supportingText,
                        singleLine = false,
                        enabled = enabled,
                        isError = false,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = interactionSource,
                        colors = colors,
                        contentPadding = OutlinedTextFieldDefaults.contentPaddingWithLabel()
                    )
                }
            )

            val dropdownModifier = if (dropdownWidth > 0f) {
                Modifier.width(with(LocalDensity.current) { dropdownWidth.toDp() })
            } else {
                Modifier
            }

            DropdownMenu(
                expanded = shouldShowDropdown,
                onDismissRequest = { expanded = false },
                modifier = dropdownModifier
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            commitToken(suggestion)
                            focusRequester.tryRequestFocus()
                        }
                    )
                }
            }
        }
    }
}

private data class SubjectInputParts(
    val tokens: List<String>,
    val query: String
)

private fun parseSubjectInput(raw: String, locale: Locale): SubjectInputParts {
    if (raw.isBlank()) {
        return SubjectInputParts(emptyList(), "")
    }
    val tokens = parseSubjectTokens(raw, locale)
    val normalizedValue = buildSubjectInput(tokens, "")
    val trimmed = raw.trim()
    val query = if (trimmed.length > normalizedValue.length) {
        val remainder = trimmed.substring(normalizedValue.length).trimStart(',', ' ')
        enforceCapitalized(remainder, locale)
    } else {
        ""
    }
    return SubjectInputParts(tokens, query)
}

private fun parseSubjectTokens(raw: String, locale: Locale): List<String> {
    if (raw.isBlank()) return emptyList()
    return raw.split(',')
        .map { enforceCapitalized(it.trim(), locale) }
        .filter { it.isNotEmpty() }
        .fold(mutableListOf<String>()) { acc, item ->
            if (acc.none { it.equals(item, ignoreCase = true) }) {
                acc.add(item)
            }
            acc
        }
}

private fun resolveSubjectSuggestions(tokens: List<String>, query: String): List<String> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return emptyList()
    return SubjectSuggestions.filter { suggestion ->
        suggestion.startsWith(normalizedQuery, ignoreCase = true)
    }.filterNot { suggestion ->
        tokens.any { token -> token.equals(suggestion, ignoreCase = true) }
    }.take(12)
}

private fun buildSubjectInput(tokens: List<String>, query: String): String {
    val base = tokens.filter { it.isNotBlank() }.joinToString(separator = ", ")
    val normalizedQuery = query.trim()
    return when {
        normalizedQuery.isNotEmpty() -> if (base.isEmpty()) normalizedQuery else "$base, $normalizedQuery"
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

@Composable
private fun ProfileSection(
    state: StudentEditorFormState,
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
    val locale = remember { Locale.getDefault() }
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
    }
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
