package com.libra.app.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.libra.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object GoogleAuthHelper {

    suspend fun launchGoogleSignIn(
        context: Context,
        serverClientId: String = "",
        onSuccess: (idToken: String, displayName: String?, email: String?, photoUrl: String?) -> Unit,
        onError: (String) -> Unit
    ) {
        val resolvedServerClientId = resolveServerClientId(context, serverClientId)

        if (resolvedServerClientId.isBlank() ||
            resolvedServerClientId.contains("REPLACE", ignoreCase = true)
        ) {
            onError(
                "Google Sign-In yapılandırılmamış. Firebase'deki Web OAuth Client ID bulunamadı."
            )
            return
        }

        withContext(Dispatchers.Main) {
            try {
                val credentialManager = CredentialManager.create(context)

                // Explicit Google button flow. It does not require a prior authorization.
                val googleSignInOption =
                    GetSignInWithGoogleOption.Builder(resolvedServerClientId)
                        .setNonce(UUID.randomUUID().toString())
                        .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleSignInOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential

                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)

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
                onError(
                    when {
                        e.message?.contains(
                            "No credentials available",
                            ignoreCase = true
                        ) == true ->
                            "Google hesabı seçilemedi. Telefonda Google hesabının açık olduğundan ve Google Play Hizmetleri'nin güncel olduğundan emin ol."
                        else ->
                            "Google girişi: " +
                                (e.localizedMessage ?: "İşlem tamamlanamadı.")
                    }
                )
            } catch (e: Exception) {
                onError(
                    "Google girişi başarısız: " +
                        (e.localizedMessage ?: "Bilinmeyen hata.")
                )
            }
        }
    }

    private fun resolveServerClientId(
        context: Context,
        explicitId: String
    ): String {
        if (explicitId.isNotBlank() &&
            !explicitId.contains("REPLACE", ignoreCase = true)
        ) {
            return explicitId
        }

        val generatedId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )

        return if (generatedId != 0) {
            context.getString(generatedId)
        } else {
            context.getString(R.string.google_web_client_id)
        }
    }
}
