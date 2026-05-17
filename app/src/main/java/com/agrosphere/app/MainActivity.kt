package com.agrosphere.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.agrosphere.app.ui.AgroSphereApp
import com.agrosphere.app.ui.theme.AgroSphereTheme

/**
 * Extends [AppCompatActivity] (not bare ComponentActivity) so that
 * AppCompatDelegate.setApplicationLocales() — used by LocaleManager — actually
 * recreates the UI in the chosen language. Without an AppCompat-rooted Activity
 * the locale write is silently dropped on Android < 13.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { false }
        setContent {
            AgroSphereTheme {
                AgroSphereApp()
            }
        }
    }
}
