package com.agrosphere.app.data.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Single source of truth for the app's active locale.
 *
 * Wraps the per-app language API (AppCompatDelegate.setApplicationLocales).
 * On Android 13+ this is backed by the platform LocaleManager; on older
 * versions AppCompat persists the choice via the AppLocalesMetadataHolderService
 * declared in the manifest.
 *
 * Picking a language anywhere in the app triggers an Activity recreation,
 * after which every stringResource() call picks up the new translation
 * automatically.
 */
object LocaleManager {

    /**
     * The languages we ship translations for. Order is intentional — same
     * priority as the AgroSphere web app's i18n.js.
     */
    val supported: List<SupportedLocale> = listOf(
        SupportedLocale("en", "English", "English"),
        SupportedLocale("hi", "हिन्दी", "Hindi"),
        SupportedLocale("mr", "मराठी", "Marathi"),
        SupportedLocale("ta", "தமிழ்", "Tamil"),
        SupportedLocale("te", "తెలుగు", "Telugu"),
        SupportedLocale("bn", "বাংলা", "Bengali"),
        SupportedLocale("gu", "ગુજરાતી", "Gujarati"),
        SupportedLocale("hne", "छत्तीसगढ़ी", "Chhattisgarhi"),
    )

    /** The currently active locale tag (e.g. "en", "hi"). "" means system default. */
    fun currentTag(): String =
        AppCompatDelegate.getApplicationLocales().toLanguageTags()

    /** The display name (in its own script) for the current selection. */
    fun currentDisplayLabel(): String {
        val tag = currentTag().substringBefore('-').ifEmpty { Locale.getDefault().language }
        return supported.firstOrNull { it.tag == tag }?.nativeName ?: "System default"
    }

    /**
     * Clean BCP-47 base tag for the currently active locale, e.g. "hi", "mr", "en".
     * Falls back to the device's default language when no per-app locale is set.
     */
    fun activeLanguageTag(): String =
        currentTag().substringBefore('-').ifEmpty { java.util.Locale.getDefault().language }

    /** Apply a new locale (or clear back to system default with `null` / ""). */
    fun setLocale(tag: String?) {
        val list = if (tag.isNullOrBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(list)
    }
}

data class SupportedLocale(
    val tag: String,
    val nativeName: String,
    val englishName: String,
)
