package com.libra.app.data.community

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.CommunityServer
import com.libra.app.domain.model.ServerMember
import com.libra.app.domain.model.ServerCategory
import com.libra.app.domain.model.ServerChannel
import com.libra.app.domain.repository.CommunityRepository
import com.libra.app.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseCommunityRepositoryImpl(
    private val userRepository: UserRepository
) : CommunityRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val serversRef get() = firestore.collection("communityServers")

    override fun observeCommunityServers(): Flow<AppResult<List<CommunityServer>>> = callbackFlow {
        val registration = serversRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Error(AppError.Database("Sunucular yüklenemedi.", error)))
                    return@addSnapshotListener
                }

                val servers = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    runCatching {
                        doc.toObject(CommunityServer::class.java)?.copy(id = doc.id)
                    }.getOrNull()
                }
                trySend(AppResult.Success(servers))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun createCommunityServer(
        name: String,
        description: String
    ): AppResult<CommunityServer> {
        val user = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Sunucu oluşturmak için giriş yapmalısın."))

        val cleanName = name.trim()
        val cleanDescription = description.trim()

        if (cleanName.length < 2) {
            return AppResult.Error(AppError.Validation("Sunucu adı en az 2 karakter olmalı."))
        }
        if (cleanName.length > 40) {
            return AppResult.Error(AppError.Validation("Sunucu adı en fazla 40 karakter olabilir."))
        }
        if (cleanDescription.length > 160) {
            return AppResult.Error(AppError.Validation("Açıklama en fazla 160 karakter olabilir."))
        }

        return try {
            val profile = (userRepository.getUserProfileFresh(user.uid) as? AppResult.Success)?.data
                ?: return AppResult.Error(AppError.Auth("Profil bulunamadı."))

            val serverRef = serversRef.document()
            val now = System.currentTimeMillis()
            val server = CommunityServer(
                id = serverRef.id,
                name = cleanName,
                description = cleanDescription,
                ownerId = user.uid,
                createdAt = now
            )

            val batch = firestore.batch()
            batch.set(serverRef, server)
            batch.set(
                serverRef.collection("members").document(user.uid),
                mapOf(
                    "uid" to user.uid,
                    "displayName" to profile.displayName,
                    "username" to profile.username,
                    "photoUrl" to profile.profileImageUrl,
                    "role" to "OWNER",
                    "joinedAt" to now
                )
            )

            // Every new server starts with a small Discord-style structure.
            val generalCategory = serverRef.collection("categories").document("general")
            val communityCategory = serverRef.collection("categories").document("community")
            batch.set(generalCategory, ServerCategory("general", "GENEL", 0))
            batch.set(communityCategory, ServerCategory("community", "TOPLULUK", 1))

            val defaultChannels = listOf(
                Triple("general-chat", generalCategory.id, "genel"),
                Triple("chat", generalCategory.id, "sohbet"),
                Triple("announcements", communityCategory.id, "duyurular")
            )
            defaultChannels.forEachIndexed { index, (id, categoryId, name) ->
                val categoryName = if (categoryId == "general") "GENEL" else "TOPLULUK"
                batch.set(
                    serverRef.collection("channels").document(id),
                    ServerChannel(
                        id = id,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        name = name,
                        type = "TEXT",
                        position = index
                    )
                )
            }

            batch.commit().await()

            AppResult.Success(server)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Sunucu oluşturulamadı.", e))
        }
    }

    override suspend fun updateCommunityServer(
        serverId: String,
        name: String,
        description: String
    ): AppResult<Unit> {
        val user = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))

        val cleanName = name.trim()
        val cleanDescription = description.trim()

        if (serverId.isBlank()) {
            return AppResult.Error(AppError.Validation("Geçersiz sunucu."))
        }
        if (cleanName.length < 2) {
            return AppResult.Error(AppError.Validation("Sunucu adı en az 2 karakter olmalı."))
        }
        if (cleanName.length > 40) {
            return AppResult.Error(AppError.Validation("Sunucu adı en fazla 40 karakter olabilir."))
        }
        if (cleanDescription.length > 160) {
            return AppResult.Error(AppError.Validation("Açıklama en fazla 160 karakter olabilir."))
        }

        return try {
            val ref = serversRef.document(serverId)
            val server = ref.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.Validation("Sunucu bulunamadı."))

            if (server.ownerId != user.uid) {
                return AppResult.Error(AppError.Auth("Sadece sunucu sahibi düzenleyebilir."))
            }

            ref.update(
                mapOf(
                    "name" to cleanName,
                    "description" to cleanDescription
                )
            ).await()

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Sunucu güncellenemedi.", e))
        }
    }

    override suspend fun joinCommunityServer(serverId: String): AppResult<Unit> {
        val user = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Sunucuya katılmak için giriş yapmalısın."))

        if (serverId.isBlank()) {
            return AppResult.Error(AppError.Validation("Geçersiz sunucu."))
        }

        return try {
            val serverRef = serversRef.document(serverId)
            val serverSnapshot = serverRef.get().await()
            if (!serverSnapshot.exists()) {
                return AppResult.Error(AppError.Validation("Sunucu bulunamadı."))
            }

            val memberRef = serverRef.collection("members").document(user.uid)
            val memberSnapshot = memberRef.get().await()
            val serverOwnerId = serverSnapshot.getString("ownerId").orEmpty()

            if (!memberSnapshot.exists()) {
                memberRef.set(
                    mapOf(
                        "uid" to user.uid,
                        "displayName" to (user.displayName ?: ""),
                        "role" to if (serverOwnerId == user.uid) "OWNER" else "MEMBER",
                        "joinedAt" to System.currentTimeMillis()
                    )
                ).await()
            } else if (
                serverOwnerId == user.uid &&
                memberSnapshot.getString("role") != "OWNER"
            ) {
                memberRef.delete().await()
                memberRef.set(
                    mapOf(
                        "uid" to user.uid,
                        "displayName" to (user.displayName ?: ""),
                        "role" to "OWNER",
                        "joinedAt" to (
                            memberSnapshot.getLong("joinedAt")
                                ?: System.currentTimeMillis()
                            )
                    )
                ).await()
            }

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Sunucuya katılınamadı.", e))
        }
    }

    override suspend fun isServerMember(serverId: String): AppResult<Boolean> {
        val user = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        if (serverId.isBlank()) return AppResult.Success(false)
        return try {
            AppResult.Success(
                serversRef.document(serverId).collection("members").document(user.uid).get().await().exists()
            )
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Sunucu üyeliği kontrol edilemedi.", e))
        }
    }

    override fun observeServerCategories(serverId: String): Flow<AppResult<List<ServerCategory>>> =
        callbackFlow {
            val registration = serversRef.document(serverId)
                .collection("categories")
                .orderBy("position", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(AppResult.Error(AppError.Database("Kategoriler yüklenemedi.", error)))
                        return@addSnapshotListener
                    }
                    val categories = snapshot?.documents.orEmpty().mapNotNull { doc ->
                        runCatching { doc.toObject(ServerCategory::class.java)?.copy(id = doc.id) }.getOrNull()
                    }
                    trySend(AppResult.Success(categories))
                }
            awaitClose { registration.remove() }
        }

    override fun observeServerChannels(serverId: String): Flow<AppResult<List<ServerChannel>>> =
        callbackFlow {
            val registration = serversRef.document(serverId)
                .collection("channels")
                .orderBy("position", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(AppResult.Error(AppError.Database("Kanallar yüklenemedi.", error)))
                        return@addSnapshotListener
                    }
                    val channels = snapshot?.documents.orEmpty().mapNotNull { doc ->
                        runCatching { doc.toObject(ServerChannel::class.java)?.copy(id = doc.id) }.getOrNull()
                    }
                    trySend(AppResult.Success(channels))
                }
            awaitClose { registration.remove() }
        }

    override fun observeServerMembers(serverId: String): Flow<AppResult<List<ServerMember>>> =
        callbackFlow {
            val registration = serversRef
                .document(serverId)
                .collection("members")
                .orderBy("joinedAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(
                            AppResult.Error(
                                AppError.Database("Üyeler yüklenemedi.", error)
                            )
                        )
                        return@addSnapshotListener
                    }

                    val members = snapshot?.documents.orEmpty().mapNotNull { doc ->
                        runCatching {
                            doc.toObject(ServerMember::class.java)?.copy(uid = doc.id)
                        }.getOrNull()
                    }
                    trySend(AppResult.Success(members))
                }

            awaitClose { registration.remove() }
        }

    override suspend fun setServerMemberRole(
        serverId: String,
        memberId: String,
        role: String
    ): AppResult<Unit> {
        val user = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))

        if (role !in setOf("ADMIN", "MEMBER")) {
            return AppResult.Error(AppError.Validation("Geçersiz rol."))
        }

        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.Validation("Sunucu bulunamadı."))

            if (server.ownerId != user.uid) {
                return AppResult.Error(AppError.Auth("Sadece sunucu sahibi rol değiştirebilir."))
            }
            if (memberId == server.ownerId) {
                return AppResult.Error(AppError.Validation("Sahibin rolü değiştirilemez."))
            }

            serverRef.collection("members").document(memberId).update("role", role).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Üye rolü değiştirilemedi.", e))
        }
    }

    override suspend fun removeServerMember(
        serverId: String,
        memberId: String
    ): AppResult<Unit> {
        val user = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))

        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.Validation("Sunucu bulunamadı."))

            if (server.ownerId != user.uid) {
                return AppResult.Error(AppError.Auth("Sadece sunucu sahibi üye çıkarabilir."))
            }
            if (memberId == server.ownerId) {
                return AppResult.Error(AppError.Validation("Sunucu sahibi çıkarılamaz."))
            }

            serverRef.collection("members").document(memberId).delete().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Üye çıkarılamadı.", e))
        }
    }

    override suspend fun leaveCommunityServer(serverId: String): AppResult<Unit> {
        val user = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Sunucudan ayrılmak için giriş yapmalısın."))

        return try {
            serversRef.document(serverId)
                .collection("members")
                .document(user.uid)
                .delete()
                .await()

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Sunucudan ayrılamadın.", e))
        }
    }
}
