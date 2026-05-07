package com.phonewhisperer.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.phonewhisperer.presentation.screens.dashboard.DashboardScreen
import com.phonewhisperer.presentation.screens.rules.RulesScreen
import com.phonewhisperer.presentation.screens.settings.SettingsScreen
import com.phonewhisperer.presentation.screens.timeline.TimelineScreen
import com.phonewhisperer.presentation.theme.DarkCard
import com.phonewhisperer.presentation.theme.TextSecondary
import com.phonewhisperer.presentation.theme.WhispererPrimary

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Rounded.Dashboard)
    object Rules : Screen("rules", "AI Rules", Icons.Rounded.AutoAwesome)
    object Timeline : Screen("timeline", "Timeline", Icons.Rounded.Timeline)
    object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(Screen.Dashboard, Screen.Rules, Screen.Timeline, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkCard,
                contentColor = WhispererPrimary,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WhispererPrimary,
                            selectedTextColor = WhispererPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Rules.route) { RulesScreen() }
            composable(Screen.Timeline.route) { TimelineScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
