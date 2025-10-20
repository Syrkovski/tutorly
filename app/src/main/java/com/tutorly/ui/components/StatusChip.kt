package com.tutorly.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tutorly.R
import com.tutorly.models.PaymentStatus
import com.tutorly.ui.theme.DebtChipContent
import com.tutorly.ui.theme.DebtChipFill
import com.tutorly.ui.theme.PaidChipContent
import java.time.ZonedDateTime

/** Visual parameters for the payment status indicator. */
data class StatusChipData(
    val label: String,
    val description: String,
    val background: Color,
    val content: Color
)

@Composable
fun StatusChip(data: StatusChipData, modifier: Modifier = Modifier) {
    Surface(
        color = data.background,
        contentColor = data.content,
        shape = CircleShape,
        modifier = modifier
            .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
            .semantics { contentDescription = data.description }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = data.label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = data.content
            )
        }
    }
}

@Composable
fun statusChipData(
    paymentStatus: PaymentStatus,
    start: ZonedDateTime,
    end: ZonedDateTime,
    now: ZonedDateTime
): StatusChipData {
    val colorScheme = MaterialTheme.colorScheme
    val cancelledColor = colorScheme.outline
    val paidColor = Color(0xFF4E998C)

    fun defaultContentColor(background: Color): Color =
        if (background.luminance() < 0.5f) Color.White else colorScheme.onSurface

    val isFutureLesson = now.isBefore(start)

    val data = when (paymentStatus) {
        PaymentStatus.CANCELLED -> StatusChipData(
            label = "×",
            description = stringResource(R.string.lesson_status_cancelled),
            background = cancelledColor,
            content = defaultContentColor(cancelledColor)
        )

        PaymentStatus.PAID -> StatusChipData(
            label = "₽",
            description = stringResource(R.string.lesson_status_paid),
            background = paidColor,
            content = PaidChipContent
        )

        PaymentStatus.DUE, PaymentStatus.UNPAID -> {
            if (isFutureLesson) {
                StatusChipData(
                    label = "₽",
                    description = stringResource(R.string.lesson_status_unpaid),
                    background = colorScheme.surfaceVariant,
                    content = colorScheme.onSurfaceVariant
                )
            } else {
                StatusChipData(
                    label = "₽",
                    description = stringResource(R.string.lesson_status_unpaid),
                    background = DebtChipFill,
                    content = DebtChipContent
                )
            }
        }
    }

    return data
}
