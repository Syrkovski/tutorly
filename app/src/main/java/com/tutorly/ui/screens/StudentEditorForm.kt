package com.tutorly.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tutorly.R

@Composable
fun StudentEditorForm(
    state: StudentEditorFormState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onMessengerChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onArchivedChange: (Boolean) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    focusOnStart: Boolean = false,
    enabled: Boolean = true,
    onSubmit: (() -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(focusOnStart) {
        if (focusOnStart) {
            focusRequester.requestFocus()
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
                .focusRequester(focusRequester),
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
            value = state.phone,
            onValueChange = onPhoneChange,
            label = { Text(text = stringResource(id = R.string.student_editor_phone)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = state.messenger,
            onValueChange = onMessengerChange,
            label = { Text(text = stringResource(id = R.string.student_editor_messenger)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = state.note,
            onValueChange = onNoteChange,
            label = { Text(text = stringResource(id = R.string.student_editor_notes)) },
            modifier = Modifier.fillMaxWidth(),
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
