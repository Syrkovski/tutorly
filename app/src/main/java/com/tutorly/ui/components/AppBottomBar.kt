package com.tutorly.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import com.tutorly.navigation.ROUTE_CALENDAR
import com.tutorly.navigation.ROUTE_FINANCE
import com.tutorly.navigation.ROUTE_STUDENTS
import com.tutorly.navigation.ROUTE_TODAY
import androidx.compose.ui.graphics.Color

enum class Tab(val route:String, val label:String, val icon: androidx.compose.ui.graphics.vector.ImageVector){
    Calendar(ROUTE_CALENDAR, "Календарь", Icons.Outlined.CalendarMonth),
    Today(ROUTE_TODAY, "Сегодня", Icons.Outlined.AssignmentTurnedIn),
    Students(ROUTE_STUDENTS, "Ученики", Icons.Outlined.People),
    Finance(ROUTE_FINANCE, "Финансы", Icons.Outlined.AttachMoney),
}

@Composable
fun AppBottomBar(currentRoute: String, onSelect:(String)->Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        Tab.entries.forEach { tab ->
            val selected = tab.route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
