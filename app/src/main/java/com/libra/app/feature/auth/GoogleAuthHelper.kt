package com.libra.app.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GoogleAuthHelper {

    suspend fun launchGoogleSignIn(
        context: Context,
        serverClientId: String,
        onSuccess: (idToken: String, displayName: String?, email: String?, photoUrl: String?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (serverClientId.isBlank() || serverClientId.contains("REPLACE", ignoreCase = true)) {
            onError("Google Sign-In yapılandırılmamış. OAuth Web Client ID ekleyin.")
            return
        }

        withContext(Dispatchers.Main) {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(request = request, context = context)
                val credential = result.credential

                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    onSuccess(
                        googleCredential.idToken,
                        googleCredential.displayName,
                        googleCredential.id,
                        googleCredential.profilePictureUri?.toString()
                    )
                } else {
                    onError("Google hesabı doğrulanamadı.")
                }
            } catch (e: GetCredentialException) {
                onError("Google Girişi: ${e.localizedMessage ?: "İşlem iptal edildi."}")
            } catch (e: Exception) {
                onError("Giriş hatası: ${e.localizedMessage ?: "Bilinmeyen hata."}")
            }
        }
    }
}
