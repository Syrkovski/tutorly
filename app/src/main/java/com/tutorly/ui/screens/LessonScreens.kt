package com.tutorly.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@Composable
fun LessonEditorScreen(
    startTime: Instant?,
    studentId: Long?,
    onClose: () -> Unit
) {
    LessonPlaceholder(
        title = "Создать урок",
        primary = startTime?.toString() ?: "—",
        secondary = studentId?.toString() ?: "—",
        actionLabel = "Закрыть",
        onAction = onClose
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailsScreen(
    lessonId: Long,
    studentId: Long?,
    startTime: Instant?,
    onBack: () -> Unit,
    vm: LessonDetailsViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = state.snackbarMessage
    LaunchedEffect(snackbarMessage) {
        if (!snackbarMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(snackbarMessage)
            vm.consumeSnackbar()
        }
    }

    var showStudentSheet by remember { mutableStateOf(false) }
    var showDurationDialog by remember { mutableStateOf(false) }
    var showPriceDialog by remember { mutableStateOf(false) }

    val locale = state.locale
    val context = LocalContext.current
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM", locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }
    val capitalizedDate = remember(state.date, locale) {
        dateFormatter.format(state.date).replaceFirstChar { char ->
            if (char.isLowerCase()) {
                char.titlecase(locale)
            } else {
                char.toString()
            }
        }
    }
    val currencyFormatter = remember(state.locale, state.currencyCode) {
        runCatching {
            NumberFormat.getCurrencyInstance(state.locale).apply {
                currency = Currency.getInstance(state.currencyCode)
            }
        }.getOrElse { NumberFormat.getCurrencyInstance(state.locale) }
    }
    val formattedPrice = remember(state.priceCents, currencyFormatter) {
        currencyFormatter.format(state.priceCents / 100.0)
    }

    if (showDurationDialog) {
        DurationDialog(
            currentMinutes = state.durationMinutes,
            onDismiss = { showDurationDialog = false },
            onConfirm = { minutes ->
                vm.onDurationSelected(minutes)
                showDurationDialog = false
            }
        )
    }

    if (showPriceDialog) {
        PriceDialog(
            currentPriceCents = state.priceCents,
            currencySymbol = state.currencySymbol,
            onDismiss = { showPriceDialog = false },
            onConfirm = { cents ->
                vm.onPriceChanged(cents)
                showPriceDialog = false
            }
        )
    }

    if (showStudentSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showStudentSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.lesson_details_student_sheet_title),
                    style = MaterialTheme.typography.titleMedium
                )
                if (state.studentOptions.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.lesson_details_empty_students),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.studentOptions, key = { it.id }) { option ->
                            val selected = option.id == state.studentId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.onStudentSelected(option.id)
                                        showStudentSheet = false
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val subtitle = listOfNotNull(option.subject, option.grade)
                                        .joinToString(separator = " ")
                                    if (subtitle.isNotBlank()) {
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = { LessonDetailsTopBar(onBack = onBack) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.isNotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.lesson_details_not_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (state.isSaving) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStudentSheet = true },
                        shape = MaterialTheme.shapes.extraLarge,
                        tonalElevation = 2.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = state.studentName.takeIf { it.isNotBlank() }
                                    ?: stringResource(id = R.string.lesson_details_student_placeholder),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            val subtitle = listOfNotNull(state.subjectName, state.studentGrade)
                                .joinToString(separator = " ")
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        vm.onDateSelected(LocalDate.of(year, month + 1, day))
                                    },
                                    state.date.year,
                                    state.date.monthValue - 1,
                                    state.date.dayOfMonth
                                ).show()
                            },
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stringResource(id = R.string.lesson_details_date_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = capitalizedDate,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            vm.onTimeSelected(LocalTime.of(hour, minute))
                                        },
                                        state.time.hour,
                                        state.time.minute,
                                        true
                                    ).show()
                                },
                            shape = MaterialTheme.shapes.large,
                            tonalElevation = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(id = R.string.lesson_details_time_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = state.time.format(timeFormatter),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDurationDialog = true },
                            shape = MaterialTheme.shapes.large,
                            tonalElevation = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Timelapse,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(id = R.string.lesson_details_duration_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${state.durationMinutes} ${stringResource(id = R.string.lesson_create_minutes_suffix)}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPriceDialog = true },
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.lesson_details_price_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formattedPrice,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonDetailsTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.lesson_details_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DurationDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val presets = listOf(45, 60, 90, 120)
    var selectedPreset by remember(currentMinutes) {
        mutableStateOf(presets.firstOrNull { it == currentMinutes })
    }
    var customInput by remember(currentMinutes) {
        mutableStateOf(if (currentMinutes in presets) "" else currentMinutes.takeIf { it > 0 }?.toString().orEmpty())
    }
    val confirmEnabled = selectedPreset != null || customInput.toIntOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.lesson_details_duration_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { option ->
                        val selected = selectedPreset == option
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedPreset = option
                                customInput = ""
                            },
                            label = { Text(text = stringResource(id = R.string.lesson_create_duration_chip, option)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { value ->
                        val digits = value.filter { it.isDigit() }
                        customInput = digits
                        if (digits.isNotEmpty()) {
                            selectedPreset = null
                        }
                    },
                    label = { Text(text = stringResource(id = R.string.lesson_details_duration_custom_hint)) },
                    suffix = { Text(text = stringResource(id = R.string.lesson_create_minutes_suffix)) },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val minutes = selectedPreset ?: customInput.toIntOrNull()
                    if (minutes != null && minutes > 0) {
                        onConfirm(minutes)
                    }
                },
                enabled = confirmEnabled
            ) {
                Text(text = stringResource(id = R.string.lesson_details_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.lesson_create_cancel))
            }
        }
    )
}

@Composable
private fun PriceDialog(
    currentPriceCents: Int,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var priceInput by remember(currentPriceCents) {
        mutableStateOf(currentPriceCents.takeIf { it > 0 }?.let { (it / 100).toString() } ?: "")
    }
    val confirmEnabled = priceInput.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.lesson_details_price_dialog_title)) },
        text = {
            OutlinedTextField(
                value = priceInput,
                onValueChange = { value ->
                    priceInput = value.filter { it.isDigit() }
                },
                label = { Text(text = stringResource(id = R.string.lesson_details_price_hint)) },
                suffix = { Text(text = currencySymbol) },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    priceInput.toIntOrNull()?.let { rubles ->
                        onConfirm(rubles * 100)
                    }
                },
                enabled = confirmEnabled
            ) {
                Text(text = stringResource(id = R.string.lesson_details_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.lesson_create_cancel))
            }
        }
    )
}

@Composable
private fun LessonPlaceholder(
    title: String,
    primary: String,
    secondary: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(text = "startTime: $primary", style = MaterialTheme.typography.bodyMedium)
        Text(text = "studentId: $secondary", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onAction) {
            Text(text = actionLabel)
        }
    }
}
