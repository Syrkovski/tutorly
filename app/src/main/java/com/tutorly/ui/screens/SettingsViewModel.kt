package com.tutorly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.repo.UserSettingsRepository
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
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = userSettingsRepository.get()
            _uiState.update { current ->
                current.copy(
                    workDayStart = minutesToLocalTime(settings.workDayStartMinutes),
                    workDayEnd = minutesToLocalTime(settings.workDayEndMinutes)
                )
            }
        }
    }

    fun updateWorkDayStart(time: LocalTime) {
        _uiState.update { current ->
            current.copy(workDayStart = time)
        }
    }

    fun updateWorkDayEnd(time: LocalTime) {
        _uiState.update { current ->
            current.copy(workDayEnd = time)
        }
    }

    fun toggleWeekend(day: DayOfWeek) {
        _uiState.update { current ->
            val updated = current.weekendDays.toMutableSet().apply {
                if (contains(day)) {
                    remove(day)
                } else {
                    add(day)
                }
            }
            current.copy(weekendDays = updated)
        }
    }

    fun selectTheme(option: ThemeColorOption) {
        _uiState.update { current ->
            current.copy(selectedTheme = option)
        }
    }

    private fun minutesToLocalTime(minutes: Int): LocalTime {
        val hours = minutes / 60
        val mins = minutes % 60
        return LocalTime.of(hours, mins)
    }
}

data class SettingsUiState(
    val workDayStart: LocalTime = LocalTime.of(9, 0),
    val workDayEnd: LocalTime = LocalTime.of(22, 0),
    val weekendDays: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
    val selectedTheme: ThemeColorOption = ThemeColorOption.OCEAN,
    val availableThemes: List<ThemeColorOption> = ThemeColorOption.values().toList()
)

enum class ThemeColorOption(val labelRes: Int, val previewColor: Long) {
    OCEAN(labelRes = com.tutorly.R.string.settings_theme_ocean, previewColor = 0xFF1E88E5),
    FOREST(labelRes = com.tutorly.R.string.settings_theme_forest, previewColor = 0xFF2E7D32),
    SUNSET(labelRes = com.tutorly.R.string.settings_theme_sunset, previewColor = 0xFFF4511E)
}
