# AgroSphere — Android

A brand-new native Android app inspired by the Agro Sphere web project (a "Farm OS" for smallholders). Built with **Kotlin + Jetpack Compose + Material 3**, single-module, no backend wired yet — runs on a fresh emulator out of the box against mock data so you can see the whole experience before any cloud setup.

```
android/
├── app/
│   └── src/main/java/com/agrosphere/app/
│       ├── MainActivity.kt               # splash + Compose entry
│       ├── ui/
│       │   ├── AgroSphereApp.kt          # NavHost + bottom bar shell
│       │   ├── theme/                    # color, type, gradients, Material3 theme
│       │   ├── navigation/               # destinations + bottom-tab list
│       │   └── components/               # GlassCard, PrimaryButton, StatChip, headers
│       ├── feature/                      # one package per screen
│       │   ├── auth/        AuthScreen.kt
│       │   ├── home/        HomeScreen.kt
│       │   ├── fields/      FieldsScreen.kt + FieldDetailScreen.kt
│       │   ├── scanner/     ScannerScreen.kt
│       │   ├── weather/     WeatherScreen.kt
│       │   ├── assistant/   AssistantScreen.kt
│       │   └── profile/     ProfileScreen.kt
│       └── data/                         # models + mock repository
├── gradle/libs.versions.toml             # version catalog
└── build.gradle.kts (+ app/build.gradle.kts)
```

## Prerequisites (one-time)

1. **JDK 17** — `winget install --id Microsoft.OpenJDK.17 -e`
2. **Android Studio** (Hedgehog or newer) — `winget install --id Google.AndroidStudio -e`
3. Open Android Studio → Standard setup → let it download Platform 34 + Build-Tools.

## Open & run

1. Launch Android Studio → **Open** → pick the `android/` folder (NOT the repo root).
2. Studio detects the Gradle project and starts syncing — accept any "install missing components" prompts.
3. Studio auto-generates the `gradlew` wrapper script + jar on first sync. (If it doesn't, run `gradle wrapper` once.)
4. Hit **Run ▶** with the default emulator (or a connected Pixel). The app installs as **AgroSphere** with a leaf-mark launcher.

You'll land on the auth screen → "Continue without signing in" → bottom-tabbed shell with Home, Fields, Scan, Weather, Ask.

## What's there

- **Auth** — email/password fields + guest entry (no Firebase yet).
- **Home** — animated farm pulse hero, quick actions, alerts feed, horizontally scrolling field cards.
- **Fields list + detail** — health bars, soil moisture, growth stage, recent activity.
- **Scanner** — viewfinder with corner brackets, capture flow, mock leaf-rust + nutrient findings with confidence + advice.
- **Weather** — current conditions card, 7-day outlook with condition-aware icons, agronomy alerts.
- **Assistant (AgroAI)** — bubble chat UI, IME-aware composer, mock replies keyed by keyword.
- **Profile** — avatar card, settings rows, themed sign-out.

## Next steps (in suggested order)

1. **Wire CameraX preview** into `ScannerScreen` (deps already added — `androidx.camera:camera-view` etc.). Use Accompanist permissions for the camera prompt.
2. **Firebase**: create a project, add Android app with package `com.agrosphere.app`, drop `google-services.json` into `app/`, add the `com.google.gms.google-services` plugin to `app/build.gradle.kts` and the classpath to root. Then swap `MockRepository` for Firestore-backed repos.
3. **Hilt or Koin** for DI once repositories multiply.
4. **Real ML**: load a TFLite plant-disease model in the scanner (try PlantVillage). Use `org.tensorflow:tensorflow-lite-task-vision`.
5. **Play Store**: bump `versionCode`, generate a signed AAB via Build → Generate Signed Bundle, create the Play Console listing.

## Design notes

- Theme follows the web app's emerald/dark DNA: bg `#070C09`, primary `#10B981`, accents `#38BDF8` / `#A78BFA` / `#F59E0B`.
- All screens use `windowInsetsPadding(...)` so they look right edge-to-edge on devices with notches/gesture nav.
- Single-module on purpose — modularize when feature folders pass ~15 files each.
