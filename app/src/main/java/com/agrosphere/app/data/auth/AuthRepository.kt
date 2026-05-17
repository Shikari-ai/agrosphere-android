package com.agrosphere.app.data.auth

import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper around FirebaseAuth and the Credential Manager API.
 *
 * Exposes a [userFlow] of the currently signed-in [FirebaseUser] (or null)
 * and suspend functions for each sign-in / sign-up path. All errors bubble
 * up as exceptions for the ViewModel to translate into UI state.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {

    val currentUser: FirebaseUser? get() = auth.currentUser

    /** Cold flow that emits whenever the auth state changes (login, logout, token refresh). */
    val userFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithEmail(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        return result.user ?: error("Sign-in succeeded but returned no user.")
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String?): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: error("Sign-up succeeded but returned no user.")
        if (!displayName.isNullOrBlank()) {
            val updates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName.trim())
                .build()
            user.updateProfile(updates).await()
        }
        return user
    }

    suspend fun signInAnonymously(): FirebaseUser {
        val result = auth.signInAnonymously().await()
        return result.user ?: error("Anonymous sign-in returned no user.")
    }

    /**
     * Launches the Credential Manager bottom-sheet to pick a Google account,
     * then exchanges the resulting Google ID token for a Firebase credential.
     *
     * @param activityContext must be an Activity context — Credential Manager
     *  needs it to show the system bottom sheet.
     * @param webClientId the OAuth 2.0 Web Client ID from Firebase Console
     *  (Project Settings → General → Your apps → Web client / "Web SDK config").
     */
    suspend fun signInWithGoogle(
        activityContext: android.content.Context,
        webClientId: String,
    ): FirebaseUser {
        val credentialManager = CredentialManager.create(activityContext)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val response = try {
            credentialManager.getCredential(activityContext, request)
        } catch (e: GetCredentialException) {
            throw IllegalStateException(
                "Google sign-in was cancelled or unavailable: ${e.message}", e,
            )
        }
        val credential = response.credential
        val tokenCred = try {
            GoogleIdTokenCredential.createFrom(credential.data)
        } catch (e: GoogleIdTokenParsingException) {
            throw IllegalStateException("Could not parse Google ID token.", e)
        }
        val firebaseCred = GoogleAuthProvider.getCredential(tokenCred.idToken, null)
        val result = auth.signInWithCredential(firebaseCred).await()
        return result.user ?: error("Google sign-in returned no user.")
    }

    fun signOut() {
        auth.signOut()
    }
}
