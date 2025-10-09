package com.tutorly.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.PaymentsRepository
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.models.Lesson
import com.tutorly.models.Student
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StudentDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    studentsRepository: StudentsRepository,
    paymentsRepository: PaymentsRepository,
    lessonsRepository: LessonsRepository
) : ViewModel() {

    private val studentId: Long = savedStateHandle.get<Long>("studentId")
        ?: error("studentId is required")

    private val studentFlow = studentsRepository.observeStudent(studentId)
    private val hasDebtFlow = paymentsRepository.observeHasDebt(studentId)
    private val totalDebtFlow = paymentsRepository.observeTotalDebt(studentId)
    private val lessonsFlow = lessonsRepository.observeByStudent(studentId)

    val uiState: StateFlow<UiState> = combine(
        studentFlow,
        hasDebtFlow,
        totalDebtFlow,
        lessonsFlow
    ) { student, hasDebt, totalDebt, lessons ->
        UiState(
            isLoading = false,
            student = student,
            hasDebt = hasDebt,
            totalDebtCents = totalDebt,
            lessons = lessons
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    data class UiState(
        val isLoading: Boolean = true,
        val student: Student? = null,
        val hasDebt: Boolean = false,
        val totalDebtCents: Long = 0L,
        val lessons: List<Lesson> = emptyList()
    )
}
