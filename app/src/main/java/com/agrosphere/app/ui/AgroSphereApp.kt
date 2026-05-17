package com.agrosphere.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.agrosphere.app.data.auth.AuthRepository
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agrosphere.app.feature.assistant.AssistantScreen
import com.agrosphere.app.feature.auth.AuthScreen
import com.agrosphere.app.feature.fields.FieldDetailScreen
import com.agrosphere.app.feature.fields.FieldsScreen
import com.agrosphere.app.feature.home.HomeScreen
import com.agrosphere.app.feature.profile.ProfileScreen
import com.agrosphere.app.feature.scanner.ScannerScreen
import com.agrosphere.app.feature.weather.WeatherScreen
import com.agrosphere.app.ui.navigation.BottomTabs
import com.agrosphere.app.ui.navigation.Dest
import com.agrosphere.app.ui.theme.AgroPalette

@Composable
fun AgroSphereApp() {
    val navController = rememberNavController()
    // Observe Firebase auth state — drives both start destination + bottom-bar visibility.
    val authRepo = remember { AuthRepository() }
    val currentUser by authRepo.userFlow.collectAsState(initial = authRepo.currentUser)
    val loggedIn = currentUser != null

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = loggedIn && currentRoute in BottomTabs.map { it.dest.route }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                AgroBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { dest ->
                        navController.navigate(dest.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (loggedIn) Dest.Home.route else Dest.Auth.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Dest.Auth.route) {
                AuthScreen(onAuthenticated = {
                    // Auth state is observed at the top; just navigate.
                    navController.navigate(Dest.Home.route) {
                        popUpTo(Dest.Auth.route) { inclusive = true }
                    }
                })
            }
            composable(Dest.Home.route) {
                HomeScreen(
                    padding = innerPadding,
                    onOpenProfile = { navController.navigate(Dest.Profile.route) },
                    onOpenField = { id -> navController.navigate(Dest.FieldDetail.build(id)) },
                    onOpenScanner = { navController.navigate(Dest.Scanner.route) },
                    onOpenAssistant = { navController.navigate(Dest.Assistant.route) },
                )
            }
            composable(Dest.Fields.route) {
                FieldsScreen(
                    padding = innerPadding,
                    onOpenField = { id -> navController.navigate(Dest.FieldDetail.build(id)) },
                )
            }
            composable(Dest.FieldDetail.route) { backStack ->
                val id = backStack.arguments?.getString("id") ?: ""
                FieldDetailScreen(fieldId = id, onBack = { navController.popBackStack() })
            }
            composable(Dest.Scanner.route) {
                ScannerScreen(padding = innerPadding)
            }
            composable(Dest.Weather.route) {
                WeatherScreen(padding = innerPadding)
            }
            composable(Dest.Assistant.route) {
                AssistantScreen(padding = innerPadding)
            }
            composable(Dest.Profile.route) {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSignOut = {
                        authRepo.signOut()
                        navController.navigate(Dest.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AgroBottomBar(
    currentRoute: String?,
    onTabSelected: (Dest) -> Unit,
) {
    NavigationBar(
        containerColor = AgroPalette.Surface.copy(alpha = 0.92f),
        contentColor = AgroPalette.Ink,
        tonalElevation = 0.dp,
    ) {
        BottomTabs.forEach { tab ->
            val selected = currentRoute == tab.dest.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab.dest) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AgroPalette.Primary,
                    selectedTextColor = AgroPalette.Primary,
                    unselectedIconColor = AgroPalette.InkMuted,
                    unselectedTextColor = AgroPalette.InkDim,
                    indicatorColor = AgroPalette.PrimaryDim,
                )
            )
        }
    }
}
