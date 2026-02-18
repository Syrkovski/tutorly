package com.tutorly.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingDialog(
    modifier: Modifier = Modifier,
    onImportLessons: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {},
        title = {
            Text(
                text = "Добро пожаловать в Tutorly",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Выберите предметы, которые вы ведёте:")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.suggestedSubjects.forEach { subject ->
                        FilterChip(
                            selected = state.selectedSubjects.contains(subject),
                            onClick = { viewModel.toggleSubject(subject) },
                            label = { Text(subject) }
                        )
                    }
                }
                if (state.selectedSubjects.isNotEmpty()) {
                    Text(
                        text = "Выбрано: ${state.selectedSubjects.joinToString()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = state.customSubject,
                        onValueChange = viewModel::setCustomSubject,
                        singleLine = true,
                        label = { Text("Свой предмет") }
                    )
                    TextButton(onClick = viewModel::addCustomSubject) {
                        Text("Добавить")
                    }
                }

                Text("Выберите ставку за час:")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.recommendedRatesRubles.forEach { rate ->
                        FilterChip(
                            selected = state.selectedRateRubles == rate,
                            onClick = { viewModel.selectRate(rate) },
                            label = { Text("₽$rate") }
                        )
                    }
                }
                Text(text = "Ставка: ₽${state.selectedRateRubles}/ч")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = state.customRateInput,
                        onValueChange = viewModel::setCustomRate,
                        singleLine = true,
                        label = { Text("Своя ставка") }
                    )
                    TextButton(onClick = viewModel::applyCustomRate) {
                        Text("Применить")
                    }
                }

                Text(
                    text = "Хотите подтянуть занятия из Google Календаря?",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                viewModel.completeOnboarding()
            }) {
                Text("Позже")
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.completeOnboarding()
                        onImportLessons()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Импортировать из Google Календаря")
                }
            }
        }
    )
}
