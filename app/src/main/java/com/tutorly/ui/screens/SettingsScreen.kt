package com.tutorly.ui.screens

import android.app.TimePickerDialog
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.ui.components.AppTopBar
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

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.settings_title),
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.settings_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        SettingsContent(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            state = uiState,
            onStartTimeClick = viewModel::updateWorkDayStart,
            onEndTimeClick = viewModel::updateWorkDayEnd,
            onWeekendToggle = viewModel::toggleWeekend,
            onThemeSelect = viewModel::selectTheme
        )
    }
}

@Composable
private fun SettingsContent(
    modifier: Modifier,
    state: SettingsUiState,
    onStartTimeClick: (LocalTime) -> Unit,
    onEndTimeClick: (LocalTime) -> Unit,
    onWeekendToggle: (DayOfWeek) -> Unit,
    onThemeSelect: (ThemeOption) -> Unit
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
                onTimeSelected = onEndTimeClick
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
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
private fun rememberTimeFormatter(): DateTimeFormatter =
    remember { DateTimeFormatter.ofPattern("HH:mm") }

@Composable
private fun TimePreferenceCard(
    modifier: Modifier = Modifier,
    label: String,
    time: LocalTime,
    formatter: DateTimeFormatter,
    onTimeSelected: (LocalTime) -> Unit
) {
    val context = LocalContext.current
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
                text = formatter.format(time),
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

