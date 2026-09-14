package com.houshmandhesab.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.houshmandhesab.app.R
import com.houshmandhesab.app.ui.screens.AccountsScreen
import com.houshmandhesab.app.ui.screens.AddTransactionScreen
import com.houshmandhesab.app.ui.screens.AiScreen
import com.houshmandhesab.app.ui.screens.BudgetsScreen
import com.houshmandhesab.app.ui.screens.CategoriesScreen
import com.houshmandhesab.app.ui.screens.DashboardScreen
import com.houshmandhesab.app.ui.screens.DebtsScreen
import com.houshmandhesab.app.ui.screens.GoalsScreen
import com.houshmandhesab.app.ui.screens.ReportsScreen
import com.houshmandhesab.app.ui.screens.SettingsScreen
import com.houshmandhesab.app.ui.screens.TransactionsScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val REPORTS = "reports"
    const val AI = "ai"
    const val SETTINGS = "settings"
    const val ACCOUNTS = "accounts"
    const val CATEGORIES = "categories"
    const val BUDGETS = "budgets"
    const val GOALS = "goals"
    const val DEBTS = "debts"
    const val ADD = "add?type={type}&id={id}"

    fun add(type: String = "EXPENSE", id: Long = -1L): String = "add?type=$type&id=$id"
}

private data class BottomDest(val route: String, val labelRes: Int, val icon: ImageVector)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val bottomDests = listOf(
        BottomDest(Routes.DASHBOARD, R.string.nav_home, Icons.Rounded.Home),
        BottomDest(Routes.TRANSACTIONS, R.string.nav_transactions, Icons.Rounded.ReceiptLong),
        BottomDest(Routes.REPORTS, R.string.nav_reports, Icons.Rounded.PieChart),
        BottomDest(Routes.AI, R.string.nav_ai, Icons.Rounded.AutoAwesome)
    )
    val showBottomBar = bottomDests.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDests.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(stringResource(dest.labelRes)) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showBottomBar) {
                FloatingActionButton(onClick = { navController.navigate(Routes.add()) }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_transaction))
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(onNavigate = { navController.navigate(it) })
            }
            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(onNavigate = { navController.navigate(it) })
            }
            composable(Routes.REPORTS) { ReportsScreen() }
            composable(Routes.AI) {
                AiScreen(onNavigate = { navController.navigate(it) })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ACCOUNTS) {
                AccountsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.BUDGETS) {
                BudgetsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.GOALS) {
                GoalsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.DEBTS) {
                DebtsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.ADD,
                arguments = listOf(
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = "EXPENSE"
                    },
                    navArgument("id") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) {
                AddTransactionScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
