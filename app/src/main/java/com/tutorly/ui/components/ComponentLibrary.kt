package com.tutorly.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tutorly.ui.theme.TutorlyColors
import com.tutorly.ui.theme.TutorlyElevation
import com.tutorly.ui.theme.TutorlyRadii
import com.tutorly.ui.theme.TutorlySpacing

@Composable
fun TutorlyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    androidx.compose.material3.Button(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }
}

@Composable
fun TutorlyCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(TutorlyRadii.card),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = TutorlyElevation.cardSoft)
    ) { Box(Modifier.padding(TutorlySpacing.lg)) { content() } }
}

@Composable
fun LessonCard(title: String, subtitle: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    TutorlyCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun StudentCard(name: String, details: String, modifier: Modifier = Modifier, avatarText: String = name.take(1)) {
    TutorlyCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TutorlySpacing.md)) {
            Avatar(label = avatarText)
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EarningsCard(title: String, amount: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TutorlyRadii.cardLarge),
        colors = CardDefaults.cardColors(containerColor = TutorlyColors.softContainer)
    ) {
        Column(Modifier.padding(TutorlySpacing.lg)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TutorlyColors.textSecondary)
            Spacer(Modifier.width(2.dp))
            Text(amount, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TutorlyColors.contentColor)
        }
    }
}

@Composable
fun EmptyState(title: String, description: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(TutorlySpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TutorlySpacing.sm)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }
}

@Composable
fun FAB(onClick: () -> Unit, icon: ImageVector, contentDescription: String, modifier: Modifier = Modifier) {
    FloatingActionButton(onClick = onClick, modifier = modifier, containerColor = MaterialTheme.colorScheme.primary) {
        Icon(icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onSelectedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = { onSelectedChange(!selected) },
        label = { Text(label) },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
fun FilterChipGroup(
    options: List<String>,
    selectedOptions: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(TutorlySpacing.sm)) {
        items(options) { option ->
            FilterChip(
                label = option,
                selected = selectedOptions.contains(option),
                onSelectedChange = { isSelected ->
                val next = selectedOptions.toMutableSet()
                if (isSelected) next.add(option) else next.remove(option)
                    onSelectionChange(next)
                }
            )
        }
    }
}

@Composable
fun PremiumBanner(title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(TutorlyRadii.cardLarge)
    ) {
        Box(
            Modifier.background(
                Brush.horizontalGradient(listOf(TutorlyColors.premiumGradientStart, TutorlyColors.premiumGradientEnd))
            ).padding(TutorlySpacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                }
                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
fun Avatar(label: String, modifier: Modifier = Modifier, background: Color = TutorlyColors.textPrimary, content: Color = Color.White) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) { Text(label, color = content, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun MenuItem(title: String, modifier: Modifier = Modifier, subtitle: String? = null, leading: (@Composable () -> Unit)? = null, onClick: () -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = TutorlySpacing.lg, vertical = TutorlySpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TutorlySpacing.md)
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Поиск"
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(TutorlyRadii.segmented)
    )
}

@Composable
fun ScheduleLessonCard(
    studentName: String,
    subtitle: String?,
    statusIcon: String,
    statusLabel: String,
    badgeContainerColor: Color,
    badgeContentColor: Color,
    amountText: String,
    statusStripeColor: Color,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    elevation: CardElevation = CardDefaults.cardElevation(
        defaultElevation = 6.dp,
        pressedElevation = 6.dp,
        focusedElevation = 6.dp,
        hoveredElevation = 6.dp,
        draggedElevation = 6.dp,
        disabledElevation = 0.dp
    ),
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    stripeWidth: Dp = 4.dp
) {
    val content: @Composable ColumnScope.() -> Unit = {
        Box(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(2f)) {
                    Text(studentName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Visible)
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(999.dp), color = badgeContainerColor) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 6.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(16.dp).background(badgeContentColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(statusIcon, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(statusLabel, color = badgeContentColor, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text(amountText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .matchParentSize()
            ) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxHeight()
//                        .width(stripeWidth)
//                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
//                        .background(statusStripeColor)
//                )
            }
        }
    }

    if (onClick != null) {
        Card(
            modifier = modifier,
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = elevation,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = elevation,
            content = content
        )
    }
}
