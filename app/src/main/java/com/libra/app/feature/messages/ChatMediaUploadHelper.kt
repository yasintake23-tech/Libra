package com.libra.app.feature.messages

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.libra.app.core.di.ServiceLocator
import com.libra.app.domain.model.DirectMessage
import com.libra.app.domain.model.SharedContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_CHAT_IMAGE_DIMENSION_PX = 1600
private const val MAX_CHAT_UPLOAD_BYTES = 8 * 1024 * 1024
private const val GROUP_MAX_GAP_MS = 3 * 60 * 1000L
private const val DIVIDER_GAP_MS = 30 * 60 * 1000L

data class ChatMessageGroupPosition(
    val showAvatar: Boolean,
    val showSenderName: Boolean,
    val showTimestampDivider: Boolean,
    val isFirstInGroup: Boolean,
    val isLastInGroup: Boolean
)

data class PreparedChatImage(
    val bytes: ByteArray,
    val contentType: String,
    val fileName: String,
    val width: Int,
    val height: Int
)

fun computeDirectMessageGrouping(
    messages: List<DirectMessage>,
    index: Int,
    currentUid: String = "",
    isGroup: Boolean = false
): ChatMessageGroupPosition {
    if (index !in messages.indices) {
        return ChatMessageGroupPosition(
            showAvatar = true,
            showSenderName = false,
            showTimestampDivider = false,
            isFirstInGroup = true,
            isLastInGroup = true
        )
    }

    val current = messages[index]
    val prev = messages.getOrNull(index - 1)
    val next = messages.getOrNull(index + 1)

    val showTimestampDivider = prev == null ||
        (current.createdAt > 0L && prev.createdAt > 0L &&
            (abs(current.createdAt - prev.createdAt) >= DIVIDER_GAP_MS ||
                !isSameCalendarDay(prev.createdAt, current.createdAt)))

    val groupedWithPrev = prev != null &&
        !showTimestampDivider &&
        prev.senderId == current.senderId &&
        current.createdAt > 0L &&
        prev.createdAt > 0L &&
        abs(current.createdAt - prev.createdAt) <= GROUP_MAX_GAP_MS

    val nextDivider = next == null ||
        (next.createdAt > 0L && current.createdAt > 0L &&
            (abs(next.createdAt - current.createdAt) >= DIVIDER_GAP_MS ||
                !isSameCalendarDay(current.createdAt, next.createdAt)))

    val groupedWithNext = next != null &&
        !nextDivider &&
        next.senderId == current.senderId &&
        next.createdAt > 0L &&
        current.createdAt > 0L &&
        abs(next.createdAt - current.createdAt) <= GROUP_MAX_GAP_MS

    val mine = currentUid.isNotBlank() && current.senderId == currentUid
    val isFirstInGroup = !groupedWithPrev
    val isLastInGroup = !groupedWithNext

    return ChatMessageGroupPosition(
        showAvatar = !mine && isLastInGroup,
        showSenderName = isGroup && !mine && isFirstInGroup,
        showTimestampDivider = showTimestampDivider,
        isFirstInGroup = isFirstInGroup,
        isLastInGroup = isLastInGroup
    )
}

private fun isSameCalendarDay(firstMs: Long, secondMs: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = firstMs }
    val c2 = Calendar.getInstance().apply { timeInMillis = secondMs }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
        c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

fun formatChatTimeHeader(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (timestampMs <= 0L) return ""
    val locale = Locale("tr", "TR")
    val timeStr = SimpleDateFormat("HH:mm", locale).format(Date(timestampMs))
    val yesterdayMs = nowMs - (24L * 60L * 60L * 1000L)
    return when {
        isSameCalendarDay(timestampMs, nowMs) -> "Bugün $timeStr"
        isSameCalendarDay(timestampMs, yesterdayMs) -> "Dün $timeStr"
        else -> SimpleDateFormat("d MMM HH:mm", locale).format(Date(timestampMs))
    }
}

fun formatMessageHour(timestampMs: Long): String {
    if (timestampMs <= 0L) return ""
    return SimpleDateFormat("HH:mm", Locale("tr", "TR")).format(Date(timestampMs))
}

fun formatConversationRelativeTime(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (timestampMs <= 0L) return ""
    val diff = (nowMs - timestampMs).coerceAtLeast(0L)
    val locale = Locale("tr", "TR")
    val yesterdayMs = nowMs - (24L * 60L * 60L * 1000L)
    return when {
        diff < 60_000L -> "Şimdi"
        diff < 60 * 60_000L -> "${(diff / 60_000L).coerceAtLeast(1)}d"
        isSameCalendarDay(timestampMs, nowMs) -> SimpleDateFormat("HH:mm", locale).format(Date(timestampMs))
        isSameCalendarDay(timestampMs, yesterdayMs) -> "Dün"
        diff < 7 * 24L * 60 * 60_000L -> SimpleDateFormat("EEE", locale).format(Date(timestampMs))
        else -> SimpleDateFormat("d MMM", locale).format(Date(timestampMs))
    }
}

fun formatReadableByteSize(bytes: Int): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "${kb.roundToInt()} KB"
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB", mb)
}

fun buildForwardSharedContent(
    message: DirectMessage,
    fallbackAuthorName: String = ""
): SharedContent {
    message.sharedContent?.let { existing ->
        return existing
    }
    val resolvedAuthor = message.senderName
        .ifBlank { fallbackAuthorName }
        .ifBlank { "Libra Üyesi" }
    val snippet = message.text.trim().ifBlank {
        if (message.mediaUrl.isNotBlank()) "Paylaşılan fotoğraf" else "Paylaşılan mesaj"
    }
    val type = if (message.mediaUrl.isNotBlank()) "dm_photo" else "message"
    return SharedContent(
        type = type,
        id = message.id.ifBlank { "msg_${message.createdAt}" },
        title = "$resolvedAuthor tarafından iletildi",
        text = snippet.take(240),
        authorId = message.senderId,
        authorName = resolvedAuthor,
        mediaUrl = message.mediaUrl,
        url = ""
    )
}

fun resolveShareableMessageText(message: DirectMessage): String {
    val parts = mutableListOf<String>()
    val cleanText = message.text.trim()
    if (cleanText.isNotBlank()) parts += cleanText
    message.sharedContent?.let { shared ->
        if (shared.title.isNotBlank() && shared.title != cleanText) parts += shared.title
        if (shared.text.isNotBlank() && shared.text != cleanText) parts += shared.text
        if (shared.url.isNotBlank()) parts += shared.url
    }
    if (parts.isEmpty() && message.mediaUrl.isNotBlank()) {
        parts += "Libra sohbetinden fotoğraf"
    }
    return parts.joinToString("\n").trim()
}

fun isMessageSeenByPeer(
    message: DirectMessage,
    peerLastReadAt: Long,
    currentUid: String
): Boolean =
    !message.id.startsWith("local-") &&
        currentUid.isNotBlank() &&
        message.senderId == currentUid &&
        message.createdAt > 0L &&
        peerLastReadAt >= message.createdAt

suspend fun prepareChatImageForUpload(
    context: Context,
    uri: Uri,
    prefix: String = "dm"
): PreparedChatImage = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val rawMime = resolver.getType(uri).orEmpty().ifBlank { "image/jpeg" }
    val timestamp = System.currentTimeMillis()

    if (rawMime.equals("image/gif", ignoreCase = true)) {
        val gifBytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("GIF okunamadı.")
        if (gifBytes.isEmpty()) throw IllegalStateException("Seçilen dosya boş.")
        if (gifBytes.size > MAX_CHAT_UPLOAD_BYTES) {
            throw IllegalStateException("GIF 8 MB'dan küçük olmalı.")
        }
        return@withContext PreparedChatImage(
            bytes = gifBytes,
            contentType = "image/gif",
            fileName = "$prefix-$timestamp.gif",
            width = 0,
            height = 0
        )
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        val rawBytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Fotoğraf okunamadı.")
        if (rawBytes.isEmpty()) throw IllegalStateException("Fotoğraf boş.")
        if (rawBytes.size > MAX_CHAT_UPLOAD_BYTES) {
            throw IllegalStateException("Fotoğraf 8 MB'dan küçük olmalı.")
        }
        val ext = rawMime.substringAfter('/').ifBlank { "jpg" }.take(8)
        return@withContext PreparedChatImage(
            bytes = rawBytes,
            contentType = rawMime,
            fileName = "$prefix-$timestamp.$ext",
            width = 0,
            height = 0
        )
    }

    var inSampleSize = 1
    while (
        (bounds.outWidth / inSampleSize) > MAX_CHAT_IMAGE_DIMENSION_PX * 2 ||
        (bounds.outHeight / inSampleSize) > MAX_CHAT_IMAGE_DIMENSION_PX * 2
    ) {
        inSampleSize *= 2
    }

    val decodeOpts = BitmapFactory.Options().apply {
        this.inSampleSize = inSampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    var decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, decodeOpts)
    } ?: throw IllegalStateException("Fotoğraf çözümlenemedi.")

    val orientation = runCatching {
        resolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    val rotated = applyExifOrientation(decoded, orientation)
    if (rotated !== decoded) {
        decoded.recycle()
        decoded = rotated
    }

    val maxSide = max(decoded.width, decoded.height)
    val scaled = if (maxSide > MAX_CHAT_IMAGE_DIMENSION_PX) {
        val ratio = MAX_CHAT_IMAGE_DIMENSION_PX.toFloat() / maxSide.toFloat()
        val targetW = (decoded.width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (decoded.height * ratio).roundToInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
            if (it !== decoded) decoded.recycle()
        }
    } else {
        decoded
    }

    val finalWidth = scaled.width
    val finalHeight = scaled.height
    var quality = 86
    var compressed: ByteArray
    do {
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        compressed = out.toByteArray()
        quality -= 8
    } while (compressed.size > MAX_CHAT_UPLOAD_BYTES && quality >= 50)

    scaled.recycle()

    if (compressed.isEmpty()) throw IllegalStateException("Fotoğraf hazırlanamadı.")
    if (compressed.size > MAX_CHAT_UPLOAD_BYTES) {
        throw IllegalStateException("Fotoğraf boyutu 8 MB sınırını aşıyor.")
    }

    PreparedChatImage(
        bytes = compressed,
        contentType = "image/jpeg",
        fileName = "$prefix-$timestamp.jpg",
        width = finalWidth,
        height = finalHeight
    )
}

private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        else -> return bitmap
    }
    return runCatching {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }.getOrDefault(bitmap)
}

suspend fun shareDirectMessageExternally(
    context: Context,
    message: DirectMessage
) {
    val caption = resolveShareableMessageText(message)
    val rawMediaKey = message.mediaUrl.ifBlank { message.sharedContent?.mediaUrl.orEmpty() }

    if (rawMediaKey.isNotBlank()) {
        val cachedFileUri: Uri? = withContext(Dispatchers.IO) {
            runCatching {
                val signedUrl = ServiceLocator.storageRepository.getSignedMediaUrl(rawMediaKey)
                if (signedUrl.isBlank()) return@runCatching null
                val dir = File(context.cacheDir, "dm_media").apply { mkdirs() }
                // Clean old shared cache files (>1 day)
                val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000L
                dir.listFiles()?.forEach { old -> if (old.lastModified() < cutoff) old.delete() }
                val outFile = File(dir, "libra-dm-${System.currentTimeMillis()}.jpg")
                val conn = (URL(signedUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 10000
                }
                conn.inputStream.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
                if (outFile.length() > 0L) {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
                } else null
            }.getOrNull()
        }

        if (cachedFileUri != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, cachedFileUri)
                if (message.text.isNotBlank()) putExtra(Intent.EXTRA_TEXT, message.text.trim())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Fotoğrafı Paylaş"))
            return
        }
    }

    val textIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, caption.ifBlank { "Libra" })
    }
    context.startActivity(Intent.createChooser(textIntent, "Mesajı Paylaş"))
}
