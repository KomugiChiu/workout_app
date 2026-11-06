package com.komugi.workout.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.komugi.workout.data.WorkoutRepository
import com.komugi.workout.ui.body.BodyMetricsScreen
import com.komugi.workout.ui.guide.GuideDetailScreen
import com.komugi.workout.ui.guide.GuideListScreen
import com.komugi.workout.ui.library.ExerciseLibraryDetailScreen
import com.komugi.workout.ui.library.ExerciseLibraryScreen
import com.komugi.workout.ui.log.AddExercisesScreen
import com.komugi.workout.ui.log.ExerciseProgressScreen
import com.komugi.workout.ui.log.LogDetailScreen
import com.komugi.workout.ui.log.LogListScreen
import com.komugi.workout.ui.log.SessionSummaryScreen
import com.komugi.workout.ui.menu.MenuDetailScreen
import com.komugi.workout.ui.menu.MenuListScreen
import com.komugi.workout.ui.settings.SettingsScreen
import com.komugi.workout.ui.theme.ThemeManager

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppRoot(
    repository: WorkoutRepository,
    themeManager: ThemeManager
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    val tabs = listOf(
        Tab("menus", "菜單", Icons.AutoMirrored.Filled.List),
        Tab("logs", "紀錄", Icons.Filled.DateRange),
        Tab("body", "身體", Icons.Filled.Person),
        Tab("guides", "動作說明", Icons.Filled.Info),
        Tab("settings", "選項", Icons.Filled.Settings)
    )

    Scaffold(
        bottomBar = {
            val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val barContainer = if (darkTheme) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            }
            val barContent = if (darkTheme) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimary
            }

            NavigationBar(containerColor = barContainer) {
                tabs.forEach { tab ->
                    val selected = when (tab.route) {
                        "menus" -> route == "menus" || route?.startsWith("menu/") == true
                        "logs" -> route == "logs" || route?.startsWith("log/") == true ||
                            route?.startsWith("exercise/") == true
                        "body" -> route == "body"
                        "guides" -> route == "guides" || route?.startsWith("guide/") == true ||
                            route == "library" || route?.startsWith("library/") == true
                        else -> route == "settings"
                    }
                    NavigationBarItem(
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = barContent,
                            selectedTextColor = barContent,
                            indicatorColor = barContent.copy(alpha = 0.2f),
                            unselectedIconColor = barContent.copy(alpha = 0.65f),
                            unselectedTextColor = barContent.copy(alpha = 0.65f)
                        ),
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "menus",
            modifier = Modifier.padding(padding)
        ) {
            composable("menus") {
                MenuListScreen(
                    repository = repository,
                    onOpenMenu = { navController.navigate("menu/$it") }
                )
            }
            composable(
                route = "menu/{menuId}",
                arguments = listOf(navArgument("menuId") { type = NavType.StringType })
            ) { entry ->
                MenuDetailScreen(
                    repository = repository,
                    menuId = entry.arguments?.getString("menuId"),
                    onBack = { navController.popBackStack() },
                    onOpenGuide = { navController.navigate("guide/$it") },
                    onOpenExercise = { navController.navigate("library/$it") },
                    onSessionCreated = { sessionId ->
                        navController.navigate("log/$sessionId")
                    }
                )
            }
            composable("logs") {
                LogListScreen(
                    repository = repository,
                    onOpenSession = { navController.navigate("log/$it") }
                )
            }
            composable("body") {
                BodyMetricsScreen(repository = repository)
            }
            composable(
                route = "log/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { entry ->
                val sessionId = entry.arguments?.getLong("sessionId") ?: 0L
                LogDetailScreen(
                    repository = repository,
                    sessionId = sessionId,
                    onBack = { navController.popBackStack() },
                    onOpenGuide = { navController.navigate("guide/$it") },
                    onOpenSummary = { navController.navigate("log/$sessionId/summary") },
                    onOpenExerciseHistory = { navController.navigate("exercise/$it") },
                    onOpenAddExercises = { navController.navigate("log/$sessionId/add") }
                )
            }
            composable(
                route = "log/{sessionId}/add",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { entry ->
                AddExercisesScreen(
                    repository = repository,
                    sessionId = entry.arguments?.getLong("sessionId") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() }
                )
            }
            composable(
                route = "log/{sessionId}/summary",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { entry ->
                SessionSummaryScreen(
                    repository = repository,
                    sessionId = entry.arguments?.getLong("sessionId") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onOpenExerciseHistory = { navController.navigate("exercise/$it") }
                )
            }
            composable(
                route = "exercise/{entryId}",
                arguments = listOf(navArgument("entryId") { type = NavType.LongType })
            ) { entry ->
                ExerciseProgressScreen(
                    repository = repository,
                    entryId = entry.arguments?.getLong("entryId") ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("guides") {
                GuideListScreen(
                    repository = repository,
                    onOpenGuide = { navController.navigate("guide/${it.id}") },
                    onOpenLibrary = { navController.navigate("library") }
                )
            }
            composable("library") {
                ExerciseLibraryScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onOpenExercise = { navController.navigate("library/$it") }
                )
            }
            composable(
                route = "library/{exerciseId}",
                arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
            ) { entry ->
                ExerciseLibraryDetailScreen(
                    repository = repository,
                    exerciseId = entry.arguments?.getLong("exerciseId") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onOpenExercise = { navController.navigate("library/$it") },
                    onOpenGuide = { navController.navigate("guide/$it") }
                )
            }
            composable(
                route = "guide/{guideId}",
                arguments = listOf(navArgument("guideId") { type = NavType.StringType })
            ) { entry ->
                GuideDetailScreen(
                    repository = repository,
                    guideId = entry.arguments?.getString("guideId"),
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    repository = repository,
                    themeManager = themeManager
                )
            }
        }
    }
}
