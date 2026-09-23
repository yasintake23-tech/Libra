package com.libra.app.data.storage

import android.content.Context
import com.libra.app.R

object CloudflareR2StorageConfig {
    private const val CLOUDFLARE_API_ROOT = "https://api.cloudflare.com/client/v4"

    fun accountId(context: Context): String =
        context.getString(R.string.cloudflare_r2_account_id).trim()

    fun bucketName(context: Context): String =
        context.getString(R.string.cloudflare_r2_bucket_name).trim()

    /**
     * Compatibility shim for older callers.
     * Android must never contain a Cloudflare API credential.
     */
    @Deprecated("Cloudflare API tokens must stay on the Worker/server side.")
    fun apiToken(context: Context): String = ""

    fun uploadEndpoint(context: Context): String =
        context.getString(R.string.r2_upload_endpoint).trim().trimEnd('/')

    fun objectApiBaseUrl(context: Context): String {
        val account = accountId(context)
        val bucket = bucketName(context)
        return CLOUDFLARE_API_ROOT + "/accounts/" + account + "/r2/buckets/" + bucket + "/objects"
    }

    fun objectUrl(context: Context, key: String): String =
        objectApiBaseUrl(context) + "/" + encodeObjectKey(key)

    fun isObjectApiUrl(url: String): Boolean =
        url.contains("/client/v4/accounts/") &&
            url.contains("/r2/buckets/") &&
            url.contains("/objects/")

    fun encodeObjectKey(key: String): String =
        android.net.Uri.encode(key.trim('/'), "/")
}
