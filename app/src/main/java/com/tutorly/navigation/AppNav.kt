package com.tutorly.navigation

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.tutorly.ui.CalendarMode
import com.tutorly.ui.CalendarScreen
import com.tutorly.ui.CalendarViewModel
import com.tutorly.ui.lessoncreation.LessonCreationConfig
import com.tutorly.ui.lessoncreation.LessonCreationOrigin
import com.tutorly.ui.lessoncreation.LessonCreationViewModel
import com.tutorly.ui.components.AppBottomBar
import com.tutorly.ui.components.AppTopBar
import com.tutorly.ui.screens.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime


const val ROUTE_CALENDAR = "calendar"
private const val ROUTE_CALENDAR_PATTERN = "${ROUTE_CALENDAR}?${CalendarViewModel.ARG_ANCHOR_DATE}={${CalendarViewModel.ARG_ANCHOR_DATE}}&${CalendarViewModel.ARG_CALENDAR_MODE}={${CalendarViewModel.ARG_CALENDAR_MODE}}"
const val ROUTE_TODAY = "today"
const val ROUTE_STUDENTS = "students"
const val ROUTE_FINANCE = "finance"
const val ROUTE_STUDENT_NEW = "student/new"
const val ROUTE_STUDENT_DETAILS = "student/{studentId}"
const val ROUTE_STUDENT_EDIT = "student/{studentId}/edit"
const val ROUTE_LESSON_NEW = "lesson/new"
const val ROUTE_LESSON_DETAILS = "lesson/{lessonId}"

private const val ARG_LESSON_ID = "lessonId"
private const val ARG_START_TIME = "startTime"
private const val ARG_STUDENT_ID = "studentId"
private const val NO_STUDENT_ID = -1L

private fun studentDetailsRoute(studentId: Long) = ROUTE_STUDENT_DETAILS.replace("{studentId}", studentId.toString())
private fun studentEditRoute(studentId: Long) = ROUTE_STUDENT_EDIT.replace("{studentId}", studentId.toString())

@Composable
fun AppNavRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destinationRoute = backStack?.destination?.route ?: ROUTE_CALENDAR
    val route = destinationRoute.substringBefore("?")

    // какой топбар показывать
    val showGlobalTopBar = when (route) {
        ROUTE_STUDENTS, ROUTE_FINANCE -> true   // тут простой заголовок уместен
        else -> false                   // calendar/today рисуют верх сами
    }

    Scaffold(
        topBar = {
            if (showGlobalTopBar) {
                AppTopBar(
                    title = when (route) {
                        ROUTE_STUDENTS -> "Ученики"
                        ROUTE_FINANCE -> "Финансы"
                        else -> ""
                    },
                    onAddClick = when (route) {
                        ROUTE_STUDENTS -> ({
                            nav.navigate(ROUTE_STUDENT_NEW) {
                                launchSingleTop = true
                            }
                        })
                        else -> null
                    }
                )
            }
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = route,
                onSelect = { dest ->
                    val target = if (dest == ROUTE_CALENDAR) {
                        calendarRoute(nav)
                    } else {
                        dest
                    }
                    nav.navigate(target) {
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
            startDestination = ROUTE_CALENDAR_PATTERN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                route = ROUTE_CALENDAR_PATTERN,
                arguments = listOf(
                    navArgument(CalendarViewModel.ARG_ANCHOR_DATE) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(CalendarViewModel.ARG_CALENDAR_MODE) {
                        type = NavType.StringType
                        defaultValue = CalendarMode.DAY.name
                    }
                )
            ) { entry ->
                val creationViewModel: LessonCreationViewModel = hiltViewModel(entry)
                CalendarScreen(
                    onLessonDetails = { lessonId, studentId, start ->
                        nav.navigate(lessonDetailsRoute(lessonId, studentId, start)) {
                            launchSingleTop = true
                        }
                    },
                    onAddStudent = {
                        nav.navigate(ROUTE_STUDENT_NEW) {
                            launchSingleTop = true
                        }
                    },
                    creationViewModel = creationViewModel
                )
            }
            composable(ROUTE_TODAY)    { TodayScreen() }      // сам рисует свой верх (заголовок + счетчики)
            composable(ROUTE_STUDENTS) {
                StudentsScreen(
                    onStudentClick = { id ->
                        nav.navigate(studentDetailsRoute(id)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(ROUTE_STUDENT_NEW) {
                val calendarEntry = remember(nav) { nav.getBackStackEntry(ROUTE_CALENDAR_PATTERN) }
                val creationViewModel: LessonCreationViewModel = hiltViewModel(calendarEntry)
                StudentEditorScreen(
                    onClose = { nav.popBackStack() },
                    onSaved = { newId ->
                        nav.popBackStack()
                        val reopened = creationViewModel.onStudentCreated(newId)
                        if (reopened) {
                            nav.navigate(calendarRoute(nav)) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            nav.navigate(studentDetailsRoute(newId)) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            composable(
                route = ROUTE_STUDENT_DETAILS,
                arguments = listOf(navArgument("studentId") { type = NavType.LongType })
            ) { entry ->
                val studentId = entry.arguments?.getLong("studentId") ?: return@composable
                val calendarEntry = remember(nav) { nav.getBackStackEntry(ROUTE_CALENDAR_PATTERN) }
                val creationViewModel: LessonCreationViewModel = hiltViewModel(calendarEntry)
                StudentDetailsScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = {
                        nav.navigate(studentEditRoute(studentId)) {
                            launchSingleTop = true
                        }
                    },
                    onCreateLesson = { student ->
                        creationViewModel.start(
                            LessonCreationConfig(
                                studentId = student.id,
                                zoneId = ZonedDateTime.now().zone,
                                origin = LessonCreationOrigin.STUDENT
                            )
                        )
                        nav.navigate(calendarRoute(nav)) {
                            launchSingleTop = true
                            restoreState = true
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
            composable(
                route = "$ROUTE_LESSON_NEW?$ARG_START_TIME={$ARG_START_TIME}&$ARG_STUDENT_ID={$ARG_STUDENT_ID}",
                arguments = listOf(
                    navArgument(ARG_START_TIME) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(ARG_STUDENT_ID) {
                        type = NavType.LongType
                        defaultValue = NO_STUDENT_ID
                    }
                )
            ) { entry ->
                val startInstant = entry.arguments?.getString(ARG_START_TIME).toInstantOrNull()
                val studentId = entry.arguments?.getLong(ARG_STUDENT_ID)?.takeIf { it != NO_STUDENT_ID }
                LessonEditorScreen(
                    startTime = startInstant,
                    studentId = studentId,
                    onClose = { nav.popBackStack() }
                )
            }
            composable(
                route = "$ROUTE_LESSON_DETAILS?$ARG_START_TIME={$ARG_START_TIME}&$ARG_STUDENT_ID={$ARG_STUDENT_ID}",
                arguments = listOf(
                    navArgument(ARG_LESSON_ID) { type = NavType.LongType },
                    navArgument(ARG_START_TIME) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(ARG_STUDENT_ID) {
                        type = NavType.LongType
                        defaultValue = NO_STUDENT_ID
                    }
                )
            ) { entry ->
                val lessonId = entry.arguments?.getLong(ARG_LESSON_ID) ?: return@composable
                val startInstant = entry.arguments?.getString(ARG_START_TIME).toInstantOrNull()
                val studentId = entry.arguments?.getLong(ARG_STUDENT_ID)?.takeIf { it != NO_STUDENT_ID }
                LessonDetailsScreen(
                    lessonId = lessonId,
                    studentId = studentId,
                    startTime = startInstant,
                    onBack = { nav.popBackStack() }
                )
            }
            composable(ROUTE_FINANCE)  { FinanceScreen() }
        }
    }
}

private fun calendarRoute(nav: NavHostController): String {
    val entry = runCatching { nav.getBackStackEntry(ROUTE_CALENDAR_PATTERN) }.getOrNull()
    val savedDate = entry?.savedStateHandle?.get<String>(CalendarViewModel.ARG_ANCHOR_DATE)
    val savedMode = entry?.savedStateHandle?.get<String>(CalendarViewModel.ARG_CALENDAR_MODE)
    return buildCalendarRoute(savedDate, savedMode)
}

private fun buildCalendarRoute(date: String?, mode: String?): String {
    val anchor = date?.takeIf { it.isNotBlank() } ?: LocalDate.now().toString()
    val tab = mode?.takeIf { it.isNotBlank() } ?: CalendarMode.DAY.name
    return "${ROUTE_CALENDAR}?${CalendarViewModel.ARG_ANCHOR_DATE}=$anchor&${CalendarViewModel.ARG_CALENDAR_MODE}=$tab"
}

private fun lessonCreateRoute(start: ZonedDateTime, studentId: Long?): String {
    val params = listOf(
        "${ARG_START_TIME}=${Uri.encode(start.toInstant().toString())}",
        "${ARG_STUDENT_ID}=${studentId ?: NO_STUDENT_ID}"
    )
    return "${ROUTE_LESSON_NEW}?${params.joinToString("&")}"
}

private fun lessonDetailsRoute(lessonId: Long, studentId: Long, start: ZonedDateTime): String {
    val params = listOf(
        "${ARG_START_TIME}=${Uri.encode(start.toInstant().toString())}",
        "${ARG_STUDENT_ID}=$studentId"
    )
    return "lesson/$lessonId?${params.joinToString("&")}"
}

private fun String?.toInstantOrNull(): Instant? =
    this?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }

