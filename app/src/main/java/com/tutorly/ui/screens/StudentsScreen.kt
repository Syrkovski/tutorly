package com.tutorly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.R
import com.tutorly.ui.components.PaymentBadge
import kotlin.math.absoluteValue

@Composable
fun StudentsScreen(
    onStudentClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    vm: StudentsViewModel = hiltViewModel(),
) {
    val query by vm.query.collectAsState()
    val students by vm.students.collectAsState()

    Scaffold { innerPadding ->
        Column(
            modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(text = stringResource(id = R.string.search_students_hint)) }
            )

            Spacer(Modifier.height(16.dp))

            if (students.isEmpty()) {
                EmptyStudentsState(Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = students,
                        key = { it.student.id }
                    ) { item ->
                        StudentCard(
                            item = item,
                            onClick = { onStudentClick(item.student.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStudentsState(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(id = R.string.students_empty_state),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StudentCard(
    item: StudentsViewModel.StudentListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColors = remember(item.student.name) { studentAccentColors(item.student.name) }
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                spotColor = accentColors.primary.copy(alpha = 0.18f),
                ambientColor = accentColors.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    accentColors.primary.copy(alpha = 0.35f),
                    accentColors.secondary.copy(alpha = 0.35f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StudentAvatar(
                name = item.student.name,
                accentColors = accentColors
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.student.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.hasDebt) {
                        Spacer(Modifier.width(8.dp))
                        PaymentBadge(paid = false)
                    }
                }

                item.student.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = note.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val contacts = buildContactLine(item)
                if (contacts != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = contacts,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun buildContactLine(item: StudentsViewModel.StudentListItem): String? {
    val contactParts = buildList {
        item.student.phone?.takeIf { it.isNotBlank() }?.let { add(it.trim()) }
        item.student.messenger?.takeIf { it.isNotBlank() }?.let { add(it.trim()) }
    }
    if (contactParts.isEmpty()) return null
    return contactParts.joinToString(separator = " · ")
}

private data class StudentAccentColors(val primary: Color, val secondary: Color)

private fun studentAccentColors(name: String): StudentAccentColors {
    val palette = listOf(
        Pair(Color(0xFF6A6CFF), Color(0xFF8A8CFF)),
        Pair(Color(0xFFFF6A88), Color(0xFFFF8BAA)),
        Pair(Color(0xFF2EC5CE), Color(0xFF5DDADB)),
        Pair(Color(0xFFF9A826), Color(0xFFFAC858)),
        Pair(Color(0xFF7B61FF), Color(0xFF9D7BFF)),
        Pair(Color(0xFF00BFA6), Color(0xFF4ADEB4))
    )
    val index = name.hashCode().absoluteValue % palette.size
    val (primary, secondary) = palette[index]
    return StudentAccentColors(primary = primary, secondary = secondary)
}

@Composable
private fun StudentAvatar(
    name: String,
    accentColors: StudentAccentColors,
) {
    val initials = name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(separator = "") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColors.primary,
                        accentColors.secondary
                    ),
                    tileMode = TileMode.Clamp
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }
}
