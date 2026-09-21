package com.libra.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class AppButtonVariant { PRIMARY, SECONDARY, OUTLINED, TEXT }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    testTag: String = "app_button"
) {
    val shape = RoundedCornerShape(12.dp)
    val buttonModifier = modifier.height(50.dp).testTag(testTag)
    when (variant) {
        AppButtonVariant.PRIMARY -> Button(onClick, enabled = enabled && !isLoading, shape = shape, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary), modifier = buttonModifier) {
            ButtonContent(text, icon, isLoading, MaterialTheme.colorScheme.onPrimary)
        }
        AppButtonVariant.SECONDARY -> Button(onClick, enabled = enabled && !isLoading, shape = shape, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary), modifier = buttonModifier) {
            ButtonContent(text, icon, isLoading, MaterialTheme.colorScheme.onSecondary)
        }
        AppButtonVariant.OUTLINED -> OutlinedButton(onClick, enabled = enabled && !isLoading, shape = shape, modifier = buttonModifier) {
            ButtonContent(text, icon, isLoading, MaterialTheme.colorScheme.primary)
        }
        AppButtonVariant.TEXT -> TextButton(onClick, enabled = enabled && !isLoading, shape = shape, modifier = buttonModifier) {
            ButtonContent(text, icon, isLoading, MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, isLoading: Boolean, contentColor: Color) {
    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = contentColor, strokeWidth = 2.dp)
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}
