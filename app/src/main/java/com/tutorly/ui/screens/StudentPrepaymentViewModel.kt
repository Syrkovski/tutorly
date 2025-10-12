package com.tutorly.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.repo.PaymentsRepository
import com.tutorly.models.Payment
import com.tutorly.models.PaymentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data class StudentPrepaymentFormState(
    val amount: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val amountError: Boolean = false,
)

@HiltViewModel
class StudentPrepaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val paymentsRepository: PaymentsRepository,
) : ViewModel() {

    private val studentId: Long = savedStateHandle.get<Long>("studentId")
        ?: error("studentId is required")

    var formState by mutableStateOf(StudentPrepaymentFormState())
        private set

    fun onAmountChange(value: String) {
        formState = formState.copy(amount = value, amountError = false)
    }

    fun onNoteChange(value: String) {
        formState = formState.copy(note = value)
    }

    fun reset() {
        formState = StudentPrepaymentFormState()
    }

    fun submit(
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (formState.isSaving) return

        val parsedAmount = parseMoneyInput(formState.amount)
        if (parsedAmount == null || parsedAmount == 0) {
            formState = formState.copy(amountError = true)
            return
        }

        val trimmedNote = formState.note.trim().ifBlank { null }

        viewModelScope.launch {
            formState = formState.copy(
                amount = formatMoneyInput(parsedAmount),
                note = trimmedNote.orEmpty(),
                amountError = false,
                isSaving = true,
            )

            val payment = Payment(
                lessonId = null,
                studentId = studentId,
                amountCents = parsedAmount,
                note = trimmedNote,
                status = PaymentStatus.PAID,
            )

            runCatching {
                paymentsRepository.insert(payment)
                paymentsRepository.applyPrepayment(studentId)
            }
                .onSuccess {
                    formState = StudentPrepaymentFormState()
                    onSuccess(parsedAmount)
                }
                .onFailure { throwable ->
                    formState = formState.copy(isSaving = false)
                    onError(throwable.message ?: "")
                }
        }
    }
}
