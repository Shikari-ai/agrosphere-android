# Firebase setup — connecting AgroSphere Android to the agritech-4d1ba project

The Android app now wires real authentication through Firebase (email + password, Google Sign-In via Credential Manager, anonymous "guest" sessions). Because the `google-services` Gradle plugin needs your project's credentials, **the build will fail until you complete the steps below**. Allow ~10 minutes the first time.

## 1. Register the Android app in Firebase Console

1. Open the Firebase Console for the existing project — https://console.firebase.google.com/project/agritech-4d1ba/overview
2. Click the **Add app** button (gear icon → Project settings if you don't see it) → choose **Android**.
3. **Register two apps** (one per build flavor) so both debug and release builds work:

   | Build variant | Android package name |
   | --- | --- |
   | Debug | `com.agrosphere.app.debug` |
   | Release | `com.agrosphere.app` |

   Nickname can be anything (e.g. `AgroSphere Android — debug`).
4. **Add the debug SHA-1 fingerprint** for Google Sign-In. From a terminal on your machine:

   ```powershell
   keytool -keystore "$env:USERPROFILE\.android\debug.keystore" -list -v `
     -alias androiddebugkey -storepass android -keypass android
   ```

   Copy the line that starts with `SHA1:` and paste it into Firebase Console → your Android app → **Add fingerprint**. Do this for both the debug and release apps (release uses your release keystore SHA-1, which you can add later when you publish).

## 2. Download `google-services.json`

After the Android app is registered, Firebase offers a `google-services.json` download. Save it to:

```
android/app/google-services.json
```

This file is in `.gitignore` and **will not be committed** — every developer needs their own copy.

> Tip: Firebase ships a single `google-services.json` that contains entries for every registered Android app under the project. As long as you registered both `com.agrosphere.app` and `com.agrosphere.app.debug` before downloading, one file works for both flavors.

## 3. Enable the sign-in providers

In Firebase Console → **Build → Authentication → Sign-in method**, enable:

- **Email/Password** — toggle on. Leave passwordless / link sign-in off.
- **Google** — toggle on. Pick a "Project support email" (usually your own). Save.
- **Anonymous** — toggle on (powers the "Continue as guest" button).

## 4. Paste the Web Client ID into the app

Google Sign-In via Credential Manager needs the project's **OAuth 2.0 Web Client ID** — not the Android client ID. Even though we sign in on Android, the token is exchanged through the web client.

1. Firebase Console → **Project settings** → **General** tab.
2. Scroll to **Your apps** → expand the **Web SDK config** card (if there isn't a web app yet, you may need to register a placeholder "Web app"). The card shows a snippet like:
   ```js
   { apiKey: "...", appId: "1:...:web:...", ... }
   ```
3. Below that snippet, find **OAuth 2.0 Client IDs**. Copy the value ending in `.apps.googleusercontent.com` that is labelled **Web client (auto created by Google Service)**.

   Alternative source: Google Cloud Console → APIs & Services → Credentials → look for the same "Web client (auto created by Google Service)" entry.
4. Paste it into `app/src/main/res/values/strings.xml`, replacing the placeholder:

   ```xml
   <string name="default_web_client_id" translatable="false">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>
   ```

## 5. Build & run

Back in Android Studio:

1. **File → Sync Project with Gradle Files** (or wait for auto-sync after the JSON drop).
2. Hit **▶ Run**.
3. On the login screen:
   - **Sign up** — create a real account (≥6 char password) → user appears in Firebase Console → Authentication → Users.
   - **Sign in** — log back in with the same credentials.
   - **Google** — opens the system account picker, signs in with your Google account.
   - **Continue as guest** — anonymous Firebase user; can later be linked to a real account.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| Build error: `File google-services.json is missing` | You skipped step 2. Drop the file into `app/`. |
| Sign-in works, Google sign-in errors with `getCredential cancelled` | SHA-1 fingerprint (step 1.4) doesn't match the keystore the IDE built with, or the Web Client ID (step 4) is wrong. |
| `12500: Sign in failed` on the Google button | Usually means SHA-1 isn't registered. Re-run the keytool command and add the fingerprint in Firebase. Wait a minute for it to propagate. |
| Email sign-up returns `ERROR_OPERATION_NOT_ALLOWED` | Email/Password provider isn't enabled in step 3. |
| "Network problem reaching Firebase" snackbar | Emulator has no internet. In Studio, Device Manager → ▼ → Cold Boot Now usually fixes it. |

## What's now in the code

- `data/auth/AuthRepository.kt` — wraps `FirebaseAuth`, exposes coroutine APIs + a Flow of the current user.
- `feature/auth/AuthViewModel.kt` — state machine (`Idle / Loading / Error / Success`) + friendly error mapping.
- `feature/auth/AuthScreen.kt` — observes VM state, shows a centered spinner while in flight and a snackbar on error.
- `ui/AgroSphereApp.kt` — observes `authRepo.userFlow` so the bottom-bar and nav state react to login/logout instantly.
- `res/values/strings.xml` — holds `default_web_client_id`.
