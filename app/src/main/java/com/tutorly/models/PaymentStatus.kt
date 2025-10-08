package com.tutorly.models

enum class PaymentStatus {
    DUE,
    PAID,
    CANCELLED,
    UNPAID,
    ;

    companion object {
        val outstandingStatuses: List<PaymentStatus> = listOf(UNPAID, DUE)
    }
}
