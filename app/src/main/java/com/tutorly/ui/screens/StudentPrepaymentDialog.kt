package com.tutorly.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.outlined.CurrencyRuble
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.ui.components.TutorlyDialog
import kotlinx.coroutines.launch

@Composable
fun StudentPrepaymentDialog(
    onDismiss: () -> Unit,
    onSaved: (Int) -> Unit,
    vm: StudentPrepaymentViewModel = hiltViewModel(),
) {
    val state = vm.formState
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val amountFocusRequester = remember { FocusRequester() }
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val context = LocalContext.current

    LaunchedEffect(state.isSaving) {
        if (!state.isSaving) {
            amountFocusRequester.requestFocus()
        }
    }

    val closeDialog = {
        vm.reset()
        onDismiss()
    }

    val attemptSave: () -> Unit = {
        if (!state.isSaving) {
            vm.submit(
                onSuccess = { amount ->
                    vm.reset()
                    onSaved(amount)
                    onDismiss()
                },
                onError = { message ->
                    val text = message.ifBlank { context.getString(R.string.student_prepayment_error) }
                    coroutineScope.launch { snackbarHostState.showSnackbar(text) }
                }
            )
        }
    }

    TutorlyDialog(
        onDismissRequest = {
            if (!state.isSaving) {
                closeDialog()
            }
        },
        modifier = Modifier.imePadding()
    ) {
        if (state.isSaving) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Text(
            text = stringResource(id = R.string.student_prepayment_title),
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = state.amount,
            onValueChange = vm::onAmountChange,
            label = { Text(text = stringResource(id = R.string.student_prepayment_amount_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(amountFocusRequester),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.CurrencyRuble,
                    contentDescription = null,
                    tint = iconTint
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            isError = state.amountError,
            supportingText = {
                if (state.amountError) {
                    Text(text = stringResource(id = R.string.student_prepayment_amount_error))
                }
            },
            singleLine = true,
            enabled = !state.isSaving
        )

        OutlinedTextField(
            value = state.note,
            onValueChange = vm::onNoteChange,
            label = { Text(text = stringResource(id = R.string.student_prepayment_note_label)) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = iconTint
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { attemptSave() }),
            enabled = !state.isSaving,
            minLines = 3
        )

        FilledTonalButton(
            onClick = attemptSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                Text(text = stringResource(id = R.string.student_prepayment_save))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        )
    }
}
