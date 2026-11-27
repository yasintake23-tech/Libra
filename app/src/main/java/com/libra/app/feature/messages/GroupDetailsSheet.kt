package com.libra.app.feature.messages

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.DirectConversation
import com.libra.app.domain.model.GroupMemberRole
import com.libra.app.domain.model.GroupPermissions
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.canAddGroupMembers
import com.libra.app.domain.model.canEditGroupInfo
import com.libra.app.domain.model.canManageGroupSettings
import com.libra.app.domain.model.resolveGroupMemberRole
import com.libra.app.domain.model.resolveGroupOwnerId
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.UserAvatar
import com.libra.app.ui.theme.LibraLavender
import com.libra.app.ui.theme.LibraLavenderBorder
import com.libra.app.ui.theme.LibraLavenderDarkBorder
import com.libra.app.ui.theme.LibraLavenderDarkSurface
import com.libra.app.ui.theme.LibraLavenderDarkText
import com.libra.app.ui.theme.LibraLavenderSurface
import com.libra.app.ui.theme.LibraLavenderText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class GroupSheetPage { OVERVIEW, ADD_MEMBERS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsSheet(
    conversation: DirectConversation,
    currentUid: String,
    allConversations: List<DirectConversation>,
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onUpdateInfo: (name: String, description: String, onDone: (Boolean, String?) -> Unit) -> Unit,
    onUpdatePermissions: (GroupPermissions, onDone: (Boolean, String?) -> Unit) -> Unit,
    onAddMembers: (List<String>, onDone: (Boolean, String?) -> Unit) -> Unit,
    onRemoveMember: (targetUid: String, onDone: (Boolean, String?) -> Unit) -> Unit,
    onSetAdmin: (targetUid: String, isAdmin: Boolean, onDone: (Boolean, String?) -> Unit) -> Unit,
    onLeaveCompleted: () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val presidentUid = remember(conversation) { resolveGroupOwnerId(conversation) }
    val isManager = remember(conversation, currentUid) { canManageGroupSettings(conversation, currentUid) }
    val canEditInfo = remember(conversation, currentUid) { canEditGroupInfo(conversation, currentUid) }
    val canAddMembers = remember(conversation, currentUid) { canAddGroupMembers(conversation, currentUid) }

    var page by remember { mutableStateOf(GroupSheetPage.OVERVIEW) }
    var isEditingInfo by remember { mutableStateOf(false) }
    var nameDraft by remember(conversation.groupName) { mutableStateOf(conversation.groupName) }
    var descDraft by remember(conversation.groupDescription) { mutableStateOf(conversation.groupDescription) }
    var savingInfo by remember { mutableStateOf(false) }

    var permissionsDraft by remember(conversation.groupPermissions) {
        mutableStateOf(conversation.groupPermissions)
    }
    var savingPerms by remember { mutableStateOf(false) }

    var memberSearch by remember { mutableStateOf("") }
    var confirmKickMember by remember { mutableStateOf<UserProfile?>(null) }
    var confirmLeaveGroup by remember { mutableStateOf(false) }
    var actionBusy by remember { mutableStateOf(false) }

    val memberProfiles = remember { mutableStateMapOf<String, UserProfile>() }
    var loadingProfiles by remember { mutableStateOf(false) }

    LaunchedEffect(conversation.participantIds) {
        val missing = conversation.participantIds.filter { it.isNotBlank() && !memberProfiles.containsKey(it) }
        if (missing.isEmpty()) return@LaunchedEffect
        loadingProfiles = true
        runCatching {
            coroutineScope {
                missing.map { uid ->
                    async {
                        val loaded = (ServiceLocator.userRepository.getUserProfileFresh(uid) as? AppResult.Success)?.data
                        uid to (loaded ?: UserProfile(uid = uid, displayName = "Libra Üyesi"))
                    }
                }.awaitAll()
            }
        }.onSuccess { pairs ->
            pairs.forEach { (uid, profile) -> memberProfiles[uid] = profile }
        }
        loadingProfiles = false
    }

    val sortedMemberUids = remember(conversation.participantIds, presidentUid, conversation.adminIds, memberProfiles.size) {
        val roleRank: (String) -> Int = { uid ->
            when (resolveGroupMemberRole(conversation, uid)) {
                GroupMemberRole.PRESIDENT -> 0
                GroupMemberRole.ADMIN -> 1
                GroupMemberRole.MEMBER -> 2
            }
        }
        conversation.participantIds.distinct().sortedWith(
            compareBy<String>({ roleRank(it) }, {
                memberProfiles[it]?.displayName?.lowercase(Locale.getDefault()).orEmpty()
            })
        )
    }

    val filteredMemberUids = remember(sortedMemberUids, memberSearch, memberProfiles.size) {
        val q = memberSearch.trim()
        if (q.isBlank()) sortedMemberUids
        else sortedMemberUids.filter { uid ->
            val p = memberProfiles[uid]
            uid.contains(q, ignoreCase = true) ||
                p?.displayName?.contains(q, ignoreCase = true) == true ||
                p?.username?.contains(q, ignoreCase = true) == true
        }
    }

    val presidentProfile = memberProfiles[presidentUid]
        ?: UserProfile(uid = presidentUid, displayName = "Grup Başkanı")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        AnimatedContent(
            targetState = page,
            label = "group_details_page"
        ) { currentPage ->
            when (currentPage) {
                GroupSheetPage.OVERVIEW -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Top Group Hero Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    UserAvatar(
                                        photoUrl = conversation.groupPhotoUrl,
                                        initials = conversation.groupName.take(1).uppercase().ifBlank { "G" },
                                        size = 72.dp
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = LibraLavender,
                                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.background),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Groups,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                if (isEditingInfo) {
                                    OutlinedTextField(
                                        value = nameDraft,
                                        onValueChange = { if (it.length <= 60) nameDraft = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        label = { Text("Grup Adı") },
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = descDraft,
                                        onValueChange = { if (it.length <= 300) descDraft = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        maxLines = 4,
                                        label = { Text("Grup Açıklaması") },
                                        placeholder = { Text("Grubun amacı, kuralları...") },
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                                    ) {
                                        TextButton(
                                            enabled = !savingInfo,
                                            onClick = {
                                                nameDraft = conversation.groupName
                                                descDraft = conversation.groupDescription
                                                isEditingInfo = false
                                            }
                                        ) { Text("Vazgeç") }
                                        Button(
                                            enabled = !savingInfo && nameDraft.trim().isNotBlank(),
                                            colors = ButtonDefaults.buttonColors(containerColor = LibraLavender),
                                            shape = RoundedCornerShape(12.dp),
                                            onClick = {
                                                savingInfo = true
                                                onUpdateInfo(nameDraft.trim(), descDraft.trim()) { ok, err ->
                                                    savingInfo = false
                                                    if (ok) {
                                                        isEditingInfo = false
                                                        Toast.makeText(context, "Grup bilgileri güncellendi.", Toast.LENGTH_SHORT).show()
                                                    } else if (!err.isNullOrBlank()) {
                                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        ) {
                                            if (savingInfo) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color.White
                                                )
                                            } else {
                                                Text("Kaydet", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = conversation.groupName.ifBlank { "Grup sohbeti" },
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (canEditInfo) {
                                            Surface(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .appleBouncy(pressedScale = 0.88f, onClick = { isEditingInfo = true }),
                                                shape = CircleShape,
                                                color = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Grup bilgilerini düzenle",
                                                        tint = if (darkTheme) LibraLavenderDarkText else LibraLavenderText,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = conversation.groupDescription.ifBlank {
                                            if (canEditInfo) "Grup açıklaması eklemek için düzenle simgesine dokun."
                                            else "Grup açıklaması henüz eklenmemiş."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(Modifier.height(10.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface,
                                            border = BorderStroke(
                                                1.dp,
                                                if (darkTheme) LibraLavenderDarkBorder else LibraLavenderBorder
                                            )
                                        ) {
                                            Text(
                                                text = "${conversation.participantIds.size} Katılımcı",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (darkTheme) LibraLavenderDarkText else LibraLavenderText,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        if (conversation.createdAt > 0L) {
                                            val formatted = remember(conversation.createdAt) {
                                                SimpleDateFormat("d MMM yyyy", Locale("tr")).format(Date(conversation.createdAt))
                                            }
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "Kuruluş: $formatted",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Group President Spotlight Card
                        if (presidentUid.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Grup Başkanı",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            onDismiss()
                                            onOpenProfile(presidentUid)
                                        },
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (darkTheme) LibraLavenderDarkSurface.copy(alpha = 0.45f)
                                    else LibraLavenderSurface.copy(alpha = 0.55f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (darkTheme) LibraLavenderDarkBorder else LibraLavenderBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        UserAvatar(
                                            photoUrl = presidentProfile.profileImageUrl,
                                            initials = presidentProfile.initials,
                                            size = 44.dp
                                        )
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = presidentProfile.displayName.ifBlank { "Libra Üyesi" } +
                                                        if (presidentUid == currentUid) " (Sen)" else "",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = if (presidentProfile.username.isNotBlank()) {
                                                    "@${presidentProfile.username} • Kurucu & Başkan"
                                                } else {
                                                    "Grup Başkanı & Tam Yetkili"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                                            )
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = LibraLavender
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Başkan",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    ),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Group Permissions & Admin Controls
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Grup Ayarları ve İzinleri",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
                                if (!isManager) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Yalnızca yöneticiler",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.background,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column {
                                    val applyPermissions: (GroupPermissions) -> Unit = { updated ->
                                        val previous = permissionsDraft
                                        permissionsDraft = updated
                                        savingPerms = true
                                        onUpdatePermissions(updated) { ok, err ->
                                            savingPerms = false
                                            if (!ok) {
                                                permissionsDraft = previous
                                                Toast.makeText(
                                                    context,
                                                    err ?: "İzinler güncellenemedi.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }

                                    PermissionToggleRow(
                                        title = "Sadece Yöneticiler Mesaj Göndersin",
                                        subtitle = "Açıkken normal üyeler yalnızca sohbeti okuyabilir.",
                                        checked = permissionsDraft.onlyAdminsCanMessage,
                                        enabled = isManager && !savingPerms,
                                        onCheckedChange = {
                                            applyPermissions(permissionsDraft.copy(onlyAdminsCanMessage = it))
                                        }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                                    PermissionToggleRow(
                                        title = "Üyeler Kişi Ekleyebilsin",
                                        subtitle = "Üyelerin gruba yeni katılımcı davet etmesine izin ver.",
                                        checked = permissionsDraft.membersCanAddOthers,
                                        enabled = isManager && !savingPerms,
                                        onCheckedChange = {
                                            applyPermissions(permissionsDraft.copy(membersCanAddOthers = it))
                                        }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                                    PermissionToggleRow(
                                        title = "Üyeler Grup Bilgisini Düzenleyebilsin",
                                        subtitle = "Grup adı ve açıklamasını değiştirebilme izni.",
                                        checked = permissionsDraft.membersCanEditInfo,
                                        enabled = isManager && !savingPerms,
                                        onCheckedChange = {
                                            applyPermissions(permissionsDraft.copy(membersCanEditInfo = it))
                                        }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                                    PermissionToggleRow(
                                        title = "Üyeler Medya Paylaşabilsin",
                                        subtitle = "Kapalıysa yalnızca Başkan ve Yöneticiler fotoğraf atabilir.",
                                        checked = permissionsDraft.membersCanSendMedia,
                                        enabled = isManager && !savingPerms,
                                        onCheckedChange = {
                                            applyPermissions(permissionsDraft.copy(membersCanSendMedia = it))
                                        }
                                    )
                                }
                            }
                        }

                        // Participants ("Kimler Var / Kişi Ekle") Section
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Katılımcılar (${sortedMemberUids.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
                                if (canAddMembers) {
                                    Surface(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .appleBouncy(pressedScale = 0.92f, onClick = { page = GroupSheetPage.ADD_MEMBERS }),
                                        shape = CircleShape,
                                        color = LibraLavender
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PersonAdd,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "Kişi Ekle",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            if (sortedMemberUids.size > 5) {
                                OutlinedTextField(
                                    value = memberSearch,
                                    onValueChange = { memberSearch = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    placeholder = { Text("Katılımcılarda ara...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    trailingIcon = if (memberSearch.isNotBlank()) {
                                        {
                                            IconButton(onClick = { memberSearch = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Temizle")
                                            }
                                        }
                                    } else null
                                )
                            }

                            if (loadingProfiles && memberProfiles.isEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Katılımcılar yükleniyor...", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.background,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column {
                                    filteredMemberUids.forEachIndexed { index, uid ->
                                        val profile = memberProfiles[uid] ?: UserProfile(
                                            uid = uid,
                                            displayName = "Libra Üyesi"
                                        )
                                        val role = resolveGroupMemberRole(conversation, uid)
                                        val isSelf = uid == currentUid
                                        var showMemberMenu by remember(uid) { mutableStateOf(false) }

                                        val canToggleAdmin = currentUid == presidentUid && !isSelf
                                        val canKickTarget = !isSelf &&
                                            role != GroupMemberRole.PRESIDENT &&
                                            isManager &&
                                            (role != GroupMemberRole.ADMIN || currentUid == presidentUid)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onDismiss()
                                                    onOpenProfile(uid)
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            UserAvatar(
                                                photoUrl = profile.profileImageUrl,
                                                initials = profile.initials,
                                                size = 42.dp
                                            )
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 12.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = profile.displayName.ifBlank { "Libra Üyesi" } +
                                                            if (isSelf) " (Sen)" else "",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    when (role) {
                                                        GroupMemberRole.PRESIDENT -> {
                                                            MemberRoleBadge(
                                                                label = "Başkan",
                                                                filled = true,
                                                                darkTheme = darkTheme
                                                            )
                                                        }
                                                        GroupMemberRole.ADMIN -> {
                                                            MemberRoleBadge(
                                                                label = "Yönetici",
                                                                filled = false,
                                                                darkTheme = darkTheme
                                                            )
                                                        }
                                                        GroupMemberRole.MEMBER -> Unit
                                                    }
                                                }
                                                if (profile.username.isNotBlank()) {
                                                    Text(
                                                        text = "@${profile.username}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            if (!isSelf && (canToggleAdmin || canKickTarget)) {
                                                Box {
                                                    IconButton(
                                                        enabled = !actionBusy,
                                                        onClick = { showMemberMenu = true }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.MoreVert,
                                                            contentDescription = "Üye seçenekleri",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    DropdownMenu(
                                                        expanded = showMemberMenu,
                                                        onDismissRequest = { showMemberMenu = false },
                                                        containerColor = MaterialTheme.colorScheme.surface
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text("Profili Gör") },
                                                            leadingIcon = { Icon(Icons.Default.Person, null) },
                                                            onClick = {
                                                                showMemberMenu = false
                                                                onDismiss()
                                                                onOpenProfile(uid)
                                                            }
                                                        )
                                                        if (canToggleAdmin) {
                                                            val makeAdmin = role != GroupMemberRole.ADMIN
                                                            DropdownMenuItem(
                                                                text = {
                                                                    Text(if (makeAdmin) "Yönetici Yap" else "Yöneticilikten Çıkar")
                                                                },
                                                                leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null) },
                                                                onClick = {
                                                                    showMemberMenu = false
                                                                    actionBusy = true
                                                                    onSetAdmin(uid, makeAdmin) { ok, err ->
                                                                        actionBusy = false
                                                                        val msg = if (ok) {
                                                                            if (makeAdmin) "${profile.displayName} yönetici yapıldı."
                                                                            else "${profile.displayName} yöneticilikten çıkarıldı."
                                                                        } else err ?: "İşlem tamamlanamadı."
                                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            )
                                                        }
                                                        if (canKickTarget) {
                                                            DropdownMenuItem(
                                                                text = {
                                                                    Text("Gruptan Çıkar", color = MaterialTheme.colorScheme.error)
                                                                },
                                                                leadingIcon = {
                                                                    Icon(
                                                                        Icons.Default.PersonRemove,
                                                                        null,
                                                                        tint = MaterialTheme.colorScheme.error
                                                                    )
                                                                },
                                                                onClick = {
                                                                    showMemberMenu = false
                                                                    confirmKickMember = profile
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            } else if (!isSelf) {
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        if (index < filteredMemberUids.lastIndex) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                                                modifier = Modifier.padding(start = 68.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Leave Group Button
                        OutlinedButton(
                            enabled = !actionBusy,
                            onClick = { confirmLeaveGroup = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Gruptan Ayrıl", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                GroupSheetPage.ADD_MEMBERS -> {
                    AddGroupMembersPage(
                        conversation = conversation,
                        currentUid = currentUid,
                        allConversations = allConversations,
                        onBack = { page = GroupSheetPage.OVERVIEW },
                        onConfirmAdd = { selectedIds, done ->
                            onAddMembers(selectedIds) { ok, err ->
                                if (ok) {
                                    Toast.makeText(
                                        context,
                                        "${selectedIds.size} kişi gruba eklendi.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    done(true)
                                    page = GroupSheetPage.OVERVIEW
                                } else {
                                    Toast.makeText(
                                        context,
                                        err ?: "Kişiler eklenemedi.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    done(false)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    confirmKickMember?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmKickMember = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Üyeyi Gruptan Çıkar", fontWeight = FontWeight.Bold) },
            text = {
                Text("${target.displayName.ifBlank { "Bu kullanıcı" }} gruptan çıkarılacak ve artık bu sohbete mesaj gönderemeyecek.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        confirmKickMember = null
                        actionBusy = true
                        onRemoveMember(target.uid) { ok, err ->
                            actionBusy = false
                            val msg = if (ok) "${target.displayName} gruptan çıkarıldı."
                            else err ?: "Üye çıkarılamadı."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Çıkar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmKickMember = null }) { Text("Vazgeç") }
            }
        )
    }

    if (confirmLeaveGroup) {
        val isPresident = currentUid == presidentUid
        AlertDialog(
            onDismissRequest = { confirmLeaveGroup = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Gruptan Ayrıl", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isPresident && sortedMemberUids.size > 1) {
                        "Grup Başkanı olduğun için ayrıldığında başkanlık sıradaki yöneticiye veya üyeye devredilecek."
                    } else {
                        "\"${conversation.groupName.ifBlank { "Bu gruptan" }}\" ayrılmak istediğine emin misin?"
                    }
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        confirmLeaveGroup = false
                        actionBusy = true
                        onRemoveMember(currentUid) { ok, err ->
                            actionBusy = false
                            if (ok) {
                                Toast.makeText(context, "Gruptan ayrıldın.", Toast.LENGTH_SHORT).show()
                                onDismiss()
                                onLeaveCompleted()
                            } else {
                                Toast.makeText(
                                    context,
                                    err ?: "Gruptan ayrılınamadı.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) { Text("Ayrıl") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeaveGroup = false }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun PermissionToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LibraLavender,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun MemberRoleBadge(
    label: String,
    filled: Boolean,
    darkTheme: Boolean
) {
    Surface(
        shape = CircleShape,
        color = when {
            filled -> LibraLavender
            darkTheme -> LibraLavenderDarkSurface
            else -> LibraLavenderSurface
        },
        border = if (!filled) {
            BorderStroke(1.dp, if (darkTheme) LibraLavenderDarkBorder else LibraLavenderBorder)
        } else null
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = when {
                filled -> Color.White
                darkTheme -> LibraLavenderDarkText
                else -> LibraLavenderText
            },
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun AddGroupMembersPage(
    conversation: DirectConversation,
    currentUid: String,
    allConversations: List<DirectConversation>,
    onBack: () -> Unit,
    onConfirmAdd: (List<String>, (Boolean) -> Unit) -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var suggestedUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selectedUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var adding by remember { mutableStateOf(false) }

    val existingMemberSet = remember(conversation.participantIds) { conversation.participantIds.toSet() }

    LaunchedEffect(currentUid, allConversations) {
        val fromDm = allConversations.mapNotNull { conv ->
            if (conv.isGroup || conv.otherUserId.isBlank()) null
            else UserProfile(
                uid = conv.otherUserId,
                displayName = conv.otherUserName,
                username = conv.otherUserUsername,
                profileImageUrl = conv.otherUserPhotoUrl
            )
        }
        val following = (ServiceLocator.userRepository.getFollowing(currentUid) as? AppResult.Success)
            ?.data.orEmpty()
        suggestedUsers = (fromDm + following)
            .filter { it.uid.isNotBlank() && it.uid != currentUid }
            .distinctBy { it.uid }
    }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            searchResults = emptyList()
        } else {
            ServiceLocator.userRepository.searchUsers(q).collect { res ->
                if (res is AppResult.Success) {
                    searchResults = res.data.filter { it.uid != currentUid }.take(25)
                }
            }
        }
    }

    val displayedUsers = remember(query, searchResults, suggestedUsers) {
        if (query.trim().length >= 2) searchResults else suggestedUsers
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .navigationBarsPadding()
            .padding(bottom = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gruba Kişi Ekle",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Grupta olanlar ve ekleyebileceğin kişiler",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("İsim veya @kullaniciadi ile ara...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (query.isNotBlank()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Temizle")
                    }
                }
            } else null
        )

        if (selectedUsers.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Eklenecek (${selectedUsers.size}): " +
                    selectedUsers.joinToString(", ") { it.displayName.ifBlank { "@${it.username}" } },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (darkTheme) LibraLavenderDarkText else LibraLavenderText
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            if (displayedUsers.isEmpty()) {
                item {
                    Text(
                        text = if (query.trim().length < 2) {
                            "Eklemek istediğin kişinin adını veya kullanıcı adını yaz."
                        } else {
                            "Aramana uygun kullanıcı bulunamadı."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
            } else {
                items(displayedUsers, key = { it.uid }) { user ->
                    val alreadyInGroup = user.uid in existingMemberSet
                    val isSelected = selectedUsers.any { it.uid == user.uid }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = !alreadyInGroup && !adding) {
                                selectedUsers = if (isSelected) {
                                    selectedUsers.filterNot { it.uid == user.uid }
                                } else {
                                    selectedUsers + user
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            isSelected -> if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
                            alreadyInGroup -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.background
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) LibraLavender else MaterialTheme.colorScheme.outline
                        )
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
                                size = 42.dp
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
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
                            when {
                                alreadyInGroup -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "Grupta",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                isSelected -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = LibraLavender,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Seçili",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Transparent,
                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.size(26.dp)
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            enabled = selectedUsers.isNotEmpty() && !adding,
            onClick = {
                adding = true
                onConfirmAdd(selectedUsers.map { it.uid }) { ok ->
                    adding = false
                    if (ok) selectedUsers = emptyList()
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LibraLavender)
        ) {
            if (adding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text(
                    text = if (selectedUsers.isEmpty()) "Kişi Seç" else "Gruba Ekle (${selectedUsers.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
