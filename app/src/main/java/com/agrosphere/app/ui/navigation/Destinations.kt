package com.agrosphere.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Dest(val route: String) {
    data object Auth : Dest("auth")
    data object Home : Dest("home")
    data object Fields : Dest("fields")
    data object FieldDetail : Dest("field/{id}") {
        fun build(id: String) = "field/$id"
    }
    data object Scanner : Dest("scanner")
    data object Weather : Dest("weather")
    data object Assistant : Dest("assistant")
    data object Profile : Dest("profile")
}

data class TabItem(
    val dest: Dest,
    val label: String,
    val icon: ImageVector,
)

val BottomTabs = listOf(
    TabItem(Dest.Home, "Home", Icons.Rounded.Home),
    TabItem(Dest.Fields, "Fields", Icons.Rounded.Grass),
    TabItem(Dest.Scanner, "Scan", Icons.Rounded.CameraAlt),
    TabItem(Dest.Weather, "Weather", Icons.Rounded.Cloud),
    TabItem(Dest.Assistant, "Ask", Icons.Rounded.AutoAwesome),
)
