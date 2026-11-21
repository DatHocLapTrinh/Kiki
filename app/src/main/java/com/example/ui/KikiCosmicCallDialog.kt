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

    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    val currentQuestion = remember(currentQuestionIndex, callQuestions) {
        if (callQuestions.isNotEmpty()) callQuestions[currentQuestionIndex % callQuestions.size] else null
    }

    var callState by remember { mutableStateOf(CosmicCallState.CONNECTING) }
    var callSeconds by remember { mutableIntStateOf(0) }
    var userSpokenText by remember { mutableStateOf("") }
    var evaluationScore by remember { mutableIntStateOf(0) }
    var evaluationFeedback by remember { mutableStateOf("") }
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
        speechManager.startListening(
            language = "en-US",
            onResult = { spoken ->
                if (spoken.isNotBlank()) {
                    userSpokenText = spoken
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
    fun askKikiToSpeak(speechText: String) {
        speechManager.stopListening()
        callState = CosmicCallState.KIKI_SPEAKING
        if (!isMuted) {
            ttsManager.speak(speechText, isSlow = false, onDone = {
                if (callState == CosmicCallState.KIKI_SPEAKING) {
                    if (hasAudioPermission) {
                        startListeningSafely()
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
        val finalSpoken = if (userSpokenText.isNotBlank()) userSpokenText else partialText
        userSpokenText = finalSpoken
        callState = CosmicCallState.ANALYZING

        coroutineScope.launch {
            // Conversational Filler - Phản hồi đệm tức thì (<200ms) triệt tiêu độ trễ
            val filler = listOf("Aha! Checking your response...", "I heard you! Let me analyze...", "Got it! Let's see...").random()
            if (!isMuted) {
                ttsManager.speak(filler)
            }

            val q = currentQuestion
            val (score, feedback) = if (viewModel != null && q != null) {
                viewModel.evaluateCosmicCallResponse(
                    context = context,
                    question = q.englishQuestion,
                    userAnswer = finalSpoken,
                    planetName = q.planetName
                )
            } else {
                localEvaluateAnswer(finalSpoken)
            }

            evaluationScore = score
            evaluationFeedback = feedback
            questionsAnsweredCount++
            val earnedXp = (score / 4).coerceAtLeast(15)
            totalEarnedXp += earnedXp
            onRewardEarned(earnedXp, 5)
            callState = CosmicCallState.EVALUATED

            if (!isMuted) {
                ttsManager.speak("Nice effort! $feedback")
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
                            if (callState == CosmicCallState.KIKI_SPEAKING || callState == CosmicCallState.LISTENING_USER) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val radius = (size.width / 2f) * wavePulse
                                    val waveColor = if (callState == CosmicCallState.KIKI_SPEAKING) {
                                        Color(0xFF8B5CF6)
                                    } else {
                                        Color(0xFF00F5D4)
                                    }
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

                                Spacer(modifier = Modifier.height(8.dp))

                                // Nút bật/tắt gợi ý (Hint toggle)
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
                                        text = if (showHint) (if (isEnglish) "Hide hint" else "Ẩn gợi ý") else (if (isEnglish) "Show hint" else "Xem gợi ý"),
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
                                        if (q.suggestedStarter.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "Starter: \"${q.suggestedStarter}\"",
                                                color = Color(0xFF51FAC1),
                                                fontSize = 11.5.sp,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        }
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

                            // Văn bản người dùng nói theo thời gian thực (Live Streaming Transcript)
                            val displayText = when {
                                userSpokenText.isNotBlank() -> userSpokenText
                                partialText.isNotBlank() -> partialText
                                callState == CosmicCallState.LISTENING_USER -> if (isEnglish) "(Kiki is listening... speak into the mic)" else "(Kiki đang lắng nghe, hãy nói tiếng Anh...)"
                                else -> if (isEnglish) "(No speech recorded yet)" else "(Chưa ghi nhận câu trả lời)"
                            }

                            Text(
                                text = displayText,
                                color = if (userSpokenText.isNotEmpty() || partialText.isNotEmpty()) Color.White else Color(0xFF64748B),
                                fontSize = 13.5.sp,
                                fontStyle = if (userSpokenText.isEmpty() && partialText.isEmpty()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                lineHeight = 19.sp
                            )

                            // Đánh giá từ AI
                            if (callState == CosmicCallState.EVALUATED) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0x2251FAC1), thickness = 0.8.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Kiki AI: $evaluationFeedback",
                                    color = Color(0xFF51FAC1),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 18.sp
                                )
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
                                    val nextIdx = currentQuestionIndex + 1
                                    currentQuestionIndex = nextIdx
                                    val nextQuestion = if (callQuestions.isNotEmpty()) callQuestions[nextIdx % callQuestions.size] else null
                                    userSpokenText = ""
                                    showHint = false
                                    nextQuestion?.let { q ->
                                        askKikiToSpeak(q.englishQuestion)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1)),
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "Next Cosmic Question ➡️" else "Câu Tiếp Theo Trong Vũ Trụ ➡️",
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
 * Hiệu ứng sóng âm thanh thời gian thực dựa trên rmsDb thật từ Micro
 */
@Composable
private fun RealtimeAudioWaveform(
    rmsDb: Float,
    isListening: Boolean
) {
    val barCount = 7
    val baseRms = if (isListening) rmsDb.coerceIn(0f, 10f) else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val factor = when (i) {
                0, 6 -> 0.35f
                1, 5 -> 0.65f
                2, 4 -> 0.85f
                else -> 1.0f
            }
            val targetHeight = (6.dp + (baseRms * factor * 2.2f).dp).coerceIn(6.dp, 26.dp)
            val animatedHeight by animateDpAsState(
                targetValue = targetHeight,
                animationSpec = tween(durationMillis = 100, easing = LinearEasing),
                label = "waveform_bar_$i"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(4.dp)
                    .height(animatedHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF00F5D4),
                                Color(0xFF8B5CF6)
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
                stageRangeText = if (isEnglish) "Section 1 (Planets 1 - 10)" else "Chặng 1 (Hành tinh 1 - 10)",
                planetName = if (isEnglish) "Solar System & Greetings" else "Hệ Mặt Trời & Chào Hỏi",
                englishQuestion = "Can you introduce yourself and tell me where you are living right now?",
                hintText = if (isEnglish) "Introduce your name and current city." else "Giới thiệu tên và thành phố bạn đang sống.",
                suggestedStarter = "My name is... and I live in..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 1 (Planets 1 - 10)" else "Chặng 1 (Hành tinh 1 - 10)",
                planetName = if (isEnglish) "Earth Planet" else "Hành Tinh Trái Đất",
                englishQuestion = "What is your favorite planet in the Solar System, and why do you like it?",
                hintText = if (isEnglish) "Name a planet (Earth, Mars, Jupiter...) and give a reason." else "Nêu tên hành tinh bạn thích và lý do.",
                suggestedStarter = "My favorite planet is..."
            )
        )
    }

    if (hasSection2) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 2 (Planets 11 - 20)" else "Chặng 2 (Hành tinh 11 - 20)",
                planetName = if (isEnglish) "Asteroid Belt" else "Vành Đai Tiểu Hành Tinh",
                englishQuestion = "What hobby helps you relax after a busy day of studying with Kiki?",
                hintText = if (isEnglish) "Talk about your relaxation activity: music, book, walk, gaming..." else "Chia sẻ sở thích thư giãn: nghe nhạc, đọc sách, đi dạo...",
                suggestedStarter = "I usually relax by..."
            )
        )
    }

    if (hasSection3) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 3 (Planets 21 - 30)" else "Chặng 3 (Hành tinh 21 - 30)",
                planetName = if (isEnglish) "Social Nebula" else "Tinh Vân Xã Hội & Công Việc",
                englishQuestion = "What is your dream job, and why is English important for your future career?",
                hintText = if (isEnglish) "Explain how English helps in your dream job." else "Giải thích tiếng Anh hỗ trợ công việc mơ ước ra sao.",
                suggestedStarter = "My dream job is... because English allows me to..."
            )
        )
    }

    if (hasSection4) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = if (isEnglish) "Section 4 (Planets 31 - 40)" else "Chặng 4 (Hành tinh 31 - 40)",
                planetName = if (isEnglish) "Deep Cosmos Gate" else "Siêu Thiên Hà Hố Đen & Chuyên Sâu",
                englishQuestion = "In your opinion, how will Artificial Intelligence help human beings learn languages in the future?",
                hintText = if (isEnglish) "Share your thoughts about AI in modern education." else "Nêu quan điểm về vai trò của AI trong học ngoại ngữ.",
                suggestedStarter = "In my view, AI empowers learners to..."
            )
        )
    }

    // Câu hỏi khởi động cho Tân binh (Cadet Orientation)
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
                englishQuestion = "How are you feeling today, and are you ready to conquer the cosmic universe?",
                hintText = if (isEnglish) "Express your feelings: happy, excited, ready..." else "Bày tỏ cảm xúc của bạn hôm nay.",
                suggestedStarter = "I feel wonderful and ready to conquer!"
            )
        )
    }

    return questions
}
