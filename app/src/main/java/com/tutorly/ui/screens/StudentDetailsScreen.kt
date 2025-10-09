package com.tutorly.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.models.Student
import com.tutorly.ui.components.PaymentBadge
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailsScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    vm: StudentDetailsViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            StudentDetailsTopBar(
                title = state.student?.name ?: stringResource(id = R.string.student_details_title_placeholder),
                onBack = onBack,
                onEdit = if (state.student != null) onEdit else null
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.student == null -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.student_details_missing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    StudentPaymentsCard(
                        hasDebt = state.hasDebt,
                        totalDebtCents = state.totalDebtCents
                    )
                    StudentContactCard(student = state.student)
                    StudentNotesCard(note = state.student.note)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentDetailsTopBar(
    title: String,
    onBack: () -> Unit,
    onEdit: (() -> Unit)?
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.student_details_back)
                )
            }
        },
        actions = {
            onEdit?.let {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(id = R.string.student_details_edit)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun StudentPaymentsCard(
    hasDebt: Boolean,
    totalDebtCents: Long,
    modifier: Modifier = Modifier
) {
    val formatter = remember { NumberFormat.getCurrencyInstance(Locale("ru", "RU")) }
    val formattedDebt = remember(totalDebtCents) { formatter.format(totalDebtCents / 100.0) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(id = R.string.student_details_payments_title),
                style = MaterialTheme.typography.titleMedium
            )
            PaymentBadge(paid = !hasDebt)
            Text(
                text = if (hasDebt) {
                    stringResource(id = R.string.student_details_debt_amount, formattedDebt)
                } else {
                    stringResource(id = R.string.student_details_no_debt)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StudentContactCard(
    student: Student,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(id = R.string.student_details_contact_title),
                style = MaterialTheme.typography.titleMedium
            )
            InfoRow(
                label = stringResource(id = R.string.student_details_phone_label),
                value = student.phone,
                placeholder = stringResource(id = R.string.student_details_phone_placeholder)
            )
            InfoRow(
                label = stringResource(id = R.string.student_details_messenger_label),
                value = student.messenger,
                placeholder = stringResource(id = R.string.student_details_messenger_placeholder)
            )
        }
    }
}

@Composable
private fun StudentNotesCard(
    note: String?,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.student_details_notes_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note?.takeIf { it.isNotBlank() }
                    ?: stringResource(id = R.string.student_details_notes_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String?,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: placeholder,
            style = MaterialTheme.typography.bodyLarge,
            color = if (value.isNullOrBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
