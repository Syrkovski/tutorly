package com.tutorly.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.R
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.models.Student
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@HiltViewModel
class StudentEditorVM @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: StudentsRepository
) : ViewModel() {
    private val id: Long? = savedStateHandle.get<Long>("studentId")
    private val editTargetName: String? = savedStateHandle.get<String>("editTarget")
    val editTarget: StudentEditTarget? = editTargetName?.let { target ->
        runCatching { StudentEditTarget.valueOf(target) }.getOrNull()
    }
    var formState by mutableStateOf(StudentEditorFormState())
        private set
    private var loadedStudent: Student? = null

    init {
        id?.let {
            viewModelScope.launch {
                repo.observeStudent(it).collect { s ->
                    if (s != null) {
                        loadedStudent = s
                        formState = StudentEditorFormState(
                            studentId = s.id,
                            name = s.name,
                            phone = s.phone.orEmpty(),
                            messenger = s.messenger.orEmpty(),
                            rate = formatRateInput(s.rateCents),
                            subject = s.subject.orEmpty(),
                            grade = s.grade.orEmpty(),
                            note = s.note.orEmpty(),
                            isArchived = s.isArchived,
                            isActive = s.active,
                        )
                    }
                }
            }
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
        val parsedRate = parseRateInput(rateInput)
        val normalizedRate = if (rateInput.isNotEmpty() && parsedRate != null) {
            formatRateInput(parsedRate)
        } else {
            rateInput
        }
        val trimmedSubject = formState.subject.trim().ifBlank { null }
        val trimmedGrade = formState.grade.trim().ifBlank { null }
        val trimmedNote = formState.note.trim().ifBlank { null }

        viewModelScope.launch {
            formState = formState.copy(
                isSaving = true,
                name = trimmedName,
                phone = trimmedPhone.orEmpty(),
                messenger = trimmedMessenger.orEmpty(),
                rate = normalizedRate,
                subject = trimmedSubject.orEmpty(),
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
                    onSaved(newId)
                }
                .onFailure { throwable ->
                    formState = formState.copy(isSaving = false)
                    onError(throwable.message ?: "")
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentEditorDialog(
    onDismiss: () -> Unit,
    onSaved: (Long) -> Unit,
    vm: StudentEditorVM = hiltViewModel(),
) {
    val formState = vm.formState
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    fun hideSheetAndThen(action: () -> Unit) {
        if (sheetState.isVisible) {
            coroutineScope.launch {
                sheetState.hide()
            }.invokeOnCompletion { action() }
        } else {
            action()
        }
    }

    BackHandler(enabled = !formState.isSaving) {
        hideSheetAndThen(onDismiss)
    }

    val attemptSave: () -> Unit = {
        if (!formState.isSaving) {
            vm.save(
                onSaved = { id ->
                    hideSheetAndThen { onSaved(id) }
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

    ModalBottomSheet(
        onDismissRequest = {
            if (!formState.isSaving) {
                hideSheetAndThen(onDismiss)
            }
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        ) { innerPadding ->
            StudentEditorSheet(
                state = formState,
                onNameChange = vm::onNameChange,
                onPhoneChange = vm::onPhoneChange,
                onMessengerChange = vm::onMessengerChange,
                onRateChange = vm::onRateChange,
                onSubjectChange = vm::onSubjectChange,
                onGradeChange = vm::onGradeChange,
                onNoteChange = vm::onNoteChange,
                onArchivedChange = vm::onArchivedChange,
                onActiveChange = vm::onActiveChange,
                onCancel = {
                    if (!formState.isSaving) {
                        hideSheetAndThen(onDismiss)
                    }
                },
                onSave = attemptSave,
                modifier = Modifier.padding(innerPadding),
                editTarget = vm.editTarget,
                initialFocus = vm.editTarget,
            )
        }
    }
}
