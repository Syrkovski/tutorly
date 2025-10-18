package com.tutorly.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.CurrencyRuble
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults.textFieldColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
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
    var isGradeDropdownExpanded by remember { mutableStateOf(false) }
    val gradeOtherOption = stringResource(id = R.string.student_editor_grade_other)
    val gradeNumbers = remember { (9..11).toList() }
    val gradeOptions = gradeNumbers.map { number ->
        stringResource(id = R.string.student_editor_grade_option, number)
    } + gradeOtherOption

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
                isGradeDropdownExpanded = isGradeDropdownExpanded,
                onGradeDropdownExpandedChange = { isGradeDropdownExpanded = it },
                gradeOptions = gradeOptions,
                gradeOtherOption = gradeOtherOption,
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
    onNameChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onGradeChange: (String) -> Unit,
    enabled: Boolean,
    nameFocusRequester: FocusRequester,
    gradeFocusRequester: FocusRequester,
    isGradeDropdownExpanded: Boolean,
    onGradeDropdownExpandedChange: (Boolean) -> Unit,
    gradeOptions: List<String>,
    gradeOtherOption: String,
    isStandalone: Boolean,
    onSubmit: (() -> Unit)?,
) {
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val popularSubjects = listOf(
        stringResource(id = R.string.student_editor_subject_math),
        stringResource(id = R.string.student_editor_subject_russian),
        stringResource(id = R.string.student_editor_subject_english),
        stringResource(id = R.string.student_editor_subject_physics),
        stringResource(id = R.string.student_editor_subject_chemistry),
        stringResource(id = R.string.student_editor_subject_it)
    )
    var isSubjectDropdownExpanded by remember { mutableStateOf(false) }
    var selectedSubjects by remember { mutableStateOf<List<String>>(emptyList()) }
    var customSubjects by remember { mutableStateOf("") }
    var isOtherSelected by remember { mutableStateOf(false) }
    val customSubjectFocusRequester = remember { FocusRequester() }
    val textFieldColors = editorFieldColors()

    LaunchedEffect(state.subject) {
        val normalizedOptions = popularSubjects.associateBy { it.lowercase(Locale.getDefault()) }
        val tokens = state.subject.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val preset = mutableListOf<String>()
        val others = mutableListOf<String>()
        tokens.forEach { token ->
            val match = normalizedOptions[token.lowercase(Locale.getDefault())]
            if (match != null) {
                if (!preset.contains(match)) {
                    preset.add(match)
                }
            } else {
                others.add(token)
            }
        }
        selectedSubjects = popularSubjects.filter { preset.contains(it) }
        val othersValue = others.joinToString(", ")
        customSubjects = othersValue
        isOtherSelected = othersValue.isNotBlank()
    }

    LaunchedEffect(isOtherSelected, enabled) {
        if (enabled && isOtherSelected) {
            customSubjectFocusRequester.safeRequestFocus()
        }
    }

    val displayedSubjects = remember(selectedSubjects, customSubjects) {
        buildSubjectValue(selectedSubjects, customSubjects)
    }
    var subjectFieldSize by remember { mutableStateOf(IntSize.Zero) }
    val subjectDropdownWidth = with(LocalDensity.current) { subjectFieldSize.width.toDp() }
    val subjectDropdownModifier = if (subjectDropdownWidth > 0.dp) Modifier.width(subjectDropdownWidth) else Modifier
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

        Box {
            OutlinedTextField(
                value = displayedSubjects,
                onValueChange = {},
                label = { Text(text = stringResource(id = R.string.student_editor_subject)) },
                placeholder = { Text(text = stringResource(id = R.string.student_editor_subject_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { subjectFieldSize = it.size }
                    .onFocusChanged { focusState ->
                        if (enabled && focusState.isFocused) {
                            isSubjectDropdownExpanded = true
                        }
                    },
                singleLine = true,
                enabled = enabled,
                readOnly = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Book,
                        contentDescription = null,
                        tint = iconTint
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { isSubjectDropdownExpanded = !isSubjectDropdownExpanded },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = if (isSubjectDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = iconTint
                        )
                    }
                },
                supportingText = {
                    Text(text = stringResource(id = R.string.student_editor_subject_support))
                },
                colors = textFieldColors
            )

            DropdownMenu(
                expanded = isSubjectDropdownExpanded,
                onDismissRequest = { isSubjectDropdownExpanded = false },
                modifier = subjectDropdownModifier,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                popularSubjects.forEach { option ->
                    val isSelected = selectedSubjects.contains(option)
                    DropdownMenuItem(
                        text = { Text(text = option) },
                        onClick = {
                            val updated = if (isSelected) {
                                selectedSubjects.filterNot { it == option }
                            } else {
                                val current = selectedSubjects.toMutableList()
                                if (!current.contains(option)) {
                                    current.add(option)
                                }
                                current
                            }
                            val ordered = popularSubjects.filter { updated.contains(it) }
                            selectedSubjects = ordered
                            onSubjectChange(buildSubjectValue(ordered, customSubjects))
                        },
                        leadingIcon = {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                enabled = enabled
                            )
                        },
                        enabled = enabled
                    )
                }
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.student_editor_subject_other_option)) },
                    onClick = {
                        val newValue = !isOtherSelected
                        isOtherSelected = newValue
                        if (!newValue) {
                            customSubjects = ""
                            onSubjectChange(buildSubjectValue(selectedSubjects, ""))
                        }
                    },
                    leadingIcon = {
                        Checkbox(
                            checked = isOtherSelected,
                            onCheckedChange = null,
                            enabled = enabled
                        )
                    },
                    enabled = enabled
                )
            }
        }

        if (isOtherSelected) {
            OutlinedTextField(
                value = customSubjects,
                onValueChange = {
                    customSubjects = it
                    onSubjectChange(buildSubjectValue(selectedSubjects, it))
                },
                label = { Text(text = stringResource(id = R.string.student_editor_subject_custom_label)) },
                placeholder = { Text(text = stringResource(id = R.string.student_editor_subject_custom_placeholder)) },
                supportingText = {
                    Text(text = stringResource(id = R.string.student_editor_subject_custom_support))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(customSubjectFocusRequester),
                enabled = enabled,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = iconTint
                    )
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = if (isStandalone) ImeAction.Done else ImeAction.Next),
                keyboardActions = if (isStandalone) {
                    KeyboardActions(onDone = { onSubmit?.invoke() })
                } else {
                    KeyboardActions.Default
                },
                colors = textFieldColors
            )
        }

        var gradeFieldSize by remember { mutableStateOf(IntSize.Zero) }
        val gradeDropdownWidth = with(LocalDensity.current) { gradeFieldSize.width.toDp() }
        val gradeDropdownModifier = if (gradeDropdownWidth > 0.dp) Modifier.width(gradeDropdownWidth) else Modifier
        Box {
            OutlinedTextField(
                value = state.grade,
                onValueChange = onGradeChange,
                label = { Text(text = stringResource(id = R.string.student_editor_grade)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { gradeFieldSize = it.size }
                    .focusRequester(gradeFocusRequester)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            onGradeDropdownExpandedChange(false)
                        } else if (enabled) {
                            onGradeDropdownExpandedChange(true)
                        }
                    },
                singleLine = true,
                enabled = enabled,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        tint = iconTint
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { onGradeDropdownExpandedChange(!isGradeDropdownExpanded) },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = if (isGradeDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = iconTint
                        )
                    }
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

            DropdownMenu(
                expanded = isGradeDropdownExpanded,
                onDismissRequest = { onGradeDropdownExpandedChange(false) },
                modifier = gradeDropdownModifier,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                gradeOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option) },
                        onClick = {
                            onGradeDropdownExpandedChange(false)
                            if (option == gradeOtherOption) {
                                onGradeChange("")
                                gradeFocusRequester.requestFocus()
                            } else {
                                onGradeChange(option)
                            }
                        },
                        enabled = enabled
                    )
                }
            }
        }
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
    var isRateDropdownExpanded by remember { mutableStateOf(false) }
    var rateFieldSize by remember { mutableStateOf(IntSize.Zero) }
    val rateDropdownWidth = with(LocalDensity.current) { rateFieldSize.width.toDp() }
    val rateDropdownModifier = if (rateDropdownWidth > 0.dp) Modifier.width(rateDropdownWidth) else Modifier
    val rateOptions = remember { listOf(1500, 2000, 2500, 3000) }
    val textFieldColors = editorFieldColors()

    Box {
        OutlinedTextField(
            value = rate,
            onValueChange = onRateChange,
            label = { Text(text = stringResource(id = R.string.student_editor_rate)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onGloballyPositioned { rateFieldSize = it.size },
            singleLine = true,
            enabled = enabled,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.CurrencyRuble,
                    contentDescription = null,
                    tint = iconTint
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = { isRateDropdownExpanded = !isRateDropdownExpanded },
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = if (isRateDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = iconTint
                    )
                }
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

        DropdownMenu(
            expanded = isRateDropdownExpanded,
            onDismissRequest = { isRateDropdownExpanded = false },
            modifier = rateDropdownModifier,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            rateOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.student_editor_rate_option_value, option)) },
                    onClick = {
                        onRateChange(option.toString())
                        isRateDropdownExpanded = false
                    },
                    enabled = enabled
                )
            }
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.student_editor_rate_option_other)) },
                onClick = {
                    isRateDropdownExpanded = false
                    onRateChange("")
                    focusRequester.tryRequestFocus()
                },
                enabled = enabled
            )
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

private fun buildSubjectValue(selected: List<String>, custom: String): String {
    val ordered = LinkedHashSet<String>()
    selected.map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { ordered.add(it) }
    custom.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { ordered.add(it) }
    return ordered.joinToString(separator = ", ")
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
