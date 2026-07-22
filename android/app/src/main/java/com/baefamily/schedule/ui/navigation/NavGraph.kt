package com.baefamily.schedule.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.baefamily.schedule.data.model.Schedule
import com.baefamily.schedule.data.model.UserProfile
import com.baefamily.schedule.ui.auth.LoginScreen
import com.baefamily.schedule.ui.auth.RoleSelectScreen
import com.baefamily.schedule.ui.auth.SignUpScreen
import com.baefamily.schedule.ui.calendar.CalendarScreen
import com.baefamily.schedule.ui.schedule.AddEditScheduleScreen
import com.baefamily.schedule.ui.settings.SettingsScreen
import com.baefamily.schedule.ui.timeline.TimelineScreen
import com.baefamily.schedule.viewmodel.AuthUiState
import com.baefamily.schedule.viewmodel.AuthViewModel
import com.baefamily.schedule.viewmodel.CalendarViewModel
import com.baefamily.schedule.viewmodel.ScheduleViewModel
import com.baefamily.schedule.viewmodel.SettingsViewModel
import com.baefamily.schedule.viewmodel.TimelineViewModel
import java.time.LocalDate

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: user's choice is respected either way */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()

    when (val state = authState) {
        is AuthUiState.Loading -> LoadingScreen()
        is AuthUiState.LoggedOut -> AuthNavHost(authViewModel)
        is AuthUiState.NeedsRoleSelection -> RoleSelectScreen(uid = state.uid, viewModel = authViewModel)
        is AuthUiState.LoggedIn -> HomeNavHost(currentUser = state.profile)
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AuthNavHost(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(viewModel = authViewModel, onNavigateToSignUp = { navController.navigate("signup") })
        }
        composable("signup") {
            SignUpScreen(viewModel = authViewModel, onNavigateToLogin = { navController.popBackStack() })
        }
    }
}

@Composable
private fun HomeNavHost(currentUser: UserProfile) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "tabs") {
        composable("tabs") {
            HomeTabsScreen(
                currentUser = currentUser,
                onAddSchedule = { date -> navController.navigate("schedule/new/$date") },
                onScheduleClick = { schedule -> navController.navigate("schedule/edit/${schedule.id}") }
            )
        }
        composable(
            route = "schedule/new/{date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val dateStr = backStackEntry.arguments?.getString("date") ?: LocalDate.now().toString()
            val scheduleViewModel: ScheduleViewModel = viewModel()
            AddEditScheduleScreen(
                viewModel = scheduleViewModel,
                currentUser = currentUser,
                scheduleId = null,
                initialDate = runCatching { LocalDate.parse(dateStr) }.getOrDefault(LocalDate.now()),
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            route = "schedule/edit/{scheduleId}",
            arguments = listOf(navArgument("scheduleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val scheduleId = backStackEntry.arguments?.getString("scheduleId") ?: return@composable
            val scheduleViewModel: ScheduleViewModel = viewModel()
            AddEditScheduleScreen(
                viewModel = scheduleViewModel,
                currentUser = currentUser,
                scheduleId = scheduleId,
                initialDate = LocalDate.now(),
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}

private enum class HomeTab(val label: String, val icon: ImageVector) {
    CALENDAR("캘린더", Icons.Default.CalendarMonth),
    TIMELINE("타임라인", Icons.Default.Timeline),
    SETTINGS("설정", Icons.Default.Settings)
}

@Composable
private fun HomeTabsScreen(
    currentUser: UserProfile,
    onAddSchedule: (LocalDate) -> Unit,
    onScheduleClick: (Schedule) -> Unit
) {
    var selectedTab by remember { mutableStateOf(HomeTab.CALENDAR) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                HomeTab.CALENDAR -> {
                    val calendarViewModel: CalendarViewModel = viewModel()
                    CalendarScreen(
                        viewModel = calendarViewModel,
                        onAddSchedule = onAddSchedule,
                        onScheduleClick = onScheduleClick
                    )
                }
                HomeTab.TIMELINE -> {
                    val timelineViewModel: TimelineViewModel = viewModel()
                    TimelineScreen(viewModel = timelineViewModel)
                }
                HomeTab.SETTINGS -> {
                    val settingsViewModel: SettingsViewModel = viewModel()
                    SettingsScreen(viewModel = settingsViewModel, currentUser = currentUser, onSignedOut = {})
                }
            }
        }
    }
}
