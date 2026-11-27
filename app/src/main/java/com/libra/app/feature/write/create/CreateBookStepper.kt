package com.libra.app.feature.write.create

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libra.app.ui.theme.LibraLavender
import com.libra.app.ui.theme.LibraLavenderBorder
import com.libra.app.ui.theme.LibraLavenderLight
import com.libra.app.ui.theme.LibraLavenderSurface
import com.libra.app.ui.theme.LibraLavenderText

@Composable
fun CreateBookStepper(
    currentStep: Int,
    onStepClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val stages = CreateBookStage.values()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Horizontal Step Indicators with Connecting Lines
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                stages.forEachIndexed { index, stage ->
                    val isCurrent = stage.stepNumber == currentStep
                    val isPassed = stage.stepNumber < currentStep
                    val stepNumber = stage.stepNumber

                    // Step Node
                    val bgColor by animateColorAsState(
                        targetValue = when {
                            isCurrent -> LibraLavenderSurface
                            isPassed -> LibraLavender
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        label = "nodeBg"
                    )

                    val borderColor by animateColorAsState(
                        targetValue = when {
                            isCurrent -> LibraLavender
                            isPassed -> LibraLavender
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        },
                        label = "nodeBorder"
                    )

                    val textColor by animateColorAsState(
                        targetValue = when {
                            isCurrent -> LibraLavenderText
                            isPassed -> Color.White
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "nodeText"
                    )

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .border(1.5.dp, borderColor, CircleShape)
                            .clickable(enabled = isPassed) { onStepClick(stepNumber) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPassed) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Tamamlandı",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = stepNumber.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }

                    // Connecting Line (except after last item)
                    if (index < stages.size - 1) {
                        val lineFill by animateColorAsState(
                            targetValue = if (stage.stepNumber < currentStep) LibraLavender else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            label = "lineFill"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .padding(horizontal = 6.dp)
                                .background(lineFill, RoundedCornerShape(1.dp))
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Current Step Header Text
            val currentStage = stages.firstOrNull { it.stepNumber == currentStep } ?: CreateBookStage.BASIC_INFO
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Aşama $currentStep / 4: ${currentStage.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentStage.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LibraLavenderSurface.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = when (currentStep) {
                            1 -> "%25"
                            2 -> "%50"
                            3 -> "%75"
                            else -> "%100"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = LibraLavenderText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
