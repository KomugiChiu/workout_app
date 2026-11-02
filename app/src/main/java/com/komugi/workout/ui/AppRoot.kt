package com.komugi.workout.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.komugi.workout.data.WorkoutRepository
import com.komugi.workout.ui.guide.GuideDetailScreen
import com.komugi.workout.ui.guide.GuideListScreen
import com.komugi.workout.ui.log.LogDetailScreen
import com.komugi.workout.ui.log.LogListScreen
import com.komugi.workout.ui.menu.MenuDetailScreen
import com.komugi.workout.ui.menu.MenuListScreen

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppRoot(repository: WorkoutRepository) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    val tabs = listOf(
        Tab("menus", "菜單", Icons.AutoMirrored.Filled.List),
        Tab("logs", "紀錄", Icons.Filled.DateRange),
        Tab("guides", "動作說明", Icons.Filled.Info)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val selected = when (tab.route) {
                        "menus" -> route == "menus" || route?.startsWith("menu/") == true
                        "logs" -> route == "logs" || route?.startsWith("log/") == true
                        else -> route == "guides" || route?.startsWith("guide/") == true
                    }
                    NavigationBarItem(
                        selected = selected,
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
                    onOpenMenu = { navController.navigate("menu/${it.id}") }
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
            composable(
                route = "log/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { entry ->
                LogDetailScreen(
                    repository = repository,
                    sessionId = entry.arguments?.getLong("sessionId") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onOpenGuide = { navController.navigate("guide/$it") }
                )
            }
            composable("guides") {
                GuideListScreen(
                    repository = repository,
                    onOpenGuide = { navController.navigate("guide/${it.id}") }
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
        }
    }
}
