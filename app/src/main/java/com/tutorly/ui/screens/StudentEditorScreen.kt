package com.tutorly.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Instant

@HiltViewModel
class StudentEditorVM @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: StudentsRepository
) : ViewModel() {
    private val id: Long? = savedStateHandle.get<Long>("studentId")
    var name by mutableStateOf("")
    var phone by mutableStateOf("")
    var note by mutableStateOf("")
    private var loadedStudent: Student? = null

    init {
        id?.let {
            viewModelScope.launch {
                repo.observeStudent(it).collect { s ->
                    if (s != null) {
                        loadedStudent = s
                        name = s.name
                        phone = s.phone.orEmpty()
                        note = s.note.orEmpty()
                    }
                }
            }
        }
    }

    fun save(onSaved: (Long) -> Unit) = viewModelScope.launch {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Имя обязательно" }
        val trimmedPhone = phone.trim().ifBlank { null }
        val trimmedNote = note.trim().ifBlank { null }
        val student = (loadedStudent?.copy(
            name = trimmedName,
            phone = trimmedPhone,
            note = trimmedNote
        ) ?: Student(
            name = trimmedName,
            phone = trimmedPhone,
            note = trimmedNote
        )).copy(updatedAt = Instant.now())
        val newId = repo.upsert(student)
        loadedStudent = student.copy(id = newId)
        onSaved(newId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentEditorScreen(
    onClose: () -> Unit,
    onSaved: (Long) -> Unit,
    vm: StudentEditorVM = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.student_editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.student_editor_close)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.save(onSaved) }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(id = R.string.student_editor_save)
                        )
                    }
                }
            )
        }
    ) { inner ->
        Column(Modifier.padding(inner).padding(16.dp)) {
            OutlinedTextField(
                value = vm.name,
                onValueChange = { vm.name = it },
                label = { Text(text = stringResource(id = R.string.student_editor_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = vm.phone,
                onValueChange = { vm.phone = it },
                label = { Text(text = stringResource(id = R.string.student_editor_phone)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = vm.note,
                onValueChange = { vm.note = it },
                label = { Text(text = stringResource(id = R.string.student_editor_notes)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }
}
