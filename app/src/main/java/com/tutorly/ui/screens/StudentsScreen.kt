package com.tutorly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import com.tutorly.R
import com.tutorly.models.PaymentStatus
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(
    onStudentClick: (Long) -> Unit,
    onStudentCreatedFromLesson: (Long) -> Unit = {},
    initialEditorOrigin: StudentEditorOrigin = StudentEditorOrigin.NONE,
    modifier: Modifier = Modifier,
    vm: StudentsViewModel = hiltViewModel(),
) {
    val query by vm.query.collectAsState()
    val students by vm.students.collectAsState()
    val formState by vm.editorFormState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editorOrigin by rememberSaveable { mutableStateOf(StudentEditorOrigin.NONE) }
    var highlightedStudent by remember { mutableStateOf<StudentsViewModel.StudentListItem?>(null) }
    val selectedSheetState by vm.selectedStudentSheetState.collectAsState()

    LaunchedEffect(selectedSheetState.isVisible) {
        if (!selectedSheetState.isVisible) {
            highlightedStudent = null
        }
    }

    LaunchedEffect(initialEditorOrigin) {
        if (initialEditorOrigin != StudentEditorOrigin.NONE) {
            editorOrigin = initialEditorOrigin
            vm.startStudentCreation()
            showEditor = true
        }
    }

    val openEditor: (StudentEditorOrigin) -> Unit = { origin ->
        editorOrigin = origin
        vm.startStudentCreation()
        showEditor = true
    }

    val closeEditor = {
        showEditor = false
        vm.resetStudentForm()
        editorOrigin = StudentEditorOrigin.NONE
    }

    val handleSave = {
        if (!formState.isSaving) {
            vm.submitNewStudent(
                onSuccess = { newId, name ->
                    closeEditor()
                    val message = context.getString(R.string.student_added_message, name)
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                    if (editorOrigin == StudentEditorOrigin.LESSON_CREATION) {
                        onStudentCreatedFromLesson(newId)
                    }
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
        containerColor = MaterialTheme.colorScheme.surface
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

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { openEditor(StudentEditorOrigin.STUDENTS) }) {
                    Text(text = stringResource(id = R.string.add_student))
                }
            }

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
                            onClick = {
                                highlightedStudent = item
                                vm.showStudentDetails(item.student.id)
                            }
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
                    closeEditor()
                }
            },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            StudentEditorSheet(
                state = formState,
                onNameChange = vm::onEditorNameChange,
                onPhoneChange = vm::onEditorPhoneChange,
                onMessengerChange = vm::onEditorMessengerChange,
                onNoteChange = vm::onEditorNoteChange,
                onArchivedChange = vm::onEditorArchivedChange,
                onActiveChange = vm::onEditorActiveChange,
                onCancel = { if (!formState.isSaving) closeEditor() },
                onSave = handleSave
            )
        }
    }

    if (selectedSheetState.isVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                vm.dismissStudentDetails()
                highlightedStudent = null
            },
            sheetState = detailSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            StudentProfileSheet(
                state = selectedSheetState,
                listItem = highlightedStudent,
                onOpenProfile = {
                    val targetId = selectedSheetState.student?.id ?: highlightedStudent?.student?.id
                    highlightedStudent = null
                    vm.dismissStudentDetails()
                    targetId?.let(onStudentClick)
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
    onNoteChange: (String) -> Unit,
    onArchivedChange: (Boolean) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.add_student),
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
            onNoteChange = onNoteChange,
            onArchivedChange = onArchivedChange,
            onActiveChange = onActiveChange,
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
                    Text(text = stringResource(id = R.string.add_student))
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
    val locale = remember { Locale("ru", "RU") }
    val currencyFormatter = remember(locale) { NumberFormat.getCurrencyInstance(locale) }
    val highlightTexts = studentHighlightTexts(item.profile, currencyFormatter)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StudentAvatar(name = item.student.name)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.student.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = highlightTexts.subjectText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (highlightTexts.isSubjectPlaceholder) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.hasDebt) {
                    Text(
                        text = stringResource(id = R.string.students_debt_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StudentHighlightRow(
                    icon = Icons.Outlined.School,
                    text = highlightTexts.gradeText,
                    isPlaceholder = highlightTexts.isGradePlaceholder
                )
                StudentHighlightRow(
                    icon = Icons.Outlined.AttachMoney,
                    text = highlightTexts.rateText,
                    isPlaceholder = highlightTexts.isRatePlaceholder
                )
            }
        }
    }
}

@Composable
private fun StudentHighlightRow(
    icon: ImageVector,
    text: String,
    isPlaceholder: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isPlaceholder) {
                MaterialTheme.colorScheme.outline
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}


@Composable
private fun StudentProfileSheet(
    state: StudentsViewModel.SelectedStudentSheetState,
    listItem: StudentsViewModel.StudentListItem?,
    onOpenProfile: () -> Unit,
) {
    val locale = remember { Locale("ru", "RU") }
    val currencyFormatter = remember(locale) { NumberFormat.getCurrencyInstance(locale) }

    when {
        state.isLoading -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        state.student == null -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.student_details_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            val student = state.student
            val lessons = remember(state.lessons) { state.lessons.sortedByDescending { it.startAt } }
            val totalEarnedCents = remember(lessons) { lessons.sumOf { it.paidCents.toLong() } }
            val formattedTotalEarned = remember(totalEarnedCents) { currencyFormatter.format(totalEarnedCents / 100.0) }
            val formattedDebt = remember(state.totalDebtCents) { currencyFormatter.format(state.totalDebtCents / 100.0) }
            val highlightTexts = studentHighlightTexts(listItem?.profile, currencyFormatter)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                StudentSummaryCard(
                    student = student,
                    hasDebt = state.hasDebt,
                    totalDebt = formattedDebt
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val subjectLine = stringResource(
                        id = R.string.students_highlight_value,
                        stringResource(id = R.string.students_subject_label),
                        highlightTexts.subjectText
                    )
                    Text(
                        text = subjectLine,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (highlightTexts.isSubjectPlaceholder) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    StudentHighlightRow(
                        icon = Icons.Outlined.School,
                        text = highlightTexts.gradeText,
                        isPlaceholder = highlightTexts.isGradePlaceholder
                    )
                    StudentHighlightRow(
                        icon = Icons.Outlined.AttachMoney,
                        text = highlightTexts.rateText,
                        isPlaceholder = highlightTexts.isRatePlaceholder
                    )
                }

                StudentContactCard(student = student)

                StudentStatsRow(
                    totalLessons = lessons.size,
                    formattedTotalEarned = formattedTotalEarned,
                    paidLessons = lessons.count { it.paymentStatus == PaymentStatus.PAID },
                    hasDebt = state.hasDebt,
                    formattedDebt = formattedDebt
                )

                student.note?.takeIf { it.isNotBlank() }?.let { note ->
                    StudentNotesCard(note = note)
                }

                LessonsHistoryCard(
                    lessons = lessons.take(3),
                    currencyFormatter = currencyFormatter,
                    locale = locale,
                    onLessonClick = {}
                )

                Button(
                    onClick = onOpenProfile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.student_details_open_profile))
                }
            }
        }
    }
}

private data class StudentHighlightTexts(
    val subjectText: String,
    val isSubjectPlaceholder: Boolean,
    val gradeText: String,
    val isGradePlaceholder: Boolean,
    val rateText: String,
    val isRatePlaceholder: Boolean,
)

@Composable
private fun studentHighlightTexts(
    profile: StudentsViewModel.StudentProfile?,
    currencyFormatter: NumberFormat
): StudentHighlightTexts {
    val subjectPlaceholder = stringResource(id = R.string.students_subject_placeholder)
    val gradeLabel = stringResource(id = R.string.students_grade_label)
    val gradePlaceholder = stringResource(id = R.string.students_grade_placeholder)
    val ratePlaceholder = stringResource(id = R.string.students_rate_placeholder)
    val rateLabelGeneric = stringResource(id = R.string.students_rate_label_generic)

    val subjectValue = profile?.subject?.takeIf { it.isNotBlank() }
    val gradeValue = profile?.grade?.takeIf { it.isNotBlank() }
    val rate = profile?.rate?.takeIf { it.priceCents > 0 && it.durationMinutes > 0 }

    val rateLabel = rate?.durationMinutes?.let { duration ->
        when (duration) {
            60 -> stringResource(id = R.string.students_rate_label_hour)
            90 -> stringResource(id = R.string.students_rate_label_hour_half)
            else -> stringResource(id = R.string.students_rate_label_custom, duration)
        }
    } ?: rateLabelGeneric

    val rateValue = rate?.let { currencyFormatter.format(it.priceCents / 100.0) } ?: ratePlaceholder

    return StudentHighlightTexts(
        subjectText = subjectValue ?: subjectPlaceholder,
        isSubjectPlaceholder = subjectValue == null,
        gradeText = stringResource(
            id = R.string.students_highlight_value,
            gradeLabel,
            gradeValue ?: gradePlaceholder
        ),
        isGradePlaceholder = gradeValue == null,
        rateText = stringResource(
            id = R.string.students_highlight_value,
            rateLabel,
            rateValue
        ),
        isRatePlaceholder = rate == null
    )
}

@Composable
private fun StudentAvatar(
    name: String,
    size: Dp = 48.dp,
) {
    val initials = name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(separator = "") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

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
