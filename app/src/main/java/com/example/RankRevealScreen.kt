package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
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
    val lastResults by viewModel.lastQuestResults.observeAsState(emptyList())

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

    var isWarping by remember { mutableStateOf(false) }
    var warpSpeed by remember { mutableFloatStateOf(0f) }
    var whiteoutAlpha by remember { mutableFloatStateOf(0f) }
    var zoomOutScale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "rank_anim")
    
    val floatAnimSlow by infiniteTransition.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "float_slow"
    )
    
    val floatAnimFast by infiniteTransition.animateFloat(
        initialValue = -15f, targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse),
        label = "float_fast"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
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
        LessonPerformanceTier.EXCELLENT -> MascotEmotion.STREAK
        LessonPerformanceTier.GOOD -> MascotEmotion.CHEER
        LessonPerformanceTier.NEEDS_WORK -> MascotEmotion.EMPATHY
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = zoomOutScale
                scaleY = zoomOutScale
                rotationZ = rotation
            }
    ) {
        // Background Floating Icons
        Icon(
            imageVector = if (isPassed) Icons.Default.Stars else Icons.Default.Favorite,
            contentDescription = null,
            tint = glowColor.copy(alpha = 0.25f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 48.dp, y = (90 + floatAnimSlow).dp)
                .size(60.dp)
                .blur(4.dp)
        )
        
        Icon(
            imageVector = if (isPassed) Icons.Default.Diamond else Icons.Default.Psychology,
            contentDescription = null,
            tint = Color(0xFF51FAC1).copy(alpha = 0.25f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-24).dp, y = (30 + floatAnimSlow).dp)
                .size(72.dp)
                .blur(6.dp)
        )
        
        Icon(
            imageVector = Icons.Default.WorkspacePremium,
            contentDescription = null,
            tint = Color(0xFFFFCCB3).copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-70).dp, y = (-220 + floatAnimFast).dp)
                .size(44.dp)
                .blur(4.dp)
        )

        // Main UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Avatar Section với LivingMascotView
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .offset(y = floatAnimSlow.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Glow
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .scale(pulseGlow)
                        .blur(26.dp)
                        .background(glowColor.copy(alpha = 0.35f), CircleShape)
                )

                // Glass Pane Background
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                        .border(1.5.dp, glowColor.copy(alpha = 0.7f), CircleShape)
                )

                // Medal / Shield Icon at top
                Icon(
                    imageVector = when (tier) {
                        LessonPerformanceTier.EXCELLENT -> Icons.Default.WorkspacePremium
                        LessonPerformanceTier.GOOD -> Icons.Default.Verified
                        LessonPerformanceTier.NEEDS_WORK -> Icons.Default.Shield
                    },
                    contentDescription = null,
                    tint = glowColor,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 12.dp)
                        .size(34.dp)
                )

                // Living Kiki Mascot
                LivingMascotView(
                    emotion = mascotEmotion,
                    size = 124.dp,
                    avatarRes = R.drawable.companion_mascot,
                    modifier = Modifier.padding(top = 18.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Performance Header Subtitle
            Text(
                text = subtitleText,
                color = glowColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.5.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Main Title
            Text(
                text = titleText,
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 42.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    shadowElevation = 18f
                    ambientShadowColor = glowColor
                    spotShadowColor = glowColor
                }
            )

            // Description
            Text(
                text = descText,
                color = Color(0xFFD6D0C5),
                fontSize = 14.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 18.dp)
                    .width(300.dp)
            )

            // Adaptive Stats Cards (3 Columns: Accuracy, Correct, XP)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x2E141426))
                    .border(1.dp, glowColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Accuracy
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.accuracyLabel,
                        color = Color(0x99FFFFFF),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$accuracy%",
                        color = glowColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0x33FFFFFF))
                )

                // 2. Correct Count
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.correctLabel,
                        color = Color(0x99FFFFFF),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$correctCount / $totalCount",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0x33FFFFFF))
                )

                // 3. XP Reward
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.xpEarnedLabel,
                        color = Color(0x99FFFFFF),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+$xpReward XP",
                            color = Color(0xFFFFD166),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (tier == LessonPerformanceTier.NEEDS_WORK) {
                    // NEEDS_WORK: Nút chính là LUYỆN TẬP LẠI (Thử lại bài học ngay)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFF5722), Color(0xFFFF9800), Color(0xFFFF5722))
                                )
                            )
                            .clickable { onRetryLesson() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.retryLesson,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Nút phụ: Xem phân tích AI để khắc phục lỗi sai
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(CircleShape)
                            .background(Color(0x2651FAC1))
                            .border(1.dp, Color(0xFF51FAC1), CircleShape)
                            .clickable { onViewAnalysis() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF51FAC1),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.checkAIAnalysis,
                                color = Color(0xFF51FAC1),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Link phụ: Về bản đồ học
                    TextButton(onClick = onEnterRealm) {
                        Text(
                            text = strings.backToMap,
                            color = Color(0x99FFFFFF),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    // PASSED / EXCELLENT: Nút Xem phân tích AI
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(Color(0x2E51FAC1))
                            .border(1.dp, Color(0xFF51FAC1), CircleShape)
                            .clickable { onViewAnalysis() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF51FAC1),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.checkAIAnalysis,
                                color = Color(0xFF51FAC1),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Nút chính: TIẾP TỤC HÀNH TRÌNH (Warp speed animation)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFF9E00), Color(0xFFFFD166), Color(0xFFFF9E00))
                                )
                            )
                            .clickable {
                                if (!isWarping) {
                                    coroutineScope.launch {
                                        isWarping = true
                                        val startTime = System.currentTimeMillis()
                                        while (System.currentTimeMillis() - startTime < 1000) {
                                            val progress = (System.currentTimeMillis() - startTime) / 1000f
                                            warpSpeed = (warpSpeed + 1.8f).coerceAtMost(120f)
                                            zoomOutScale = 1f - (progress * 0.95f)
                                            rotation = progress * 45f
                                            delay(16)
                                        }
                                        whiteoutAlpha = 1f
                                        delay(350)
                                        onEnterRealm()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = strings.continueJourney,
                                color = Color(0xFF4E4636),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF4E4636)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Warp Canvas Overlay
        if (isWarping) {
            val stars = remember {
                List(300) {
                    RankWarpStar(
                        x = Random.nextFloat() * 2000 - 1000,
                        y = Random.nextFloat() * 2000 - 1000,
                        z = Random.nextFloat() * 1000 + 100,
                        color = listOf(Color(0xFFFFD166), Color(0xFF00F5D4), Color(0xFF8A2BE2), Color.White).random(),
                        vel = Random.nextFloat() * 2 + 1
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x2605000A))
            ) {
                val cx = size.width / 2
                val cy = size.height / 2
                
                for (star in stars) {
                    star.prevZ = star.z
                    star.z -= (star.vel + warpSpeed * 80)
                    if (star.z <= 0) {
                        star.x = Random.nextFloat() * size.width * 2 - size.width
                        star.y = Random.nextFloat() * size.height * 2 - size.height
                        star.z = size.width
                        star.prevZ = star.z
                    }
                    
                    val px = (star.x / star.prevZ) * cx + cx
                    val py = (star.y / star.prevZ) * cy + cy
                    val x = (star.x / star.z) * cx + cx
                    val y = (star.y / star.z) * cy + cy
                    
                    val alpha = (warpSpeed / 50f).coerceIn(0f, 1f)
                    val strokeW = 1f + (1f - star.z / size.width) * (10f + warpSpeed * 15f)
                    
                    drawLine(
                        color = star.color.copy(alpha = alpha.coerceAtLeast(0.1f)),
                        start = Offset(px, py),
                        end = Offset(x, y),
                        strokeWidth = strokeW.coerceAtMost(20f),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Whiteout Overlay
        if (whiteoutAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = whiteoutAlpha))
            )
        }
    }
}

class RankWarpStar(
    var x: Float,
    var y: Float,
    var z: Float,
    var color: Color,
    var vel: Float
) {
    var prevZ: Float = z
}
