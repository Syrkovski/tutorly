package com.tutorly.ui.lessoncard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.models.PaymentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LessonCardViewModel @Inject constructor(
    private val lessonsRepository: LessonsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonCardUiState())
    val uiState: StateFlow<LessonCardUiState> = _uiState.asStateFlow()

    private var lessonJob: Job? = null

    fun open(lessonId: Long) {
        if (_uiState.value.isVisible && _uiState.value.details?.id == lessonId) {
            return
        }
        lessonJob?.cancel()
        _uiState.value = LessonCardUiState(isVisible = true, isLoading = true)
        lessonJob = viewModelScope.launch {
            lessonsRepository.observeLessonDetails(lessonId).collect { details ->
                if (details == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            details = null
                        )
                    }
                } else {
                    _uiState.update { state ->
                        val currentDraft = state.noteDraft
                        val noteFromRepo = details.lessonNote.orEmpty()
                        val keepDraft = state.isNoteDirty && currentDraft != noteFromRepo
                        state.copy(
                            isLoading = false,
                            details = details.toCardDetails(),
                            noteDraft = if (keepDraft) currentDraft else noteFromRepo,
                            isNoteDirty = if (keepDraft) state.isNoteDirty else false
                        )
                    }
                }
            }
        }
    }

    fun onNoteChange(value: String) {
        val normalized = value.take(LESSON_CARD_NOTE_LIMIT)
        _uiState.update { state ->
            val original = state.details?.lessonNote.orEmpty()
            state.copy(
                noteDraft = normalized,
                isNoteDirty = normalized != original
            )
        }
    }

    fun saveNote() {
        val details = _uiState.value.details ?: return
        if (!_uiState.value.isNoteDirty || _uiState.value.isSavingNote) return
        val note = _uiState.value.noteDraft.trim().ifBlank { null }
        _uiState.update { it.copy(isSavingNote = true) }
        viewModelScope.launch {
            runCatching { lessonsRepository.saveNote(details.id, note) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSavingNote = false,
                            isNoteDirty = false,
                            snackbarMessage = LessonCardMessage.NoteSaved
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingNote = false,
                            snackbarMessage = LessonCardMessage.Error(error)
                        )
                    }
                }
        }
    }

    fun markPaid() {
        val details = _uiState.value.details ?: return
        if (_uiState.value.isPaymentActionRunning) return
        _uiState.update { it.copy(isPaymentActionRunning = true) }
        viewModelScope.launch {
            runCatching { lessonsRepository.markPaid(details.id) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isPaymentActionRunning = false,
                            snackbarMessage = LessonCardMessage.PaymentMarked(PaymentStatus.PAID)
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isPaymentActionRunning = false,
                            snackbarMessage = LessonCardMessage.Error(error)
                        )
                    }
                }
        }
    }

    fun requestMarkDue() {
        if (_uiState.value.isPaymentActionRunning) return
        _uiState.update { it.copy(showMarkDueDialog = true) }
    }

    fun dismissMarkDueDialog() {
        _uiState.update { it.copy(showMarkDueDialog = false) }
    }

    fun confirmMarkDue() {
        val details = _uiState.value.details ?: return
        _uiState.update { it.copy(showMarkDueDialog = false, isPaymentActionRunning = true) }
        viewModelScope.launch {
            runCatching { lessonsRepository.markDue(details.id) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isPaymentActionRunning = false,
                            snackbarMessage = LessonCardMessage.PaymentMarked(PaymentStatus.DUE)
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isPaymentActionRunning = false,
                            snackbarMessage = LessonCardMessage.Error(error)
                        )
                    }
                }
        }
    }

    fun requestDismiss() {
        if (_uiState.value.isNoteDirty) {
            _uiState.update {
                it.copy(
                    showUnsavedExitDialog = true,
                    pendingExitAction = LessonCardExitAction.Close
                )
            }
        } else {
            closeAndEmit(null)
        }
    }

    fun requestEdit() {
        val details = _uiState.value.details ?: return
        if (_uiState.value.isNoteDirty) {
            _uiState.update {
                it.copy(
                    showUnsavedExitDialog = true,
                    pendingExitAction = LessonCardExitAction.NavigateToEdit(details)
                )
            }
        } else {
            closeAndEmit(LessonCardExitAction.NavigateToEdit(details))
        }
    }

    fun cancelDismiss() {
        _uiState.update {
            it.copy(
                showUnsavedExitDialog = false,
                pendingExitAction = null
            )
        }
    }

    fun confirmDismiss() {
        val action = _uiState.value.pendingExitAction
        closeAndEmit(action)
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun consumeExitAction() {
        _uiState.update { it.copy(pendingExitAction = null) }
    }

    private fun closeAndEmit(action: LessonCardExitAction?) {
        lessonJob?.cancel()
        lessonJob = null
        _uiState.value = LessonCardUiState(pendingExitAction = action)
    }
}

private fun LessonDetails.toCardDetails(): LessonCardDetails = LessonCardDetails(
    id = id,
    studentId = studentId,
    studentName = studentName,
    studentNote = studentNote,
    subjectName = subjectName,
    lessonTitle = lessonTitle,
    lessonNote = lessonNote,
    startAt = startAt,
    endAt = endAt,
    duration = duration,
    priceCents = priceCents,
    paidCents = paidCents,
    paymentStatus = paymentStatus,
    paymentStatusIcon = paymentStatusIcon,
)
