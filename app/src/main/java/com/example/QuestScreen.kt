package com.example

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import coil.compose.AsyncImage
import com.example.ui.mascot.LivingMascotView
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import com.example.model.MascotEmotion
import com.example.model.MascotQuotes
import com.example.model.QuestItem
import com.example.model.QuestionType
import com.example.ui.ConfettiEffect
import com.example.ui.MatchingPairsView
import com.example.ui.ListeningChallengeView
import com.example.ui.SentenceBuilderView
import com.example.ui.SpeakingChallengeView
import com.example.ui.OutOfHeartsDialog
import com.example.util.PronunciationResult
import com.example.viewmodel.StudyViewModel
import kotlinx.coroutines.launch
import java.util.Locale

// --- Duolingo-style Speech Bubble Shape with Left Tail pointing to Kiki ---
class SpeechBubbleLeftTailShape(
    private val cornerRadius: Dp = 18.dp,
    private val tailWidth: Dp = 8.dp,
    private val tailHeight: Dp = 14.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = with(density) { cornerRadius.toPx() }
        val tw = with(density) { tailWidth.toPx() }
        val th = with(density) { tailHeight.toPx() }

        val path = Path().apply {
            moveTo(tw + r, 0f)
            lineTo(size.width - r, 0f)
            arcTo(
                rect = Rect(size.width - 2 * r, 0f, size.width, 2 * r),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(size.width, size.height - r)
            arcTo(
                rect = Rect(size.width - 2 * r, size.height - 2 * r, size.width, size.height),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(tw + r, size.height)
            arcTo(
                rect = Rect(tw, size.height - 2 * r, tw + 2 * r, size.height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            val tailCenterY = size.height * 0.45f
            val tailTopY = (tailCenterY - th / 2).coerceAtLeast(r)
            val tailBottomY = (tailCenterY + th / 2).coerceAtMost(size.height - r)
            lineTo(tw, tailBottomY)
            lineTo(0f, tailCenterY)
            lineTo(tw, tailTopY)
            lineTo(tw, r)
            arcTo(
                rect = Rect(tw, 0f, tw + 2 * r, 2 * r),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            close()
        }
        return Outline.Generic(path)
    }
}

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

    // Chế độ tập trung (Focus Mode): Giữ màn hình luôn sáng, không bị tắt màn hình / gây gián đoạn khi đang làm bài
    val activity = LocalContext.current as? android.app.Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val streakShields by viewModel.streakShields.observeAsState(1)
    val streak by viewModel.streak.observeAsState(0)

    var showMockShopSheet by remember { mutableStateOf(false) }
    var showStreakRestoredEffect by remember { mutableStateOf(false) }

    val questions by viewModel.questions.observeAsState(emptyList())
    val chapterTitle by viewModel.currentChapterTitle.observeAsState("English Fundamentals")
    val currentNode by viewModel.currentNode.observeAsState(1)
    val isSpeaking by viewModel.ttsManager.isSpeaking
    val hearts by viewModel.hearts.observeAsState(5)
    val stars by viewModel.stars.observeAsState(0)

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF27E0A9))
        }
        return
    }

    val activeQuestions = remember(questions) { mutableStateListOf<QuestItem>().apply { addAll(questions) } }
    val originalTotal = remember(questions) { questions.size }
    val missedQuestionsQueue = remember { mutableListOf<Pair<Int, QuestItem>>() }
    val reviewToOriginalMap = remember { mutableStateMapOf<Int, Int>() }
    val reviewAttemptResults = remember { mutableStateMapOf<Int, Boolean>() }
    val firstAttemptResults = remember { mutableStateMapOf<Int, Boolean>() }
    val firstAttemptUserAnswers = remember { mutableStateMapOf<Int, Int>() }
    var isReviewPhase by remember { mutableStateOf(false) }
    var showReviewIntro by remember { mutableStateOf(false) }
    var reviewTotalCount by remember { mutableIntStateOf(0) }

    var currentQuestion by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableIntStateOf(-1) }
    var selectedTokenIndices by remember(currentQuestion) { mutableStateOf<List<Int>>(emptyList()) }
    var isMatchingCompleted by remember(currentQuestion) { mutableStateOf(false) }
    var matchingErrorCount by remember(currentQuestion) { mutableIntStateOf(0) }
    var speakingResult by remember(currentQuestion) { mutableStateOf<PronunciationResult?>(null) }
    var isChecked by remember { mutableStateOf(false) }
    var isCurrentCorrect by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showOutOfHeartsDialog by remember { mutableStateOf(false) }
    var mascotSpeech by remember { mutableStateOf<String?>(null) }
    var isSavedToVault by remember(currentQuestion) { mutableStateOf(false) }
    var consecutiveCorrect by remember { mutableIntStateOf(0) }
    var showConfetti by remember { mutableStateOf(false) }
    var isPetted by remember { mutableStateOf(false) }
    var idleSeconds by remember(currentQuestion) { mutableIntStateOf(0) }
    var questionStartTime by remember(currentQuestion) { mutableLongStateOf(System.currentTimeMillis()) }
    val questionLatencies = remember { mutableStateMapOf<Int, Long>() }
    val questionHintsUsed = remember { mutableStateMapOf<Int, Boolean>() }

    // Kiểm tra hết tim ngay khi vào bài học
    LaunchedEffect(Unit) {
        if ((viewModel.hearts.value ?: 5) <= 0) {
            showOutOfHeartsDialog = true
        }
    }

    // Intercept hardware / gesture Back on Android
    BackHandler {
        showExitDialog = true
    }

    // Idle thinking timer
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

    val userAnswers = remember { mutableStateMapOf<Int, Int>() }
    val totalQuestions = activeQuestions.size

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
        // Nền Vũ Trụ Galaxy Siêu Thực Dịu Mắt
        CosmicPhotorealisticSpaceBackground(
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TOP HEADER: 1 Single Clean Unified Row (No clutter, 0% cut-off text) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subtle back arrow button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showExitDialog = true
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Exit lesson",
                        tint = Color(0x99FFFFFF),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Progress Bar (Rounded pill)
                val progressRatio = if (totalQuestions > 0) ((currentQuestion + 1).toFloat() / totalQuestions).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressRatio)
                            .clip(CircleShape)
                            .background(
                                if (isReviewPhase) {
                                    Brush.horizontalGradient(listOf(Color(0xFFFFD166), Color(0xFF51FAC1)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color(0xFFA3E635), Color(0xFF2DD4BF)))
                                }
                            )
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Question progress counter: 2/5 or Review indicator
                if (isReviewPhase) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${currentQuestion + 1}/$totalQuestions",
                            color = Color(0xFFFFD166),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else {
                    Text(
                        text = "${currentQuestion + 1}/$totalQuestions",
                        color = Color(0xCCFFFFFF),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Hearts & Shield Indicators
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Hearts Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x22FF4D6D))
                            .border(1.dp, Color(0x44FF4D6D), RoundedCornerShape(14.dp))
                            .clickable { showMockShopSheet = true }
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_stat_heart),
                            contentDescription = "Hearts",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$hearts",
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Shields Pill (if shields available)
                    if (streakShields > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x3351FAC1))
                                .border(1.dp, Color(0x6651FAC1), RoundedCornerShape(14.dp))
                                .clickable { showMockShopSheet = true }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_stat_shield),
                                contentDescription = "Shields",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$streakShields",
                                color = Color(0xFF51FAC1),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // --- Scrollable Open Canvas Question Area (No heavy outer cards) ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                AnimatedContent(
                    targetState = currentQuestion,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    },
                    label = "question_transition"
                ) { qIdx ->
                    val currentQ = activeQuestions.getOrNull(qIdx) ?: return@AnimatedContent
                    val currentMascotRes = when {
                        isChecked && isCurrentCorrect -> R.drawable.kiki_happy
                        isChecked && !isCurrentCorrect -> R.drawable.kiki_sad
                        else -> R.drawable.kiki_speaker
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Task Type Label (Subtle, single-line guide)
                        val taskCategoryLabel = when (currentQ.type) {
                            QuestionType.MATCHING_PAIRS -> if (isEnglish) "Match the word pairs" else "Ghép các cặp từ tương ứng"
                            QuestionType.LISTENING_CHALLENGE -> if (isEnglish) "Listen and assemble" else "Nghe và sắp xếp lại câu"
                            QuestionType.SENTENCE_BUILDER -> if (isEnglish) "Assemble the sentence" else "Sắp xếp thành câu hoàn chỉnh"
                            QuestionType.SPEAKING_CHALLENGE -> if (isEnglish) "Speaking challenge" else "Luyện nói câu tiếng Anh"
                            else -> if (isEnglish) "Choose the correct answer" else "Chọn đáp án đúng"
                        }
                        Text(
                            text = taskCategoryLabel.uppercase(Locale.ROOT),
                            color = Color(0x9951FAC1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp, start = 4.dp)
                        )

                        // Kiki Mascot + Speech Bubble Area
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Kiki Mascot (Plays speaker animation once and holds pose pointing to question)
                            Box(
                                modifier = Modifier
                                    .width(82.dp)
                                    .height(124.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                key(qIdx, isChecked, isCurrentCorrect) {
                                    AsyncImage(
                                        model = currentMascotRes,
                                        contentDescription = "Kiki Mentor",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // 2. Speech Bubble pointing left to Kiki
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(SpeechBubbleLeftTailShape(cornerRadius = 18.dp, tailWidth = 8.dp, tailHeight = 14.dp))
                                    .background(Color(0xD91F1A3A))
                                    .border(
                                        1.5.dp,
                                        Brush.linearGradient(listOf(Color(0xFF51FAC1), Color(0xFF818CF8))),
                                        SpeechBubbleLeftTailShape(cornerRadius = 18.dp, tailWidth = 8.dp, tailHeight = 14.dp)
                                    )
                                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
                            ) {
                                when (currentQ.type) {
                                    QuestionType.LISTENING_CHALLENGE -> {
                                        // Speech Bubble IS the Audio Player!
                                        Column {
                                            Text(
                                                text = if (isEnglish) "Listen to Kiki and assemble the sentence:" else "Lắng nghe Kiki đọc câu mẫu:",
                                                color = Color(0xB3FFFFFF),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // Normal Audio button
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(Brush.radialGradient(listOf(Color(0xFF00B0FF), Color(0xFF0284C7))))
                                                        .clickable {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            val sentence = currentQ.speakingSentence.ifEmpty { currentQ.correctSentence }
                                                            viewModel.ttsManager.speak(sentence, isSlow = false)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp,
                                                        contentDescription = "Play Audio",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }

                                                // Turtle Slow Audio button
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0x33FFB703))
                                                        .border(1.dp, Color(0x66FFB703), CircleShape)
                                                        .clickable {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            val sentence = currentQ.speakingSentence.ifEmpty { currentQ.correctSentence }
                                                            viewModel.ttsManager.speak(sentence, isSlow = true)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("🐢", fontSize = 16.sp)
                                                }

                                                Text(
                                                    text = if (isSpeaking) (if (isEnglish) "Playing..." else "Đang phát...") else (if (isEnglish) "Tap to listen" else "Chạm để nghe"),
                                                    color = Color(0xFF51FAC1),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                    QuestionType.MATCHING_PAIRS -> {
                                        Text(
                                            text = if (isEnglish) "Match the corresponding English and Vietnamese word pairs!" else "Chạm chọn các cặp từ tiếng Anh và nghĩa tiếng Việt tương ứng nhé!",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 20.sp
                                        )
                                    }
                                    QuestionType.SENTENCE_BUILDER -> {
                                        Column {
                                            Text(
                                                text = if (isEnglish) "Translate this sentence:" else "Dịch câu này sang tiếng Anh:",
                                                color = Color(0xB3FFFFFF),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = currentQ.question,
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }
                                    QuestionType.SPEAKING_CHALLENGE -> {
                                        Text(
                                            text = if (isEnglish) "Listen to the audio & speak clearly into the microphone 🎙️" else "Lắng nghe câu mẫu và phát âm to, rõ ràng vào micro nhé! 🎙️",
                                            color = Color.White,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 20.sp
                                        )
                                    }
                                    else -> {
                                        // Multiple Choice: Question text + pronunciation button
                                        Column {
                                            Text(
                                                text = currentQ.question,
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 22.sp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0x3351FAC1))
                                                        .border(1.dp, Color(0x6651FAC1), CircleShape)
                                                        .clickable {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            viewModel.ttsManager.speak(currentQ.question, isSlow = false)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp,
                                                        contentDescription = "Speak Question",
                                                        tint = if (isSpeaking) Color(0xFF51FAC1) else Color(0xFFFFD166),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0x33FFB703))
                                                        .border(1.dp, Color(0x66FFB703), CircleShape)
                                                        .clickable {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            viewModel.ttsManager.speak(currentQ.question, isSlow = true)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("🐢", fontSize = 13.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Combo multiplier badge
                        if (consecutiveCorrect >= 2) {
                            val badge = when (consecutiveCorrect) {
                                2 -> ComboBadgeInfo("🔥 COMBO x2 (+20 XP)", Color(0x33FF9E00), Color(0xFFFF9E00), 20)
                                3 -> ComboBadgeInfo("🔥 COMBO x3 (+30 XP)", Color(0x44FF5722), Color(0xFFFF5722), 30)
                                4 -> ComboBadgeInfo("⚡ SIÊU COMBO x4 (+40 XP)", Color(0x44E040FB), Color(0xFFE040FB), 40)
                                else -> ComboBadgeInfo("🌟 HUYỀN THOẠI x5 (+60 XP)", Color(0x44FFD700), Color(0xFFFFD700), 60)
                            }
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
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

                        // Specific Workspace View based on QuestionType
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
                                // Multiple Choice Options (Directly on canvas with clean pill shapes)
                                val options = currentQ.options
                                val labels = listOf("A", "B", "C", "D")

                                options.forEachIndexed { index, text ->
                                    val isSelected = selectedOption == index
                                    val isCorrectAnswer = index == currentQ.correctIndex

                                    val targetAlpha = if (isChecked && !isSelected && !isCorrectAnswer) 0.45f else 1f

                                    val (optionBg, optionBorder, optionTextColor) = when {
                                        isChecked && isCorrectAnswer -> Triple(Color(0xF20F2E20), Color(0xFF22C55E), Color(0xFF51FAC1))
                                        isChecked && isSelected && !isCorrectAnswer -> Triple(Color(0xF238121E), Color(0xFFEF4444), Color(0xFFFF8A8A))
                                        isSelected -> Triple(Color(0xF212263C), Color(0xFF51FAC1), Color.White)
                                        else -> Triple(Color(0xF2191530), Color(0x55818CF8), Color.White)
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
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(animatedBgColor)
                                            .border(
                                                width = if (isSelected || (isChecked && isCorrectAnswer) || (isChecked && isSelected)) 2.dp else 1.5.dp,
                                                color = animatedBorderColor,
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                            .clickable(enabled = !isChecked) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedOption = index
                                            }
                                            .padding(horizontal = 18.dp, vertical = 15.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            val badgeBg = when {
                                                isChecked && isCorrectAnswer -> Color(0xFF22C55E)
                                                isChecked && isSelected && !isCorrectAnswer -> Color(0xFFEF4444)
                                                isSelected -> Color(0xFF51FAC1)
                                                else -> Color(0x33818CF8)
                                            }
                                            val badgeBorder = when {
                                                isChecked && isCorrectAnswer -> Color(0xFF22C55E)
                                                isChecked && isSelected && !isCorrectAnswer -> Color(0xFFEF4444)
                                                isSelected -> Color(0xFF51FAC1)
                                                else -> Color(0x80818CF8)
                                            }
                                            val badgeTextColor = if (isSelected && !isChecked) Color(0xFF0F172A) else Color.White

                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(badgeBg)
                                                    .border(1.5.dp, badgeBorder, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    labels[index],
                                                    color = badgeTextColor,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.5.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Text(
                                                text = text,
                                                color = optionTextColor,
                                                fontSize = 17.sp,
                                                fontWeight = if (isSelected || (isChecked && isCorrectAnswer)) FontWeight.Bold else FontWeight.SemiBold
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
                Spacer(modifier = Modifier.height(140.dp))
            }
        }

        // --- Sticky Bottom Action Dock (Clean, floating, no Skip button) ---
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
                    // Pre-check: Full-width Check Answer button (100% balanced, no skip button)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xF20F0B1E), Color(0xFF0F0B1E))
                                )
                            )
                            .navigationBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp)
                    ) {
                        val isCheckEnabled = when (activeQuestions.getOrNull(currentQuestion)?.type) {
                            QuestionType.SENTENCE_BUILDER -> selectedTokenIndices.isNotEmpty()
                            QuestionType.LISTENING_CHALLENGE -> selectedTokenIndices.isNotEmpty()
                            QuestionType.MATCHING_PAIRS -> isMatchingCompleted
                            QuestionType.SPEAKING_CHALLENGE -> speakingResult != null
                            else -> selectedOption != -1
                        }
                        Button(
                            onClick = {
                                val currentQ = activeQuestions[currentQuestion]
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
                                val measuredLatency = (System.currentTimeMillis() - questionStartTime).coerceAtLeast(150L)
                                questionLatencies[currentQuestion] = measuredLatency
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
                                    
                                } else {
                                    consecutiveCorrect = 0
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.soundEffectManager.playIncorrect()
                                    mascotSpeech = MascotQuotes.getQuoteForEmotion(MascotEmotion.EMPATHY, isEnglish, consecutiveCorrect)
                                    val newHearts = viewModel.deductHeart()
                                    if (newHearts <= 0) {
                                        showOutOfHeartsDialog = true
                                    }
                                }
                            },
                            enabled = isCheckEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
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
                } else {
                    // Post-check Sheet (Streamlined: zero repetitive text, clean actions)
                    val currentQ = activeQuestions[currentQuestion]
                    val bannerBg = if (isCurrentCorrect) Brush.verticalGradient(
                        listOf(Color(0xF00D2D2A), Color(0xF507161F))
                    ) else Brush.verticalGradient(
                        listOf(Color(0xF03D1226), Color(0xF51A0714))
                    )
                    val bannerBorder = if (isCurrentCorrect) Color(0xFF00F5D4) else Color(0xFFFF6B6B)

                    val correctText = when (currentQ.type) {
                        QuestionType.SENTENCE_BUILDER -> currentQ.correctSentence
                        QuestionType.LISTENING_CHALLENGE -> currentQ.correctSentence
                        QuestionType.MATCHING_PAIRS -> currentQ.matchingPairs.joinToString("\n") { "• ${it.english} ↔ ${it.vietnamese}" }
                        QuestionType.SPEAKING_CHALLENGE -> currentQ.speakingSentence.ifEmpty { currentQ.question }
                        else -> currentQ.options.getOrNull(currentQ.correctIndex) ?: ""
                    }

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
                                .padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 14.dp)
                        ) {
                            if (isCurrentCorrect) {
                                // Correct Result Header
                                val feedbackTitle = when {
                                    isReviewPhase -> if (isEnglish) "Awesome! Mistake fixed! 🎉" else "Tuyệt vời! Đã sửa đúng lỗi sai! 🎉"
                                    consecutiveCorrect >= 3 -> if (isEnglish) "Awesome!" else "Tuyệt vời!"
                                    consecutiveCorrect == 2 -> if (isEnglish) "Well done!" else "Tốt lắm!"
                                    else -> if (isEnglish) "Correct!" else "Chính xác!"
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = feedbackTitle,
                                        color = Color(0xFF51FAC1),
                                        fontSize = if (feedbackTitle.length > 20) 17.sp else 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 2
                                    )

                                    if (consecutiveCorrect >= 2) {
                                        val bonus = when (consecutiveCorrect) {
                                            2 -> "+20 XP"
                                            3 -> "+30 XP"
                                            4 -> "+40 XP"
                                            else -> "+60 XP"
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
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
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Incorrect Result: Title on Left, Quick Audio & Save on Right (35% lower height)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isReviewPhase) {
                                            if (isEnglish) "Keep Practicing!" else "Cần luyện tập thêm"
                                        } else {
                                            if (isEnglish) "Incorrect" else "Chưa chính xác"
                                        },
                                        color = Color(0xFFFF6B6B),
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 2
                                    )

                                    if (true) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Quick Audio pronunciation
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x33FFD166))
                                                    .border(1.dp, Color(0x66FFD166), CircleShape)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    val textToSpeak = when (currentQ.type) {
                                                        QuestionType.MATCHING_PAIRS -> currentQ.matchingPairs.joinToString(", ") { it.english }
                                                        else -> correctText
                                                    }
                                                    viewModel.ttsManager.speak(textToSpeak, isSlow = false)
                                                },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Speak Correct Answer",
                                                    tint = Color(0xFFFFD166),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Quick Save to Vault
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSavedToVault) Color(0x3351FAC1) else Color(0x22FFFFFF))
                                                    .border(1.dp, if (isSavedToVault) Color(0xFF51FAC1) else Color(0x44FFFFFF), CircleShape)
                                                .clickable {
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
                                                },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isSavedToVault) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                                                    contentDescription = "Save to vault",
                                                    tint = if (isSavedToVault) Color(0xFF51FAC1) else Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Single Clear Correct Answer Display
                                Text(
                                    text = if (isEnglish) "Correct answer:" else "Đáp án đúng:",
                                    color = Color(0xB3FFFFFF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = correctText,
                                    color = Color(0xFFFFD166),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Continue Button (Đưa câu sai về cuối bài học để làm lại)
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val activeQ = activeQuestions[currentQuestion]
                                    val answerIndex = when (activeQ.type) {
                                        QuestionType.SENTENCE_BUILDER, QuestionType.LISTENING_CHALLENGE, QuestionType.MATCHING_PAIRS, QuestionType.SPEAKING_CHALLENGE -> {
                                            if (isCurrentCorrect) 0 else -1
                                        }
                                        else -> selectedOption
                                    }
                                    userAnswers[currentQuestion] = answerIndex

                                    if (currentQuestion < originalTotal) {
                                        firstAttemptResults[currentQuestion] = isCurrentCorrect
                                        firstAttemptUserAnswers[currentQuestion] = answerIndex
                                        if (!isCurrentCorrect) {
                                            missedQuestionsQueue.add(Pair(currentQuestion, copyForReview(activeQ)))
                                        }
                                    } else {
                                        // Vòng ôn tập sửa lỗi: Mỗi câu sai chỉ được sửa ĐÚNG 1 LẦN DUY NHẤT
                                        val origIdx = reviewToOriginalMap[currentQuestion]
                                        if (origIdx != null) {
                                            reviewAttemptResults[origIdx] = isCurrentCorrect
                                        }
                                        // Tuyệt đối không thêm lại vào activeQuestions để tránh lặp vô tận
                                    }

                                    // Logic xử lý khi hết tim: Game Over hoặc nạp tim bằng Sao để học tiếp
                                    val currentHearts = viewModel.hearts.value ?: 5
                                    if (currentHearts <= 0) {
                                        showOutOfHeartsDialog = true
                                        return@Button
                                    }

                                    // Nếu vừa hết câu hỏi vòng đầu và có câu sai: Kích hoạt Intro sửa lỗi sai!
                                    if (currentQuestion == originalTotal - 1 && !isReviewPhase && missedQuestionsQueue.isNotEmpty()) {
                                        missedQuestionsQueue.forEach { (origIdx, qItem) ->
                                            val reviewIdx = activeQuestions.size
                                            reviewToOriginalMap[reviewIdx] = origIdx
                                            activeQuestions.add(qItem)
                                        }
                                        reviewTotalCount = missedQuestionsQueue.size
                                        isReviewPhase = true
                                        showReviewIntro = true
                                        isChecked = false
                                        return@Button
                                    }

                                    if (currentQuestion < activeQuestions.size - 1) {
                                        currentQuestion++
                                        selectedOption = -1
                                        selectedTokenIndices = emptyList()
                                        isMatchingCompleted = false
                                        matchingErrorCount = 0
                                        speakingResult = null
                                        isChecked = false
                                        isSavedToVault = false
                                        mascotSpeech = null
                                    } else {
                                        val finalResults = (0 until originalTotal).map { idx ->
                                            val item = activeQuestions[idx]
                                            val isItemCorrect = firstAttemptResults[idx] == true || reviewAttemptResults[idx] == true
                                            val copy = when (item.type) {
                                                QuestionType.SENTENCE_BUILDER, QuestionType.LISTENING_CHALLENGE -> {
                                                    val assembled = item.userSentenceTokens.joinToString(" ")
                                                    val c = QuestItem(
                                                        question = item.question,
                                                        options = if (isItemCorrect) listOf(item.correctSentence) else listOf(item.correctSentence, assembled.ifEmpty { "Incomplete" }),
                                                        correctIndex = 0,
                                                        type = item.type,
                                                        sentenceTokens = item.sentenceTokens,
                                                        correctSentence = item.correctSentence,
                                                        userSentenceTokens = item.userSentenceTokens
                                                    )
                                                    c.selectedIndex = if (isItemCorrect) 0 else 1
                                                    c
                                                }
                                                QuestionType.MATCHING_PAIRS -> {
                                                    val pairsSummary = item.matchingPairs.joinToString(", ") { "${it.english} = ${it.vietnamese}" }
                                                    val c = QuestItem(
                                                        question = item.question,
                                                        options = listOf(pairsSummary),
                                                        correctIndex = 0,
                                                        type = item.type,
                                                        matchingPairs = item.matchingPairs
                                                    )
                                                    c.selectedIndex = if (isItemCorrect) 0 else 1
                                                    c
                                                }
                                                QuestionType.SPEAKING_CHALLENGE -> {
                                                    val c = QuestItem(
                                                        question = item.question,
                                                        options = listOf(item.speakingSentence),
                                                        correctIndex = 0,
                                                        type = item.type,
                                                        speakingSentence = item.speakingSentence,
                                                        pronunciationScore = item.pronunciationScore
                                                    )
                                                    c.selectedIndex = if (isItemCorrect) 0 else 1
                                                    c
                                                }
                                                else -> {
                                                    val c = QuestItem(item.question, item.options, item.correctIndex)
                                                    c.selectedIndex = if (isItemCorrect) item.correctIndex else (firstAttemptUserAnswers[idx] ?: -1)
                                                    c
                                                }
                                            }
                                            val latency = questionLatencies[idx] ?: 2500L
                                            val usedHint = questionHintsUsed[idx] ?: false
                                            val isDistracted = latency > com.example.util.Sm2Algorithm.DISTRACTION_LATENCY_THRESHOLD_MS
                                            copy.responseLatencyMs = latency
                                            copy.usedHint = usedHint
                                            copy.isDistracted = isDistracted
                                            copy.objectiveQuality = com.example.util.Sm2Algorithm.calculateObjectiveQuality(
                                                isCorrect = isItemCorrect,
                                                responseLatencyMs = latency,
                                                usedHint = usedHint,
                                                isDistracted = isDistracted
                                            )
                                            copy.sloTopic = chapterTitle
                                            copy
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

        // Hộp thoại Game Over khi hết tim (yêu cầu đổi sao nạp tim hoặc dừng học)
        if (showOutOfHeartsDialog) {
            OutOfHeartsDialog(
                currentStars = stars,
                isEnglish = isEnglish,
                starCost = 20,
                onRefillWithStars = {
                    val success = viewModel.purchaseShopItem(
                        itemId = "refill_hearts_quest",
                        type = com.example.ShopItemType.HEART,
                        starCost = 20,
                        quantity = 5
                    )
                    if (success) {
                        showOutOfHeartsDialog = false
                        viewModel.soundEffectManager.playFanfare()
                        if (isChecked && currentQuestion < totalQuestions - 1) {
                            currentQuestion++
                            selectedOption = -1
                            selectedTokenIndices = emptyList()
                            isMatchingCompleted = false
                            matchingErrorCount = 0
                            speakingResult = null
                            isChecked = false
                            isSavedToVault = false
                            mascotSpeech = null
                        }
                    }
                },
                onQuitLesson = {
                    showOutOfHeartsDialog = false
                    onFinish()
                }
            )
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

        // --- MISTAKE REVIEW INTRO INTERSTITIAL (Đoạn Intro sửa lỗi sai tuyệt đẹp) ---
        if (showReviewIntro) {
            ReviewMistakesIntroOverlay(
                isEnglish = isEnglish,
                reviewCount = reviewTotalCount,
                onStartReview = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.soundEffectManager.playCorrect()
                    showReviewIntro = false
                    currentQuestion = originalTotal
                    selectedOption = -1
                    selectedTokenIndices = emptyList()
                    isMatchingCompleted = false
                    matchingErrorCount = 0
                    speakingResult = null
                    isChecked = false
                    isSavedToVault = false
                    mascotSpeech = null
                }
            )
        }
    }
}

private fun copyForReview(item: QuestItem): QuestItem {
    return QuestItem(
        question = item.question,
        options = item.options,
        correctIndex = item.correctIndex,
        type = item.type,
        sentenceTokens = item.sentenceTokens,
        correctSentence = item.correctSentence,
        userSentenceTokens = emptyList(),
        matchingPairs = item.matchingPairs,
        speakingSentence = item.speakingSentence,
        pronunciationScore = item.pronunciationScore
    )
}

@Composable
private fun ReviewMistakesIntroOverlay(
    isEnglish: Boolean,
    reviewCount: Int,
    onStartReview: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "review_intro_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseGlow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A0717))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft cosmic radial aura
        Box(
            modifier = Modifier
                .size(340.dp)
                .scale(pulseGlow)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0x3851FAC1), Color(0x1000F5D4), Color.Transparent)
                    ),
                    CircleShape
                )
                .blur(36.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFC1B1535), Color(0xFC100C22))
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF51FAC1), Color(0x40FFFFFF), Color(0xFF00F5D4))
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mascot spotlight
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0x3351FAC1), Color(0x1A141028))
                        )
                    )
                    .border(1.5.dp, Color(0x6651FAC1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                LivingMascotView(
                    emotion = MascotEmotion.CHEER,
                    size = 100.dp,
                    showBadge = false
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Capsule badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x2E51FAC1))
                    .border(1.dp, Color(0x6651FAC1), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isEnglish) "MISTAKE REVIEW ROUND" else "VÒNG ÔN TẬP & SỬA LỖI",
                    color = Color(0xFF51FAC1),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Title
            Text(
                text = if (isEnglish) "Let's Review Your\nMistakes Together!" else "Hãy Cùng Nhau\nSửa Lại Lỗi Sai Nhé!",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 31.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFF51FAC1).copy(alpha = 0.45f),
                        blurRadius = 24f,
                        offset = Offset(0f, 3f)
                    )
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Description
            Text(
                text = if (isEnglish) "Practicing the questions you missed helps you remember deeper and master the entire lesson."
                       else "Luyện tập lại các câu chưa đúng giúp bạn ghi nhớ sâu và hoàn thành bài học trọn vẹn.",
                color = Color(0xFFD6D0E4),
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Questions count pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x33141028))
                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = Color(0xFFFFD166),
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEnglish) "$reviewCount questions to conquer" else "$reviewCount câu hỏi cần bạn chinh phục lại",
                    color = Color(0xFFFFD166),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            Button(
                onClick = onStartReview,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF51FAC1), Color(0xFF00F5D4), Color(0xFF51FAC1))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isEnglish) "START REVIEW" else "BẮT ĐẦU SỬA SAI",
                            color = Color(0xFF0B1B18),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.5.sp,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF0B1B18),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
