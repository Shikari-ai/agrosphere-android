# Map picker — Esri WorldImagery via osmdroid (no API key)

The "Draw field on map" picker uses the same satellite imagery layer that **Mission Planner** (and most drone-survey ground stations) defaults to: **Esri WorldImagery** raster tiles, served via `osmdroid`. No API key, no Google Cloud setup.

If you saw an older version of this doc that asked for a Google Maps API key — that step is gone. The Maps API key setting in `strings.xml` has been removed too.

## Why this provider

- **Free** for non-commercial use — no key, no quota dashboards.
- **Same imagery** ArduPilot's Mission Planner uses as its default satellite layer.
- **Updated frequently** by Esri's WorldImagery program.
- **Compose-friendly** via `osmdroid-android` wrapped in `AndroidView`.

## Positioning (NavIC, GPS, etc.)

Nothing to configure. `FusedLocationProviderClient` automatically uses whichever GNSS constellations the device supports — **GPS, GLONASS, Galileo, BeiDou, and NavIC** (the latter on supported chipsets running Android 11+, which covers most India-market phones from 2020 onwards). The map picker calls `LocationProvider.fastCurrent()` which is backed by the same fused provider, so NavIC kicks in transparently where available.

## What you'll see

1. Fields tab → tap **Draw on map** chip (or the *"or draw it on the map →"* link if you have no fields yet).
2. Esri satellite imagery centred on your device's location.
3. Tap to drop polygon corners. Numbered markers appear at each tap.
4. After three corners, the area appears live at the bottom — computed by the spherical-excess formula (same approach mission-planning software uses, accurate to <0.5% at field scale).
5. Right rail controls: **layer switch** (Satellite ↔ Street/OSM Mapnik), **My location**, **Undo**, **Clear**.
6. Fill in name, pick a crop and stage, hit **Save**. The new field appears immediately on Home, Map, Fields, and Profile — all reactive.

## Internet + storage

osmdroid downloads tiles on demand and caches them under the app's private storage (no `WRITE_EXTERNAL_STORAGE` permission needed on Android 10+). The manifest already includes `INTERNET` and `ACCESS_NETWORK_STATE`. Tiles are reused offline once cached.

## Switching providers

`MapPickerScreen.kt` declares the tile source in one place:

```kotlin
private val EsriWorldImagery: OnlineTileSourceBase = object : OnlineTileSourceBase(
    "Esri WorldImagery", 0, 19, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
    "Powered by Esri",
) { ... }
```

To swap in another provider — Bing satellite, Mapbox, your own tile server — replace the URL pattern and update `MapLayer.Satellite.tileSource()`. Built-in `TileSourceFactory` constants (MAPNIK, USGS_TOPO, etc.) work out of the box for additional layers.
