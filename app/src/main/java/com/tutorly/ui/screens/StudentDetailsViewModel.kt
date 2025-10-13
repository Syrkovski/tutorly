package com.tutorly.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.model.StudentProfile
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.models.Student
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")

@HiltViewModel
class StudentDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    studentsRepository: StudentsRepository
) : ViewModel() {

    private val studentId: Long = savedStateHandle.get<Long>("studentId")
        ?: error("studentId is required")

    private val repo = studentsRepository

    private val _editorFormState = MutableStateFlow(StudentEditorFormState())
    val editorFormState: StateFlow<StudentEditorFormState> = _editorFormState.asStateFlow()
    private var editingStudent: Student? = null

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

    fun startStudentEditor(profile: StudentProfile) {
        val student = profile.student
        editingStudent = student
        _editorFormState.value = StudentEditorFormState(
            studentId = student.id,
            name = student.name,
            phone = student.phone.orEmpty(),
            messenger = student.messenger.orEmpty(),
            rate = formatMoneyInput(student.rateCents),
            subject = student.subject.orEmpty(),
            grade = student.grade.orEmpty(),
            note = student.note.orEmpty(),
            isArchived = student.isArchived,
            isActive = student.active
        )
    }

    fun resetStudentEditor() {
        editingStudent = null
        _editorFormState.value = StudentEditorFormState()
    }

    fun onEditorNameChange(value: String) {
        _editorFormState.update { it.copy(name = value, nameError = false) }
    }

    fun onEditorPhoneChange(value: String) {
        _editorFormState.update { it.copy(phone = value) }
    }

    fun onEditorMessengerChange(value: String) {
        _editorFormState.update { it.copy(messenger = value) }
    }

    fun onEditorRateChange(value: String) {
        _editorFormState.update { it.copy(rate = value) }
    }

    fun onEditorSubjectChange(value: String) {
        _editorFormState.update { it.copy(subject = value) }
    }

    fun onEditorGradeChange(value: String) {
        _editorFormState.update { it.copy(grade = value) }
    }

    fun onEditorNoteChange(value: String) {
        _editorFormState.update { it.copy(note = value) }
    }

    fun onEditorArchivedChange(value: Boolean) {
        _editorFormState.update { it.copy(isArchived = value) }
    }

    fun onEditorActiveChange(value: Boolean) {
        _editorFormState.update { it.copy(isActive = value) }
    }

    fun submitStudent(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val state = _editorFormState.value
        val trimmedName = state.name.trim()
        if (trimmedName.isEmpty()) {
            _editorFormState.update { it.copy(nameError = true) }
            return
        }

        val trimmedPhone = state.phone.trim().ifBlank { null }
        val trimmedMessenger = state.messenger.trim().ifBlank { null }
        val trimmedSubject = state.subject.trim().ifBlank { null }
        val trimmedGrade = state.grade.trim().ifBlank { null }
        val trimmedNote = state.note.trim().ifBlank { null }
        val rateInput = state.rate.trim()
        val parsedRate = parseMoneyInput(rateInput)
        val normalizedRate = if (rateInput.isNotEmpty() && parsedRate != null) {
            formatMoneyInput(parsedRate)
        } else {
            rateInput
        }

        viewModelScope.launch {
            _editorFormState.update {
                it.copy(
                    name = trimmedName,
                    phone = trimmedPhone.orEmpty(),
                    messenger = trimmedMessenger.orEmpty(),
                    rate = normalizedRate,
                    subject = trimmedSubject.orEmpty(),
                    grade = trimmedGrade.orEmpty(),
                    note = trimmedNote.orEmpty(),
                    nameError = false,
                    isSaving = true
                )
            }

            val base = editingStudent ?: state.studentId?.let { repo.getByIdSafe(it) }
            if (base == null) {
                editingStudent = null
                _editorFormState.update { it.copy(studentId = null, isSaving = false) }
                onError("")
                return@launch
            }

            val updated = base.copy(
                name = trimmedName,
                phone = trimmedPhone,
                messenger = trimmedMessenger,
                rateCents = parsedRate,
                subject = trimmedSubject,
                grade = trimmedGrade,
                note = trimmedNote,
                isArchived = state.isArchived,
                active = state.isActive,
                updatedAt = Instant.now()
            )

            runCatching { repo.upsert(updated) }
                .onSuccess { id ->
                    editingStudent = updated.copy(id = id)
                    _editorFormState.update { it.copy(isSaving = false) }
                    onSuccess(trimmedName)
                }
                .onFailure { throwable ->
                    _editorFormState.update { it.copy(isSaving = false) }
                    onError(throwable.message ?: "")
                }
        }
    }
}
