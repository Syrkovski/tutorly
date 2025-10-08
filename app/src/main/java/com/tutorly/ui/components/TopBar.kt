package com.tutorly.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import com.tutorly.R
import com.tutorly.ui.theme.RoyalBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onAddClick: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = RoyalBlue,
            titleContentColor = Color.White
        ),
        actions = {
            onAddClick?.let {
                FilledIconButton(
                    onClick = it,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White,
                        contentColor = RoyalBlue
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_student))
                }
            }
        }
    )
}
