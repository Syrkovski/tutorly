package com.tutorly.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tutorly.ui.CalendarScreen
import com.tutorly.ui.components.AppBottomBar
import com.tutorly.ui.components.AppTopBar
import com.tutorly.ui.screens.*


const val ROUTE_STUDENTS = "students"
const val ROUTE_STUDENT_NEW = "student/new"
const val ROUTE_STUDENT_EDIT = "student/{studentId}"
const val ROUTE_LESSON_NEW = "lesson/new?studentId={studentId}" // под автоподстановку

private fun studentDetailsRoute(studentId: Long) = ROUTE_STUDENT_EDIT.replace("{studentId}", studentId.toString())

@Composable
fun AppNavRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "calendar"

    // какой топбар показывать
    val showGlobalTopBar = when (route) {
        "students", "finance" -> true   // тут простой заголовок уместен
        else -> false                   // calendar/today рисуют верх сами
    }

    Scaffold(
        topBar = {
            if (showGlobalTopBar) {
        AppTopBar(
            title = when(route){
                "students" -> "Ученики"
                "finance"  -> "Финансы"
                else -> ""
            },
            onAddClick = when(route){
                "students" -> ({ nav.navigate(ROUTE_STUDENT_NEW) })
                else -> null
            }
        )
    }
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = route,
                onSelect = { dest ->
                    nav.navigate(dest) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                    }
                }
            )
        },
        // чтобы контент корректно учитывал статус/навигационные панели
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = "calendar",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("calendar") { CalendarScreen() }   // сам рисует свой верх (месяц/табы/лента)
            composable("today")    { TodayScreen() }      // сам рисует свой верх (заголовок + счетчики)
            composable(ROUTE_STUDENTS) {
                StudentsScreen(
                    onStudentClick = { id -> nav.navigate(studentDetailsRoute(id)) },
                    onAddClick = { nav.navigate(ROUTE_STUDENT_NEW) }
                )
            }
            composable(ROUTE_STUDENT_NEW) {
                StudentEditorScreen(
                    onClose = { nav.popBackStack() },
                    onSaved = { newId ->
                        nav.popBackStack()
                        nav.navigate(studentDetailsRoute(newId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                route = ROUTE_STUDENT_EDIT,
                arguments = listOf(navArgument("studentId") { type = NavType.LongType })
            ) {
                StudentEditorScreen(
                    onClose = { nav.popBackStack() },
                    onSaved = {
                        nav.popBackStack()
                    }
                )
            }
            composable("finance")  { FinanceScreen() }
        }
    }
}

