package com.agrosphere.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.agrosphere.app.R

sealed class Dest(val route: String) {
    data object Auth : Dest("auth")
    data object Home : Dest("home")
    data object Fields : Dest("fields")
    data object FieldDetail : Dest("field/{id}") {
        fun build(id: String) = "field/$id"
    }
    data object Scanner : Dest("scanner")
    data object ScanHistory : Dest("scan-history")
    data object PestPrediction : Dest("pest-prediction")
    data object Weather : Dest("weather")
    data object Assistant : Dest("assistant")
    data object Copilot : Dest("copilot")
    data object Map : Dest("map")
    data object MapPicker : Dest("fields/add-from-map")
    data object Regional : Dest("regional")
    data object Developer : Dest("developer")
    data object Profile : Dest("profile")
    data object ProfileDetail : Dest("profile/{section}") {
        fun build(section: String) = "profile/$section"
    }
    data object Plants : Dest("plants")
    data object AddPlant : Dest("plants/add")
    data object PlantDetail : Dest("plant/{id}") {
        fun build(id: String) = "plant/$id"
    }
    data object RescanPlant : Dest("plant/{id}/rescan") {
        fun build(id: String) = "plant/$id/rescan"
    }
    data object PlantAnalytics : Dest("plant-analytics")
    data object FieldAnalytics : Dest("field-analytics")
}

/** Section ids consumed by [ProfileDetail] — kept as constants so the menu + screen can't drift. */
object ProfileSections {
    const val FARM_OVERVIEW = "farm-overview"
    const val ACCOUNT = "account"
    const val EDIT_PROFILE = "edit"
    const val MY_FARMS = "my-farms"
    const val SUBSCRIPTION = "subscription"
    const val AI_PREFS = "ai-prefs"
    const val AI_RELIABILITY = "ai-reliability"
    const val LEARNING = "learning"
    const val NOTIFICATIONS = "notifications"
    const val MODE = "mode"
    const val LANGUAGE = "language"
    const val HELP = "help"
    const val ABOUT = "about"
}

data class TabItem(
    val dest: Dest,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

/** Build the bottom tab list based on the user's chosen mode.
 *  Weather isn't a tab — it's reached via the Home weather card so the nav
 *  bar stays focused on actions (Home, what-I-grow, Scan, Ask). */
fun bottomTabsForMode(mode: String): List<TabItem> = buildList {
    add(TabItem(Dest.Home,      R.string.nav_home,    Icons.Rounded.Home))
    if (mode == "farmer" || mode == "both") {
        add(TabItem(Dest.Fields, R.string.nav_fields, Icons.Rounded.Grass))
    }
    if (mode == "plant" || mode == "both") {
        add(TabItem(Dest.Plants, R.string.nav_plants, Icons.Rounded.LocalFlorist))
    }
    add(TabItem(Dest.Scanner,   R.string.nav_scan,    Icons.Rounded.CameraAlt))
    add(TabItem(Dest.Assistant, R.string.nav_ask,     Icons.Rounded.AutoAwesome))
}

/** Fallback static list (farmer default) — used only where compose state isn't available. */
val BottomTabs = bottomTabsForMode("farmer")
