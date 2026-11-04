package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LessonPerformanceEvaluator
import com.example.model.LessonPerformanceTier
import com.example.model.MascotEmotion
import com.example.ui.mascot.LivingMascotView
import com.example.viewmodel.StudyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun RankRevealScreen(
    viewModel: StudyViewModel,
    onEnterRealm: () -> Unit,
    onViewAnalysis: () -> Unit,
    onRetryLesson: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current

    val lastResults by viewModel.lastQuestResults.observeAsState(emptyList())
    val unlockEvent by viewModel.chapterUnlockEvent.observeAsState(null)
    val isGeneratingNextChapter by viewModel.isGeneratingNextChapter.observeAsState(false)

    val correctCount = lastResults.count { it.selectedIndex == it.correctIndex }
    val totalCount = lastResults.size
    val tier = remember(correctCount, totalCount) {
        LessonPerformanceEvaluator.evaluate(correctCount, totalCount)
    }
    val accuracy = remember(correctCount, totalCount) {
        LessonPerformanceEvaluator.calculateAccuracy(correctCount, totalCount)
    }
    val xpReward = remember(tier, correctCount) {
        LessonPerformanceEvaluator.calculateXpReward(tier, correctCount)
    }
    val isPassed = remember(tier) {
        LessonPerformanceEvaluator.isPassed(tier)
    }

    // Smooth continuing transition state (Đẳng cấp, không xoay vặn, không nháy trắng)
    var isContinuing by remember { mutableStateOf(false) }

    val exitAlpha by animateFloatAsState(
        targetValue = if (isContinuing) 0f else 1f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "exitAlpha"
    )

    val exitScale by animateFloatAsState(
        targetValue = if (isContinuing) 1.05f else 1f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "exitScale"
    )

    val shockwaveScale by animateFloatAsState(
        targetValue = if (isContinuing) 2.4f else 0.8f,
        animationSpec = tween(durationMillis = 360, easing = LinearOutSlowInEasing),
        label = "shockwaveScale"
    )

    val shockwaveAlpha by animateFloatAsState(
        targetValue = if (isContinuing) 0f else 0.7f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "shockwaveAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "rank_anim")
    
    val floatAnimSlow by infiniteTransition.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float_slow"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.90f, targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_glow"
    )

    val glowColor = when (tier) {
        LessonPerformanceTier.EXCELLENT -> Color(0xFFFFD166)
        LessonPerformanceTier.GOOD -> Color(0xFF51FAC1)
        LessonPerformanceTier.NEEDS_WORK -> Color(0xFFFF6B6B)
    }

    val subtitleText = when (tier) {
        LessonPerformanceTier.EXCELLENT -> strings.tierExcellentSubtitle
        LessonPerformanceTier.GOOD -> strings.tierGoodSubtitle
        LessonPerformanceTier.NEEDS_WORK -> strings.tierNeedsWorkSubtitle
    }

    val titleText = when (tier) {
        LessonPerformanceTier.EXCELLENT -> strings.tierExcellentTitle
        LessonPerformanceTier.GOOD -> strings.tierGoodTitle
        LessonPerformanceTier.NEEDS_WORK -> strings.tierNeedsWorkTitle
    }

    val descText = when (tier) {
        LessonPerformanceTier.EXCELLENT -> strings.tierExcellentDesc
        LessonPerformanceTier.GOOD -> strings.tierGoodDesc
        LessonPerformanceTier.NEEDS_WORK -> strings.tierNeedsWorkDesc
    }

    val mascotEmotion = when (tier) {
        LessonPerformanceTier.EXCELLENT -> MascotEmotion.CHEER
        LessonPerformanceTier.GOOD -> MascotEmotion.STREAK
        LessonPerformanceTier.NEEDS_WORK -> MascotEmotion.EMPATHY
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = exitAlpha
                scaleX = exitScale
                scaleY = exitScale
            }
    ) {
        // Subtle ambient spotlight centered behind mascot and victory title
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .size(340.dp)
                .scale(pulseGlow)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.22f),
                            glowColor.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
                .blur(36.dp)
        )

        // Main UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Avatar Section: LivingMascotView in a sleek celestial glass portal
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .offset(y = floatAnimSlow.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer celestial breathing glow
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(pulseGlow)
                        .background(
                            Brush.radialGradient(
                                listOf(glowColor.copy(alpha = 0.32f), Color.Transparent)
                            ),
                            CircleShape
                        )
                        .blur(20.dp)
                )

                // Sleek obsidian glass disk
                Box(
                    modifier = Modifier
                        .size(164.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x331C1635), Color(0x660E0B1C))
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    glowColor.copy(alpha = 0.85f),
                                    Color(0x33FFFFFF),
                                    glowColor.copy(alpha = 0.85f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    LivingMascotView(
                        emotion = mascotEmotion,
                        size = 126.dp,
                        showBadge = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subtitle Tag / Badge (Sleek minimalist capsule)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(glowColor.copy(alpha = 0.12f))
                    .border(1.dp, glowColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = subtitleText,
                    color = glowColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Title: Pure typography with soft celestial shadow (KHÔNG VIỀN VUÔNG, KHÔNG SHADOW ELEVATION)
            Text(
                text = titleText,
                color = Color.White,
                fontSize = 35.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 41.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = glowColor.copy(alpha = 0.40f),
                        blurRadius = 28f,
                        offset = Offset(0f, 3f)
                    )
                )
            )

            // Description
            Text(
                text = descText,
                color = Color(0xFFD5CEE4),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 20.dp)
                    .widthIn(max = 310.dp)
            )

            // Adaptive Stats Cards: Dark Obsidian Glassmorphism (Không để lọt thiên thạch/hạt phía sau gây bẩn số liệu)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xF518142A), Color(0xF5100D1E))
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                glowColor.copy(alpha = 0.55f),
                                Color(0x33FFFFFF),
                                glowColor.copy(alpha = 0.20f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Accuracy
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.accuracyLabel.uppercase(),
                        color = Color(0x8AFFFFFF),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "$accuracy%",
                        color = glowColor,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(Color(0x29FFFFFF))
                )

                // 2. Correct Count
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.correctLabel.uppercase(),
                        color = Color(0x8AFFFFFF),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "$correctCount / $totalCount",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(Color(0x29FFFFFF))
                )

                // 3. XP Reward
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.xpEarnedLabel.uppercase(),
                        color = Color(0x8AFFFFFF),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "+$xpReward XP",
                        color = Color(0xFFFFD166),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Chapter Completion & Next Chapter Unlock Banner
            if (isPassed && (unlockEvent != null || isGeneratingNextChapter)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E1138), Color(0xFF0F1E36))
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                if (isGeneratingNextChapter)
                                    listOf(Color(0xFF00F5D4), Color(0xFF8A2BE2), Color(0xFF00F5D4))
                                else
                                    listOf(Color(0xFFFFD166), Color(0xFF51FAC1), Color(0xFFFFD166))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(15.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isGeneratingNextChapter) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color(0xFF00F5D4),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = strings.aiGeneratingNextChapter,
                                    color = Color(0xFF00F5D4),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (unlockEvent != null) {
                            Text(
                                text = strings.chapterClearedTitle,
                                color = Color(0xFFFFD166),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${strings.nextChapterUnlockedTitle}: ${unlockEvent!!.nextChapterTitle}",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            if (unlockEvent!!.isAiGenerated) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (unlockEvent!!.weakPointsAddressedCount > 0)
                                        strings.aiReinforceWeakPointsDesc.replace("{count}", unlockEvent!!.weakPointsAddressedCount.toString())
                                    else
                                        strings.aiGeneratedNextChapterSuccess,
                                    color = Color(0xFF51FAC1),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (tier == LessonPerformanceTier.NEEDS_WORK) {
                    // NEEDS_WORK: Nút chính là LUYỆN TẬP LẠI
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF5722), Color(0xFFFF9800), Color(0xFFFF5722))
                                )
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRetryLesson()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.retryLesson,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Nút phụ: Xem phân tích AI
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(CircleShape)
                            .background(Color(0x2251FAC1))
                            .border(1.2.dp, Color(0xFF51FAC1), CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onViewAnalysis()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF51FAC1),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.checkAIAnalysis,
                                color = Color(0xFF51FAC1),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Link phụ: Về bản đồ học
                    TextButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onEnterRealm()
                    }) {
                        Text(
                            text = strings.backToMap,
                            color = Color(0x99FFFFFF),
                            fontSize = 13.5.sp
                        )
                    }
                } else {
                    // PASSED / EXCELLENT: Nút Xem phân tích AI (Glassmorphism tinh tế)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(Color(0x1F00F5D4))
                            .border(1.2.dp, Color(0x6600F5D4), CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onViewAnalysis()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF00F5D4),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.checkAIAnalysis,
                                color = Color(0xFF00F5D4),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Nút chính: TIẾP TỤC (Đẳng cấp, hiệu ứng nhấn mượt mà, chuyển cảnh êm ái)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFBF38), Color(0xFFFF9500), Color(0xFFFFBF38))
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isContinuing) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.soundEffectManager.playCorrect()
                                    isContinuing = true
                                    coroutineScope.launch {
                                        delay(360)
                                        onEnterRealm()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = strings.continueJourney,
                                color = Color(0xFF2B1B00),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.5.sp,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF2B1B00),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Elegant expanding cosmic shockwave ring on continue (không nhấp nháy, không chói mắt)
        if (isContinuing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = shockwaveScale
                        scaleY = shockwaveScale
                        alpha = shockwaveAlpha
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(CircleShape)
                        .border(2.dp, glowColor.copy(alpha = 0.8f), CircleShape)
                )
            }
        }
    }
}
