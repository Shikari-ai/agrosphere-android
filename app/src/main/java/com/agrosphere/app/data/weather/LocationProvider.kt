package com.agrosphere.app.data.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class Place(val latitude: Double, val longitude: Double, val label: String)

/** Default fallback if permission is denied or no location is available yet. */
private val DefaultPlace = Place(
    latitude = 19.9975,
    longitude = 73.7898,
    label = "Nashik, Maharashtra",
)

object LocationProvider {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns the device's current location, or [DefaultPlace] if permission isn't granted
     * or the lookup fails. Never throws — always returns *some* Place.
     */
    @SuppressLint("MissingPermission")
    suspend fun current(context: Context): Place {
        if (!hasLocationPermission(context)) return DefaultPlace
        val client = LocationServices.getFusedLocationProviderClient(context)
        return try {
            val location = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                ?: client.lastLocation.await()
                ?: return DefaultPlace
            val label = reverseGeocode(context, location.latitude, location.longitude)
                ?: "%.3f, %.3f".format(location.latitude, location.longitude)
            Place(location.latitude, location.longitude, label)
        } catch (_: Throwable) {
            DefaultPlace
        }
    }

    private suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        cont.resume(formatAddress(addresses.firstOrNull()))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocation(lat, lon, 1)
                formatAddress(list?.firstOrNull())
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun formatAddress(a: android.location.Address?): String? {
        if (a == null) return null
        val parts = listOfNotNull(
            a.locality ?: a.subLocality ?: a.subAdminArea,
            a.adminArea,
        ).filter { it.isNotBlank() }
        return parts.joinToString(", ").ifBlank { a.countryName }
    }
}
