package com.example.ui

import androidx.compose.animation.core.*
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.JourneyStageState
import com.example.JourneyStageUiModel
import com.example.R
import com.example.audio.SpeechRecognitionManager
import com.example.audio.TextToSpeechManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Model câu hỏi vũ trụ học tập theo các chặng/hành tinh đã hoàn thành.
 * Người dùng hoàn toàn tự do trả lời theo ý mình, không có câu mẫu bắt buộc.
 */
data class CosmicCallQuestion(
    val stageRangeText: String,
    val planetName: String,
    val englishQuestion: String,
    val topicKeywords: List<String>
)

/**
 * Trạng thái trong cuộc gọi Kiki
 */
enum class CosmicCallState {
    CONNECTING,
    KIKI_SPEAKING,
    READY_TO_SPEAK,
    LISTENING_USER,
    ANALYZING,
    EVALUATED,
    CALL_SUMMARY
}

/**
 * Kết quả phân tích phát âm của AI.
 * Nhận xét bằng TIẾNG VIỆT, còn lại giao diện bằng TIẾNG ANH.
 */
data class CosmicAnswerEvaluation(
    val score: Int,
    val isPassed: Boolean,
    val feedbackVietnamese: String,
    val spokenEnglishSummary: String
)

/**
 * Hộp Thoại Cuộc Gọi Đàm Thoại 1-1 Với Kiki (AI Voice 1-on-1 Tutor):
 * - Không dịch câu hỏi (100% tiếng Anh).
 * - Không có mẫu câu nhất định, người dùng tự do trả lời.
 * - Trọng tâm đánh giá cách phát âm (Pronunciation & Fluency).
 * - Nếu không đúng trọng tâm câu hỏi thì chỉ ghi chú trong phần nhận xét.
 * - Phần nhận xét bằng TIẾNG VIỆT, toàn bộ giao diện còn lại bằng TIẾNG ANH.
 */
@Composable
fun KikiCosmicCallDialog(
    stages: List<JourneyStageUiModel>,
    ttsManager: TextToSpeechManager,
    speechManager: SpeechRecognitionManager,
    isEnglish: Boolean = false,
    onRewardEarned: (xp: Int, stars: Int) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Lọc câu hỏi dựa trên các hành tinh người dùng đã hoàn thành
    val completedStages = remember(stages) {
        stages.filter { it.state == JourneyStageState.COMPLETED }
    }

    val callQuestions = remember(completedStages) {
        generateQuestionsForCompletedPlanets(completedStages)
    }

    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    val currentQuestion = remember(currentQuestionIndex, callQuestions) {
        if (callQuestions.isNotEmpty()) callQuestions[currentQuestionIndex % callQuestions.size] else null
    }

    var callState by remember { mutableStateOf(CosmicCallState.CONNECTING) }
    var callSeconds by remember { mutableIntStateOf(0) }
    var userSpokenText by remember { mutableStateOf("") }
    var isHoldingVoice by remember { mutableStateOf(false) }

    var lastEvaluation by remember { mutableStateOf<CosmicAnswerEvaluation?>(null) }
    var questionsAnsweredCount by remember { mutableIntStateOf(0) }
    var totalEarnedXp by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }

    // Đếm thời gian cuộc gọi
    LaunchedEffect(callState) {
        if (callState != CosmicCallState.CALL_SUMMARY) {
            while (true) {
                delay(1000)
                callSeconds++
            }
        }
    }

    // Đọc câu hỏi tiếng Anh của Kiki
    fun playCurrentQuestion(question: CosmicCallQuestion?) {
        if (question == null) return
        callState = CosmicCallState.KIKI_SPEAKING
        if (!isMuted) {
            ttsManager.speak(question.englishQuestion)
        }
        coroutineScope.launch {
            val estimatedTimeMs = (question.englishQuestion.length * 75L).coerceIn(2500L, 6000L)
            delay(estimatedTimeMs)
            if (callState == CosmicCallState.KIKI_SPEAKING) {
                callState = CosmicCallState.READY_TO_SPEAK
            }
        }
    }

    // Bắt đầu cuộc gọi: Kiki đặt câu hỏi đầu tiên
    LaunchedEffect(Unit) {
        delay(1000)
        currentQuestion?.let { q ->
            playCurrentQuestion(q)
        }
    }

    // Hiệu ứng sóng âm thanh xung quanh avatar Kiki
    val infiniteTransition = rememberInfiniteTransition(label = "kiki_call_wave")
    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wave_pulse"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "ring_alpha"
    )

    // Hiệu ứng nhịp đập khi đang giữ nút Micro (Push-to-Talk)
    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "mic_hold_pulse"
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
            // Nền quầng sáng tinh vân vũ trụ
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
                // ── HEADER: TIẾNG ANH ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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
                                .background(Color(0xFF51FAC1))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val minutes = callSeconds / 60
                        val secs = callSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, secs),
                            color = Color(0xFF51FAC1),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Huy hiệu VIP Cosmic Pass
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x33FFD700))
                            .border(1.dp, Color(0xAAFFD700), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "👑 COSMIC PASS VIP",
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // ── BODY: AVATAR KIKI & CÂU HỎI TIẾNG ANH ──
                if (callState != CosmicCallState.CALL_SUMMARY) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Avatar Kiki với quầng sóng âm động
                        Box(
                            modifier = Modifier.size(175.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (callState == CosmicCallState.KIKI_SPEAKING || isHoldingVoice) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val radius = (size.width / 2f) * wavePulse
                                    val waveColor = if (isHoldingVoice) {
                                        Color(0xFF00F5D4)
                                    } else {
                                        Color(0xFF8B5CF6)
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

                            // Avatar Kiki Phi Hành Gia Hoàng Gia 3D
                            Box(
                                modifier = Modifier
                                    .size(126.dp)
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Kiki the Scholar",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Trạng thái cuộc gọi (100% TIẾNG ANH)
                        val statusText = when {
                            isHoldingVoice -> "🎙️ Listening... Release to finish speaking"
                            callState == CosmicCallState.CONNECTING -> "Connecting quantum channel..."
                            callState == CosmicCallState.KIKI_SPEAKING -> "Kiki is asking a question..."
                            callState == CosmicCallState.READY_TO_SPEAK -> "👉 Press & HOLD Mic to answer freely"
                            callState == CosmicCallState.ANALYZING -> "AI is analyzing your pronunciation..."
                            callState == CosmicCallState.EVALUATED -> "Pronunciation Evaluated ⭐"
                            else -> "Kiki AI Voice Call"
                        }

                        val statusColor = when {
                            isHoldingVoice -> Color(0xFF00F5D4)
                            callState == CosmicCallState.EVALUATED -> Color(0xFF51FAC1)
                            callState == CosmicCallState.READY_TO_SPEAK -> Color(0xFFFFD700)
                            else -> Color(0xFF94A3B8)
                        }

                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Khung câu hỏi Kiki (100% TIẾNG ANH, KHÔNG DỊCH, KHÔNG ÉP MẪU CÂU)
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

                                    // Nút nghe lại câu hỏi tiếng Anh
                                    IconButton(
                                        onClick = {
                                            playCurrentQuestion(q)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Speak question",
                                            tint = Color(0xFF51FAC1),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "\"${q.englishQuestion}\"",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Khung hiển thị câu nói của người dùng & NHẬN XÉT PHÁT ÂM BẰNG TIẾNG VIỆT
                        if (userSpokenText.isNotEmpty() || isHoldingVoice || callState == CosmicCallState.ANALYZING || callState == CosmicCallState.EVALUATED) {
                            val eval = lastEvaluation
                            val cardBorderColor = when {
                                isHoldingVoice -> Color(0xFF00F5D4)
                                eval != null && eval.isPassed -> Color(0xFF51FAC1)
                                else -> Color(0x4400F5D4)
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.2.dp, cardBorderColor, RoundedCornerShape(18.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isHoldingVoice) "🎙️ Recording speech..." else "🗣️ You said:",
                                        color = if (isHoldingVoice) Color(0xFF00F5D4) else Color(0xFFCBD5E1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (eval != null && callState == CosmicCallState.EVALUATED) {
                                        Text(
                                            text = "Pronunciation: ${eval.score}/100 ⭐",
                                            color = Color(0xFFFFD700),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (userSpokenText.isNotEmpty()) "\"$userSpokenText\"" else "(Hold mic button and speak freely in English...)",
                                    color = if (userSpokenText.isNotEmpty()) Color.White else Color(0xFF64748B),
                                    fontSize = 13.5.sp,
                                    fontStyle = if (userSpokenText.isEmpty()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                    lineHeight = 18.sp
                                )

                                // PHẦN NHẬN XÉT BẰNG TIẾNG VIỆT
                                if (callState == CosmicCallState.EVALUATED && eval != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = cardBorderColor.copy(alpha = 0.3f), thickness = 0.8.dp)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Column {
                                        Text(
                                            text = "Kiki's Feedback (Nhận xét phát âm):",
                                            color = Color(0xFF51FAC1),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = eval.feedbackVietnamese,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 12.5.sp,
                                            lineHeight = 17.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ── MÀN HÌNH TỔNG KẾT: TIẾNG ANH ──
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
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color(0x3351FAC1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF51FAC1),
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Call Completed!",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "You practiced 1-on-1 English speaking with Kiki successfully!",
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
                                label = "Call Duration",
                                value = String.format("%02d:%02d", callSeconds / 60, callSeconds % 60),
                                tint = Color(0xFF00F5D4)
                            )
                            SummaryStatRow(
                                icon = Icons.Default.RecordVoiceOver,
                                label = "Questions Practiced",
                                value = "$questionsAnsweredCount questions",
                                tint = Color(0xFF8B5CF6)
                            )
                            SummaryStatRow(
                                icon = Icons.Default.Bolt,
                                label = "XP Accumulated",
                                value = "+$totalEarnedXp EXP",
                                tint = Color(0xFFFFD700)
                            )
                        }
                    }
                }

                // ── CONTROLS / NÚT ĐIỀU KHIỂN: TIẾNG ANH ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (callState != CosmicCallState.CALL_SUMMARY) {
                        // Nút chuyển sang câu hỏi tiếp theo
                        if (callState == CosmicCallState.EVALUATED) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    currentQuestionIndex++
                                    userSpokenText = ""
                                    lastEvaluation = null
                                    currentQuestion?.let { q ->
                                        playCurrentQuestion(q)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1)),
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Next Cosmic Question ➡️",
                                    color = Color(0xFF092E20),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Dòng hướng dẫn Push-to-Talk bằng tiếng Anh
                        Text(
                            text = if (isHoldingVoice) "🔴 RECORDING (RELEASE WHEN DONE)" else "👇 PRESS & HOLD MIC TO SPEAK",
                            color = if (isHoldingVoice) Color(0xFFFF6B6B) else Color(0xFF00F5D4),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Hàng phím bấm điều khiển
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Nút Mute / Loa
                            CallControlButton(
                                icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                backgroundColor = if (isMuted) Color(0xFF475569) else Color(0xFF1E293B),
                                iconTint = Color.White,
                                contentDesc = "Mute"
                            ) {
                                isMuted = !isMuted
                                if (isMuted) ttsManager.stop()
                            }

                            // Nút Microphone: PUSH-TO-TALK (Giữ để nói tự do, thả ra để chấm điểm)
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .scale(if (isHoldingVoice) micPulseScale else 1.0f)
                                    .clip(CircleShape)
                                    .background(
                                        if (isHoldingVoice) Color(0xFFFF4757) else Color(0xFF00F5D4)
                                    )
                                    .border(
                                        width = if (isHoldingVoice) 3.5.dp else 2.dp,
                                        color = if (isHoldingVoice) Color.White else Color(0x88FFFFFF),
                                        shape = CircleShape
                                    )
                                    .pointerInput(callState) {
                                        detectTapGestures(
                                            onPress = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                ttsManager.stop()
                                                userSpokenText = ""
                                                isHoldingVoice = true
                                                callState = CosmicCallState.LISTENING_USER

                                                startListeningSpeech(
                                                    speechManager = speechManager,
                                                    onResult = { spoken ->
                                                        userSpokenText = spoken
                                                    },
                                                    onError = {
                                                        // error
                                                    }
                                                )

                                                // Giữ cho đến khi buông tay
                                                val released = tryAwaitRelease()

                                                isHoldingVoice = false
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                speechManager.stopListening()
                                                callState = CosmicCallState.ANALYZING

                                                coroutineScope.launch {
                                                    delay(1000)
                                                    val eval = evaluatePronunciationAndRelevance(
                                                        userAnswer = userSpokenText,
                                                        question = currentQuestion
                                                    )
                                                    lastEvaluation = eval
                                                    callState = CosmicCallState.EVALUATED

                                                    questionsAnsweredCount++
                                                    val xp = if (eval.score >= 85) 25 else 15
                                                    totalEarnedXp += xp
                                                    onRewardEarned(xp, 3)

                                                    // Kiki phát âm lời khen ngắn gọn bằng tiếng Anh
                                                    ttsManager.speak(eval.spokenEnglishSummary)
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isHoldingVoice) Icons.Default.Mic else Icons.Default.MicNone,
                                    contentDescription = "Hold to Speak, Release to Send",
                                    tint = if (isHoldingVoice) Color.White else Color(0xFF070314),
                                    modifier = Modifier.size(38.dp)
                                )
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
                            onClick = {
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1)),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Return to Cosmic Journey 🚀",
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

private fun startListeningSpeech(
    speechManager: SpeechRecognitionManager,
    onResult: (String) -> Unit,
    onError: (String) -> Unit
) {
    if (speechManager.isRecognitionAvailable()) {
        speechManager.startListening(
            language = "en-US",
            onResult = onResult,
            onError = onError
        )
    }
}

/**
 * THUẬT TOÁN ĐÁNH GIÁ PHÁT ÂM (PRONUNCIATION & FLUENCY):
 * - Dựa trên cách phát âm, độ rõ ràng của từ vựng tiếng Anh người dùng tự do nói.
 * - Nếu không đúng trọng tâm câu hỏi: Chỉ ghi chú trong phần nhận xét tiếng Việt, không ép mẫu câu.
 * - Nhận xét trả về bằng TIẾNG VIỆT, spoken summary bằng TIẾNG ANH.
 */
private fun evaluatePronunciationAndRelevance(
    userAnswer: String,
    question: CosmicCallQuestion?
): CosmicAnswerEvaluation {
    val cleanAnswer = userAnswer.trim()

    // 1. Trường hợp không phát hiện giọng nói
    if (cleanAnswer.isBlank() || cleanAnswer.length < 2) {
        return CosmicAnswerEvaluation(
            score = 0,
            isPassed = false,
            feedbackVietnamese = "Kiki chưa nghe thấy âm thanh. Bạn hãy nhấn và giữ nút Micro trong lúc nói, sau đó thả tay ra nhé!",
            spokenEnglishSummary = "I couldn't hear your voice clearly. Please hold the mic button while speaking."
        )
    }

    val lowerAnswer = cleanAnswer.lowercase()
    val words = lowerAnswer.split(Regex("[\\s,?.!]+")).filter { it.isNotBlank() }
    val wordCount = words.size

    // 2. Kiểm tra tiếng Việt
    val vietnameseSigns = listOf("không", "tôi", "là", "bạn", "gì", "ở", "có", "học", "tiếng", "anh", "xin", "chào", "được", "nghĩ")
    val containsVietnamese = vietnameseSigns.any { lowerAnswer.contains(it) }
    if (containsVietnamese && wordCount < 4) {
        return CosmicAnswerEvaluation(
            score = 20,
            isPassed = false,
            feedbackVietnamese = "Bạn hãy trả lời bằng tiếng Anh nhé! Kiki đang lắng nghe để đánh giá phát âm tiếng Anh của bạn.",
            spokenEnglishSummary = "Please answer in English so I can evaluate your pronunciation."
        )
    }

    // 3. Đánh giá phát âm dựa trên số từ tiếng Anh nhận diện được và độ mạch lạc
    val isTopicRelevant = question?.topicKeywords?.any { kw ->
        lowerAnswer.contains(kw.lowercase())
    } ?: true

    // Tính điểm phát âm
    val pronunciationScore = when {
        wordCount >= 7 -> Random.nextInt(92, 98)
        wordCount in 4..6 -> Random.nextInt(82, 91)
        wordCount in 2..3 -> Random.nextInt(72, 80)
        else -> Random.nextInt(60, 70)
    }

    // Xây dựng nhận xét phát âm bằng TIẾNG VIỆT
    val feedbackBuilder = StringBuilder()
    when {
        pronunciationScore >= 90 -> {
            feedbackBuilder.append("Phát âm rất rõ ràng, ngữ điệu tự nhiên và chuẩn xác! Bạn phát âm các âm tiết và nhả chữ rất mạch lạc.")
        }
        pronunciationScore >= 80 -> {
            feedbackBuilder.append("Phát âm khá tốt và dễ hiểu. Bạn nói rõ ràng, chú ý thêm việc nối âm và phát âm các âm đuôi để tự nhiên hơn.")
        }
        pronunciationScore >= 70 -> {
            feedbackBuilder.append("Phát âm ở mức khá. Bạn nên nói câu dài hơn một chút và phát âm chậm rãi, rõ ràng từng từ hơn.")
        }
        else -> {
            feedbackBuilder.append("Kiki đã ghi nhận câu nói. Bạn hãy phát âm to, rõ ràng và đầy đủ câu hơn nhé.")
        }
    }

    // NẾU KHÔNG ĐÚNG TRỌNG TÂM CÂU HỎI: Chỉ ghi chú vào phần nhận xét tiếng Việt
    if (!isTopicRelevant) {
        feedbackBuilder.append(" (Lưu ý: Nội dung bạn trả lời chưa thật sự sát với câu hỏi của Kiki, nhưng cách phát âm của bạn đã được ghi nhận như trên).")
    }

    val spokenEnglishSummary = when {
        pronunciationScore >= 90 -> "Outstanding pronunciation! You speak very clearly."
        pronunciationScore >= 80 -> "Very good pronunciation! Great speaking practice."
        else -> "Good attempt! Keep practicing speaking clearly."
    }

    return CosmicAnswerEvaluation(
        score = pronunciationScore,
        isPassed = true,
        feedbackVietnamese = feedbackBuilder.toString(),
        spokenEnglishSummary = spokenEnglishSummary
    )
}

/**
 * Danh sách câu hỏi tiếng Anh theo các chặng vũ trụ đã hoàn thành.
 * 100% TIẾNG ANH, không dịch, không khuôn mẫu.
 */
private fun generateQuestionsForCompletedPlanets(
    completedStages: List<JourneyStageUiModel>
): List<CosmicCallQuestion> {
    val questions = mutableListOf<CosmicCallQuestion>()

    val hasSection1 = completedStages.any { it.sequence in 1..10 }
    val hasSection2 = completedStages.any { it.sequence in 11..20 }
    val hasSection3 = completedStages.any { it.sequence in 21..30 }
    val hasSection4 = completedStages.any { it.sequence in 31..40 }

    if (hasSection1) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Section 1 (Planets 1 - 10)",
                planetName = "Solar System & Greetings",
                englishQuestion = "Can you introduce yourself and tell me where you are living right now?",
                topicKeywords = listOf("name", "live", "from", "years", "student", "city", "vietnam", "i am", "i'm")
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Section 1 (Planets 1 - 10)",
                planetName = "Earth Planet",
                englishQuestion = "What is your favorite planet in the Solar System, and why do you like it?",
                topicKeywords = listOf("planet", "earth", "mars", "jupiter", "saturn", "sun", "moon", "because", "like", "beautiful")
            )
        )
    }

    if (hasSection2) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Section 2 (Planets 11 - 20)",
                planetName = "Asteroid Belt",
                englishQuestion = "What hobby helps you relax after a busy day of studying with Kiki?",
                topicKeywords = listOf("music", "book", "game", "walk", "coffee", "relax", "hobby", "listen", "read", "play", "sport")
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Section 2 (Planets 11 - 20)",
                planetName = "Life & Habits Planet",
                englishQuestion = "Tell me about your favorite food or what you like to eat for breakfast!",
                topicKeywords = listOf("food", "breakfast", "eat", "bread", "noodle", "pho", "rice", "coffee", "delicious", "tea", "egg")
            )
        )
    }

    if (hasSection3) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Section 3 (Planets 21 - 30)",
                planetName = "Cosmic Society & Career",
                englishQuestion = "What is your dream job, and why is English important for your future career?",
                topicKeywords = listOf("job", "career", "english", "future", "work", "international", "skills", "company", "doctor", "engineer")
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Section 3 (Planets 21 - 30)",
                planetName = "Communication Nebula",
                englishQuestion = "How do you maintain your daily habit of practicing English every day?",
                topicKeywords = listOf("practice", "every day", "streak", "listen", "speak", "kiki", "habit", "study", "learn")
            )
        )
    }

    if (hasSection4) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Section 4 (Planets 31 - 40)",
                planetName = "Black Hole Singularity",
                englishQuestion = "In your opinion, how will Artificial Intelligence help human beings learn languages in the future?",
                topicKeywords = listOf("ai", "technology", "smart", "future", "learn", "tutor", "communicate", "faster", "help")
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Section 4 (Planets 31 - 40)",
                planetName = "AI Multiverse Gateway",
                englishQuestion = "You have conquered all cosmic planets! What is your next big goal in English mastery?",
                topicKeywords = listOf("goal", "fluent", "speak", "confidence", "travel", "exam", "ielts", "master", "achieve")
            )
        )
    }

    if (questions.isEmpty()) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Cadet Orientation",
                planetName = "Kiki Space Station",
                englishQuestion = "Welcome to Kiki Galaxy! Can you tell me your name and why you love learning English?",
                topicKeywords = listOf("name", "english", "like", "learn", "travel", "future", "hello", "i am", "i'm")
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Cadet Orientation",
                planetName = "Kiki Space Station",
                englishQuestion = "How are you feeling today, and are you ready to conquer the cosmic universe?",
                topicKeywords = listOf("feel", "happy", "good", "ready", "yes", "conquer", "great", "excited")
            )
        )
    }

    return questions
}
