package com.tutorly.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

object TutorlyCardDefaults {
    @Composable
    fun containerColor(): Color {
        val colorScheme = MaterialTheme.colorScheme
        val base = colorScheme.surfaceContainerLowest
        val surface = colorScheme.surface
        return if (base.luminance() > surface.luminance()) {
            base
        } else {
            colorScheme.surfaceContainerHighest
        }
    }

    @Composable
    fun colors(): CardColors = CardDefaults.cardColors(containerColor = containerColor())

    @Composable
    fun elevation(): CardElevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
}
