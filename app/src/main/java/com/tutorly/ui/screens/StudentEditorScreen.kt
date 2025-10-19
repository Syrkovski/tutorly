package com.tutorly.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.R
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.domain.repo.SubjectPresetsRepository
import com.tutorly.models.Student
import java.util.Locale
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@HiltViewModel
class StudentEditorVM @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repo: StudentsRepository,
    private val subjectPresetsRepository: SubjectPresetsRepository
) : ViewModel() {
    private val id: Long? = savedStateHandle.get<Long>("studentId")
    private val editTargetName: String? = savedStateHandle.get<String>("editTarget")
    var editTarget by mutableStateOf(
        editTargetName?.let { target ->
            runCatching { StudentEditTarget.valueOf(target) }.getOrNull()
        }
    )
        private set
    var formState by mutableStateOf(StudentEditorFormState())
        private set
    private var loadedStudent: Student? = null
    var subjectSuggestions by mutableStateOf(listOf<String>())
        private set
    private val suggestionLocale = Locale.getDefault()

    init {
        id?.let {
            viewModelScope.launch {
                repo.observeStudent(it).collect { s ->
                    if (s != null) {
                        loadedStudent = s
                        formState = s.toFormState()
                    }
                }
            }
        }

        refreshSubjectSuggestions()
    }

    fun resetFormToLoadedStudent() {
        loadedStudent?.let { student ->
            formState = student.toFormState()
        }
    }

    fun onNameChange(value: String) {
        formState = formState.copy(name = value, nameError = false)
    }

    fun onPhoneChange(value: String) {
        formState = formState.copy(phone = value)
    }

    fun onMessengerChange(value: String) {
        formState = formState.copy(messenger = value)
    }

    fun onRateChange(value: String) {
        formState = formState.copy(rate = value)
    }

    fun onSubjectChange(value: String) {
        formState = formState.copy(subject = value)
    }

    fun onGradeChange(value: String) {
        formState = formState.copy(grade = value)
    }

    fun onNoteChange(value: String) {
        formState = formState.copy(note = value)
    }

    fun onArchivedChange(value: Boolean) {
        formState = formState.copy(isArchived = value)
    }

    fun onActiveChange(value: Boolean) {
        formState = formState.copy(isActive = value)
    }

    fun updateEditTarget(target: StudentEditTarget?) {
        editTarget = target
        savedStateHandle["editTarget"] = target?.name
    }

    fun save(
        onSaved: (Long) -> Unit,
        onError: (String) -> Unit,
    ) {
        val trimmedName = formState.name.trim()
        if (trimmedName.isEmpty()) {
            formState = formState.copy(nameError = true)
            return
        }

        val trimmedPhone = formState.phone.trim().ifBlank { null }
        val trimmedMessenger = formState.messenger.trim().ifBlank { null }
        val rateInput = formState.rate.trim()
        val parsedRate = parseMoneyInput(rateInput)
        val normalizedRate = if (rateInput.isNotEmpty() && parsedRate != null) {
            formatMoneyInput(parsedRate)
        } else {
            rateInput
        }
        val normalizedSubject = normalizeSubjectsInput(formState.subject)
        val trimmedSubject = normalizedSubject.ifBlank { null }
        val trimmedGrade = formState.grade.trim().ifBlank { null }
        val trimmedNote = formState.note.trim().ifBlank { null }

        viewModelScope.launch {
            formState = formState.copy(
                isSaving = true,
                name = trimmedName,
                phone = trimmedPhone.orEmpty(),
                messenger = trimmedMessenger.orEmpty(),
                rate = normalizedRate,
                subject = normalizedSubject,
                grade = trimmedGrade.orEmpty(),
                note = trimmedNote.orEmpty(),
                nameError = false
            )
            val student = (loadedStudent?.copy(
                name = trimmedName,
                phone = trimmedPhone,
                messenger = trimmedMessenger,
                rateCents = parsedRate,
                subject = trimmedSubject,
                grade = trimmedGrade,
                note = trimmedNote,
                isArchived = formState.isArchived,
                active = formState.isActive,
            ) ?: Student(
                name = trimmedName,
                phone = trimmedPhone,
                messenger = trimmedMessenger,
                rateCents = parsedRate,
                subject = trimmedSubject,
                grade = trimmedGrade,
                note = trimmedNote,
                isArchived = formState.isArchived,
                active = formState.isActive,
            )).copy(updatedAt = Instant.now())

            runCatching { repo.upsert(student) }
                .onSuccess { newId ->
                    loadedStudent = student.copy(id = newId)
                    formState = formState.copy(isSaving = false)
                    refreshSubjectSuggestions()
                    onSaved(newId)
                }
                .onFailure { throwable ->
                    formState = formState.copy(isSaving = false)
                    onError(throwable.message ?: "")
                }
        }
    }

    private fun refreshSubjectSuggestions() {
        viewModelScope.launch {
            val presets = runCatching { subjectPresetsRepository.all() }.getOrDefault(emptyList())
            val presetNames = presets
                .map { formatSubjectName(it.name, suggestionLocale) }
                .filter { it.isNotEmpty() }
            val studentSubjects = runCatching { repo.allActive() }
                .getOrDefault(emptyList())
                .flatMap { parseSubjectNames(it.subject, suggestionLocale) }
            val merged = (presetNames + studentSubjects)
            val seen = mutableSetOf<String>()
            val deduped = mutableListOf<String>()
            merged.forEach { name ->
                val key = name.lowercase(suggestionLocale)
                if (name.isNotEmpty() && seen.add(key)) {
                    deduped += name
                }
            }
            subjectSuggestions = deduped
        }
    }

    private fun Student.toFormState(): StudentEditorFormState = StudentEditorFormState(
        studentId = id,
        name = name,
        phone = phone.orEmpty(),
        messenger = messenger.orEmpty(),
        rate = formatMoneyInput(rateCents),
        subject = normalizeSubjectsInput(subject.orEmpty()),
        grade = grade.orEmpty(),
        note = note.orEmpty(),
        isArchived = isArchived,
        isActive = active,
    )

    private fun normalizeSubjectsInput(input: String): String {
        return input.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(Locale.getDefault()) }
            .joinToString(separator = ", ")
    }
}

@Composable
fun StudentEditorDialog(
    onDismiss: () -> Unit,
    onSaved: (Long) -> Unit,
    vm: StudentEditorVM = hiltViewModel(),
) {
    val formState = vm.formState
    val editTarget = vm.editTarget
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val subjectSuggestions = vm.subjectSuggestions

    val currentOnDismiss by androidx.compose.runtime.rememberUpdatedState(onDismiss)
    val currentOnSaved by androidx.compose.runtime.rememberUpdatedState(onSaved)

    val attemptSave: () -> Unit = {
        if (!formState.isSaving) {
            vm.save(
                onSaved = { id ->
                    currentOnSaved(id)
                },
                onError = { message ->
                    val text = if (message.isNotBlank()) {
                        message
                    } else {
                        context.getString(R.string.student_editor_save_error)
                    }
                    coroutineScope.launch { snackbarHostState.showSnackbar(text) }
                }
            )
        }
    }

    StudentEditorSheet(
        state = formState,
        onNameChange = vm::onNameChange,
        onPhoneChange = vm::onPhoneChange,
        onMessengerChange = vm::onMessengerChange,
        onRateChange = vm::onRateChange,
        onSubjectChange = vm::onSubjectChange,
        onGradeChange = vm::onGradeChange,
        onNoteChange = vm::onNoteChange,
        onSave = attemptSave,
        onDismiss = {
            if (!formState.isSaving) {
                currentOnDismiss()
            }
        },
        editTarget = editTarget,
        initialFocus = editTarget,
        snackbarHostState = snackbarHostState,
        subjectSuggestions = subjectSuggestions
    )
}
