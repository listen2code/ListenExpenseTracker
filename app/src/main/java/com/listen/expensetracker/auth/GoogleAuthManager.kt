package com.listen.expensetracker.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Clean data model representing authenticated Google account user profile.
 */
data class GoogleUserProfile(
    val email: String,
    val displayName: String?,
    val avatarUrl: String?,
    val idToken: String?
)

/**
 * Modern AndroidX Credential Manager Authentication Engine.
 * Fully conforms to Google's latest Credential Manager and Google Identity specs with ZERO deprecated APIs.
 */
object GoogleAuthManager {

    // Configured Google Cloud OAuth 2.0 Web Client ID
    private const val DEFAULT_WEB_CLIENT_ID = "1069102462195-dbs5gu3p6nf64jqou8f29g7vhb8e1s6m.apps.googleusercontent.com"

    /**
     * Obtains the official AndroidX CredentialManager instance.
     *
     * @param context Context reference
     * @return CredentialManager instance
     */
    fun getCredentialManager(context: Context): CredentialManager {
        return CredentialManager.create(context)
    }

    /**
     * Builds modern GetGoogleIdOption for Google Identity sign-in.
     *
     * @param serverClientId Optional OAuth 2.0 Web Client ID
     * @return Configured GetGoogleIdOption
     */
    fun buildGoogleIdOption(serverClientId: String = ""): GetGoogleIdOption {
        val clientId = serverClientId.ifBlank { DEFAULT_WEB_CLIENT_ID }
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .build()
    }

    /**
     * Builds the unified GetCredentialRequest incorporating Google Identity options.
     *
     * @param serverClientId Optional OAuth 2.0 Web Client ID
     * @return Configured GetCredentialRequest
     */
    fun buildGetCredentialRequest(serverClientId: String = ""): GetCredentialRequest {
        return GetCredentialRequest.Builder()
            .addCredentialOption(buildGoogleIdOption(serverClientId))
            .build()
    }

    /**
     * Parses the modern GoogleIdTokenCredential returned from AndroidX CredentialManager.
     *
     * @param response GetCredentialResponse from CredentialManager.getCredential()
     * @return Result containing parsed GoogleUserProfile or detailed failure
     */
    fun parseGoogleIdCredential(response: GetCredentialResponse): Result<GoogleUserProfile> {
        return try {
            val credential = response.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val profile = GoogleUserProfile(
                email = googleIdTokenCredential.id,
                displayName = googleIdTokenCredential.displayName,
                avatarUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                idToken = googleIdTokenCredential.idToken
            )
            Result.success(profile)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Clears all credential state and signs out the user.
     *
     * @param context Context reference
     */
    suspend fun clearCredentials(context: Context) {
        try {
            getCredentialManager(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Throwable) {}
    }
}
