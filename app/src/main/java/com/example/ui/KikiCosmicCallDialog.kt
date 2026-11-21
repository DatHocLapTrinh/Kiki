package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.JourneyStageState
import com.example.JourneyStageUiModel
import com.example.R
import com.example.audio.SpeechRecognitionManager
import com.example.audio.TextToSpeechManager
import com.example.viewmodel.StudyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

/**
 * Model câu hỏi vũ trụ học tập theo các chặng/hành tinh đã hoàn thành và từ vựng của người dùng
 */
data class CosmicCallQuestion(
    val stageRangeText: String,
    val planetName: String,
    val englishQuestion: String,
    val hintText: String,
    val suggestedStarter: String
)

/**
 * Trạng thái trong cuộc gọi Kiki
 */
enum class CosmicCallState {
    CONNECTING,       // Đang kết nối vũ trụ
    KIKI_SPEAKING,    // Kiki đang nói (TTS)
    LISTENING_USER,   // Kiki đang lắng nghe người dùng (Micro bật)
    ANALYZING,        // AI đang phân tích câu trả lời
    EVALUATED,        // Đã hoàn thành đánh giá
    CALL_SUMMARY      // Màn hình tổng kết cuộc gọi
}

/**
 * Hộp Thoại Cuộc Gọi Đàm Thoại Trực Tiếp Với Kiki (AI Voice 1-1 Tutor):
 * Kiến trúc Hybrid Tối Ưu (Zero-Lag, Real Waveform, Real LLM Feedback, Tap-to-Interrupt).
 */
@Composable
fun KikiCosmicCallDialog(
    stages: List<JourneyStageUiModel>,
    ttsManager: TextToSpeechManager,
    speechManager: SpeechRecognitionManager,
    viewModel: StudyViewModel? = null,
    isEnglish: Boolean = false,
    onRewardEarned: (xp: Int, stars: Int) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Quan sát trạng thái giọng nói và âm thanh thời gian thực từ Managers
    val rmsDb by speechManager.rmsDb
    val partialText by speechManager.partialText
    val isSpeechListening by speechManager.isListening
    val isTtsSpeaking by ttsManager.isSpeaking

    // 1. Lọc các câu hỏi dựa trên các hành tinh người dùng đã hoàn thành và từ vựng Sổ tay
    val completedStages = remember(stages) {
        stages.filter { it.state == JourneyStageState.COMPLETED }
    }
    val userSavedVocab = viewModel?.vocabularyList?.value ?: emptyList()

    val callQuestions = remember(completedStages, userSavedVocab) {
        generateSmartQuestionsForCall(completedStages, userSavedVocab, isEnglish)
    }

    val conversationHistory = remember { mutableStateListOf<com.example.model.CosmicCallTurnDto>() }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var activeDynamicQuestion by remember { mutableStateOf<CosmicCallQuestion?>(null) }

    val currentQuestion = activeDynamicQuestion ?: remember(currentQuestionIndex, callQuestions) {
        if (callQuestions.isNotEmpty()) callQuestions[currentQuestionIndex % callQuestions.size] else null
    }

    var callState by remember { mutableStateOf(CosmicCallState.CONNECTING) }
    var callSeconds by remember { mutableIntStateOf(0) }
    var userSpokenText by remember { mutableStateOf("") }
    var editableTranscriptText by remember { mutableStateOf("") }
    var isEditingTranscript by remember { mutableStateOf(false) }
    var evaluationScore by remember { mutableIntStateOf(0) }
    var evaluationFeedback by remember { mutableStateOf("") }
    var latestSpokenFeedbackEn by remember { mutableStateOf("") }
    var latestGrammarCorrection by remember { mutableStateOf("") }
    var pendingNextDynamicQuestion by remember { mutableStateOf<CosmicCallQuestion?>(null) }
    var questionsAnsweredCount by remember { mutableIntStateOf(0) }
    var totalEarnedXp by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Khởi động lắng nghe an toàn, dọn dẹp TTS trước khi mở Mic
    fun startListeningSafely() {
        if (!hasAudioPermission) return
        callState = CosmicCallState.LISTENING_USER
        userSpokenText = ""
        editableTranscriptText = ""
        isEditingTranscript = false
        speechManager.startListening(
            language = "en-US",
            continuous = true,
            onResult = { spoken ->
                if (spoken.isNotBlank()) {
                    userSpokenText = spoken
                    editableTranscriptText = spoken
                }
            },
            onPartialResult = { partial ->
                if (partial.isNotBlank()) {
                    userSpokenText = partial
                    editableTranscriptText = partial
                }
            },
            onError = { /* Cho phép người dùng chạm Mic để nói lại nếu timeout */ }
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            startListeningSafely()
        } else {
            Toast.makeText(
                context,
                if (isEnglish) "Microphone permission is needed to talk with Kiki" else "Cần cấp quyền Micro để trò chuyện cùng Kiki",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Hàm yêu cầu Kiki cất giọng, dứt câu thì tự động chuyển sang lắng nghe (Zero race-condition)
    fun askKikiToSpeak(speechText: String, targetLocale: Locale = Locale.US) {
        speechManager.stopListening()
        callState = CosmicCallState.KIKI_SPEAKING
        if (!isMuted) {
            ttsManager.speak(speechText, isSlow = false, targetLocale = targetLocale, onDone = {
                coroutineScope.launch {
                    // Trì hoãn nhẹ 350ms để giải phóng kênh AudioTrack trước khi bật Micro, tránh lỗi ERROR_AUDIO
                    delay(350)
                    if (callState == CosmicCallState.KIKI_SPEAKING) {
                        if (hasAudioPermission) {
                            startListeningSafely()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            })
        } else {
            coroutineScope.launch {
                delay(2200)
                if (callState == CosmicCallState.KIKI_SPEAKING) {
                    startListeningSafely()
                }
            }
        }
    }

    // Tap-to-Interrupt: Cho phép người dùng ngắt lời Kiki để trả lời ngay
    fun onInterruptKiki() {
        if (callState == CosmicCallState.KIKI_SPEAKING) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            ttsManager.stop()
            if (hasAudioPermission) {
                startListeningSafely()
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Gửi câu trả lời để AI phân tích
    fun submitAnswerForAnalysis() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        speechManager.stopListening()
        val finalSpoken = when {
            editableTranscriptText.isNotBlank() -> editableTranscriptText.trim()
            userSpokenText.isNotBlank() -> userSpokenText.trim()
            partialText.isNotBlank() -> partialText.trim()
            else -> ""
        }
        userSpokenText = finalSpoken
        editableTranscriptText = finalSpoken
        isEditingTranscript = false
        callState = CosmicCallState.ANALYZING

        coroutineScope.launch {
            // Conversational Filler - Phản hồi đệm tức thì (<200ms) triệt tiêu độ trễ
            val filler = listOf("Aha! Let me listen...", "Got it! Analyzing your response...", "I heard you! Let's see...").random()
            if (!isMuted) {
                ttsManager.speak(filler, targetLocale = Locale.US)
            }

            val q = currentQuestion
            val currentUserName = viewModel?.userName?.value ?: "Adventurer"
            val candidates = speechManager.candidateResults.value
            val response = if (viewModel != null && q != null) {
                viewModel.converseCosmicCall(
                    context = context,
                    question = q.englishQuestion,
                    userAnswer = finalSpoken,
                    planetName = q.planetName,
                    conversationHistory = conversationHistory.toList(),
                    userName = currentUserName,
                    candidateTranscripts = candidates
                )
            } else {
                viewModel?.fallbackConverseCosmicCall(finalSpoken, q?.englishQuestion ?: "", currentUserName)
                    ?: com.example.model.CosmicCallResponse(
                        score = 80,
                        spokenFeedbackEn = "Good effort! Keep speaking English with me.",
                        feedbackVi = "Khởi đầu rất tốt! Hãy tiếp tục tự tin nhé.",
                        grammarCorrection = finalSpoken,
                        nextQuestionEn = "What is your favorite hobby to relax?",
                        nextHintVi = "Nói về sở thích lúc rảnh rỗi."
                    )
            }

            // Cập nhật câu nói đã được khôi phục tên tiếng Việt chính xác (vd: "my name is Truong" thay vì "my name is john")
            if (!response.sanitizedUserAnswer.isNullOrBlank()) {
                userSpokenText = response.sanitizedUserAnswer
                editableTranscriptText = response.sanitizedUserAnswer
            }

            // Ghi nhận lịch sử hội thoại cho các lượt tiếp theo
            if (finalSpoken.isNotBlank()) {
                conversationHistory.add(com.example.model.CosmicCallTurnDto("user", finalSpoken))
            }
            if (response.spokenFeedbackEn.isNotBlank()) {
                conversationHistory.add(com.example.model.CosmicCallTurnDto("kiki", response.spokenFeedbackEn))
            }

            evaluationScore = response.score
            evaluationFeedback = response.feedbackVi
            latestSpokenFeedbackEn = response.spokenFeedbackEn
            latestGrammarCorrection = response.grammarCorrection ?: finalSpoken

            if (!response.nextQuestionEn.isNullOrBlank()) {
                val contextualStarter = if (!response.suggestedStarterEn.isNullOrBlank()) {
                    response.suggestedStarterEn
                } else {
                    deriveStarterForQuestion(response.nextQuestionEn)
                }
                pendingNextDynamicQuestion = CosmicCallQuestion(
                    stageRangeText = if (isEnglish) "Cosmic Dialogue" else "Đàm Thoại Trực Tiếp",
                    planetName = q?.planetName ?: "Cosmic Space",
                    englishQuestion = response.nextQuestionEn,
                    hintText = response.nextHintVi ?: (if (isEnglish) "Answer naturally." else "Trả lời tự nhiên theo ý bạn."),
                    suggestedStarter = contextualStarter
                )
            }

            questionsAnsweredCount++
            val earnedXp = (response.score / 4).coerceAtLeast(15)
            totalEarnedXp += earnedXp
            onRewardEarned(earnedXp, 5)
            callState = CosmicCallState.EVALUATED

            if (!isMuted && response.spokenFeedbackEn.isNotBlank()) {
                // Kiki speaks the encouraging English feedback with Locale.US
                ttsManager.speak(
                    text = response.spokenFeedbackEn,
                    targetLocale = Locale.US
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsManager.stop()
            speechManager.cancel()
        }
    }

    // Đếm thời gian cuộc gọi
    LaunchedEffect(callState) {
        if (callState != CosmicCallState.CALL_SUMMARY) {
            while (true) {
                delay(1000)
                callSeconds++
            }
        }
    }

    // Khởi động cuộc gọi mượt mà: Kiki chào và hỏi câu đầu tiên
    LaunchedEffect(Unit) {
        delay(800)
        currentQuestion?.let { q ->
            val greeting = if (isEnglish) {
                "Hello explorer! I am Kiki. Let's practice: ${q.englishQuestion}"
            } else {
                "Hello explorer! I am Kiki. ${q.englishQuestion}"
            }
            askKikiToSpeak(greeting)
        }
    }

    // Hiệu ứng sóng âm xung quanh Kiki Avatar
    val infiniteTransition = rememberInfiniteTransition(label = "kiki_call_wave")
    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wave_pulse"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "ring_alpha"
    )

    Dialog(
        onDismissRequest = {
            ttsManager.stop()
            speechManager.cancel()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0070314))
        ) {
            // Nền radial gradient vũ trụ mờ ảo
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height * 0.35f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x558B5CF6),
                            Color(0x3300F5D4),
                            Color(0x22EC4899),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.85f
                    ),
                    center = center,
                    radius = size.width * 0.85f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ── HEADER CUỘC GỌI ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Thời gian cuộc gọi
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x3351FAC1))
                            .border(1.dp, Color(0x6651FAC1), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (callState == CosmicCallState.LISTENING_USER) Color(0xFF00F5D4) else Color(0xFF51FAC1))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val minutes = callSeconds / 60
                        val secs = callSeconds % 60
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", minutes, secs),
                            color = Color(0xFF51FAC1),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Huy hiệu VIP
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x33FFD700))
                            .border(1.dp, Color(0xAAFFD700), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "👑 COSMIC CALL VIP" else "👑 GIA SƯ KIKI 1-1",
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // ── BODY CUỘC GỌI ──
                if (callState != CosmicCallState.CALL_SUMMARY) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp)
                    ) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Avatar Kiki với quầng sóng âm động & hỗ trợ Tap-to-Interrupt
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clickable { onInterruptKiki() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (callState == CosmicCallState.KIKI_SPEAKING) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val radius = (size.width / 2f) * wavePulse
                                    val waveColor = Color(0xFF8B5CF6)
                                    drawCircle(
                                        color = waveColor.copy(alpha = ringAlpha),
                                        radius = radius,
                                        center = center
                                    )
                                    drawCircle(
                                        color = waveColor.copy(alpha = ringAlpha * 0.5f),
                                        radius = radius * 1.15f,
                                        center = center
                                    )
                                }
                            } else if (callState == CosmicCallState.LISTENING_USER) {
                                // Diễn tả animation thu âm phản ứng theo giọng nói:
                                // Khi nói: sóng âm lan toả theo lực giọng nói thực tế
                                // Khi yên lặng / ngắt nghỉ: hoàn toàn tĩnh lặng, không có sóng
                                val rawUserPower = if (rmsDb > 1.8f) ((rmsDb - 1.8f) / 6.5f).coerceIn(0f, 1f) else 0f
                                val animatedUserPower by animateFloatAsState(
                                    targetValue = rawUserPower,
                                    animationSpec = tween(durationMillis = 80, easing = LinearEasing),
                                    label = "user_voice_power_halo"
                                )
                                if (animatedUserPower > 0.04f) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val radius = (size.width / 2f) * (1.0f + animatedUserPower * 0.35f)
                                        val waveColor = Color(0xFF00F5D4)
                                        drawCircle(
                                            color = waveColor.copy(alpha = animatedUserPower * 0.65f),
                                            radius = radius,
                                            center = center
                                        )
                                        drawCircle(
                                            color = waveColor.copy(alpha = animatedUserPower * 0.3f),
                                            radius = radius * 1.18f,
                                            center = center
                                        )
                                    }
                                }
                            }

                            // Avatar Kiki Phi Hành Gia 3D
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .shadow(20.dp, CircleShape, spotColor = Color(0xFF51FAC1))
                                    .clip(CircleShape)
                                    .border(
                                        3.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFFFFD700),
                                                Color(0xFF51FAC1),
                                                Color(0xFF8B5CF6)
                                            )
                                        ),
                                        CircleShape
                                    )
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.kiki_cosmic_pass_hero),
                                    contentDescription = "Kiki AI Mascot",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isEnglish) "Scholar Kiki Ant" else "Kiến Kiki Học Giả",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Trạng thái cuộc gọi
                        val statusText = when (callState) {
                            CosmicCallState.CONNECTING -> if (isEnglish) "Connecting cosmic channel..." else "Đang kết nối không gian..."
                            CosmicCallState.KIKI_SPEAKING -> if (isEnglish) "Kiki is speaking (Tap to speak)..." else "Kiki đang đặt câu hỏi (Chạm để trả lời)..."
                            CosmicCallState.LISTENING_USER -> if (isEnglish) "Kiki is listening to you..." else "Kiki đang lắng nghe bạn nói..."
                            CosmicCallState.ANALYZING -> if (isEnglish) "AI is analyzing your response..." else "AI đang phân tích phản xạ..."
                            CosmicCallState.EVALUATED -> if (isEnglish) "Answer evaluated!" else "Hoàn thành câu trả lời!"
                            CosmicCallState.CALL_SUMMARY -> if (isEnglish) "Call Summary" else "Tổng kết cuộc gọi"
                        }
                        Text(
                            text = statusText,
                            color = if (callState == CosmicCallState.LISTENING_USER) Color(0xFF00F5D4) else Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Khung hiển thị câu hỏi của Kiki
                        currentQuestion?.let { q ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF161129))
                                    .border(1.2.dp, Color(0x558B5CF6), RoundedCornerShape(20.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Public,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${q.stageRangeText} • ${q.planetName}",
                                            color = Color(0xFFFFD700),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Nút nghe lại câu hỏi
                                    IconButton(
                                        onClick = {
                                            askKikiToSpeak(q.englishQuestion)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Speak question",
                                            tint = Color(0xFF51FAC1),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "\"${q.englishQuestion}\"",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 21.sp
                                )

                                // Câu gợi ý mở đầu đúng ngữ cảnh (Contextual Starter) hiển thị ngay khi Kiki nói xong
                                if (q.suggestedStarter.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x3300F5D4))
                                            .border(1.dp, Color(0x6600F5D4), RoundedCornerShape(12.dp))
                                            .clickable {
                                                if (userSpokenText.isBlank()) {
                                                    userSpokenText = q.suggestedStarter
                                                    editableTranscriptText = q.suggestedStarter
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "💡",
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isEnglish) "Starter: \"${q.suggestedStarter}\"" else "Gợi ý mở đầu: \"${q.suggestedStarter}\"",
                                            color = Color(0xFF51FAC1),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Nút bật/tắt gợi ý tiếng Việt (Hint toggle)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x228B5CF6))
                                        .clickable { showHint = !showHint }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showHint) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color(0xFFCBD5E1),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (showHint) (if (isEnglish) "Hide hint" else "Ẩn gợi ý nghĩa") else (if (isEnglish) "Show hint" else "Xem gợi ý nghĩa"),
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                AnimatedVisibility(visible = showHint) {
                                    Column(modifier = Modifier.padding(top = 6.dp)) {
                                        Text(
                                            text = q.hintText,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Khung hiển thị câu trả lời & Live Real-time Waveform Audio Visualizer
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0F172A))
                                .border(
                                    1.dp,
                                    if (callState == CosmicCallState.EVALUATED) Color(0xFF51FAC1) else Color(0x4400F5D4),
                                    RoundedCornerShape(18.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (callState == CosmicCallState.LISTENING_USER) {
                                            if (isEnglish) "🎙️ Speaking now..." else "🎙️ Bạn đang nói..."
                                        } else {
                                            if (isEnglish) "🗣️ Your speech:" else "🗣️ Câu trả lời của bạn:"
                                        },
                                        color = Color(0xFF00F5D4),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // Nút sửa text ✏️ nếu nhận diện giọng nói bị sai tên riêng
                                    if (callState == CosmicCallState.LISTENING_USER || callState == CosmicCallState.EVALUATED) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                isEditingTranscript = !isEditingTranscript
                                                if (editableTranscriptText.isBlank()) {
                                                    editableTranscriptText = userSpokenText.ifBlank { partialText }
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit transcription",
                                                tint = Color(0xFFFFD700),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                if (callState == CosmicCallState.EVALUATED) {
                                    Text(
                                        text = "$evaluationScore/100 ⭐",
                                        color = Color(0xFFFFD700),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Hiển thị Real-time Audio Waveform khi người dùng đang nói
                            if (callState == CosmicCallState.LISTENING_USER) {
                                RealtimeAudioWaveform(
                                    rmsDb = rmsDb,
                                    isListening = isSpeechListening
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Chế độ chỉnh sửa văn bản hoặc hiển thị Live Transcript
                            if (isEditingTranscript) {
                                OutlinedTextField(
                                    value = editableTranscriptText,
                                    onValueChange = {
                                        editableTranscriptText = it
                                        userSpokenText = it
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color(0xFFE2E8F0),
                                        focusedBorderColor = Color(0xFF00F5D4),
                                        unfocusedBorderColor = Color(0x6600F5D4),
                                        cursorColor = Color(0xFF00F5D4)
                                    ),
                                    placeholder = {
                                        Text(
                                            text = if (isEnglish) "Edit speech or type your name..." else "Sửa câu nói hoặc gõ tên của bạn...",
                                            color = Color(0x77CBD5E1),
                                            fontSize = 12.5.sp
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isEditingTranscript = false }) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Done",
                                                tint = Color(0xFF51FAC1)
                                            )
                                        }
                                    }
                                )
                            } else {
                                val displayText = when {
                                    editableTranscriptText.isNotBlank() -> editableTranscriptText
                                    userSpokenText.isNotBlank() -> userSpokenText
                                    partialText.isNotBlank() -> partialText
                                    callState == CosmicCallState.LISTENING_USER -> if (isEnglish) "(Kiki is listening... speak into the mic)" else "(Kiki đang lắng nghe, hãy nói tiếng Anh...)"
                                    else -> if (isEnglish) "(No speech recorded yet)" else "(Chưa ghi nhận câu trả lời)"
                                }

                                Text(
                                    text = displayText,
                                    color = if (userSpokenText.isNotEmpty() || partialText.isNotEmpty() || editableTranscriptText.isNotEmpty()) Color.White else Color(0xFF64748B),
                                    fontSize = 13.5.sp,
                                    fontStyle = if (userSpokenText.isEmpty() && partialText.isEmpty() && editableTranscriptText.isEmpty()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                    lineHeight = 19.sp
                                )

                                // Gợi ý các phương án nhận diện âm thanh khác (Candidate Chips)
                                val candidates by speechManager.candidateResults
                                if (candidates.size > 1 && (callState == CosmicCallState.LISTENING_USER || callState == CosmicCallState.EVALUATED)) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        candidates.take(3).forEach { cand ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0x2200F5D4))
                                                    .border(0.8.dp, Color(0x5500F5D4), RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        userSpokenText = cand
                                                        editableTranscriptText = cand
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(cand, color = Color(0xFFE2E8F0), fontSize = 11.5.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // Đánh giá từ AI
                            if (callState == CosmicCallState.EVALUATED) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color(0x2251FAC1), thickness = 0.8.dp)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Bảng Điểm 100 Kèm Lời Động Viên Truyền Cảm Hứng
                                val (encouragementTitle, encouragementMsg, badgeColor) = when {
                                    evaluationScore >= 90 -> Triple(
                                        if (isEnglish) "🌟 Excellent Mastery!" else "🌟 Xuất sắc tuyệt vời!",
                                        if (isEnglish) "Outstanding pronunciation, fluent response and natural reflex!" else "Bạn phát âm rất chuẩn, nói tự nhiên và phản xạ cực kỳ xuất sắc!",
                                        Color(0xFFFFD700)
                                    )
                                    evaluationScore >= 80 -> Triple(
                                        if (isEnglish) "👏 Great & Fluent!" else "👏 Rất tốt & tự nhiên!",
                                        if (isEnglish) "Clear response, great vocabulary and very natural tone!" else "Câu trả lời rõ ràng, lưu loát và bắt nhịp hội thoại rất chuẩn!",
                                        Color(0xFF51FAC1)
                                    )
                                    evaluationScore >= 70 -> Triple(
                                        if (isEnglish) "💪 Confident Effort!" else "💪 Khởi đầu rất tự tin!",
                                        if (isEnglish) "Good bravery and clear message, keep pushing forward!" else "Bạn rất dũng cảm và phát âm rõ ý, tiếp tục phát huy năng lượng này nhé!",
                                        Color(0xFF00F5D4)
                                    )
                                    else -> Triple(
                                        if (isEnglish) "✨ Keep Practicing!" else "✨ Cố gắng lên, bạn làm được!",
                                        if (isEnglish) "Every practice session brings you closer to fluency!" else "Mỗi lần luyện nói là một bước tiến bộ, Kiki luôn đồng hành cùng bạn!",
                                        Color(0xFFFFB703)
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(badgeColor.copy(alpha = 0.12f))
                                        .border(1.2.dp, badgeColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = encouragementTitle,
                                            color = badgeColor,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "$evaluationScore / 100 Điểm",
                                            color = badgeColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = encouragementMsg,
                                        color = Color(0xFFF1F5F9),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 1. Lời phản hồi trực tiếp bằng tiếng Anh của Kiki
                                if (latestSpokenFeedbackEn.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubble,
                                            contentDescription = null,
                                            tint = Color(0xFF00F5D4),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Kiki: $latestSpokenFeedbackEn",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            lineHeight = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                // 2. Gợi ý cấu trúc chuẩn tự nhiên
                                if (latestGrammarCorrection.isNotBlank() && !latestGrammarCorrection.equals(userSpokenText, ignoreCase = true)) {
                                    Text(
                                        text = "✨ Gợi ý chuẩn: \"$latestGrammarCorrection\"",
                                        color = Color(0xFFFFD166),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                // 3. Nhận xét sư phạm bằng tiếng Việt kèm loa đọc tiếng Việt
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "💡 Nhận xét: $evaluationFeedback",
                                        color = Color(0xFF51FAC1),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            ttsManager.speak(
                                                text = evaluationFeedback,
                                                targetLocale = Locale("vi", "VN")
                                            )
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Listen Vietnamese feedback",
                                            tint = Color(0xFF51FAC1),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ── MÀN HÌNH TỔNG KẾT CUỘC GỌI ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Color(0x3351FAC1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF51FAC1),
                                modifier = Modifier.size(52.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isEnglish) "Cosmic Call Completed!" else "Cuộc Gọi Thành Công!",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isEnglish) "You've successfully completed the 1-1 AI practice session with Kiki!" else "Bạn đã hoàn thành buổi đàm thoại 1-1 với Kiki xuất sắc!",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF140E29))
                                .border(1.2.dp, Color(0x5551FAC1), RoundedCornerShape(20.dp))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryStatRow(
                                icon = Icons.Default.Timer,
                                label = if (isEnglish) "Call Duration" else "Thời lượng cuộc gọi",
                                value = String.format(Locale.US, "%02d:%02d", callSeconds / 60, callSeconds % 60),
                                tint = Color(0xFF00F5D4)
                            )
                            SummaryStatRow(
                                icon = Icons.Default.RecordVoiceOver,
                                label = if (isEnglish) "Questions Completed" else "Số câu đã đàm thoại",
                                value = "$questionsAnsweredCount",
                                tint = Color(0xFF8B5CF6)
                            )
                            SummaryStatRow(
                                icon = Icons.Default.Bolt,
                                label = if (isEnglish) "Experience Earned" else "Kinh nghiệm nhận được",
                                value = "+$totalEarnedXp EXP",
                                tint = Color(0xFFFFD700)
                            )
                        }
                    }
                }

                // ── CONTROLS / HÀNG PHÍM ĐIỀU KHIỂN DƯỚI ĐÁY ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (callState != CosmicCallState.CALL_SUMMARY) {
                        // Nút chuyển câu tiếp theo nếu đã đánh giá xong
                        if (callState == CosmicCallState.EVALUATED) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val dynamicQ = pendingNextDynamicQuestion
                                    userSpokenText = ""
                                    editableTranscriptText = ""
                                    isEditingTranscript = false
                                    showHint = false
                                    latestSpokenFeedbackEn = ""
                                    latestGrammarCorrection = ""
                                    pendingNextDynamicQuestion = null

                                    if (dynamicQ != null) {
                                        activeDynamicQuestion = dynamicQ
                                        askKikiToSpeak(dynamicQ.englishQuestion)
                                    } else {
                                        val nextIdx = currentQuestionIndex + 1
                                        currentQuestionIndex = nextIdx
                                        activeDynamicQuestion = null
                                        val nextQuestion = if (callQuestions.isNotEmpty()) callQuestions[nextIdx % callQuestions.size] else null
                                        nextQuestion?.let { q ->
                                            askKikiToSpeak(q.englishQuestion)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1)),
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "Tiếp tục luyện nói ! 🎙️" else "Tiếp tục luyện nói ! 🎙️",
                                    color = Color(0xFF092E20),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Hàng phím bấm điều khiển cuộc gọi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Nút Mute / Loa ngoài
                            CallControlButton(
                                icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                backgroundColor = if (isMuted) Color(0xFF475569) else Color(0xFF1E293B),
                                iconTint = Color.White,
                                contentDesc = "Mute"
                            ) {
                                isMuted = !isMuted
                                if (isMuted) ttsManager.stop()
                            }

                            // Nút Microphone trung tâm
                            when (callState) {
                                CosmicCallState.LISTENING_USER -> {
                                    // Đang lắng nghe: Chạm để gửi phân tích
                                    CallControlButton(
                                        icon = Icons.Default.Mic,
                                        backgroundColor = Color(0xFF00F5D4),
                                        iconTint = Color(0xFF070314),
                                        size = 68.dp,
                                        contentDesc = "Submit and analyze"
                                    ) {
                                        submitAnswerForAnalysis()
                                    }
                                }
                                CosmicCallState.KIKI_SPEAKING -> {
                                    // Kiki đang nói: Chạm để ngắt lời và nói ngay
                                    CallControlButton(
                                        icon = Icons.Default.Hearing,
                                        backgroundColor = Color(0xFF8B5CF6),
                                        iconTint = Color.White,
                                        size = 68.dp,
                                        contentDesc = "Tap to interrupt Kiki"
                                    ) {
                                        onInterruptKiki()
                                    }
                                }
                                CosmicCallState.ANALYZING -> {
                                    // Đang phân tích
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF334155)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = Color(0xFF00F5D4),
                                            strokeWidth = 3.dp
                                        )
                                    }
                                }
                                else -> {
                                    // Nút Mic sẵn sàng bấm nói
                                    CallControlButton(
                                        icon = Icons.Default.MicNone,
                                        backgroundColor = Color(0xFF1E293B),
                                        iconTint = Color(0xFF94A3B8),
                                        size = 68.dp,
                                        contentDesc = "Start speaking"
                                    ) {
                                        if (hasAudioPermission) {
                                            startListeningSafely()
                                        } else {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                            }

                            // Nút Kết thúc cuộc gọi
                            CallControlButton(
                                icon = Icons.Default.CallEnd,
                                backgroundColor = Color(0xFFEF4444),
                                iconTint = Color.White,
                                contentDesc = "End Call"
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                ttsManager.stop()
                                speechManager.cancel()
                                callState = CosmicCallState.CALL_SUMMARY
                            }
                        }
                    } else {
                        // Nút thoát khi ở màn hình tổng kết
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1)),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Return to Cosmic Journey 🚀" else "Quay Lại Vũ Trụ Học Tập 🚀",
                                color = Color(0xFF092E20),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Hiệu ứng sóng âm thanh thời gian thực dựa trên rmsDb thật từ Micro:
 * - Khi người dùng nói (rmsDb > 1.8f): các cột âm thanh nhảy múa theo lực giọng nói
 * - Khi im lặng hoặc ngắt nghỉ (rmsDb <= 1.8f): sóng âm hạ phẳng (2.dp) hoàn toàn tĩnh lặng
 */
@Composable
private fun RealtimeAudioWaveform(
    rmsDb: Float,
    isListening: Boolean
) {
    val barCount = 9
    val isVoiceActive = isListening && rmsDb > 1.8f
    val normalizedVoice = if (isVoiceActive) ((rmsDb - 1.8f) / 6.5f).coerceIn(0f, 1f) else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val factor = when (i) {
                0, 8 -> 0.3f
                1, 7 -> 0.55f
                2, 6 -> 0.8f
                3, 5 -> 0.95f
                else -> 1.0f
            }
            val targetHeight = if (isVoiceActive) {
                (4.dp + (normalizedVoice * factor * 24f).dp).coerceIn(4.dp, 28.dp)
            } else {
                2.dp
            }
            val animatedHeight by animateDpAsState(
                targetValue = targetHeight,
                animationSpec = tween(durationMillis = 75, easing = LinearEasing),
                label = "waveform_bar_$i"
            )
            val barAlpha = if (isVoiceActive) 1.0f else 0.22f

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.5.dp)
                    .width(3.5.dp)
                    .height(animatedHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF00F5D4).copy(alpha = barAlpha),
                                Color(0xFF8B5CF6).copy(alpha = barAlpha)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    iconTint: Color,
    size: androidx.compose.ui.unit.Dp = 54.dp,
    contentDesc: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = iconTint,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

@Composable
private fun SummaryStatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = label, color = Color(0xFFCBD5E1), fontSize = 13.5.sp)
        }
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Đánh giá phản xạ thông minh ngoại tuyến nếu không có kết nối API
 */
private fun localEvaluateAnswer(userAnswer: String): Pair<Int, String> {
    val trimmed = userAnswer.trim()
    val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
    val count = words.size
    return when {
        count >= 6 -> Pair(92, "Phản xạ xuất sắc! Câu trả lời dài, rõ ý và phát âm rất tự tin.")
        count >= 3 -> Pair(84, "Rất tốt! Bạn phát âm trôi chảy, hãy thử mở rộng thêm một ý nữa nhé.")
        count >= 1 -> Pair(75, "Tốt lắm! Bạn đã phản xạ kịp thời, hãy ghép thành câu hoàn chỉnh nhé.")
        else -> Pair(55, "Chưa nghe rõ câu trả lời. Hãy bấm Micro và nói to hơn nhé!")
    }
}

/**
 * Sinh danh sách câu hỏi thông minh kết hợp giữa Chặng đã hoàn thành VÀ Từ vựng Sổ tay của người dùng
 */
private fun generateSmartQuestionsForCall(
    completedStages: List<JourneyStageUiModel>,
    userSavedVocab: List<com.example.sqlite.room.VocabularyEntity>,
    isEnglish: Boolean
): List<CosmicCallQuestion> {
    val questions = mutableListOf<CosmicCallQuestion>()

    // 1. Tích hợp từ vựng Sổ tay cá nhân hóa (Pedagogical Focus)
    if (userSavedVocab.isNotEmpty()) {
        val sampleWord = userSavedVocab.shuffled().firstOrNull()
        if (sampleWord != null) {
            questions.add(
                CosmicCallQuestion(
                    stageRangeText = if (isEnglish) "Vocabulary Vault" else "Sổ Tay Từ Vựng Của Bạn",
                    planetName = sampleWord.word,
                    englishQuestion = "I noticed you saved the word '${sampleWord.word}'. Can you make a complete sentence using '${sampleWord.word}'?",
                    hintText = if (isEnglish) "Meaning: ${sampleWord.meaning}" else "Nghĩa: ${sampleWord.meaning}",
                    suggestedStarter = "I use '${sampleWord.word}' when I..."
                )
            )
        }
    }

    // 2. Tích hợp theo các chặng đã hoàn thành
    val hasSection1 = completedStages.any { it.sequence in 1..10 }
    val hasSection2 = completedStages.any { it.sequence in 11..20 }
    val hasSection3 = completedStages.any { it.sequence in 21..30 }
    val hasSection4 = completedStages.any { it.sequence in 31..40 }

    if (hasSection1) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 1: Solar System" else "Chặng 1: Hệ Mặt Trời",
                planetName = if (isEnglish) "Mercury: Greetings" else "Sao Thủy: Chào Hỏi",
                englishQuestion = "Can you introduce yourself and tell me where you are living right now?",
                hintText = if (isEnglish) "Introduce your name and current city." else "Giới thiệu tên và thành phố bạn đang sống.",
                suggestedStarter = "My name is... and I live in..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 1: Solar System" else "Chặng 1: Hệ Mặt Trời",
                planetName = if (isEnglish) "Venus: Identity & Daily Life" else "Sao Kim: Bản Thân & Đời Sống",
                englishQuestion = "What time do you usually wake up, and what is your morning routine?",
                hintText = if (isEnglish) "Describe your waking time and morning habits." else "Kể giờ thức dậy và thói quen buổi sáng.",
                suggestedStarter = "I usually wake up at... and then I..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 1: Solar System" else "Chặng 1: Hệ Mặt Trời",
                planetName = if (isEnglish) "Earth: Food & Drinks" else "Trái Đất: Ẩm Thực & Đồ Uống",
                englishQuestion = "What is your favorite dish or drink, and how often do you have it?",
                hintText = if (isEnglish) "Talk about your favorite food or coffee/tea." else "Chia sẻ món ăn hoặc đồ uống yêu thích.",
                suggestedStarter = "My favorite food is... because..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 1: Solar System" else "Chặng 1: Hệ Mặt Trời",
                planetName = if (isEnglish) "Mars: Weather & Seasons" else "Sao Hỏa: Thời Tiết & Mùa",
                englishQuestion = "How is the weather in your city today, and which season do you like best?",
                hintText = if (isEnglish) "Describe today's weather (sunny, rainy, cool)." else "Miêu tả thời tiết hôm nay và mùa bạn thích.",
                suggestedStarter = "The weather today is... and I love..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 1: Solar System" else "Chặng 1: Hệ Mặt Trời",
                planetName = if (isEnglish) "Jupiter: City & Transport" else "Sao Mộc: Đô Thị & Di Chuyển",
                englishQuestion = "How do you usually commute to work or school every day?",
                hintText = if (isEnglish) "Mention your vehicle (motorbike, bus, car, bicycle)." else "Nêu phương tiện bạn đi lại hàng ngày.",
                suggestedStarter = "I usually commute by..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 1: Solar System" else "Chặng 1: Hệ Mặt Trời",
                planetName = if (isEnglish) "Solar Apex: Cosmic Wonders" else "Đỉnh Mặt Trời: Kỳ Quan Vũ Trụ",
                englishQuestion = "What is your favorite planet in the Solar System, and why do you like it?",
                hintText = if (isEnglish) "Name a planet (Earth, Mars, Jupiter...) and give a reason." else "Nêu tên hành tinh bạn thích và lý do.",
                suggestedStarter = "My favorite planet is..."
            )
        )
    }

    if (hasSection2) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 2: Andromeda" else "Chặng 2: Thiên Hà Tiên Nữ",
                planetName = if (isEnglish) "ISS Outpost: Travel & Airports" else "Trạm ISS: Du Lịch & Sân Bay",
                englishQuestion = "Have you ever traveled to another city or country? How was your trip?",
                hintText = if (isEnglish) "Share an experience about a memorable trip." else "Kể về một chuyến đi đáng nhớ của bạn.",
                suggestedStarter = "I traveled to... and it was..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 2: Andromeda" else "Chặng 2: Thiên Hà Tiên Nữ",
                planetName = if (isEnglish) "Kepler Outpost: Hotels" else "Trạm Kepler: Khách Sạn & Nghỉ Dưỡng",
                englishQuestion = "When staying at a hotel, what amenities are most important to you?",
                hintText = if (isEnglish) "Think about Wi-Fi, swimming pool, breakfast, comfy bed." else "Nêu tiện nghi quan trọng: bữa sáng, giường êm, Wi-Fi...",
                suggestedStarter = "The most important things for me are..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 2: Andromeda" else "Chặng 2: Thiên Hà Tiên Nữ",
                planetName = if (isEnglish) "Eagle Nebula: Dining" else "Tinh Vân Đại Bàng: Nhà Hàng & Gọi Món",
                englishQuestion = "What do you usually say when ordering coffee or food at a restaurant?",
                hintText = if (isEnglish) "Use polite phrases: 'Can I have...', 'I would like...'" else "Sử dụng mẫu câu: 'Can I have...', 'I would like...'",
                suggestedStarter = "I usually say: 'Excuse me, could I have...'"
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 2: Andromeda" else "Chặng 2: Thiên Hà Tiên Nữ",
                planetName = if (isEnglish) "Asteroid Belt: Hobbies" else "Vành Đai Tiểu Hành Tinh: Giải Trí",
                englishQuestion = "What hobby helps you relax after a busy day of studying or working?",
                hintText = if (isEnglish) "Talk about relaxation: music, reading, walking, gaming..." else "Chia sẻ sở thích thư giãn: nghe nhạc, đọc sách, đi dạo...",
                suggestedStarter = "I usually relax by..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 2: Andromeda" else "Chặng 2: Thiên Hà Tiên Nữ",
                planetName = if (isEnglish) "Orion Market: Shopping" else "Chợ Orion: Mua Sắm & Mặc Cả",
                englishQuestion = "Do you prefer shopping online or going to traditional stores, and why?",
                hintText = if (isEnglish) "Compare convenience, prices, and seeing products in person." else "So sánh tính tiện lợi, giá cả và trải nghiệm mua sắm.",
                suggestedStarter = "I prefer shopping... because..."
            )
        )
    }

    if (hasSection3) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 3: Infinite Nebula" else "Chặng 3: Tinh Vân Vô Cực",
                planetName = if (isEnglish) "Social Nebula: Career" else "Tinh Vân Xã Hội: Sự Nghiệp",
                englishQuestion = "What is your dream job, and why is English important for your future career?",
                hintText = if (isEnglish) "Explain how English helps in your dream job." else "Giải thích tiếng Anh hỗ trợ công việc mơ ước ra sao.",
                suggestedStarter = "My dream job is... because English allows me to..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 3: Infinite Nebula" else "Chặng 3: Tinh Vân Vô Cực",
                planetName = if (isEnglish) "Pulsar Core: Teamwork" else "Lõi Sao Xung: Làm Việc Nhóm",
                englishQuestion = "What qualities make a great teammate when working on a challenging project?",
                hintText = if (isEnglish) "Mention communication, honesty, responsibility, positivity." else "Nêu phẩm chất: giao tiếp tốt, trách nhiệm, chủ động...",
                suggestedStarter = "In my opinion, a great teammate should be..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 3: Infinite Nebula" else "Chặng 3: Tinh Vân Vô Cực",
                planetName = if (isEnglish) "Mystic Cluster: Time Management" else "Cụm Sao Thần Bí: Quản Lý Thời Gian",
                englishQuestion = "How do you stay focused and handle tight deadlines when you have many tasks?",
                hintText = if (isEnglish) "Share your methods: to-do list, Pomodoro, deep work." else "Chia sẻ cách bạn xử lý áp lực và hạn chót.",
                suggestedStarter = "To manage tight deadlines, I always..."
            )
        )
    }

    if (hasSection4) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 4: Dark Horse Galaxy" else "Chặng 4: Quần Thể Sao Hắc Mã",
                planetName = if (isEnglish) "Deep Cosmos Gate: AI & Future" else "Cổng Vũ Trụ: AI & Tương Lai",
                englishQuestion = "In your opinion, how will Artificial Intelligence help human beings learn languages in the future?",
                hintText = if (isEnglish) "Share your thoughts about AI in modern education." else "Nêu quan điểm về vai trò của AI trong học ngoại ngữ.",
                suggestedStarter = "In my view, AI empowers learners to..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 4: Dark Horse Galaxy" else "Chặng 4: Quần Thể Sao Hắc Mã",
                planetName = if (isEnglish) "Singularity Hub: Life Philosophy" else "Vùng Điểm Dị Thường: Triết Lý Sống",
                englishQuestion = "What is the most valuable lesson you have learned from overcoming a difficult challenge?",
                hintText = if (isEnglish) "Reflect on resilience, patience, or personal growth." else "Chiêm nghiệm về sự kiên trì và trưởng thành bản thân.",
                suggestedStarter = "The greatest lesson I learned was that..."
            )
        )
    }

    // Câu hỏi khởi động phong phú cho Tân binh (Cadet Orientation)
    if (questions.isEmpty()) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Cadet Orientation" else "Khởi Động Tân Binh",
                planetName = if (isEnglish) "Kiki Space Station" else "Trạm Không Gian Kiki",
                englishQuestion = "Welcome to Kiki Galaxy! Can you tell me your name and why you love learning English?",
                hintText = if (isEnglish) "Say hello, your name, and your goal." else "Chào Kiki, nói tên và mục tiêu học tiếng Anh của bạn.",
                suggestedStarter = "Hello Kiki, my name is..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Cadet Orientation" else "Khởi Động Tân Binh",
                planetName = if (isEnglish) "Kiki Space Station" else "Trạm Không Gian Kiki",
                englishQuestion = "Where do you live, and what is the best thing about your hometown?",
                hintText = if (isEnglish) "Name your city and mention food, people, or scenery." else "Nói về quê hương và điểm đẹp nhất ở đó.",
                suggestedStarter = "I live in... and the best thing is..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Cadet Orientation" else "Khởi Động Tân Binh",
                planetName = if (isEnglish) "Kiki Space Station" else "Trạm Không Gian Kiki",
                englishQuestion = "What do you like to do on weekends when you have free time?",
                hintText = if (isEnglish) "Talk about relaxing, friends, family, or hobbies." else "Chia sẻ hoạt động cuối tuần bạn thích làm.",
                suggestedStarter = "On weekends, I usually enjoy..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Cadet Orientation" else "Khởi Động Tân Binh",
                planetName = if (isEnglish) "Kiki Space Station" else "Trạm Không Gian Kiki",
                englishQuestion = "What is your favorite food or drink, and why do you like it?",
                hintText = if (isEnglish) "Name a favorite food and explain why." else "Kể món ăn yêu thích và lý do bạn thích.",
                suggestedStarter = "My favorite food is... because it is delicious."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Cadet Orientation" else "Khởi Động Tân Binh",
                planetName = if (isEnglish) "Kiki Space Station" else "Trạm Không Gian Kiki",
                englishQuestion = "How are you feeling today, and are you ready to conquer the cosmic universe?",
                hintText = if (isEnglish) "Express your feelings: happy, excited, ready..." else "Bày tỏ cảm xúc của bạn hôm nay.",
                suggestedStarter = "I feel wonderful and ready to conquer!"
            )
        )
    }

    return questions
}

/**
 * Tạo câu gợi ý mở đầu (Contextual Starter) tự nhiên tương ứng với ngữ cảnh câu hỏi,
 * tránh việc luôn hiển thị câu mặc định rập khuôn "I think that...".
 */
private fun deriveStarterForQuestion(question: String): String {
    val q = question.lowercase(Locale.US).trim()
    return when {
        q.contains("your name") || q.contains("who are you") || q.contains("introduce yourself") -> "My name is..."
        q.contains("where do you live") || q.contains("where are you from") || q.contains("hometown") -> "I live in..."
        q.contains("favorite food") || q.contains("favorite drink") || q.contains("like to eat") -> "My favorite food is..."
        q.contains("hobby") || q.contains("free time") || q.contains("on weekends") -> "In my free time, I love..."
        q.contains("why do you") || q.contains("why should") || q.contains("why is") -> "I think the reason is that..."
        q.contains("how do you") || q.contains("how can") -> "I usually..."
        q.contains("what is your dream") || q.contains("future career") -> "My dream is to..."
        q.contains("what do you think") || q.contains("in your opinion") || q.contains("do you prefer") -> "In my opinion,..."
        q.contains("how are you") || q.contains("how do you feel") -> "I feel..."
        else -> "Well, to answer your question,..."
    }
}

