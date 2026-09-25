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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val adminSections = listOf(
    "Üye işlemleri",
    "Kitap işlemleri",
    "Gönderi işlemleri",
    "Hikâye işlemleri",
    "Sunucu işlemleri",
    "Genel chat işlemleri"
)

@Composable
fun AdminPanelScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        "Yönetim",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        "Libra yönetim merkezi",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF777777)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            adminSections.forEach { title ->
                AdminSectionRow(title)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun AdminSectionRow(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF7F7F7),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { }
            .padding(horizontal = 18.dp, vertical = 19.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
        Icon(
            Icons.Default.Settings,
            contentDescription = null,
            tint = Color(0xFF777777)
        )
    }
}
