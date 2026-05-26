# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build commands

```bash
# Cached Gradle binary (no gradlew.bat on Windows — use the cached installation)
GRADLE="C:/Users/super/.gradle/wrapper/dists/gradle-8.10.2-bin/a04bxjujx95o3nb99gddekhwo/gradle-8.10.2/bin/gradle"

$GRADLE -p . :app:compileDebugKotlin   # fast type-check (no APK)
$GRADLE -p . :app:assembleDebug        # full debug APK
$GRADLE -p . :app:assembleRelease      # minified release APK
$GRADLE -p . :app:testDebugUnitTest    # unit tests
```

Versions: Gradle 8.10.2 · AGP 8.7.2 · Kotlin 2.1.20 · Compose BOM 2025.04.00 · compileSdk 35 · minSdk 26.

## Architecture

**Single-module** Kotlin + Jetpack Compose + Material 3 app. No DI framework — repositories are `object` singletons.

```
MainActivity  →  AgroSphereApp (NavHost + bottom bar)
                      │
              feature/<screen>/          # one package per destination
                  ScreenName.kt          # all composables for that screen
                  ScreenViewModel.kt     # AndroidViewModel, StateFlow<UiState>
              data/
                  auth/AuthRepository    # Firebase Auth, exposes userFlow
                  repo/FieldRepository   # in-memory StateFlow<List<Field>> (swap for Firestore later)
                  repo/VisionScanRepository  # camera bitmap → Gemini multimodal via Val.town proxy
                  repo/GeminiRepository  # text chat → Val.town proxy (no SDK)
                  weather/WeatherRepository  # Open-Meteo REST → WeatherBundle
              ui/
                  theme/Color.kt         # AgroPalette + AgroBrushes
                  components/            # GlassCard, PrimaryButton, AgroSphereEmblem, Splash, Intro
                  navigation/Destinations.kt  # Dest sealed class + BottomTabs list
```

## Key patterns

**ViewModel ↔ UI**: Every screen collects a single `UiState` data class from a `StateFlow`. ViewModels use the `AndroidViewModel(app)` pattern with a `Factory` companion object (no Hilt).

**Navigation**: `AgroSphereApp.kt` holds the single `NavHost`. All routes are `Dest` objects in `Destinations.kt`. Bottom tabs are driven by `BottomTabs` list; other screens (Profile, Scanner, FieldDetail, etc.) push on top of the tab stack.

**GlassCard**: The primary card primitive. Takes `modifier`, `background` (default `SurfaceGlass`), `border`, `radius`, `padding`, and an optional `onClick`. Always draws a neon top hairline internally — pass `padding = 0.dp` and use a `Box` with `matchParentSize` Canvas if you need animated overlays inside.

**Canvas animations**: All visual effects are procedural Canvas (no image assets). Use `rememberInfiniteTransition` + `animateFloat`. Heavy Canvas (particles, rings) goes inside a `Canvas(Modifier.fillMaxSize())` or `Canvas(Modifier.matchParentSize())` inside a `Box`.

**Colour palette** (`AgroPalette`): bg `#070C09`, primary `#10B981` (emerald), sky `#38BDF8`, iris `#A78BFA`, amber `#F59E0B`, rose `#EF4444`. All surfaces use `SurfaceGlass = 0x14FFFFFF` over the dark background.

## Backend / data sources

| Source | What it provides |
|--------|-----------------|
| Firebase Auth | Sign-in (email, Google, anonymous); `AuthRepository.userFlow` |
| Firebase Firestore | Scan history (`ScanHistoryRepository`), regional pest contributions |
| Open-Meteo (free REST) | Real-time weather + 7-day forecast (`WeatherApi`, `WeatherRepository`) |
| Val.town proxy → Gemini 2.5 Flash | AI chat (`GeminiRepository`) + vision diagnosis (`VisionScanRepository`) |
| osmdroid + Esri WorldImagery tiles | Satellite map in `MapPickerScreen` / `MapScreen` |
| On-device DataStore | Locale preference (`LocaleManager`) |
| In-memory `FieldRepository` | User's fields — seeded empty, no persistence yet |

`FieldRepository` and `WeatherRepository` are global `object`s; any screen can read them without going through a ViewModel.

## Localisation

`LocaleManager` drives per-app locale (Hindi, Marathi, Tamil, Telugu, Gujarati, Bengali, Bhojpuri, English). All string resources live in `res/values*/strings.xml`. AI responses are prompted in the active language via `LocaleManager.activeLanguageTag()`.

## Splash / onboarding flow

`MainActivity` shows `AgroSplashScreen` (3-second cinematic Canvas animation) → `AgroIntroScreen` (feature cards, physics-drop) → `AuthScreen` → main shell. The flow is controlled by local `var phase` state in `AgroSphereApp`; once the user signs in or taps "guest", `phase` advances to `Phase.Main` and the NavHost appears.
