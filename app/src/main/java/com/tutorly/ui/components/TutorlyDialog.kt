package com.tutorly.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tutorly.ui.theme.TutorlySizing
import com.tutorly.ui.theme.TutorlySpacing
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorlyDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: ColumnScopeContent,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = TutorlySpacing.lg)
                .widthIn(max = TutorlySizing.dialogMaxWidth),
            shape = AlertDialogDefaults.shape,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = Color.White,
            contentColor = AlertDialogDefaults.textContentColor
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TutorlySpacing.xl, vertical = TutorlySpacing.lg)
                    .heightIn(max = TutorlySizing.dialogMaxHeight)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(TutorlySpacing.xl),
                    content = content
                )
            }
        }
    }
}

typealias ColumnScopeContent = @Composable ColumnScope.() -> Unit
