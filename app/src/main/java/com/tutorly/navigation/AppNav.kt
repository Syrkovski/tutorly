package com.tutorly.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import androidx.navigation.compose.dialog
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.tutorly.ui.CalendarMode
import com.tutorly.ui.CalendarScreen
import com.tutorly.ui.CalendarViewModel
import com.tutorly.ui.lessoncreation.LessonCreationConfig
import com.tutorly.ui.lessoncreation.LessonCreationOrigin
import com.tutorly.ui.lessoncreation.LessonCreationViewModel
import com.tutorly.ui.components.AppBottomBar
import com.tutorly.ui.components.AppTopBar
import com.tutorly.ui.screens.*
import java.time.LocalDate
import java.time.ZonedDateTime
import com.tutorly.ui.theme.ScreenGradientEnd
import com.tutorly.ui.theme.ScreenGradientStart

const val ROUTE_CALENDAR = "calendar"
private const val ROUTE_CALENDAR_PATTERN = "${ROUTE_CALENDAR}?${CalendarViewModel.ARG_ANCHOR_DATE}={${CalendarViewModel.ARG_ANCHOR_DATE}}&${CalendarViewModel.ARG_CALENDAR_MODE}={${CalendarViewModel.ARG_CALENDAR_MODE}}"
const val ROUTE_TODAY = "today"
const val ROUTE_STUDENTS = "students"
private const val ARG_STUDENT_EDITOR_ORIGIN = "studentEditorOrigin"
private const val ROUTE_STUDENTS_PATTERN = "$ROUTE_STUDENTS?$ARG_STUDENT_EDITOR_ORIGIN={$ARG_STUDENT_EDITOR_ORIGIN}"
const val ROUTE_FINANCE = "finance"
const val ROUTE_STUDENT_DETAILS = "student/{studentId}"
private const val ROUTE_STUDENT_EDIT_BASE = "student/{studentId}/edit"
private const val ARG_STUDENT_EDIT_TARGET = "editTarget"
const val ROUTE_STUDENT_EDIT = "$ROUTE_STUDENT_EDIT_BASE?$ARG_STUDENT_EDIT_TARGET={$ARG_STUDENT_EDIT_TARGET}"
private fun studentDetailsRoute(studentId: Long) = ROUTE_STUDENT_DETAILS.replace("{studentId}", studentId.toString())
private fun studentEditRoute(studentId: Long, target: StudentEditTarget? = null): String {
    val base = ROUTE_STUDENT_EDIT_BASE.replace("{studentId}", studentId.toString())
    return if (target != null) {
        "$base?$ARG_STUDENT_EDIT_TARGET=${target.name}"
    } else {
        base
    }
}

@OptIn(
    ExperimentalSharedTransitionApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun AppNavRoot() {
    val nav = rememberAnimatedNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destinationRoute = backStack?.destination?.route ?: ROUTE_CALENDAR
    val route = destinationRoute.substringBefore("?")

    // какой топбар показывать
    val showGlobalTopBar = when (route) {
        ROUTE_STUDENTS, ROUTE_FINANCE -> true   // тут простой заголовок уместен
        else -> false                   // calendar/today рисуют верх сами
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ScreenGradientStart, ScreenGradientEnd)
                )
            )
    ) {
        Scaffold(
            topBar = {
                if (showGlobalTopBar) {
                    AppTopBar(
                        title = when (route) {
                            ROUTE_STUDENTS -> "Ученики"
                            ROUTE_FINANCE -> "Финансы"
                            else -> ""
                        },
                        onAddClick = null
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
            containerColor = Color.Transparent,
            // чтобы контент корректно учитывал статус/навигационные панели
            contentWindowInsets = WindowInsets.systemBars
        ) { innerPadding ->
            SharedTransitionLayout {
                val sharedScope = this
                AnimatedNavHost(
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
                        onAddStudent = {
                            nav.navigate("$ROUTE_STUDENTS?$ARG_STUDENT_EDITOR_ORIGIN=${StudentEditorOrigin.LESSON_CREATION.name}") {
                                launchSingleTop = true
                            }
                        },
                        creationViewModel = creationViewModel
                    )
                }
                composable(ROUTE_TODAY) {
                    TodayScreen(
                        onAddLesson = {
                            nav.navigate(calendarRoute(nav)) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onAddStudent = {
                            nav.navigate("$ROUTE_STUDENTS?$ARG_STUDENT_EDITOR_ORIGIN=${StudentEditorOrigin.LESSON_CREATION.name}") {
                                launchSingleTop = true
                            }
                        },
                        onOpenStudentProfile = { studentId ->
                            nav.navigate(studentDetailsRoute(studentId)) {
                                launchSingleTop = true
                            }
                        },
                        onOpenDebtors = {
                            nav.navigate(ROUTE_FINANCE) {
                                launchSingleTop = true
                            }
                        }
                    )
                }      // сам рисует свой верх (заголовок + счетчики)
                composable(
                    route = ROUTE_STUDENTS_PATTERN,
                    arguments = listOf(
                        navArgument(ARG_STUDENT_EDITOR_ORIGIN) {
                            type = NavType.StringType
                            defaultValue = StudentEditorOrigin.NONE.name
                        }
                    )
                ) { entry ->
                    val calendarEntry =
                        remember(nav) { nav.getBackStackEntry(ROUTE_CALENDAR_PATTERN) }
                    val creationViewModel: LessonCreationViewModel = hiltViewModel(calendarEntry)
                    val originName = entry.arguments?.getString(ARG_STUDENT_EDITOR_ORIGIN).orEmpty()
                    val origin =
                        runCatching { StudentEditorOrigin.valueOf(originName) }.getOrDefault(
                            StudentEditorOrigin.NONE
                        )
                    StudentsScreen(
                        onStudentEdit = { id ->
                            nav.navigate(studentEditRoute(id)) {
                                launchSingleTop = true
                            }
                        },
                        onAddLesson = { studentId ->
                            creationViewModel.start(
                                LessonCreationConfig(
                                    studentId = studentId,
                                    zoneId = ZonedDateTime.now().zone,
                                    origin = LessonCreationOrigin.STUDENT
                                )
                            )
                            nav.navigate(calendarRoute(nav)) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onStudentOpen = { id ->
                            nav.navigate(studentDetailsRoute(id)) {
                                launchSingleTop = true
                            }
                        },
                        onStudentCreatedFromLesson = { newId ->
                            val reopened = creationViewModel.onStudentCreated(newId)
                            nav.navigate(calendarRoute(nav)) {
                                launchSingleTop = true
                                restoreState = true
                            }
                            if (!reopened) {
                                creationViewModel.dismiss()
                            }
                        },
                        initialEditorOrigin = origin,
                        sharedTransitionScope = sharedScope,
                        animatedVisibilityScope = this
                    )
                }
                composable(
                    route = ROUTE_STUDENT_DETAILS,
                    arguments = listOf(navArgument("studentId") { type = NavType.LongType })
                ) { entry ->
                    val studentId = entry.arguments?.getLong("studentId") ?: return@composable
                    val calendarEntry =
                        remember(nav) { nav.getBackStackEntry(ROUTE_CALENDAR_PATTERN) }
                    val creationViewModel: LessonCreationViewModel = hiltViewModel(calendarEntry)
                    StudentDetailsScreen(
                        onBack = { nav.popBackStack() },
                        onAddStudentFromCreation = {
                            nav.navigate("$ROUTE_STUDENTS?$ARG_STUDENT_EDITOR_ORIGIN=${StudentEditorOrigin.LESSON_CREATION.name}") {
                                launchSingleTop = true
                            }
                        },
                        creationViewModel = creationViewModel
                    )
                }
                dialog(
                    route = ROUTE_STUDENT_EDIT,
                    arguments = listOf(
                        navArgument("studentId") { type = NavType.LongType },
                        navArgument(ARG_STUDENT_EDIT_TARGET) {
                            type = NavType.StringType
                            defaultValue = StudentEditTarget.PROFILE.name
                        }
                    ),
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    StudentEditorDialog(
                        onDismiss = { nav.popBackStack() },
                        onSaved = {
                            nav.popBackStack()
                        }
                    )
                }
                composable(ROUTE_FINANCE) { FinanceScreen() }
            }
        }
    }
}}

fun calendarRoute(nav: NavHostController): String {
    val entry = runCatching { nav.getBackStackEntry(ROUTE_CALENDAR_PATTERN) }.getOrNull()
    val savedDate = entry?.savedStateHandle?.get<String>(CalendarViewModel.ARG_ANCHOR_DATE)
    val savedMode = entry?.savedStateHandle?.get<String>(CalendarViewModel.ARG_CALENDAR_MODE)
    return buildCalendarRoute(savedDate, savedMode)
}

fun buildCalendarRoute(date: String?, mode: String?): String {
    val anchor = date?.takeIf { it.isNotBlank() } ?: LocalDate.now().toString()
    val tab = mode?.takeIf { it.isNotBlank() } ?: CalendarMode.DAY.name
    return "${ROUTE_CALENDAR}?${CalendarViewModel.ARG_ANCHOR_DATE}=$anchor&${CalendarViewModel.ARG_CALENDAR_MODE}=$tab"
}
