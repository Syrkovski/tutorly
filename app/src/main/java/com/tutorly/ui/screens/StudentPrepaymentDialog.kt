package com.tutorly.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CurrencyRuble
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.ui.components.TutorlyDialog
import com.tutorly.ui.theme.extendedColors
import kotlinx.coroutines.launch

@Composable
fun StudentPrepaymentDialog(
    onDismiss: () -> Unit,
    onSaved: (StudentPrepaymentResult) -> Unit,
    vm: StudentPrepaymentViewModel = hiltViewModel(),
) {
    val state = vm.formState
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val amountFocusRequester = remember { FocusRequester() }
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.extendedColors.accent
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
                onSuccess = { result ->
                    vm.reset()
                    onSaved(result)
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
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { attemptSave() }),
                isError = state.amountError,
                supportingText = {
                    if (state.amountError) {
                        Text(text = stringResource(id = R.string.student_prepayment_amount_error))
                    }
                },
                singleLine = true,
                enabled = !state.isSaving,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    errorContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = accent,
                    unfocusedBorderColor = accent.copy(alpha = 0.4f),
                    disabledBorderColor = accent.copy(alpha = 0.24f),
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val actionColors = ButtonDefaults.textButtonColors(
                contentColor = accent,
                disabledContentColor = accent.copy(alpha = 0.5f)
            )
            TextButton(
                onClick = {
                    if (!state.isSaving) {
                        closeDialog()
                    }
                },
                enabled = !state.isSaving,
                colors = actionColors
            ) {
                Text(text = stringResource(id = R.string.student_editor_cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = attemptSave,
                enabled = !state.isSaving,
                colors = actionColors
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = accent
                    )
                } else {
                    Text(text = stringResource(id = R.string.student_editor_save))
                }
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
