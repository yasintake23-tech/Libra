package com.libra.app.core.di

import com.libra.app.data.admin.FirebaseAdminRepositoryImpl
import com.libra.app.data.ai.AiAssistantRepositoryImpl
import com.libra.app.data.auth.FirebaseAuthRepositoryImpl
import com.libra.app.data.book.BookRepositoryImpl
import com.libra.app.data.chat.RealtimeChatRepositoryImpl
import com.libra.app.data.community.FirebaseCommunityRepositoryImpl
import com.libra.app.data.post.FirebasePostRepositoryImpl
import com.libra.app.data.storage.CloudflareR2StorageRepositoryImpl
import com.libra.app.data.story.FirebaseStoryRepositoryImpl
import com.libra.app.data.user.FirebaseUserRepositoryImpl
import com.libra.app.data.notification.FirebaseNotificationRepositoryImpl
import com.libra.app.data.update.CloudflareR2AppReleaseStorageRepositoryImpl
import com.libra.app.data.update.FirebaseUpdateRepositoryImpl
import com.libra.app.domain.repository.AdminRepository
import com.libra.app.domain.repository.AiAssistantRepository
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.ChatRepository
import com.libra.app.domain.repository.CommunityRepository
import com.libra.app.domain.repository.PostRepository
import com.libra.app.domain.repository.StorageRepository
import com.libra.app.domain.repository.StoryRepository
import com.libra.app.domain.repository.UserRepository
import com.libra.app.domain.repository.NotificationRepository
import com.libra.app.domain.repository.UpdateRepository
import com.libra.app.domain.repository.AppReleaseStorageRepository

object ServiceLocator {
    val userRepository: UserRepository by lazy { FirebaseUserRepositoryImpl() }
    val authRepository: AuthRepository by lazy { FirebaseAuthRepositoryImpl(userRepository) }
    val adminRepository: AdminRepository by lazy { FirebaseAdminRepositoryImpl() }
    val bookRepository: BookRepository by lazy { BookRepositoryImpl() }
    val communityRepository: CommunityRepository by lazy {
        FirebaseCommunityRepositoryImpl(userRepository, storageRepository)
    }
    val chatRepository: ChatRepository by lazy { RealtimeChatRepositoryImpl(userRepository, storageRepository, notificationRepository) }
    val postRepository: PostRepository by lazy { FirebasePostRepositoryImpl(userRepository, storageRepository, notificationRepository) }
    val storyRepository: StoryRepository by lazy { FirebaseStoryRepositoryImpl() }
    val notificationRepository: NotificationRepository by lazy { FirebaseNotificationRepositoryImpl() }

    val r2StorageRepository: CloudflareR2StorageRepositoryImpl by lazy {
        CloudflareR2StorageRepositoryImpl(com.google.firebase.FirebaseApp.getInstance().applicationContext)
    }

    val storageRepository: StorageRepository by lazy { r2StorageRepository }

    val updateRepository: UpdateRepository by lazy { FirebaseUpdateRepositoryImpl() }

    val appReleaseStorageRepository: AppReleaseStorageRepository by lazy {
        CloudflareR2AppReleaseStorageRepositoryImpl(
            com.google.firebase.FirebaseApp.getInstance().applicationContext
        )
    }

    val aiAssistantRepository: AiAssistantRepository by lazy { AiAssistantRepositoryImpl() }
}