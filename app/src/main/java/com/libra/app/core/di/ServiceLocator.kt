package com.libra.app.core.di

import com.libra.app.data.ai.AiAssistantRepositoryImpl
import com.libra.app.data.auth.FirebaseAuthRepositoryImpl
import com.libra.app.data.book.BookRepositoryImpl
import com.libra.app.data.chat.FirebaseChatRepositoryImpl
import com.libra.app.data.chat.RealtimeChatRepositoryImpl
import com.libra.app.data.post.FirebasePostRepositoryImpl
import com.libra.app.data.storage.CloudflareR2StorageRepositoryImpl
import com.libra.app.data.user.FirebaseUserRepositoryImpl
import com.libra.app.data.notification.FirebaseNotificationRepositoryImpl
import com.libra.app.domain.repository.AiAssistantRepository
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.ChatRepository
import com.libra.app.domain.repository.PostRepository
import com.libra.app.domain.repository.StorageRepository
import com.libra.app.domain.repository.UserRepository
import com.libra.app.domain.repository.NotificationRepository

object ServiceLocator {
    val userRepository: UserRepository by lazy { FirebaseUserRepositoryImpl() }
    val authRepository: AuthRepository by lazy { FirebaseAuthRepositoryImpl(userRepository) }
    val bookRepository: BookRepository by lazy { BookRepositoryImpl() }
    private val firestoreChatRepository: ChatRepository by lazy { FirebaseChatRepositoryImpl(userRepository) }
    val chatRepository: ChatRepository by lazy { RealtimeChatRepositoryImpl(userRepository, firestoreChatRepository) }
    val postRepository: PostRepository by lazy { FirebasePostRepositoryImpl(userRepository) }
    val notificationRepository: NotificationRepository by lazy { FirebaseNotificationRepositoryImpl() }

    val r2StorageRepository: CloudflareR2StorageRepositoryImpl by lazy {
        CloudflareR2StorageRepositoryImpl(com.google.firebase.FirebaseApp.getInstance().applicationContext)
    }

    val storageRepository: StorageRepository by lazy { r2StorageRepository }

    val aiAssistantRepository: AiAssistantRepository by lazy { AiAssistantRepositoryImpl() }
}
