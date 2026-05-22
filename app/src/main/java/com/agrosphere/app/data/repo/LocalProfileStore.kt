package com.agrosphere.app.data.repo

import android.content.Context

/** On-device personal info that isn't part of the Firebase auth profile (phone, location). */
object LocalProfileStore {

    private const val PREFS = "agro_profile"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getPhone(context: Context): String = prefs(context).getString("phone", "").orEmpty()
    fun getLocation(context: Context): String = prefs(context).getString("location", "").orEmpty()

    fun save(context: Context, phone: String, location: String) {
        prefs(context).edit()
            .putString("phone", phone.trim())
            .putString("location", location.trim())
            .apply()
    }
}
