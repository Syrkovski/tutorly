package com.tutorly.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.repo.StudentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

@HiltViewModel
class StudentDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val studentsRepository: StudentsRepository
) : ViewModel() {

    private val studentId: Long = savedStateHandle.get<Long>("studentId")
        ?: error("studentId is required")

    val uiState: StateFlow<StudentProfileUiState> = studentsRepository
        .observeStudentProfile(studentId)
        .map { profile ->
            if (profile != null) {
                StudentProfileUiState.Content(profile)
            } else {
                StudentProfileUiState.Error
            }
        }
        .onStart { emit(StudentProfileUiState.Loading) }
        .catch { emit(StudentProfileUiState.Error) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StudentProfileUiState.Loading
        )

    fun toggleArchive(
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val currentStudent = (uiState.value as? StudentProfileUiState.Content)?.profile?.student
        if (currentStudent == null) {
            onComplete()
            return
        }

        viewModelScope.launch {
            val result = runCatching {
                val updated = currentStudent.copy(
                    isArchived = !currentStudent.isArchived,
                    updatedAt = Instant.now()
                )
                studentsRepository.upsert(updated)
            }
            result.exceptionOrNull()?.let(onError)
            onComplete()
        }
    }

    fun deleteStudent(
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            val student = studentsRepository.getById(studentId)
            if (student == null) {
                onError(IllegalStateException(""))
                return@launch
            }

            runCatching { studentsRepository.delete(student) }
                .onSuccess { onSuccess() }
                .onFailure { throwable -> onError(throwable) }
        }
    }
}
