package com.example.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Result data from native Google Sign-in via Credential Manager.
 */
data class GoogleSignInResult(
    val idToken: String,
    val email: String,
    val displayName: String?,
    val profilePictureUri: String?,
    val givenName: String?,
    val familyName: String?
)

object CredentialManagerHelper {
    private const val TAG = "CredentialManagerHelper"

    /**
     * Retrieves a Google ID Token using Android's Credential Manager API.
     * Uses native account picker with no web-redirect, browser, or localhost dependency.
     */
    suspend fun getGoogleIdToken(
        context: Context,
        serverClientId: String? = null
    ): Result<GoogleSignInResult> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val clientId = (serverClientId?.takeIf { it.isNotBlank() } ?: try {
                context.getString(R.string.default_web_client_id)
            } catch (e: Exception) {
                ""
            }).ifBlank {
                com.example.data.supabase.SupabaseConfig.GOOGLE_WEB_CLIENT_ID
            }

            if (clientId.isBlank()) {
                return Result.failure(IllegalStateException("Server Web Client ID is not configured."))
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = response.credential
            when {
                credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        Result.success(
                            GoogleSignInResult(
                                idToken = googleIdTokenCredential.idToken,
                                email = googleIdTokenCredential.id,
                                displayName = googleIdTokenCredential.displayName,
                                profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString(),
                                givenName = googleIdTokenCredential.givenName,
                                familyName = googleIdTokenCredential.familyName
                            )
                        )
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Failed to parse Google ID token credential: ${e.message}", e)
                        Result.failure(e)
                    }
                }
                else -> {
                    Result.failure(IllegalStateException("Unexpected credential type: ${credential.javaClass.name}"))
                }
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User cancelled Google Sign-in")
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google Sign-in: ${e.message}", e)
            Result.failure(e)
        }
    }
}
