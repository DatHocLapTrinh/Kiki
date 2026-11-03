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
fun SentenceBuilderView(
    promptText: String,
    availableTokens: List<String>,
    selectedTokenIndices: List<Int>,
    isChecked: Boolean,
    isCorrect: Boolean,
    isEnglish: Boolean,
    onTokenSelected: (Int) -> Unit,
    onTokenRemoved: (Int) -> Unit,
    onSpeakPrompt: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Assembly Area (Khu vực lắp ghép câu)
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

        val assemblyBgColor = when {
            isChecked && isCorrect -> Color(0x2222C55E)
            isChecked && !isCorrect -> Color(0x22EF4444)
            else -> Color(0x1F14142B)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 90.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(assemblyBgColor)
                .border(1.5.dp, assemblyBorderColor, RoundedCornerShape(20.dp))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selectedTokenIndices.isEmpty()) {
                Text(
                    text = if (isEnglish) "Tap words below to assemble the sentence 👇" else "Chạm vào các từ bên dưới để ghép thành câu 👇",
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
                        WordTokenChip(
                            text = word,
                            isSelectedInAssembly = true,
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

        // Word Tokens Bank (Kho thẻ từ để chọn)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            availableTokens.forEachIndexed { index, word ->
                val isUsed = selectedTokenIndices.contains(index)
                if (isUsed) {
                    // Ghost Placeholder (giữ nguyên vị trí lưới layout)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x0AFFFFFF))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = word,
                            color = Color(0x22FFFFFF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    // Active Clickable Chip
                    WordTokenChip(
                        text = word,
                        isSelectedInAssembly = false,
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
private fun WordTokenChip(
    text: String,
    isSelectedInAssembly: Boolean,
    isChecked: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit
) {
    val (bgColor, borderColor, textColor) = when {
        isSelectedInAssembly && isChecked && isCorrect -> Triple(Color(0xFF22C55E), Color(0xFF22C55E), Color(0xFF0D0A1A))
        isSelectedInAssembly && isChecked && !isCorrect -> Triple(Color(0xFFEF4444), Color(0xFFEF4444), Color.White)
        isSelectedInAssembly -> Triple(Color(0xFF51FAC1), Color(0xFF51FAC1), Color(0xFF0D0A1A))
        else -> Triple(Color(0x261E1E38), Color(0x6651FAC1), Color.White)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (isSelectedInAssembly && !isChecked) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
