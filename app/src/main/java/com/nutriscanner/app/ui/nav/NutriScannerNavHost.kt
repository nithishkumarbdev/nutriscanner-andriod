package com.nutriscanner.app.ui.nav

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nutriscanner.app.ui.ScanFlowViewModel
import com.nutriscanner.app.ui.history.HistoryScreen
import com.nutriscanner.app.ui.result.ResultScreen
import com.nutriscanner.app.ui.scan.ScanScreen

private sealed class Destination(val route: String, val label: String) {
    data object Scan : Destination("scan", "Scan")
    data object Result : Destination("result", "Result")
    data object History : Destination("history", "History")
}

@Composable
fun NutriScannerNavHost(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
) {
    val navController = rememberNavController()
    // Shared across the scan -> result hop so extracted facts survive the
    // navigation transition without round-tripping through nav arguments.
    val scanFlowViewModel: ScanFlowViewModel = viewModel()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                listOf(Destination.Scan, Destination.History).forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {},
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Scan.route,
            modifier = androidx.compose.ui.Modifier.padding(padding),
        ) {
            composable(Destination.Scan.route) {
                ScanScreen(
                    viewModel = scanFlowViewModel,
                    hasCameraPermission = hasCameraPermission,
                    onRequestCameraPermission = onRequestCameraPermission,
                    onFactsReady = { navController.navigate(Destination.Result.route) },
                )
            }
            composable(Destination.Result.route) {
                ResultScreen(
                    viewModel = scanFlowViewModel,
                    onSaved = { navController.navigate(Destination.History.route) },
                )
            }
            composable(Destination.History.route) {
                HistoryScreen(onScanTapped = { /* detail dialog is a follow-up; row already shows the band */ })
            }
        }
    }
}
