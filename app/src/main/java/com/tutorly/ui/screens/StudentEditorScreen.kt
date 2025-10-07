package com.tutorly.ui.screens

import android.graphics.drawable.Icon
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.models.Student
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class StudentEditorVM @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: StudentsRepository
) : ViewModel() {
    private val id: Long? = savedStateHandle["studentId"]
    var name by mutableStateOf("")
    var phone by mutableStateOf("")
    var notes by mutableStateOf("")

    init {
        id?.let {
            viewModelScope.launch {
                repo.observeStudent(it).collect { s ->
                    if (s != null) {
                        name = s.name; phone = s.phone.orEmpty(); notes = s.notes.orEmpty()
                    }
                }
            }
        }
    }

    fun save(onSaved: (Long) -> Unit) = viewModelScope.launch {
        require(name.isNotBlank()) { "Имя обязательно" }
        val newId = repo.upsert(
            Student(
                id ?: 0,
                name.trim(),
                phone.ifBlank { null },
                notes.ifBlank { null })
        )
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
                title = { Text("Ученик") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) } },
                actions = { IconButton(onClick = { vm.save(onSaved) }) { Icon(Icons.Default.Check, null) } }
            )
        }
    ) { inner ->
        Column(Modifier.padding(inner).padding(16.dp)) {
            OutlinedTextField(vm.name, { vm.name = it }, label = { Text("Имя*") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(vm.phone, { vm.phone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(vm.notes, { vm.notes = it }, label = { Text("Заметки") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        }
    }
}
