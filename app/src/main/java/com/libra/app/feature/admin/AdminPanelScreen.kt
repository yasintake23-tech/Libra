package com.libra.app.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val sections = listOf("Üye işlemleri","Kitap işlemleri","Gönderi işlemleri","Hikâye işlemleri","Sunucu işlemleri","Genel chat işlemleri")

@Composable
fun AdminPanelScreen(onBack: () -> Unit, modifier: Modifier = Modifier, permissions: AdminPermissionSet = AdminPermissionSet(true,true,true,true,true,true,true,true,true,true,true), vm: AdminViewModel = viewModel()) {
    var section by remember { mutableStateOf<String?>(null) }
    var member by remember { mutableStateOf<UserProfile?>(null) }
    if (member != null) {
        MemberAdminScreen(member!!, vm, { member = null }, permissions, modifier)
    } else if (section == "Üye işlemleri") {
        MembersScreen(vm.members.collectAsState().value, { section = null }, { member = it }, modifier, permissions.manageMembers)
    } else {
        Surface(modifier.fillMaxSize(), color = Color.White) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
                    Column {
                        Text("Yönetim", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                        Text("Libra yönetim merkezi", color = Color.Gray)
                    }
                }
                Spacer(Modifier.height(28.dp))
                sections.forEach { title ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).background(Color(0xFFF7F7F7), RoundedCornerShape(18.dp)).clickable(enabled = title == "Üye işlemleri" && permissions.manageMembers) { section = title }.padding(18.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.Settings, null, tint = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable private fun MembersScreen(members: List<UserProfile>, onBack: () -> Unit, onSelect: (UserProfile) -> Unit, modifier: Modifier, enabled: Boolean) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Tümü") }
    val now = System.currentTimeMillis()
    val list = members.filter { u ->
        val q = query.trim().lowercase()
        val match = q.isBlank() || u.displayName.lowercase().contains(q) || u.username.lowercase().contains(q) || u.email.lowercase().contains(q)
        val date = when(filter) {
            "Bugün" -> u.createdAt >= now - 86400000L
            "7 gün" -> u.createdAt >= now - 7L*86400000L
            "30 gün" -> u.createdAt >= now - 30L*86400000L
            else -> true
        }
        match && date
    }
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Text("Üye işlemleri", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("İsim, kullanıcı adı veya e-posta ara") })
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Tümü","Bugün","7 gün","30 gün").forEach { FilterChip(filter == it, { filter = it }, label = { Text(it) }) }
        }
        Text(list.size.toString() + " üye", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.uid }) { u ->
                Row(Modifier.fillMaxWidth().background(Color(0xFFF7F7F7), RoundedCornerShape(16.dp)).clickable { onSelect(u) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(u.displayName.ifBlank { "İsimsiz üye" }, fontWeight = FontWeight.SemiBold)
                        Text("@" + u.username, color = Color.Gray)
                        Text(u.email, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                    if (u.moderation.isBanned) Text("BAN", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable private fun MemberAdminScreen(member: UserProfile, vm: AdminViewModel, onBack: () -> Unit, permissions: AdminPermissionSet, modifier: Modifier) {
    var current by remember(member) { mutableStateOf(member) }
    var dialog by remember { mutableStateOf<String?>(null) }
    Surface(modifier.fillMaxSize(), color = Color.White) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
                Column {
                    Text(current.displayName, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    Text("@" + current.username, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(18.dp))
            listOf("Ban ve erişim işlemleri" to permissions.manageBans,"Hesap ve üyelik işlemleri" to permissions.editProfiles,"Kitap işlemleri" to permissions.manageBooks,"Yönetici rolü verme işlemleri" to permissions.manageAdminRoles,"Kozmetik rol" to permissions.manageCosmetics,"Kullanıcıya geri bildirim / DM" to permissions.sendFeedback).filter { it.second }.forEach { (title, allowed) ->
                Row(Modifier.fillMaxWidth().padding(bottom=10.dp).background(Color(0xFFF7F7F7), RoundedCornerShape(18.dp)).clickable { dialog=title }.padding(18.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(title, fontWeight=FontWeight.SemiBold)
                    Icon(Icons.Default.Settings,null,tint=Color.Gray)
                }
            }
        }
    }
    when(dialog) {
        "Ban ve erişim işlemleri" -> ModerationDialog(current.moderation,{dialog=null}) { m -> current=current.copy(moderation=m); vm.saveModeration(current.uid,m) }
        "Hesap ve üyelik işlemleri" -> ProfileDialog(current,{dialog=null}) { p -> current=p; vm.saveProfile(p) }
        "Kitap işlemleri" -> InfoDialog("Kitap işlemleri","Yakında... Kitap mekaniği hazır olduğunda bağlanacak.",{dialog=null})
        "Yönetici rolü verme işlemleri" -> AdminRoleDialog(current.uid,vm,{dialog=null})
        "Kozmetik rol" -> CosmeticDialog(current,vm,{dialog=null})
        "Kullanıcıya geri bildirim / DM" -> ContactDialog(current,{dialog=null})
    }
}

@Composable private fun ModerationDialog(m: UserModeration, dismiss:()->Unit, save:(UserModeration)->Unit) {
    var x by remember { mutableStateOf(m) }
    var reason by remember { mutableStateOf(m.banReason) }
    var duration by remember { mutableStateOf("Kalıcı") }
    AlertDialog(onDismissRequest=dismiss,title={Text("Ban ve erişim işlemleri")},text={ Column(Modifier.heightIn(max=560.dp)) {
        OutlinedTextField(reason,{reason=it},Modifier.fillMaxWidth(),label={Text("Ban sebebi")})
        Text("Ban süresi", fontWeight=FontWeight.Bold, modifier=Modifier.padding(top=8.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("1 saat","1 gün","7 gün","Kalıcı").forEach { FilterChip(duration == it, { duration = it }, label = { Text(it) }) } }
        SwitchRow("Ban",x.banned){x=x.copy(banned=it)}
        SwitchRow("Gönderi paylaşma engeli",x.postingDisabled){x=x.copy(postingDisabled=it)}
        SwitchRow("Mesaj atma engeli",x.messagingDisabled){x=x.copy(messagingDisabled=it)}
        SwitchRow("Hikâye paylaşma engeli",x.storyDisabled){x=x.copy(storyDisabled=it)}
        SwitchRow("Yorum yapma engeli",x.commentingDisabled){x=x.copy(commentingDisabled=it)}
        SwitchRow("Kitap paylaşma engeli",x.bookPublishingDisabled){x=x.copy(bookPublishingDisabled=it)}
        SwitchRow("Sunucu oluşturma engeli",x.serverCreationDisabled){x=x.copy(serverCreationDisabled=it)}
        SwitchRow("Sunucu/genel chat mesaj engeli",x.serverMessagingDisabled){x=x.copy(serverMessagingDisabled=it)}
    }},confirmButton={TextButton({val until=when(duration){"1 saat"->System.currentTimeMillis()+3600000L;"1 gün"->System.currentTimeMillis()+86400000L;"7 gün"->System.currentTimeMillis()+7L*86400000L;else->0L};save(x.copy(banReason=reason,banUntil=if(x.banned)until else 0L));dismiss()}){Text("Kaydet")}},dismissButton={TextButton(dismiss){Text("İptal")}})
}

@Composable private fun SwitchRow(t:String,c:Boolean,change:(Boolean)->Unit){
    Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){Text(t,Modifier.weight(1f));Switch(c,change)}
}

@Composable private fun ProfileDialog(p:UserProfile,dismiss:()->Unit,save:(UserProfile)->Unit){
    var n by remember{mutableStateOf(p.displayName)}
    var u by remember{mutableStateOf(p.username)}
    var b by remember{mutableStateOf(p.bio)}
    var photo by remember{mutableStateOf(p.profileImageUrl)}
    val scope = rememberCoroutineScope()
    var resetStatus by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=dismiss,title={Text("Hesap ve üyelik işlemleri")},text={Column{
        OutlinedTextField(n,{n=it},label={Text("Hesap ismi")})
        OutlinedTextField(u,{u=it},label={Text("Kullanıcı adı")})
        OutlinedTextField(b,{b=it},label={Text("Hakkımda")})
        OutlinedTextField(photo,{photo=it},label={Text("Profil fotoğrafı URL")})
        TextButton(onClick={scope.launch{runCatching{com.google.firebase.auth.FirebaseAuth.getInstance().sendPasswordResetEmail(p.email).await()}.onSuccess{resetStatus="Şifre yenileme bağlantısı gönderildi."}.onFailure{resetStatus="Bağlantı gönderilemedi."}}}){Text("Şifre değiştirme bağlantısı gönder")}
        if(resetStatus.isNotBlank()) Text(resetStatus,color=MaterialTheme.colorScheme.primary)
    }},confirmButton={TextButton({save(p.copy(displayName=n,username=u,bio=b,profileImageUrl=photo));dismiss()}){Text("Kaydet")}},dismissButton={TextButton(dismiss){Text("İptal")}})
}

@Composable private fun AdminRoleDialog(uid:String,vm:AdminViewModel,dismiss:()->Unit){
    var role by remember{mutableStateOf<AdminRole?>(null)}
    var name by remember{mutableStateOf("")}
    var perms by remember{mutableStateOf(AdminPermissionSet())}
    LaunchedEffect(uid){role=vm.loadAdminRole(uid);role?.let{name=it.name;perms=it.permissions}}
    AlertDialog(onDismissRequest=dismiss,title={Text("Yönetici rolü")},text={Column(Modifier.heightIn(max=560.dp)){
        OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Rol adı")})
        SwitchRow("Üye yönetimi",perms.manageMembers){perms=perms.copy(manageMembers=it)}
        SwitchRow("Ban/susturma",perms.manageBans){perms=perms.copy(manageBans=it)}
        SwitchRow("Profil düzenleme",perms.editProfiles){perms=perms.copy(editProfiles=it)}
        SwitchRow("Kitap yönetimi",perms.manageBooks){perms=perms.copy(manageBooks=it)}
        SwitchRow("Gönderi yönetimi",perms.managePosts){perms=perms.copy(managePosts=it)}
        SwitchRow("Hikâye yönetimi",perms.manageStories){perms=perms.copy(manageStories=it)}
        SwitchRow("Sunucu yönetimi",perms.manageServers){perms=perms.copy(manageServers=it)}
        SwitchRow("Genel chat",perms.manageGlobalChat){perms=perms.copy(manageGlobalChat=it)}
        SwitchRow("Yönetici rolü",perms.manageAdminRoles){perms=perms.copy(manageAdminRoles=it)}
        SwitchRow("Kozmetik rol",perms.manageCosmetics){perms=perms.copy(manageCosmetics=it)}
        SwitchRow("Geri bildirim",perms.sendFeedback){perms=perms.copy(sendFeedback=it)}
    }},confirmButton={TextButton({vm.saveAdminRole(uid,AdminRole(name=name.ifBlank{"Özel rol"},permissions=perms));dismiss()}){Text("Kaydet")}},dismissButton={TextButton({vm.saveAdminRole(uid,null);dismiss()}){Text("Rolü kaldır")}})
}

@Composable private fun CosmeticDialog(p:UserProfile,vm:AdminViewModel,dismiss:()->Unit){
    val roles=vm.cosmeticRoles.collectAsState().value
    val assigned=p.cosmeticRoleIds.toSet()
    var name by remember{mutableStateOf("")}
    var icon by remember{mutableStateOf("✦")}
    AlertDialog(onDismissRequest=dismiss,title={Text("Kozmetik roller")},text={Column{
        roles.forEach{r->SwitchRow(r.icon+" "+r.name,r.id in assigned){vm.assignCosmetic(p.uid,r.id,it)}}
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(name,{name=it},label={Text("Yeni rozet adı")})
        OutlinedTextField(icon,{icon=it},label={Text("Rozet simgesi")})
        Text("Rozetler kullanıcının adının yanında gösterilecek ve özel animasyon sistemine bağlanacak.")
    }},confirmButton={TextButton({if(name.isNotBlank())vm.saveCosmetic(CosmeticRole(name=name,icon=icon));dismiss()}){Text("Oluştur / Kapat")}})
}

@Composable private fun ContactDialog(p:UserProfile,dismiss:()->Unit){
    val scope=rememberCoroutineScope()
    var text by remember{mutableStateOf("")}
    var status by remember{mutableStateOf("")}
    val me=com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    AlertDialog(onDismissRequest=dismiss,title={Text("Kullanıcıya geri bildirim / DM")},text={Column{
        OutlinedTextField(text,{text=it},Modifier.fillMaxWidth(),label={Text("Mesaj")})
        Text(status)
    }},confirmButton={Row{
        TextButton(enabled=text.isNotBlank(),onClick={scope.launch{
            val r=ServiceLocator.notificationRepository.create(AppNotification(recipientId=p.uid,actorId=me,actorName="Libra Yönetim",type="ADMIN_FEEDBACK",title="Yönetimden bildirim",body=text.trim(),createdAt=System.currentTimeMillis()))
            status=if(r is AppResult.Success)"Bildirim gönderildi." else "Bildirim gönderilemedi."
        }}) { Text("Bildirim gönder") }
        TextButton(enabled=text.isNotBlank(),onClick={scope.launch{
            val r=ServiceLocator.chatRepository.sendDirectMessage(p.uid,text.trim())
            status=if(r is AppResult.Success)"DM gönderildi." else "DM gönderilemedi."
        }}) { Text("DM gönder") }
    }},dismissButton={TextButton(dismiss){Text("Kapat")}})
}

@Composable private fun InfoDialog(title:String,message:String,dismiss:()->Unit){
    AlertDialog(onDismissRequest=dismiss,title={Text(title)},text={Text(message)},confirmButton={TextButton(dismiss){Text("Tamam")}})
}