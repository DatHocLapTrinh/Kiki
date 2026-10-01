package com.example.ui.mascot

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MascotEmotion

@Composable
fun MascotSpeechBubble(
    speech: String?,
    emotion: MascotEmotion,
    modifier: Modifier = Modifier,
    onSpeakClick: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = speech != null,
        enter = fadeIn() + expandVertically() + slideInVertically { -20 },
        exit = fadeOut() + shrinkVertically() + slideOutVertically { -20 }
    ) {
        speech?.let { text ->
            val borderColor = when (emotion) {
                MascotEmotion.STREAK -> Color(0xFFFF9100)
                MascotEmotion.CHEER -> Color(0xFFFFD700)
                MascotEmotion.PETTED -> Color(0xFFFF80AB)
                MascotEmotion.LISTENING -> Color(0xFF00E5FF)
                MascotEmotion.EMPATHY -> Color(0xFF81C784)
                MascotEmotion.THINKING -> Color(0xFFFFCA28)
                MascotEmotion.IDLE -> Color(0xFF51FAC1)
            }

            val bgBrush = Brush.linearGradient(
                listOf(
                    borderColor.copy(alpha = 0.22f),
                    Color(0xFF13131F).copy(alpha = 0.95f)
                )
            )

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(bgBrush)
                    .border(1.5.dp, borderColor.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = borderColor,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Kiki: \"$text\"",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )

                    if (onSpeakClick != null) {
                        IconButton(
                            onClick = onSpeakClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Listen",
                                tint = borderColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0x99FFFFFF),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
