package com.libra.app.domain.model

enum class ShelfType(val titleTr: String) {
    MY_WRITINGS("Yazdıklarım"),
    READING("Okuduklarım"),
    SAVED("Kaydettiklerim")
}

data class UserShelfItem(
    val id: String = "",
    val userId: String = "",
    val book: Book = Book(),
    val shelfType: ShelfType = ShelfType.SAVED,
    val progressPercent: Int = 0,
    val addedAt: Long = 0L
)

data class FriendConnection(
    val id: String = "",
    val userId: String = "",
    val friendProfile: UserProfile = UserProfile(),
    val status: String = "ACCEPTED",
    val mutualFriendsCount: Int = 0,
    val createdAt: Long = 0L
)

data class StorageUploadRequest(
    val fileName: String,
    val bytes: ByteArray,
    val contentType: String,
    val targetDirectory: String = "uploads"
)
