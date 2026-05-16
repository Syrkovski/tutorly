package com.tutorly.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tutorly.navigation.ROUTE_CALENDAR
import com.tutorly.navigation.ROUTE_FINANCE
import com.tutorly.navigation.ROUTE_STUDENTS
import com.tutorly.navigation.ROUTE_TODAY
import com.tutorly.ui.theme.SecondaryTextColor
import com.tutorly.ui.theme.TutorlyColors
import com.tutorly.ui.theme.TutorlyElevation
import com.tutorly.ui.theme.TutorlyRadii
import com.tutorly.ui.theme.TutorlySizing
import com.tutorly.ui.theme.TutorlySpacing
import com.tutorly.ui.theme.TutorlyComponentSpacing

enum class Tab(val route:String, val label:String, val icon: androidx.compose.ui.graphics.vector.ImageVector){
    Calendar(ROUTE_CALENDAR, "Расписание", Icons.Outlined.CalendarMonth),
    Today(ROUTE_TODAY, "Сегодня", Icons.Outlined.AssignmentTurnedIn),
    Students(ROUTE_STUDENTS, "Ученики", Icons.Outlined.People),
    Finance(ROUTE_FINANCE, "Финансы", Icons.Outlined.AttachMoney),
}

@Composable
fun AppBottomBar(currentRoute: String, onSelect:(String)->Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Surface(
        color = TutorlyColors.bottomBarContainer,
        tonalElevation = TutorlyElevation.bottomBarTonal,
        shadowElevation = TutorlyElevation.bottomBarShadow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TutorlySizing.bottomBarHeight)
                .padding(horizontal = TutorlySpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tab.entries.forEach { tab ->
                val selected = tab.route == currentRoute
                val interactionSource = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(TutorlyRadii.tabItem))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onSelect(tab.route) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(TutorlySpacing.xxs))
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(TutorlySizing.tabIndicatorHeight)
//                            .clip(RoundedCornerShape(TutorlyRadii.tabIndicator))
//                            .background(if (selected) primaryColor else Color.Transparent)
//                    )
                    Spacer(modifier = Modifier.height(TutorlyComponentSpacing.tabIndicatorToIcon))
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) TutorlyColors.textPrimary else TutorlyColors.textSecondary,
                        modifier = Modifier.size(TutorlySizing.navIcon)
                    )
                    Spacer(modifier = Modifier.height(TutorlySpacing.xs))
                    Text(
                        text = tab.label,
                        color = if (selected) TutorlyColors.textPrimary else TutorlyColors.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}


