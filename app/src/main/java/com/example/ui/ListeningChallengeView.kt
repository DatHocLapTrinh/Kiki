package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    onPlayAudio: () -> Unit = {},
    onPlaySlow: () -> Unit = {},
    isSpeaking: Boolean = false
) {
    val haptic = LocalHapticFeedback.current

    val assemblyBorderColor by animateColorAsState(
        targetValue = when {
            isChecked && isCorrect -> Color(0xFF22C55E)
            isChecked && !isCorrect -> Color(0xFFEF4444)
            selectedTokenIndices.isNotEmpty() -> Color(0xFF00B0FF)
            else -> Color(0x33FFFFFF)
        },
        animationSpec = tween(250),
        label = "assembly_border"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Assembly Area (Vùng lắp ghép câu nghe được)
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
                    text = if (isEnglish) "Tap words below to assemble your answer 👇" else "Chạm vào các từ bên dưới để ghép câu nghe được 👇",
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

        // Word Tokens Bank (Kho từ vựng chọn câu)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
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
                        Text(
                            text = word,
                            color = Color(0x22FFFFFF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
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
            Text(
                text = text,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
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
