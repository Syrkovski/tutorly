package com.tutorly.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tutorly.navigation.ROUTE_CALENDAR
import com.tutorly.navigation.ROUTE_FINANCE
import com.tutorly.navigation.ROUTE_STUDENTS
import com.tutorly.navigation.ROUTE_TODAY
import com.tutorly.ui.theme.RoyalBlue

enum class Tab(val route:String, val label:String, val icon: androidx.compose.ui.graphics.vector.ImageVector){
    Calendar(ROUTE_CALENDAR, "Календарь", Icons.Outlined.CalendarMonth),
    Today(ROUTE_TODAY, "Сегодня", Icons.Outlined.AssignmentTurnedIn),
    Students(ROUTE_STUDENTS, "Ученики", Icons.Outlined.People),
    Finance(ROUTE_FINANCE, "Финансы", Icons.Outlined.AttachMoney),
}

@Composable
fun AppBottomBar(currentRoute: String, onSelect:(String)->Unit) {
    NavigationBar(containerColor = Color.White) {
        Tab.entries.forEach { tab ->
            val selected = tab.route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RoyalBlue,
                    selectedTextColor = RoyalBlue,
                    indicatorColor = RoyalBlue.copy(alpha = 0.08f)
                )
            )
        }
    }
}
