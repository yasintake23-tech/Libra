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
        context.getString(R.string.cloudflare_r2_public_base_url).trim().trimEnd('/')

    fun isConfigured(context: Context): Boolean =
        accountId(context).isNotBlank() &&
            bucketName(context).isNotBlank() &&
            accessKeyId(context).isNotBlank() &&
            secretAccessKey(context).isNotBlank()

    fun objectKeyFromValue(value: String): String? {
        val raw = value.trim()
        if (raw.isBlank()) return null
        return when {
            "/media/" in raw -> raw.substringAfter("/media/").trim('/').takeIf { it.isNotBlank() }
            "/objects/" in raw -> raw.substringAfter("/objects/").trim('/').takeIf { it.isNotBlank() }
            raw.startsWith("http://") || raw.startsWith("https://") -> null
            else -> raw.trim('/').takeIf { it.startsWith("users/") }
        }
    }
}
