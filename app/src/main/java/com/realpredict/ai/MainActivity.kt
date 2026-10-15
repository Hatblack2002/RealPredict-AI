package com.realpredict.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.realpredict.ai.ui.components.RealPredictBottomBar
import com.realpredict.ai.ui.screens.*
import com.realpredict.ai.ui.theme.DarkBackground
import com.realpredict.ai.ui.theme.RealPredictTheme
import com.realpredict.ai.ui.theme.RealPredictMotion

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RealPredictTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "home"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    bottomBar = {
                        RealPredictBottomBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = { RealPredictMotion.screenEnterTransition() },
                        exitTransition = { RealPredictMotion.screenExitTransition() },
                        popEnterTransition = { RealPredictMotion.screenEnterTransition() },
                        popExitTransition = { RealPredictMotion.screenExitTransition() }
                    ) {
                        composable("home") {
                            HomeScreen(
                                onNavigateToStats = { navController.navigate("stats") },
                                onNavigateToLab = { navController.navigate("lab") }
                            )
                        }
                        composable("stats") {
                            StatisticsScreen()
                        }
                        composable("prediction") {
                            PredictionScreen()
                        }
                        composable("lab") {
                            LaboratoryScreen()
                        }
                        composable("chat") {
                            ChatScreen()
                        }
                        composable("import") {
                            ImportScreen()
                        }
                        composable("history") {
                            HistoryScreen()
                        }
                        composable("apikeys") {
                            ApiKeysScreen()
                        }
                        composable("settings") {
                            SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}
