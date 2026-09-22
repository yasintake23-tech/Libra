package com.libra.app.core.di

import com.libra.app.data.ai.AiAssistantRepositoryImpl
import com.libra.app.data.auth.FirebaseAuthRepositoryImpl
import com.libra.app.data.book.BookRepositoryImpl
import com.libra.app.data.storage.CloudflareR2StorageRepositoryImpl
import com.libra.app.data.storage.FirebaseStorageRepositoryImpl
import com.libra.app.data.storage.HybridStorageRepositoryImpl
import com.libra.app.data.user.FirebaseUserRepositoryImpl
import com.libra.app.domain.repository.AiAssistantRepository
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.StorageRepository
import com.libra.app.domain.repository.UserRepository

object ServiceLocator {
    val userRepository: UserRepository by lazy { FirebaseUserRepositoryImpl() }
    val authRepository: AuthRepository by lazy { FirebaseAuthRepositoryImpl(userRepository) }
    val bookRepository: BookRepository by lazy { BookRepositoryImpl() }

    val r2StorageRepository: CloudflareR2StorageRepositoryImpl by lazy {
        CloudflareR2StorageRepositoryImpl()
    }

    val storageRepository: StorageRepository by lazy {
        HybridStorageRepositoryImpl(
            r2 = r2StorageRepository,
            fallback = FirebaseStorageRepositoryImpl()
        )
    }

    val aiAssistantRepository: AiAssistantRepository by lazy { AiAssistantRepositoryImpl() }
}
