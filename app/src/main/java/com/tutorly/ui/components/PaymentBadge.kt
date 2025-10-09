package com.tutorly.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaymentBadge(paid: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    val (txt, container, content) = if (paid) {
        Triple(
            "Оплачено",
            colorScheme.tertiaryContainer,
            colorScheme.onTertiaryContainer
        )
    } else {
        Triple(
            "Долг",
            colorScheme.errorContainer,
            colorScheme.onErrorContainer
        )
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
