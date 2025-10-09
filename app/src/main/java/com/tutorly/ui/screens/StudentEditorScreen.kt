package com.tutorly.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
                            name = s.name,
                            phone = s.phone.orEmpty(),
                            messenger = s.messenger.orEmpty(),
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
        val trimmedNote = formState.note.trim().ifBlank { null }

        viewModelScope.launch {
            formState = formState.copy(
                isSaving = true,
                name = trimmedName,
                phone = trimmedPhone.orEmpty(),
                messenger = trimmedMessenger.orEmpty(),
                note = trimmedNote.orEmpty(),
                nameError = false
            )
            val student = (loadedStudent?.copy(
                name = trimmedName,
                phone = trimmedPhone,
                messenger = trimmedMessenger,
                note = trimmedNote,
                isArchived = formState.isArchived,
                active = formState.isActive,
            ) ?: Student(
                name = trimmedName,
                phone = trimmedPhone,
                messenger = trimmedMessenger,
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
fun StudentEditorScreen(
    onClose: () -> Unit,
    onSaved: (Long) -> Unit,
    vm: StudentEditorVM = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.student_editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose, enabled = !vm.formState.isSaving) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.student_editor_close)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!vm.formState.isSaving) {
                                vm.save(
                                    onSaved = onSaved,
                                    onError = { message ->
                                        coroutineScope.launch {
                                            val text = if (message.isNotBlank()) {
                                                message
                                            } else {
                                                context.getString(R.string.student_editor_save_error)
                                            }
                                            snackbarHostState.showSnackbar(text)
                                        }
                                    }
                                )
                            }
                        },
                        enabled = !vm.formState.isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(id = R.string.student_editor_save)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { inner ->
        StudentEditorForm(
            state = vm.formState,
            onNameChange = vm::onNameChange,
            onPhoneChange = vm::onPhoneChange,
            onMessengerChange = vm::onMessengerChange,
            onNoteChange = vm::onNoteChange,
            onArchivedChange = vm::onArchivedChange,
            onActiveChange = vm::onActiveChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            focusOnStart = false,
            enabled = !vm.formState.isSaving,
            onSubmit = {
                if (!vm.formState.isSaving) {
                    vm.save(
                        onSaved = onSaved,
                        onError = { message ->
                            coroutineScope.launch {
                                val text = if (message.isNotBlank()) {
                                    message
                                } else {
                                    stringResource(id = R.string.student_editor_save_error)
                                }
                                snackbarHostState.showSnackbar(text)
                            }
                        }
                    )
                }
            }
        )
    }
}
