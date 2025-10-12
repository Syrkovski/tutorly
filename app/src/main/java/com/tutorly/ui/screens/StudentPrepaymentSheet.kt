package com.tutorly.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.outlined.CurrencyRuble
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.ui.components.TutorlyBottomSheetContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentPrepaymentSheet(
    onDismiss: () -> Unit,
    onSaved: (Int) -> Unit,
    vm: StudentPrepaymentViewModel = hiltViewModel(),
) {
    val state = vm.formState
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val amountFocusRequester = remember { FocusRequester() }

    fun hideSheetAndThen(action: () -> Unit) {
        if (sheetState.isVisible) {
            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { action() }
        } else {
            action()
        }
    }

    BackHandler(enabled = !state.isSaving) {
        hideSheetAndThen {
            vm.reset()
            onDismiss()
        }
    }

    LaunchedEffect(state.isSaving) {
        if (!state.isSaving) {
            amountFocusRequester.requestFocus()
        }
    }

    val attemptSave: () -> Unit = {
        if (!state.isSaving) {
            vm.submit(
                onSuccess = { amount ->
                    hideSheetAndThen {
                        vm.reset()
                        onSaved(amount)
                    }
                },
                onError = { message ->
                    val text = if (message.isNotBlank()) {
                        message
                    } else {
                        context.getString(R.string.student_prepayment_error)
                    }
                    coroutineScope.launch { snackbarHostState.showSnackbar(text) }
                }
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!state.isSaving) {
                hideSheetAndThen {
                    vm.reset()
                    onDismiss()
                }
            }
        },
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Unspecified,
        scrimColor = Color.Black.copy(alpha = 0.32f),
    ) {
        TutorlyBottomSheetContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
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
                    Icon(imageVector = Icons.Outlined.CurrencyRuble, contentDescription = null)
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
                    Icon(imageVector = Icons.Filled.Description, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { attemptSave() }),
                enabled = !state.isSaving,
                minLines = 3
            )

            Button(
                onClick = attemptSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = stringResource(id = R.string.student_prepayment_save))
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
            }
        }
    }
}
