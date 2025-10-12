package com.tutorly.ui.screens

import com.tutorly.domain.model.StudentProfile

sealed interface StudentProfileUiState {
    data object Hidden : StudentProfileUiState
    data object Loading : StudentProfileUiState
    data object Error : StudentProfileUiState
    data class Content(val profile: StudentProfile) : StudentProfileUiState
}
