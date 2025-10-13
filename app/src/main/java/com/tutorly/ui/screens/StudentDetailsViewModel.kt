package com.tutorly.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.models.Student
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StudentDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val studentsRepository: StudentsRepository
) : ViewModel() {

    private val studentId: Long = savedStateHandle.get<Long>("studentId")
        ?: error("studentId is required")

    private val _editorFormState = MutableStateFlow(StudentEditorFormState())
    val editorFormState: StateFlow<StudentEditorFormState> = _editorFormState.asStateFlow()

    private val _isEditorVisible = MutableStateFlow(false)
    val isEditorVisible: StateFlow<Boolean> = _isEditorVisible.asStateFlow()

    private val _editorTarget = MutableStateFlow<StudentEditTarget?>(null)
    val editorTarget: StateFlow<StudentEditTarget?> = _editorTarget.asStateFlow()

    private val _editorInitialFocus = MutableStateFlow<StudentEditTarget?>(null)
    val editorInitialFocus: StateFlow<StudentEditTarget?> = _editorInitialFocus.asStateFlow()

    private var loadedStudent: Student? = null

    val uiState: StateFlow<StudentProfileUiState> = studentsRepository
        .observeStudentProfile(studentId)
        .onEach { profile ->
            loadedStudent = profile?.student
        }
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

    fun openEditor(target: StudentEditTarget) {
        val student = loadedStudent ?: return
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
        _editorTarget.value = target
        _editorInitialFocus.value = target
        _isEditorVisible.value = true
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

    fun submitEditor(
        onSuccess: (Long, String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val student = loadedStudent ?: return
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

            val updatedStudent = student.copy(
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

            runCatching { studentsRepository.upsert(updatedStudent) }
                .onSuccess { id ->
                    loadedStudent = updatedStudent.copy(id = id)
                    _editorFormState.update { it.copy(isSaving = false) }
                    onSuccess(id, trimmedName)
                }
                .onFailure { throwable ->
                    _editorFormState.update { it.copy(isSaving = false) }
                    onError(throwable.message ?: "")
                }
        }
    }

    fun onEditorDismissed() {
        _isEditorVisible.value = false
        _editorTarget.value = null
        _editorInitialFocus.value = null
        _editorFormState.value = StudentEditorFormState()
    }
}
