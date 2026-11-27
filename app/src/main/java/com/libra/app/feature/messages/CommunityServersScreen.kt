package com.libra.app.feature.messages

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.CommunityServer
import com.libra.app.domain.model.ReactionUser
import com.libra.app.domain.model.ServerCategory
import com.libra.app.domain.model.ServerChannel
import com.libra.app.domain.model.ServerChannelPermissionOverride
import com.libra.app.domain.model.ServerMember
import com.libra.app.domain.model.ServerMessage
import com.libra.app.domain.model.ServerRoleDefinition
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.FullScreenImageViewerDialog
import com.libra.app.ui.components.UserAvatar
import com.libra.app.ui.theme.LibraLavender
import com.libra.app.ui.theme.LibraLavenderBorder
import com.libra.app.ui.theme.LibraLavenderDarkBorder
import com.libra.app.ui.theme.LibraLavenderDarkSurface
import com.libra.app.ui.theme.LibraLavenderDarkText
import com.libra.app.ui.theme.LibraLavenderSurface
import com.libra.app.ui.theme.LibraLavenderText
import com.libra.app.ui.theme.LocalLibraDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

private val ServerIndigoAccent = Color(0xFF4F46E5)
private val ServerUnreadDotColor = Color(0xFFF43F5E)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CommunityServersScreen(
    onBack: () -> Unit = {},
    onSwitchToMessages: (Offset) -> Unit = { onBack() },
    unreadPrivateCount: Int = 0,
    onOpenProfile: (String) -> Unit = {},
    hubListState: LazyListState = rememberLazyListState(),
    viewModel: CommunityServersViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val darkTheme = LocalLibraDarkTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUid = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()

    LaunchedEffect(context) {
        viewModel.bindPreferences(context)
    }

    val allServers by viewModel.allServers.collectAsState()
    val joinedServers by viewModel.joinedServers.collectAsState()
    val pinnedServerIds by viewModel.pinnedServerIds.collectAsState()
    val isLoadingServers by viewModel.isLoadingServers.collectAsState()
    val serversError by viewModel.serversError.collectAsState()
    val selectedServerId by viewModel.selectedServerId.collectAsState()
    val selectedChannelId by viewModel.selectedChannelId.collectAsState()
    val isLoadingWorkspace by viewModel.isLoadingWorkspace.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val members by viewModel.members.collectAsState()
    val roles by viewModel.roles.collectAsState()
    val channelPermissions by viewModel.channelPermissions.collectAsState()
    val activeInviteKey by viewModel.activeInviteKey.collectAsState()
    val latestByChannel by viewModel.latestMessageByChannel.collectAsState()
    val readAtByChannel by viewModel.readAtByChannel.collectAsState()

    val sortedServers = remember(joinedServers, pinnedServerIds) {
        joinedServers.distinctBy { it.id }
            .sortedWith(
                compareByDescending<CommunityServer> { pinnedServerIds.contains(it.id) }
                    .thenByDescending { it.createdAt }
            )
    }

    // Live derived server object (prevents stale snapshots after name/avatar/banner edits!)
    val selectedServer = remember(selectedServerId, allServers, joinedServers) {
        val sid = selectedServerId ?: return@remember null
        allServers.firstOrNull { it.id == sid } ?: joinedServers.firstOrNull { it.id == sid }
    }
    val selectedChannel = remember(selectedChannelId, channels) {
        val cid = selectedChannelId ?: return@remember null
        channels.firstOrNull { it.id == cid }
    }

    var showCreateSheet by remember { mutableStateOf(false) }
    var showJoinSheet by remember { mutableStateOf(false) }
    var inviteKeyInput by remember { mutableStateOf("") }
    var inviteLookupLoading by remember { mutableStateOf(false) }
    var previewServer by remember { mutableStateOf<CommunityServer?>(null) }
    var previewIsMember by remember { mutableStateOf(false) }
    var enteringServer by remember { mutableStateOf(false) }
    var inlineError by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedServer != null || selectedChannel != null) {
        if (selectedChannel != null) {
            viewModel.selectChannel(null)
        } else {
            viewModel.selectServer(null)
        }
    }

    // Full-screen in-channel messaging view
    if (selectedServer != null && selectedChannel != null) {
        val isOwner = currentUid == selectedServer.ownerId
        val currentMember = members.firstOrNull { it.uid == currentUid }
        val currentRolePermissions = roles.firstOrNull { it.id == currentMember?.role }?.permissions.orEmpty()
        val visibleChannels = remember(channels, channelPermissions, currentMember, currentRolePermissions, isOwner) {
            channels.filter { ch ->
                isOwner || canViewServerChannel(ch, channelPermissions[ch.id].orEmpty(), currentMember, currentRolePermissions)
            }
        }
        ServerChatScreen(
            server = selectedServer,
            channel = selectedChannel,
            visibleChannels = visibleChannels,
            categories = categories,
            members = members,
            serverRoles = roles,
            inviteKey = activeInviteKey.ifBlank { selectedServer.inviteKey },
            latestByChannel = latestByChannel,
            readAtByChannel = readAtByChannel,
            onBack = { viewModel.selectChannel(null) },
            onSwitchChannel = { viewModel.selectChannel(it.id) },
            onMarkChannelRead = { ts -> viewModel.markChannelRead(selectedServer.id, selectedChannel.id, ts) },
            onServerDeleted = { viewModel.onServerDeleted(selectedServer.id) },
            onLeaveServer = {
                viewModel.leaveServer(selectedServer.id) { ok, err ->
                    if (ok) Toast.makeText(context, "Sunucudan ayrıldın.", Toast.LENGTH_SHORT).show()
                    else if (!err.isNullOrBlank()) Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            },
            onOpenProfile = onOpenProfile,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    val activeUnreadChannelsCount = remember(latestByChannel, readAtByChannel, currentUid) {
        countUnreadServerChannels(latestByChannel, readAtByChannel, currentUid)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Persistent Realm Top Bar with Reverse Mechanical Portal Button ("Mesajlar")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MechanicalPortalTriggerButton(
                title = "Mesajlar",
                isServerDestination = false,
                badgeCount = unreadPrivateCount,
                compact = true,
                onTrigger = onSwitchToMessages
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedServer?.name ?: "Sunucu Atlası",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (selectedServer != null) {
                        if (activeUnreadChannelsCount > 0) "$activeUnreadChannelsCount okunmamış kanal"
                        else "${members.size} üye • ${channels.size} kanal"
                    } else {
                        if (sortedServers.isEmpty()) "Topluluk alanların"
                        else "${sortedServers.size} sunucu aktif"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedServer != null && activeUnreadChannelsCount > 0) ServerUnreadDotColor
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            ServerCircleAction(
                icon = Icons.Default.VpnKey,
                contentDescription = "Anahtarla katıl",
                onClick = { showJoinSheet = true }
            )
            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .appleBouncy(pressedScale = 0.88f, onClick = { showCreateSheet = true }),
                shape = CircleShape,
                color = ServerIndigoAccent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Sunucu oluştur",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Libra Signature Horizontal Constellation Server Switcher Ribbon
        ConstellationServerRibbon(
            servers = sortedServers,
            pinnedIds = pinnedServerIds,
            selectedServerId = selectedServer?.id,
            onSelectHub = { viewModel.selectServer(null) },
            onSelectServer = { viewModel.selectServer(it.id) }
        )

        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))

        // Main Realm Stage: Either Server Atlas Home or Selected Server Workspace
        AnimatedContent(
            targetState = selectedServer,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "server_hub_workspace_stage",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { activeServer ->
            if (activeServer == null) {
                ServerAtlasHomeContent(
                    servers = sortedServers,
                    pinnedServerIds = pinnedServerIds,
                    currentUid = currentUid,
                    isLoading = isLoadingServers,
                    error = inlineError ?: serversError,
                    inviteKeyInput = inviteKeyInput,
                    inviteLookupLoading = inviteLookupLoading,
                    listState = hubListState,
                    onInviteKeyChange = { inviteKeyInput = it.take(40) },
                    onLookupInvite = {
                        if (inviteKeyInput.isBlank() || inviteLookupLoading) return@ServerAtlasHomeContent
                        inviteLookupLoading = true
                        inlineError = null
                        viewModel.lookupByInviteKey(inviteKeyInput) { found, isMem, err ->
                            inviteLookupLoading = false
                            if (found != null) {
                                previewIsMember = isMem
                                previewServer = found
                            } else {
                                inlineError = err ?: "Sunucu bulunamadı."
                            }
                        }
                    },
                    onRefresh = {
                        inlineError = null
                        viewModel.refreshServers()
                    },
                    onTogglePin = { viewModel.togglePinServer(it) },
                    onSelectServer = { viewModel.selectServer(it.id) },
                    onCreateServer = { showCreateSheet = true }
                )
            } else {
                ServerWorkspaceContent(
                    server = activeServer,
                    currentUid = currentUid,
                    isLoading = isLoadingWorkspace,
                    categories = categories,
                    channels = channels,
                    members = members,
                    serverRoles = roles,
                    channelPermissions = channelPermissions,
                    inviteKey = activeInviteKey.ifBlank { activeServer.inviteKey },
                    latestByChannel = latestByChannel,
                    readAtByChannel = readAtByChannel,
                    onChannelClick = { viewModel.selectChannel(it.id) },
                    onOpenProfile = onOpenProfile,
                    onServerDeleted = { viewModel.onServerDeleted(activeServer.id) },
                    onLeaveServer = {
                        viewModel.leaveServer(activeServer.id) { ok, err ->
                            if (ok) Toast.makeText(context, "Sunucudan ayrıldın.", Toast.LENGTH_SHORT).show()
                            else if (!err.isNullOrBlank()) Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    // Join by Invite Key Bottom Sheet (accessible anytime from top bar key icon)
    if (showJoinSheet) {
        var modalKey by remember { mutableStateOf(inviteKeyInput) }
        var modalBusy by remember { mutableStateOf(false) }
        var modalErr by remember { mutableStateOf<String?>(null) }
        ModalBottomSheet(
            onDismissRequest = { showJoinSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Davet Anahtarıyla Katıl",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Bir sunucudan aldığın `libra.sc/...` bağlantısını veya kısa davet kodunu gir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = modalKey,
                    onValueChange = { modalKey = it.take(40); modalErr = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("libra.sc/AbCd1234") },
                    label = { Text("Davet Anahtarı") },
                    leadingIcon = { Icon(Icons.Default.VpnKey, null) }
                )
                modalErr?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    enabled = modalKey.isNotBlank() && !modalBusy,
                    onClick = {
                        modalBusy = true
                        modalErr = null
                        viewModel.lookupByInviteKey(modalKey) { found, isMem, err ->
                            modalBusy = false
                            if (found != null) {
                                inviteKeyInput = modalKey
                                showJoinSheet = false
                                previewIsMember = isMem
                                previewServer = found
                            } else {
                                modalErr = err ?: "Bu anahtara ait sunucu bulunamadı."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ServerIndigoAccent)
                ) {
                    if (modalBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Sunucuyu Bul", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    // Server Profile Preview & Join Sheet
    previewServer?.let { server ->
        ServerProfilePreviewSheet(
            server = server,
            isMember = previewIsMember,
            entering = enteringServer,
            onDismiss = { if (!enteringServer) previewServer = null },
            onEnter = {
                if (enteringServer) return@ServerProfilePreviewSheet
                enteringServer = true
                viewModel.joinOrEnterServer(server, previewIsMember) { ok, err ->
                    enteringServer = false
                    if (ok) {
                        previewServer = null
                        inlineError = null
                        inviteKeyInput = ""
                        Toast.makeText(
                            context,
                            if (previewIsMember) "${server.name} açıldı." else "${server.name} sunucusuna katıldın!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        inlineError = err ?: "Sunucuya katılınamadı."
                    }
                }
            }
        )
    }

    // Create Server Studio Sheet (supports Name, Description, Optional Avatar & Banner!)
    if (showCreateSheet) {
        CreateServerStudioSheet(
            onDismiss = { showCreateSheet = false },
            onCreate = { name, description, avatarUpload, bannerUpload, onDone ->
                viewModel.createServer(name, description, avatarUpload, bannerUpload) { created, err ->
                    onDone(created != null, err)
                    if (created != null) {
                        showCreateSheet = false
                        Toast.makeText(context, "${created.name} oluşturuldu!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun ServerCircleAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .appleBouncy(pressedScale = 0.88f, onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ConstellationServerRibbon(
    servers: List<CommunityServer>,
    pinnedIds: Set<String>,
    selectedServerId: String?,
    onSelectHub: () -> Unit,
    onSelectServer: (CommunityServer) -> Unit
) {
    val darkTheme = LocalLibraDarkTheme.current
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item(key = "atlas_hub_pill") {
            val isHub = selectedServerId == null
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(56.dp)
                    .clickable(onClick = onSelectHub)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isHub) ServerIndigoAccent else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isHub) 2.dp else 1.dp,
                        color = if (isHub) Color(0xFFA5B4FC) else MaterialTheme.colorScheme.outline
                    ),
                    shadowElevation = if (isHub) 3.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Sunucu Atlası",
                            tint = if (isHub) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Atlas",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isHub) FontWeight.ExtraBold else FontWeight.Medium,
                        fontSize = 10.5.sp
                    ),
                    color = if (isHub) {
                        if (darkTheme) Color(0xFFA5B4FC) else ServerIndigoAccent
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        items(servers, key = { it.id }) { server ->
            val selected = server.id == selectedServerId
            val pinned = server.id in pinnedIds
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(58.dp)
                    .clickable { onSelectServer(server) }
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    LibraSquircleAvatar(
                        photoUrl = server.avatarUrl,
                        title = server.name,
                        size = 48.dp,
                        cornerRadius = if (selected) 15.dp else 18.dp,
                        borderColor = if (selected) ServerIndigoAccent else MaterialTheme.colorScheme.outline,
                        borderWidth = if (selected) 2.4.dp else 1.dp,
                        onClick = { onSelectServer(server) }
                    )
                    if (pinned) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF59E0B),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.background),
                            modifier = Modifier.size(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Sabitli",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                        fontSize = 10.5.sp
                    ),
                    color = if (selected) {
                        if (darkTheme) Color(0xFFA5B4FC) else ServerIndigoAccent
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ServerAtlasHomeContent(
    servers: List<CommunityServer>,
    pinnedServerIds: Set<String>,
    currentUid: String,
    isLoading: Boolean,
    error: String?,
    inviteKeyInput: String,
    inviteLookupLoading: Boolean,
    listState: LazyListState,
    onInviteKeyChange: (String) -> Unit,
    onLookupInvite: () -> Unit,
    onRefresh: () -> Unit,
    onTogglePin: (String) -> Unit,
    onSelectServer: (CommunityServer) -> Unit,
    onCreateServer: () -> Unit
) {
    val darkTheme = LocalLibraDarkTheme.current
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Architectural Invite Key Portal Card
        item(key = "invite_portal_card") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = if (darkTheme) Color(0xFF181728) else Color(0xFF1A192E),
                border = BorderStroke(1.dp, ServerIndigoAccent.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ServerIndigoAccent.copy(alpha = 0.35f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = Color(0xFFC7D2FE),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Bir Topluluğa Katıl",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                            Text(
                                text = "Davet anahtarını yaz ve sunucuyu anında keşfet",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC7D2FE)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                        ) {
                            TextField(
                                value = inviteKeyInput,
                                onValueChange = onInviteKeyChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                placeholder = {
                                    Text(
                                        text = "libra.sc/anahtar",
                                        color = Color.White.copy(alpha = 0.55f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = Color(0xFFF59E0B)
                                )
                            )
                        }
                        val canLookup = inviteKeyInput.isNotBlank() && !inviteLookupLoading
                        Button(
                            enabled = canLookup,
                            onClick = onLookupInvite,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ServerIndigoAccent,
                                disabledContainerColor = Color.White.copy(alpha = 0.14f)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            if (inviteLookupLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text("Bul", color = Color.White, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        // Error Recovery Banner
        if (!error.isNullOrBlank()) {
            item(key = "servers_error_banner") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.WifiOff, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRefresh) {
                            Text("Yenile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }

        item(key = "section_joined_servers") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Katıldığın Sunucular",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.weight(1f)
                )
                if (servers.isNotEmpty()) {
                    Text(
                        text = "${servers.size} sunucu",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isLoading && servers.isEmpty()) {
            items(3) {
                val block = if (darkTheme) Color(0xFF22212E) else Color(0xFFECE9F5)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = block.copy(alpha = 0.7f)
                ) {}
            }
        } else if (servers.isEmpty()) {
            item(key = "servers_empty_card") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (darkTheme) Color(0xFF1F1D36) else Color(0xFFEEF2FF),
                            modifier = Modifier.size(58.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = if (darkTheme) Color(0xFFA5B4FC) else ServerIndigoAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Henüz bir sunucun yok",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Yukarıdan davet anahtarıyla mevcut bir topluluğa katılabilir veya kendi sunucunu kurabilirsin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onCreateServer,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = ServerIndigoAccent)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Kendi Sunucunu Kur", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(servers, key = { it.id }) { server ->
                val isPinned = server.id in pinnedServerIds
                val isOwner = server.ownerId == currentUid
                ServerAtlasCard(
                    server = server,
                    isPinned = isPinned,
                    isOwner = isOwner,
                    onTogglePin = { onTogglePin(server.id) },
                    onClick = { onSelectServer(server) }
                )
            }
        }
    }
}

@Composable
private fun ServerAtlasCard(
    server: CommunityServer,
    isPinned: Boolean,
    isOwner: Boolean,
    onTogglePin: () -> Unit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .appleBouncy(pressedScale = 0.98f, onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isPinned) ServerIndigoAccent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline
        ),
        shadowElevation = 1.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
            ) {
                LibraArchitecturalBanner(
                    seedKey = server.id.ifBlank { server.name },
                    bannerMediaKey = server.bannerUrl,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.50f))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOwner) {
                        LibraRoleChip(label = "Kurucu", highlighted = true, accentColor = ServerIndigoAccent)
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (isPinned) Color(0xFFF59E0B) else Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onTogglePin)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.Star else Icons.Default.PushPin,
                                contentDescription = if (isPinned) "Sabitlemeyi kaldır" else "Sabitle",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibraSquircleAvatar(
                    photoUrl = server.avatarUrl,
                    title = server.name,
                    size = 48.dp,
                    cornerRadius = 15.dp,
                    borderColor = MaterialTheme.colorScheme.outline,
                    borderWidth = 1.dp
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = server.description.ifBlank { "Kanalları keşfetmek ve sohbete katılmak için aç" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Sunucuyu aç",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerWorkspaceContent(
    server: CommunityServer,
    currentUid: String,
    isLoading: Boolean,
    categories: List<ServerCategory>,
    channels: List<ServerChannel>,
    members: List<ServerMember>,
    serverRoles: List<ServerRoleDefinition>,
    channelPermissions: Map<String, List<ServerChannelPermissionOverride>>,
    inviteKey: String,
    latestByChannel: Map<String, ServerMessage?>,
    readAtByChannel: Map<String, Long>,
    onChannelClick: (ServerChannel) -> Unit,
    onOpenProfile: (String) -> Unit,
    onServerDeleted: () -> Unit,
    onLeaveServer: () -> Unit
) {
    val darkTheme = LocalLibraDarkTheme.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val repo = ServiceLocator.communityRepository
    val scope = rememberCoroutineScope()

    val isOwner = currentUid == server.ownerId
    val currentMember = remember(members, currentUid) { members.firstOrNull { it.uid == currentUid } }
    val currentRolePermissions = remember(serverRoles, currentMember) {
        serverRoles.firstOrNull { it.id == currentMember?.role }?.permissions.orEmpty()
    }

    var channelSearch by remember(server.id) { mutableStateOf("") }
    var collapsedCategories by remember(server.id) { mutableStateOf<Set<String>>(emptySet()) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var showMembersSheet by remember { mutableStateOf(false) }
    var showOwnerSettings by remember { mutableStateOf(false) }
    var showCreateCategory by remember { mutableStateOf(false) }
    var createChannelCategory by remember { mutableStateOf<ServerCategory?>(null) }
    var manageChannel by remember { mutableStateOf<ServerChannel?>(null) }
    var manageCategory by remember { mutableStateOf<ServerCategory?>(null) }
    var editChannel by remember { mutableStateOf<ServerChannel?>(null) }
    var editCategory by remember { mutableStateOf<ServerCategory?>(null) }
    var moveChannel by remember { mutableStateOf<ServerChannel?>(null) }
    var permissionChannel by remember { mutableStateOf<ServerChannel?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }

    val visibleChannels = remember(channels, channelPermissions, currentMember, currentRolePermissions, isOwner, channelSearch) {
        val q = channelSearch.trim()
        channels.filter { ch ->
            (isOwner || canViewServerChannel(ch, channelPermissions[ch.id].orEmpty(), currentMember, currentRolePermissions)) &&
                (q.isBlank() || ch.name.contains(q, ignoreCase = true))
        }
    }
    val visibleChannelsByCategory = remember(visibleChannels) { visibleChannels.groupBy { it.categoryId } }
    val uncategorizedChannels = remember(visibleChannels, categories) {
        val catIds = categories.map { it.id }.toSet()
        visibleChannels.filter { it.categoryId !in catIds }
    }
    val unreadCount = remember(visibleChannels, latestByChannel, readAtByChannel, currentUid) {
        visibleChannels.count { ch ->
            isServerChannelUnread(latestByChannel[ch.id], readAtByChannel[ch.id] ?: 0L, currentUid)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Architectural Server Hero Banner Card
        item(key = "server_workspace_hero") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                    ) {
                        LibraArchitecturalBanner(
                            seedKey = server.id.ifBlank { server.name },
                            bannerMediaKey = server.bannerUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.65f))
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LibraSquircleAvatar(
                                photoUrl = server.avatarUrl,
                                title = server.name,
                                size = 54.dp,
                                cornerRadius = 17.dp,
                                borderColor = Color.White.copy(alpha = 0.85f),
                                borderWidth = 2.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = server.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = buildString {
                                        append("${members.size} üye • ${visibleChannels.size} kanal")
                                        if (unreadCount > 0) append(" • $unreadCount okunmamış")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }
                    }

                    if (server.description.isNotBlank()) {
                        Text(
                            text = server.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp)
                        )
                    }

                    // Quick Server Actions Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ServerActionChip(
                            icon = Icons.Default.Info,
                            label = "Hakkında",
                            onClick = { showAboutSheet = true }
                        )
                        ServerActionChip(
                            icon = Icons.Default.People,
                            label = "Üyeler (${members.size})",
                            onClick = { showMembersSheet = true }
                        )
                        if (inviteKey.isNotBlank()) {
                            ServerActionChip(
                                icon = Icons.Default.Share,
                                label = "Davet Et",
                                highlighted = !isOwner,
                                onClick = {
                                    val link = "libra.sc/$inviteKey"
                                    clipboard.setText(AnnotatedString(link))
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "${server.name} sunucusuna katıl: $link")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Sunucu Davetini Paylaş"))
                                }
                            )
                        }
                        if (isOwner) {
                            ServerActionChip(
                                icon = Icons.Default.Settings,
                                label = "Ayarlar",
                                highlighted = true,
                                onClick = { showOwnerSettings = true }
                            )
                            ServerActionChip(
                                icon = Icons.Default.Add,
                                label = "Kategori",
                                onClick = { showCreateCategory = true }
                            )
                        }
                    }
                }
            }
        }

        // Channel Search Input
        item(key = "channel_search_pill") {
            val searchBg = if (darkTheme) Color(0xFF1A1923) else Color(0xFFF2F0F8)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = searchBg,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                TextField(
                    value = channelSearch,
                    onValueChange = { channelSearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Kanal ara…", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp)) },
                    trailingIcon = if (channelSearch.isNotBlank()) {
                        {
                            IconButton(onClick = { channelSearch = "" }) {
                                Icon(Icons.Default.Close, "Temizle")
                            }
                        }
                    } else null,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }

        if (isLoading && categories.isEmpty() && channels.isEmpty()) {
            item(key = "workspace_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ServerIndigoAccent, strokeWidth = 2.5.dp)
                }
            }
        } else {
            categories.forEach { category ->
                val catChannels = visibleChannelsByCategory[category.id].orEmpty()
                val collapsed = category.id in collapsedCategories
                val catUnread = catChannels.count { ch ->
                    isServerChannelUnread(latestByChannel[ch.id], readAtByChannel[ch.id] ?: 0L, currentUid)
                }
                item(key = "cat_hdr_${category.id}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                collapsedCategories = if (collapsed) collapsedCategories - category.id
                                else collapsedCategories + category.id
                            }
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = "Kategoriyi aç/kapat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = category.name.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (catUnread > 0 && collapsed) {
                            Surface(
                                shape = CircleShape,
                                color = ServerUnreadDotColor
                            ) {
                                Text(
                                    text = "$catUnread yeni",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (isOwner) {
                            IconButton(
                                onClick = { createChannelCategory = category },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Add, "Kanal ekle", modifier = Modifier.size(17.dp))
                            }
                            IconButton(
                                onClick = { manageCategory = category },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, "Kategori seçenekleri", modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
                if (!collapsed) {
                    items(catChannels, key = { "ch_${it.id}" }) { channel ->
                        val latest = latestByChannel[channel.id]
                        val isUnread = isServerChannelUnread(latest, readAtByChannel[channel.id] ?: 0L, currentUid)
                        ServerChannelRowCard(
                            channel = channel,
                            latestMessage = latest,
                            isUnread = isUnread,
                            isOwner = isOwner,
                            onClick = { onChannelClick(channel) },
                            onManage = { manageChannel = channel }
                        )
                    }
                }
            }

            if (uncategorizedChannels.isNotEmpty()) {
                items(uncategorizedChannels, key = { "ch_uncat_${it.id}" }) { channel ->
                    val latest = latestByChannel[channel.id]
                    val isUnread = isServerChannelUnread(latest, readAtByChannel[channel.id] ?: 0L, currentUid)
                    ServerChannelRowCard(
                        channel = channel,
                        latestMessage = latest,
                        isUnread = isUnread,
                        isOwner = isOwner,
                        onClick = { onChannelClick(channel) },
                        onManage = { manageChannel = channel }
                    )
                }
            }

            if (visibleChannels.isEmpty() && !isLoading) {
                item(key = "no_visible_channels") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                text = if (channelSearch.isNotBlank()) "Aramana uyan kanal bulunamadı" else "Görebildiğin kanal yok",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (channelSearch.isNotBlank()) "Farklı bir kanal adı dene." else "Sunucu sahibiyle kanal izinlerini kontrol edebilirsin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Server About & Invite Studio Sheet
    if (showAboutSheet) {
        ServerAboutStudioSheet(
            server = server,
            currentUid = currentUid,
            members = members,
            categoriesCount = categories.size,
            channelsCount = channels.size,
            rolesCount = serverRoles.size,
            inviteKey = inviteKey,
            onDismiss = { showAboutSheet = false },
            onOpenSettings = {
                showAboutSheet = false
                showOwnerSettings = true
            },
            onOpenMembers = {
                showAboutSheet = false
                showMembersSheet = true
            },
            onLeaveServer = {
                showAboutSheet = false
                onLeaveServer()
            }
        )
    }

    // Members Sheet
    if (showMembersSheet) {
        ServerMembersModalSheet(
            server = server,
            members = members,
            roles = serverRoles,
            currentUid = currentUid,
            onDismiss = { showMembersSheet = false },
            onOpenProfile = { uid ->
                showMembersSheet = false
                onOpenProfile(uid)
            }
        )
    }

    if (showCreateCategory) {
        NameDialog("Yeni Kategori", "Kategori adı", "Oluştur", { showCreateCategory = false }) { name ->
            scope.launch {
                when (val res = repo.createServerCategory(server.id, name)) {
                    is AppResult.Success -> showCreateCategory = false
                    is AppResult.Error -> actionError = res.error.message
                }
            }
        }
    }

    createChannelCategory?.let { cat ->
        NameDialog("${cat.name} • Yeni Kanal", "# kanal adı", "Oluştur", { createChannelCategory = null }) { name ->
            scope.launch {
                when (val res = repo.createServerChannel(server.id, cat.id, name)) {
                    is AppResult.Success -> createChannelCategory = null
                    is AppResult.Error -> actionError = res.error.message
                }
            }
        }
    }

    // Fixed Channel Management Sheet (replaces old broken literal "# \${channel.name}" dialog!)
    manageChannel?.let { channel ->
        ModalBottomSheet(
            onDismissRequest = { manageChannel = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "# ${channel.name}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Kanal yönetimi ve izinler",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                SheetActionRow(Icons.Default.Edit, "Kanal Adını Düzenle") {
                    manageChannel = null
                    editChannel = channel
                }
                if (categories.size > 1) {
                    SheetActionRow(Icons.Default.SwapHoriz, "Başka Kategoriye Taşı") {
                        manageChannel = null
                        moveChannel = channel
                    }
                }
                SheetActionRow(Icons.Default.Lock, "Kanal İzinlerini Düzenle") {
                    manageChannel = null
                    permissionChannel = channel
                }
                SheetActionRow(Icons.Default.DeleteOutline, "Kanalı Sil", isDestructive = true) {
                    scope.launch {
                        when (val res = repo.deleteServerChannel(server.id, channel.id)) {
                            is AppResult.Success -> manageChannel = null
                            is AppResult.Error -> actionError = res.error.message
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    editChannel?.let { ch ->
        NameDialog("Kanalı Düzenle", "# kanal adı", "Kaydet", { editChannel = null }, initialValue = ch.name) { name ->
            scope.launch {
                when (val res = repo.updateServerChannel(server.id, ch.id, name)) {
                    is AppResult.Success -> editChannel = null
                    is AppResult.Error -> actionError = res.error.message
                }
            }
        }
    }

    moveChannel?.let { ch ->
        AlertDialog(
            onDismissRequest = { moveChannel = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("# ${ch.name} • Taşı", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.filter { it.id != ch.categoryId }.forEach { cat ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    scope.launch {
                                        when (val res = repo.moveServerChannelToCategory(server.id, ch.id, cat.id)) {
                                            is AppResult.Success -> moveChannel = null
                                            is AppResult.Error -> actionError = res.error.message
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(cat.name, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { moveChannel = null }) { Text("Kapat") } }
        )
    }

    manageCategory?.let { cat ->
        ModalBottomSheet(
            onDismissRequest = { manageCategory = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(cat.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
                SheetActionRow(Icons.Default.Add, "Bu Kategoriye Kanal Ekle") {
                    manageCategory = null
                    createChannelCategory = cat
                }
                SheetActionRow(Icons.Default.Edit, "Kategori Adını Düzenle") {
                    manageCategory = null
                    editCategory = cat
                }
                SheetActionRow(Icons.Default.DeleteOutline, "Kategoriyi ve Kanallarını Sil", isDestructive = true) {
                    scope.launch {
                        when (val res = repo.deleteServerCategory(server.id, cat.id)) {
                            is AppResult.Success -> manageCategory = null
                            is AppResult.Error -> actionError = res.error.message
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    editCategory?.let { cat ->
        NameDialog("Kategoriyi Düzenle", "Kategori adı", "Kaydet", { editCategory = null }, initialValue = cat.name) { name ->
            scope.launch {
                when (val res = repo.updateServerCategory(server.id, cat.id, name)) {
                    is AppResult.Success -> editCategory = null
                    is AppResult.Error -> actionError = res.error.message
                }
            }
        }
    }

    permissionChannel?.let { ch ->
        ChannelPermissionDialog(server.id, ch, members, serverRoles) { permissionChannel = null }
    }

    if (showOwnerSettings && isOwner) {
        ServerOwnerSettingsDialog(
            server = server,
            members = members,
            categories = categories,
            channels = channels,
            onDismiss = { showOwnerSettings = false },
            onDeleted = {
                showOwnerSettings = false
                onServerDeleted()
            },
            onError = { actionError = it }
        )
    }

    actionError?.let { err ->
        AlertDialog(
            onDismissRequest = { actionError = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("İşlem Tamamlanamadı", fontWeight = FontWeight.Bold) },
            text = { Text(err) },
            confirmButton = { TextButton(onClick = { actionError = null }) { Text("Tamam") } }
        )
    }
}

@Composable
private fun ServerActionChip(
    icon: ImageVector,
    label: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val darkTheme = LocalLibraDarkTheme.current
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .appleBouncy(pressedScale = 0.94f, onClick = onClick),
        shape = CircleShape,
        color = when {
            highlighted -> ServerIndigoAccent
            darkTheme -> Color(0xFF1F1E2B)
            else -> Color(0xFFF3F2FA)
        },
        border = if (!highlighted) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (highlighted) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ServerChannelRowCard(
    channel: ServerChannel,
    latestMessage: ServerMessage?,
    isUnread: Boolean,
    isOwner: Boolean,
    onClick: () -> Unit,
    onManage: () -> Unit
) {
    val darkTheme = LocalLibraDarkTheme.current
    val shape = RoundedCornerShape(18.dp)
    val bgColor = when {
        isUnread && darkTheme -> Color(0xFF1E1B33)
        isUnread -> Color(0xFFF2F1FF)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isUnread) ServerIndigoAccent.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .pointerInput(channel.id, isOwner) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { if (isOwner) onManage() }
                )
            },
        shape = shape,
        color = bgColor,
        border = BorderStroke(if (isUnread) 1.2.dp else 0.85.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isUnread) ServerIndigoAccent else {
                        if (darkTheme) Color(0xFF232231) else Color(0xFFEEEBF8)
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = null,
                            tint = if (isUnread) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ServerUnreadDotColor)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (latestMessage != null && latestMessage.createdAt > 0L) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = formatConversationRelativeTime(latestMessage.createdAt),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isUnread) ServerUnreadDotColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val preview = remember(latestMessage) {
                    when {
                        latestMessage == null -> "Sohbete katıl"
                        latestMessage.text.isNotBlank() -> {
                            val sender = latestMessage.senderName.ifBlank { "Üye" }
                            "$sender: ${latestMessage.text}"
                        }
                        latestMessage.mediaUrl.isNotBlank() -> {
                            val sender = latestMessage.senderName.ifBlank { "Üye" }
                            "$sender: 📷 Fotoğraf"
                        }
                        else -> "Yeni mesaj"
                    }
                }
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isOwner) {
                IconButton(onClick = onManage, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Kanalı yönet",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Kanala gir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
            Text(label, fontWeight = FontWeight.SemiBold, color = contentColor, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerAboutStudioSheet(
    server: CommunityServer,
    currentUid: String,
    members: List<ServerMember>,
    categoriesCount: Int,
    channelsCount: Int,
    rolesCount: Int,
    inviteKey: String,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMembers: () -> Unit,
    onLeaveServer: () -> Unit
) {
    val darkTheme = LocalLibraDarkTheme.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val isOwner = currentUid == server.ownerId
    val ownerMember = remember(members, server.ownerId) { members.firstOrNull { it.uid == server.ownerId } }
    var confirmLeave by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(106.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    LibraArchitecturalBanner(
                        seedKey = server.id.ifBlank { server.name },
                        bannerMediaKey = server.bannerUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                LibraSquircleAvatar(
                    photoUrl = server.avatarUrl,
                    title = server.name,
                    size = 68.dp,
                    cornerRadius = 20.dp,
                    borderColor = MaterialTheme.colorScheme.surface,
                    borderWidth = 3.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = server.description.ifBlank { "Bu sunucu için henüz açıklama girilmemiş." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ServerMetricChip(members.size.toString(), "Üye", Modifier.weight(1f))
                ServerMetricChip(channelsCount.toString(), "Kanal", Modifier.weight(1f))
                ServerMetricChip(categoriesCount.toString(), "Kategori", Modifier.weight(1f))
                ServerMetricChip(rolesCount.toString(), "Rol", Modifier.weight(1f))
            }

            ownerMember?.let { owner ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UserAvatar(
                            photoUrl = owner.photoUrl,
                            initials = owner.displayName.take(1).uppercase().ifBlank { "K" },
                            size = 40.dp
                        )
                        Column(Modifier.weight(1f)) {
                            Text(owner.displayName.ifBlank { "Sunucu Kurucusu" }, fontWeight = FontWeight.Bold)
                            if (owner.username.isNotBlank()) {
                                Text("@${owner.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        LibraRoleChip(label = "Kurucu", highlighted = true, accentColor = ServerIndigoAccent)
                    }
                }
            }

            if (inviteKey.isNotBlank()) {
                val link = "libra.sc/$inviteKey"
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Sunucu Davet Anahtarı", fontWeight = FontWeight.Bold)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
                        ) {
                            Text(
                                text = link,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(link))
                                    Toast.makeText(context, "Kopyalandı: $link", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Kopyala")
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "${server.name} sunucusuna katıl: $link")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Davet Paylaş"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ServerIndigoAccent)
                            ) {
                                Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Paylaş", color = Color.White)
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenMembers,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.People, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Üyeleri Gör")
                }
                if (isOwner) {
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ServerIndigoAccent)
                    ) {
                        Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sunucu Ayarları", color = Color.White)
                    }
                } else {
                    OutlinedButton(
                        onClick = { confirmLeave = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ayrıl", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Sunucudan Ayrıl", fontWeight = FontWeight.Bold) },
            text = { Text("\"${server.name}\" sunucusundan ayrılmak istediğine emin misin?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        confirmLeave = false
                        onLeaveServer()
                    }
                ) { Text("Ayrıl") }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Vazgeç") } }
        )
    }
}

@Composable
private fun ServerMetricChip(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerMembersModalSheet(
    server: CommunityServer,
    members: List<ServerMember>,
    roles: List<ServerRoleDefinition>,
    currentUid: String,
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val repo = ServiceLocator.communityRepository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isOwner = currentUid == server.ownerId
    val roleMap = remember(roles) { roles.associate { it.id to it.name } }
    var search by remember { mutableStateOf("") }

    val sorted = remember(members, search, server.ownerId) {
        val q = search.trim()
        members
            .filter {
                q.isBlank() ||
                    it.displayName.contains(q, ignoreCase = true) ||
                    it.username.contains(q, ignoreCase = true)
            }
            .sortedWith(
                compareBy<ServerMember> {
                    when {
                        it.uid == server.ownerId -> 0
                        it.role == "ADMIN" -> 1
                        else -> 2
                    }
                }.thenBy { it.displayName.lowercase() }
            )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Üyeler (${members.size})",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("Üye ara…") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sorted, key = { it.uid }) { member ->
                    val roleLabel = when {
                        member.uid == server.ownerId -> "Kurucu"
                        member.role == "ADMIN" -> "Yönetici"
                        else -> roleMap[member.role] ?: "Üye"
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenProfile(member.uid) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                photoUrl = member.photoUrl,
                                initials = member.displayName.take(1).uppercase().ifBlank { "U" },
                                size = 42.dp,
                                onClick = { onOpenProfile(member.uid) }
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 11.dp)
                            ) {
                                Text(
                                    text = member.displayName.ifBlank { member.username.ifBlank { member.uid } },
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (member.username.isNotBlank()) {
                                        Text(
                                            text = "@${member.username}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    LibraRoleChip(
                                        label = roleLabel,
                                        highlighted = member.uid == server.ownerId || member.role == "ADMIN",
                                        accentColor = ServerIndigoAccent
                                    )
                                }
                            }
                            if (isOwner && member.uid != currentUid) {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            val next = if (member.role == "ADMIN") "MEMBER" else "ADMIN"
                                            when (val res = repo.setServerMemberRole(server.id, member.uid, next)) {
                                                is AppResult.Success -> Toast.makeText(
                                                    context,
                                                    if (next == "ADMIN") "Yönetici yapıldı." else "Yönetici alındı.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                is AppResult.Error -> Toast.makeText(context, res.error.message, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Text(if (member.role == "ADMIN") "Üye Yap" else "Yönetici")
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerProfilePreviewSheet(
    server: CommunityServer,
    isMember: Boolean,
    entering: Boolean,
    onDismiss: () -> Unit,
    onEnter: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(98.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    LibraArchitecturalBanner(
                        seedKey = server.id.ifBlank { server.name },
                        bannerMediaKey = server.bannerUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                LibraSquircleAvatar(
                    photoUrl = server.avatarUrl,
                    title = server.name,
                    size = 64.dp,
                    cornerRadius = 18.dp,
                    borderColor = MaterialTheme.colorScheme.surface,
                    borderWidth = 3.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp)
                )
            }
            Text(server.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
            Text(
                text = server.description.ifBlank { "Libra topluluğuna katıl ve kanallarda sohbete başla." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = "Kategoriler • Metin kanalları • Roller • Gerçek zamanlı sohbet",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    enabled = !entering,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Vazgeç") }
                Button(
                    enabled = !entering,
                    onClick = onEnter,
                    modifier = Modifier.weight(1.4f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ServerIndigoAccent)
                ) {
                    if (entering) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text(if (isMember) "Sunucuya Gir" else "Topluluğa Katıl", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateServerStudioSheet(
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        description: String,
        avatarUpload: PendingServerImageUpload?,
        bannerUpload: PendingServerImageUpload?,
        onDone: (Boolean, String?) -> Unit
    ) -> Unit
) {
    val darkTheme = LocalLibraDarkTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var bannerUri by remember { mutableStateOf<Uri?>(null) }
    var avatarUpload by remember { mutableStateOf<PendingServerImageUpload?>(null) }
    var bannerUpload by remember { mutableStateOf<PendingServerImageUpload?>(null) }
    var preparingImage by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var errText by remember { mutableStateOf<String?>(null) }

    fun preparePicked(uri: Uri, isBanner: Boolean) {
        preparingImage = true
        errText = null
        scope.launch {
            try {
                val prepared = prepareChatImageForUpload(
                    context = context,
                    uri = uri,
                    prefix = if (isBanner) "srv_banner" else "srv_avatar"
                )
                val upload = PendingServerImageUpload(
                    fileName = prepared.fileName,
                    bytes = prepared.bytes,
                    contentType = prepared.contentType
                )
                if (isBanner) {
                    bannerUri = uri
                    bannerUpload = upload
                } else {
                    avatarUri = uri
                    avatarUpload = upload
                }
            } catch (e: Exception) {
                errText = e.localizedMessage ?: "Görsel hazırlanamadı."
            } finally {
                preparingImage = false
            }
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { preparePicked(it, isBanner = false) }
    }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { preparePicked(it, isBanner = true) }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!creating) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Yeni Sunucu Kur", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
            Text(
                "Topluluğuna kimlik ver. İsim, kapak ve simgeyi sonradan dilediğin zaman değiştirebilirsin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(98.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(enabled = !preparingImage && !creating) {
                            bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        if (bannerUri != null) {
                            AsyncImage(bannerUri, "Sunucu kapağı", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            LibraArchitecturalBanner(seedKey = name.ifBlank { "new_server" }, bannerMediaKey = null, modifier = Modifier.fillMaxSize())
                        }
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.58f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Image, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text(
                                    if (bannerUri != null) "Kapağı Değiştir" else "Kapak Ekle",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp)
                        .size(66.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(enabled = !preparingImage && !creating) {
                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface,
                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (avatarUri != null) {
                            AsyncImage(avatarUri, "Sunucu simgesi", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Default.AddAPhoto, "Simge ekle", tint = if (darkTheme) LibraLavenderDarkText else LibraLavenderText)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 40) name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                label = { Text("Sunucu Adı") },
                placeholder = { Text("Örn. Bilim Kurgu Okurları") }
            )
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 160) description = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                label = { Text("Açıklama (isteğe bağlı)") },
                placeholder = { Text("Bu toplulukta neler konuşuluyor?") }
            )

            errText?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                enabled = name.trim().length >= 2 && !preparingImage && !creating,
                onClick = {
                    creating = true
                    errText = null
                    onCreate(name.trim(), description.trim(), avatarUpload, bannerUpload) { ok, err ->
                        creating = false
                        if (!ok) errText = err ?: "Sunucu oluşturulamadı."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ServerIndigoAccent)
            ) {
                if (creating || preparingImage) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Sunucuyu Oluştur", color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun MessageDateSeparator(createdAt: Long) {
    val locale = java.util.Locale.getDefault()
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = createdAt }
    val now = java.util.Calendar.getInstance()

    val text = remember(createdAt) {
        val sameDay = calendar.get(java.util.Calendar.ERA) == now.get(java.util.Calendar.ERA) &&
            calendar.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
            calendar.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR)

        val yesterday = java.util.Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(java.util.Calendar.DAY_OF_YEAR, -1)
        }

        val isYesterday = calendar.get(java.util.Calendar.ERA) == yesterday.get(java.util.Calendar.ERA) &&
            calendar.get(java.util.Calendar.YEAR) == yesterday.get(java.util.Calendar.YEAR) &&
            calendar.get(java.util.Calendar.DAY_OF_YEAR) == yesterday.get(java.util.Calendar.DAY_OF_YEAR)

        when {
            sameDay -> "Bugün"
            isYesterday -> "Dün"
            else -> java.text.SimpleDateFormat("dd MMMM yyyy", locale).format(java.util.Date(createdAt))
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = CircleShape
        ) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    }
}

private fun canViewServerChannel(
    channel: ServerChannel,
    permissions: List<ServerChannelPermissionOverride>,
    member: ServerMember?,
    rolePermissions: List<String>
): Boolean {
    var allowed = when (member?.role) {
        "ADMIN", "OWNER", "MEMBER" -> channel.allowEveryoneView
        else -> "VIEW_CHANNEL" in rolePermissions && channel.allowEveryoneView
    }
    permissions.firstOrNull { it.subjectType == "ROLE" && it.subjectId == "EVERYONE" }?.canView?.let { allowed = it }
    val role = member?.role.orEmpty()
    permissions.firstOrNull { it.subjectType == "ROLE" && it.subjectId == role }?.canView?.let { allowed = it }
    permissions.firstOrNull { it.subjectType == "USER" && it.subjectId == member?.uid }?.canView?.let { allowed = it }
    return allowed
}

private fun canSendServerChannelMedia(
    channel: ServerChannel,
    permissions: List<ServerChannelPermissionOverride>,
    member: ServerMember?,
    rolePermissions: List<String>
): Boolean {
    if (member?.role == "ADMIN" || member?.role == "OWNER") return true

    var allowed = when (member?.role) {
        "MEMBER" -> channel.allowEveryoneSendMedia
        else -> "SEND_MEDIA" in rolePermissions && channel.allowEveryoneSendMedia
    }
    permissions.firstOrNull { it.subjectType == "ROLE" && it.subjectId == "EVERYONE" }?.canSendMedia?.let { allowed = it }
    val role = member?.role.orEmpty()
    permissions.firstOrNull { it.subjectType == "ROLE" && it.subjectId == role }?.canSendMedia?.let { allowed = it }
    permissions.firstOrNull { it.subjectType == "USER" && it.subjectId == member?.uid }?.canSendMedia?.let { allowed = it }
    return allowed
}

private fun canSendServerChannel(
    channel: ServerChannel,
    permissions: List<ServerChannelPermissionOverride>,
    member: ServerMember?,
    rolePermissions: List<String>
): Boolean {
    var allowed = when (member?.role) {
        "ADMIN", "OWNER", "MEMBER" -> channel.allowEveryoneSend
        else -> "SEND_MESSAGES" in rolePermissions && channel.allowEveryoneSend
    }
    permissions.firstOrNull { it.subjectType == "ROLE" && it.subjectId == "EVERYONE" }?.canSend?.let { allowed = it }
    val role = member?.role.orEmpty()
    permissions.firstOrNull { it.subjectType == "ROLE" && it.subjectId == role }?.canSend?.let { allowed = it }
    permissions.firstOrNull { it.subjectType == "USER" && it.subjectId == member?.uid }?.canSend?.let { allowed = it }
    return allowed
}

@Composable
private fun NameDialog(
    title: String,
    label: String,
    confirm: String,
    onDismiss: () -> Unit,
    initialValue: String = "",
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= 40) value = it },
                label = { Text(label) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        },
        confirmButton = {
            Button(
                enabled = value.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = ServerIndigoAccent),
                onClick = { onConfirm(value.trim()) }
            ) { Text(confirm, color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
fun ChannelPermissionDialog(
    serverId: String,
    channel: ServerChannel,
    members: List<ServerMember>,
    roles: List<ServerRoleDefinition>,
    onDismiss: () -> Unit
) {
    val darkTheme = LocalLibraDarkTheme.current
    val repo = ServiceLocator.communityRepository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var permissions by remember(channel.id) { mutableStateOf<List<ServerChannelPermissionOverride>>(emptyList()) }
    var selected by remember { mutableStateOf<ServerChannelPermissionOverride?>(null) }

    LaunchedEffect(channel.id) {
        repo.observeChannelPermissions(serverId, channel.id).collect { result ->
            if (result is AppResult.Success) permissions = result.data
        }
    }

    val subjects = remember(members, roles) {
        listOf(
            ServerChannelPermissionOverride(subjectType = "ROLE", subjectId = "EVERYONE", subjectName = "@everyone"),
            ServerChannelPermissionOverride(subjectType = "ROLE", subjectId = "ADMIN", subjectName = "@admin"),
            ServerChannelPermissionOverride(subjectType = "ROLE", subjectId = "MEMBER", subjectName = "@member")
        ) + roles.map { role ->
            ServerChannelPermissionOverride(
                subjectType = "ROLE",
                subjectId = role.id,
                subjectName = "@" + role.name
            )
        } + members.map { member ->
            ServerChannelPermissionOverride(
                subjectType = "USER",
                subjectId = member.uid,
                subjectName = "@" + member.username.ifBlank { member.displayName }
            )
        }
    }.distinctBy { it.subjectType + "_" + it.subjectId }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                            }
                            Column(Modifier.weight(1f)) {
                                Text("# ${channel.name}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                                Text("Kanal izinleri", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }

                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        "Roller ve kişiler için kanal izinlerini ayrı ayrı belirleyebilirsin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(subjects, key = { it.subjectType + "_" + it.subjectId }) { subject ->
                            val active = selected?.subjectType == subject.subjectType &&
                                selected?.subjectId == subject.subjectId
                            val hasOverride = permissions.any {
                                it.subjectType == subject.subjectType && it.subjectId == subject.subjectId
                            }
                            Surface(
                                color = if (active) {
                                    if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    if (active) 1.5.dp else 1.dp,
                                    if (active) LibraLavender else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selected = permissions.firstOrNull {
                                            it.subjectType == subject.subjectType &&
                                                it.subjectId == subject.subjectId
                                        } ?: subject
                                    }
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (subject.subjectType == "ROLE") Icons.Default.Group else Icons.Default.Person,
                                        null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (active) LibraLavender else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(subject.subjectName, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    if (hasOverride) {
                                        LibraRoleChip(label = "Özel", highlighted = false)
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    selected?.let { target ->
                        var canView by remember(target.id, target.subjectId) { mutableStateOf(target.canView) }
                        var canSend by remember(target.id, target.subjectId) { mutableStateOf(target.canSend) }
                        var canSendMedia by remember(target.id, target.subjectId) { mutableStateOf(target.canSendMedia) }

                        Spacer(Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, LibraLavender.copy(alpha = 0.55f))
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Seçili: ${target.subjectName}", fontWeight = FontWeight.ExtraBold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = canView == true, onCheckedChange = { canView = it })
                                    Text("Kanalı görebilir")
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = canSend == true, onCheckedChange = { canSend = it })
                                    Text("Mesaj yazabilir")
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = canSendMedia == true, onCheckedChange = { canSendMedia = it })
                                    Text("Fotoğraf / medya gönderebilir")
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                repo.setChannelPermission(
                                                    serverId,
                                                    channel.id,
                                                    target.copy(
                                                        canView = canView,
                                                        canSend = canSend,
                                                        canSendMedia = canSendMedia
                                                    )
                                                )
                                                Toast.makeText(context, "İzin kaydedildi.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ServerIndigoAccent)
                                    ) { Text("Kaydet", color = Color.White) }
                                    OutlinedButton(
                                        onClick = {
                                            if (target.id.isNotBlank()) {
                                                scope.launch {
                                                    repo.deleteChannelPermission(serverId, channel.id, target.id)
                                                }
                                            }
                                            selected = null
                                        }
                                    ) { Text("Varsayılana Dön") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerChatScreen(
    server: CommunityServer,
    channel: ServerChannel,
    visibleChannels: List<ServerChannel>,
    categories: List<ServerCategory>,
    members: List<ServerMember>,
    serverRoles: List<ServerRoleDefinition>,
    inviteKey: String,
    latestByChannel: Map<String, ServerMessage?>,
    readAtByChannel: Map<String, Long>,
    onBack: () -> Unit,
    onSwitchChannel: (ServerChannel) -> Unit,
    onMarkChannelRead: (Long) -> Unit,
    onServerDeleted: () -> Unit,
    onLeaveServer: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val darkTheme = LocalLibraDarkTheme.current
    val chatRepository = ServiceLocator.chatRepository
    val communityRepository = ServiceLocator.communityRepository
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val currentUid = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()
    val isOwner = currentUid == server.ownerId

    var messages by remember(server.id, channel.id) { mutableStateOf<List<ServerMessage>>(emptyList()) }
    var error by remember(server.id, channel.id) { mutableStateOf<String?>(null) }
    var draft by remember(channel.id) { mutableStateOf("") }
    var replyTarget by remember(channel.id) { mutableStateOf<ServerMessage?>(null) }
    var actionMessage by remember { mutableStateOf<ServerMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ServerMessage?>(null) }
    var pendingUri by remember(channel.id) { mutableStateOf<Uri?>(null) }
    var pendingUrl by remember(channel.id) { mutableStateOf("") }
    var pendingType by remember(channel.id) { mutableStateOf("image/jpeg") }
    var uploading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var mediaError by remember { mutableStateOf<String?>(null) }
    var showChannelSwitcher by remember { mutableStateOf(false) }
    var showMembers by remember { mutableStateOf(false) }
    var showOwnerSettings by remember { mutableStateOf(false) }
    var showServerAbout by remember { mutableStateOf(false) }
    var actionError by remember(server.id, channel.id) { mutableStateOf<String?>(null) }
    var reactionLookup by remember { mutableStateOf<Pair<ServerMessage, String>?>(null) }
    var reactionUsers by remember { mutableStateOf<List<ReactionUser>>(emptyList()) }
    var fullscreenImageKey by remember { mutableStateOf<String?>(null) }
    var channelPermissionOverrides by remember(channel.id) {
        mutableStateOf<List<ServerChannelPermissionOverride>>(emptyList())
    }

    var isSelectionMode by remember(server.id, channel.id) { mutableStateOf(false) }
    var selectedMessageIds by remember(server.id, channel.id) { mutableStateOf<Set<String>>(emptySet()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    var highlightNonce by remember { mutableIntStateOf(0) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedMessageIds = emptySet()
    }

    fun toggleSelection(messageId: String) {
        val next = if (messageId in selectedMessageIds) selectedMessageIds - messageId else selectedMessageIds + messageId
        selectedMessageIds = next
        if (next.isEmpty()) isSelectionMode = false
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        pendingUri = uri
        pendingUrl = ""
        uploading = true
        progress = 0
        mediaError = null
        scope.launch {
            try {
                val prepared = prepareChatImageForUpload(context, uri, prefix = "server")
                pendingType = prepared.contentType
                val request = StorageUploadRequest(
                    fileName = prepared.fileName,
                    bytes = prepared.bytes,
                    contentType = prepared.contentType,
                    targetDirectory = "users/$currentUid"
                )
                when (val result = ServiceLocator.storageRepository.uploadMedia(request) { pct ->
                    scope.launch(Dispatchers.Main) { progress = pct }
                }.first()) {
                    is AppResult.Success -> { pendingUrl = result.data; progress = 100 }
                    is AppResult.Error -> mediaError = result.error.message
                }
            } catch (e: Exception) {
                mediaError = e.localizedMessage ?: "Fotoğraf yüklenemedi."
            } finally {
                uploading = false
            }
        }
    }

    LaunchedEffect(server.id, channel.id) {
        when (val result = communityRepository.getChannelPermissions(server.id, channel.id)) {
            is AppResult.Success -> channelPermissionOverrides = result.data
            is AppResult.Error -> channelPermissionOverrides = emptyList()
        }
    }

    val currentMember = remember(members, currentUid) { members.firstOrNull { it.uid == currentUid } }
    val currentRolePermissions = remember(serverRoles, currentMember) {
        serverRoles.firstOrNull { it.id == currentMember?.role }?.permissions.orEmpty()
    }
    val canSendInChannel = remember(channel, channelPermissionOverrides, currentMember, currentRolePermissions, isOwner) {
        isOwner || currentMember?.let {
            canSendServerChannel(channel, channelPermissionOverrides, it, currentRolePermissions)
        } ?: true
    }
    val canSendMediaInChannel = remember(channel, channelPermissionOverrides, currentMember, currentRolePermissions, isOwner) {
        isOwner || currentMember?.let {
            canSendServerChannelMedia(channel, channelPermissionOverrides, it, currentRolePermissions)
        } ?: true
    }

    LaunchedEffect(server.id, channel.id) {
        messages = emptyList()
        onMarkChannelRead(System.currentTimeMillis())
        chatRepository.observeServerMessages(server.id, channelId = channel.id).collect { result ->
            when (result) {
                is AppResult.Success -> {
                    messages = result.data
                    val newestTs = result.data.lastOrNull()?.createdAt ?: System.currentTimeMillis()
                    onMarkChannelRead(max(System.currentTimeMillis(), newestTs))
                }
                is AppResult.Error -> error = result.error.message
            }
        }
    }

    val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
    val lastMessageId = messages.lastOrNull()?.id.orEmpty()
    val showScrollToBottom by remember { derivedStateOf { listState.canScrollForward } }

    LaunchedEffect(lastMessageId, imeBottomPx) {
        if (messages.isEmpty() || isSelectionMode) return@LaunchedEffect
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        val shouldStickToBottom =
            imeBottomPx > 0 || lastVisibleIndex < 0 || lastVisibleIndex >= messages.lastIndex - 3
        if (shouldStickToBottom) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LaunchedEffect(highlightedMessageId, highlightNonce) {
        if (highlightedMessageId != null) {
            delay(1700L)
            highlightedMessageId = null
        }
    }

    val deletableSelectedIds = remember(messages, selectedMessageIds, currentUid) {
        messages.filter { it.id in selectedMessageIds && it.senderId == currentUid }.map { it.id }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Channel Header (shows exact Server + Channel context + 1-tap Channel Switcher!)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isSelectionMode) {
                if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
            } else {
                MaterialTheme.colorScheme.surface
            }
        ) {
            Column {
                if (isSelectionMode) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isSelectionMode = false; selectedMessageIds = emptySet() }) {
                            Icon(Icons.Default.Close, "Seçimi kapat")
                        }
                        Text("${selectedMessageIds.size} seçildi", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(
                            enabled = selectedMessageIds.isNotEmpty(),
                            onClick = {
                                val copied = messages.filter { it.id in selectedMessageIds }
                                    .sortedBy { it.createdAt }
                                    .mapNotNull { m -> m.text.trim().takeIf { it.isNotBlank() }?.let { "${m.senderName.ifBlank { "Üye" }}: $it" } }
                                    .joinToString("\n")
                                if (copied.isNotBlank()) {
                                    clipboard.setText(AnnotatedString(copied))
                                    Toast.makeText(context, "Seçilen mesajlar kopyalandı.", Toast.LENGTH_SHORT).show()
                                }
                                isSelectionMode = false
                                selectedMessageIds = emptySet()
                            }
                        ) { Icon(Icons.Default.ContentCopy, "Kopyala") }
                        IconButton(
                            enabled = deletableSelectedIds.isNotEmpty(),
                            onClick = { showBatchDeleteConfirm = true }
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                "Sil",
                                tint = if (deletableSelectedIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kanallara dön")
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showChannelSwitcher = true }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LibraSquircleAvatar(
                                photoUrl = server.avatarUrl,
                                title = server.name,
                                size = 34.dp,
                                cornerRadius = 11.dp
                            )
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "# ${channel.name}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = "Kanal değiştir",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "${server.name} • ${members.size} üye",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (messages.isNotEmpty()) {
                            IconButton(onClick = { isSelectionMode = true }) {
                                Icon(Icons.Default.CheckCircleOutline, "Mesajları seç", modifier = Modifier.size(20.dp))
                            }
                        }
                        IconButton(onClick = { showServerAbout = true }) {
                            Icon(Icons.Default.Info, "Sunucu hakkında", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { showMembers = true }) {
                            Icon(Icons.Default.People, "Üyeler", modifier = Modifier.size(20.dp))
                        }
                        if (isOwner) {
                            IconButton(onClick = { showOwnerSettings = true }) {
                                Icon(Icons.Default.Settings, "Sunucu ayarları", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }

        listOfNotNull(error, actionError, mediaError).forEach { msg ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 28.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("# ${channel.name} kanalına hoş geldin", fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Bu kanalda henüz mesaj yok. Sohbeti ilk sen başlatabilirsin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                    val previous = messages.getOrNull(index - 1)
                    val sameDayAsPrev = previous != null &&
                        formatChatTimeHeader(previous.createdAt) == formatChatTimeHeader(message.createdAt)
                    if (!sameDayAsPrev && message.createdAt > 0L) {
                        MessageDateSeparator(message.createdAt)
                    }
                    val groupedWithPrevious = previous != null &&
                        sameDayAsPrev &&
                        previous.senderId == message.senderId &&
                        message.createdAt - previous.createdAt in 0..300_000L

                    val mine = message.senderId == currentUid
                    var dragX by remember(message.id) { mutableFloatStateOf(0f) }
                    val isSelected = message.id in selectedMessageIds
                    val isHighlighted = highlightedMessageId == message.id
                    val isMentioned = message.mentionedUserIds.contains(currentUid) || message.mentionsEveryone || message.mentionsHere
                    val rowBg by animateColorAsState(
                        targetValue = when {
                            isHighlighted -> if (darkTheme) Color(0xFF4C1D95) else Color(0xFFEDE9FE)
                            isSelected -> if (darkTheme) Color(0xFF2E1065) else Color(0xFFF3E8FF)
                            else -> Color.Transparent
                        },
                        label = "server_msg_highlight"
                    )

                    val bubbleColor = when {
                        isMentioned && !mine -> if (darkTheme) Color(0xFF3B2814) else Color(0xFFFFF4E5)
                        mine -> if (darkTheme) Color(0xFF4338CA) else ServerIndigoAccent
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val primaryText = if (mine) Color.White else MaterialTheme.colorScheme.onSurface
                    val secondaryText = if (mine) Color(0xFFE0E7FF) else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(rowBg)
                            .let { mod ->
                                if (isSelectionMode) {
                                    mod.clickable { toggleSelection(message.id) }
                                } else {
                                    mod.pointerInput(message.id + "-swipe") {
                                        detectHorizontalDragGestures(
                                            onHorizontalDrag = { _, amount ->
                                                dragX = (dragX + amount).coerceIn(0f, 92f)
                                            },
                                            onDragEnd = {
                                                if (dragX >= 58f) {
                                                    runCatching { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                                                    replyTarget = message
                                                }
                                                dragX = 0f
                                            },
                                            onDragCancel = { dragX = 0f }
                                        )
                                    }
                                }
                            }
                    ) {
                        if (dragX > 10f) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 6.dp)
                                    .size(30.dp),
                                shape = CircleShape,
                                color = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.Reply, "Yanıtla", modifier = Modifier.size(16.dp), tint = LibraLavender)
                                }
                            }
                        }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(dragX.roundToInt(), 0) },
                            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            if (isSelectionMode) {
                                Surface(
                                    modifier = Modifier
                                        .padding(end = 6.dp, bottom = 6.dp)
                                        .size(20.dp),
                                    shape = CircleShape,
                                    color = if (isSelected) ServerIndigoAccent else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.5.dp, if (isSelected) ServerIndigoAccent else MaterialTheme.colorScheme.outline)
                                ) {
                                    if (isSelected) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }

                            if (!mine) {
                                if (groupedWithPrevious) {
                                    Spacer(Modifier.width(36.dp))
                                } else {
                                    UserAvatar(
                                        photoUrl = message.senderPhotoUrl,
                                        initials = message.senderName.take(1).uppercase().ifBlank { "U" },
                                        size = 30.dp,
                                        onClick = {
                                            if (isSelectionMode) toggleSelection(message.id)
                                            else onOpenProfile(message.senderId)
                                        }
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                            }

                            Surface(
                                color = bubbleColor,
                                shape = RoundedCornerShape(
                                    topStart = if (!mine && groupedWithPrevious) 6.dp else 18.dp,
                                    topEnd = if (mine && groupedWithPrevious) 6.dp else 18.dp,
                                    bottomStart = 18.dp,
                                    bottomEnd = 18.dp
                                ),
                                border = when {
                                    isSelected -> BorderStroke(2.dp, if (mine) Color.White else ServerIndigoAccent)
                                    isMentioned && !mine -> BorderStroke(1.dp, Color(0xFFF59E0B))
                                    mine -> null
                                    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                },
                                modifier = Modifier.pointerInput(message.id + "-tap", isSelectionMode) {
                                    detectTapGestures(
                                        onTap = { if (isSelectionMode) toggleSelection(message.id) },
                                        onDoubleTap = {
                                            if (isSelectionMode) toggleSelection(message.id)
                                            else {
                                                runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                                scope.launch {
                                                    chatRepository.toggleServerMessageReaction(server.id, message.id, "❤️", channelId = channel.id)
                                                }
                                            }
                                        },
                                        onLongPress = {
                                            if (isSelectionMode) toggleSelection(message.id)
                                            else {
                                                runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                                actionMessage = message
                                            }
                                        }
                                    )
                                }
                            ) {
                                Column(
                                    Modifier
                                        .widthIn(max = 310.dp)
                                        .padding(horizontal = 9.dp, vertical = 7.dp)
                                ) {
                                    if (!mine && !groupedWithPrevious) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(bottom = 3.dp)
                                        ) {
                                            Text(
                                                text = message.senderName.ifBlank { "Libra Üyesi" },
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = deterministicSenderAccent(message.senderId, darkTheme),
                                                modifier = Modifier.clickable { onOpenProfile(message.senderId) }
                                            )
                                            if (message.senderId == server.ownerId) {
                                                LibraRoleChip(label = "Kurucu", highlighted = true, accentColor = ServerIndigoAccent)
                                            }
                                        }
                                    }

                                    if (message.replyToMessageId.isNotBlank()) {
                                        val quoteBg = if (mine) Color.Black.copy(alpha = 0.22f)
                                        else if (darkTheme) MaterialTheme.colorScheme.surfaceVariant
                                        else Color(0xFFF3F1FA)
                                        Surface(
                                            color = quoteBg,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 5.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    if (isSelectionMode) {
                                                        toggleSelection(message.id)
                                                    } else {
                                                        val idx = findReplyTargetIndex(messages, message.replyToMessageId) { it.id }
                                                        if (idx >= 0) {
                                                            scope.launch {
                                                                listState.animateScrollToItem(idx)
                                                                highlightedMessageId = message.replyToMessageId
                                                                highlightNonce++
                                                            }
                                                        } else {
                                                            Toast.makeText(context, "Alıntılanan mesaj bulunamadı.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                        ) {
                                            Column(Modifier.padding(8.dp)) {
                                                Text(
                                                    message.replyToSenderName.ifBlank { "Yanıtlanan mesaj" },
                                                    color = if (mine) Color.White else ServerIndigoAccent,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                                Text(
                                                    message.replyToText.ifBlank { "📷 Fotoğraf" },
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = secondaryText
                                                )
                                            }
                                        }
                                    }

                                    message.sharedContent?.let { shared ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 4.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (mine) Color.Black.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Column(Modifier.padding(8.dp)) {
                                                Text(
                                                    when (shared.type) {
                                                        "story" -> "Hikâyeden bahsetti"
                                                        "post" -> "Bir gönderi paylaştı"
                                                        "book" -> "Bir kitap paylaştı"
                                                        else -> "Paylaşılan içerik"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (mine) Color(0xFFC7D2FE) else ServerIndigoAccent
                                                )
                                                Text(shared.title.ifBlank { "Libra içeriği" }, fontWeight = FontWeight.SemiBold, color = primaryText)
                                                if (shared.text.isNotBlank()) {
                                                    Text(shared.text, maxLines = 2, style = MaterialTheme.typography.bodySmall, color = secondaryText)
                                                }
                                            }
                                        }
                                    }

                                    if (message.mediaUrl.isNotBlank()) {
                                        LibraSignedImage(
                                            mediaKeyOrUrl = message.mediaUrl,
                                            contentDescription = "Gönderilen fotoğraf",
                                            modifier = Modifier
                                                .widthIn(min = 170.dp, max = 250.dp)
                                                .heightIn(min = 120.dp, max = 280.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable {
                                                    if (isSelectionMode) toggleSelection(message.id)
                                                    else fullscreenImageKey = message.mediaUrl
                                                }
                                        )
                                    }

                                    if (message.text.isNotBlank()) {
                                        Text(
                                            text = message.text,
                                            color = primaryText,
                                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                            modifier = Modifier.padding(
                                                top = if (message.mediaUrl.isNotBlank()) 6.dp else 1.dp,
                                                bottom = 1.dp
                                            )
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (message.editedAt != null) {
                                            Text("düzenlendi •", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = secondaryText)
                                        }
                                        if (message.createdAt > 0L) {
                                            Text(formatMessageHour(message.createdAt), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = secondaryText)
                                        }
                                    }

                                    if (message.reactions.isNotEmpty()) {
                                        val reactionCounts = remember(message.id, message.reactions) {
                                            message.reactions.values.groupingBy { it }.eachCount()
                                        }
                                        Row(
                                            modifier = Modifier.padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            reactionCounts.forEach { (emoji, count) ->
                                                Surface(
                                                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                                    shape = CircleShape,
                                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                                                    modifier = Modifier.pointerInput(message.id + emoji) {
                                                        detectTapGestures(
                                                            onTap = {
                                                                scope.launch {
                                                                    chatRepository.toggleServerMessageReaction(server.id, message.id, emoji, channelId = channel.id)
                                                                }
                                                            },
                                                            onLongPress = { reactionLookup = message to emoji }
                                                        )
                                                    }
                                                ) {
                                                    Text(
                                                        "$emoji $count",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurface
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
            }

            AnimatedVisibility(
                visible = showScrollToBottom && messages.isNotEmpty(),
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .appleBouncy(
                            pressedScale = 0.86f,
                            onClick = {
                                if (messages.isNotEmpty()) scope.launch { listState.animateScrollToItem(messages.lastIndex) }
                            }
                        ),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.KeyboardArrowDown, "En alta git")
                    }
                }
            }
        }

        if (pendingUri != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(pendingUri, "Gönderilecek fotoğraf", Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(
                            if (uploading) "Fotoğraf yükleniyor %$progress"
                            else if (pendingUrl.isNotBlank()) "Fotoğraf hazır ✓"
                            else "Hazırlanıyor…",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        if (uploading) {
                            LinearProgressIndicator(
                                progress = { (progress / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 5.dp)
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = ServerIndigoAccent
                            )
                        }
                    }
                    IconButton(onClick = { pendingUri = null; pendingUrl = ""; mediaError = null }) {
                        Icon(Icons.Default.Close, "Kaldır")
                    }
                }
            }
        }

        replyTarget?.let { RichReplyBanner("Yanıtlanıyor: ${it.senderName}", it.text.ifBlank { "📷 Fotoğraf" }, { replyTarget = null }) }
        editingMessage?.let { RichReplyBanner("Mesaj düzenleniyor", it.text, { editingMessage = null; draft = "" }) }

        // Pill Composer Dock
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 2.dp
        ) {
            Row(
                Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val canPick = canSendMediaInChannel && !uploading && editingMessage == null
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .appleBouncy(
                            enabled = canPick,
                            pressedScale = 0.86f,
                            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        ),
                    shape = CircleShape,
                    color = if (canPick) {
                        if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            "Fotoğraf ekle",
                            tint = if (canPick) {
                                if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                TextField(
                    value = draft,
                    onValueChange = { if (it.length <= 1000) draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            when {
                                !canSendInChannel -> "Bu kanalda mesaj yazma iznin yok."
                                !canSendMediaInChannel -> "#${channel.name} kanalına yaz…"
                                else -> "#${channel.name} kanalına mesaj yaz…"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    enabled = canSendInChannel && !uploading,
                    maxLines = 4,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )

                val canSend = canSendInChannel && (draft.isNotBlank() || pendingUrl.isNotBlank()) && !uploading
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .appleBouncy(
                            enabled = canSend,
                            pressedScale = 0.86f,
                            onClick = {
                                val text = draft.trim()
                                val edit = editingMessage
                                if (edit != null) {
                                    scope.launch {
                                        when (val result = chatRepository.editServerMessage(server.id, edit.id, text, channelId = channel.id)) {
                                            is AppResult.Error -> actionError = result.error.message
                                            is AppResult.Success -> {
                                                actionError = null
                                                editingMessage = null
                                                draft = ""
                                            }
                                        }
                                    }
                                } else if (pendingUrl.isNotBlank()) {
                                    scope.launch {
                                        when (val result = chatRepository.sendServerMediaMessage(server.id, pendingUrl, pendingType, text, replyTarget, channelId = channel.id)) {
                                            is AppResult.Error -> actionError = result.error.message
                                            is AppResult.Success -> {
                                                actionError = null
                                                pendingUri = null
                                                pendingUrl = ""
                                                replyTarget = null
                                                draft = ""
                                            }
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        when (val result = chatRepository.sendServerMessage(server.id, text, replyTarget, channelId = channel.id)) {
                                            is AppResult.Error -> actionError = result.error.message
                                            is AppResult.Success -> {
                                                actionError = null
                                                replyTarget = null
                                                draft = ""
                                            }
                                        }
                                    }
                                }
                            }
                        ),
                    shape = CircleShape,
                    color = if (canSend) ServerIndigoAccent else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (editingMessage != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                            contentDescription = if (editingMessage != null) "Kaydet" else "Gönder",
                            tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }

    // 1-Tap Channel Switcher Modal Sheet (fast hop between channels with unread indicators!)
    if (showChannelSwitcher) {
        ModalBottomSheet(
            onDismissRequest = { showChannelSwitcher = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LibraSquircleAvatar(photoUrl = server.avatarUrl, title = server.name, size = 38.dp, cornerRadius = 12.dp)
                    Column(Modifier.weight(1f)) {
                        Text(server.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                        Text("Kanal değiştir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(visibleChannels, key = { it.id }) { ch ->
                        val active = ch.id == channel.id
                        val unread = !active && isServerChannelUnread(latestByChannel[ch.id], readAtByChannel[ch.id] ?: 0L, currentUid)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    showChannelSwitcher = false
                                    if (!active) onSwitchChannel(ch)
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) {
                                if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
                            } else {
                                MaterialTheme.colorScheme.background
                            },
                            border = BorderStroke(
                                if (active) 1.5.dp else 1.dp,
                                if (active) ServerIndigoAccent else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Tag, null, modifier = Modifier.size(17.dp), tint = if (active) ServerIndigoAccent else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = ch.name,
                                    fontWeight = if (active || unread) FontWeight.ExtraBold else FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (unread) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(ServerUnreadDotColor)
                                    )
                                } else if (active) {
                                    Icon(Icons.Default.Check, null, tint = ServerIndigoAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    fullscreenImageKey?.let { mediaKey ->
        var signedUrl by remember(mediaKey) { mutableStateOf<String?>(null) }
        LaunchedEffect(mediaKey) {
            signedUrl = runCatching { ServiceLocator.storageRepository.getSignedMediaUrl(mediaKey) }.getOrNull()
                ?: runCatching { ServiceLocator.storageRepository.getPublicCdnUrl(mediaKey) }.getOrNull()
        }
        signedUrl?.let { url ->
            FullScreenImageViewerDialog(mediaUrl = url, onDismiss = { fullscreenImageKey = null })
        }
    }

    reactionLookup?.let { (message, emoji) ->
        LaunchedEffect(message.id, emoji) {
            reactionUsers = (chatRepository.getServerMessageReactionUsers(server.id, message.id, emoji, channel.id)
                as? AppResult.Success)?.data.orEmpty()
        }
        ModalBottomSheet(
            onDismissRequest = { reactionLookup = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Text("$emoji Tepkisi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${reactionUsers.size} kişi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                reactionUsers.forEach { user ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                reactionLookup = null
                                onOpenProfile(user.uid)
                            }
                            .padding(vertical = 7.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            photoUrl = user.photoUrl,
                            initials = user.displayName.take(1).uppercase().ifBlank { "U" },
                            size = 36.dp,
                            onClick = {
                                reactionLookup = null
                                onOpenProfile(user.uid)
                            }
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(user.displayName.ifBlank { "Libra Üyesi" }, fontWeight = FontWeight.SemiBold)
                            if (user.username.isNotBlank()) Text("@${user.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }

    actionMessage?.let { message ->
        RichMessageActionSheet(
            canEdit = message.senderId == currentUid && message.mediaUrl.isBlank(),
            canDelete = message.senderId == currentUid,
            previewText = message.text,
            previewMediaUrl = message.mediaUrl,
            isOwnMessage = message.senderId == currentUid,
            selectedReaction = message.reactions[currentUid],
            onDismiss = { actionMessage = null },
            onReply = { replyTarget = message; actionMessage = null },
            onSelectMultiple = {
                actionMessage = null
                isSelectionMode = true
                selectedMessageIds = setOf(message.id)
            },
            onCopy = if (message.text.isNotBlank()) {
                {
                    clipboard.setText(AnnotatedString(message.text))
                    Toast.makeText(context, "Mesaj kopyalandı.", Toast.LENGTH_SHORT).show()
                    actionMessage = null
                }
            } else null,
            onReaction = { emoji ->
                scope.launch { chatRepository.toggleServerMessageReaction(server.id, message.id, emoji, channelId = channel.id) }
                actionMessage = null
            },
            onEdit = { editingMessage = message; draft = message.text; actionMessage = null },
            onDelete = {
                scope.launch { chatRepository.deleteServerMessage(server.id, message.id, channelId = channel.id) }
                actionMessage = null
            }
        )
    }

    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Mesajları Sil (${deletableSelectedIds.size})", fontWeight = FontWeight.Bold) },
            text = {
                val skipped = selectedMessageIds.size - deletableSelectedIds.size
                val note = if (skipped > 0) " Başkasına ait $skipped mesaj atlanacak." else ""
                Text("Seçilen ${deletableSelectedIds.size} mesaj kalıcı olarak silinecek.$note")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val ids = deletableSelectedIds
                        showBatchDeleteConfirm = false
                        isSelectionMode = false
                        selectedMessageIds = emptySet()
                        scope.launch {
                            when (val result = chatRepository.deleteServerMessages(server.id, ids, channelId = channel.id)) {
                                is AppResult.Success -> {
                                    actionError = null
                                    Toast.makeText(context, "${ids.size} mesaj silindi.", Toast.LENGTH_SHORT).show()
                                }
                                is AppResult.Error -> actionError = result.error.message
                            }
                        }
                    }
                ) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { showBatchDeleteConfirm = false }) { Text("Vazgeç") } }
        )
    }

    if (showServerAbout) {
        ServerAboutStudioSheet(
            server = server,
            currentUid = currentUid,
            members = members,
            categoriesCount = categories.size,
            channelsCount = visibleChannels.size,
            rolesCount = serverRoles.size,
            inviteKey = inviteKey,
            onDismiss = { showServerAbout = false },
            onOpenSettings = { showServerAbout = false; showOwnerSettings = true },
            onOpenMembers = { showServerAbout = false; showMembers = true },
            onLeaveServer = { showServerAbout = false; onLeaveServer() }
        )
    }

    if (showMembers) {
        ServerMembersModalSheet(
            server = server,
            members = members,
            roles = serverRoles,
            currentUid = currentUid,
            onDismiss = { showMembers = false },
            onOpenProfile = { uid -> showMembers = false; onOpenProfile(uid) }
        )
    }

    if (showOwnerSettings && isOwner) {
        ServerOwnerSettingsDialog(
            server = server,
            members = members,
            categories = categories,
            channels = visibleChannels,
            onDismiss = { showOwnerSettings = false },
            onDeleted = { showOwnerSettings = false; onServerDeleted() },
            onError = { actionError = it }
        )
    }
}