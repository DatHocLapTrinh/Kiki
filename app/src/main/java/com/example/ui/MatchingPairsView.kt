package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MatchingPair
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class CardStatus {
    DEFAULT,
    SELECTED,
    WRONG,
    MATCHED
}

@Composable
fun MatchingPairsView(
    pairs: List<MatchingPair>,
    isEnglish: Boolean,
    onSpeakWord: (String) -> Unit = {},
    onPairMatched: () -> Unit = {},
    onMismatch: () -> Unit = {},
    onAllMatched: (errors: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Stable shuffled cards for the session
    val shuffledEnglish = remember(pairs) { pairs.shuffled() }
    val shuffledVietnamese = remember(pairs) { pairs.shuffled() }

    var selectedEnglishId by remember(pairs) { mutableStateOf<Int?>(null) }
    var selectedVietnameseId by remember(pairs) { mutableStateOf<Int?>(null) }

    var matchedIds by remember(pairs) { mutableStateOf<Set<Int>>(emptySet()) }
    var wrongPair by remember(pairs) { mutableStateOf<Pair<Int, Int>?>(null) }
    var errorCount by remember(pairs) { mutableIntStateOf(0) }

    fun evaluateMatch(engId: Int, vnId: Int) {
        if (engId == vnId) {
            val updatedMatched = matchedIds + engId
            matchedIds = updatedMatched
            selectedEnglishId = null
            selectedVietnameseId = null
            wrongPair = null
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onPairMatched()

            if (updatedMatched.size == pairs.size) {
                onAllMatched(errorCount)
            }
        } else {
            errorCount++
            wrongPair = Pair(engId, vnId)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onMismatch()

            scope.launch {
                delay(550)
                if (wrongPair == Pair(engId, vnId)) {
                    selectedEnglishId = null
                    selectedVietnameseId = null
                    wrongPair = null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Subtle pair counter badge
        if (matchedIds.size < pairs.size) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp, end = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "${matchedIds.size}/${pairs.size} ${if (isEnglish) "Pairs matched" else "Cặp đã ghép"}",
                    color = Color(0x9951FAC1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2-Column Matching Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // English Column (Left)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                shuffledEnglish.forEach { item ->
                    val isMatched = matchedIds.contains(item.id)
                    val isSelected = selectedEnglishId == item.id
                    val isWrong = wrongPair?.first == item.id

                    val status = when {
                        isMatched -> CardStatus.MATCHED
                        isWrong -> CardStatus.WRONG
                        isSelected -> CardStatus.SELECTED
                        else -> CardStatus.DEFAULT
                    }

                    MatchingCard(
                        text = item.english,
                        status = status,
                        isEnglish = true,
                        onClick = {
                            if (!isMatched && wrongPair == null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSpeakWord(item.english)
                                selectedEnglishId = item.id
                                val currVnId = selectedVietnameseId
                                if (currVnId != null) {
                                    evaluateMatch(item.id, currVnId)
                                }
                            }
                        }
                    )
                }
            }

            // Vietnamese Column (Right)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                shuffledVietnamese.forEach { item ->
                    val isMatched = matchedIds.contains(item.id)
                    val isSelected = selectedVietnameseId == item.id
                    val isWrong = wrongPair?.second == item.id

                    val status = when {
                        isMatched -> CardStatus.MATCHED
                        isWrong -> CardStatus.WRONG
                        isSelected -> CardStatus.SELECTED
                        else -> CardStatus.DEFAULT
                    }

                    MatchingCard(
                        text = item.vietnamese,
                        status = status,
                        isEnglish = false,
                        onClick = {
                            if (!isMatched && wrongPair == null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedVietnameseId = item.id
                                val currEngId = selectedEnglishId
                                if (currEngId != null) {
                                    evaluateMatch(currEngId, item.id)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchingCard(
    text: String,
    status: CardStatus,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    val (bgColor, borderColor, textColor, targetAlpha) = when (status) {
        CardStatus.MATCHED -> Quadruple(Color(0x1522C55E), Color(0x4422C55E), Color(0x7751FAC1), 0.45f)
        CardStatus.SELECTED -> Quadruple(Color(0x3351FAC1), Color(0xFF51FAC1), Color.White, 1f)
        CardStatus.WRONG -> Quadruple(Color(0x33EF4444), Color(0xFFEF4444), Color(0xFFFF7A7A), 1f)
        CardStatus.DEFAULT -> Quadruple(Color(0x1F1A1B36), Color(0x33FFFFFF), Color.White, 1f)
    }

    val animatedBg by animateColorAsState(targetValue = bgColor, animationSpec = tween(200), label = "card_bg")
    val animatedBorder by animateColorAsState(targetValue = borderColor, animationSpec = tween(200), label = "card_border")
    val animatedScale by animateFloatAsState(
        targetValue = if (status == CardStatus.SELECTED) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 58.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = targetAlpha
            }
            .clip(RoundedCornerShape(16.dp))
            .background(animatedBg)
            .border(
                width = if (status == CardStatus.SELECTED || status == CardStatus.WRONG) 2.dp else 1.dp,
                color = animatedBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = status != CardStatus.MATCHED, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (status == CardStatus.SELECTED) FontWeight.ExtraBold else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (status == CardStatus.MATCHED) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Matched",
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
