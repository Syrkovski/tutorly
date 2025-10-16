package com.tutorly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

// MainActivity.kt

import com.tutorly.navigation.AppNavRoot
import com.tutorly.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint // ✅ подключает Hilt к Activity (чтобы внутри Compose работали hiltViewModel() и т.д.)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                AppNavRoot() // твой корень навигации/экранов
            }
        }
    }
}


//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent { AppTheme { AppNavRoot() } }
//    }
//}


//@Composable
//fun TutorlyApp() {
//    val navController = rememberNavController()
//    Scaffold(
//        bottomBar = {
//            NavigationBar {
//                val currentDestination = navController
//                    .currentBackStackEntryAsState().value?.destination
//
//                Screen.items.forEach { screen ->
//                    NavigationBarItem(
//                        selected = currentDestination?.route == screen.route,
//                        onClick = {
//                            navController.navigate(screen.route) {
//                                popUpTo(navController.graph.startDestinationId) {
//                                    saveState = true
//                                }
//                                launchSingleTop = true
//                                restoreState = true
//                            }
//                        },
//                        icon = { Icon(AppIcons.Home, contentDescription = null) },
//                        label = { Text(screen.title) }
//                    )
//                }
//            }
//        }
//    ) { innerPadding ->
//        NavHost(
//            navController,
//            startDestination = Screen.Plan.route,
//            modifier = Modifier.padding(innerPadding)
//        ) {
//            composable(Screen.Plan.route) { PlanScreen() }
//            composable(Screen.Today.route) { Text("Экран: Сегодня") }
//            composable(Screen.Students.route) { Text("Экран: Ученики") }
//            composable(Screen.Finance.route) { Text("Экран: Финансы") }
//        }
//    }
//}

