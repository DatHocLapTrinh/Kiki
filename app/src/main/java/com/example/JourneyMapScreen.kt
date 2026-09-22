package com.example

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.StudyViewModel

@Composable
fun JourneyMapScreen(
    viewModel: StudyViewModel,
    onStartQuest: () -> Unit = {},
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
    val completedLessons = JourneyStageMapper.completedLessons(stages)
    val totalLessons = JourneyStageMapper.totalLessons(stages)
    val overallProgress = JourneyStageMapper.overallProgress(stages)
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
                .padding(top = 74.dp, bottom = 124.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            JourneyTopBar(
                userName = userName,
                isEnglish = isEnglish,
                earnedStars = earnedStars,
                strings = strings
            )
            Spacer(modifier = Modifier.height(20.dp))

            JourneySummarySection(
                completedLessons = completedLessons,
                totalLessons = totalLessons,
                overallProgress = overallProgress,
                earnedStars = earnedStars,
                stageCount = stages.size,
                onViewStats = onViewStats,
                strings = strings
            )
            Spacer(modifier = Modifier.height(28.dp))

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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        verticalAlignment = Alignment.Top
    ) {
        Spacer(modifier = Modifier.width(48.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(strings.mapTitle, color = Color(0xFF51FAC1), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                if (isEnglish) "$userName's Adventure" else "Hành trình của $userName",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(strings.mapSubtitle, color = Color(0xCCFFFFFF), fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        Box(
            modifier = Modifier
                .width(86.dp)
                .height(76.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x3314142C))
                .border(1.dp, Color(0x995C2A9D), RoundedCornerShape(18.dp))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(22.dp))
                Text("$earnedStars", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(strings.starsLabel, color = Color(0xCCFFFFFF), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun JourneySummarySection(
    completedLessons: Int,
    totalLessons: Int,
    overallProgress: Float,
    earnedStars: Int,
    stageCount: Int,
    onViewStats: () -> Unit,
    strings: AppStrings
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(104.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x3314142C))
                .border(1.dp, Color(0x665B39A2), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.learningProgress, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("$completedLessons/$totalLessons", color = Color(0xFF51FAC1), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0x44251E42))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(overallProgress).fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF51FAC1), Color(0xFF7E5CFF))))
                    )
                }
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    "${JourneyStageMapper.percentage(overallProgress)}% • $stageCount stages • $earnedStars ★",
                    color = Color(0xB3FFFFFF),
                    fontSize = 10.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(104.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x2214142C))
                .border(1.dp, Color(0x665B39A2), RoundedCornerShape(20.dp))
                .clickable(onClick = onViewStats)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BarChart, contentDescription = strings.viewStats, tint = Color(0xFF9CD9FF), modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.height(5.dp))
                Text(strings.viewStats, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFB7A7D9), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun JourneyPath(
    stages: List<JourneyStageUiModel>,
    currentStageIndex: Int,
    strings: AppStrings,
    onStageClick: (JourneyStageUiModel) -> Unit
) {
    val stageStep = 230.dp
    val rewardTop = (stages.size * stageStep.value + 10).dp

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height((stages.size * stageStep.value + 190).dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val leftX = size.width * 0.26f
            val rightX = size.width * 0.74f
            val nodeY = { index: Int -> index * stageStep.toPx() + 58.dp.toPx() }
            val nodeX = { index: Int -> if (index % 2 == 0) leftX else rightX }

            fun journeyPath(lastSegment: Int): Path {
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
                drawPath(
                    path = journeyPath(stages.lastIndex),
                    color = Color(0x665D4A88),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx())))
                )
                val activeSegments = if (currentStageIndex < 0) stages.lastIndex else currentStageIndex.coerceAtMost(stages.lastIndex)
                if (activeSegments > 0) {
                    drawPath(
                        path = journeyPath(activeSegments),
                        color = Color(0xFF51FAC1),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(11.dp.toPx(), 7.dp.toPx())))
                    )
                }
                for (index in 0 until stages.lastIndex) {
                    val x = (nodeX(index) + nodeX(index + 1)) / 2f
                    val y = (nodeY(index) + nodeY(index + 1)) / 2f
                    drawCircle(Color(0xFFFFD166), radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
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

@Composable
private fun JourneyStageRow(
    stage: JourneyStageUiModel,
    index: Int,
    stageStep: Dp,
    strings: AppStrings,
    onClick: () -> Unit
) {
    val isLeft = index % 2 == 0
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height(stageStep)
    ) {
        val cardWidth = maxWidth * 0.54f
        val cardModifier = if (isLeft) {
            Modifier.align(Alignment.TopEnd).padding(end = 12.dp).width(cardWidth)
        } else {
            Modifier.align(Alignment.TopStart).padding(start = 12.dp).width(cardWidth)
        }
        val nodeModifier = if (isLeft) {
            Modifier.align(Alignment.TopStart).padding(start = 18.dp)
        } else {
            Modifier.align(Alignment.TopEnd).padding(end = 18.dp)
        }

        JourneyPlanetNode(stage = stage, modifier = nodeModifier, onClick = onClick)
        JourneyStageInformationCard(stage = stage, modifier = cardModifier, strings = strings, onClick = onClick)
    }
}

@Composable
private fun JourneyPlanetNode(
    stage: JourneyStageUiModel,
    modifier: Modifier,
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
        PlanetVisualType.CYAN -> listOf(Color(0xFFB1FFF4), Color(0xFF13C7B8), Color(0xFF075A75))
        PlanetVisualType.PURPLE -> listOf(Color(0xFFE5C1FF), Color(0xFF974DDA), Color(0xFF33145F))
        PlanetVisualType.BLUE -> listOf(Color(0xFFA8E0FF), Color(0xFF3887E6), Color(0xFF143B82))
        PlanetVisualType.ORANGE -> listOf(Color(0xFFFFD28A), Color(0xFFEA7218), Color(0xFF79250F))
        PlanetVisualType.MASTER -> listOf(Color(0xFFE5E1F4), Color(0xFF87809D), Color(0xFF39344D))
    }
    val accent = when (stage.state) {
        JourneyStageState.COMPLETED -> Color(0xFFFFD166)
        JourneyStageState.CURRENT -> Color(0xFF51FAC1)
        JourneyStageState.AVAILABLE -> colors.first()
        JourneyStageState.LOCKED -> Color(0xFF9A91AE)
    }
    val locked = stage.state == JourneyStageState.LOCKED
    val label = "${stage.sequence}. ${stage.title}. " + if (locked) stage.description else "${stage.completedLessons}/${stage.totalLessons} lessons"

    Box(
        modifier = modifier
            .size(116.dp)
            .graphicsLayer(scaleX = pulse, scaleY = pulse)
            .semantics { contentDescription = label }
            .clickable(enabled = !locked, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isCurrent) {
            Box(modifier = Modifier.size(108.dp).blur(18.dp).background(Color(0x8851FAC1), CircleShape))
        }
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = 8.dp)
                .size(width = 98.dp, height = 18.dp)
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.34f))
                .border(1.dp, accent.copy(alpha = 0.75f), RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape)
                .background(if (locked) Brush.radialGradient(listOf(colors.first().copy(alpha = 0.28f), Color(0x3314141E))) else Brush.radialGradient(colors))
                .border(2.dp, accent.copy(alpha = if (locked) 0.35f else 0.9f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (stage.state) {
                    JourneyStageState.COMPLETED -> Icons.Default.Check
                    JourneyStageState.CURRENT -> Icons.Default.PlayArrow
                    JourneyStageState.AVAILABLE -> Icons.AutoMirrored.Filled.ArrowForward
                    JourneyStageState.LOCKED -> Icons.Default.Lock
                },
                contentDescription = null,
                tint = if (locked) Color(0xB3FFFFFF) else Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
        if (isCurrent) {
            Icon(Icons.Default.Flag, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-12).dp).size(24.dp))
        }
    }
}

@Composable
private fun JourneyStageInformationCard(
    stage: JourneyStageUiModel,
    modifier: Modifier,
    strings: AppStrings,
    onClick: () -> Unit
) {
    val locked = stage.state == JourneyStageState.LOCKED
    val accent = when (stage.state) {
        JourneyStageState.COMPLETED -> Color(0xFFFFD166)
        JourneyStageState.CURRENT -> Color(0xFF51FAC1)
        JourneyStageState.AVAILABLE -> Color(0xFF9EDBFF)
        JourneyStageState.LOCKED -> Color(0xFF776A91)
    }
    val stateLabel = when (stage.state) {
        JourneyStageState.COMPLETED -> strings.chapterCompleted
        JourneyStageState.CURRENT, JourneyStageState.AVAILABLE -> strings.chapterCurrent
        JourneyStageState.LOCKED -> strings.chapterLocked
    }

    Box(
        modifier = modifier
            .height(154.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xE614142B))
            .border(1.dp, accent.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
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
                Text(stage.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(stage.description, color = Color(0xCCFFFFFF), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            Text("${stage.completedLessons}/${stage.totalLessons} lessons", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(5.dp))
            Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(Color(0x44302A4A))) {
                Box(modifier = Modifier.fillMaxWidth(stage.progress).fillMaxHeight().background(accent))
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
