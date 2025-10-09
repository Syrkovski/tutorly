package com.tutorly.ui.lessoncard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tutorly.R
import com.tutorly.domain.model.PaymentStatusIcon
import com.tutorly.models.PaymentStatus
import java.text.NumberFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LessonCardSheet(
    state: LessonCardUiState,
    zoneId: ZoneId,
    onDismissRequest: () -> Unit,
    onCancelDismiss: () -> Unit,
    onConfirmDismiss: () -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveNote: () -> Unit,
    onMarkPaid: () -> Unit,
    onRequestMarkDue: () -> Unit,
    onDismissMarkDue: () -> Unit,
    onConfirmMarkDue: () -> Unit,
    onRequestEdit: () -> Unit,
    onSnackbarConsumed: () -> Unit,
) {
    if (!state.isVisible) {
        return
    }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    val snackbarText = state.snackbarMessage?.let { message ->
        when (message) {
            LessonCardMessage.NoteSaved -> stringResource(R.string.lesson_card_snackbar_note_saved)
            is LessonCardMessage.PaymentMarked -> when (message.status) {
                PaymentStatus.PAID -> stringResource(R.string.lesson_card_snackbar_paid)
                PaymentStatus.DUE, PaymentStatus.UNPAID -> stringResource(R.string.lesson_card_snackbar_due)
                PaymentStatus.CANCELLED -> stringResource(R.string.lesson_card_snackbar_due)
            }
            is LessonCardMessage.Error -> stringResource(R.string.lesson_card_snackbar_error)
        }
    }

    LaunchedEffect(snackbarText) {
        snackbarText?.let {
            snackbarHostState.showSnackbar(it)
            onSnackbarConsumed()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (state.isNoteDirty) {
                scope.launch { sheetState.show() }
                onDismissRequest()
            } else {
                onDismissRequest()
            }
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                }
                LessonHeader(state = state, onClose = onDismissRequest)
                LessonMetadataBlock(state = state, zoneId = zoneId)
                LessonPaymentBlock(state = state, onMarkPaid = onMarkPaid, onRequestMarkDue = onRequestMarkDue)
                LessonNoteBlock(
                    state = state,
                    onNoteChange = onNoteChange,
                    onSaveNote = onSaveNote
                )
                OutlinedButton(
                    onClick = onRequestEdit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.details != null
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.lesson_card_edit))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
    }

    if (state.showUnsavedExitDialog) {
        AlertDialog(
            onDismissRequest = onCancelDismiss,
            title = { Text(stringResource(R.string.lesson_card_unsaved_title)) },
            text = { Text(stringResource(R.string.lesson_card_unsaved_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmDismiss) {
                    Text(stringResource(R.string.lesson_card_unsaved_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDismiss) {
                    Text(stringResource(R.string.lesson_card_unsaved_keep))
                }
            }
        )
    }

    if (state.showMarkDueDialog) {
        AlertDialog(
            onDismissRequest = onDismissMarkDue,
            title = { Text(stringResource(R.string.lesson_card_mark_due_title)) },
            text = { Text(stringResource(R.string.lesson_card_mark_due_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmMarkDue) {
                    Text(stringResource(R.string.lesson_card_mark_due_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissMarkDue) {
                    Text(stringResource(R.string.lesson_card_mark_due_cancel))
                }
            }
        )
    }
}

@Composable
private fun LessonHeader(state: LessonCardUiState, onClose: () -> Unit) {
    val details = state.details
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val name = details?.studentName.orEmpty()
        val initials = remember(name) {
            name.split(" ").filter { it.isNotBlank() }.take(2).map { it.first().uppercase() }.joinToString("")
        }
        Surface(
            modifier = Modifier
                .sizeAvatar()
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials.ifBlank { "?" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name.ifBlank { stringResource(R.string.lesson_card_student_placeholder) },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            details?.lessonTitle?.takeIf { it.isNotBlank() }?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.lesson_card_close))
        }
    }
}

@Composable
private fun LessonMetadataBlock(state: LessonCardUiState, zoneId: ZoneId) {
    val details = state.details ?: return
    val locale = remember { Locale.getDefault() }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }
    val durationMinutes = remember(details.duration) { details.duration.toMinutes().toInt().coerceAtLeast(0) }
    val start = remember(details.startAt, zoneId) { ZonedDateTime.ofInstant(details.startAt, zoneId) }
    val end = remember(details.endAt, zoneId) { ZonedDateTime.ofInstant(details.endAt, zoneId) }

    val subject = details.subjectName?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.lesson_card_subject_placeholder)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = subject, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                InfoCell(
                    label = stringResource(R.string.lesson_card_time_label),
                    value = stringResource(
                        R.string.lesson_card_time_value,
                        dateFormatter.format(start),
                        timeFormatter.format(start),
                        timeFormatter.format(end)
                    ),
                    leadingIcon = Icons.Default.Schedule
                )
                InfoCell(
                    label = stringResource(R.string.lesson_card_duration_label),
                    value = stringResource(R.string.lesson_card_duration_value, durationMinutes),
                    leadingIcon = null
                )
            }
        }
    }
}

@Composable
private fun LessonPaymentBlock(
    state: LessonCardUiState,
    onMarkPaid: () -> Unit,
    onRequestMarkDue: () -> Unit,
) {
    val details = state.details ?: return
    val locale = remember { Locale.getDefault() }
    val currencyFormatter = remember(locale) { NumberFormat.getCurrencyInstance(locale) }
    val amount = remember(details.priceCents) { currencyFormatter.format(details.priceCents / 100.0) }
    val statusChip = remember(details.paymentStatus) { paymentStatusChip(details.paymentStatus) }

    Card { 
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.lesson_card_price_label), style = MaterialTheme.typography.labelMedium)
                    Text(text = amount, style = MaterialTheme.typography.titleLarge)
                }
                StatusChip(statusChip)
            }
            val isPaid = details.paymentStatus == PaymentStatus.PAID
            val actionLabel = if (isPaid) {
                stringResource(R.string.lesson_card_mark_due)
            } else {
                stringResource(R.string.lesson_card_mark_paid)
            }
            val onClick = if (isPaid) onRequestMarkDue else onMarkPaid
            val enabled = !state.isPaymentActionRunning && details.paymentStatus != PaymentStatus.CANCELLED
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled
            ) {
                if (state.isPaymentActionRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun LessonNoteBlock(
    state: LessonCardUiState,
    onNoteChange: (String) -> Unit,
    onSaveNote: () -> Unit,
) {
    if (state.details == null) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.noteDraft,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.lesson_card_note_label)) },
            placeholder = { Text(stringResource(R.string.lesson_card_note_placeholder)) },
            supportingText = {
                Text(
                    stringResource(
                        R.string.lesson_card_note_counter,
                        state.noteDraft.length,
                        LESSON_CARD_NOTE_LIMIT
                    )
                )
            },
            maxLines = 4,
            shape = RoundedCornerShape(12.dp)
        )
        Button(
            onClick = onSaveNote,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isNoteDirty && !state.isSavingNote
        ) {
            if (state.isSavingNote) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.lesson_card_note_save))
            }
        }
    }
}

private data class PaymentStatusChip(
    val labelRes: Int,
    val icon: PaymentStatusIcon?,
    val background: Color,
    val foreground: Color,
)

@Composable
private fun paymentStatusChip(status: PaymentStatus): PaymentStatusChip {
    val (label, background, foreground) = when (status) {
        PaymentStatus.PAID -> Triple(
            R.string.lesson_status_paid,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.tertiary
        )
        PaymentStatus.DUE, PaymentStatus.UNPAID -> Triple(
            R.string.lesson_status_due,
            MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.error
        )
        PaymentStatus.CANCELLED -> Triple(
            R.string.lesson_status_cancelled,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    val icon = when (status) {
        PaymentStatus.PAID -> PaymentStatusIcon.PAID
        PaymentStatus.DUE, PaymentStatus.UNPAID -> PaymentStatusIcon.OUTSTANDING
        PaymentStatus.CANCELLED -> PaymentStatusIcon.CANCELLED
    }
    return PaymentStatusChip(label, icon, background, foreground)
}

@Composable
private fun StatusChip(chip: PaymentStatusChip) {
    Surface(color = chip.background, contentColor = chip.foreground, shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (chip.icon) {
                PaymentStatusIcon.PAID -> Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                PaymentStatusIcon.OUTSTANDING -> Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                PaymentStatusIcon.CANCELLED -> Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(text = stringResource(chip.labelRes), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun InfoCell(label: String, value: String, leadingIcon: androidx.compose.ui.graphics.vector.ImageVector?) {
    Surface(
        modifier = Modifier.weight(1f),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leadingIcon?.let { icon ->
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun Modifier.sizeAvatar(): Modifier = this.then(Modifier.size(48.dp))
