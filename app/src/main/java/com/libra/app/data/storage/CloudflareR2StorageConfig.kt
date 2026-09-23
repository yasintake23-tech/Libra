package com.libra.app.data.storage

import android.content.Context
import com.libra.app.R

object CloudflareR2StorageConfig {
    fun accountId(context: Context): String =
        context.getString(R.string.cloudflare_r2_account_id).trim()

    fun bucketName(context: Context): String =
        context.getString(R.string.cloudflare_r2_bucket_name).trim()

    fun accessKeyId(context: Context): String =
        context.getString(R.string.cloudflare_r2_access_key_id).trim()

    fun secretAccessKey(context: Context): String =
        context.getString(R.string.cloudflare_r2_secret_access_key).trim()

    fun endpoint(context: Context): String =
        "https://" + accountId(context) + ".r2.cloudflarestorage.com"

    fun publicBaseUrl(context: Context): String =
        context.getString(R.string.cloudflare_r2_public_base_url)
            .trim()
            .trimEnd('/')
            .ifBlank { DEFAULT_PUBLIC_BASE_URL }

    fun isConfigured(context: Context): Boolean =
        accountId(context).isNotBlank() &&
            bucketName(context).isNotBlank() &&
            accessKeyId(context).isNotBlank() &&
            secretAccessKey(context).isNotBlank() &&
            publicBaseUrl(context).isNotBlank()

    /**
     * Accept the canonical object key as well as URLs left by older storage paths.
     * A public R2 URL is converted back to its object key so delete operations keep
     * working if an old document stores the public URL instead of the key.
     */
    fun objectKeyFromValue(context: Context, value: String): String? {
        val raw = value.trim()
        if (raw.isBlank()) return null

        val publicBase = publicBaseUrl(context)
        if (raw.startsWith(publicBase + "/")) {
            val path = raw.removePrefix(publicBase + "/").trim('/')
            val withoutBucket = path.removePrefix(bucketName(context) + "/")
            return withoutBucket.takeIf { it.startsWith("users/") }
        }

        // Accept any Cloudflare public r2.dev URL saved by an older/manual build.
        if (raw.startsWith("https://") && raw.contains(".r2.dev/")) {
            val path = raw.substringAfter(".r2.dev/").trim('/')
            val withoutBucket = path.removePrefix(bucketName(context) + "/")
            return withoutBucket.takeIf { it.startsWith("users/") }
        }

        // Accept direct R2 S3 endpoint URLs saved by older/manual uploads:
        // https://<account>.r2.cloudflarestorage.com/libra-media/users/...
        val endpointPrefix = "https://" + accountId(context) + ".r2.cloudflarestorage.com/"
        if (raw.startsWith(endpointPrefix)) {
            val path = raw.removePrefix(endpointPrefix).trim('/')
            val withoutBucket = path.removePrefix(bucketName(context) + "/")
            return withoutBucket.takeIf { it.startsWith("users/") }
        }

        return objectKeyFromValue(raw)
    }

    private const val DEFAULT_PUBLIC_BASE_URL =
        "https://pub-6a68b6b4caa84c10ab277cd5ee1f9cc4.r2.dev"

    fun objectKeyFromValue(value: String): String? {
        val raw = value.trim()
        if (raw.isBlank()) return null
        return when {
            "/media/" in raw -> raw.substringAfter("/media/").trim('/').takeIf { it.startsWith("users/") }
            "/objects/" in raw -> raw.substringAfter("/objects/").trim('/').takeIf { it.startsWith("users/") }
            raw.startsWith("http://") || raw.startsWith("https://") -> null
            else -> raw.trim('/').takeIf { it.startsWith("users/") }
        }
    }
}
