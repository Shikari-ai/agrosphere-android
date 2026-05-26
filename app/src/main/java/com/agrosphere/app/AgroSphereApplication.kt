package com.agrosphere.app

import android.app.Application

class AgroSphereApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Load persisted plants (and their scan history) before any screen reads them.
        com.agrosphere.app.data.repo.PlantRepository.init(this)
        // Seed last-known weather from disk so cold starts never render empty even
        // if Open-Meteo is throwing 5xx at this exact moment.
        com.agrosphere.app.data.weather.WeatherRepository.seedFromDisk(this)
    }
}
