package com.agrosphere.app

import android.app.Application

class AgroSphereApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Load persisted plants (and their scan history) before any screen reads them.
        // init() also kicks off a background Firestore pull so new-device sign-ins
        // recover the user's full plant inventory from the cloud backend.
        com.agrosphere.app.data.repo.PlantRepository.init(this)
        // Warm the field cache from Firestore in the background — same recovery
        // story as plants, fields are persisted to the 'fields' collection.
        com.agrosphere.app.data.repo.FieldRepository.ensurePulled()
        // Seed last-known weather from disk so cold starts never render empty even
        // if Open-Meteo is throwing 5xx at this exact moment.
        com.agrosphere.app.data.weather.WeatherRepository.seedFromDisk(this)
    }
}
