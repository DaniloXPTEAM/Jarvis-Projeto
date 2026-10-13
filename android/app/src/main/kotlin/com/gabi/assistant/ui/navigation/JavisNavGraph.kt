package com.gabi.assistant.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.gabi.assistant.ui.screens.*
import com.gabi.assistant.ui.theme.DarkBg
import com.gabi.assistant.ui.viewmodel.OnboardingViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Chat : Screen("chat")
    object Memory : Screen("memory")
    object Notifications : Screen("notifications")
    object Settings : Screen("settings")
    object Permissions : Screen("permissions")
    object Files : Screen("files")
    object Room : Screen("room")
}

@Composable
fun JavisNavGraph(
    navController: NavHostController = rememberNavController(),
    autoStartListening: MutableState<Boolean>? = null
) {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val userOnboarded by onboardingViewModel.userOnboarded.collectAsState()

    // Espera resolver se já passou pelas Permissões antes de montar o grafo — assim a
    // tela inicial certa (Permissões na primeira vez, Home depois) já sai correta de
    // cara, sem "flashar" a tela errada por um instante.
    val onboarded = userOnboarded
    if (onboarded == null) {
        Box(modifier = Modifier.fillMaxSize().background(DarkBg))
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (onboarded) Screen.Home.route else Screen.Permissions.route,
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 8 } },
        exitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 8 } },
        popEnterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 8 } },
        popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { it / 8 } }
    ) {
        composable(Screen.Home.route) { HomeScreen(navController = navController, autoStartListening = autoStartListening) }
        composable(Screen.Chat.route) { ChatScreen(navController = navController) }
        composable(Screen.Memory.route) { MemoryScreen(navController = navController) }
        composable(Screen.Notifications.route) { NotificationsScreen(navController = navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
        composable(Screen.Permissions.route) { PermissionsScreen(navController = navController) }
        composable(Screen.Files.route) { FilesScreen(navController = navController) }
        composable(Screen.Room.route) { RoomScreen(navController = navController) }
    }
}
