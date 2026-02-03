package com.tutorly.ui.screens

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.R
import com.tutorly.data.calendar.GoogleCalendarImportCandidate
import com.tutorly.data.calendar.GoogleCalendarImportResult
import com.tutorly.data.calendar.GoogleCalendarMigrationService
import com.tutorly.domain.repo.UserProfileRepository
import com.tutorly.models.AppThemePreset
import com.tutorly.models.UserProfile
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
    private val userProfileRepository: UserProfileRepository,
    private val calendarMigrationService: GoogleCalendarMigrationService
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
                        workDayStartMinutes = profile.workDayStartMinutes,
                        workDayEndMinutes = profile.workDayEndMinutes,
                        workDayEndExtendsToNextDay = profile.workDayEndMinutes == MINUTES_IN_DAY,
                        weekendDays = profile.weekendDays,
                        selectedTheme = profile.theme
                    )
                }
            }
        }
    }

    fun updateWorkDayStart(time: LocalTime) {
        val newStartMinutes = time.roundToStep().coerceIn(0, MAX_START_MINUTE)
        val currentEndMinutes = _uiState.value.workDayEndMinutes
            .coerceIn(newStartMinutes + SLOT_STEP_MINUTES, MAX_END_MINUTE)
        val adjustedEndMinutes = if (newStartMinutes >= currentEndMinutes) {
            (newStartMinutes + SLOT_STEP_MINUTES).coerceAtMost(MAX_END_MINUTE)
        } else {
            currentEndMinutes
        }
        val newStart = minutesToLocalTime(newStartMinutes)
        val newEnd = minutesToLocalTime(adjustedEndMinutes)
        _uiState.update { current ->
            current.copy(
                workDayStart = newStart,
                workDayEnd = newEnd,
                workDayStartMinutes = newStartMinutes,
                workDayEndMinutes = adjustedEndMinutes,
                workDayEndExtendsToNextDay = adjustedEndMinutes == MINUTES_IN_DAY
            )
        }
        viewModelScope.launch {
            userProfileRepository.updateWorkDay(newStartMinutes, adjustedEndMinutes)
        }
    }

    fun updateWorkDayEnd(time: LocalTime) {
        val startMinutes = _uiState.value.workDayStartMinutes
            .coerceIn(0, MAX_START_MINUTE)
        val desiredEnd = if (time == LocalTime.MIDNIGHT) {
            MINUTES_IN_DAY
        } else {
            time.roundToStep()
        }
        val endMinutes = desiredEnd.coerceIn(startMinutes + SLOT_STEP_MINUTES, MAX_END_MINUTE)
        val endTime = minutesToLocalTime(endMinutes)
        _uiState.update { current ->
            current.copy(
                workDayEnd = endTime,
                workDayEndMinutes = endMinutes,
                workDayEndExtendsToNextDay = endMinutes == MINUTES_IN_DAY
            )
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

    fun loadCalendarCandidates() {
        if (_uiState.value.isCalendarImporting || _uiState.value.isCalendarCandidatesLoading) return
        _uiState.update { current ->
            current.copy(
                isCalendarCandidatesLoading = true,
                calendarImportError = null,
                calendarImportResult = null
            )
        }
        viewModelScope.launch {
            runCatching { calendarMigrationService.fetchImportCandidates() }
                .onSuccess { candidates ->
                    val selected = candidates.mapTo(linkedSetOf()) { it.studentName }
                    _uiState.update { current ->
                        current.copy(
                            isCalendarCandidatesLoading = false,
                            calendarImportCandidates = candidates,
                            calendarImportSelectedNames = selected,
                            isCalendarCandidateDialogVisible = true
                        )
                    }
                }
                .onFailure {
                    _uiState.update { current ->
                        current.copy(
                            isCalendarCandidatesLoading = false,
                            calendarImportError = CalendarImportError.GENERIC
                        )
                    }
                }
        }
    }

    fun toggleCalendarCandidate(name: String) {
        _uiState.update { current ->
            val updated = current.calendarImportSelectedNames.toMutableSet().apply {
                if (contains(name)) remove(name) else add(name)
            }
            current.copy(calendarImportSelectedNames = updated)
        }
    }

    fun selectAllCalendarCandidates(selectAll: Boolean) {
        _uiState.update { current ->
            val updated = if (selectAll) {
                current.calendarImportCandidates.mapTo(linkedSetOf()) { it.studentName }
            } else {
                emptySet()
            }
            current.copy(calendarImportSelectedNames = updated)
        }
    }

    fun dismissCalendarCandidatesDialog() {
        _uiState.update { current ->
            current.copy(isCalendarCandidateDialogVisible = false)
        }
    }

    fun confirmCalendarImport() {
        val selectedNames = _uiState.value.calendarImportSelectedNames
        if (selectedNames.isEmpty()) {
            _uiState.update { current ->
                current.copy(calendarImportError = CalendarImportError.EMPTY_SELECTION)
            }
            return
        }
        startCalendarImport(selectedNames)
    }

    private fun startCalendarImport(selectedNames: Set<String>) {
        if (_uiState.value.isCalendarImporting) return
        _uiState.update { current ->
            current.copy(
                isCalendarImporting = true,
                calendarImportError = null,
                calendarImportResult = null
            )
        }
        viewModelScope.launch {
            runCatching {
                calendarMigrationService.importFromGoogleCalendar(
                    allowedStudentNames = selectedNames
                )
            }.onSuccess { result ->
                _uiState.update { current ->
                    current.copy(
                        isCalendarImporting = false,
                        calendarImportResult = result,
                        calendarImportError = null,
                        isCalendarCandidateDialogVisible = false
                    )
                }
            }.onFailure {
                _uiState.update { current ->
                    current.copy(
                        isCalendarImporting = false,
                        calendarImportError = CalendarImportError.GENERIC
                    )
                }
            }
        }
    }

    fun onCalendarPermissionDenied() {
        _uiState.update { current ->
            current.copy(
                isCalendarImporting = false,
                calendarImportError = CalendarImportError.PERMISSION_DENIED
            )
        }
    }

    private fun minutesToLocalTime(minutes: Int): LocalTime {
        val clamped = minutes % MINUTES_IN_DAY
        val hours = clamped / 60
        val mins = clamped % 60
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
    val workDayStartMinutes: Int = UserProfile.DEFAULT_WORK_DAY_START,
    val workDayEndMinutes: Int = UserProfile.DEFAULT_WORK_DAY_END,
    val workDayEndExtendsToNextDay: Boolean = false,
    val weekendDays: Set<DayOfWeek> = emptySet(),
    val selectedTheme: AppThemePreset = AppThemePreset.ORIGINAL,
    val availableThemes: List<ThemeOption> = ThemeOption.defaults(),
    val isCalendarImporting: Boolean = false,
    val isCalendarCandidatesLoading: Boolean = false,
    val isCalendarCandidateDialogVisible: Boolean = false,
    val calendarImportCandidates: List<GoogleCalendarImportCandidate> = emptyList(),
    val calendarImportSelectedNames: Set<String> = emptySet(),
    val calendarImportResult: GoogleCalendarImportResult? = null,
    val calendarImportError: CalendarImportError? = null
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

enum class CalendarImportError(@StringRes val messageRes: Int) {
    PERMISSION_DENIED(R.string.settings_calendar_import_permission_denied),
    EMPTY_SELECTION(R.string.settings_calendar_import_select_at_least_one),
    GENERIC(R.string.settings_calendar_import_error)
}

private const val SLOT_STEP_MINUTES: Int = 30
private const val MINUTES_IN_DAY: Int = 24 * 60
private const val MAX_END_MINUTE: Int = MINUTES_IN_DAY
private const val MAX_START_MINUTE: Int = MAX_END_MINUTE - SLOT_STEP_MINUTES
