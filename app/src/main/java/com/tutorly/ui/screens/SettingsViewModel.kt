package com.tutorly.ui.screens

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.R
import com.tutorly.domain.repo.UserProfileRepository
import com.tutorly.models.AppThemePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileRepository.profile.collect { profile ->
                _uiState.update { current ->
                    current.copy(
                        workDayStart = minutesToLocalTime(profile.workDayStartMinutes),
                        workDayEnd = minutesToLocalTime(profile.workDayEndMinutes),
                        weekendDays = profile.weekendDays,
                        selectedTheme = profile.theme
                    )
                }
            }
        }
    }

    fun updateWorkDayStart(time: LocalTime) {
        val newStartMinutes = time.roundToStep().coerceIn(0, MAX_START_MINUTE)
        val currentEndMinutes = _uiState.value.workDayEnd.roundToStep().coerceIn(newStartMinutes + SLOT_STEP_MINUTES, MAX_END_MINUTE)
        val adjustedEndMinutes = if (newStartMinutes >= currentEndMinutes) {
            (newStartMinutes + SLOT_STEP_MINUTES).coerceAtMost(MAX_END_MINUTE)
        } else {
            currentEndMinutes
        }
        val newStart = minutesToLocalTime(newStartMinutes)
        val newEnd = minutesToLocalTime(adjustedEndMinutes)
        _uiState.update { current ->
            current.copy(workDayStart = newStart, workDayEnd = newEnd)
        }
        viewModelScope.launch {
            userProfileRepository.updateWorkDay(newStartMinutes, adjustedEndMinutes)
        }
    }

    fun updateWorkDayEnd(time: LocalTime) {
        val startMinutes = _uiState.value.workDayStart.roundToStep().coerceIn(0, MAX_START_MINUTE)
        val desiredEnd = time.roundToStep()
        val endMinutes = desiredEnd.coerceIn(startMinutes + SLOT_STEP_MINUTES, MAX_END_MINUTE)
        val endTime = minutesToLocalTime(endMinutes)
        _uiState.update { current ->
            current.copy(workDayEnd = endTime)
        }
        viewModelScope.launch {
            userProfileRepository.updateWorkDay(startMinutes, endMinutes)
        }
    }

    fun toggleWeekend(day: DayOfWeek) {
        val updated = _uiState.value.weekendDays.toMutableSet().apply {
            if (contains(day)) {
                remove(day)
            } else {
                add(day)
            }
        }
        _uiState.update { current -> current.copy(weekendDays = updated) }
        viewModelScope.launch { userProfileRepository.setWeekendDays(updated) }
    }

    fun selectTheme(option: ThemeOption) {
        _uiState.update { current ->
            current.copy(selectedTheme = option.preset)
        }
        viewModelScope.launch { userProfileRepository.setTheme(option.preset) }
    }

    private fun minutesToLocalTime(minutes: Int): LocalTime {
        val hours = minutes / 60
        val mins = minutes % 60
        return LocalTime.of(hours, mins)
    }

    private fun LocalTime.roundToStep(): Int {
        val totalMinutes = hour * 60 + minute
        val rounded = (totalMinutes / SLOT_STEP_MINUTES) * SLOT_STEP_MINUTES
        return rounded
    }
}

data class SettingsUiState(
    val workDayStart: LocalTime = LocalTime.of(9, 0),
    val workDayEnd: LocalTime = LocalTime.of(22, 0),
    val weekendDays: Set<DayOfWeek> = emptySet(),
    val selectedTheme: AppThemePreset = AppThemePreset.ORIGINAL,
    val availableThemes: List<ThemeOption> = ThemeOption.defaults()
)

data class ThemeOption(
    val preset: AppThemePreset,
    @StringRes val labelRes: Int,
    val previewColor: Long
) {
    companion object {
        fun defaults(): List<ThemeOption> = listOf(
            ThemeOption(
                preset = AppThemePreset.ORIGINAL,
                labelRes = R.string.settings_theme_original,
                previewColor = 0xFF4E998C
            ),
            ThemeOption(
                preset = AppThemePreset.PLUM,
                labelRes = R.string.settings_theme_plum,
                previewColor = 0xFF8F4AA1
            ),
            ThemeOption(
                preset = AppThemePreset.ROYAL,
                labelRes = R.string.settings_theme_royal,
                previewColor = 0xFF4D71CE
            )
        )
    }
}

private const val SLOT_STEP_MINUTES: Int = 30
private const val LAST_DAY_MINUTE: Int = 23 * 60 + 30
private const val MAX_START_MINUTE: Int = LAST_DAY_MINUTE - SLOT_STEP_MINUTES
private const val MAX_END_MINUTE: Int = LAST_DAY_MINUTE
