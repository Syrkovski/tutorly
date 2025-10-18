package com.tutorly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tutorly.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onAddClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    navigationIcon: (@Composable () -> Unit)? = null
) {
    GradientTopBarContainer {
        TopAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .padding(start = 30.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            navigationIcon = {
                navigationIcon?.invoke()
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            windowInsets = WindowInsets(0, 0, 0, 0),
            actions = {
                actions()
                onAddClick?.let {
                    FilledTonalIconButton(
                        onClick = it,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.add_student)
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun GradientTopBarContainer(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(0.dp)
    val colorScheme = MaterialTheme.colorScheme
    val startColor = colorScheme.primary
    val endColor = colorScheme.primaryContainer
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = shape, clip = false),
        color = Color.Transparent,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(startColor, endColor)
                    )
                )
                .statusBarsPadding()
        ) {
            content()
        }
    }
}
