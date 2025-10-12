package com.tutorly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tutorly.R
import com.tutorly.domain.model.StudentProfile
import com.tutorly.domain.model.StudentProfileLesson
import com.tutorly.models.PaymentStatus
import com.tutorly.ui.lessoncard.LessonCardSheet
import com.tutorly.ui.lessoncard.LessonCardViewModel
import com.tutorly.ui.components.PaymentBadge
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(
    onStudentEdit: (Long) -> Unit,
    onAddLesson: (Long) -> Unit,
    onStudentCreatedFromLesson: (Long) -> Unit = {},
    initialEditorOrigin: StudentEditorOrigin = StudentEditorOrigin.NONE,
    modifier: Modifier = Modifier,
    vm: StudentsViewModel = hiltViewModel(),
) {
    val query by vm.query.collectAsState()
    val students by vm.students.collectAsState()
    val formState by vm.editorFormState.collectAsState()
    val profileUiState by vm.profileUiState.collectAsState()
    val lessonCardViewModel: LessonCardViewModel = hiltViewModel()
    val lessonCardState by lessonCardViewModel.uiState.collectAsState()
    LessonCardSheet(
        state = lessonCardState,
        onDismissRequest = lessonCardViewModel::dismiss,
        onStudentSelect = lessonCardViewModel::onStudentSelected,
        onDateSelect = lessonCardViewModel::onDateSelected,
        onTimeSelect = lessonCardViewModel::onTimeSelected,
        onDurationSelect = lessonCardViewModel::onDurationSelected,
        onPriceChange = lessonCardViewModel::onPriceChanged,
        onStatusSelect = lessonCardViewModel::onPaymentStatusSelected,
        onNoteChange = lessonCardViewModel::onNoteChanged,
        onSnackbarConsumed = lessonCardViewModel::consumeSnackbar
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val editorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val profileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editorOrigin by rememberSaveable { mutableStateOf(StudentEditorOrigin.NONE) }
    var pendingProfileId by remember { mutableStateOf<Long?>(null) }

    val openCreationEditor: (StudentEditorOrigin) -> Unit = { origin ->
        editorOrigin = origin
        pendingProfileId = null
        vm.startStudentCreation()
        showEditor = true
    }

    LaunchedEffect(initialEditorOrigin) {
        if (initialEditorOrigin != StudentEditorOrigin.NONE) {
            editorOrigin = initialEditorOrigin
            pendingProfileId = null
            vm.startStudentCreation()
            showEditor = true
        }
    }

    val closeEditor = {
        showEditor = false
        vm.resetStudentForm()
        editorOrigin = StudentEditorOrigin.NONE
    }

    val handleSave = {
        if (!formState.isSaving) {
            vm.submitStudent(
                onSuccess = { id, name, isNew ->
                    closeEditor()
                    val message = if (isNew) {
                        context.getString(R.string.student_added_message, name)
                    } else {
                        context.getString(R.string.student_updated_message, name)
                    }
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                    if (isNew) {
                        if (editorOrigin == StudentEditorOrigin.LESSON_CREATION) {
                            onStudentCreatedFromLesson(id)
                        }
                    } else {
                        pendingProfileId?.let { vm.openStudentProfile(it) }
                    }
                    pendingProfileId = null
                },
                onError = { error ->
                    val message = if (error.isNotBlank()) {
                        error
                    } else {
                        context.getString(R.string.student_editor_save_error)
                    }
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
            )
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openCreationEditor(StudentEditorOrigin.STUDENTS) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.add_student)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(text = stringResource(id = R.string.search_students_hint)) },
                shape = MaterialTheme.shapes.large
            )

            Spacer(Modifier.height(16.dp))

            if (students.isEmpty()) {
                EmptyStudentsState(Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = students,
                        key = { it.student.id }
                    ) { item ->
                        StudentCard(
                            item = item,
                            onClick = { vm.openStudentProfile(item.student.id) }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!formState.isSaving) {
                    pendingProfileId = null
                    closeEditor()
                }
            },
            sheetState = editorSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            StudentEditorSheet(
                state = formState,
                onNameChange = vm::onEditorNameChange,
                onPhoneChange = vm::onEditorPhoneChange,
                onMessengerChange = vm::onEditorMessengerChange,
                onRateChange = vm::onEditorRateChange,
                onSubjectChange = vm::onEditorSubjectChange,
                onGradeChange = vm::onEditorGradeChange,
                onNoteChange = vm::onEditorNoteChange,
                onArchivedChange = vm::onEditorArchivedChange,
                onActiveChange = vm::onEditorActiveChange,
                onCancel = {
                    if (!formState.isSaving) {
                        pendingProfileId = null
                        closeEditor()
                    }
                },
                onSave = handleSave
            )
        }
    }

    if (profileUiState !is StudentProfileUiState.Hidden) {
        ModalBottomSheet(
            onDismissRequest = vm::clearSelectedStudent,
            sheetState = profileSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            StudentProfileSheet(
                state = profileUiState,
                onClose = vm::clearSelectedStudent,
                onEdit = { studentId ->
                    val profileStudent = (profileUiState as? StudentProfileUiState.Content)?.profile?.student
                    if (profileStudent != null && profileStudent.id == studentId) {
                        vm.clearSelectedStudent()
                        editorOrigin = StudentEditorOrigin.STUDENTS
                        pendingProfileId = studentId
                        vm.startStudentEdit(profileStudent)
                        showEditor = true
                    } else {
                        vm.clearSelectedStudent()
                        onStudentEdit(studentId)
                    }
                },
                onAddLesson = { studentId ->
                    vm.clearSelectedStudent()
                    onAddLesson(studentId)
                },
                onLessonClick = { lessonId ->
                    vm.clearSelectedStudent()
                    lessonCardViewModel.open(lessonId)
                }
            )
        }
    }
}

@Composable
private fun StudentEditorSheet(
    state: StudentEditorFormState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onMessengerChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onGradeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onArchivedChange: (Boolean) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val isEditing = state.studentId != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    id = if (isEditing) R.string.student_editor_edit_title else R.string.add_student
                ),
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onCancel, enabled = !state.isSaving) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(id = R.string.student_editor_close)
                )
            }
        }

        StudentEditorForm(
            state = state,
            onNameChange = onNameChange,
            onPhoneChange = onPhoneChange,
            onMessengerChange = onMessengerChange,
            onRateChange = onRateChange,
            onSubjectChange = onSubjectChange,
            onGradeChange = onGradeChange,
            onNoteChange = onNoteChange,
            onArchivedChange = onArchivedChange,
            onActiveChange = onActiveChange,
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(),
            focusOnStart = true,
            enabled = !state.isSaving,
            onSubmit = onSave
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving
            ) {
                Text(text = stringResource(id = R.string.student_editor_cancel))
            }

            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving && state.name.isNotBlank()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.student_editor_save)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStudentsState(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(id = R.string.students_empty_state),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StudentCard(
    item: StudentsViewModel.StudentListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subject = item.profile.subject?.takeIf { it.isNotBlank() }?.trim()
    val grade = item.profile.grade?.takeIf { it.isNotBlank() }?.trim()
    val subtitle = listOfNotNull(subject, grade)
        .joinToString(separator = " • ")
        .takeIf { it.isNotBlank() }

    val phone = item.student.phone?.takeIf { it.isNotBlank() }?.trim()
    val email = item.student.messenger?.takeIf { it.isNotBlank() }?.trim()
    val note = item.student.note?.takeIf { it.isNotBlank() }?.trim()
    val showTrailingRow = phone != null || email != null || item.hasDebt

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudentAvatar(name = item.student.name, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.student.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                note?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stringResource(
                        id = R.string.student_card_progress,
                        item.progress.paidLessons,
                        item.progress.completedLessons
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showTrailingRow) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (phone != null) {
                            Icon(
                                imageVector = Icons.Outlined.Phone,
                                contentDescription = phone,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (email != null) {
                            if (phone != null) {
                                Spacer(Modifier.width(12.dp))
                            }
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = email,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (item.hasDebt) {
                            if (phone != null || email != null) {
                                Spacer(Modifier.width(12.dp))
                            }
                            PaymentBadge(paid = false)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun StudentProfileSheet(
    state: StudentProfileUiState,
    onClose: () -> Unit,
    onEdit: (Long) -> Unit,
    onAddLesson: (Long) -> Unit,
    onLessonClick: (Long) -> Unit,
    onCall: ((String) -> Unit)? = null,
    onMessage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (state) {
        StudentProfileUiState.Hidden -> Unit
        StudentProfileUiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        StudentProfileUiState.Error -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.student_profile_error),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onClose) {
                    Text(text = stringResource(id = R.string.student_editor_close))
                }
            }
        }

        is StudentProfileUiState.Content -> {
            StudentProfileContent(
                profile = state.profile,
                onEdit = onEdit,
                onAddLesson = onAddLesson,
                onLessonClick = onLessonClick,
                onCall = onCall,
                onMessage = onMessage,
                modifier = modifier
            )
        }
    }
}


@Composable
private fun StudentProfileContent(
    profile: StudentProfile,
    onEdit: (Long) -> Unit,
    onAddLesson: (Long) -> Unit,
    onLessonClick: (Long) -> Unit,
    onCall: ((String) -> Unit)?,
    onMessage: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val locale = remember { Locale("ru", "RU") }
    val currencyFormatter = remember(locale) {
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance("RUB")
        }
    }
    val zoneId = remember { ZoneId.systemDefault() }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM yyyy", locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("LLLL yyyy", locale) }

    val groupedLessons = remember(profile.recentLessons, zoneId) {
        val sorted = profile.recentLessons.sortedByDescending { it.startAt }
        val groups = linkedMapOf<YearMonth, MutableList<StudentProfileLesson>>()
        sorted.forEach { lesson ->
            val key = YearMonth.from(lesson.startAt.atZone(zoneId))
            groups.getOrPut(key) { mutableListOf() }.add(lesson)
        }
        groups.map { it.key to it.value.toList() }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                StudentProfileHeader(
                    profile = profile,
                    onEdit = onEdit
                )
            }
            item {
                StudentProfileContacts(
                    profile = profile,
                    onCall = onCall,
                    onMessage = onMessage
                )
            }
            item {
                StudentProfileMetricsSection(
                    profile = profile,
                    currencyFormatter = currencyFormatter
                )
            }
            item {
                Text(
                    text = stringResource(id = R.string.student_details_history_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (profile.recentLessons.isEmpty()) {
                item {
                    StudentProfileEmptyHistory(
                        onAddLesson = { onAddLesson(profile.student.id) }
                    )
                }
            } else {
                groupedLessons.forEach { (month, lessons) ->
                    item(key = "month-$month") {
                        val monthTitle = remember(month) {
                            month.format(monthFormatter).replaceFirstChar { char ->
                                if (char.isLowerCase()) char.titlecase(locale) else char.toString()
                            }
                        }
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                    }
                    items(lessons, key = { it.id }) { lesson ->
                        StudentProfileLessonCard(
                            lesson = lesson,
                            fallbackSubject = profile.subject,
                            currencyFormatter = currencyFormatter,
                            zoneId = zoneId,
                            dateFormatter = dateFormatter,
                            timeFormatter = timeFormatter,
                            onClick = { onLessonClick(lesson.id) }
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { onAddLesson(profile.student.id) },
            icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
            text = { Text(text = stringResource(id = R.string.student_details_create_lesson)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding()
        )
    }
}



@Composable
private fun StudentProfileHeader(
    profile: StudentProfile,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StudentAvatar(name = profile.student.name, size = 64.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = profile.student.name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val subject = profile.subject?.takeIf { it.isNotBlank() }?.trim()
            val grade = profile.grade?.takeIf { it.isNotBlank() }?.trim()
            val details = listOfNotNull(subject, grade).joinToString(separator = " ")
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = { onEdit(profile.student.id) }) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(id = R.string.student_details_edit)
            )
        }
    }
}


@Composable
private fun StudentProfileContacts(
    profile: StudentProfile,
    onCall: ((String) -> Unit)?,
    onMessage: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.student_details_contact_title),
            style = MaterialTheme.typography.titleMedium
        )
        ProfileContactRow(
            icon = Icons.Outlined.Phone,
            label = stringResource(id = R.string.student_profile_contact_call),
            value = profile.student.phone,
            onClick = onCall
        )
        ProfileContactRow(
            icon = Icons.Outlined.Email,
            label = stringResource(id = R.string.student_profile_contact_message),
            value = profile.student.messenger,
            onClick = onMessage
        )
    }
}

@Composable
private fun ProfileContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String?,
    onClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val hasValue = !value.isNullOrBlank()
    val displayValue = value?.takeIf { it.isNotBlank() }
        ?: stringResource(id = R.string.student_profile_contact_placeholder)
    val background = if (hasValue) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = if (hasValue) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(background)
            .clickable(enabled = hasValue && onClick != null) {
                value?.let { onClick?.invoke(it) }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (hasValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun StudentProfileMetricsSection(
    profile: StudentProfile,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val metrics = profile.metrics
    val totalLessons = metrics.totalLessons.toString()
    val totalPaid = formatCurrency(metrics.totalPaidCents, currencyFormatter)
    val paidLessons = metrics.paidLessons.toString()
    val debtText = if (metrics.outstandingCents > 0) {
        formatCurrency(metrics.outstandingCents, currencyFormatter)
    } else {
        stringResource(id = R.string.student_details_no_debt)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.student_profile_metrics_title),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileMetricCard(
                label = stringResource(id = R.string.student_details_stats_lessons),
                value = totalLessons
            )
            ProfileMetricCard(
                label = stringResource(id = R.string.student_details_stats_paid),
                value = totalPaid
            )
            ProfileMetricCard(
                label = stringResource(id = R.string.student_details_stats_paid_lessons),
                value = paidLessons
            )
            ProfileMetricCard(
                label = stringResource(id = R.string.student_details_stats_debt),
                value = debtText,
                badge = if (profile.hasDebt) {
                    {
                        PaymentBadge(paid = false)
                    }
                } else {
                    null
                }
            )
        }
    }
}


@Composable
private fun ProfileMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (badge != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    badge()
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun StudentProfileEmptyHistory(
    onAddLesson: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.student_details_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onAddLesson) {
                Text(text = stringResource(id = R.string.student_details_create_lesson))
            }
        }
    }
}


@Composable
private fun StudentProfileLessonCard(
    lesson: StudentProfileLesson,
    fallbackSubject: String?,
    currencyFormatter: NumberFormat,
    zoneId: ZoneId,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val start = remember(lesson.startAt, zoneId) { lesson.startAt.atZone(zoneId) }
    val end = remember(lesson.endAt, zoneId) { lesson.endAt.atZone(zoneId) }
    val dateText = remember(start) { dateFormatter.format(start) }
    val timeText = stringResource(
        id = R.string.student_details_history_time_range,
        timeFormatter.format(start),
        timeFormatter.format(end),
        lesson.durationMinutes
    )
    val fallbackSubjectText = fallbackSubject?.takeIf { it.isNotBlank() }?.trim()
    val title = lesson.title?.takeIf { it.isNotBlank() }?.trim()
        ?: lesson.subjectName?.takeIf { it.isNotBlank() }?.trim()
        ?: fallbackSubjectText
        ?: stringResource(id = R.string.lesson_card_subject_placeholder)
    val amount = formatCurrency(lesson.priceCents.toLong(), currencyFormatter)
    val isPaid = lesson.paymentStatus == PaymentStatus.PAID

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleMedium
                )
                PaymentBadge(paid = isPaid)
            }
        }
    }
}


private fun formatCurrency(amountCents: Long, formatter: NumberFormat): String {
    return formatter.format(amountCents / 100.0)
}

@Composable
private fun StudentAvatar(
    name: String,
    size: Dp = 48.dp,
) {
    val initials = remember(name) {
        name
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString(separator = "") { it.first().uppercaseChar().toString() }
            .ifEmpty { "?" }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
