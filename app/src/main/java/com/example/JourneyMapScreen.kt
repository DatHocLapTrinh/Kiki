package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.ui.BubbleArrowDirection
import com.example.ui.SpeechBubbleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.viewmodel.StudyViewModel
import kotlinx.coroutines.launch


@Composable
fun JourneyMapScreen(
    viewModel: StudyViewModel,
    onStartQuest: () -> Unit = {},
    onStartActiveLesson: () -> Unit = {},
    onLogout: () -> Unit = {},
    onViewStats: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()
    val userName by viewModel.userName.observeAsState("Adventurer")
    val avatarUri by viewModel.avatarUri.observeAsState(null)
    val streak by viewModel.streak.observeAsState(0)
    val hearts by viewModel.hearts.observeAsState(5)
    val streakShields by viewModel.streakShields.observeAsState(0)
    val isEnglish by viewModel.isEnglish.observeAsState(false)
    val availableChapters by viewModel.availableChapters.observeAsState(emptyList())
    val chapterProgress by viewModel.chapterProgress.observeAsState(emptyList())
    val chaptersLoading by viewModel.chaptersLoading.observeAsState(false)
    val chaptersError by viewModel.chaptersError.observeAsState(null)

    val stages = remember(availableChapters, chapterProgress) {
        JourneyStageMapper.mapStages(availableChapters, chapterProgress)
    }
    val earnedStars = JourneyStageMapper.earnedStars(stages)
    val currentStageIndex = stages.indexOfFirst { it.state == JourneyStageState.CURRENT }
    var isNavigating by remember { mutableStateOf(false) }

    LaunchedEffect(availableChapters) {
        if (availableChapters.isEmpty() && !chaptersLoading) {
            viewModel.refreshChapters()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Banner chỉ số trên cùng (Avatar, Streak, Hearts, Shields)
            CosmicUserStatsBanner(
                userName = userName,
                avatarUri = avatarUri,
                streak = streak,
                hearts = hearts,
                streakShields = streakShields,
                onAvatarClick = onViewStats
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(top = 8.dp, bottom = 124.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                JourneyTopBar(
                    userName = userName,
                    isEnglish = isEnglish,
                    earnedStars = earnedStars,
                    strings = strings
                )
                Spacer(modifier = Modifier.height(16.dp))

            if (stages.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
                    when {
                        chaptersLoading -> JourneyLoadingState()
                        chaptersError != null -> JourneyErrorState(
                            message = chaptersError ?: strings.noChaptersFound,
                            onRetry = viewModel::refreshChapters
                        )
                        else -> JourneyEmptyState(onRetry = viewModel::refreshChapters)
                    }
                }
            } else {
                JourneyPath(
                    stages = stages,
                    currentStageIndex = currentStageIndex,
                    strings = strings,
                    onStageClick = { stage ->
                        if (!isNavigating && stage.state != JourneyStageState.LOCKED) {
                            isNavigating = true
                            viewModel.selectChapterByTitle(stage.title, stage.sequence)
                            onStartQuest()
                        }
                    }
                )
            }
        }
    }
}
}

@Composable
private fun JourneyTopBar(
    userName: String,
    isEnglish: Boolean,
    earnedStars: Int,
    strings: AppStrings
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isEnglish) "$userName's Roadmap" else "Lộ trình của $userName",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isEnglish) "Master English step-by-step" else "Lộ trình chinh phục tiếng Anh từng bước",
                color = Color(0xB3FFFFFF),
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x3314142C))
                .border(1.dp, Color(0x995C2A9D), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$earnedStars", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text(strings.starsLabel, color = Color(0xCCFFFFFF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CosmicUserStatsBanner(
    userName: String,
    avatarUri: String?,
    streak: Int,
    hearts: Int,
    streakShields: Int,
    onAvatarClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xDD130B29),
                        Color(0xDD0D1630),
                        Color(0xDD150E2D)
                    )
                )
            )
            .border(
                1.2.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0x668A2BE2),
                        Color(0x6651FAC1),
                        Color(0x6600F5D4),
                        Color(0x668A2BE2)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Avatar bên trái
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(Color(0xFF51FAC1), Color(0xFF8A2BE2), Color(0xFFFFD166), Color(0xFF51FAC1))
                    )
                )
                .padding(2.dp)
                .clip(CircleShape)
                .background(Color(0xFF140D2B))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAvatarClick()
                },
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUri.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = userName.trim().take(1).uppercase().ifBlank { "K" },
                    color = Color(0xFF51FAC1),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Nhóm các chỉ số bên phải (Streak, Hearts, Shields)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 2. Chuỗi ngày Streak 🔥
            CosmicStatBadge(
                icon = Icons.Default.LocalFireDepartment,
                iconTint = Color(0xFFFF6D00),
                value = "$streak",
                valueColor = Color(0xFFFFD166),
                badgeBg = Color(0x33FF6D00),
                borderColor = Color(0x66FF6D00)
            )

            // 3. Số lượng tym Hearts ❤️
            CosmicStatBadge(
                icon = Icons.Default.Favorite,
                iconTint = Color(0xFFFF2A6D),
                value = "$hearts",
                valueColor = Color(0xFFFF85A1),
                badgeBg = Color(0x33FF2A6D),
                borderColor = Color(0x66FF2A6D)
            )

            // 4. Số lượng khiên Shields 🛡️
            CosmicStatBadge(
                icon = Icons.Default.Shield,
                iconTint = Color(0xFF00E5FF),
                value = "$streakShields",
                valueColor = Color(0xFF80D8FF),
                badgeBg = Color(0x3300E5FF),
                borderColor = Color(0x6600E5FF)
            )
        }
    }
}

@Composable
private fun CosmicStatBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    valueColor: Color,
    badgeBg: Color,
    borderColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(badgeBg)
            .border(1.dp, borderColor, RoundedCornerShape(13.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.3.sp
        )
    }
}

// ─── Cosmic Planetary Path + Progressive Planet Nodes ─────────────────────────

@Composable
private fun JourneyPath(
    stages: List<JourneyStageUiModel>,
    currentStageIndex: Int,
    strings: AppStrings,
    onStageClick: (JourneyStageUiModel) -> Unit
) {
    val stageStep = 230.dp
    val rewardTop = (stages.size * stageStep.value + 10).dp

    val auroraAnim = rememberCosmicAuroraAnimation()

    fun getStageColor(index: Int): Color {
        val planetType = CosmicPlanetHelper.getPlanetTypeForStage(index)
        return CosmicPlanetHelper.getPlanetPrimaryColor(planetType)
    }

    val activeUpTo = if (currentStageIndex < 0) stages.lastIndex
        else (currentStageIndex + 1).coerceAtMost(stages.lastIndex)

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height((stages.size * stageStep.value + 190).dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val nodeX = { index: Int -> if (index % 2 == 0) 78.dp.toPx() else size.width - 78.dp.toPx() }
            val nodeY = { index: Int -> index * stageStep.toPx() + 75.dp.toPx() }

            if (stages.size > 1) {
                for (s in 1..stages.lastIndex) {
                    val startX = nodeX(s - 1)
                    val startY = nodeY(s - 1)
                    val endX = nodeX(s)
                    val endY = nodeY(s)
                    val startColor = getStageColor(s - 1)
                    val endColor = getStageColor(s)

                    val isActive = s <= activeUpTo

                    drawCosmicAuroraSegment(
                        startX = startX,
                        startY = startY,
                        endX = endX,
                        endY = endY,
                        startColor = startColor,
                        endColor = endColor,
                        animPhase = auroraAnim.phase.value,
                        waveOffsetPx = auroraAnim.waveOffset.value.dp.toPx(),
                        isActive = isActive,
                        alphaScale = if (isActive) 1f else 0.55f
                    )
                }
            }
        }

        var selectedStageIndex by remember { mutableStateOf<Int?>(null) }

        Column(modifier = Modifier.fillMaxWidth()) {
            stages.forEachIndexed { index, stage ->
                JourneyStageRow(
                    stage = stage,
                    index = index,
                    stageStep = stageStep,
                    strings = strings,
                    isSelected = selectedStageIndex == index,
                    onToggleSelect = {
                        selectedStageIndex = if (selectedStageIndex == index) null else index
                    },
                    onEnterChapter = {
                        selectedStageIndex = null
                        onStageClick(stage)
                    }
                )
            }
        }

        RewardChestCard(
            unlocked = JourneyStageMapper.rewardUnlocked(stages),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = rewardTop, start = 22.dp, end = 22.dp)
        )
    }
}

// ─── Stage row — Cosmic Planet node & Smart info card ─────────────────────────

@Composable
private fun JourneyStageRow(
    stage: JourneyStageUiModel,
    index: Int,
    stageStep: Dp,
    strings: AppStrings,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onEnterChapter: () -> Unit
) {
    val isLeft = index % 2 == 0
    val planetType = remember(index) { CosmicPlanetHelper.getPlanetTypeForStage(index) }
    val planetDiameter = remember(index) { CosmicPlanetHelper.getPlanetDiameter(index) }
    val isSaturn = planetType == CosmicPlanetType.SATURN_MONARCH
    val isSun = planetType == CosmicPlanetType.SOLAR_SUN
    val containerWidth = when {
        isSaturn -> planetDiameter + 68.dp
        isSun    -> planetDiameter + 48.dp
        else     -> planetDiameter + 28.dp
    }
    val containerHeight = when {
        isSun    -> planetDiameter + 48.dp
        isSaturn -> planetDiameter + 38.dp
        else     -> planetDiameter + 28.dp
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height(stageStep)
    ) {
        val arrowDirection = if (isLeft) BubbleArrowDirection.LEFT else BubbleArrowDirection.RIGHT
        val cardWidth = (maxWidth - 120.dp).coerceAtMost(maxWidth * 0.64f)
        val cardModifier = if (isLeft) {
            Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 8.dp).width(cardWidth)
        } else {
            Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 8.dp).width(cardWidth)
        }

        // Tọa độ tâm nodeX = 78.dp, nodeY = 75.dp
        val centerOffsetX = 78.dp - (containerWidth / 2f)
        val centerOffsetY = 75.dp - (containerHeight / 2f)

        val nodeModifier = if (isLeft) {
            Modifier
                .align(Alignment.TopStart)
                .offset(x = centerOffsetX.coerceAtLeast(0.dp), y = centerOffsetY.coerceAtLeast(0.dp))
        } else {
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-centerOffsetX).coerceAtMost(0.dp), y = centerOffsetY.coerceAtLeast(0.dp))
        }

        AnimatedVisibility(
            visible = isSelected,
            modifier = cardModifier,
            enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.88f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.88f)
        ) {
            JourneyStageInformationCard(
                stage = stage,
                index = index,
                strings = strings,
                arrowDirection = arrowDirection,
                onClick = onEnterChapter
            )
        }

        CosmicPlanetView(
            stage = stage,
            index = index,
            modifier = nodeModifier,
            onClick = {
                if (isSelected && stage.state != JourneyStageState.LOCKED) {
                    onEnterChapter()
                } else {
                    onToggleSelect()
                }
            }
        )
    }
}

// ─── Info card ───────────────────────────────────────────────────────────────

@Composable
private fun JourneyStageInformationCard(
    stage: JourneyStageUiModel,
    index: Int = 0,
    strings: AppStrings,
    arrowDirection: BubbleArrowDirection = BubbleArrowDirection.LEFT,
    onClick: () -> Unit
) {
    val planetType = remember(index) { CosmicPlanetHelper.getPlanetTypeForStage(index) }
    val planetName = remember(planetType) { CosmicPlanetHelper.getPlanetTypeName(planetType) }
    val planetColor = remember(planetType) { CosmicPlanetHelper.getPlanetPrimaryColor(planetType) }

    val locked = stage.state == JourneyStageState.LOCKED
    val accent = when (stage.state) {
        JourneyStageState.COMPLETED -> Color(0xFFFFD166)
        JourneyStageState.CURRENT   -> Color(0xFF51FAC1)
        JourneyStageState.AVAILABLE -> Color(0xFF9EDBFF)
        JourneyStageState.LOCKED    -> Color(0xFF776A91)
    }
    val stateLabel = when (stage.state) {
        JourneyStageState.COMPLETED                              -> strings.chapterCompleted
        JourneyStageState.CURRENT, JourneyStageState.AVAILABLE -> strings.chapterCurrent
        JourneyStageState.LOCKED                                 -> strings.chapterLocked
    }

    val bubbleShape = remember(arrowDirection) {
        SpeechBubbleShape(
            direction = arrowDirection,
            cornerRadius = 18.dp,
            arrowWidth = 12.dp,
            arrowHeight = 18.dp,
            arrowCenterY = 48.dp
        )
    }

    Box(
        modifier = Modifier
            .height(174.dp)
            .clip(bubbleShape)
            .background(Color(0xF2100E24))
            .border(
                1.5.dp,
                Brush.linearGradient(listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.35f))),
                bubbleShape
            )
            .clickable(enabled = !locked, onClick = onClick)
            .padding(
                start = if (arrowDirection == BubbleArrowDirection.LEFT) 20.dp else 14.dp,
                end = if (arrowDirection == BubbleArrowDirection.RIGHT) 20.dp else 14.dp,
                top = 11.dp,
                bottom = 11.dp
            )
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(accent.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${stage.sequence}", color = Color(0xFF11101B), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stage.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "🪐 Cấp ${index + 1} • $planetName",
                        color = planetColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stage.description,
                color = Color(0xCCFFFFFF),
                fontSize = 10.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Thống kê bài học trong chương
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📚 ${stage.completedLessons}/${stage.totalLessons} bài học",
                    color = accent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${(stage.progress * 100).toInt()}% hoàn thành",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(Color(0x44302A4A))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(stage.progress).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.5f))))
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (locked) "🔒 Cần mở khóa" else "👉 Xem ${stage.totalLessons} bài học trong chương",
                    color = if (locked) Color(0x99FFFFFF) else Color(0xFF00F5D4),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (locked) Icons.Default.Lock else Icons.Default.ChevronRight,
                    contentDescription = if (locked) stage.description else null,
                    tint = if (locked) Color(0x88FFFFFF) else Color(0xFF00F5D4),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

