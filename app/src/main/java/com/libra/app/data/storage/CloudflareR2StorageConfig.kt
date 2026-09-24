package com.libra.app.data.storage

import android.content.Context
import android.net.Uri
import com.libra.app.R

/**
 * Single source of truth for the R2 bucket used by Libra media.
 *
 * The endpoint is intentionally stored explicitly because R2 jurisdictional
 * endpoints are different from the account-only endpoint.
 */
object CloudflareR2StorageConfig {

    fun accountId(context: Context): String =
        context.getString(R.string.cloudflare_r2_account_id).trim()

    fun bucketName(context: Context): String =
        context.getString(R.string.cloudflare_r2_bucket_name).trim()

    fun endpoint(context: Context): String =
        context.getString(R.string.cloudflare_r2_endpoint)
            .trim()
            .trimEnd('/')

    fun accessKeyId(context: Context): String =
        context.getString(R.string.cloudflare_r2_access_key_id).trim()

    fun secretAccessKey(context: Context): String =
        context.getString(R.string.cloudflare_r2_secret_access_key).trim()

    fun publicBaseUrl(context: Context): String =
        context.getString(R.string.cloudflare_r2_public_base_url)
            .trim()
            .trimEnd('/')
            .ifBlank { DEFAULT_PUBLIC_BASE_URL }

    fun isConfigured(context: Context): Boolean =
        accountId(context).isNotBlank() &&
            bucketName(context).isNotBlank() &&
            endpoint(context).startsWith("https://") &&
            accessKeyId(context).isNotBlank() &&
            secretAccessKey(context).isNotBlank() &&
            publicBaseUrl(context).startsWith("https://")

    /**
     * Converts current and legacy storage values into the canonical key:
     * users/<uid>/...
     */
    fun objectKeyFromValue(context: Context, value: String): String? {
        val raw = value.trim()
        if (raw.isBlank()) return null

        val publicBase = publicBaseUrl(context)
        if (raw.startsWith(publicBase + "/")) {
            return canonicalKey(context, raw.removePrefix(publicBase + "/"))
        }

        if (raw.startsWith("https://") && raw.contains(".r2.dev/")) {
            return canonicalKey(context, raw.substringAfter(".r2.dev/"))
        }

        val exactEndpointPrefix = endpoint(context).trimEnd('/') + "/"
        if (raw.startsWith(exactEndpointPrefix)) {
            return canonicalKey(context, raw.removePrefix(exactEndpointPrefix))
        }

        // Backward compatibility with older non-jurisdictional S3 URLs.
        val accountEndpointPrefix =
            "https://" + accountId(context) + ".r2.cloudflarestorage.com/"
        if (raw.startsWith(accountEndpointPrefix)) {
            return canonicalKey(context, raw.removePrefix(accountEndpointPrefix))
        }

        return objectKeyFromValue(raw)
    }

    fun objectKeyFromValue(value: String): String? {
        val raw = value.trim()
        if (raw.isBlank()) return null

        return when {
            raw.startsWith("http://") || raw.startsWith("https://") -> null
            "/media/" in raw -> canonicalRawKey(raw.substringAfter("/media/"))
            "/objects/" in raw -> canonicalRawKey(raw.substringAfter("/objects/"))
            else -> canonicalRawKey(raw)
        }
    }

    private fun canonicalKey(context: Context, value: String): String? {
        val decoded = Uri.decode(value).trim('/')
        val bucket = context.getString(R.string.cloudflare_r2_bucket_name).trim()
        val withoutBucket = decoded
            .removePrefix(bucket + "/")
            .removePrefix("/" + bucket + "/")

        return canonicalRawKey(withoutBucket)
    }

    private fun canonicalRawKey(value: String): String? {
        val decoded = Uri.decode(value).trim('/')
        return decoded.takeIf {
            it.startsWith("users/") && it.length > "users/".length
        }
    }

    private const val DEFAULT_PUBLIC_BASE_URL =
        "https://pub-6a68b6b4caa84c10ab277cd5ee1f9cc4.r2.dev"
}
