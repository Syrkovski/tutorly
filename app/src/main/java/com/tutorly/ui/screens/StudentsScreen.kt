package com.tutorly.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
//import androidx.compose.animation.sharedBounds
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CurrencyRuble
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.ui.components.PaymentBadge
import com.tutorly.ui.components.PaymentBadgeStatus
import com.tutorly.ui.components.TutorlyBottomSheetContainer
import com.tutorly.ui.theme.TutorlyCardDefaults
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun StudentsScreen(
    onStudentEdit: (Long) -> Unit,
    onAddLesson: (Long) -> Unit,
    onStudentOpen: (Long) -> Unit,
    onStudentCreatedFromLesson: (Long) -> Unit = {},
    initialEditorOrigin: StudentEditorOrigin = StudentEditorOrigin.NONE,
    modifier: Modifier = Modifier,
    vm: StudentsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val query by vm.query.collectAsState()
    val students by vm.students.collectAsState()
    val formState by vm.editorFormState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editorOrigin by rememberSaveable { mutableStateOf(StudentEditorOrigin.NONE) }

    val openCreationEditor: (StudentEditorOrigin) -> Unit = { origin ->
        editorOrigin = origin
        vm.startStudentCreation()
        showEditor = true
    }

    LaunchedEffect(initialEditorOrigin) {
        if (initialEditorOrigin != StudentEditorOrigin.NONE) {
            editorOrigin = initialEditorOrigin
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
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openCreationEditor(StudentEditorOrigin.STUDENTS) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, MaterialTheme.shapes.large, clip = false),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(text = stringResource(id = R.string.search_students_hint)) },
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
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
                        val sharedKey = "student-card-${item.student.id}"
                        StudentCard(
                            item = item,
                            onClick = { onStudentOpen(item.student.id) },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            sharedContentKey = sharedKey
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        StudentEditorSheet(
            state = formState,
            onNameChange = vm::onEditorNameChange,
            onPhoneChange = vm::onEditorPhoneChange,
            onMessengerChange = vm::onEditorMessengerChange,
            onRateChange = vm::onEditorRateChange,
            onSubjectChange = vm::onEditorSubjectChange,
            onGradeChange = vm::onEditorGradeChange,
            onNoteChange = vm::onEditorNoteChange,
            onSave = handleSave,
            onDismiss = {
                if (!formState.isSaving) {
                    closeEditor()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentEditorSheet(
    state: StudentEditorFormState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onMessengerChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onGradeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    editTarget: StudentEditTarget? = null,
    initialFocus: StudentEditTarget? = StudentEditTarget.PROFILE,
    snackbarHostState: SnackbarHostState? = null,
) {
    val isEditing = state.studentId != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val minHeight = remember(configuration) { configuration.screenHeightDp.dp * 0.5f }
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Unspecified,
        scrimColor = Color.Black.copy(alpha = 0.32f)
    ) {
        TutorlyBottomSheetContainer(color = Color.White, dragHandle = null) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            id = if (isEditing) R.string.student_editor_edit_title else R.string.add_student
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, enabled = !state.isSaving) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(id = R.string.student_editor_close))
                    }
                }

                if (state.isSaving) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary
                    )
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
                    modifier = Modifier.fillMaxWidth(),
                    editTarget = editTarget,
                    initialFocus = initialFocus,
                    enableScrolling = false,
                    enabled = !state.isSaving,
                    onSubmit = onSave
                )

                if (snackbarHostState != null) {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = onSave,
                    enabled = !state.isSaving && state.name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = Color.White,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Text(text = stringResource(id = R.string.student_editor_save))
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
@OptIn(ExperimentalSharedTransitionApi::class)
private fun StudentCard(
    item: StudentsViewModel.StudentListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedContentKey: String? = null,
) {
    val subject = item.profile.subject?.takeIf { it.isNotBlank() }?.trim()
    val grade = item.profile.grade?.takeIf { it.isNotBlank() }?.trim()
    val subtitle = listOfNotNull(subject, grade)
        .joinToString(separator = " • ")
        .takeIf { it.isNotBlank() }

    val phone = item.student.phone?.takeIf { it.isNotBlank() }?.trim()
    val email = item.student.messenger?.takeIf { it.isNotBlank() }?.trim()
    val note = item.student.note?.takeIf { it.isNotBlank() }?.trim()
    val showTrailingRow = phone != null || email != null

    val sharedModifier = if (
        sharedTransitionScope != null &&
        animatedVisibilityScope != null &&
        sharedContentKey != null
    ) {
        with(sharedTransitionScope) {
            val sharedState = rememberSharedContentState(key = sharedContentKey)
            Modifier.sharedBounds(
                sharedContentState = sharedState,
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> tween(durationMillis = 450, easing = FastOutSlowInEasing) }
            )
        }
    } else {
        Modifier
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(sharedModifier),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = TutorlyCardDefaults.elevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            StudentAvatar(name = item.student.name, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.student.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item.hasDebt) {
                            Spacer(Modifier.width(8.dp))
                            PaymentBadge(status = PaymentBadgeStatus.DEBT)
                        }
                    }
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
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.StickyNote2,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CurrencyRuble,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(
                                    id = R.string.student_card_progress,
                                    item.progress.paidLessons,
                                    item.progress.completedLessons
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
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
                    }
                }
            }
        }
    }
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
