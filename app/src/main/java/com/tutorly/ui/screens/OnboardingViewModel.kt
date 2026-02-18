package com.tutorly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.data.calendar.GoogleCalendarImportCandidate
import com.tutorly.data.calendar.GoogleCalendarMigrationService
import com.tutorly.domain.repo.SubjectPresetsRepository
import com.tutorly.domain.repo.UserProfileRepository
import com.tutorly.models.SubjectPreset
import com.tutorly.ui.subject.SubjectSuggestionDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val subjectPresetsRepository: SubjectPresetsRepository,
    private val calendarMigrationService: GoogleCalendarMigrationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun toggleSubject(subject: String) {
        _uiState.update { current ->
            val updated = current.selectedSubjects.toMutableSet().apply {
                if (contains(subject)) remove(subject) else add(subject)
            }
            current.copy(selectedSubjects = updated)
        }
    }

    fun setCustomSubject(value: String) {
        _uiState.update { current -> current.copy(customSubject = value) }
    }

    fun addCustomSubject() {
        val subject = _uiState.value.customSubject.trim()
        if (subject.isBlank()) return
        _uiState.update { current ->
            current.copy(
                suggestedSubjects = addSubjectIfMissing(current.suggestedSubjects, subject),
                selectedSubjects = current.selectedSubjects + subject,
                customSubject = ""
            )
        }
    }

    fun selectRate(rateRubles: Int) {
        _uiState.update { current -> current.copy(selectedRateRubles = rateRubles) }
    }

    fun setCustomRate(value: String) {
        _uiState.update { current -> current.copy(customRateInput = value.filter { it.isDigit() }) }
    }

    fun applyCustomRate() {
        val value = _uiState.value.customRateInput.toIntOrNull() ?: return
        if (value <= 0) return
        _uiState.update { current ->
            current.copy(
                recommendedRatesRubles = addRateIfMissing(current.recommendedRatesRubles, value),
                selectedRateRubles = value,
                customRateInput = ""
            )
        }
    }

    fun goNext() {
        _uiState.update { current ->
            current.copy(step = (current.step + 1).coerceAtMost(ONBOARDING_LAST_STEP))
        }
    }

    fun goBack() {
        _uiState.update { current ->
            current.copy(step = (current.step - 1).coerceAtLeast(ONBOARDING_FIRST_STEP))
        }
    }

    fun loadCalendarCandidates() {
        _uiState.update { current ->
            current.copy(isCandidatesLoading = true, importError = null)
        }
        viewModelScope.launch {
            runCatching { calendarMigrationService.fetchImportCandidates() }
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(
                            isCandidatesLoading = false,
                            isCandidateDialogVisible = true,
                            calendarImportCandidates = it,
                            selectedCandidateNames = it.mapTo(linkedSetOf()) { c -> c.studentName }
                        )
                    }
                }
                .onFailure {
                    _uiState.update { current ->
                        current.copy(
                            isCandidatesLoading = false,
                            importError = "Не удалось импортировать занятия. Проверьте доступ к календарю."
                        )
                    }
                }
        }
    }

    fun dismissCandidateDialog() {
        _uiState.update { current -> current.copy(isCandidateDialogVisible = false) }
    }

    fun toggleCalendarCandidate(name: String) {
        _uiState.update { current ->
            val updated = current.selectedCandidateNames.toMutableSet().apply {
                if (contains(name)) remove(name) else add(name)
            }
            current.copy(selectedCandidateNames = updated)
        }
    }

    fun selectAllCalendarCandidates(selectAll: Boolean) {
        _uiState.update { current ->
            val updated = if (selectAll) {
                current.calendarImportCandidates.mapTo(linkedSetOf()) { it.studentName }
            } else {
                emptySet()
            }
            current.copy(selectedCandidateNames = updated)
        }
    }

    fun importFromGoogleCalendar() {
        val selected = _uiState.value.selectedCandidateNames
        if (selected.isEmpty()) {
            _uiState.update { current ->
                current.copy(importError = "Выберите хотя бы одного ученика для импорта")
            }
            return
        }
        _uiState.update { current ->
            current.copy(isImporting = true, importError = null)
        }
        viewModelScope.launch {
            runCatching {
                calendarMigrationService.importFromGoogleCalendar(allowedStudentNames = selected)
            }
                .onSuccess {
                    completeOnboarding()
                }
                .onFailure {
                    _uiState.update { current ->
                        current.copy(
                            isImporting = false,
                            importError = "Не удалось импортировать занятия. Проверьте доступ к календарю."
                        )
                    }
                }
        }
    }

    fun completeOnboarding() {
        val subjects = _uiState.value.selectedSubjects
        val rateCents = (_uiState.value.selectedRateRubles * 100).coerceAtLeast(0)
        viewModelScope.launch {
            if (subjects.isNotEmpty() && rateCents > 0) {
                val existing = subjectPresetsRepository.all().map { it.name.lowercase() }.toSet()
                subjects.forEach { name ->
                    if (!existing.contains(name.lowercase())) {
                        subjectPresetsRepository.upsert(
                            SubjectPreset(
                                name = name,
                                durationMinutes = DEFAULT_LESSON_DURATION_MINUTES,
                                defaultPriceCents = rateCents,
                                colorArgb = DEFAULT_SUBJECT_COLOR
                            )
                        )
                    }
                }
            }
            userProfileRepository.setOnboardingCompleted(true)
            _uiState.update { current -> current.copy(isImporting = false, importError = null) }
        }
    }
}

data class OnboardingUiState(
    val step: Int = ONBOARDING_FIRST_STEP,
    val suggestedSubjects: List<String> = SubjectSuggestionDefaults.take(8),
    val selectedSubjects: Set<String> = emptySet(),
    val customSubject: String = "",
    val recommendedRatesRubles: List<Int> = listOf(1000, 1500, 2000, 2500, 3000),
    val selectedRateRubles: Int = 1500,
    val customRateInput: String = "",
    val isCandidatesLoading: Boolean = false,
    val isCandidateDialogVisible: Boolean = false,
    val calendarImportCandidates: List<GoogleCalendarImportCandidate> = emptyList(),
    val selectedCandidateNames: Set<String> = emptySet(),
    val isImporting: Boolean = false,
    val importError: String? = null
)

private fun addSubjectIfMissing(subjects: List<String>, subject: String): List<String> {
    if (subjects.any { it.equals(subject, ignoreCase = true) }) return subjects
    return subjects + subject
}

private fun addRateIfMissing(rates: List<Int>, rate: Int): List<Int> {
    if (rate in rates) return rates
    return (rates + rate).sorted()
}

private const val ONBOARDING_FIRST_STEP: Int = 1
private const val ONBOARDING_LAST_STEP: Int = 3

private const val DEFAULT_LESSON_DURATION_MINUTES: Int = 60
private const val DEFAULT_SUBJECT_COLOR: Int = 0xFF4E998C.toInt()
