package com.tutorly.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.lifecycle.ViewModel
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.models.Student
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@Composable
fun StudentScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    vm: StudentsVM = hiltViewModel()
) {
    val q by vm.search.collectAsStateWithLifecycle()
    val items by vm.students.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopBar(title = "Ученики") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, null) }
        }
    ) { inner ->
        Column(Modifier.padding(inner).padding(16.dp)) {
            OutlinedTextField(
                value = q,
                onValueChange = vm::onSearchChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Поиск по имени или телефону") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Spacer(Modifier.height(12.dp))

            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Пока пусто. Добавьте первого ученика.")
                }
            } else {
                LazyColumn {
                    items(items, key = { it.student.id }) { it ->
                        ListItem(
                            headlineContent = { Text(it.student.name) },
                            supportingContent = { it.student.phone?.let { p -> Text(p) } },
                            trailingContent = {
                                if (it.hasDebt) PaymentBadge() // ← твой компонент
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(it.student.id) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}


@HiltViewModel
class StudentsVM @Inject constructor(
    private val repo: StudentsRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    val search = query.asStateFlow()

    val students: StateFlow<List<StudentItem>> =
        query
            .debounce(200)
            .flatMapLatest { q -> repo.observeStudents(q) }
            .flatMapLatest { list ->
                // подмешаем признак долга
                combine(list.map { s ->
                    repo.observeHasDebt(s.id).map { hasDebt ->
                        StudentItem(s, hasDebt)
                    }
                }) { it.toList() }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchChange(v: String) { query.value = v }
}

data class StudentItem(val student: Student, val hasDebt: Boolean)


