package com.tutorly.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tutorly.ui.theme.DebtChipContent
import com.tutorly.ui.theme.DebtChipFill
import com.tutorly.ui.theme.PaidChipContent
import com.tutorly.ui.theme.TutorlyColors
import com.tutorly.ui.theme.TutorlyRadii
import com.tutorly.ui.theme.TutorlySpacing
import com.tutorly.ui.theme.TutorlyTypeScale

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
    val paidColor = TutorlyColors.paymentPaid
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
        shape = RoundedCornerShape(TutorlyRadii.pill),
        tonalElevation = 1.dp
    ) {
        Text(
            txt,
            fontSize = TutorlyTypeScale.badgeText,
            modifier = Modifier.padding(horizontal = TutorlySpacing.sm + 2.dp, vertical = TutorlySpacing.xs)
        )
    }
}
