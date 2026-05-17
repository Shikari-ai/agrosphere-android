# Google Maps API key — "Draw on map" field picker

The new map picker (Fields tab → **Draw on map** chip or "or draw it on the map →" link) uses Google Maps SDK to show satellite tiles + let you tap polygon corners. It needs a Maps API key. ~3 min in the Google Cloud Console.

You can use the **same Google Cloud project** as your Firebase setup (`agritech-4d1ba`), so no new project required.

## Steps

1. Open the Google Cloud Console for your Firebase project:
   https://console.cloud.google.com/google/maps-apis/credentials?project=agritech-4d1ba

2. Enable the SDK:
   - Top search bar → "Maps SDK for Android" → click → **Enable**.

3. Create the API key:
   - Left nav → **APIs & Services → Credentials**.
   - **+ Create credentials → API key**. A new key appears — copy it.
   - Click the key name to edit. Under **Application restrictions** pick **Android apps**. Add two entries:

     | Package name | SHA-1 |
     | --- | --- |
     | `com.agrosphere.app.debug` | `33:AD:38:43:A9:BD:A9:89:BC:C5:98:D9:CD:AD:D1:1D:76:A5:22:5A` |
     | `com.agrosphere.app` | (same SHA-1 — add your release SHA-1 later when you ship) |

     (The SHA-1 above is your debug keystore — same one you registered with Firebase.)

   - Under **API restrictions** → **Restrict key** → check **Maps SDK for Android** → **Save**.

4. Paste the key into the app:
   - Open `app/src/main/res/values/strings.xml`
   - Replace `REPLACE_WITH_MAPS_API_KEY` in `maps_api_key` with the value you copied.

5. Back in Android Studio: **Sync** → **▶ Run**.

## What you'll see

- Fields tab → tap **Draw on map** chip (or the "or draw it on the map →" link if you have no fields yet).
- A satellite map opens centered on your device's location.
- Tap the map to drop corners. Each tap adds a numbered vertex marker.
- Once you've placed 3+ corners, an emerald polygon fills the shape and the area appears live at the bottom (computed via `SphericalUtil.computeArea`).
- **Undo** removes the last vertex; **🗑** clears everything.
- Fill in the name, pick a crop and stage, hit **Save → ha field**. The new field appears in the Fields list, on Home's "My fields" carousel, on the Field Map screen, and in Profile → My farms — all reactive.

## Without the key

The screen still loads — Google Maps renders a grey background with a **"For development purposes only"** watermark. Drawing and area calculation still work locally; you just don't see the satellite imagery. The moment you paste the real key and re-run, satellite tiles light up.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| Grey map + "For development purposes only" | API key not set or restricted to a SHA-1 / package that doesn't match. Re-check Step 3. |
| Crash on opening map screen | Key is invalid or the SDK isn't enabled. Re-run Step 2. |
| `IllegalStateException: Google Maps requires Google Play Services` | Emulator image without Play Store — use a Play Store-enabled AVD. |
| Area looks tiny / huge | Make sure you tapped real corners; 3 collinear points = ~0 ha. Switch off the Map Picker and back on to reset. |
