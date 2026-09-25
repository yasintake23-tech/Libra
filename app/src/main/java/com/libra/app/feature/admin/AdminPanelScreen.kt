package com.libra.app.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class AdminSection(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun AdminPanelScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sections = listOf(
        AdminSection("Üye işlemleri", Icons.Default.People),
        AdminSection("Kitap işlemleri", Icons.Default.Book),
        AdminSection("Gönderi işlemleri", Icons.Default.PostAdd),
        AdminSection("Hikâye işlemleri", Icons.Default.PhotoLibrary),
        AdminSection("Sunucu işlemleri", Icons.Default.Group),
        AdminSection("Genel chat işlemleri", Icons.Default.Chat)
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.White,
        contentColor = Color(0xFF111111)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color(0xFF111111)
                    )
                }

                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        "Yönetim",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF111111)
                    )
                    Text(
                        "Libra yönetim merkezi",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF777777)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            sections.forEach { section ->
                AdminSectionRow(
                    section = section,
                    onClick = {
                        // Bölümlerin içeriği sonraki aşamada eklenecek.
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun AdminSectionRow(
    section: AdminSection,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF7F7F7),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Row(
            modifier = Modifier
                .size(46.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                section.icon,
                contentDescription = null,
                tint = Color(0xFF111111)
            )
        }

        Text(
            section.title,
            modifier = Modifier.padding(start = 14.dp),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = Color(0xFF111111)
        )
    }
}
