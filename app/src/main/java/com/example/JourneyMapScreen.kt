package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.ui.BubbleArrowDirection
import com.example.ui.CosmicSectionBanner
import com.example.ui.CosmicSectionConfig
import com.example.ui.CosmicSectionData
import com.example.ui.SpeechBubbleShape
import com.example.ui.CosmicPlanetLessonPopup
import com.example.ui.CosmicMilestoneDivider
import com.example.ui.OutOfHeartsDialog
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import kotlinx.coroutines.delay
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
    val density = LocalDensity.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val userName by viewModel.userName.observeAsState("Adventurer")
    val avatarUri by viewModel.avatarUri.observeAsState(null)
    val streak by viewModel.streak.observeAsState(0)
    val hearts by viewModel.hearts.observeAsState(5)
    val streakShields by viewModel.streakShields.observeAsState(0)
    val stars by viewModel.stars.observeAsState(0)
    val isEnglish by viewModel.isEnglish.observeAsState(false)
    var showOutOfHeartsPrompt by remember { mutableStateOf(false) }
    val availableChapters by viewModel.availableChapters.observeAsState(emptyList())
    val chapterProgress by viewModel.chapterProgress.observeAsState(emptyList())
    val chaptersLoading by viewModel.chaptersLoading.observeAsState(false)
    val chaptersError by viewModel.chaptersError.observeAsState(null)

    val stages = remember(availableChapters, chapterProgress) {
        JourneyStageMapper.mapStages(availableChapters, chapterProgress)
    }
    val currentStageIndex = stages.indexOfFirst { it.state == JourneyStageState.CURRENT }
    var isNavigating by remember { mutableStateOf(false) }

    val unlockEvent by viewModel.chapterUnlockEvent.observeAsState(null)
    var animatingUnlockStageIndex by remember { mutableIntStateOf(-1) }
    val unlockAnim = remember { Animatable(0f) }

    fun triggerPlanetUnlock(targetIdx: Int) {
        coroutineScope.launch {
            if (targetIdx > 0 && targetIdx < stages.size && animatingUnlockStageIndex == -1) {
                animatingUnlockStageIndex = targetIdx
                val stageStepPx = with(density) { 255.dp.toPx() }
                val sectionBreakPx = with(density) { 56.dp.toPx() }
                val targetPlanetY = (targetIdx / 10) * sectionBreakPx + targetIdx * stageStepPx + with(density) { 85.dp.toPx() }
                val viewportHeightPx = scrollState.viewportSize.toFloat()
                val desiredScroll = (targetPlanetY - (if (viewportHeightPx > 0) viewportHeightPx * 0.45f else 600f)).toInt().coerceAtLeast(0)

                scrollState.animateScrollTo(desiredScroll, tween(800, easing = FastOutSlowInEasing))

                unlockAnim.snapTo(0f)
                viewModel.soundEffectManager.playCorrect(3)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                val shatterJob = launch {
                    delay(1750) // Thời điểm khiên nổ tung sau Phase 1 & 2 (~65% của 2700ms)
                    viewModel.soundEffectManager.playFanfare()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                unlockAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 2700, easing = LinearEasing)
                )

                shatterJob.cancel()
                viewModel.clearChapterUnlockEvent()
                animatingUnlockStageIndex = -1
            }
        }
    }

    LaunchedEffect(unlockEvent) {
        val event = unlockEvent ?: return@LaunchedEffect
        var targetIdx = stages.indexOfFirst { it.title.equals(event.nextChapterTitle, ignoreCase = true) }
        if (targetIdx == -1) {
            targetIdx = stages.indexOfFirst { it.state == JourneyStageState.LOCKED }
        }
        if (targetIdx > 0 && targetIdx < stages.size) {
            triggerPlanetUnlock(targetIdx)
        } else {
            viewModel.clearChapterUnlockEvent()
        }
    }

    LaunchedEffect(Unit) {
        isNavigating = false
    }

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
            // Banner chỉ số trên cùng (Avatar profile, Streak, Hearts, Shields) - 4 cột chia đều, không đóng khung
            CosmicUserStatsBanner(
                avatarUri = avatarUri,
                streak = streak,
                hearts = hearts,
                streakShields = streakShields
            )

            // ─── STICKY COSMIC SECTION BANNER (Ghim cố định ở đầu màn hình) ───
            // Không bị cuộn trôi mất khi vuốt lên hay vuốt xuống, tự động đổi màu khi qua chặng khác
            if (stages.isNotEmpty()) {
                val stageStepPx = with(density) { 255.dp.toPx() }
                val currentSectionData = remember(stages, scrollState.value) {
                    val scrollY = scrollState.value.toFloat()
                    val approxStageIndex = ((scrollY + 220f) / stageStepPx).toInt().coerceIn(0, stages.lastIndex)
                    val sectionNum = (approxStageIndex / 10) + 1
                    val startIdx = (sectionNum - 1) * 10
                    val sectionStages = stages.drop(startIdx).take(10)
                    val completed = sectionStages.count { it.state == JourneyStageState.COMPLETED }
                    CosmicSectionConfig.getSectionData(
                        sectionNumber = sectionNum,
                        completedPlanets = completed,
                        totalInThisSection = if (sectionStages.isNotEmpty()) sectionStages.size else 10
                    )
                }

                CosmicSectionBanner(
                    section = currentSectionData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(98.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(top = 4.dp, bottom = 160.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                        isEnglish = isEnglish,
                        animatingUnlockStageIndex = animatingUnlockStageIndex,
                        unlockAnimProgress = unlockAnim.value,
                        onStageStartQuest = { stage ->
                            if (hearts <= 0) {
                                showOutOfHeartsPrompt = true
                            } else if (!isNavigating) {
                                isNavigating = true
                                viewModel.selectChapterByTitle(stage.title, stage.sequence)
                                onStartQuest()
                            }
                        },
                        onStageLongClick = { _, index ->
                            if (animatingUnlockStageIndex == -1) {
                                triggerPlanetUnlock(index)
                            }
                        },
                        onEnsureVisible = { popupBottomPx, planetNodeYPx ->
                            val viewportHeight = scrollState.viewportSize.toFloat()
                            val currentScroll = scrollState.value.toFloat()
                            if (viewportHeight > 0) {
                                if (popupBottomPx > currentScroll + viewportHeight - 120) {
                                    val targetScroll = (popupBottomPx - viewportHeight + 150).toInt()
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(targetScroll, tween(400, easing = FastOutSlowInEasing))
                                    }
                                } else if (planetNodeYPx - 120 < currentScroll) {
                                    val targetScroll = (planetNodeYPx - 140).toInt().coerceAtLeast(0)
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(targetScroll, tween(400, easing = FastOutSlowInEasing))
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
        if (showOutOfHeartsPrompt) {
            OutOfHeartsDialog(
                currentStars = stars,
                isEnglish = isEnglish,
                starCost = 20,
                onRefillWithStars = {
                    val success = viewModel.purchaseShopItem(
                        itemId = "refill_hearts_map",
                        type = com.example.ShopItemType.HEART,
                        starCost = 20,
                        quantity = 5
                    )
                    if (success) {
                        showOutOfHeartsPrompt = false
                    }
                },
                onQuitLesson = {
                    showOutOfHeartsPrompt = false
                }
            )
        }
    }
}

@Composable
private fun CosmicUserStatsBanner(
    avatarUri: String?,
    streak: Int,
    hearts: Int,
    streakShields: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cột 1: Avatar Profile của user
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14141E))
                    .border(2.dp, Color(0xFFFFD166), CircleShape)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null && (avatarUri.startsWith("http://") || avatarUri.startsWith("https://") || avatarUri.startsWith("content://"))) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    AsyncImage(
                        model = resolveAvatarResource(avatarUri),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // Cột 2: Chuỗi ngày Streak 🔥
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_stat_streak),
                    contentDescription = "Streak",
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$streak",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Cột 3: Số lượng tym Hearts ❤️
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_stat_heart),
                    contentDescription = "Hearts",
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$hearts",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Cột 4: Số lượng khiên Shields 🛡️
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_stat_shield),
                    contentDescription = "Shields",
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$streakShields",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ─── Cosmic Planetary Path + Progressive Planet Nodes ─────────────────────────

@Composable
private fun JourneyPath(
    stages: List<JourneyStageUiModel>,
    currentStageIndex: Int,
    strings: AppStrings,
    isEnglish: Boolean = false,
    animatingUnlockStageIndex: Int = -1,
    unlockAnimProgress: Float = 0f,
    onStageStartQuest: (JourneyStageUiModel) -> Unit,
    onStageLongClick: ((JourneyStageUiModel, Int) -> Unit)? = null,
    onEnsureVisible: ((Float, Float) -> Unit)? = null
) {
    val stageStep = 255.dp
    val sectionBreakHeight = 56.dp

    val totalSectionBreaks = if (stages.isEmpty()) 0 else (stages.lastIndex / 10)
    val totalPathHeight = (stages.size * stageStep.value + totalSectionBreaks * sectionBreakHeight.value + 190).dp
    val rewardTop = (stages.size * stageStep.value + totalSectionBreaks * sectionBreakHeight.value + 10).dp

    val auroraAnim = rememberCosmicAuroraAnimation()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var selectedStageIndex by remember { mutableStateOf<Int?>(null) }

    fun getStageColor(index: Int): Color {
        val planetType = CosmicPlanetHelper.getPlanetTypeForStage(index)
        return CosmicPlanetHelper.getPlanetPrimaryColor(planetType)
    }

    val activeUpTo = if (currentStageIndex < 0) stages.lastIndex
        else (currentStageIndex + 1).coerceAtMost(stages.lastIndex)

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height(totalPathHeight)
    ) {
        val boxWidth = maxWidth

        // 1. Scrim overlay trong suốt để chạm ra ngoài là đóng Popup
        if (selectedStageIndex != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedStageIndex = null
                    }
            )
        }

        // 2. Dải cực quang ngân hà nối các hành tinh (Cosmic Aurora Segments)
        Canvas(modifier = Modifier.matchParentSize()) {
            val nodeX = { index: Int -> if (index % 2 == 0) 90.dp.toPx() else size.width - 90.dp.toPx() }
            val nodeY = { index: Int -> ((index / 10) * sectionBreakHeight.toPx()) + index * stageStep.toPx() + 85.dp.toPx() }

            if (stages.size > 1) {
                for (s in 1..stages.lastIndex) {
                    val startX = nodeX(s - 1)
                    val startY = nodeY(s - 1)
                    val endX = nodeX(s)
                    val endY = nodeY(s)
                    val startColor = getStageColor(s - 1)
                    val endColor = getStageColor(s)

                    val isActive = s <= activeUpTo
                    val isUnlockingThisSegment = (s == animatingUnlockStageIndex)
                    val surgePulse = if (isUnlockingThisSegment && unlockAnimProgress < 0.35f) {
                        (unlockAnimProgress / 0.35f).coerceIn(0f, 1f)
                    } else null

                    drawCosmicAuroraSegment(
                        startX = startX,
                        startY = startY,
                        endX = endX,
                        endY = endY,
                        startColor = startColor,
                        endColor = endColor,
                        animPhase = auroraAnim.phase.value,
                        waveOffsetPx = auroraAnim.waveOffset.value.dp.toPx(),
                        isActive = isActive || isUnlockingThisSegment,
                        alphaScale = if (isActive || isUnlockingThisSegment) 1f else 0.55f,
                        surgePulseProgress = surgePulse
                    )
                }
            }
        }

        // 3. Cột chứa các hành tinh và mốc chuyển tiếp ngân hà (Column)
        Column(modifier = Modifier.fillMaxWidth()) {
            stages.forEachIndexed { index, stage ->
                if (index > 0 && index % 10 == 0) {
                    val nextSectionNum = (index / 10) + 1
                    CosmicMilestoneDivider(
                        sectionNum = nextSectionNum,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(sectionBreakHeight)
                    )
                }

                val stageUnlockProgress = if (index == animatingUnlockStageIndex && unlockAnimProgress >= 0.35f) {
                    ((unlockAnimProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                } else 0f

                JourneyStageRow(
                    stage = stage,
                    index = index,
                    totalStages = stages.size,
                    stageStep = stageStep,
                    strings = strings,
                    unlockProgress = stageUnlockProgress,
                    onStageClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val newIdx = if (selectedStageIndex == index) null else index
                        selectedStageIndex = newIdx
                        if (newIdx != null) {
                            val sectionBreakPx = with(density) { sectionBreakHeight.toPx() }
                            val stageStepPx = with(density) { stageStep.toPx() }
                            val nodeYPx = (index / 10) * sectionBreakPx + index * stageStepPx + with(density) { 85.dp.toPx() }
                            val popupBottomPx = nodeYPx + with(density) { 84.dp.toPx() } + with(density) { 180.dp.toPx() }
                            onEnsureVisible?.invoke(popupBottomPx, nodeYPx)
                        }
                    },
                    onStageLongClick = {
                        onStageLongClick?.invoke(stage, index)
                    }
                )
            }
        }

        // 4. Rương kho báu cuối chặng
        RewardChestCard(
            unlocked = JourneyStageMapper.rewardUnlocked(stages),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = rewardTop, start = 22.dp, end = 22.dp)
        )

        // 5. Duolingo-style Cosmic Planet Lesson Pop-up với mũi tên trỏ vào hành tinh
        // Đặt popupY = planetNodeY + 84.dp (ở dưới vành quỹ đạo, KHÔNG BỊ ĐÈ LÊN HÀNH TINH)
        val currentSelectedIdx = selectedStageIndex
        if (currentSelectedIdx != null && currentSelectedIdx in stages.indices) {
            val selectedStage = stages[currentSelectedIdx]
            val isLeft = currentSelectedIdx % 2 == 0
            val planetNodeY = sectionBreakHeight * (currentSelectedIdx / 10) + stageStep * currentSelectedIdx + 85.dp
            val popupY = planetNodeY + 84.dp
            val cardWidth = 260.dp.coerceAtMost(boxWidth - 40.dp)
            val arrowX = if (isLeft) (90.dp - 20.dp) else (cardWidth - (90.dp - 20.dp))

            key(currentSelectedIdx) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = popupY)
                        .padding(horizontal = 20.dp),
                    contentAlignment = if (isLeft) Alignment.TopStart else Alignment.TopEnd
                ) {
                    CosmicPlanetLessonPopup(
                        stage = selectedStage,
                        index = currentSelectedIdx,
                        totalStages = stages.size,
                        isEnglish = isEnglish,
                        arrowX = arrowX,
                        modifier = Modifier.width(cardWidth),
                        onStartQuest = {
                            selectedStageIndex = null
                            onStageStartQuest(selectedStage)
                        },
                        onDismiss = {
                            selectedStageIndex = null
                        }
                    )
                }
            }
        }
    }
}

// ─── Stage row — Cosmic Planet node ──────────────────────────────────────────

@Composable
private fun JourneyStageRow(
    stage: JourneyStageUiModel,
    index: Int,
    totalStages: Int = -1,
    stageStep: Dp,
    strings: AppStrings,
    unlockProgress: Float = 0f,
    onStageClick: () -> Unit,
    onStageLongClick: (() -> Unit)? = null
) {
    val isLeft = index % 2 == 0
    val planetType = remember(index, totalStages) { CosmicPlanetHelper.getPlanetTypeForStage(index, totalStages) }
    val planetDiameter = remember(index, totalStages) { CosmicPlanetHelper.getPlanetDiameter(index, totalStages) }
    val isSaturn = planetType == CosmicPlanetType.SATURN_MONARCH
    val containerWidth = if (isSaturn) 236.dp else 216.dp
    val containerHeight = 236.dp

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height(stageStep)
    ) {
        // Tọa độ tâm nodeX = 90.dp, nodeY = 85.dp
        val centerOffsetX = 90.dp - (containerWidth / 2f)
        val centerOffsetY = 85.dp - (containerHeight / 2f)

        val nodeModifier = if (isLeft) {
            Modifier
                .align(Alignment.TopStart)
                .offset(x = centerOffsetX.coerceAtLeast(0.dp), y = centerOffsetY.coerceAtLeast(0.dp))
        } else {
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-centerOffsetX).coerceAtMost(0.dp), y = centerOffsetY.coerceAtLeast(0.dp))
        }

        CosmicPlanetView(
            stage = stage,
            index = index,
            totalStages = totalStages,
            modifier = nodeModifier,
            unlockProgress = unlockProgress,
            onLongClick = onStageLongClick,
            onClick = {
                onStageClick()
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

