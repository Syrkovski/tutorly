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
    val isSaving: Boolean = false,
    val amountError: Boolean = false,
)

data class StudentPrepaymentResult(
    val depositedCents: Int,
    val debtCoveredCents: Long
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

    fun reset() {
        formState = StudentPrepaymentFormState()
    }

    fun submit(
        onSuccess: (StudentPrepaymentResult) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (formState.isSaving) return

        val parsedAmount = parseMoneyInput(formState.amount)
        if (parsedAmount == null || parsedAmount == 0) {
            formState = formState.copy(amountError = true)
            return
        }

        viewModelScope.launch {
            formState = formState.copy(
                amount = formatMoneyInput(parsedAmount),
                amountError = false,
                isSaving = true,
            )

            val payment = Payment(
                lessonId = null,
                studentId = studentId,
                amountCents = parsedAmount,
                status = PaymentStatus.PAID,
            )

            runCatching {
                val outstandingBefore = paymentsRepository.totalDebt(studentId)
                paymentsRepository.insert(payment)
                paymentsRepository.applyPrepayment(studentId)
                val outstandingAfter = paymentsRepository.totalDebt(studentId)
                val debtCovered = (outstandingBefore - outstandingAfter).coerceAtLeast(0)
                StudentPrepaymentResult(
                    depositedCents = parsedAmount,
                    debtCoveredCents = debtCovered
                )
            }
                .onSuccess { result ->
                    formState = StudentPrepaymentFormState()
                    onSuccess(result)
                }
                .onFailure { throwable ->
                    formState = formState.copy(isSaving = false)
                    onError(throwable.message ?: "")
                }
        }
    }
}
