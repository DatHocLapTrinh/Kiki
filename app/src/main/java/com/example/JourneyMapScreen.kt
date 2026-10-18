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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.StudyViewModel


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
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(top = 12.dp, bottom = 124.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            JourneyTopBar(
                userName = userName,
                isEnglish = isEnglish,
                earnedStars = earnedStars,
                strings = strings
            )
            Spacer(modifier = Modifier.height(24.dp))

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
                text = strings.mapTitle,
                color = Color(0xFF51FAC1),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = if (isEnglish) "$userName's Journey" else "Hành trình của $userName",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = strings.mapSubtitle,
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

// ─── Neon Energy Waves path + stage nodes ────────────────────────────────────

@Composable
private fun JourneyPath(
    stages: List<JourneyStageUiModel>,
    currentStageIndex: Int,
    strings: AppStrings,
    onStageClick: (JourneyStageUiModel) -> Unit
) {
    val stageStep = 230.dp
    val rewardTop = (stages.size * stageStep.value + 10).dp

    val infiniteTransition = rememberInfiniteTransition(label = "neon_wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height((stages.size * stageStep.value + 190).dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val leftX = size.width * 0.26f
            val rightX = size.width * 0.74f
            val nodeY = { index: Int -> index * stageStep.toPx() + 58.dp.toPx() }
            val nodeX = { index: Int -> if (index % 2 == 0) leftX else rightX }

            fun buildPath(lastSegment: Int): Path {
                return Path().apply {
                    moveTo(nodeX(0), nodeY(0))
                    for (segment in 1..lastSegment) {
                        val startY = nodeY(segment - 1)
                        val endY = nodeY(segment)
                        quadraticTo(
                            (nodeX(segment - 1) + nodeX(segment)) / 2f,
                            startY + (endY - startY) * 0.45f,
                            nodeX(segment),
                            endY
                        )
                    }
                }
            }

            if (stages.size > 1) {
                // Dim background path for locked/future stages
                drawPath(
                    path = buildPath(stages.lastIndex),
                    color = Color(0x22564A7A),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(8.dp.toPx(), 10.dp.toPx())
                        )
                    )
                )

                val activeSegments = if (currentStageIndex < 0) stages.lastIndex
                    else currentStageIndex.coerceAtMost(stages.lastIndex)

                if (activeSegments > 0) {
                    val activePath = buildPath(activeSegments)

                    // Neon soft glow (subtle, not overwhelming)
                    drawPath(
                        path = activePath,
                        brush = Brush.verticalGradient(
                            listOf(Color(0x1A51FAC1), Color(0x1A7E5CFF), Color(0x1A51FAC1))
                        ),
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Neon Energy Wave — animated dash flow
                    drawPath(
                        path = activePath,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF51FAC1), Color(0xFF7E5CFF), Color(0xFF51FAC1))
                        ),
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(14.dp.toPx(), 8.dp.toPx()),
                                phase = waveOffset.dp.toPx()
                            )
                        )
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            stages.forEachIndexed { index, stage ->
                JourneyStageRow(
                    stage = stage,
                    index = index,
                    stageStep = stageStep,
                    strings = strings,
                    onClick = { onStageClick(stage) }
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

// ─── Stage row — long press reveals the info card ────────────────────────────

@Composable
private fun JourneyStageRow(
    stage: JourneyStageUiModel,
    index: Int,
    stageStep: Dp,
    strings: AppStrings,
    onClick: () -> Unit
) {
    val isLeft = index % 2 == 0
    var showCard by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height(stageStep)
    ) {
        val cardWidth = maxWidth * 0.52f
        val cardModifier = if (isLeft) {
            Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = 8.dp).width(cardWidth)
        } else {
            Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 8.dp).width(cardWidth)
        }
        val nodeModifier = if (isLeft) {
            Modifier.align(Alignment.TopStart).padding(start = 14.dp)
        } else {
            Modifier.align(Alignment.TopEnd).padding(end = 14.dp)
        }

        AnimatedVisibility(
            visible = showCard,
            modifier = cardModifier,
            enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.88f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.88f)
        ) {
            JourneyStageInformationCard(
                stage = stage,
                strings = strings,
                onClick = {
                    showCard = false
                    onClick()
                }
            )
        }

        JourneyPlanetNode(
            stage = stage,
            modifier = nodeModifier,
            onLongPress = { showCard = !showCard },
            onClick = {
                showCard = false
                onClick()
            }
        )
    }
}

// ─── Planet node ─────────────────────────────────────────────────────────────

@Composable
private fun JourneyPlanetNode(
    stage: JourneyStageUiModel,
    modifier: Modifier,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    val isCurrent = stage.state == JourneyStageState.CURRENT
    val pulse = if (isCurrent) {
        val transition = rememberInfiniteTransition(label = "journey_planet")
        transition.animateFloat(
            initialValue = 0.97f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Reverse),
            label = "journey_planet_scale"
        ).value
    } else 1f

    val colors = when (stage.visualType) {
        PlanetVisualType.CYAN   -> listOf(Color(0xFFB1FFF4), Color(0xFF13C7B8), Color(0xFF075A75))
        PlanetVisualType.PURPLE -> listOf(Color(0xFFE5C1FF), Color(0xFF974DDA), Color(0xFF33145F))
        PlanetVisualType.BLUE   -> listOf(Color(0xFFA8E0FF), Color(0xFF3887E6), Color(0xFF143B82))
        PlanetVisualType.ORANGE -> listOf(Color(0xFFFFD28A), Color(0xFFEA7218), Color(0xFF79250F))
        PlanetVisualType.MASTER -> listOf(Color(0xFFE5E1F4), Color(0xFF87809D), Color(0xFF39344D))
    }
    val accent = when (stage.state) {
        JourneyStageState.COMPLETED -> Color(0xFFFFD166)
        JourneyStageState.CURRENT   -> Color(0xFF51FAC1)
        JourneyStageState.AVAILABLE -> colors.first()
        JourneyStageState.LOCKED    -> Color(0xFF9A91AE)
    }
    val locked = stage.state == JourneyStageState.LOCKED
    val label = "${stage.sequence}. ${stage.title}. " +
        if (locked) stage.description else "${stage.completedLessons}/${stage.totalLessons} lessons"

    Box(
        modifier = modifier
            .size(116.dp)
            .graphicsLayer(scaleX = pulse, scaleY = pulse)
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { if (!locked) onLongPress() },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (isCurrent) {
            Box(modifier = Modifier.size(100.dp).blur(10.dp).background(Color(0x5551FAC1), CircleShape))
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter).offset(y = 8.dp)
                .size(width = 98.dp, height = 18.dp)
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.34f))
                .border(1.dp, accent.copy(alpha = 0.75f), RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .size(96.dp).clip(CircleShape)
                .background(
                    if (locked) Brush.radialGradient(listOf(colors.first().copy(alpha = 0.28f), Color(0x3314141E)))
                    else Brush.radialGradient(colors)
                )
                .border(2.dp, accent.copy(alpha = if (locked) 0.35f else 0.9f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (stage.state) {
                    JourneyStageState.COMPLETED -> Icons.Default.Check
                    JourneyStageState.CURRENT   -> Icons.Default.PlayArrow
                    JourneyStageState.AVAILABLE -> Icons.AutoMirrored.Filled.ArrowForward
                    JourneyStageState.LOCKED    -> Icons.Default.Lock
                },
                contentDescription = null,
                tint = if (locked) Color(0xB3FFFFFF) else Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
        if (isCurrent) {
            Icon(
                Icons.Default.Flag,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-12).dp).size(24.dp)
            )
        }
        // Hint dot — signals the planet can be long-pressed
        if (!locked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-20).dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.75f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            )
        }
    }
}

// ─── Info card ───────────────────────────────────────────────────────────────

@Composable
private fun JourneyStageInformationCard(
    stage: JourneyStageUiModel,
    strings: AppStrings,
    onClick: () -> Unit
) {
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

    Box(
        modifier = Modifier
            .height(154.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF0100E22))
            .border(
                1.5.dp,
                Brush.linearGradient(listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.3f))),
                RoundedCornerShape(20.dp)
            )
            .clickable(enabled = !locked, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(26.dp).clip(CircleShape).background(accent.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${stage.sequence}", color = Color(0xFF11101B), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stage.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(stage.description, color = Color(0xCCFFFFFF), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            Text("${stage.completedLessons}/${stage.totalLessons} lessons", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(5.dp))
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
                Text(stateLabel, color = Color(0x99FFFFFF), fontSize = 10.sp)
                Icon(
                    imageVector = if (locked) Icons.Default.Lock else Icons.Default.ChevronRight,
                    contentDescription = if (locked) stage.description else null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
