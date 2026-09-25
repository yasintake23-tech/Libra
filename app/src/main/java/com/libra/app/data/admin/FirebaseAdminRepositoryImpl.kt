package com.libra.app.data.admin

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AdminRole
import com.libra.app.domain.model.CosmeticRole
import com.libra.app.domain.model.UserModeration
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.repository.AdminRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAdminRepositoryImpl : AdminRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val users = firestore.collection("users")
    private val roles = firestore.collection("adminRoles")
    private val cosmetics = firestore.collection("cosmeticRoles")
    private val rtdb = FirebaseDatabase.getInstance("https://libra-3bfb9-default-rtdb.europe-west1.firebasedatabase.app")

    override fun observeMembers(): Flow<AppResult<List<UserProfile>>> = callbackFlow {
        val registration = users.orderBy("createdAt").limit(1000).addSnapshotListener { snapshot, error ->
            if (error != null) { trySend(AppResult.Error(AppError.Database("Üyeler yüklenemedi.", error))); return@addSnapshotListener }
            trySend(AppResult.Success(snapshot?.documents.orEmpty().mapNotNull { it.toObject(UserProfile::class.java) }.sortedByDescending { it.createdAt }))
        }
        awaitClose { registration.remove() }
    }

    override suspend fun updateMemberProfile(profile: UserProfile): AppResult<Unit> = try {
        val userRef = users.document(profile.uid)
        val username = profile.username.trim().lowercase()
        firestore.runTransaction { tx ->
            val current = tx.get(userRef)
            val oldUsername = current.getString("username").orEmpty().lowercase()
            if (username.isNotBlank() && username != oldUsername) {
                val newRef = firestore.collection("usernames").document(username)
                val existing = tx.get(newRef)
                if (existing.exists() && existing.getString("uid") != profile.uid) throw IllegalStateException("Bu kullanıcı adı zaten kullanılıyor.")
                tx.set(newRef, mapOf("uid" to profile.uid, "username" to username, "createdAt" to System.currentTimeMillis(), "updatedAt" to System.currentTimeMillis()))
                if (oldUsername.isNotBlank()) tx.delete(firestore.collection("usernames").document(oldUsername))
            }
            tx.update(userRef, mapOf("displayName" to profile.displayName.trim(),"username" to username,"bio" to profile.bio.trim(),"profileImageUrl" to profile.profileImageUrl,"updatedAt" to System.currentTimeMillis()))
            null
        }.await()
        AppResult.Success(Unit)
    } catch(e:Exception) { AppResult.Error(AppError.Database("Üye profili güncellenemedi: " + (e.localizedMessage ?: "Bilinmeyen hata."),e)) }

    override suspend fun updateModeration(uid:String, moderation:UserModeration):AppResult<Unit> = try {
        users.document(uid).update("moderation",moderation).await()
        rtdb.getReference("moderation").child(uid).setValue(moderation).await()
        AppResult.Success(Unit)
    } catch(e:Exception) { AppResult.Error(AppError.Database("Üye erişim ayarları güncellenemedi.",e)) }

    override suspend fun getAdminRole(uid:String):AppResult<AdminRole?> = try {
        val snap=roles.document(uid).get(Source.SERVER).await()
        AppResult.Success(if(snap.exists()) snap.toObject(AdminRole::class.java)?.copy(id=snap.id) else null)
    } catch(e:Exception){AppResult.Error(AppError.Database("Yönetici rolü okunamadı.",e))}

    override suspend fun setAdminRole(uid:String,role:AdminRole?):AppResult<Unit> = try {
        if(role==null) roles.document(uid).delete().await() else roles.document(uid).set(role.copy(id=uid)).await()
        AppResult.Success(Unit)
    } catch(e:Exception){AppResult.Error(AppError.Database("Yönetici rolü kaydedilemedi.",e))}

    override fun observeCosmeticRoles():Flow<AppResult<List<CosmeticRole>>> = callbackFlow {
        val registration=cosmetics.orderBy("createdAt").addSnapshotListener{snapshot,error->
            if(error!=null){trySend(AppResult.Error(AppError.Database("Kozmetik roller yüklenemedi.",error)));return@addSnapshotListener}
            trySend(AppResult.Success(snapshot?.documents.orEmpty().mapNotNull{it.toObject(CosmeticRole::class.java)?.copy(id=it.id)}))
        }
        awaitClose{registration.remove()}
    }

    override suspend fun saveCosmeticRole(role:CosmeticRole):AppResult<Unit> = try {
        val ref=if(role.id.isBlank())cosmetics.document() else cosmetics.document(role.id)
        ref.set(role.copy(id=ref.id,createdAt=if(role.createdAt==0L)System.currentTimeMillis()else role.createdAt)).await()
        AppResult.Success(Unit)
    }catch(e:Exception){AppResult.Error(AppError.Database("Kozmetik rol kaydedilemedi.",e))}

    override suspend fun deleteCosmeticRole(roleId:String):AppResult<Unit> = try{cosmetics.document(roleId).delete().await();AppResult.Success(Unit)}catch(e:Exception){AppResult.Error(AppError.Database("Kozmetik rol silinemedi.",e))}

    override suspend fun assignCosmeticRole(uid:String,roleId:String,assigned:Boolean):AppResult<Unit> = try{
        val ref=users.document(uid);val snap=ref.get(Source.SERVER).await()
        val ids=((snap.get("cosmeticRoleIds")as?List<*>)?.filterIsInstance<String>().orEmpty()).toMutableList()
        if(assigned&&roleId !in ids)ids.add(roleId);if(!assigned)ids.remove(roleId)
        ref.update("cosmeticRoleIds",ids).await();AppResult.Success(Unit)
    }catch(e:Exception){AppResult.Error(AppError.Database("Kozmetik rol ataması değiştirilemedi.",e))}
}