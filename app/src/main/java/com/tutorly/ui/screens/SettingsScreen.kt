package com.tutorly.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import com.tutorly.R
import com.tutorly.ui.components.TopBarContainer
import com.tutorly.ui.theme.extendedColors
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.loadCalendarCandidates()
        } else {
            viewModel.onCalendarPermissionDenied()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { SettingsTopBar(onBack = onBack) }
    ) { padding ->
        SettingsContent(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            state = uiState,
            onStartTimeClick = viewModel::updateWorkDayStart,
            onEndTimeClick = viewModel::updateWorkDayEnd,
            onWeekendToggle = viewModel::toggleWeekend,
            onThemeSelect = viewModel::selectTheme,
            onCalendarImportClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    viewModel.loadCalendarCandidates()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                }
            }
        )
    }

    if (uiState.isCalendarCandidateDialogVisible) {
        CalendarImportCandidatesDialog(
            candidates = uiState.calendarImportCandidates,
            selectedNames = uiState.calendarImportSelectedNames,
            isImporting = uiState.isCalendarImporting,
            onToggleCandidate = viewModel::toggleCalendarCandidate,
            onSelectAll = { selectAll -> viewModel.selectAllCalendarCandidates(selectAll) },
            onDismiss = viewModel::dismissCalendarCandidatesDialog,
            onConfirm = viewModel::confirmCalendarImport
        )
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    TopBarContainer {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.settings_back)
                )
            }

            Text(
                text = stringResource(id = R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.surface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 72.dp)
            )
        }
    }
}

@Composable
private fun SettingsContent(
    modifier: Modifier,
    state: SettingsUiState,
    onStartTimeClick: (LocalTime) -> Unit,
    onEndTimeClick: (LocalTime) -> Unit,
    onWeekendToggle: (DayOfWeek) -> Unit,
    onThemeSelect: (ThemeOption) -> Unit,
    onCalendarImportClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val timeFormatter = rememberTimeFormatter()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SettingsSectionTitle(text = stringResource(id = R.string.settings_work_hours))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimePreferenceCard(
                modifier = Modifier.weight(1f),
                label = stringResource(id = R.string.settings_work_start),
                time = state.workDayStart,
                formatter = timeFormatter,
                onTimeSelected = onStartTimeClick
            )
            TimePreferenceCard(
                modifier = Modifier.weight(1f),
                label = stringResource(id = R.string.settings_work_end),
                time = state.workDayEnd,
                formatter = timeFormatter,
                onTimeSelected = onEndTimeClick,
                displayOverride = if (state.workDayEndExtendsToNextDay && state.workDayEnd == LocalTime.MIDNIGHT) {
                    "24:00"
                } else {
                    null
                }
            )
        }

        SettingsSectionTitle(text = stringResource(id = R.string.settings_weekends))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DayOfWeek.values().forEach { day ->
                val isWeekend = day in state.weekendDays
                FilterChip(
                    selected = isWeekend,
                    onClick = { onWeekendToggle(day) },
                    label = {
                        Text(
                            text = day.getDisplayName(TextStyle.SHORT, Locale("ru")),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.extendedColors.chipSelected,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        SettingsSectionTitle(text = stringResource(id = R.string.settings_theme))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.availableThemes.forEach { option ->
                val isSelected = option.preset == state.selectedTheme
                ThemeOptionChip(
                    option = option,
                    isSelected = isSelected,
                    onSelect = { onThemeSelect(option) }
                )
            }
        }

        SettingsSectionTitle(text = stringResource(id = R.string.settings_calendar_import_title))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.settings_calendar_import_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onCalendarImportClick,
                    enabled = !state.isCalendarImporting && !state.isCalendarCandidatesLoading
                ) {
                    Text(
                        text = if (state.isCalendarImporting) {
                            stringResource(id = R.string.settings_calendar_import_in_progress)
                        } else if (state.isCalendarCandidatesLoading) {
                            stringResource(id = R.string.settings_calendar_import_loading)
                        } else {
                            stringResource(id = R.string.settings_calendar_import_button)
                        }
                    )
                }
                if (state.isCalendarImporting || state.isCalendarCandidatesLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = if (state.isCalendarCandidatesLoading) {
                                stringResource(id = R.string.settings_calendar_import_loading)
                            } else {
                                stringResource(id = R.string.settings_calendar_import_in_progress)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val errorMessage = state.calendarImportError?.let { error ->
                    stringResource(id = error.messageRes)
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                val result = state.calendarImportResult
                if (result != null && errorMessage == null) {
                    Text(
                        text = stringResource(
                            id = R.string.settings_calendar_import_success,
                            result.createdLessons,
                            result.createdStudents,
                            result.skippedDuplicates
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun CalendarImportCandidatesDialog(
    candidates: List<com.tutorly.data.calendar.GoogleCalendarImportCandidate>,
    selectedNames: Set<String>,
    isImporting: Boolean,
    onToggleCandidate: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.settings_calendar_import_dialog_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (candidates.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.settings_calendar_import_no_candidates),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val selectAllChecked = selectedNames.size == candidates.size
                        Text(
                            text = stringResource(id = R.string.settings_calendar_import_select_all),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Checkbox(
                            checked = selectAllChecked,
                            onCheckedChange = { onSelectAll(it) }
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        candidates.forEach { candidate ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = candidate.studentName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = stringResource(
                                            id = R.string.settings_calendar_import_candidate_count,
                                            candidate.lessonsCount
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Checkbox(
                                    checked = candidate.studentName in selectedNames,
                                    onCheckedChange = { onToggleCandidate(candidate.studentName) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isImporting && candidates.isNotEmpty()
            ) {
                Text(text = stringResource(id = R.string.settings_calendar_import_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isImporting
            ) {
                Text(text = stringResource(id = R.string.settings_calendar_import_cancel))
            }
        }
    )
}

@Composable
private fun rememberTimeFormatter(): DateTimeFormatter =
    remember { DateTimeFormatter.ofPattern("HH:mm") }

@Composable
private fun TimePreferenceCard(
    modifier: Modifier = Modifier,
    label: String,
    time: LocalTime,
    formatter: DateTimeFormatter,
    onTimeSelected: (LocalTime) -> Unit,
    displayOverride: String? = null
) {
    val context = LocalContext.current
    val displayValue = remember(time, formatter, displayOverride) {
        displayOverride ?: formatter.format(time)
    }
    Card(
        modifier = modifier.wrapContentHeight(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            onTimeSelected(LocalTime.of(hourOfDay, minute))
                        },
                        time.hour,
                        time.minute,
                        true
                    ).show()
                }
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ThemeOptionChip(
    option: ThemeOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    ElevatedAssistChip(
        onClick = onSelect,
        label = { Text(text = stringResource(id = option.labelRes)) },
        leadingIcon = {
            val previewColor = Color(option.previewColor)
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(previewColor, shape = CircleShape)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
            )
        },
        colors = AssistChipDefaults.elevatedAssistChipColors(
            containerColor = if (isSelected) {
                MaterialTheme.extendedColors.chipSelected
            } else {
                Color.White
            },
            labelColor = if (isSelected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            leadingIconContentColor = if (isSelected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        shape = RoundedCornerShape(50)
    )
}
