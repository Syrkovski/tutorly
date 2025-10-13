package com.tutorly.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tutorly.ui.theme.DarkOnSuccessGreenContainer
import com.tutorly.ui.theme.DarkSuccessGreenContainer
import com.tutorly.ui.theme.OnSuccessGreenContainer
import com.tutorly.ui.theme.SuccessGreenContainer

enum class PaymentBadgeStatus {
    PAID,
    DEBT,
    PREPAID
}

@Composable
fun PaymentBadge(status: PaymentBadgeStatus) {
    val colorScheme = MaterialTheme.colorScheme
    val isLightTheme = colorScheme.surface.luminance() > 0.5f
    val (txt, container, content) = when (status) {
        PaymentBadgeStatus.PAID -> {
            val background = if (isLightTheme) SuccessGreenContainer else DarkSuccessGreenContainer
            val foreground = if (isLightTheme) OnSuccessGreenContainer else DarkOnSuccessGreenContainer
            Triple("Оплачено", background, foreground)
        }

        PaymentBadgeStatus.DEBT -> {
            Triple("Долг", colorScheme.errorContainer, colorScheme.onErrorContainer)
        }

        PaymentBadgeStatus.PREPAID -> {
            Triple(
                "Предоплата",
                colorScheme.primaryContainer,
                colorScheme.onPrimaryContainer
            )
        }
    }
    Surface(
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
