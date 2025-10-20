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
import com.tutorly.models.SubjectPreset
import com.tutorly.models.Student
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
    var subjectPresets by mutableStateOf<List<SubjectPreset>>(emptyList())
        private set
    private var loadedStudent: Student? = null

    init {
        viewModelScope.launch {
            subjectPresets = subjectPresetsRepository.all()
        }
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

        val normalizedName = titleCaseWords(trimmedName)
        val trimmedPhone = formState.phone.trim().ifBlank { null }
        val trimmedMessenger = formState.messenger.trim().ifBlank { null }
        val rateInput = formState.rate.trim()
        val parsedRate = parseMoneyInput(rateInput)
        val normalizedRate = if (rateInput.isNotEmpty() && parsedRate != null) {
            formatMoneyInput(parsedRate)
        } else {
            rateInput
        }
        val trimmedSubject = formState.subject.trim().ifBlank { null }
        val normalizedSubject = normalizeSubject(trimmedSubject)
        val normalizedGrade = normalizeGrade(formState.grade)
        val trimmedNote = formState.note.trim().ifBlank { null }

        viewModelScope.launch {
            formState = formState.copy(
                isSaving = true,
                name = normalizedName,
                phone = trimmedPhone.orEmpty(),
                messenger = trimmedMessenger.orEmpty(),
                rate = normalizedRate,
                subject = normalizedSubject.orEmpty(),
                grade = normalizedGrade.orEmpty(),
                note = trimmedNote.orEmpty(),
                nameError = false
            )
            val student = (loadedStudent?.copy(
                name = normalizedName,
                phone = trimmedPhone,
                messenger = trimmedMessenger,
                rateCents = parsedRate,
                subject = normalizedSubject,
                grade = normalizedGrade,
                note = trimmedNote,
                isArchived = formState.isArchived,
                active = formState.isActive,
            ) ?: Student(
                name = normalizedName,
                phone = trimmedPhone,
                messenger = trimmedMessenger,
                rateCents = parsedRate,
                subject = normalizedSubject,
                grade = normalizedGrade,
                note = trimmedNote,
                isArchived = formState.isArchived,
                active = formState.isActive,
            )).copy(updatedAt = Instant.now())

            runCatching { repo.upsert(student) }
                .onSuccess { newId ->
                    loadedStudent = student.copy(id = newId)
                    formState = formState.copy(isSaving = false)
                    onSaved(newId)
                }
                .onFailure { throwable ->
                    formState = formState.copy(isSaving = false)
                    onError(throwable.message ?: "")
                }
        }
    }

    private fun Student.toFormState(): StudentEditorFormState = StudentEditorFormState(
        studentId = id,
        name = titleCaseWords(name),
        phone = phone.orEmpty(),
        messenger = messenger.orEmpty(),
        rate = formatMoneyInput(rateCents),
        subject = normalizeSubject(subject).orEmpty(),
        grade = normalizeGrade(grade).orEmpty(),
        note = note.orEmpty(),
        isArchived = isArchived,
        isActive = active,
    )
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
        subjectPresets = vm.subjectPresets,
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
        snackbarHostState = snackbarHostState
    )
}
