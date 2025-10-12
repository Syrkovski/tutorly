package com.tutorly.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.util.Locale

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
    onArchivedChange: (Boolean) -> Unit,
    onActiveChange: (Boolean) -> Unit,
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
    val gradeNumbers = remember { (1..11).toList() }
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
    val columnModifier = if (enableScrolling) {
        modifier.verticalScroll(scrollState)
    } else {
        modifier
    }

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

        if (showFullForm || editTarget == StudentEditTarget.PHONE) {
            PhoneSection(
                phone = state.phone,
                onPhoneChange = onPhoneChange,
                enabled = enabled,
                focusRequester = phoneFocusRequester,
                isStandalone = !showFullForm && editTarget == StudentEditTarget.PHONE,
                onSubmit = onSubmit
            )
        }

        if (showFullForm || editTarget == StudentEditTarget.MESSENGER) {
            MessengerSection(
                messenger = state.messenger,
                onMessengerChange = onMessengerChange,
                enabled = enabled,
                focusRequester = messengerFocusRequester,
                isStandalone = !showFullForm && editTarget == StudentEditTarget.MESSENGER,
                onSubmit = onSubmit
            )
        }

        if (showFullForm || editTarget == StudentEditTarget.NOTES) {
            NotesSection(
                note = state.note,
                onNoteChange = onNoteChange,
                enabled = enabled,
                focusRequester = noteFocusRequester,
                onSubmit = onSubmit
            )
        }

        if (showFullForm) {
            StatusSection(
                isArchived = state.isArchived,
                onArchivedChange = onArchivedChange,
                isActive = state.isActive,
                onActiveChange = onActiveChange,
                enabled = enabled
            )
        }

        if (showFullForm) {
            Spacer(Modifier.height(4.dp))
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
                Icon(imageVector = Icons.Filled.Person, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )
        if (state.nameError) {
            Text(
                text = stringResource(id = R.string.student_editor_name_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        OutlinedTextField(
            value = state.subject,
            onValueChange = onSubjectChange,
            label = { Text(text = stringResource(id = R.string.student_editor_subject)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Book, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )

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
                    Icon(imageVector = Icons.Filled.School, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(
                        onClick = { onGradeDropdownExpandedChange(!isGradeDropdownExpanded) },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = if (isGradeDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null
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
                }
            )

            DropdownMenu(
                expanded = isGradeDropdownExpanded,
                onDismissRequest = { onGradeDropdownExpandedChange(false) },
                modifier = gradeDropdownModifier
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
            Icon(imageVector = Icons.Filled.AttachMoney, contentDescription = null)
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Decimal,
            imeAction = if (isStandalone) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = if (isStandalone) {
            KeyboardActions(onDone = { onSubmit?.invoke() })
        } else {
            KeyboardActions.Default
        }
    )
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
            Icon(imageVector = Icons.Filled.Phone, contentDescription = null)
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = if (isStandalone) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = if (isStandalone) {
            KeyboardActions(onDone = { onSubmit?.invoke() })
        } else {
            KeyboardActions.Default
        }
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
                    Icon(imageVector = Icons.Filled.Message, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }, enabled = enabled) {
                        Icon(
                            imageVector = if (isDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }
            )

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
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
                Icon(imageVector = Icons.Filled.AlternateEmail, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = if (isStandalone) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = if (isStandalone) {
                KeyboardActions(onDone = { onSubmit?.invoke() })
            } else {
                KeyboardActions.Default
            }
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
            Icon(imageVector = Icons.Filled.Description, contentDescription = null)
        },
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onSubmit?.invoke()
        })
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

@Composable
private fun StatusSection(
    isArchived: Boolean,
    onArchivedChange: (Boolean) -> Unit,
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RowSwitch(
            label = stringResource(id = R.string.student_editor_is_archived),
            checked = isArchived,
            enabled = enabled,
            onCheckedChange = onArchivedChange
        )
        RowSwitch(
            label = stringResource(id = R.string.student_editor_active),
            checked = isActive,
            enabled = enabled,
            onCheckedChange = onActiveChange
        )
    }
}

@Composable
private fun RowSwitch(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

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
