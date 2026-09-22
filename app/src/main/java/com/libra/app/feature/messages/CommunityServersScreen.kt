package com.libra.app.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.CommunityServer
import com.libra.app.domain.model.ServerMessage
import com.libra.app.domain.model.ServerMember
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun CommunityServersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = ServiceLocator.chatRepository
    val scope = rememberCoroutineScope()
    var servers by remember { mutableStateOf<List<CommunityServer>>(emptyList()) }
    var selectedServer by remember { mutableStateOf<CommunityServer?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.observeCommunityServers().collect { result ->
            when (result) {
                is AppResult.Success -> servers = result.data
                is AppResult.Error -> error = result.error.message
            }
        }
    }

    selectedServer?.let { server ->
        ServerChatScreen(
            server = server,
            onBack = { selectedServer = null },
            modifier = modifier
        )
        return
    }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Column(Modifier.weight(1f)) {
                Text("Sunucular", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Text("Kitap türlerine göre topluluklara katıl.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, "Sunucu oluştur")
            }
        }
        Spacer(Modifier.height(12.dp))

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        if (servers.isEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Groups, null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Henüz sunucu yok.", fontWeight = FontWeight.Bold)
                    Text("İlk topluluğu sen oluştur.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { showCreate = true }) { Text("Sunucu oluştur") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(servers, key = { it.id }) { server ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Groups, null) }
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(server.name, fontWeight = FontWeight.Bold)
                                if (server.description.isNotBlank()) {
                                    Text(server.description, maxLines = 2, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    when (val result = repository.joinCommunityServer(server.id)) {
                                        is AppResult.Success -> {
                                            error = null
                                            selectedServer = server
                                        }
                                        is AppResult.Error -> error = result.error.message
                                    }
                                }
                            }) {
                                Icon(Icons.Default.ChevronRight, "Katıl")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateServerDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, description ->
                scope.launch {
                    when (val result = repository.createCommunityServer(name, description)) {
                        is AppResult.Success -> {
                            showCreate = false
                            error = null
                            selectedServer = result.data
                        }
                        is AppResult.Error -> error = result.error.message
                    }
                }
            }
        )
    }

}

@Composable
private fun CreateServerDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni sunucu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { if (it.length <= 40) name = it }, label = { Text("Sunucu adı") }, singleLine = true)
                OutlinedTextField(description, { if (it.length <= 160) description = it }, label = { Text("Açıklama") }, minLines = 2, maxLines = 3)
            }
        },
        confirmButton = {
            TextButton(enabled = name.trim().length >= 2, onClick = { onCreate(name, description) }) { Text("Oluştur") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun ServerChatScreen(
    server: CommunityServer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = ServiceLocator.chatRepository
    val scope = rememberCoroutineScope()
    var messages by remember(server.id) { mutableStateOf<List<ServerMessage>>(emptyList()) }
    var error by remember(server.id) { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var showMembers by remember { mutableStateOf(false) }
    var members by remember { mutableStateOf<List<ServerMember>>(emptyList()) }
    val listState = rememberLazyListState()
    val currentUid = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()

    LaunchedEffect(server.id) {
        repository.observeServerMembers(server.id).collect { result ->
            if (result is AppResult.Success) members = result.data
        }
    }

    LaunchedEffect(server.id) {
        repository.observeServerMessages(server.id).collect { result ->
            when (result) {
                is AppResult.Success -> messages = result.data
                is AppResult.Error -> error = result.error.message
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Column(Modifier.weight(1f)) {
                Text(server.name, fontWeight = FontWeight.Bold)
                Text("${members.size} üye • Topluluk sohbeti", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showMembers = true }) {
                Icon(Icons.Default.People, "Üyeler")
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.senderId == currentUid) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (message.senderId == currentUid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                            if (message.senderId != currentUid) {
                                Text(message.senderName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                            }
                            Text(message.text)
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= 1000) draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Topluluğa mesaj yaz…") },
                maxLines = 4
            )
            IconButton(
                enabled = draft.isNotBlank(),
                onClick = {
                    val text = draft
                    draft = ""
                    scope.launch {
                        when (val result = repository.sendServerMessage(server.id, text)) {
                            is AppResult.Success -> error = null
                            is AppResult.Error -> error = result.error.message
                        }
                    }
                }
            ) { Icon(Icons.Default.Send, "Gönder") }
        }
    }
    if (showMembers) {
        AlertDialog(
            onDismissRequest = { showMembers = false },
            title = { Text("Üyeler (${members.size})") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(members, key = { it.uid }) { member ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(member.displayName.ifBlank { member.uid }, fontWeight = FontWeight.Bold)
                                Text(member.role, style = MaterialTheme.typography.labelSmall)
                            }
                            if (server.ownerId == currentUid && member.uid != currentUid) {
                                TextButton(onClick = {
                                    scope.launch {
                                        repository.setServerMemberRole(server.id, member.uid, if (member.role == "ADMIN") "MEMBER" else "ADMIN")
                                    }
                                }) { Text(if (member.role == "ADMIN") "Üyeye indir" else "Admin yap") }
                                TextButton(onClick = {
                                    scope.launch { repository.removeServerMember(server.id, member.uid) }
                                }) { Text("Çıkar") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMembers = false }) { Text("Kapat") } }
        )
    }
}