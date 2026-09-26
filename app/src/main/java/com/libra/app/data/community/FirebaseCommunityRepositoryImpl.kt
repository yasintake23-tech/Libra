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
import com.libra.app.domain.model.ServerChannelPermissionOverride
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.repository.CommunityRepository
import com.libra.app.domain.repository.StorageRepository
import com.libra.app.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class FirebaseCommunityRepositoryImpl(
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository
) : CommunityRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val serversRef get() = firestore.collection("communityServers")
    private val inviteRandom = SecureRandom()
    private val inviteAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"

    private fun normalizeInviteKey(input: String): String {
        return input.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("libra.sc/")
            .trim('/')
            .take(32)
    }

    private fun generateInviteKey(): String {
        return buildString(9) {
            repeat(9) { append(inviteAlphabet[inviteRandom.nextInt(inviteAlphabet.length)]) }
        }
    }

    private suspend fun createUniqueInviteKey(): String {
        repeat(8) {
            val candidate = generateInviteKey()
            val exists = serversRef.whereEqualTo("inviteKey", candidate).limit(1).get().await()
            if (exists.isEmpty) return candidate
        }
        throw IllegalStateException("Sunucu davet anahtarı üretilemedi.")
    }

    override suspend fun findCommunityServerByInviteKey(
        inviteKey: String
    ): AppResult<CommunityServer> {
        val cleanKey = normalizeInviteKey(inviteKey)
        if (cleanKey.length != 9) {
            return AppResult.Error(AppError.Validation("Geçerli bir sunucu anahtarı yaz. Örnek: libra.sc/Je9jehowm"))
        }

        return try {
            val snapshot = serversRef
                .whereEqualTo("inviteKey", cleanKey)
                .limit(1)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull()
                ?: return AppResult.Error(AppError.NotFound("Bu anahtarla eşleşen bir sunucu bulunamadı."))

            val server = doc.toObject(CommunityServer::class.java)?.copy(id = doc.id)
                ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))


            AppResult.Success(server)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Sunucu anahtarı aranamadı.", e))
        }
    }

    override suspend fun ensureCommunityServerInviteKey(serverId: String): AppResult<String> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        if (serverId.isBlank()) return AppResult.Error(AppError.Validation("Geçersiz sunucu."))

        return try {
            val ref = serversRef.document(serverId)
            val server = ref.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))

            if (server.ownerId != user.uid) {
                return AppResult.Error(AppError.Auth("Sadece sunucu sahibi davet anahtarını oluşturabilir."))
            }

            if (server.inviteKey.isNotBlank()) {
                return AppResult.Success(server.inviteKey)
            }

            val key = createUniqueInviteKey()
            ref.update("inviteKey", key).await()
            AppResult.Success(key)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Sunucu anahtarı oluşturulamadı.", e))
        }
    }

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
            val inviteKey = createUniqueInviteKey()
            val server = CommunityServer(
                id = serverRef.id,
                name = cleanName,
                description = cleanDescription,
                ownerId = user.uid,
                createdAt = now,
                inviteKey = inviteKey,
                avatarUrl = "",
                bannerUrl = ""
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

    override suspend fun updateCommunityServerMedia(
        serverId: String,
        avatarUrl: String,
        bannerUrl: String
    ): AppResult<Unit> {
        val user = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))

        if (serverId.isBlank()) {
            return AppResult.Error(AppError.Validation("Geçersiz sunucu."))
        }

        val cleanAvatar = avatarUrl.trim()
        val cleanBanner = bannerUrl.trim()

        return try {
            val ref = serversRef.document(serverId)
            val server = ref.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))

            if (server.ownerId != user.uid) {
                return AppResult.Error(AppError.Auth("Sadece sunucu sahibi medya ayarlarını değiştirebilir."))
            }

            val ownedPrefix = "users/" + user.uid + "/"
            if (
                (cleanAvatar.isNotBlank() && !cleanAvatar.startsWith(ownedPrefix)) ||
                (cleanBanner.isNotBlank() && !cleanBanner.startsWith(ownedPrefix))
            ) {
                return AppResult.Error(AppError.Validation("Sunucu görselleri yalnızca Libra R2 nesne anahtarı olabilir."))
            }

            ref.update(
                mapOf(
                    "avatarUrl" to cleanAvatar,
                    "bannerUrl" to cleanBanner
                )
            ).await()

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Sunucu görselleri güncellenemedi.", e))
        }
    }

    override suspend fun uploadServerAvatar(
        serverId: String,
        fileName: String,
        bytes: ByteArray,
        contentType: String,
        onProgress: (Int) -> Unit
    ): AppResult<String> = uploadServerMedia(
        serverId = serverId,
        fileName = "server-" + serverId + "-avatar-" + fileName,
        bytes = bytes,
        contentType = contentType,
        onProgress = onProgress
    )

    override suspend fun uploadServerBanner(
        serverId: String,
        fileName: String,
        bytes: ByteArray,
        contentType: String,
        onProgress: (Int) -> Unit
    ): AppResult<String> = uploadServerMedia(
        serverId = serverId,
        fileName = "server-" + serverId + "-banner-" + fileName,
        bytes = bytes,
        contentType = contentType,
        onProgress = onProgress
    )

    private suspend fun uploadServerMedia(
        serverId: String,
        fileName: String,
        bytes: ByteArray,
        contentType: String,
        onProgress: (Int) -> Unit
    ): AppResult<String> {
        val user = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Medya yüklemek için giriş yapmalısın."))

        if (serverId.isBlank()) {
            return AppResult.Error(AppError.Validation("Geçersiz sunucu."))
        }

        if (contentType.isBlank() || !contentType.startsWith("image/")) {
            return AppResult.Error(AppError.Validation("Sunucu görseli geçerli bir resim olmalı."))
        }

        return try {
            val server = serversRef.document(serverId).get().await()
                .toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))

            if (server.ownerId != user.uid) {
                return AppResult.Error(AppError.Auth("Sadece sunucu sahibi medya yükleyebilir."))
            }

            storageRepository.uploadMedia(
                StorageUploadRequest(
                    fileName = fileName,
                    bytes = bytes,
                    contentType = contentType,
                    targetDirectory = "users/" + user.uid
                ),
                onProgress = onProgress
            ).first()
        } catch (e: Exception) {
            AppResult.Error(AppError.Storage("Sunucu medyası yüklenemedi.", e))
        }
    }

    override suspend fun deleteServerMedia(
        serverId: String,
        fileKey: String
    ): AppResult<Unit> {
        val user = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))

        if (serverId.isBlank() || fileKey.isBlank()) {
            return AppResult.Error(AppError.Validation("Geçersiz sunucu medyası."))
        }

        return try {
            val server = serversRef.document(serverId).get().await()
                .toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))

            if (server.ownerId != user.uid) {
                return AppResult.Error(AppError.Auth("Sadece sunucu sahibi medya silebilir."))
            }

            val ownedPrefix = "users/" + user.uid + "/"
            if (!fileKey.trim().startsWith(ownedPrefix)) {
                return AppResult.Error(AppError.Auth("Bu medya dosyasına erişimin yok."))
            }

            storageRepository.deleteMedia(fileKey.trim())
        } catch (e: Exception) {
            AppResult.Error(AppError.Storage("Sunucu medyası silinemedi.", e))
        }
    }

    override suspend fun deleteCommunityServer(serverId: String): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi silebilir."))
            val batch = firestore.batch()
            listOf("members", "categories", "channels", "roles", "bans").forEach { collection ->
                serverRef.collection(collection).get().await().documents.forEach { batch.delete(it.reference) }
            }
            batch.delete(serverRef)
            batch.commit().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Sunucu silinemedi.", e))
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

            if (serverRef.collection("bans").document(user.uid).get().await().exists()) {
                return AppResult.Error(AppError.Auth("Bu sunucudan yasaklandın."))
            }

            val memberRef = serverRef.collection("members").document(user.uid)
            val memberSnapshot = memberRef.get().await()
            val serverOwnerId = serverSnapshot.getString("ownerId").orEmpty()

            if (memberSnapshot.exists()) {
                // Joining is idempotent. A second tap never recreates membership.
                return AppResult.Success(Unit)
            }

            memberRef.set(
                mapOf(
                    "uid" to user.uid,
                    "displayName" to (user.displayName ?: ""),
                    "role" to if (serverOwnerId == user.uid) "OWNER" else "MEMBER",
                    "joinedAt" to System.currentTimeMillis()
                )
            ).await()

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

    override suspend fun ensureServerStructure(serverId: String): AppResult<Unit> {
        val user = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        if (serverId.isBlank()) return AppResult.Error(AppError.Validation("Geçersiz sunucu."))

        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) {
                if (serverRef.collection("bans").document(user.uid).get().await().exists()) {
                    return AppResult.Error(AppError.Auth("Bu sunucudan yasaklandın."))
                }
                val member = serverRef.collection("members").document(user.uid).get().await()
                if (!member.exists()) {
                    return AppResult.Error(AppError.Auth("Bu sunucuya erişimin yok."))
                }
            }

            val categories = serverRef.collection("categories").get().await()
            val channels = serverRef.collection("channels").get().await()
            if (!categories.isEmpty && !channels.isEmpty) return AppResult.Success(Unit)

            val batch = firestore.batch()
            val generalCategory = serverRef.collection("categories").document("general")
            val communityCategory = serverRef.collection("categories").document("community")
            if (!categories.documents.any { it.id == "general" }) {
                batch.set(generalCategory, ServerCategory("general", "GENEL", 0))
            }
            if (!categories.documents.any { it.id == "community" }) {
                batch.set(communityCategory, ServerCategory("community", "TOPLULUK", 1))
            }

            val defaults = listOf(
                ServerChannel("general-chat", "general", "GENEL", "genel", "TEXT", 0),
                ServerChannel("chat", "general", "GENEL", "sohbet", "TEXT", 1),
                ServerChannel("announcements", "community", "TOPLULUK", "duyurular", "TEXT", 2)
            )
            defaults.forEach { channel ->
                if (!channels.documents.any { it.id == channel.id }) {
                    batch.set(serverRef.collection("channels").document(channel.id), channel)
                }
            }
            batch.commit().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Sunucu yapısı hazırlanamadı.", e))
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


    override suspend fun getChannelPermissions(
        serverId: String,
        channelId: String
    ): AppResult<List<ServerChannelPermissionOverride>> {
        return try {
            val snapshot = serversRef.document(serverId).collection("channels")
                .document(channelId).collection("permissions").get().await()
            AppResult.Success(
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(ServerChannelPermissionOverride::class.java)?.copy(id = doc.id)
                    }.getOrNull()
                }
            )
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kanal izinleri okunamadı.", e))
        }
    }

    override fun observeChannelPermissions(
        serverId: String,
        channelId: String
    ): Flow<AppResult<List<ServerChannelPermissionOverride>>> = callbackFlow {
        val registration = serversRef.document(serverId).collection("channels")
            .document(channelId).collection("permissions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Error(AppError.Database("Kanal izinleri yüklenemedi.", error)))
                    return@addSnapshotListener
                }
                val permissions = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    runCatching {
                        doc.toObject(ServerChannelPermissionOverride::class.java)?.copy(id = doc.id)
                    }.getOrNull()
                }
                trySend(AppResult.Success(permissions))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun createServerCategory(serverId: String, name: String): AppResult<ServerCategory> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        val clean = name.trim()
        if (clean.length !in 1..40) return AppResult.Error(AppError.Validation("Kategori adı 1-40 karakter olmalı."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi kategori oluşturabilir."))
            val position = serverRef.collection("categories").get().await().size()
            val ref = serverRef.collection("categories").document()
            val category = ServerCategory(ref.id, clean, position)
            ref.set(category).await()
            AppResult.Success(category)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kategori oluşturulamadı.", e))
        }
    }

    override suspend fun updateServerCategory(serverId: String, categoryId: String, name: String): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        val clean = name.trim()
        if (clean.length !in 1..40) return AppResult.Error(AppError.Validation("Kategori adı 1-40 karakter olmalı."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java) ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi kategori düzenleyebilir."))
            serverRef.collection("categories").document(categoryId).update("name", clean).await()
            val channels = serverRef.collection("channels").whereEqualTo("categoryId", categoryId).get().await()
            val batch = firestore.batch()
            channels.documents.forEach { batch.update(it.reference, "categoryName", clean) }
            batch.commit().await()
            AppResult.Success(Unit)
        } catch (e: Exception) { AppResult.Error(AppError.Database("Kategori güncellenemedi.", e)) }
    }

    override suspend fun deleteServerCategory(serverId: String, categoryId: String): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi kategori silebilir."))
            val channels = serverRef.collection("channels").whereEqualTo("categoryId", categoryId).get().await()
            val batch = firestore.batch()
            channels.documents.forEach { batch.delete(it.reference) }
            batch.delete(serverRef.collection("categories").document(categoryId))
            batch.commit().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kategori silinemedi.", e))
        }
    }

    override suspend fun createServerChannel(
        serverId: String,
        categoryId: String,
        name: String
    ): AppResult<ServerChannel> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        val clean = name.trim().lowercase().replace(Regex("\\s+"), "-")
        if (clean.length !in 1..40) return AppResult.Error(AppError.Validation("Kanal adı 1-40 karakter olmalı."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi kanal oluşturabilir."))
            val category = serverRef.collection("categories").document(categoryId).get().await()
            if (!category.exists()) return AppResult.Error(AppError.NotFound("Kategori bulunamadı."))
            val position = serverRef.collection("channels").whereEqualTo("categoryId", categoryId).get().await().size()
            val ref = serverRef.collection("channels").document()
            val channel = ServerChannel(
                id = ref.id,
                categoryId = categoryId,
                categoryName = category.getString("name").orEmpty(),
                name = clean,
                position = position
            )
            ref.set(channel).await()
            AppResult.Success(channel)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kanal oluşturulamadı.", e))
        }
    }

    override suspend fun updateServerChannel(serverId: String, channelId: String, name: String): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        val clean = name.trim().lowercase().replace(" ", "-")
        if (clean.length !in 1..40) return AppResult.Error(AppError.Validation("Kanal adı 1-40 karakter olmalı."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java) ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi kanal düzenleyebilir."))
            serverRef.collection("channels").document(channelId).update("name", clean).await()
            AppResult.Success(Unit)
        } catch (e: Exception) { AppResult.Error(AppError.Database("Kanal güncellenemedi.", e)) }
    }

    override suspend fun moveServerChannelToCategory(serverId: String, channelId: String, categoryId: String): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java) ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi kanal taşıyabilir."))
            val category = serverRef.collection("categories").document(categoryId).get().await()
            if (!category.exists()) return AppResult.Error(AppError.NotFound("Kategori bulunamadı."))
            val channelRef = serverRef.collection("channels").document(channelId)
            val channel = channelRef.get().await()
            if (!channel.exists()) return AppResult.Error(AppError.NotFound("Kanal bulunamadı."))
            if (channel.getString("categoryId").orEmpty() == categoryId) return AppResult.Success(Unit)
            val targetCount = serverRef.collection("channels").whereEqualTo("categoryId", categoryId).get().await().size()
            channelRef.update(mapOf("categoryId" to categoryId, "categoryName" to category.getString("name").orEmpty(), "position" to targetCount)).await()
            AppResult.Success(Unit)
        } catch (e: Exception) { AppResult.Error(AppError.Database("Kanal taşınamadı.", e)) }
    }

    override suspend fun deleteServerChannel(serverId: String, channelId: String): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi kanal silebilir."))
            serverRef.collection("channels").document(channelId).delete().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kanal silinemedi.", e))
        }
    }

    override suspend fun setChannelPermission(
        serverId: String,
        channelId: String,
        override: ServerChannelPermissionOverride
    ): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi izin değiştirebilir."))
            if (override.subjectId.isBlank()) return AppResult.Error(AppError.Validation("İzin hedefi bulunamadı."))
            val id = override.id.ifBlank { override.subjectType + "_" + override.subjectId }
            serverRef.collection("channels").document(channelId)
                .collection("permissions").document(id)
                .set(override.copy(id = id)).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kanal izni kaydedilemedi.", e))
        }
    }

    override suspend fun deleteChannelPermission(
        serverId: String,
        channelId: String,
        overrideId: String
    ): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            val server = serversRef.document(serverId).get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi izin silebilir."))
            serversRef.document(serverId).collection("channels").document(channelId)
                .collection("permissions").document(overrideId).delete().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kanal izni silinemedi.", e))
        }
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

    override fun observeServerRoles(serverId: String): Flow<AppResult<List<com.libra.app.domain.model.ServerRoleDefinition>>> = callbackFlow {
        val registration = serversRef.document(serverId).collection("roles").orderBy("position", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Error(AppError.Database("Roller yüklenemedi.", error)))
                    return@addSnapshotListener
                }
                val roles = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    runCatching { doc.toObject(com.libra.app.domain.model.ServerRoleDefinition::class.java)?.copy(id = doc.id) }.getOrNull()
                }
                trySend(AppResult.Success(roles))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun createServerRole(serverId: String, name: String, permissions: List<String>): AppResult<com.libra.app.domain.model.ServerRoleDefinition> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        val clean = name.trim()
        if (clean.length !in 1..30) return AppResult.Error(AppError.Validation("Rol adı 1-30 karakter olmalı."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java) ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi rol oluşturabilir."))
            val ref = serverRef.collection("roles").document()
            val position = serverRef.collection("roles").get().await().size()
            val role = com.libra.app.domain.model.ServerRoleDefinition(ref.id, clean, permissions.distinct(), position)
            ref.set(role).await()
            AppResult.Success(role)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Rol oluşturulamadı.", e))
        }
    }

    override suspend fun updateServerRole(serverId: String, roleId: String, name: String, permissions: List<String>): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        val clean = name.trim()
        if (clean.length !in 1..30) return AppResult.Error(AppError.Validation("Rol adı 1-30 karakter olmalı."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi rol düzenleyebilir."))
            serverRef.collection("roles").document(roleId).update(
                mapOf("name" to clean, "permissions" to permissions.distinct())
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Rol güncellenemedi.", e))
        }
    }

    override suspend fun moveServerRole(serverId: String, roleId: String, direction: Int): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        if (direction != -1 && direction != 1) {
            return AppResult.Error(AppError.Validation("Geçersiz rol sıralama yönü."))
        }
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) {
                return AppResult.Error(AppError.Auth("Sadece sunucu sahibi rol sıralayabilir."))
            }
            val docs = serverRef.collection("roles").orderBy("position").get().await().documents
            val index = docs.indexOfFirst { it.id == roleId }
            if (index < 0) return AppResult.Error(AppError.NotFound("Rol bulunamadı."))
            val target = index + direction
            if (target !in docs.indices) return AppResult.Success(Unit)

            val batch = firestore.batch()
            docs.forEachIndexed { position, doc ->
                val newPosition = when (position) {
                    index -> target
                    target -> index
                    else -> position
                }
                if (newPosition != position) {
                    batch.update(doc.reference, "position", newPosition)
                }
            }
            batch.commit().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Rol sırası değiştirilemedi.", e))
        }
    }

    override suspend fun deleteServerRole(serverId: String, roleId: String): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java) ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi rol silebilir."))
            serverRef.collection("roles").document(roleId).delete().await()
            val members = serverRef.collection("members").whereEqualTo("role", roleId).get().await()
            val batch = firestore.batch()
            members.documents.forEach { batch.update(it.reference, "role", "MEMBER") }
            batch.commit().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Rol silinemedi.", e))
        }
    }

    override suspend fun setServerMemberRole(
        serverId: String,
        memberId: String,
        role: String
    ): AppResult<Unit> {
        val user = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))

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
            if (role != "MEMBER" && role != "ADMIN" && !serverRef.collection("roles").document(role).get().await().exists()) {
                return AppResult.Error(AppError.Validation("Rol bulunamadı."))
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

    override suspend fun banServerMember(serverId: String, memberId: String, reason: String): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java) ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi üye yasaklayabilir."))
            if (memberId == server.ownerId) return AppResult.Error(AppError.Validation("Sunucu sahibi yasaklanamaz."))
            val memberRef = serverRef.collection("members").document(memberId)
            val member = memberRef.get().await()
            if (!member.exists()) return AppResult.Error(AppError.NotFound("Üye bulunamadı."))
            val batch = firestore.batch()
            batch.set(serverRef.collection("bans").document(memberId), mapOf(
                "uid" to memberId,
                "displayName" to member.getString("displayName").orEmpty(),
                "username" to member.getString("username").orEmpty(),
                "bannedAt" to System.currentTimeMillis(),
                "reason" to reason.trim()
            ))
            batch.delete(memberRef)
            batch.commit().await()
            AppResult.Success(Unit)
        } catch (e: Exception) { AppResult.Error(AppError.Database("Üye yasaklanamadı.", e)) }
    }

    override fun observeServerBans(serverId: String): Flow<AppResult<List<com.libra.app.domain.model.ServerBan>>> = callbackFlow {
        val registration = serversRef.document(serverId).collection("bans")
            .orderBy("bannedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Error(AppError.Database("Yasaklar yüklenemedi.", error)))
                    return@addSnapshotListener
                }
                val bans = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    runCatching {
                        doc.toObject(com.libra.app.domain.model.ServerBan::class.java)?.copy(uid = doc.id)
                    }.getOrNull()
                }
                trySend(AppResult.Success(bans))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun unbanServerMember(serverId: String, memberId: String): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java) ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi yasağı kaldırabilir."))
            serverRef.collection("bans").document(memberId).delete().await()
            AppResult.Success(Unit)
        } catch (e: Exception) { AppResult.Error(AppError.Database("Yasak kaldırılamadı.", e)) }
    }

    override suspend fun moveServerCategory(serverId: String, categoryId: String, direction: Int): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            val ref = serversRef.document(serverId)
            val server = ref.get().await().toObject(CommunityServer::class.java) ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi taşıyabilir."))
            val docs = ref.collection("categories").orderBy("position").get().await().documents
            val index = docs.indexOfFirst { it.id == categoryId }; val target = index + direction
            if (index < 0 || target !in docs.indices) return AppResult.Success(Unit)
            val batch = firestore.batch()
            docs.forEachIndexed { pos, doc -> batch.update(doc.reference, "position", when(pos) { index -> target; target -> index; else -> pos }) }
            batch.commit().await(); AppResult.Success(Unit)
        } catch (e: Exception) { AppResult.Error(AppError.Database("Kategori taşınamadı.", e)) }
    }

    override suspend fun moveServerChannel(serverId: String, channelId: String, direction: Int): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            val ref = serversRef.document(serverId)
            val server = ref.get().await().toObject(CommunityServer::class.java) ?: return AppResult.Error(AppError.NotFound("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi taşıyabilir."))
            val channel = ref.collection("channels").document(channelId).get().await()
            if (!channel.exists()) return AppResult.Error(AppError.NotFound("Kanal bulunamadı."))
            val categoryId = channel.getString("categoryId").orEmpty()
            val docs = ref.collection("channels").whereEqualTo("categoryId", categoryId).orderBy("position").get().await().documents
            val index = docs.indexOfFirst { it.id == channelId }; val target = index + direction
            if (index < 0 || target !in docs.indices) return AppResult.Success(Unit)
            val batch = firestore.batch()
            docs.forEachIndexed { pos, doc -> batch.update(doc.reference, "position", when(pos) { index -> target; target -> index; else -> pos }) }
            batch.commit().await(); AppResult.Success(Unit)
        } catch (e: Exception) { AppResult.Error(AppError.Database("Kanal taşınamadı.", e)) }
    }

    override suspend fun isServerBanned(serverId: String): AppResult<Boolean> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try { AppResult.Success(serversRef.document(serverId).collection("bans").document(user.uid).get().await().exists()) }
        catch (e: Exception) { AppResult.Error(AppError.Database("Sunucu yasağı kontrol edilemedi.", e)) }
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
