package com.tutorly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.models.Student
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

@HiltViewModel
class StudentsViewModel @Inject constructor(
    private val repo: StudentsRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _editorFormState = MutableStateFlow(StudentEditorFormState())
    val editorFormState: StateFlow<StudentEditorFormState> = _editorFormState.asStateFlow()

    private val debtObservers = mutableMapOf<Long, Job>()
    private val _debts = MutableStateFlow<Map<Long, Boolean>>(emptyMap())

    private val studentsStream = _query
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { repo.observeStudents(it) }
        .onEach { syncDebtObservers(it) }

    val students: StateFlow<List<StudentListItem>> = combine(studentsStream, _debts) { students, debts ->
        students.map { student ->
            StudentListItem(student, debts[student.id] == true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun startStudentCreation(
        name: String = "",
        phone: String = "",
        messenger: String = "",
        note: String = "",
        isArchived: Boolean = false,
        isActive: Boolean = true,
    ) {
        _editorFormState.value = StudentEditorFormState(
            name = name,
            phone = phone,
            messenger = messenger,
            note = note,
            isArchived = isArchived,
            isActive = isActive
        )
    }

    fun resetStudentForm() {
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

    fun onEditorNoteChange(value: String) {
        _editorFormState.update { it.copy(note = value) }
    }

    fun onEditorArchivedChange(value: Boolean) {
        _editorFormState.update { it.copy(isArchived = value) }
    }

    fun onEditorActiveChange(value: Boolean) {
        _editorFormState.update { it.copy(isActive = value) }
    }

    fun submitNewStudent(
        onSuccess: (Long, String) -> Unit,
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
        val trimmedNote = state.note.trim().ifBlank { null }

        viewModelScope.launch {
            _editorFormState.update {
                it.copy(
                    name = trimmedName,
                    phone = trimmedPhone.orEmpty(),
                    messenger = trimmedMessenger.orEmpty(),
                    note = trimmedNote.orEmpty(),
                    nameError = false,
                    isSaving = true
                )
            }

            val student = Student(
                name = trimmedName,
                phone = trimmedPhone,
                messenger = trimmedMessenger,
                note = trimmedNote,
                isArchived = state.isArchived,
                active = state.isActive,
                updatedAt = Instant.now()
            )

            runCatching { repo.upsert(student) }
                .onSuccess { newId ->
                    _editorFormState.value = StudentEditorFormState()
                    onSuccess(newId, trimmedName)
                }
                .onFailure { throwable ->
                    _editorFormState.update { it.copy(isSaving = false) }
                    onError(throwable.message ?: "")
                }
        }
    }

    private fun syncDebtObservers(students: List<Student>) {
        val ids = students.map { it.id }.toSet()
        val existing = debtObservers.keys.toSet()

        val toRemove = existing - ids
        if (toRemove.isNotEmpty()) {
            toRemove.forEach { id ->
                debtObservers.remove(id)?.cancel()
            }
            _debts.update { debts -> debts - toRemove }
        }

        val toAdd = ids - existing
        toAdd.forEach { id ->
            debtObservers[id] = viewModelScope.launch {
                repo.observeHasDebt(id).collect { hasDebt ->
                    _debts.update { debts -> debts + (id to hasDebt) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        debtObservers.values.forEach { it.cancel() }
        debtObservers.clear()
    }

    data class StudentListItem(
        val student: Student,
        val hasDebt: Boolean
    )
}
