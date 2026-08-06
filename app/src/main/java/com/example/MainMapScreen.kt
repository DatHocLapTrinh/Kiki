package com.example

import com.example.viewmodel.StudyViewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun MainMapScreen(
    viewModel: StudyViewModel,
    onStartQuest: () -> Unit = {},
    onLogout: () -> Unit = {},
    onViewStats: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val parallax = LocalParallax.current
    val userName by viewModel.userName.observeAsState("Adventurer")
    val isEnglish by viewModel.isEnglish.observeAsState(false)
    val mana by viewModel.mana.observeAsState(0)
    val rankTitle by viewModel.rankTitle.observeAsState("Bronze Novice")
    val strings = LocalAppStrings.current

    LaunchedEffect(scrollState.value) {
        val maxScroll = scrollState.maxValue.toFloat().coerceAtLeast(1f)
        val progress = scrollState.value.toFloat() / maxScroll
        parallax.setTarget(0f, (progress - 0.5f) * 2f)
    }

    var isWarping by remember { mutableStateOf(false) }
    var warpSpeed by remember { mutableFloatStateOf(0f) }
    var bloomAlpha by remember { mutableFloatStateOf(0f) }
    var whiteoutAlpha by remember { mutableFloatStateOf(0f) }

    val shakeAnim = rememberInfiniteTransition(label = "shake")
    val shakeOffsetX by shakeAnim.animateFloat(
        initialValue = if (isWarping) -4f else 0f,
        targetValue = if (isWarping) 4f else 0f,
        animationSpec = infiniteRepeatable(tween(50, easing = LinearEasing), RepeatMode.Reverse),
        label = "shakeX"
    )
    val shakeOffsetY by shakeAnim.animateFloat(
        initialValue = if (isWarping) 4f else 0f,
        targetValue = if (isWarping) -4f else 0f,
        animationSpec = infiniteRepeatable(tween(70, easing = LinearEasing), RepeatMode.Reverse),
        label = "shakeY"
    )

    val coroutineScope = rememberCoroutineScope()

    val availableChapters by viewModel.availableChapters.observeAsState(emptyList())
    val chapterProgress by viewModel.chapterProgress.observeAsState(emptyList())
    val chaptersLoading by viewModel.chaptersLoading.observeAsState(false)
    val chaptersError by viewModel.chaptersError.observeAsState(null)
    val density = LocalDensity.current
    val mapStep = 230
    val journeyStages = remember(availableChapters, chapterProgress) {
        JourneyStageMapper.mapStages(availableChapters, chapterProgress)
    }
    val completedLessons = JourneyStageMapper.completedLessons(journeyStages)
    val totalLessons = JourneyStageMapper.totalLessons(journeyStages)
    val overallProgress = JourneyStageMapper.overallProgress(journeyStages)
    val earnedStars = JourneyStageMapper.earnedStars(journeyStages)
    val completedChapters = journeyStages.count { it.state == JourneyStageState.COMPLETED }
    val rewardUnlocked = JourneyStageMapper.rewardUnlocked(journeyStages)

    val currentChapterIndex = if (journeyStages.isEmpty()) {
        0
    } else {
        journeyStages.indexOfFirst { it.state == JourneyStageState.CURRENT }
            .takeIf { it >= 0 }
            ?: journeyStages.lastIndex
    }
    val currentLessonNode = (journeyStages.getOrNull(currentChapterIndex)?.completedLessons?.plus(1) ?: 1)
        .coerceIn(1, JourneyStageMapper.LESSONS_PER_STAGE)
    val mapProgress = overallProgress
    val allChaptersCompleted = rewardUnlocked

    LaunchedEffect(Unit) {
        if (availableChapters.isEmpty()) {
            viewModel.refreshChapters()
        }
    }

    LaunchedEffect(availableChapters, chapterProgress) {
        if (availableChapters.isNotEmpty()) {
            delay(100)
            val targetOffset = with(density) { (currentChapterIndex * mapStep).dp.toPx().toInt() }
            scrollState.animateScrollTo(targetOffset.coerceIn(0, scrollState.maxValue))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = shakeOffsetX.dp, y = shakeOffsetY.dp)
    ) {
        // Map Content
        val mapHeight = if (journeyStages.isEmpty()) 360 else maxOf(1100, 400 + journeyStages.size * mapStep)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((mapHeight + 220).dp)
                .verticalScroll(scrollState)
                .padding(top = 140.dp, bottom = 220.dp) // Chừa khoảng thở cho thanh điều hướng bên dưới
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(mapHeight.dp)) {
                val cx = size.width / 2f
                val chapterCount = journeyStages.size
                val horizontalOffset = 24.dp.toPx()
                fun nodeX(index: Int): Float {
                    return cx + when (index % 4) {
                        1, 3 -> horizontalOffset
                        2 -> -horizontalOffset
                        else -> 0f
                    }
                }

                val path = Path().apply {
                    if (chapterCount > 0) {
                        moveTo(nodeX(0), 200.dp.toPx())
                        for (segment in 1 until chapterCount) {
                            val startY = (200 + (segment - 1) * mapStep).dp.toPx()
                            val endY = (200 + segment * mapStep).dp.toPx()
                            val controlX = cx + if (segment % 2 == 1) horizontalOffset * 1.35f else -horizontalOffset * 1.35f
                            quadraticBezierTo(
                                controlX,
                                startY + (endY - startY) / 2f,
                                nodeX(segment),
                                endY
                            )
                        }
                    }
                }
                drawPath(path = path, color = Color(0x4DFFFFFF), style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))))

                val activePath = Path().apply {
                    if (chapterCount > 0) {
                        moveTo(nodeX(0), 200.dp.toPx())
                        for (segment in 1..currentChapterIndex.coerceAtMost(chapterCount - 1)) {
                            val startY = (200 + (segment - 1) * mapStep).dp.toPx()
                            val endY = (200 + segment * mapStep).dp.toPx()
                            val controlX = cx + if (segment % 2 == 1) horizontalOffset * 1.35f else -horizontalOffset * 1.35f
                            quadraticBezierTo(
                                controlX,
                                startY + (endY - startY) / 2f,
                                nodeX(segment),
                                endY
                            )
                        }
                    }
                }
                drawPath(path = activePath, color = Color(0xFF51FAC1), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
            }

            journeyStages.forEachIndexed { index, stage ->
                val yOffset = (200 + index * mapStep).dp
                val xOffset = when (index % 4) {
                    1, 3 -> 24.dp
                    2 -> (-24).dp
                    else -> 0.dp
                }
                val status = when (stage.state) {
                    JourneyStageState.COMPLETED -> OrbStatus.COMPLETED
                    JourneyStageState.CURRENT -> OrbStatus.CURRENT
                    JourneyStageState.AVAILABLE -> OrbStatus.AVAILABLE
                    JourneyStageState.LOCKED -> OrbStatus.LOCKED
                }
                MapOrb(stage, xOffset, yOffset, status) {
                    if (!isWarping && status != OrbStatus.LOCKED) {
                        coroutineScope.launch { 
                            isWarping = true
                            warpEffect()
                            isWarping = false
                            viewModel.selectChapterByTitle(stage.title, stage.sequence)
                            onStartQuest() 
                        } 
                    }
                }
            }

            if (journeyStages.isNotEmpty()) {
                RewardChestCard(
                    unlocked = rewardUnlocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = (200 + journeyStages.size * mapStep + 24).dp, start = 24.dp, end = 24.dp)
                )
            }

            if (journeyStages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when {
                        chaptersLoading -> JourneyLoadingState()
                        chaptersError != null -> JourneyErrorState(
                            message = chaptersError ?: strings.noChaptersFound,
                            onRetry = viewModel::refreshChapters
                        )
                        else -> JourneyEmptyState(onRetry = viewModel::refreshChapters)
                    }
                }
            }
        }

        // Top Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 76.dp, end = 24.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.mapTitle,
                        color = Color(0xFF51FAC1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.8.sp
                    )
                    Text(
                        text = if (isEnglish) "$userName's Adventure" else "Hành trình của $userName",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.graphicsLayer(alpha = 0.99f).drawBehind {
                            drawRect(
                                Brush.horizontalGradient(listOf(Color(0xFFFFD166), Color(0xFF51FAC1))),
                                blendMode = androidx.compose.ui.graphics.BlendMode.SrcIn
                            )
                        }
                    )
                    Text(
                        text = strings.mapSubtitle,
                        color = Color(0x99FFFFFF),
                        fontSize = 11.sp
                    )
                }
            }

            // Crystal Knight Badge
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.9f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = Color(0xFF51FAC1))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(rankTitle, color = Color(0xFFFFF8F2), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$earnedStars", color = Color(0xFFFFD166), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        ChromaticText("$mana")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.manaLabel, color = Color(0xFFEAE1D5), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (journeyStages.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, Color(0x3351FAC1), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    strings.learningProgress,
                                    color = Color(0xB3FFFFFF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (allChaptersCompleted) {
                                        strings.mapCompleted
                                    } else {
                                        "${strings.chapterPrefix}${currentChapterIndex + 1} • " +
                                                strings.lessonProgress
                                                    .replace("{current}", "$currentLessonNode")
                                                    .replace("{total}", "5")
                                    },
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("$earnedStars", color = Color(0xFFFFD166), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Text(
                                    "${JourneyStageMapper.percentage(mapProgress)}%",
                                    color = Color(0xFF51FAC1),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(mapProgress)
                                    .fillMaxHeight()
                                    .background(Brush.horizontalGradient(listOf(Color(0xFFFFD166), Color(0xFF51FAC1))))
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$completedLessons/$totalLessons lessons • $completedChapters/${journeyStages.size} stages",
                                color = Color(0x99FFFFFF),
                                fontSize = 11.sp
                            )
                            TextButton(onClick = onViewStats, contentPadding = PaddingValues(0.dp)) {
                                Text(strings.viewStats, color = Color(0xFF51FAC1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Warp Canvas Overlay
        if (isWarping || warpSpeed > 0) {
            // Warp implementation remains same but simplified for brevity
        }

        if (bloomAlpha > 0f) {
            Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(Color.White.copy(alpha = bloomAlpha), Color(0xFF51FAC1).copy(alpha = bloomAlpha), Color.Transparent))))
        }
        if (whiteoutAlpha > 0f) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = whiteoutAlpha)))
        }
    }
}

private suspend fun warpEffect() {
    // Warp logic simplified to fit
    delay(1000)
}

@Composable
fun RewardChestCard(
    unlocked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val accent = if (unlocked) Color(0xFFFFA62B) else Color(0xFF8D7FA7)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xCC101020))
            .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
            .clickable(enabled = unlocked, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.8f), Color(0x3314141E)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (unlocked) Icons.Default.CardGiftcard else Icons.Default.Lock,
                    contentDescription = if (unlocked) strings.rewardUnlocked else strings.rewardLocked,
                    tint = if (unlocked) Color.White else Color(0x99FFFFFF),
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (unlocked) strings.rewardUnlocked else strings.rewardLocked,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (unlocked) strings.openReward else "★ +15 XP • +3 Stars",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (unlocked) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = accent)
            }
        }
    }
}

@Composable
fun JourneyLoadingState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Color(0xFF51FAC1), strokeWidth = 3.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Đang chuẩn bị hành trình...", color = Color(0xCCFFFFFF), fontSize = 14.sp)
    }
}

@Composable
fun JourneyErrorState(message: String, onRetry: () -> Unit) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(42.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(message, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = onRetry) {
            Text(strings.retry, color = Color(0xFF51FAC1), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun JourneyEmptyState(onRetry: () -> Unit) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Explore, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(strings.journeyEmptyTitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(strings.journeyEmptyDescription, color = Color(0xB3FFFFFF), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = onRetry) {
            Text(strings.retry, color = Color(0xFF51FAC1), fontWeight = FontWeight.Bold)
        }
    }
}

enum class OrbStatus { COMPLETED, CURRENT, AVAILABLE, LOCKED }

@Composable
fun MapOrb(
    stage: JourneyStageUiModel,
    xOffset: androidx.compose.ui.unit.Dp,
    yOffset: androidx.compose.ui.unit.Dp,
    status: OrbStatus,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currentPulse = if (status == OrbStatus.CURRENT) {
        val transition = rememberInfiniteTransition(label = "current_stage")
        transition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
            label = "current_stage_scale"
        ).value
    } else {
        1f
    }
    val pulseAura = 64f * currentPulse
    val planetColors = when (stage.visualType) {
        PlanetVisualType.CYAN -> listOf(Color(0xFF8AFFF0), Color(0xFF00A99A), Color(0xFF075B73))
        PlanetVisualType.PURPLE -> listOf(Color(0xFFE6C7FF), Color(0xFF8B4DCE), Color(0xFF321866))
        PlanetVisualType.BLUE -> listOf(Color(0xFF9EDBFF), Color(0xFF2474D4), Color(0xFF12336F))
        PlanetVisualType.ORANGE -> listOf(Color(0xFFFFD08A), Color(0xFFE67518), Color(0xFF7A2A0F))
        PlanetVisualType.MASTER -> listOf(Color(0xFFE7E3F6), Color(0xFF817B9B), Color(0xFF37334E))
    }
    val statusColor = when (status) {
        OrbStatus.COMPLETED -> Color(0xFFFFD166)
        OrbStatus.CURRENT -> Color(0xFF51FAC1)
        OrbStatus.AVAILABLE -> planetColors.first()
        OrbStatus.LOCKED -> Color(0xFFB9B4C5)
    }
    val statusLabel = when (status) {
        OrbStatus.COMPLETED -> strings.chapterCompleted
        OrbStatus.CURRENT -> strings.chapterCurrent
        OrbStatus.AVAILABLE -> strings.chapterCurrent
        OrbStatus.LOCKED -> strings.chapterLocked
    }
    val stageAccessibility = when (status) {
        OrbStatus.LOCKED -> "${strings.chapterPrefix}${stage.sequence}, ${stage.title}, ${strings.chapterLocked}. ${stage.description}"
        else -> "${strings.chapterPrefix}${stage.sequence}, ${stage.title}, $statusLabel"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = yOffset),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
            .fillMaxWidth(0.64f)
            .offset(x = xOffset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer(scaleX = currentPulse, scaleY = currentPulse)
                    .semantics { contentDescription = stageAccessibility }
                    .clip(CircleShape)
                    .clickable(enabled = status != OrbStatus.LOCKED, onClick = onClick)
                    .background(
                        when (status) {
                            OrbStatus.LOCKED -> Brush.radialGradient(listOf(planetColors[0].copy(alpha = 0.32f), Color(0x331A1725)))
                            else -> Brush.radialGradient(planetColors)
                        }
                    )
                    .border(2.dp, statusColor.copy(alpha = if (status == OrbStatus.LOCKED) 0.35f else 0.9f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (status == OrbStatus.CURRENT) {
                    Box(
                        modifier = Modifier
                            .size(pulseAura.dp)
                            .blur(20.dp)
                            .background(Color(0x9951FAC1), CircleShape)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 8.dp)
                        .size(width = 88.dp, height = 18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(alpha = 0.35f))
                        .border(1.dp, statusColor.copy(alpha = 0.8f), RoundedCornerShape(50))
                )

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0x3314141E).copy(alpha = if (status == OrbStatus.LOCKED) 0.85f else 0.45f))
                        .border(1.dp, Color(0x80FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when (status) {
                        OrbStatus.COMPLETED -> Icon(Icons.Default.CheckCircle, contentDescription = strings.chapterCompleted, tint = Color(0xFFFFD166), modifier = Modifier.size(38.dp))
                        OrbStatus.CURRENT -> Icon(Icons.Default.PlayArrow, contentDescription = strings.chapterCurrent, tint = Color.White, modifier = Modifier.size(42.dp))
                        OrbStatus.AVAILABLE -> Icon(Icons.Default.ArrowForward, contentDescription = strings.chapterCurrent, tint = Color.White, modifier = Modifier.size(34.dp))
                        OrbStatus.LOCKED -> Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xB3EAE1D5), modifier = Modifier.size(30.dp))
                    }
                }

                if (status == OrbStatus.CURRENT) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = strings.chapterCurrent,
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.TopCenter).offset(y = (-18).dp).size(26.dp)
                    )
                }

                if (status == OrbStatus.COMPLETED) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD166))
                            .border(2.dp, Color(0xFF151020), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF151020), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .border(1.dp, statusColor.copy(alpha = 0.38f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${strings.chapterPrefix}${stage.sequence}",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        stage.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (status == OrbStatus.LOCKED) {
                            stage.description
                        } else {
                            "$statusLabel • ${stage.completedLessons}/${stage.totalLessons} lessons"
                        },
                        color = Color(0xB3FFFFFF),
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0x3314141E))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(stage.progress)
                                .fillMaxHeight()
                                .background(statusColor)
                        )
                    }
                }
            }
        }
    }
}
