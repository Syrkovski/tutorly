package com.tutorly.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingDialog(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.importFromGoogleCalendar()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Добро пожаловать в Tutorly",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Шаг ${state.step} из 3",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (state.step) {
                    1 -> StepSubjects(state = state, viewModel = viewModel)
                    2 -> StepRate(state = state, viewModel = viewModel)
                    else -> StepImport(state = state)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (state.step) {
                    1 -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = viewModel::completeOnboarding) { Text("Пропустить") }
                            Button(onClick = viewModel::goNext) { Text("Далее") }
                        }
                    }

                    2 -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = viewModel::goBack) { Text("Назад") }
                            Button(onClick = viewModel::goNext) { Text("Далее") }
                        }
                    }

                    else -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = viewModel::goBack) { Text("Назад") }
                            TextButton(onClick = viewModel::completeOnboarding) { Text("Пока без импорта") }
                        }
                        Button(
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_CALENDAR
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    viewModel.importFromGoogleCalendar()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                }
                            },
                            enabled = !state.isImporting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (state.isImporting) "Импортируем…" else "Импортировать из Google Календаря")
                        }
                        state.importError?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepSubjects(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Выберите основные предметы:")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.suggestedSubjects.forEach { subject ->
                val selected = state.selectedSubjects.contains(subject)
                Box(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.toggleSubject(subject) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = subject,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
                value = state.customSubject,
                onValueChange = viewModel::setCustomSubject,
                singleLine = true,
                label = { Text("Свой предмет") }
            )
            Button(onClick = viewModel::addCustomSubject) {
                Text("Добавить")
            }
        }
    }
}

@Composable
private fun StepRate(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Выберите ставку за час:")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.recommendedRatesRubles.forEach { rate ->
                val selected = state.selectedRateRubles == rate
                Box(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.selectRate(rate) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "₽$rate",
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Text(text = "Ставка: ₽${state.selectedRateRubles}/ч")

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
                value = state.customRateInput,
                onValueChange = viewModel::setCustomRate,
                singleLine = true,
                label = { Text("Своя ставка") }
            )
            Button(onClick = viewModel::applyCustomRate) {
                Text("Добавить")
            }
        }
    }
}

@Composable
private fun StepImport(state: OnboardingUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Импортировать занятия из Google Календаря?",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Мы попробуем автоматически перенести уроки в Tutorly.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (state.isImporting) {
            Text(
                text = "Идёт импорт…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
