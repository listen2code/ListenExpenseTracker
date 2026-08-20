package com.listen.listenexpensetracker.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

object GoogleAuthManager {

    fun getClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return try {
            GoogleSignIn.getLastSignedInAccount(context)
        } catch (_: Exception) {
            null
        }
    }

    fun parseSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                Result.success(account)
            } else {
                Result.failure(Exception("Account is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        try {
            getClient(context).signOut().addOnCompleteListener { onComplete() }
        } catch (_: Exception) {
            onComplete()
        }
    }
}
