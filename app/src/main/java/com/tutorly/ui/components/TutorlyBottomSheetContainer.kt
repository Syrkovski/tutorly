package com.tutorly.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Wraps bottom-sheet content into a surface with rounded top corners so we can keep
 * the sheet background while allowing the modal container itself to stay transparent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorlyBottomSheetContainer(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    tonalElevation: Dp = 0.dp,
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable () -> Unit,
) {
    val containerColor = if (color == Color.Unspecified) {
        MaterialTheme.colorScheme.surfaceContainerLowest
    } else {
        color
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (dragHandle != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    dragHandle()
                }
            }

            content()
        }
    }
}
