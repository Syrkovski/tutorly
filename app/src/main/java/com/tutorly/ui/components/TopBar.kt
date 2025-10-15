package com.tutorly.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tutorly.R
import com.tutorly.ui.theme.PrimaryTextColor
import com.tutorly.ui.theme.TopBarGradientEnd
import com.tutorly.ui.theme.TopBarGradientStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onAddClick: (() -> Unit)? = null) {
    GradientTopBarContainer {
        TopAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            title = {
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = PrimaryTextColor
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = PrimaryTextColor
            ),
            windowInsets = WindowInsets(0, 0, 0, 0),
            actions = {
                onAddClick?.let {
                    FilledTonalIconButton(
                        onClick = it,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_student))
                    }
                }
            }
        )
    }
}

@Composable
fun GradientTopBarContainer(content: @Composable () -> Unit) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(TopBarGradientStart, TopBarGradientEnd)
                )
            )
    ) {
        Spacer(modifier = Modifier.height(statusBarPadding))
        content()
    }
}
