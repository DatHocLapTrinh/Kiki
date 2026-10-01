package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.model.QuestItem
import com.example.model.QuestionType
import com.example.ui.ConfettiEffect
import com.example.ui.SentenceBuilderView
import com.example.ui.safeBottomDockPadding
import com.example.viewmodel.StudyViewModel
import java.util.Locale

private data class ComboBadgeInfo(
    val text: String,
    val bg: Color,
    val border: Color,
    val bonusXp: Int
)

@Composable
fun QuestScreen(viewModel: StudyViewModel, onFinish: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val strings = LocalAppStrings.current
    val isEnglish by viewModel.isEnglish.observeAsState(false)
    val questions by viewModel.questions.observeAsState(emptyList())
    val chapterTitle by viewModel.currentChapterTitle.observeAsState("English Fundamentals")
    val isSpeaking by viewModel.ttsManager.isSpeaking

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF27E0A9))
        }
        return
    }

    var currentQuestion by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableIntStateOf(-1) }
    var selectedTokenIndices by remember(currentQuestion) { mutableStateOf<List<Int>>(emptyList()) }
    var isChecked by remember { mutableStateOf(false) }
    var isCurrentCorrect by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var mascotSpeech by remember { mutableStateOf<String?>(null) }
    var isSavedToVault by remember(currentQuestion) { mutableStateOf(false) }
    var consecutiveCorrect by remember { mutableIntStateOf(0) }
    var showConfetti by remember { mutableStateOf(false) }

    // Map lưu câu trả lời bất biến của người dùng
    val userAnswers = remember { mutableStateMapOf<Int, Int>() }
    val totalQuestions = questions.size

    val infiniteTransition = rememberInfiniteTransition(label = "quest_anim")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "float"
    )
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Thanh tiến độ & Linh vật
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nút đóng / thoát bài học
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showExitDialog = true
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x26FFFFFF))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val progressRatio = (currentQuestion + 1).toFloat() / totalQuestions
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressRatio)
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(Color(0xFFA3E635), Color(0xFF2DD4BF))))
                        )
                    }
                    Text(
                        text = "${if (isEnglish) "Question" else "Câu"} ${currentQuestion + 1}/$totalQuestions",
                        color = Color(0xFF51FAC1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Avatar linh vật Kiki tương tác chạm
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val quotes = if (isEnglish) listOf(
                                "You've got this! Step by step to English fluency!",
                                "Mistakes are proof that you're learning! Keep going!",
                                "Every question makes your mind sharper!",
                                "Kiki believes in your superpower!"
                            ) else listOf(
                                "Cố lên bạn ơi! Từng bước một sẽ nói tiếng Anh lưu loát!",
                                "Lỗi sai là bước đệm để thành công! Tiếp tục nhé!",
                                "Mỗi câu hỏi giúp phản xạ tiếng Anh của bạn nhạy bén hơn!",
                                "Kiki luôn đồng hành và cổ vũ bạn hết mình!"
                            )
                            val q = quotes.random()
                            mascotSpeech = q
                            viewModel.ttsManager.speak(q)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .scale(pulseAnim)
                            .background(Brush.linearGradient(listOf(Color(0xFF785A00), Color(0xFF006C4F))), CircleShape)
                            .blur(8.dp)
                    )
                    AsyncImage(
                        model = R.drawable.companion_mascot,
                        contentDescription = "Companion",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0x33FFFFFF), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF27E0A9))
                            .border(2.dp, Color(0xFF0A0A12), CircleShape)
                    )
                }
            }

            // Bong bóng thoại động viên của linh vật Kiki
            AnimatedVisibility(
                visible = mascotSpeech != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                mascotSpeech?.let { speech ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x2251FAC1))
                            .border(1.dp, Color(0x6651FAC1), RoundedCornerShape(16.dp))
                            .clickable { mascotSpeech = null }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kiki: \"$speech\"",
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0x88FFFFFF), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Scrollable Question Card Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Card câu hỏi chính
                AnimatedContent(
                    targetState = currentQuestion,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    },
                    label = "question_transition"
                ) { qIdx ->
                    val currentQ = questions[qIdx]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .offset(y = floatAnim.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(32.dp))
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "${strings.questPrefix}${qIdx + 1}: $chapterTitle",
                                    color = Color(0xFF27E0A9),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Nút nghe phát âm tiếng Anh
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val textToSpeak = if (currentQ.type == QuestionType.SENTENCE_BUILDER) {
                                            currentQ.correctSentence.ifEmpty { currentQ.question }
                                        } else {
                                            currentQ.question
                                        }
                                        viewModel.ttsManager.speak(textToSpeak, isSlow = false)
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x3351FAC1))
                                ) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Speak Question",
                                        tint = if (isSpeaking) Color(0xFF51FAC1) else Color(0xFFFFD166),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                // Nút nghe phát âm chậm ELSA Style 0.68x
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val textToSpeak = if (currentQ.type == QuestionType.SENTENCE_BUILDER) {
                                            currentQ.correctSentence.ifEmpty { currentQ.question }
                                        } else {
                                            currentQ.question
                                        }
                                        viewModel.ttsManager.speak(textToSpeak, isSlow = true)
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33FFB703))
                                ) {
                                    Text("🐢", fontSize = 15.sp)
                                }
                            }

                            // Badge chuỗi thắng liên tiếp (Combo Multiplier)
                            if (consecutiveCorrect >= 2) {
                                val badge = when (consecutiveCorrect) {
                                    2 -> ComboBadgeInfo("🔥 COMBO x2 (+20 XP)", Color(0x33FF9E00), Color(0xFFFF9E00), 20)
                                    3 -> ComboBadgeInfo("🔥 COMBO x3 (+30 XP)", Color(0x44FF5722), Color(0xFFFF5722), 30)
                                    4 -> ComboBadgeInfo("⚡ SIÊU COMBO x4 (+40 XP)", Color(0x44E040FB), Color(0xFFE040FB), 40)
                                    else -> ComboBadgeInfo("🌟 HUYỀN THOẠI x5 (+60 XP)", Color(0x44FFD700), Color(0xFFFFD700), 60)
                                }
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 10.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(badge.bg)
                                        .border(1.dp, badge.border, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = badge.text,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            if (currentQ.type == QuestionType.SENTENCE_BUILDER) {
                                SentenceBuilderView(
                                    promptText = currentQ.question,
                                    availableTokens = currentQ.sentenceTokens,
                                    selectedTokenIndices = selectedTokenIndices,
                                    isChecked = isChecked,
                                    isCorrect = isCurrentCorrect,
                                    isEnglish = isEnglish,
                                    onTokenSelected = { tokenIdx ->
                                        if (!selectedTokenIndices.contains(tokenIdx)) {
                                            selectedTokenIndices = selectedTokenIndices + tokenIdx
                                        }
                                    },
                                    onTokenRemoved = { position ->
                                        if (position in selectedTokenIndices.indices) {
                                            selectedTokenIndices = selectedTokenIndices.filterIndexed { idx, _ -> idx != position }
                                        }
                                    },
                                    onSpeakPrompt = {
                                        viewModel.ttsManager.speak(currentQ.correctSentence.ifEmpty { currentQ.question })
                                    }
                                )
                            } else {
                                Text(
                                    text = currentQ.question,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 32.sp,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )

                                val options = currentQ.options
                                val labels = listOf("A", "B", "C", "D")

                                options.forEachIndexed { index, text ->
                                    val isSelected = selectedOption == index
                                    val isCorrectAnswer = index == currentQ.correctIndex

                                    val targetAlpha = if (isChecked && !isSelected && !isCorrectAnswer) 0.45f else 1f

                                    val (optionBg, optionBorder, optionTextColor) = when {
                                        isChecked && isCorrectAnswer -> Triple(Color(0x3322C55E), Color(0xFF22C55E), Color(0xFF51FAC1))
                                        isChecked && isSelected && !isCorrectAnswer -> Triple(Color(0x33EF4444), Color(0xFFEF4444), Color(0xFFFF7A7A))
                                        isSelected -> Triple(Color(0x2651FAC1), Color(0xFF51FAC1), Color.White)
                                        else -> Triple(Color(0x0DFFFFFF), Color(0x26FFFFFF), Color(0xCCFFFFFF))
                                    }

                                    val animatedBorderColor by animateColorAsState(targetValue = optionBorder, animationSpec = tween(250), label = "border_color")
                                    val animatedBgColor by animateColorAsState(targetValue = optionBg, animationSpec = tween(250), label = "bg_color")
                                    val animatedScale by animateFloatAsState(targetValue = if (isSelected) 1.02f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "option_scale")

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                            .graphicsLayer {
                                                scaleX = animatedScale
                                                scaleY = animatedScale
                                                alpha = targetAlpha
                                            }
                                            .clip(CircleShape)
                                            .background(animatedBgColor)
                                            .border(
                                                width = if (isSelected || (isChecked && isCorrectAnswer)) 2.dp else 1.dp,
                                                color = animatedBorderColor,
                                                shape = CircleShape
                                            )
                                            .clickable(enabled = !isChecked) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedOption = index
                                            }
                                            .padding(horizontal = 20.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            val badgeBg = when {
                                                isChecked && isCorrectAnswer -> Color(0xFF22C55E)
                                                isChecked && isSelected && !isCorrectAnswer -> Color(0xFFEF4444)
                                                isSelected -> Color(0xFF51FAC1)
                                                else -> Color(0x1AFFFFFF)
                                            }
                                            val badgeTextColor = if (isSelected && !isChecked) Color(0xFF0F172A) else Color.White

                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(badgeBg)
                                                    .border(1.dp, animatedBorderColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    labels[index],
                                                    color = badgeTextColor,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Text(
                                                text = text,
                                                color = optionTextColor,
                                                fontSize = 16.sp,
                                                fontWeight = if (isSelected || (isChecked && isCorrectAnswer)) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }

                                        if (isChecked && isCorrectAnswer) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(24.dp))
                                        } else if (isChecked && isSelected && !isCorrectAnswer) {
                                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                                        } else if (isSelected) {
                                            Icon(Icons.Default.Diamond, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Spacing to keep content clear above the sticky bottom dock
                Spacer(modifier = Modifier.height(160.dp))
            }
        }

        // Duolingo-style Unified Sticky Bottom Dock
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = isChecked,
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn()).togetherWith(
                        slideOutVertically { height -> height } + fadeOut()
                    )
                },
                label = "bottom_dock_state"
            ) { checked ->
                if (!checked) {
                    // Pre-check Action Bar: Skip button + Check button
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xF212111E),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3351FAC1))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showExitDialog = true
                                },
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = null,
                                    tint = Color(0x99FFFFFF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    strings.skipQuest,
                                    color = Color(0x99FFFFFF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val isCheckEnabled = if (questions[currentQuestion].type == QuestionType.SENTENCE_BUILDER) {
                                selectedTokenIndices.isNotEmpty()
                            } else {
                                selectedOption != -1
                            }
                            Button(
                                onClick = {
                                    val currentQ = questions[currentQuestion]
                                    isChecked = true
                                    val isCorrect = if (currentQ.type == QuestionType.SENTENCE_BUILDER) {
                                        val assembledWords = selectedTokenIndices.mapNotNull { currentQ.sentenceTokens.getOrNull(it) }
                                        currentQ.userSentenceTokens = assembledWords
                                        val assembledSentence = assembledWords.joinToString(" ")
                                        fun normalize(s: String) = s.lowercase(Locale.ROOT)
                                            .replace(Regex("[.,!?;:\"]"), "")
                                            .replace(Regex("\\s+"), " ")
                                            .trim()
                                        normalize(assembledSentence) == normalize(currentQ.correctSentence)
                                    } else {
                                        selectedOption == currentQ.correctIndex
                                    }
                                    isCurrentCorrect = isCorrect
                                    if (isCorrect) {
                                        consecutiveCorrect++
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.soundEffectManager.playCorrect(consecutiveCorrect)
                                        if (consecutiveCorrect >= 3) {
                                            showConfetti = true
                                        }
                                    } else {
                                        consecutiveCorrect = 0
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.soundEffectManager.playIncorrect()
                                    }
                                },
                                enabled = isCheckEnabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    disabledContainerColor = Color(0x1AFFFFFF),
                                    contentColor = Color(0xFF0F172A),
                                    disabledContentColor = Color(0x44FFFFFF)
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (isCheckEnabled) Brush.horizontalGradient(
                                                listOf(Color(0xFF51FAC1), Color(0xFF22C55E))
                                            ) else Brush.horizontalGradient(
                                                listOf(Color(0x1AFFFFFF), Color(0x1AFFFFFF))
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            strings.checkAnswer,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            letterSpacing = 1.sp,
                                            color = if (isCheckEnabled) Color(0xFF0F172A) else Color(0x44FFFFFF)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = if (isCheckEnabled) Color(0xFF0F172A) else Color(0x44FFFFFF),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Post-check Sheet (Celebratory Correct or Guided Review Incorrect)
                    val currentQ = questions[currentQuestion]
                    val bannerBg = if (isCurrentCorrect) Brush.verticalGradient(
                        listOf(Color(0xF50D3320), Color(0xF5051810))
                    ) else Brush.verticalGradient(
                        listOf(Color(0xF53D1219), Color(0xF51E070B))
                    )
                    val bannerBorder = if (isCurrentCorrect) Color(0xFF22C55E) else Color(0xFFEF4444)
                    val iconTint = if (isCurrentCorrect) Color(0xFF51FAC1) else Color(0xFFFF6B6B)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Transparent,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, bannerBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bannerBg)
                                .navigationBarsPadding()
                                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp)
                        ) {
                            // Header row: Icon + Notice + Combo Tag
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrentCorrect) Color(0x3322C55E) else Color(0x33EF4444)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = iconTint,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isCurrentCorrect) strings.correctNotice else strings.incorrectNotice,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                if (isCurrentCorrect && consecutiveCorrect >= 2) {
                                    val bonus = when (consecutiveCorrect) {
                                        2 -> "+20 XP"
                                        3 -> "+30 XP"
                                        4 -> "+40 XP"
                                        else -> "+60 XP"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x33FFD166))
                                            .border(1.dp, Color(0xFFFFD166), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "🔥 ${consecutiveCorrect}x ($bonus)",
                                            color = Color(0xFFFFD166),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            // If incorrect: reveal correct answer with TTS audio + 1-tap save to vault
                            if (!isCurrentCorrect) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val correctText = if (currentQ.type == QuestionType.SENTENCE_BUILDER) {
                                    currentQ.correctSentence
                                } else {
                                    currentQ.options.getOrNull(currentQ.correctIndex) ?: ""
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0x26FFD166))
                                        .border(1.dp, Color(0x4DFFD166), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.correctAnswerNotice,
                                            color = Color(0xB3FFFFFF),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = correctText,
                                            color = Color(0xFFFFD166),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.ttsManager.speak(correctText, isSlow = false)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Speak Normal",
                                                tint = Color(0xFFFFD166),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.ttsManager.speak(correctText, isSlow = true)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text("🐢", fontSize = 16.sp)
                                        }
                                    }
                                }

                                // 1-tap save to vault button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            if (!isSavedToVault) {
                                                isSavedToVault = true
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.saveWord(
                                                    word = currentQ.question,
                                                    meaning = correctText,
                                                    example = "Answer: $correctText"
                                                )
                                                Toast.makeText(
                                                    context,
                                                    if (isEnglish) "Saved to Vocabulary Vault! ⭐" else "Đã lưu vào Sổ tay từ vựng! ⭐",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSavedToVault) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                                            contentDescription = "Save to vault",
                                            tint = Color(0xFF51FAC1),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isSavedToVault) strings.savedToVault else strings.saveToVault,
                                            color = Color(0xFF51FAC1),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Nút TIẾP TỤC (Continue)
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val activeQ = questions[currentQuestion]
                                    if (activeQ.type == QuestionType.SENTENCE_BUILDER) {
                                        userAnswers[currentQuestion] = if (isCurrentCorrect) 0 else -1
                                    } else {
                                        userAnswers[currentQuestion] = selectedOption
                                    }
                                    if (currentQuestion < totalQuestions - 1) {
                                        currentQuestion++
                                        selectedOption = -1
                                        selectedTokenIndices = emptyList()
                                        isChecked = false
                                        isSavedToVault = false
                                    } else {
                                        val finalResults = questions.mapIndexed { idx, item ->
                                            if (item.type == QuestionType.SENTENCE_BUILDER) {
                                                val assembled = item.userSentenceTokens.joinToString(" ")
                                                val isItemCorrect = userAnswers[idx] == 0
                                                val copy = QuestItem(
                                                    question = item.question,
                                                    options = if (isItemCorrect) listOf(item.correctSentence) else listOf(item.correctSentence, assembled.ifEmpty { "Incomplete" }),
                                                    correctIndex = 0,
                                                    type = item.type,
                                                    sentenceTokens = item.sentenceTokens,
                                                    correctSentence = item.correctSentence,
                                                    userSentenceTokens = item.userSentenceTokens
                                                )
                                                copy.selectedIndex = if (isItemCorrect) 0 else 1
                                                copy
                                            } else {
                                                val copy = QuestItem(item.question, item.options, item.correctIndex)
                                                copy.selectedIndex = userAnswers[idx] ?: -1
                                                copy
                                            }
                                        }
                                        viewModel.completeCurrentLesson()
                                        viewModel.analyzeQuestResults(context, finalResults)
                                        onFinish()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrentCorrect) Color(0xFF22C55E) else Color(0xFFEF4444),
                                    contentColor = Color.White
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        strings.continueLesson,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hộp thoại xác nhận thoát bài học
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                containerColor = Color(0xFF1A1A2E),
                title = {
                    Text(strings.pauseLessonTitle, color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(strings.pauseLessonDesc, color = Color(0xCCFFFFFF), fontSize = 14.sp)
                },
                confirmButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text(strings.resumeLesson, color = Color(0xFF51FAC1), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            onFinish()
                        }
                    ) {
                        Text(strings.exitLesson, color = Color(0xFFFF6B6B))
                    }
                }
            )
        }

        // Confetti Effect for milestone combos
        ConfettiEffect(
            visible = showConfetti,
            onDismiss = { showConfetti = false }
        )
    }
}
