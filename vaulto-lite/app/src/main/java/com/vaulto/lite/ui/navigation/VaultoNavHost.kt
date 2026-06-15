package com.vaulto.lite.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vaulto.lite.ui.MainViewModel
import com.vaulto.lite.ui.screens.addexpense.AddExpenseScreen
import com.vaulto.lite.ui.screens.analytics.AnalyticsScreen
import com.vaulto.lite.ui.screens.home.HomeScreen
import com.vaulto.lite.ui.screens.settings.SettingsScreen

@Composable
fun VaultoNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { VaultoBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = VaultoDestination.Home.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(VaultoDestination.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onAddExpense = { navController.navigate(VaultoDestination.AddExpense.route(null)) },
                    onEditExpense = { id -> navController.navigate(VaultoDestination.AddExpense.route(id)) },
                    onSetBudget = { navController.navigate(VaultoDestination.Settings.route) }
                )
            }
            composable(VaultoDestination.Analytics.route) {
                AnalyticsScreen(viewModel = viewModel)
            }
            composable(VaultoDestination.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
            composable(VaultoDestination.AddExpense.route) { backStackEntry ->
                val expenseIdArg = backStackEntry.arguments
                    ?.getString(VaultoDestination.AddExpense.ARG_EXPENSE_ID)
                    ?.toLongOrNull() ?: -1L
                AddExpenseScreen(
                    viewModel = viewModel,
                    expenseId = expenseIdArg.takeIf { it != -1L },
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun VaultoBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        VaultoDestination.bottomNavItems.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            val (icon, label) = when (destination) {
                VaultoDestination.Home -> Icons.Filled.Home to "Home"
                VaultoDestination.Analytics -> Icons.Filled.PieChart to "Analytics"
                VaultoDestination.Settings -> Icons.Filled.Settings to "Settings"
                else -> Icons.Filled.Home to ""
            }
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}
