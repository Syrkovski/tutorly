package com.tutorly.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object TutorlyCardDefaults {
    @Composable
    fun containerColor(): Color = MaterialTheme.colorScheme.surface

    @Composable
    fun colors(
        containerColor: Color = containerColor(),
        contentColor: Color = MaterialTheme.colorScheme.onSurface
    ): CardColors =
        CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )

    @Composable
    fun elevation(): CardElevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
}
