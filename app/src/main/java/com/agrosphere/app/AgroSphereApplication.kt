package com.agrosphere.app

import android.app.Application

class AgroSphereApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Load persisted plants (and their scan history) before any screen reads them.
        com.agrosphere.app.data.repo.PlantRepository.init(this)
    }
}
