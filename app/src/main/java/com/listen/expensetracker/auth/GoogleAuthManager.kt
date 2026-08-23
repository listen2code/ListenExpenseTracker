package com.listen.expensetracker.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential

/**
 * Modern Google Identity Services Authentication Manager.
 * Replaces deprecated legacy GoogleSignIn and GoogleSignInClient with official Google Identity API.
 */
object GoogleAuthManager {

    /**
     * Obtains the modern Google Identity SignInClient instance.
     *
     * @param context Context reference
     * @return Google Identity SignInClient
     */
    fun getSignInClient(context: Context): SignInClient {
        return Identity.getSignInClient(context)
    }

    /**
     * Parses the modern SignInCredential result returned from Google Identity Sign-In Intent.
     *
     * @param context Context reference
     * @param data Intent data returned from Activity result launcher
     * @return Result containing parsed SignInCredential or detailed failure
     */
    fun parseSignInCredential(context: Context, data: Intent?): Result<SignInCredential> {
        return try {
            if (data == null) {
                return Result.failure(IllegalArgumentException("Sign-in result intent data is null"))
            }
            val credential = getSignInClient(context).getSignInCredentialFromIntent(data)
            Result.success(credential)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs out the user from Google Identity Services.
     *
     * @param context Context reference
     * @param onComplete Callback invoked when sign-out completes
     */
    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        try {
            getSignInClient(context).signOut().addOnCompleteListener { onComplete() }
        } catch (_: Exception) {
            onComplete()
        }
    }
}
