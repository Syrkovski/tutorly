package com.tutorly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val subjectPresetsRepository: SubjectPresetsRepository
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
                selectedRateRubles = value,
                customRateInput = ""
            )
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
        }
    }
}

data class OnboardingUiState(
    val suggestedSubjects: List<String> = SubjectSuggestionDefaults.take(12),
    val selectedSubjects: Set<String> = emptySet(),
    val customSubject: String = "",
    val recommendedRatesRubles: List<Int> = listOf(1000, 1500, 2000, 2500, 3000),
    val selectedRateRubles: Int = 1500,
    val customRateInput: String = ""
)

private const val DEFAULT_LESSON_DURATION_MINUTES: Int = 60
private const val DEFAULT_SUBJECT_COLOR: Int = 0xFF4E998C.toInt()
