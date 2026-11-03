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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.model.MascotEmotion
import com.example.model.MascotQuotes
import com.example.model.QuestItem
import com.example.model.QuestionType
import com.example.ui.ConfettiEffect
import com.example.ui.MatchingPairsView
import com.example.ui.ListeningChallengeView
import com.example.ui.SentenceBuilderView
import com.example.ui.SpeakingChallengeView
import com.example.ui.mascot.LivingMascotView
import com.example.ui.mascot.MascotSpeechBubble
import com.example.ui.safeBottomDockPadding
import com.example.util.PronunciationResult
import com.example.viewmodel.StudyViewModel
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()

    // Chế độ tập trung (Focus Mode): Giữ màn hình luôn sáng, không bị tắt màn hình / gây gián đoạn khi đang làm bài
    val activity = LocalContext.current as? android.app.Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF27E0A9))
        }
        return
    }

    var currentQuestion by remember { mutableIntStateOf(0) }
    val hearts by viewModel.hearts.observeAsState(5)
    val streakShields by viewModel.streakShields.observeAsState(1)
    val streak by viewModel.streak.observeAsState(0)

    var remainingRetries by remember(currentQuestion) {
        mutableIntStateOf(questions.getOrNull(currentQuestion)?.maxRetries ?: 1)
    }
    var isRetryPhase by remember { mutableStateOf(false) }
    var showOutOfHeartsDialog by remember { mutableStateOf(false) }
    var showMockShopSheet by remember { mutableStateOf(false) }
    var showStreakRestoredEffect by remember { mutableStateOf(false) }

    var selectedOption by remember { mutableIntStateOf(-1) }
    var selectedTokenIndices by remember(currentQuestion) { mutableStateOf<List<Int>>(emptyList()) }
    var isMatchingCompleted by remember(currentQuestion) { mutableStateOf(false) }
    var matchingErrorCount by remember(currentQuestion) { mutableIntStateOf(0) }
    var speakingResult by remember(currentQuestion) { mutableStateOf<PronunciationResult?>(null) }
    var isChecked by remember { mutableStateOf(false) }
    var isCurrentCorrect by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var mascotSpeech by remember { mutableStateOf<String?>(null) }
    var isSavedToVault by remember(currentQuestion) { mutableStateOf(false) }
    var consecutiveCorrect by remember { mutableIntStateOf(0) }
    var showConfetti by remember { mutableStateOf(false) }
    var isPetted by remember { mutableStateOf(false) }
    var idleSeconds by remember(currentQuestion) { mutableIntStateOf(0) }

    // Theo dõi thời gian suy nghĩ của người học
    LaunchedEffect(currentQuestion, isChecked, selectedOption, selectedTokenIndices, isMatchingCompleted, speakingResult) {
        idleSeconds = 0
        if (!isChecked) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                idleSeconds++
            }
        }
    }

    val isListeningActive by viewModel.speechRecognitionManager.isListening

    val currentEmotion = remember(
        isPetted,
        isChecked,
        isCurrentCorrect,
        consecutiveCorrect,
        isListeningActive,
        idleSeconds
    ) {
        when {
            isPetted -> MascotEmotion.PETTED
            isListeningActive -> MascotEmotion.LISTENING
            isChecked && isCurrentCorrect && consecutiveCorrect >= 3 -> MascotEmotion.STREAK
            isChecked && isCurrentCorrect -> MascotEmotion.CHEER
            isChecked && !isCurrentCorrect -> MascotEmotion.EMPATHY
            idleSeconds >= 3 -> MascotEmotion.THINKING
            else -> MascotEmotion.IDLE
        }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F0B1E),
                        Color(0xFF130E26),
                        Color(0xFF090614)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp),
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

                Spacer(modifier = Modifier.width(8.dp))

                // Heart & Shield Indicators
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Hearts Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33EF4444))
                            .border(1.dp, Color(0x66EF4444), RoundedCornerShape(12.dp))
                            .clickable { showMockShopSheet = true }
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = "Hearts", tint = Color(0xFFFF5252), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$hearts",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Shields Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x3351FAC1))
                            .border(1.dp, Color(0x6651FAC1), RoundedCornerShape(12.dp))
                            .clickable { showMockShopSheet = true }
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = "Shields", tint = Color(0xFF51FAC1), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$streakShields",
                                color = Color(0xFF51FAC1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Linh vật Kiki sống động tương tác cảm xúc
                LivingMascotView(
                    emotion = currentEmotion,
                    size = 46.dp,
                    onClick = {
                        coroutineScope.launch {
                            isPetted = true
                            val quoteEmotion = when {
                                isChecked && isCurrentCorrect && consecutiveCorrect >= 3 -> MascotEmotion.STREAK
                                isChecked && isCurrentCorrect -> MascotEmotion.CHEER
                                isChecked && !isCurrentCorrect -> MascotEmotion.EMPATHY
                                currentEmotion == MascotEmotion.THINKING -> MascotEmotion.THINKING
                                else -> MascotEmotion.PETTED
                            }
                            val q = MascotQuotes.getQuoteForEmotion(quoteEmotion, isEnglish, consecutiveCorrect)
                            mascotSpeech = q
                            viewModel.ttsManager.speak(q)
                            kotlinx.coroutines.delay(1800)
                            isPetted = false
                        }
                    }
                )
            }

            // Bong bóng thoại động viên thông minh của linh vật Kiki
            MascotSpeechBubble(
                speech = mascotSpeech,
                emotion = currentEmotion,
                onSpeakClick = {
                    mascotSpeech?.let { speech ->
                        viewModel.ttsManager.speak(speech)
                    }
                },
                onDismiss = { mascotSpeech = null }
            )

            // Scrollable Question Card Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hiển thị linh vật Kiki đọc đề khi ở trong câu hỏi quá lâu (>5s)
                AnimatedVisibility(
                    visible = idleSeconds >= 3 && !isChecked,
                    enter = fadeIn(tween(350)) + expandVertically(),
                    exit = fadeOut(tween(250)) + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x33FFCA28))
                            .border(1.dp, Color(0x66FFCA28), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LivingMascotView(
                            emotion = MascotEmotion.THINKING,
                            isFullBody = true,
                            modifier = Modifier
                                .height(68.dp)
                                .aspectRatio(9f / 16f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) "Kiki is reading the question with you 💡" else "Kiki đang đọc đề cùng bạn nè 💡",
                                color = Color(0xFFFFD166),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEnglish) "Take your time and think carefully!" else "Cứ bình tĩnh suy nghĩ và chọn đáp án chuẩn xác nhé!",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xF215112B))
                            .border(
                                1.5.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color(0x5551FAC1),
                                        Color(0x33A78BFA)
                                    )
                                ),
                                RoundedCornerShape(26.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            val hasDedicatedAudio = currentQ.type == QuestionType.SPEAKING_CHALLENGE || currentQ.type == QuestionType.SENTENCE_BUILDER || currentQ.type == QuestionType.LISTENING_CHALLENGE
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "${strings.questPrefix}${qIdx + 1}: $chapterTitle",
                                    color = Color(0xFF27E0A9),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!hasDedicatedAudio) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Nút nghe phát âm tiếng Anh
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x3351FAC1))
                                                .border(1.dp, Color(0x6651FAC1), CircleShape)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    val textToSpeak = when (currentQ.type) {
                                                        QuestionType.MATCHING_PAIRS -> currentQ.matchingPairs.joinToString(", ") { it.english }
                                                        else -> currentQ.question
                                                    }
                                                    viewModel.ttsManager.speak(textToSpeak, isSlow = false)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Speak Question",
                                                tint = if (isSpeaking) Color(0xFF51FAC1) else Color(0xFFFFD166),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        // Nút nghe phát âm chậm ELSA Style 0.68x
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x33FFB703))
                                                .border(1.dp, Color(0x66FFB703), CircleShape)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    val textToSpeak = when (currentQ.type) {
                                                        QuestionType.MATCHING_PAIRS -> currentQ.matchingPairs.joinToString(", ") { it.english }
                                                        else -> currentQ.question
                                                    }
                                                    viewModel.ttsManager.speak(textToSpeak, isSlow = true)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🐢", fontSize = 16.sp)
                                        }
                                    }
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

                            when (currentQ.type) {
                                QuestionType.SENTENCE_BUILDER -> {
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
                                }
                                QuestionType.LISTENING_CHALLENGE -> {
                                    ListeningChallengeView(
                                        sentenceToListen = currentQ.speakingSentence.ifEmpty { currentQ.correctSentence },
                                        availableTokens = currentQ.sentenceTokens,
                                        selectedTokenIndices = selectedTokenIndices,
                                        isChecked = isChecked,
                                        isCorrect = isCurrentCorrect,
                                        isEnglish = isEnglish,
                                        isSpeaking = isSpeaking,
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
                                        onPlayAudio = {
                                            val sentence = currentQ.speakingSentence.ifEmpty { currentQ.correctSentence }
                                            viewModel.ttsManager.speak(sentence, isSlow = false)
                                        },
                                        onPlaySlow = {
                                            val sentence = currentQ.speakingSentence.ifEmpty { currentQ.correctSentence }
                                            viewModel.ttsManager.speak(sentence, isSlow = true)
                                        }
                                    )
                                }
                                QuestionType.MATCHING_PAIRS -> {
                                    MatchingPairsView(
                                        pairs = currentQ.matchingPairs,
                                        isEnglish = isEnglish,
                                        onSpeakWord = { word ->
                                            viewModel.ttsManager.speak(word, isSlow = false)
                                        },
                                        onPairMatched = {
                                            viewModel.soundEffectManager.playCorrect(consecutiveCorrect + 1)
                                        },
                                        onMismatch = {
                                            viewModel.soundEffectManager.playIncorrect()
                                        },
                                        onAllMatched = { errors ->
                                            isMatchingCompleted = true
                                            matchingErrorCount = errors
                                        }
                                    )
                                }
                                QuestionType.SPEAKING_CHALLENGE -> {
                                    SpeakingChallengeView(
                                        targetSentence = currentQ.speakingSentence.ifEmpty { currentQ.question },
                                        isEnglish = isEnglish,
                                        speechManager = viewModel.speechRecognitionManager,
                                        onSpeakSentence = { isSlow ->
                                            viewModel.ttsManager.speak(currentQ.speakingSentence.ifEmpty { currentQ.question }, isSlow = isSlow)
                                        },
                                        onSpeakWord = { word ->
                                            viewModel.ttsManager.speak(word, isSlow = false)
                                        },
                                        onEvaluationComplete = { result ->
                                            speakingResult = result
                                            currentQ.pronunciationScore = result.overallScore
                                        }
                                    )
                                }
                                else -> {
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

                            val isCheckEnabled = when (questions[currentQuestion].type) {
                                QuestionType.SENTENCE_BUILDER -> selectedTokenIndices.isNotEmpty()
                                QuestionType.LISTENING_CHALLENGE -> selectedTokenIndices.isNotEmpty()
                                QuestionType.MATCHING_PAIRS -> isMatchingCompleted
                                QuestionType.SPEAKING_CHALLENGE -> speakingResult != null
                                else -> selectedOption != -1
                            }
                            Button(
                                onClick = {
                                    val currentQ = questions[currentQuestion]
                                    isChecked = true
                                    val isCorrect = when (currentQ.type) {
                                        QuestionType.SENTENCE_BUILDER -> {
                                            val assembledWords = selectedTokenIndices.mapNotNull { currentQ.sentenceTokens.getOrNull(it) }
                                            currentQ.userSentenceTokens = assembledWords
                                            val assembledSentence = assembledWords.joinToString(" ")
                                            fun normalize(s: String) = s.lowercase(Locale.ROOT)
                                                .replace(Regex("[.,!?;:\"]"), "")
                                                .replace(Regex("\\s+"), " ")
                                                .trim()
                                            normalize(assembledSentence) == normalize(currentQ.correctSentence)
                                        }
                                        QuestionType.LISTENING_CHALLENGE -> {
                                            val assembledWords = selectedTokenIndices.mapNotNull { currentQ.sentenceTokens.getOrNull(it) }
                                            currentQ.userSentenceTokens = assembledWords
                                            val assembledSentence = assembledWords.joinToString(" ")
                                            fun normalize(s: String) = s.lowercase(Locale.ROOT)
                                                .replace(Regex("[.,!?;:\"]"), "")
                                                .replace(Regex("\\s+"), " ")
                                                .trim()
                                            normalize(assembledSentence) == normalize(currentQ.correctSentence)
                                        }
                                        QuestionType.MATCHING_PAIRS -> {
                                            matchingErrorCount <= 2
                                        }
                                        QuestionType.SPEAKING_CHALLENGE -> {
                                            (speakingResult?.overallScore ?: 0) >= 70
                                        }
                                        else -> {
                                            selectedOption == currentQ.correctIndex
                                        }
                                    }
                                    isCurrentCorrect = isCorrect
                                    if (isCorrect) {
                                        consecutiveCorrect++
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.soundEffectManager.playCorrect(consecutiveCorrect)
                                        if (consecutiveCorrect >= 3) {
                                            showConfetti = true
                                        }
                                        val checkEmotion = if (consecutiveCorrect >= 3) MascotEmotion.STREAK else MascotEmotion.CHEER
                                        mascotSpeech = MascotQuotes.getQuoteForEmotion(checkEmotion, isEnglish, consecutiveCorrect)
                                        isRetryPhase = false
                                    } else {
                                        consecutiveCorrect = 0
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.soundEffectManager.playIncorrect()
                                        mascotSpeech = MascotQuotes.getQuoteForEmotion(MascotEmotion.EMPATHY, isEnglish, consecutiveCorrect)
                                        val newHearts = viewModel.deductHeart()
                                        if (newHearts <= 0) {
                                            showOutOfHeartsDialog = true
                                            isRetryPhase = false
                                        } else if (remainingRetries > 0) {
                                            remainingRetries--
                                            isRetryPhase = true
                                        } else {
                                            isRetryPhase = false
                                        }
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
                                    // Linh vật Kiki sống động vỗ tay ăn mừng / chia buồn
                                    LivingMascotView(
                                        emotion = if (isCurrentCorrect && consecutiveCorrect >= 3) MascotEmotion.STREAK
                                                  else if (isCurrentCorrect) MascotEmotion.CHEER
                                                  else MascotEmotion.EMPATHY,
                                        isFullBody = true,
                                        modifier = Modifier
                                            .height(84.dp)
                                            .aspectRatio(9f / 16f)
                                            .padding(end = 12.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = if (isCurrentCorrect) {
                                                if (consecutiveCorrect >= 3) {
                                                    if (isEnglish) "🎉 AMAZING! 3+ STREAK CLAP! 👏" else "🎉 ĐỈNH CAO! 5 CÂU LIÊN TIẾP! 👏"
                                                } else strings.correctNotice
                                            } else if (isRetryPhase) {
                                                if (isEnglish) "⚡ NOT QUITE! (Retries left: $remainingRetries)" else "⚡ CHƯA ĐÚNG! (Còn $remainingRetries lần thử lại)"
                                            } else strings.incorrectNotice,
                                            color = Color.White,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        val encouragementText = if (!isCurrentCorrect && isRetryPhase) {
                                            if (isEnglish) "This question allows retries! Review carefully and try again 💪"
                                            else "Câu hỏi này cho phép sửa sai! Hãy bình tĩnh xem lại và thử lại nhé 💪"
                                        } else when (currentQ.type) {
                                            QuestionType.LISTENING_CHALLENGE -> if (isCurrentCorrect) {
                                                if (isEnglish) "Awesome! Your listening ear is spot on! 🎧✨"
                                                else "Tuyệt vời! Bạn nghe rất chuẩn và bắt nhịp cực kỳ chính xác! 🎧✨"
                                            } else {
                                                if (isEnglish) "Keep going! Tap speaker to listen again and master it 💪"
                                                else "Đừng nản lòng! Hãy bấm nút loa để nghe lại một lần nữa và ghi nhớ nhé 💪"
                                            }
                                            QuestionType.SPEAKING_CHALLENGE -> if (isCurrentCorrect) {
                                                if (isEnglish) "Great pronunciation! Native-like cadence! 🎙️"
                                                else "Phát âm rất tốt! Ngữ điệu chuẩn bản xứ! 🎙️"
                                            } else {
                                                if (isEnglish) "Speak clearly into the microphone to improve! 🎙️"
                                                else "Hãy kiên trì luyện tập! Phát âm to và rõ ràng hơn nhé 🎙️"
                                            }
                                            QuestionType.SENTENCE_BUILDER -> if (isCurrentCorrect) {
                                                if (isEnglish) "Perfect sentence structure! 🧩"
                                                else "Cấu trúc câu hoàn hảo! Bạn sắp xếp từ rất chuẩn 🧩"
                                            } else {
                                                if (isEnglish) "Review the correct word order below! 💡"
                                                else "Xem kỹ thứ tự các từ trong câu chuẩn bên dưới nhé! 💡"
                                            }
                                            QuestionType.MATCHING_PAIRS -> if (isCurrentCorrect) {
                                                if (isEnglish) "Flawless matching! Great vocabulary memory! 🎯"
                                                else "Ghép cặp chuẩn xác! Trí nhớ từ vựng rất tuyệt vời! 🎯"
                                            } else {
                                                if (isEnglish) "Review the word pairs below to remember! 📚"
                                                else "Xem lại các cặp từ bên dưới để ghi nhớ nhé! 📚"
                                            }
                                            else -> if (isCurrentCorrect) {
                                                if (isEnglish) "Spot on! Keep up the brilliant momentum! 🔥"
                                                else "Chính xác! Tiếp tục giữ vững phong độ xuất sắc nhé! 🔥"
                                            } else {
                                                if (isEnglish) "Learn from mistakes to conquer next time! 💪"
                                                else "Sai một lần để nhớ lâu hơn! Cố gắng ở câu tiếp theo nhé 💪"
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = encouragementText,
                                            color = if (isCurrentCorrect) Color(0xFF51FAC1) else Color(0xFFFFD166),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
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

                            // If incorrect and in retry phase: show retry prompt without spoiling answer
                            if (!isCurrentCorrect && isRetryPhase) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0x26FFAA00))
                                        .border(1.dp, Color(0x4DFFAA00), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Retry",
                                        tint = Color(0xFFFFD166),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isEnglish) "Remaining retries: $remainingRetries. You can do this!" else "Còn $remainingRetries lượt làm lại cho câu này. Cố lên nhé!",
                                        color = Color(0xFFFFD166),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // If incorrect and retries exhausted: reveal correct answer with TTS audio + 1-tap save to vault
                            if (!isCurrentCorrect && !isRetryPhase) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val correctText = when (currentQ.type) {
                                    QuestionType.SENTENCE_BUILDER -> currentQ.correctSentence
                                    QuestionType.LISTENING_CHALLENGE -> currentQ.correctSentence
                                    QuestionType.MATCHING_PAIRS -> currentQ.matchingPairs.joinToString("\n") { "• ${it.english} ↔ ${it.vietnamese}" }
                                    QuestionType.SPEAKING_CHALLENGE -> currentQ.speakingSentence.ifEmpty { currentQ.question }
                                    else -> currentQ.options.getOrNull(currentQ.correctIndex) ?: ""
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
                                                val textToSpeak = when (currentQ.type) {
                                                    QuestionType.MATCHING_PAIRS -> currentQ.matchingPairs.joinToString(", ") { it.english }
                                                    else -> correctText
                                                }
                                                viewModel.ttsManager.speak(textToSpeak, isSlow = false)
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
                                                val textToSpeak = when (currentQ.type) {
                                                    QuestionType.MATCHING_PAIRS -> currentQ.matchingPairs.joinToString(", ") { it.english }
                                                    else -> correctText
                                                }
                                                viewModel.ttsManager.speak(textToSpeak, isSlow = true)
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

                            if (isRetryPhase) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isRetryPhase = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66EF4444)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFFFF6B6B)
                                        )
                                    ) {
                                        Text(
                                            if (isEnglish) "Skip & See" else "Bỏ qua & Xem",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isChecked = false
                                            isRetryPhase = false
                                            selectedOption = -1
                                            selectedTokenIndices = emptyList()
                                            isMatchingCompleted = false
                                            matchingErrorCount = 0
                                            speakingResult = null
                                            mascotSpeech = null
                                        },
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(50.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEF4444)
                                        )
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (isEnglish) "TRY AGAIN ($remainingRetries)" else "THỬ LẠI ($remainingRetries)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                // Nút TIẾP TỤC (Continue)
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val activeQ = questions[currentQuestion]
                                        when (activeQ.type) {
                                            QuestionType.SENTENCE_BUILDER, QuestionType.LISTENING_CHALLENGE, QuestionType.MATCHING_PAIRS, QuestionType.SPEAKING_CHALLENGE -> {
                                                userAnswers[currentQuestion] = if (isCurrentCorrect) 0 else -1
                                            }
                                            else -> {
                                                userAnswers[currentQuestion] = selectedOption
                                            }
                                        }
                                        if (currentQuestion < totalQuestions - 1) {
                                            currentQuestion++
                                            selectedOption = -1
                                            selectedTokenIndices = emptyList()
                                            isMatchingCompleted = false
                                            matchingErrorCount = 0
                                            speakingResult = null
                                            isChecked = false
                                            isSavedToVault = false
                                            mascotSpeech = null
                                            remainingRetries = questions.getOrNull(currentQuestion)?.maxRetries ?: 1
                                            isRetryPhase = false
                                        } else {
                                            val finalResults = questions.mapIndexed { idx, item ->
                                            when (item.type) {
                                                QuestionType.SENTENCE_BUILDER, QuestionType.LISTENING_CHALLENGE -> {
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
                                                }
                                                QuestionType.MATCHING_PAIRS -> {
                                                    val isItemCorrect = userAnswers[idx] == 0
                                                    val pairsSummary = item.matchingPairs.joinToString(", ") { "${it.english} = ${it.vietnamese}" }
                                                    val copy = QuestItem(
                                                        question = item.question,
                                                        options = listOf(pairsSummary),
                                                        correctIndex = 0,
                                                        type = item.type,
                                                        matchingPairs = item.matchingPairs
                                                    )
                                                    copy.selectedIndex = if (isItemCorrect) 0 else 1
                                                    copy
                                                }
                                                QuestionType.SPEAKING_CHALLENGE -> {
                                                    val isItemCorrect = userAnswers[idx] == 0
                                                    val copy = QuestItem(
                                                        question = item.question,
                                                        options = listOf(item.speakingSentence),
                                                        correctIndex = 0,
                                                        type = item.type,
                                                        speakingSentence = item.speakingSentence,
                                                        pronunciationScore = item.pronunciationScore
                                                    )
                                                    copy.selectedIndex = if (isItemCorrect) 0 else 1
                                                    copy
                                                }
                                                else -> {
                                                    val copy = QuestItem(item.question, item.options, item.correctIndex)
                                                    copy.selectedIndex = userAnswers[idx] ?: -1
                                                    copy
                                                }
                                            }
                                        }
                                        val correctCount = finalResults.count { it.selectedIndex == it.correctIndex }
                                        val totalCount = finalResults.size
                                        val isPassed = totalCount > 0 && (correctCount.toFloat() / totalCount >= 0.5f)
                                        if (isPassed) {
                                            viewModel.completeCurrentLesson()
                                        }
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
                            // Ghi nhận trạng thái từ bỏ bài làm: kết quả chưa đạt yêu cầu
                            val abandonedResults = questions.mapIndexed { index, q ->
                                val userAns = userAnswers[index] ?: -1
                                q.copy(selectedIndex = userAns)
                            }
                            viewModel.recordAbandonedQuest(abandonedResults)
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

        // Hộp thoại Hết Tim (Out Of Hearts)
        if (showOutOfHeartsDialog) {
            AlertDialog(
                onDismissRequest = { /* Không đóng khi chưa chọn cách xử lý */ },
                containerColor = Color(0xFF131722),
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💔", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "Out of Hearts!" else "Hết Mạng Rồi!",
                            color = Color(0xFFFF5252),
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = if (isEnglish)
                                "You've depleted all hearts. Protect your progress with a Cosmic Shield or refill for free!"
                            else
                                "Bạn đã hết mạng để tiếp tục bài học. Dùng Khiên Vũ Trụ để phục hồi chuỗi hoặc nạp đầy tim miễn phí!",
                            color = Color(0xCCFFFFFF),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (streakShields > 0) {
                            Button(
                                onClick = {
                                    val success = viewModel.useShieldToRestoreHearts()
                                    if (success) {
                                        showOutOfHeartsDialog = false
                                        showStreakRestoredEffect = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4))
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isEnglish) "Use 1 Shield (Have $streakShields)" else "Dùng 1 Khiên Hồi 5 Tim (Còn $streakShields)",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.refillHearts(5)
                                showOutOfHeartsDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF51FAC1))
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "Refill 5 Hearts (Free / Practice)" else "Nạp Đầy 5 Tim Miễn Phí",
                                color = Color(0xFF51FAC1),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = {
                            showOutOfHeartsDialog = false
                            onFinish()
                        }
                    ) {
                        Text(
                            text = if (isEnglish) "Exit Lesson" else "Thoát Bài Học",
                            color = Color(0x88FFFFFF)
                        )
                    }
                }
            )
        }

        // Cửa hàng giả định (Mock Shop & Debug Overlay)
        if (showMockShopSheet) {
            AlertDialog(
                onDismissRequest = { showMockShopSheet = false },
                containerColor = Color(0xFF131722),
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛒", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "Mock Shop & Inventory" else "Cửa Hàng Giả Định & Vật Phẩm",
                            color = Color(0xFF51FAC1),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isEnglish)
                                "Test heart loss, shield recovery, and streak restoration here:"
                            else
                                "Khu vực kiểm thử mạng (tim), khiên bảo vệ và khôi phục chuỗi streak:",
                            color = Color(0x99FFFFFF),
                            fontSize = 12.5.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Heart controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("❤️ $hearts/5", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { viewModel.deductHeart() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("-1 Tim", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { viewModel.refillHearts(1) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x3322C55E)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("+1 Tim", color = Color(0xFF51FAC1), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { viewModel.refillHearts(5) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("Hồi 5", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Black) }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Shield controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🛡️ $streakShields", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { if (streakShields > 0) viewModel.addShields(-1) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("-1 Khiên", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { viewModel.addShields(1) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("+1 Khiên", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Black) }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Streak controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🔥 Streak: $streak ngày", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { viewModel.setMockStreak(0) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("Về 0", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { viewModel.setMockStreak(streak + 1) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFD166)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("+1 Ngày", color = Color(0xFFFFD166), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action: Use Shield to Restore Streak / Hearts with animation
                        Button(
                            onClick = {
                                val success = viewModel.restoreStreakWithShield()
                                if (success) {
                                    showMockShopSheet = false
                                    showStreakRestoredEffect = true
                                } else {
                                    Toast.makeText(context, if (isEnglish) "You need at least 1 Shield!" else "Bạn cần ít nhất 1 Khiên Vũ Trụ!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1))
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "Trigger Shield Recovery Effect 🛡️" else "Kích Hoạt Khôi Phục Khiên 🛡️",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMockShopSheet = false }) {
                        Text(if (isEnglish) "Close" else "Đóng", color = Color(0xFF51FAC1), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Hiệu ứng khôi phục khi dùng khiên vũ trụ (Cosmic Shield Recovery Overlay)
        if (showStreakRestoredEffect) {
            LaunchedEffect(Unit) {
                viewModel.soundEffectManager.playFanfare()
                kotlinx.coroutines.delay(2800)
                showStreakRestoredEffect = false
            }

            val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0.85f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow_alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { showStreakRestoredEffect = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulseScale),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glowing cosmic ring
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF00F5D4).copy(alpha = glowAlpha * 0.35f))
                                .border(3.dp, Color(0xFF00F5D4), CircleShape)
                        )
                        // Inner circle
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFF51FAC1), Color(0xFF0F172A))))
                                .border(2.dp, Color(0xFFFFD166), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = "Restored Shield",
                                tint = Color.White,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (isEnglish) "✨ SHIELD ACTIVATED! ✨" else "✨ KHIÊN ĐÃ KÍCH HOẠT! ✨",
                        color = Color(0xFF00F5D4),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isEnglish)
                            "Your streak & hearts have been magically preserved by the Cosmic Shield!"
                        else
                            "Chuỗi ngày học & Mạng (Tim) của bạn đã được Khiên Vũ Trụ bảo vệ trọn vẹn!",
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isEnglish) "(Tap anywhere to continue)" else "(Chạm để tiếp tục)",
                        color = Color(0x88FFFFFF),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
