package com.tutorly.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tutorly.ui.theme.DebtChipContent
import com.tutorly.ui.theme.DebtChipFill
import com.tutorly.ui.theme.PaidChipContent

enum class PaymentBadgeStatus {
    PAID,
    DEBT,
    PREPAID
}

@Composable
fun PaymentBadge(
    status: PaymentBadgeStatus,
    modifier: Modifier = Modifier
) {
    val paidColor = Color(0xFF4E998C)
    val (txt, container, content) = when (status) {
        PaymentBadgeStatus.PAID -> Triple("Оплачено", paidColor, PaidChipContent)

        PaymentBadgeStatus.DEBT -> Triple("Долг", DebtChipFill, DebtChipContent)

        PaymentBadgeStatus.PREPAID -> Triple(
            "Предоплата",
            paidColor,
            PaidChipContent
        )
    }
    Surface(
        modifier = modifier,
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 1.dp
    ) {
        Text(
            txt,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
