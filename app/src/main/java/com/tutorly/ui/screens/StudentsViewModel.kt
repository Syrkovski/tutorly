package com.tutorly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.model.StudentProfile
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.domain.repo.SubjectPresetsRepository
import com.tutorly.models.Student
import com.tutorly.models.SubjectPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Instant
import java.time.Duration
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf

@HiltViewModel
class StudentsViewModel @Inject constructor(
    private val repo: StudentsRepository,
    private val lessonsRepository: LessonsRepository,
    private val subjectPresetsRepository: SubjectPresetsRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _editorFormState = MutableStateFlow(StudentEditorFormState())
    val editorFormState: StateFlow<StudentEditorFormState> = _editorFormState.asStateFlow()

    private val debtObservers = mutableMapOf<Long, Job>()
    private val _debts = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    private val lessonObservers = mutableMapOf<Long, Job>()
    private val _latestLessons = MutableStateFlow<Map<Long, LessonSnapshot?>>(emptyMap())
    private val _subjects = MutableStateFlow<Map<Long, SubjectPreset>>(emptyMap())
    private val _selectedStudentId = MutableStateFlow<Long?>(null)

    private val studentsStream = _query
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { repo.observeStudents(it) }
        .onEach {
            syncDebtObservers(it)
            syncLessonObservers(it)
        }

    val students: StateFlow<List<StudentListItem>> = combine(
        studentsStream,
        _debts,
        _latestLessons,
        _subjects
    ) { students, debts, lessons, subjects ->
        students.map { student ->
            val snapshot = lessons[student.id]
            StudentListItem(
                student = student,
                hasDebt = debts[student.id] == true,
                profile = buildProfile(student, snapshot, subjects)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedStudentId: StateFlow<Long?> = _selectedStudentId.asStateFlow()

    val profileUiState: StateFlow<StudentProfileUiState> = _selectedStudentId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(StudentProfileUiState.Hidden)
            } else {
                repo.observeStudentProfile(id)
                    .map { profile ->
                        if (profile != null) {
                            StudentProfileUiState.Content(profile)
                        } else {
                            StudentProfileUiState.Error
                        }
                    }
                    .onStart { emit(StudentProfileUiState.Loading) }
                    .catch { emit(StudentProfileUiState.Error) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StudentProfileUiState.Hidden
        )

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun openStudentProfile(studentId: Long) {
        _selectedStudentId.value = studentId
    }

    fun clearSelectedStudent() {
        _selectedStudentId.value = null
    }

    fun startStudentCreation(
        name: String = "",
        phone: String = "",
        messenger: String = "",
        subject: String = "",
        grade: String = "",
        note: String = "",
        isArchived: Boolean = false,
        isActive: Boolean = true,
    ) {
        _editorFormState.value = StudentEditorFormState(
            name = name,
            phone = phone,
            messenger = messenger,
            subject = subject,
            grade = grade,
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
        val trimmedSubject = state.subject.trim().ifBlank { null }
        val trimmedGrade = state.grade.trim().ifBlank { null }
        val trimmedNote = state.note.trim().ifBlank { null }

        viewModelScope.launch {
            _editorFormState.update {
                it.copy(
                    name = trimmedName,
                    phone = trimmedPhone.orEmpty(),
                    messenger = trimmedMessenger.orEmpty(),
                    subject = trimmedSubject.orEmpty(),
                    grade = trimmedGrade.orEmpty(),
                    note = trimmedNote.orEmpty(),
                    nameError = false,
                    isSaving = true
                )
            }

            val student = Student(
                name = trimmedName,
                phone = trimmedPhone,
                messenger = trimmedMessenger,
                subject = trimmedSubject,
                grade = trimmedGrade,
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

    private fun syncLessonObservers(students: List<Student>) {
        val ids = students.map { it.id }.toSet()
        val existing = lessonObservers.keys.toSet()

        val toRemove = existing - ids
        if (toRemove.isNotEmpty()) {
            toRemove.forEach { id ->
                lessonObservers.remove(id)?.cancel()
            }
            _latestLessons.update { lessons -> lessons - toRemove }
        }

        val toAdd = ids - existing
        toAdd.forEach { id ->
            lessonObservers[id] = viewModelScope.launch {
                lessonsRepository.observeByStudent(id).collect { lessons ->
                    val latest = lessons.maxByOrNull { it.startAt }
                    val snapshot = latest?.let {
                        val durationMinutes = Duration.between(it.startAt, it.endAt)
                            .toMinutes()
                            .toInt()
                            .coerceAtLeast(0)
                        LessonSnapshot(
                            lessonId = it.id,
                            subjectId = it.subjectId,
                            durationMinutes = durationMinutes,
                            priceCents = it.priceCents
                        )
                    }
                    _latestLessons.update { existingMap -> existingMap + (id to snapshot) }

                    val subjectId = latest?.subjectId
                    if (subjectId != null && _subjects.value[subjectId] == null) {
                        val preset = subjectPresetsRepository.getById(subjectId)
                        if (preset != null) {
                            _subjects.update { cache -> cache + (subjectId to preset) }
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        debtObservers.values.forEach { it.cancel() }
        debtObservers.clear()
        lessonObservers.values.forEach { it.cancel() }
        lessonObservers.clear()
    }

    data class StudentListItem(
        val student: Student,
        val hasDebt: Boolean,
        val profile: StudentCardProfile
    )

    data class StudentCardProfile(
        val subject: String?,
        val grade: String?,
        val rate: LessonRate?
    )

    data class LessonRate(
        val durationMinutes: Int,
        val priceCents: Int
    )

    private data class LessonSnapshot(
        val lessonId: Long,
        val subjectId: Long?,
        val durationMinutes: Int,
        val priceCents: Int
    )

    private fun buildProfile(
        student: Student,
        snapshot: LessonSnapshot?,
        subjects: Map<Long, SubjectPreset>
    ): StudentCardProfile {
        val subject = student.subject
            ?.takeIf { it.isNotBlank() }
            ?.trim()
            ?: snapshot?.subjectId?.let { subjectId ->
                subjects[subjectId]?.name?.takeIf { it.isNotBlank() }?.trim()
            }
            ?: student.note
                ?.lineSequence()
                ?.firstOrNull { it.isNotBlank() }
                ?.trim()

        val grade = student.grade
            ?.takeIf { it.isNotBlank() }
            ?.trim()
            ?: student.note.extractGrade()
        val rate = snapshot?.takeIf { it.priceCents > 0 && it.durationMinutes > 0 }?.let {
            LessonRate(durationMinutes = it.durationMinutes, priceCents = it.priceCents)
        }

        return StudentCardProfile(subject = subject, grade = grade, rate = rate)
    }
}

sealed interface StudentProfileUiState {
    data object Hidden : StudentProfileUiState
    data object Loading : StudentProfileUiState
    data object Error : StudentProfileUiState
    data class Content(val profile: StudentProfile) : StudentProfileUiState
}

private fun String?.extractGrade(): String? {
    if (this.isNullOrBlank()) return null
    val regex = Regex("""(?i)(\n|^|\s)(\d{1,2})\s*(класс|кл\\.?)""")
    val match = regex.find(this)
    val gradeNumber = match?.groups?.get(2)?.value
    return gradeNumber?.let { "$it класс" }
}
