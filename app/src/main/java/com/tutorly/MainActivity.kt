package com.tutorly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tutorly.navigation.AppNavRoot
import com.tutorly.ui.UserProfileViewModel
import com.tutorly.ui.screens.WelcomeScreen
import com.tutorly.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay


@AndroidEntryPoint // ✅ подключает Hilt к Activity (чтобы внутри Compose работали hiltViewModel() и т.д.)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val profileViewModel: UserProfileViewModel = hiltViewModel()
            val profileState by profileViewModel.profile.collectAsStateWithLifecycle()
            var showWelcome by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(1800)
                showWelcome = false
            }

            AppTheme(preset = profileState.theme) {
                if (showWelcome) {
                    WelcomeScreen()
                } else {
                    AppNavRoot()
                }
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
//                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
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

