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

                val isGeneratingNextChapter by viewModel.isGeneratingNextChapter.observeAsState(false)
                val allCompleted = stages.isNotEmpty() && stages.all { it.state == JourneyStageState.COMPLETED }

                if (allCompleted || stages.size >= 5) {
                    Spacer(modifier = Modifier.height(24.dp))
                    AiNextChapterCard(
                        isGenerating = isGeneratingNextChapter,
                        strings = strings,
                        onGenerate = {
                            viewModel.generateNextChapterOnDemand()
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
                text = strings.mapTitle,
                color = Color(0xFF51FAC1),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
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

    val auroraAnim = rememberCosmicAuroraAnimation()

    fun getStageColor(visualType: PlanetVisualType): Color {
        return when (visualType) {
            PlanetVisualType.CYAN -> Color(0xFF51FAC1)
            PlanetVisualType.PURPLE -> Color(0xFF974DDA)
            PlanetVisualType.BLUE -> Color(0xFF3887E6)
            PlanetVisualType.ORANGE -> Color(0xFFEA7218)
            PlanetVisualType.MASTER -> Color(0xFFC084FC)
        }
    }

    val activeUpTo = if (currentStageIndex < 0) stages.lastIndex
        else (currentStageIndex + 1).coerceAtMost(stages.lastIndex)

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height((stages.size * stageStep.value + 190).dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val nodeX = { index: Int -> if (index % 2 == 0) 72.dp.toPx() else size.width - 72.dp.toPx() }
            val nodeY = { index: Int -> index * stageStep.toPx() + 58.dp.toPx() }

            if (stages.size > 1) {
                for (s in 1..stages.lastIndex) {
                    val startX = nodeX(s - 1)
                    val startY = nodeY(s - 1)
                    val endX = nodeX(s)
                    val endY = nodeY(s)
                    val startColor = getStageColor(stages[s - 1].visualType)
                    val endColor = getStageColor(stages[s].visualType)

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

// ─── Stage row — tap reveals the smart info card ─────────────────────────────

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

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height(stageStep)
    ) {
        val arrowDirection = if (isLeft) BubbleArrowDirection.LEFT else BubbleArrowDirection.RIGHT
        val cardWidth = (maxWidth - 105.dp).coerceAtMost(maxWidth * 0.65f)
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
            visible = isSelected,
            modifier = cardModifier,
            enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.88f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.88f)
        ) {
            JourneyStageInformationCard(
                stage = stage,
                strings = strings,
                arrowDirection = arrowDirection,
                onClick = onEnterChapter
            )
        }

        JourneyPlanetNode(
            stage = stage,
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

// ─── Planet node ─────────────────────────────────────────────────────────────

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
        JourneyStageState.LOCKED    -> colors[1].copy(alpha = 0.75f)
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
                .size(96.dp).clip(CircleShape)
                .background(
                    if (locked) Brush.radialGradient(listOf(colors.first().copy(alpha = 0.35f), Color(0x3314141E)))
                    else Brush.radialGradient(colors)
                )
                .border(2.dp, accent.copy(alpha = if (locked) 0.55f else 0.9f), CircleShape),
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
                tint = if (locked) Color(0xD9FFFFFF) else Color.White,
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
    }
}

// ─── Info card ───────────────────────────────────────────────────────────────

@Composable
private fun JourneyStageInformationCard(
    stage: JourneyStageUiModel,
    strings: AppStrings,
    arrowDirection: BubbleArrowDirection = BubbleArrowDirection.LEFT,
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
            .height(162.dp)
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
                Text(
                    stage.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
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

@Composable
private fun AiNextChapterCard(
    isGenerating: Boolean,
    strings: AppStrings,
    onGenerate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF130924), Color(0xFF0D1B2A))
                )
            )
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(Color(0xFF00F5D4), Color(0xFF8A2BE2), Color(0xFF00F5D4))
                ),
                RoundedCornerShape(22.dp)
            )
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF00F5D4),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Adaptive Learning",
                    color = Color(0xFF00F5D4),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hệ thống AI sẵn sàng phân tích các lỗ hổng kiến thức để kiến tạo chặng học tiếp theo tối ưu cho bạn.",
                color = Color(0xCCFFFFFF),
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGenerating)
                            Brush.linearGradient(listOf(Color(0xFF332050), Color(0xFF203050)))
                        else
                            Brush.linearGradient(listOf(Color(0xFF00F5D4), Color(0xFF8A2BE2)))
                    )
                    .clickable(enabled = !isGenerating, onClick = onGenerate),
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF00F5D4),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.aiGeneratingNextChapter,
                            color = Color(0xFF00F5D4),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = strings.generateNextAiChapterButton,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

