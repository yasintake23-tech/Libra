package com.libra.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.google.firebase.auth.FirebaseAuth
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.LibraNotificationTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles WhatsApp-style inline notification actions:
 * - ACTION_INLINE_REPLY: sends direct or group reply without opening the app, marks notification & chat read (Görüldü).
 * - ACTION_MARK_READ: marks notification read in Firestore, triggers DM/group Görüldü receipt in RTDB, cancels tray item.
 */
class LibraNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val safeIntent = intent ?: return
        val action = safeIntent.action.orEmpty()
        if (action != ACTION_INLINE_REPLY && action != ACTION_MARK_READ) return

        val appContext = context.applicationContext
        val notificationId = safeIntent.getStringExtra(LibraNotificationHelper.EXTRA_NOTIFICATION_ID).orEmpty()
        val type = safeIntent.getStringExtra(LibraNotificationHelper.EXTRA_NOTIFICATION_TYPE).orEmpty()
        val referenceId = safeIntent.getStringExtra(LibraNotificationHelper.EXTRA_NOTIFICATION_REF_ID).orEmpty()
        val actorId = safeIntent.getStringExtra(LibraNotificationHelper.EXTRA_NOTIFICATION_ACTOR_ID).orEmpty()
        val title = safeIntent.getStringExtra(LibraNotificationHelper.EXTRA_NOTIFICATION_TITLE).orEmpty()
        val incomingBody = safeIntent.getStringExtra(LibraNotificationHelper.EXTRA_NOTIFICATION_BODY).orEmpty()
        val actorName = safeIntent.getStringExtra(LibraNotificationHelper.EXTRA_NOTIFICATION_ACTOR_NAME).orEmpty()
        val requestCode = safeIntent.getIntExtra(LibraNotificationHelper.EXTRA_NOTIFICATION_REQUEST_CODE, 0)

        val replyText = if (action == ACTION_INLINE_REPLY) {
            RemoteInput.getResultsFromIntent(safeIntent)
                ?.getCharSequence(LibraNotificationHelper.KEY_REPLY_TEXT)
                ?.toString()
                ?.trim()
                .orEmpty()
        } else {
            ""
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                LibraNotificationSyncManager.ensureFirebaseInitialized(appContext)
                val currentUid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull().orEmpty()
                if (currentUid.isBlank()) {
                    if (requestCode != 0) LibraNotificationHelper.cancelNotification(appContext, requestCode)
                    return@launch
                }

                val isGroup = type.equals(LibraNotificationTypes.GROUP_MESSAGE, ignoreCase = true)
                val isDm = type.equals(LibraNotificationTypes.MESSAGE, ignoreCase = true)

                val conversationId = when {
                    isGroup -> referenceId
                    isDm && referenceId.isNotBlank() -> referenceId
                    isDm && actorId.isNotBlank() -> listOf(currentUid, actorId).sorted().joinToString("_")
                    else -> ""
                }

                when (action) {
                    ACTION_MARK_READ -> {
                        if (notificationId.isNotBlank()) {
                            runCatching { ServiceLocator.notificationRepository.markRead(notificationId) }
                        }
                        if (conversationId.isNotBlank()) {
                            runCatching { ServiceLocator.chatRepository.markDirectConversationRead(conversationId) }
                        }
                        if (requestCode != 0) {
                            LibraNotificationHelper.cancelNotification(appContext, requestCode)
                        }
                    }

                    ACTION_INLINE_REPLY -> {
                        if (replyText.isBlank()) {
                            if (requestCode != 0) LibraNotificationHelper.cancelNotification(appContext, requestCode)
                            return@launch
                        }

                        val sendResult = when {
                            isGroup && referenceId.isNotBlank() -> {
                                ServiceLocator.chatRepository.sendGroupMessage(referenceId, replyText)
                            }
                            isDm -> {
                                val peerUid = actorId.ifBlank {
                                    referenceId.split("_").firstOrNull { it.isNotBlank() && it != currentUid }.orEmpty()
                                }
                                if (peerUid.isNotBlank()) {
                                    ServiceLocator.chatRepository.sendDirectMessage(peerUid, replyText)
                                } else {
                                    AppResult.Success(Unit)
                                }
                            }
                            else -> AppResult.Success(Unit)
                        }

                        if (notificationId.isNotBlank()) {
                            runCatching { ServiceLocator.notificationRepository.markRead(notificationId) }
                        }
                        if (conversationId.isNotBlank()) {
                            runCatching { ServiceLocator.chatRepository.markDirectConversationRead(conversationId) }
                        }

                        if (requestCode != 0) {
                            LibraNotificationHelper.showInlineReplyResultNotification(
                                context = appContext,
                                requestCode = requestCode,
                                conversationTitle = title.ifBlank { "Libra" },
                                senderName = actorName.ifBlank { title },
                                incomingText = incomingBody,
                                replyText = replyText,
                                isGroup = isGroup,
                                sentSuccessfully = sendResult is AppResult.Success
                            )
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_INLINE_REPLY = "com.libra.app.ACTION_NOTIF_INLINE_REPLY"
        const val ACTION_MARK_READ = "com.libra.app.ACTION_NOTIF_MARK_READ"
    }
}
