package com.libra.app.feature.messages

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.DirectConversation
import com.libra.app.domain.model.GroupPermissions
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.UserAvatar
import com.libra.app.ui.theme.LibraLavender
import com.libra.app.ui.theme.LibraLavenderDarkSurface
import com.libra.app.ui.theme.LibraLavenderDarkText
import com.libra.app.ui.theme.LibraLavenderSurface
import com.libra.app.ui.theme.LibraLavenderText
import com.libra.app.ui.theme.LocalLibraDarkTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class NewChatStudioTab { DIRECT, CREATE_GROUP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatAndGroupStudioSheet(
    initialTab: NewChatStudioTab = NewChatStudioTab.DIRECT,
    existingConversations: List<DirectConversation>,
    onDismiss: () -> Unit,
    onDirectUser: (UserProfile) -> Unit,
    onCreateGroup: (
        name: String,
        participantIds: List<String>,
        description: String,
        photoUrl: String,
        bannerUrl: String,
        permissions: GroupPermissions,
        onDone: (Boolean) -> Unit
    ) -> Unit
) {
    val darkTheme = LocalLibraDarkTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentUid = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()

    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var suggestedUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    // Group creation fields
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var selectedMembers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var groupPermissions by remember { mutableStateOf(GroupPermissions()) }
    var avatarLocalUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedAvatarKey by remember { mutableStateOf("") }
    var uploadingAvatar by remember { mutableStateOf(false) }
    var bannerLocalUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedBannerKey by remember { mutableStateOf("") }
    var uploadingBanner by remember { mutableStateOf(false) }
    var creatingGroup by remember { mutableStateOf(false) }

    fun uploadPickedGroupImage(uri: Uri, isBanner: Boolean) {
        if (currentUid.isBlank()) return
        if (isBanner) {
            bannerLocalUri = uri
            uploadingBanner = true
        } else {
            avatarLocalUri = uri
            uploadingAvatar = true
        }
        scope.launch {
            try {
                val prepared = prepareChatImageForUpload(
                    context = context,
                    uri = uri,
                    prefix = if (isBanner) "grp_banner" else "grp_avatar"
                )
                val req = StorageUploadRequest(
                    fileName = prepared.fileName,
                    bytes = prepared.bytes,
                    contentType = prepared.contentType,
                    targetDirectory = "users/$currentUid"
                )
                when (val res = ServiceLocator.storageRepository.uploadMedia(req).first()) {
                    is AppResult.Success -> {
                        if (isBanner) uploadedBannerKey = res.data else uploadedAvatarKey = res.data
                    }
                    is AppResult.Error -> {
                        Toast.makeText(context, res.error.message, Toast.LENGTH_SHORT).show()
                        if (isBanner) bannerLocalUri = null else avatarLocalUri = null
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.localizedMessage ?: "Görsel yüklenemedi.", Toast.LENGTH_SHORT).show()
                if (isBanner) bannerLocalUri = null else avatarLocalUri = null
            } finally {
                if (isBanner) uploadingBanner = false else uploadingAvatar = false
            }
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { uploadPickedGroupImage(it, isBanner = false) }
    }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { uploadPickedGroupImage(it, isBanner = true) }
    }

    LaunchedEffect(currentUid, existingConversations) {
        val fromDm = existingConversations.mapNotNull { conv ->
            if (conv.isGroup || conv.otherUserId.isBlank()) null
            else UserProfile(
                uid = conv.otherUserId,
                displayName = conv.otherUserName,
                username = conv.otherUserUsername,
                profileImageUrl = conv.otherUserPhotoUrl
            )
        }
        val following = if (currentUid.isNotBlank()) {
            (ServiceLocator.userRepository.getFollowing(currentUid) as? AppResult.Success)?.data.orEmpty()
        } else emptyList()
        suggestedUsers = (fromDm + following)
            .filter { it.uid.isNotBlank() && it.uid != currentUid }
            .distinctBy { it.uid }
    }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            searchResults = emptyList()
        } else {
            ServiceLocator.userRepository.searchUsers(q).collect { result ->
                if (result is AppResult.Success) {
                    searchResults = result.data.filter { it.uid != currentUid }.take(25)
                }
            }
        }
    }

    val displayedUsers = remember(query, searchResults, suggestedUsers) {
        if (query.trim().length >= 2) searchResults else suggestedUsers
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            // Mode Switch Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StudioTabPill(
                        icon = Icons.Default.ChatBubbleOutline,
                        label = "Özel Mesaj",
                        selected = activeTab == NewChatStudioTab.DIRECT,
                        onClick = { activeTab = NewChatStudioTab.DIRECT },
                        modifier = Modifier.weight(1f)
                    )
                    StudioTabPill(
                        icon = Icons.Default.GroupAdd,
                        label = "Yeni Grup",
                        selected = activeTab == NewChatStudioTab.CREATE_GROUP,
                        onClick = { activeTab = NewChatStudioTab.CREATE_GROUP },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            AnimatedContent(
                targetState = activeTab,
                label = "new_chat_studio_tab"
            ) { tab ->
                when (tab) {
                    NewChatStudioTab.DIRECT -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                placeholder = { Text("İsim veya @kullaniciadi ile ara…") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                trailingIcon = if (query.isNotBlank()) {
                                    {
                                        IconButton(onClick = { query = "" }) {
                                            Icon(Icons.Default.Close, "Temizle")
                                        }
                                    }
                                } else null
                            )

                            Text(
                                text = if (query.trim().length >= 2) "Arama Sonuçları" else "Önerilen Kişiler",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 380.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (displayedUsers.isEmpty()) {
                                    item {
                                        Text(
                                            text = if (query.trim().length < 2) {
                                                "Sohbet başlatmak istediğin kişinin adını yaz."
                                            } else {
                                                "Eşleşen kullanıcı bulunamadı."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 20.dp)
                                        )
                                    }
                                } else {
                                    items(displayedUsers, key = { it.uid }) { user ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { onDirectUser(user) },
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.background,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                UserAvatar(
                                                    photoUrl = user.profileImageUrl,
                                                    initials = user.initials,
                                                    size = 44.dp
                                                )
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(horizontal = 12.dp)
                                                ) {
                                                    Text(
                                                        text = user.displayName.ifBlank { "Libra Üyesi" },
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (user.username.isNotBlank()) {
                                                        Text(
                                                            text = "@${user.username}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
                                                ) {
                                                    Text(
                                                        text = "Yaz",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = if (darkTheme) LibraLavenderDarkText else LibraLavenderText,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    NewChatStudioTab.CREATE_GROUP -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 560.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cover Banner & Squircle Avatar Studio Header
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(126.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(94.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable(enabled = !uploadingBanner) {
                                            bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (bannerLocalUri != null) {
                                            AsyncImage(
                                                model = bannerLocalUri,
                                                contentDescription = "Grup kapağı",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            LibraArchitecturalBanner(
                                                seedKey = groupName.ifBlank { "new_group" },
                                                bannerMediaKey = null,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp),
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.56f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (uploadingBanner) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(12.dp),
                                                        strokeWidth = 1.8.dp,
                                                        color = Color.White
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Default.Image,
                                                        null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                Text(
                                                    text = if (bannerLocalUri != null) "Kapağı Değiştir" else "Kapak Ekle",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }

                                // Floating Squircle Group Avatar Picker
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 14.dp)
                                        .size(66.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable(enabled = !uploadingAvatar) {
                                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.surface),
                                    shadowElevation = 3.dp
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (avatarLocalUri != null) {
                                            AsyncImage(
                                                model = avatarLocalUri,
                                                contentDescription = "Grup avatarı",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AddAPhoto,
                                                contentDescription = "Grup fotoğrafı seç",
                                                tint = if (darkTheme) LibraLavenderDarkText else LibraLavenderText,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        if (uploadingAvatar) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.45f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = groupName,
                                onValueChange = { if (it.length <= 60) groupName = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                label = { Text("Grup Adı") },
                                placeholder = { Text("Örn. Gece Okurları Kulübü") }
                            )

                            OutlinedTextField(
                                value = groupDescription,
                                onValueChange = { if (it.length <= 300) groupDescription = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 3,
                                shape = RoundedCornerShape(14.dp),
                                label = { Text("Açıklama (isteğe bağlı)") },
                                placeholder = { Text("Grubun konusu, kuralları veya amacı…") }
                            )

                            // Initial Quick Permissions
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.background,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                "Üyeler kişi ekleyebilsin",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            Text(
                                                "Kapalıysa sadece Başkan ve Yöneticiler davet edebilir",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = groupPermissions.membersCanAddOthers,
                                            onCheckedChange = {
                                                groupPermissions = groupPermissions.copy(membersCanAddOthers = it)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = LibraLavender
                                            )
                                        )
                                    }
                                }
                            }

                            // Selected Member Chips Rail
                            if (selectedMembers.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    selectedMembers.forEach { member ->
                                        Surface(
                                            shape = CircleShape,
                                            color = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface,
                                            border = BorderStroke(1.dp, LibraLavender.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .clickable {
                                                        selectedMembers = selectedMembers.filterNot { it.uid == member.uid }
                                                    }
                                                    .padding(start = 6.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                UserAvatar(
                                                    photoUrl = member.profileImageUrl,
                                                    initials = member.initials,
                                                    size = 22.dp
                                                )
                                                Text(
                                                    text = member.displayName.ifBlank { "@${member.username}" },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (darkTheme) LibraLavenderDarkText else LibraLavenderText,
                                                    maxLines = 1
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Kaldır",
                                                    tint = if (darkTheme) LibraLavenderDarkText else LibraLavenderText,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                placeholder = { Text("Katılımcı ara veya aşağıdan seç…") },
                                leadingIcon = { Icon(Icons.Default.Search, null) }
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                displayedUsers.take(12).forEach { user ->
                                    val chosen = selectedMembers.any { it.uid == user.uid }
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                selectedMembers = if (chosen) {
                                                    selectedMembers.filterNot { it.uid == user.uid }
                                                } else {
                                                    selectedMembers + user
                                                }
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (chosen) {
                                            if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
                                        } else {
                                            MaterialTheme.colorScheme.background
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (chosen) LibraLavender else MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            UserAvatar(
                                                photoUrl = user.profileImageUrl,
                                                initials = user.initials,
                                                size = 38.dp
                                            )
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 10.dp)
                                            ) {
                                                Text(
                                                    text = user.displayName.ifBlank { "Libra Üyesi" },
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (user.username.isNotBlank()) {
                                                    Text(
                                                        text = "@${user.username}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Surface(
                                                shape = CircleShape,
                                                color = if (chosen) LibraLavender else Color.Transparent,
                                                border = BorderStroke(
                                                    1.5.dp,
                                                    if (chosen) LibraLavender else MaterialTheme.colorScheme.outline
                                                ),
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                if (chosen) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            val canSubmit = groupName.trim().isNotBlank() &&
                                selectedMembers.isNotEmpty() &&
                                !uploadingAvatar &&
                                !uploadingBanner &&
                                !creatingGroup
                            Button(
                                enabled = canSubmit,
                                onClick = {
                                    creatingGroup = true
                                    onCreateGroup(
                                        groupName.trim(),
                                        selectedMembers.map { it.uid },
                                        groupDescription.trim(),
                                        uploadedAvatarKey,
                                        uploadedBannerKey,
                                        groupPermissions
                                    ) {
                                        creatingGroup = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LibraLavender)
                            ) {
                                if (creatingGroup) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        text = if (selectedMembers.isEmpty()) "En Az 1 Kişi Seç"
                                        else "Grubu Oluştur (${selectedMembers.size + 1} Kişi)",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioTabPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .appleBouncy(pressedScale = 0.96f, onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = if (selected) LibraLavender else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                ),
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
