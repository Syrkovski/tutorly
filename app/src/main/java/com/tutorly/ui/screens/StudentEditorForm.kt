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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    initialFocus: StudentEditTarget? = null,
    enabled: Boolean = true,
    onSubmit: (() -> Unit)? = null,
) {
    val nameFocusRequester = remember { FocusRequester() }
    val gradeFocusRequester = remember { FocusRequester() }
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
                StudentEditTarget.PROFILE -> nameFocusRequester.requestFocus()
                StudentEditTarget.PHONE -> phoneFocusRequester.requestFocus()
                StudentEditTarget.MESSENGER -> messengerFocusRequester.requestFocus()
                StudentEditTarget.NOTES -> noteFocusRequester.requestFocus()
                null -> Unit
            }
        }
    }

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )

        var gradeFieldSize by remember { mutableStateOf(IntSize.Zero) }
        val gradeDropdownWidth = with(LocalDensity.current) { gradeFieldSize.width.toDp() }
        val gradeDropdownModifier = if (gradeDropdownWidth > 0.dp) Modifier.width(gradeDropdownWidth) else Modifier
        Box {
            OutlinedTextField(
                value = state.grade,
                onValueChange = {
                    onGradeChange(it)
                },
                label = { Text(text = stringResource(id = R.string.student_editor_grade)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { gradeFieldSize = it.size }
                    .focusRequester(gradeFocusRequester)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            isGradeDropdownExpanded = false
                        } else if (enabled) {
                            isGradeDropdownExpanded = true
                        }
                    },
                singleLine = true,
                enabled = enabled,
                trailingIcon = {
                    IconButton(
                        onClick = { isGradeDropdownExpanded = !isGradeDropdownExpanded },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = if (isGradeDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
            )

            DropdownMenu(
                expanded = isGradeDropdownExpanded,
                onDismissRequest = { isGradeDropdownExpanded = false },
                modifier = gradeDropdownModifier
            ) {
                gradeOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option) },
                        onClick = {
                            isGradeDropdownExpanded = false
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

        OutlinedTextField(
            value = state.rate,
            onValueChange = onRateChange,
            label = { Text(text = stringResource(id = R.string.student_editor_rate)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            label = { Text(text = stringResource(id = R.string.student_editor_phone)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(phoneFocusRequester),
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = state.messenger,
            onValueChange = onMessengerChange,
            label = { Text(text = stringResource(id = R.string.student_editor_messenger)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(messengerFocusRequester),
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = state.note,
            onValueChange = onNoteChange,
            label = { Text(text = stringResource(id = R.string.student_editor_notes)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(noteFocusRequester),
            minLines = 3,
            enabled = enabled,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                onSubmit?.invoke()
            })
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RowSwitch(
                label = stringResource(id = R.string.student_editor_is_archived),
                checked = state.isArchived,
                enabled = enabled,
                onCheckedChange = onArchivedChange
            )
            RowSwitch(
                label = stringResource(id = R.string.student_editor_active),
                checked = state.isActive,
                enabled = enabled,
                onCheckedChange = onActiveChange
            )
        }

        Spacer(Modifier.height(4.dp))
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
