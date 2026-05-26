package com.agrosphere.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import android.content.Context
import com.agrosphere.app.data.auth.AuthRepository
import com.agrosphere.app.data.repo.AppPreferences
import com.agrosphere.app.data.i18n.LocaleManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agrosphere.app.feature.assistant.AssistantScreen
import com.agrosphere.app.feature.auth.AuthScreen
import com.agrosphere.app.feature.copilot.CopilotScreen
import com.agrosphere.app.feature.developer.DeveloperScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agrosphere.app.feature.fields.FieldDetailScreen
import com.agrosphere.app.feature.fields.FieldsScreen
import com.agrosphere.app.feature.plants.PlantDetailScreen
import com.agrosphere.app.feature.plants.PlantsScreen
import com.agrosphere.app.feature.fields.FieldsViewModel
import com.agrosphere.app.feature.fields.MapPickerScreen
import com.agrosphere.app.feature.home.HomeScreen
import com.agrosphere.app.feature.onboarding.OnboardingScreen
import com.agrosphere.app.feature.map.MapScreen
import com.agrosphere.app.feature.pest.PestPredictionScreen
import com.agrosphere.app.feature.profile.ProfileDetailScreen
import com.agrosphere.app.feature.profile.ProfileScreen
import com.agrosphere.app.feature.regional.RegionalScreen
import com.agrosphere.app.feature.scanner.ScanHistoryScreen
import com.agrosphere.app.feature.scanner.ScannerScreen
import com.agrosphere.app.feature.weather.WeatherScreen
import com.agrosphere.app.ui.components.AgroIntroScreen
import com.agrosphere.app.ui.components.AgroSplashScreen
import com.agrosphere.app.ui.navigation.BottomTabs
import com.agrosphere.app.ui.navigation.bottomTabsForMode
import com.agrosphere.app.ui.navigation.Dest
import com.agrosphere.app.ui.theme.AgroPalette
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat

@Composable
fun AgroSphereApp() {
    val navController = rememberNavController()
    // Observe Firebase auth state — drives both start destination + bottom-bar visibility.
    val authRepo = remember { AuthRepository() }
    val currentUser by authRepo.userFlow.collectAsState(initial = authRepo.currentUser)
    val loggedIn = currentUser != null

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // Show the bottom nav on every logged-in screen (not just the 5 tab routes)
    // so the user can jump to any section from a detail/sub-page too.
    val showBottomBar = loggedIn && currentRoute != null &&
        currentRoute != Dest.Auth.route &&
        currentRoute != Dest.MapPicker.route &&
        currentRoute != Dest.AddPlant.route &&
        currentRoute != Dest.RescanPlant.route &&
        currentRoute != Dest.PlantDetail.route.substringBefore("{") // hide for plant detail sub-page

    // Branded launch experience: splash every launch, intro + onboarding after every login.
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("agro_prefs", Context.MODE_PRIVATE) }
    // If the activity was recreated mid-onboarding (e.g. locale change), resume from saved step.
    val savedOnbStep = prefs.getInt("onboarding_resume_step", -1)
    var showSplash by remember { mutableStateOf(true) }
    var showIntro by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(loggedIn && savedOnbStep >= 0) }
    var onboardingStartStep by remember { mutableIntStateOf(savedOnbStep.coerceAtLeast(0)) }

    // Synchronous check: was the user already logged in with no pending onboarding at launch?
    // Firebase's currentUser is available instantly (no network call), so this is safe to read
    // once at first composition and never changes — gives NavHost its correct start destination
    // from frame 1 so the Auth screen never flashes for returning users.
    val initiallyLoggedIn = remember { authRepo.currentUser != null && savedOnbStep < 0 }

    // Seed user-mode preference from SharedPreferences (set during onboarding).
    val savedMode = remember { prefs.getString("user_mode", "both") ?: "both" }
    remember { AppPreferences.setUserMode(savedMode) }
    val userMode by AppPreferences.userMode.collectAsState()

    // Ask for POST_NOTIFICATIONS once — after splash/intro/onboarding are all gone.
    val notifPermLauncher = rememberLauncherForActivityResult(RequestPermission()) { /* result surfaced in profile */ }
    val fullyLoggedIn = loggedIn && !showSplash && !showIntro && !showOnboarding
    LaunchedEffect(fullyLoggedIn) {
        if (fullyLoggedIn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyAsked = prefs.getBoolean("notif_permission_asked", false)
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!alreadyAsked && !granted) {
                prefs.edit().putBoolean("notif_permission_asked", true).apply()
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    tabs = bottomTabsForMode(userMode),
                    onTabSelected = { dest ->
                        navController.navigate(dest.route) {
                            // Clear any detail screens stacked on top so a tab tap always
                            // lands on that tab's root (e.g. Home → clean Home screen).
                            // No saveState/restoreState: detail screens live in the same
                            // graph, so restoring would just re-open the page we popped.
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (initiallyLoggedIn) Dest.Home.route else Dest.Auth.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it / 5 },
                    animationSpec  = tween(300, easing = FastOutSlowInEasing),
                ) + fadeIn(tween(300)) + scaleIn(initialScale = 0.94f, animationSpec = tween(300))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { -it / 8 },
                    animationSpec = tween(240),
                ) + fadeOut(tween(240)) + scaleOut(targetScale = 0.94f, animationSpec = tween(240))
            },
            popEnterTransition = {
                slideInVertically(
                    initialOffsetY = { -it / 8 },
                    animationSpec  = tween(300, easing = FastOutSlowInEasing),
                ) + fadeIn(tween(300)) + scaleIn(initialScale = 0.94f, animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it / 5 },
                    animationSpec = tween(240),
                ) + fadeOut(tween(240)) + scaleOut(targetScale = 0.96f, animationSpec = tween(240))
            },
        ) {
            composable(Dest.Auth.route) {
                AuthScreen(onAuthenticated = {
                    // Reset to step 0 for this login — clears any stale step from a prior session.
                    prefs.edit().putInt("onboarding_resume_step", 0).apply()
                    onboardingStartStep = 0
                    showIntro = true
                    // Do NOT navigate — overlays cover the auth screen until onboarding finishes.
                })
            }
            composable(Dest.Home.route) {
                HomeScreen(
                    padding = innerPadding,
                    onOpenProfile = { navController.navigate(Dest.Profile.route) },
                    onOpenField = { id -> navController.navigate(Dest.FieldDetail.build(id)) },
                    onOpenScanner = { navController.navigate(Dest.Scanner.route) },
                    onOpenPestPrediction = { navController.navigate(Dest.PestPrediction.route) },
                    onOpenAssistant = { navController.navigate(Dest.Assistant.route) },
                    onOpenWeather = { navController.navigate(Dest.Weather.route) },
                    onOpenFields = { navController.navigate(Dest.Fields.route) },
                )
            }
            composable(Dest.Fields.route) {
                FieldsScreen(
                    padding = innerPadding,
                    onOpenField = { id -> navController.navigate(Dest.FieldDetail.build(id)) },
                    onOpenMap = { navController.navigate(Dest.Map.route) },
                    onOpenMapPicker = { navController.navigate(Dest.MapPicker.route) },
                )
            }
            composable(Dest.MapPicker.route) {
                Box(Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    MapPickerScreen(onBack = { navController.popBackStack() })
                }
            }
            composable(Dest.FieldDetail.route) { backStack ->
                val id = backStack.arguments?.getString("id") ?: ""
                val fieldsVm: FieldsViewModel = viewModel(factory = FieldsViewModel.Factory)
                Box(Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    FieldDetailScreen(
                        fieldId = id,
                        onBack = { navController.popBackStack() },
                        onDelete = {
                            fieldsVm.deleteField(id)
                            navController.popBackStack()
                        },
                    )
                }
            }
            composable(Dest.Plants.route) {
                PlantsScreen(
                    padding     = innerPadding,
                    onOpenPlant = { id -> navController.navigate(Dest.PlantDetail.build(id)) },
                    onAddPlant  = { navController.navigate(Dest.AddPlant.route) },
                )
            }
            composable(Dest.AddPlant.route) {
                Box(Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    com.agrosphere.app.feature.plants.AddPlantFlow(
                        onBack  = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
            }
            composable(Dest.PlantDetail.route) { backStack ->
                val id = backStack.arguments?.getString("id") ?: ""
                Box(Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    PlantDetailScreen(
                        plantId  = id,
                        onBack   = { navController.popBackStack() },
                        onRescan = { navController.navigate(Dest.RescanPlant.build(id)) },
                        onDelete = {
                            navController.popBackStack()
                        },
                    )
                }
            }
            composable(Dest.RescanPlant.route) { backStack ->
                val id = backStack.arguments?.getString("id") ?: ""
                Box(Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    com.agrosphere.app.feature.plants.RescanPlantScreen(
                        plantId    = id,
                        onBack     = { navController.popBackStack() },
                        onFinished = { navController.popBackStack() },
                    )
                }
            }
            composable(Dest.Scanner.route) {
                ScannerScreen(
                    padding = innerPadding,
                    onOpenHistory = { navController.navigate(Dest.ScanHistory.route) },
                )
            }
            composable(Dest.ScanHistory.route) {
                ScanHistoryScreen(
                    padding = innerPadding,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Dest.PestPrediction.route) {
                PestPredictionScreen(
                    padding = innerPadding,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Dest.Weather.route) {
                WeatherScreen(padding = innerPadding)
            }
            composable(Dest.Assistant.route) {
                AssistantScreen(padding = innerPadding)
            }
            composable(Dest.Copilot.route) {
                Box(Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    CopilotScreen(onBack = { navController.popBackStack() })
                }
            }
            composable(Dest.Map.route) {
                Box(Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    MapScreen(
                        onBack = { navController.popBackStack() },
                        onOpenField = { id -> navController.navigate(Dest.FieldDetail.build(id)) },
                    )
                }
            }
            composable(Dest.Regional.route) {
                Box(Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    RegionalScreen(onBack = { navController.popBackStack() })
                }
            }
            composable(Dest.Developer.route) {
                Box(Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    DeveloperScreen(onBack = { navController.popBackStack() })
                }
            }
            composable(Dest.Profile.route) {
                Box(Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    ProfileScreen(
                        onBack = { navController.popBackStack() },
                        onSignOut = {
                            authRepo.signOut()
                            navController.navigate(Dest.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onOpenSection = { section ->
                            navController.navigate(Dest.ProfileDetail.build(section))
                        },
                        onOpenRegional = { navController.navigate(Dest.Regional.route) },
                    )
                }
            }
            composable(Dest.ProfileDetail.route) { entry ->
                val section = entry.arguments?.getString("section") ?: ""
                Box(Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    ProfileDetailScreen(
                        section = section,
                        onBack = { navController.popBackStack() },
                        onOpenField = { id -> navController.navigate(Dest.FieldDetail.build(id)) },
                        onSignOut = {
                            authRepo.signOut()
                            navController.navigate(Dest.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onOpenSection = { s -> navController.navigate(Dest.ProfileDetail.build(s)) },
                    )
                }
            }
        }
    }

        // Intro screen — shown after every login.
        if (showIntro && !showSplash) {
            AgroIntroScreen(onGetStarted = {
                showIntro = false
                showOnboarding = true
            })
        }
        // 4-step onboarding wizard — shown after Get Started.
        if (showOnboarding && !showSplash && !showIntro) {
            OnboardingScreen(
                userName = currentUser?.displayName ?: "",
                startStep = onboardingStartStep,
                onApplyLanguage = { tag ->
                    // Persist resume point BEFORE recreating — so we land on step 1 after restart.
                    prefs.edit().putInt("onboarding_resume_step", 1).apply()
                    onboardingStartStep = 1
                    LocaleManager.setLocale(tag)
                    // Force recreation so every stringResource() picks up the new locale.
                    var c: android.content.Context = context
                    while (c is android.content.ContextWrapper) {
                        if (c is android.app.Activity) { c.recreate(); break }
                        c = c.baseContext
                    }
                },
                onStepChange = { s ->
                    prefs.edit().putInt("onboarding_resume_step", s).apply()
                },
                onModeSelected = { mode ->
                    prefs.edit().putString("user_mode", mode).apply()
                    AppPreferences.setUserMode(mode)
                },
                onFinished = { pendingAction ->
                    prefs.edit().remove("onboarding_resume_step").apply()
                    showOnboarding = false
                    // Navigate to Home, clearing the auth screen from the back stack.
                    navController.navigate(Dest.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                    when (pendingAction) {
                        "fields"  -> navController.navigate(Dest.Fields.route)
                        "plants"  -> navController.navigate(Dest.Plants.route)
                        "scanner" -> navController.navigate(Dest.Scanner.route)
                    }
                },
            )
        }
        // Branded splash sits on top during launch.
        if (showSplash) {
            AgroSplashScreen(onDone = { showSplash = false })
        }
    }
}

@Composable
private fun AgroBottomBar(
    currentRoute: String?,
    tabs: List<com.agrosphere.app.ui.navigation.TabItem> = BottomTabs,
    onTabSelected: (Dest) -> Unit,
) {
    // Continuous glow pulse for the active tab indicator
    val inf = rememberInfiniteTransition(label = "nav-glow")
    val glow by inf.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(1800), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "g",
    )

    NavigationBar(
        containerColor = AgroPalette.Surface.copy(alpha = 0.95f),
        contentColor   = AgroPalette.Ink,
        tonalElevation = 0.dp,
        modifier       = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Neon top hairline — glows brighter at the active-tab x position
                val mid = size.width / 2f
                val len = size.width * 0.92f
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            AgroPalette.Primary.copy(alpha = 0.35f),
                            AgroPalette.Primary.copy(alpha = 0.65f * glow),
                            AgroPalette.Primary.copy(alpha = 0.35f),
                            androidx.compose.ui.graphics.Color.Transparent,
                        ),
                        startX = mid - len / 2f, endX = mid + len / 2f,
                    ),
                    start       = Offset(mid - len / 2f, 0f),
                    end         = Offset(mid + len / 2f, 0f),
                    strokeWidth = 1.2f,
                )
            },
    ) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.dest.route
            NavigationBarItem(
                selected = selected,
                onClick  = { onTabSelected(tab.dest) },
                icon = {
                    Box(
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                        modifier = androidx.compose.ui.Modifier.size(36.dp),
                    ) {
                        // Glow halo behind selected icon
                        if (selected) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        0f to AgroPalette.Primary.copy(alpha = 0.35f * glow),
                                        1f to androidx.compose.ui.graphics.Color.Transparent,
                                        center = Offset(cx, cy), radius = size.minDimension,
                                    ),
                                    radius = size.minDimension,
                                    center = Offset(cx, cy),
                                )
                            }
                        }
                        Icon(
                            tab.icon,
                            contentDescription = androidx.compose.ui.res.stringResource(tab.labelRes),
                        )
                    }
                },
                label = {
                    Text(
                        androidx.compose.ui.res.stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = AgroPalette.Primary,
                    selectedTextColor   = AgroPalette.Primary,
                    unselectedIconColor = AgroPalette.InkMuted,
                    unselectedTextColor = AgroPalette.InkDim,
                    indicatorColor      = AgroPalette.PrimaryDim,
                ),
            )
        }
    }
}
