package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Listening Challenge View
 * User bấm nút nghe -> TTS phát câu -> Sắp xếp từ -> Kiểm tra đáp án
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListeningChallengeView(
    sentenceToListen: String,
    availableTokens: List<String>,
    selectedTokenIndices: List<Int>,
    isChecked: Boolean,
    isCorrect: Boolean,
    isEnglish: Boolean,
    onTokenSelected: (Int) -> Unit,
    onTokenRemoved: (Int) -> Unit,
    onPlayAudio: () -> Unit,
    onPlaySlow: () -> Unit,
    isSpeaking: Boolean = false
) {
    val haptic = LocalHapticFeedback.current

    // Pulse animation for audio button when idle (encourage user to press)
    val infiniteTransition = rememberInfiniteTransition(label = "audio_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    var hasListened by remember { mutableStateOf(false) }

    // When checked, reveal the audio play one more time
    val assemblyBorderColor by animateColorAsState(
        targetValue = when {
            isChecked && isCorrect -> Color(0xFF22C55E)
            isChecked && !isCorrect -> Color(0xFFEF4444)
            selectedTokenIndices.isNotEmpty() -> Color(0xFF51FAC1)
            else -> Color(0x33FFFFFF)
        },
        animationSpec = tween(250),
        label = "assembly_border"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x3300B0FF))
                    .border(1.dp, Color(0x6600B0FF), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Headset,
                        contentDescription = null,
                        tint = Color(0xFF00B0FF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "LISTENING CHALLENGE 🎧" else "NGHE & SẮP XẾP CÂU 🎧",
                        color = Color(0xFF00B0FF),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Audio player card - central visual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A1628), Color(0xFF0D1F3C))
                    )
                )
                .border(
                    1.5.dp,
                    if (hasListened) Color(0xFF00B0FF) else Color(0x5500B0FF),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Instruction text
                Text(
                    text = if (isEnglish)
                        "Press play to listen to the sentence, then arrange the words below"
                    else
                        "Nhấn nút phát để nghe câu, sau đó sắp xếp các từ bên dưới",
                    color = Color(0xB3FFFFFF),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Audio buttons row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Main play button - animated pulse
                    Box(
                        modifier = Modifier
                            .scale(if (!hasListened && !isSpeaking) pulseScale else 1f)
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF1565C0), Color(0xFF0D47A1))
                                )
                            )
                            .border(2.dp, Color(0xFF42A5F5), CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                hasListened = true
                                onPlayAudio()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                            contentDescription = "Play audio",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Slow play button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x2242A5F5))
                            .border(1.dp, Color(0x6642A5F5), CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                hasListened = true
                                onPlaySlow()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐢", fontSize = 22.sp)
                    }
                }

                // Status text
                if (isSpeaking) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Audio wave animation dots
                        repeat(3) { i ->
                            val dotAnim by rememberInfiniteTransition(label = "dot_$i").animateFloat(
                                initialValue = 4f, targetValue = 12f,
                                animationSpec = infiniteRepeatable(tween(400, delayMillis = i * 150), RepeatMode.Reverse),
                                label = "dot_height_$i"
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .width(3.dp)
                                    .height(dotAnim.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF42A5F5))
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "Playing..." else "Đang phát...",
                            color = Color(0xFF42A5F5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (hasListened) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isEnglish) "Now arrange the words ↓" else "Bây giờ hãy sắp xếp các từ ↓",
                        color = Color(0xFF51FAC1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Assembly Area (word arrangement area)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 90.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    when {
                        isChecked && isCorrect -> Color(0x2222C55E)
                        isChecked && !isCorrect -> Color(0x22EF4444)
                        else -> Color(0x1F14142B)
                    }
                )
                .border(1.5.dp, assemblyBorderColor, RoundedCornerShape(20.dp))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selectedTokenIndices.isEmpty()) {
                Text(
                    text = if (isEnglish) "Tap words below to assemble your answer 👇" else "Chạm vào các từ bên dưới để xếp câu trả lời 👇",
                    color = Color(0x66FFFFFF),
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedTokenIndices.forEachIndexed { position, originalTokenIndex ->
                        val word = availableTokens.getOrNull(originalTokenIndex) ?: ""
                        ListeningTokenChip(
                            text = word,
                            isInAssembly = true,
                            isChecked = isChecked,
                            isCorrect = isCorrect,
                            onClick = {
                                if (!isChecked) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTokenRemoved(position)
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Word Bank
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEnglish) "WORD BANK" else "KHO TỪ VỰNG",
                color = Color(0x9900B0FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.weight(1f).height(1.dp).background(Color(0x2200B0FF))
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            availableTokens.forEachIndexed { index, word ->
                val isUsed = selectedTokenIndices.contains(index)
                if (isUsed) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x0AFFFFFF))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = word, color = Color(0x22FFFFFF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    ListeningTokenChip(
                        text = word,
                        isInAssembly = false,
                        isChecked = isChecked,
                        isCorrect = isCorrect,
                        onClick = {
                            if (!isChecked) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTokenSelected(index)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ListeningTokenChip(
    text: String,
    isInAssembly: Boolean,
    isChecked: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit
) {
    val (bgColor, borderColor, textColor) = when {
        isInAssembly && isChecked && isCorrect -> Triple(Color(0xFF22C55E), Color(0xFF22C55E), Color(0xFF0D0A1A))
        isInAssembly && isChecked && !isCorrect -> Triple(Color(0xFFEF4444), Color(0xFFEF4444), Color.White)
        isInAssembly -> Triple(Color(0xFF1565C0), Color(0xFF42A5F5), Color.White)
        else -> Triple(Color(0x261E1E38), Color(0x6642A5F5), Color(0xCCFFFFFF))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = text, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (isInAssembly && !isChecked) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
