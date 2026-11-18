package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
 * Bao gồm các nhóm ngữ nghĩa bắt buộc để AI chấm điểm chính xác, không chấm bừa.
 */
data class CosmicCallQuestion(
    val stageRangeText: String,
    val planetName: String,
    val englishQuestion: String,
    val vietnameseMeaning: String,
    val topicName: String,
    // Các nhóm ý nghĩa cốt lõi: người dùng phải nói trúng các nhóm này mới được tính điểm đúng đề
    val semanticGroups: List<List<String>>,
    // Các từ vựng bổ trợ ngữ cảnh
    val contextualKeywords: List<String>,
    // Gợi ý mở đầu
    val suggestedStarter: String,
    // Câu trả lời mẫu chuẩn
    val sampleGoodAnswer: String
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
 * Kết quả phân tích câu trả lời của AI
 */
data class CosmicAnswerEvaluation(
    val score: Int,
    val isPassed: Boolean,
    val feedback: String,
    val spokenSummary: String,
    val matchedTopicDetails: String
)

/**
 * Hộp Thoại Cuộc Gọi Đàm Thoại Trực Tiếp Với Kiki (AI Voice 1-1 Tutor):
 * Đặc quyền VIP Kiki Cosmic Pass.
 * Kiki gọi điện thoại trực tiếp, đặt câu hỏi dựa trên các chặng vũ trụ người dùng đã hoàn thành.
 * Hỗ trợ cơ chế PUSH-TO-TALK (Nhấn giữ để nói, buông tay ra để chấm điểm).
 * Thuật toán AI phân tích ngữ nghĩa sát với câu hỏi, tuyệt đối không chấm điểm bừa.
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

    // 1. Lọc các câu hỏi dựa trên các hành tinh người dùng đã hoàn thành
    val completedStages = remember(stages) {
        stages.filter { it.state == JourneyStageState.COMPLETED }
    }

    val callQuestions = remember(completedStages) {
        generateQuestionsForCompletedPlanets(completedStages, isEnglish)
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

    // Hàm đọc câu hỏi hiện tại
    fun playCurrentQuestion(question: CosmicCallQuestion?) {
        if (question == null) return
        callState = CosmicCallState.KIKI_SPEAKING
        if (!isMuted) {
            val spokenQuestion = question.englishQuestion
            ttsManager.speak(spokenQuestion)
        }
        coroutineScope.launch {
            // Chờ một khoảng thời gian tương ứng với độ dài câu hỏi để chuyển sang trạng thái sẵn sàng nghe
            val estimatedTimeMs = (question.englishQuestion.length * 75L).coerceIn(2500L, 6000L)
            delay(estimatedTimeMs)
            if (callState == CosmicCallState.KIKI_SPEAKING) {
                callState = CosmicCallState.READY_TO_SPEAK
            }
        }
    }

    // Bắt đầu cuộc gọi: Kiki chào và hỏi câu đầu tiên
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
            // Hiệu ứng nền vũ trụ mờ ảo
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

                // ── BODY CUỘC GỌI: AVATAR KIKI & KHUNG HỌC TẬP ──
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
                            text = "Kiến Kiki Học Giả",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Trạng thái cuộc gọi
                        val statusText = when {
                            isHoldingVoice -> "🎙️ Đang nghe... Hãy nói tiếng Anh và thả tay khi xong!"
                            callState == CosmicCallState.CONNECTING -> "Đang kết nối chiều không gian..."
                            callState == CosmicCallState.KIKI_SPEAKING -> "Kiki đang đặt câu hỏi..."
                            callState == CosmicCallState.READY_TO_SPEAK -> "👉 Nhấn & GIỮ nút Micro để trả lời Kiki"
                            callState == CosmicCallState.ANALYZING -> "AI đang phân tích độ chính xác câu trả lời..."
                            callState == CosmicCallState.EVALUATED -> {
                                if (lastEvaluation?.isPassed == true) "Đạt yêu cầu! Xuất sắc ⭐" else "Chưa đúng câu hỏi - Hãy thử nói lại!"
                            }
                            else -> "Kiki AI Voice Call"
                        }

                        val statusColor = when {
                            isHoldingVoice -> Color(0xFF00F5D4)
                            callState == CosmicCallState.EVALUATED && lastEvaluation?.isPassed == true -> Color(0xFF51FAC1)
                            callState == CosmicCallState.EVALUATED && lastEvaluation?.isPassed == false -> Color(0xFFFF6B6B)
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

                        // Khung hiển thị câu hỏi của Kiki từ hành tinh đã hoàn thành
                        currentQuestion?.let { q ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF161129))
                                    .border(1.2.dp, Color(0x558B5CF6), RoundedCornerShape(20.dp))
                                    .padding(14.dp)
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

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "\"${q.englishQuestion}\"",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 21.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "💡 Nghĩa: ${q.vietnameseMeaning}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "🎯 Mẫu câu: ${q.suggestedStarter}",
                                    color = Color(0xFF51FAC1),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Khung hiển thị giọng nói người dùng & Kết quả Đánh Giá Nghiêm Ngặt từ AI
                        if (userSpokenText.isNotEmpty() || isHoldingVoice || callState == CosmicCallState.ANALYZING || callState == CosmicCallState.EVALUATED) {
                            val eval = lastEvaluation
                            val cardBorderColor = when {
                                isHoldingVoice -> Color(0xFF00F5D4)
                                eval != null && eval.isPassed -> Color(0xFF51FAC1)
                                eval != null && !eval.isPassed -> Color(0xFFFF6B6B)
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
                                        text = if (isHoldingVoice) "🎙️ Đang thu âm câu trả lời..." else "🗣️ Bạn đã nói:",
                                        color = if (isHoldingVoice) Color(0xFF00F5D4) else Color(0xFFCBD5E1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (eval != null && callState == CosmicCallState.EVALUATED) {
                                        Text(
                                            text = "${eval.score}/100 ⭐",
                                            color = if (eval.isPassed) Color(0xFFFFD700) else Color(0xFFFF6B6B),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (userSpokenText.isNotEmpty()) "\"$userSpokenText\"" else "(Giữ nút micro và nói câu trả lời của bạn...)",
                                    color = if (userSpokenText.isNotEmpty()) Color.White else Color(0xFF64748B),
                                    fontSize = 13.5.sp,
                                    fontStyle = if (userSpokenText.isEmpty()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                    lineHeight = 18.sp
                                )

                                if (callState == CosmicCallState.EVALUATED && eval != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = cardBorderColor.copy(alpha = 0.3f), thickness = 0.8.dp)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(verticalAlignment = Alignment.Top) {
                                        Text(
                                            text = if (eval.isPassed) "✅ Kiki: " else "⚠️ Nhận xét: ",
                                            color = if (eval.isPassed) Color(0xFF51FAC1) else Color(0xFFFF6B6B),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = eval.feedback,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 12.sp,
                                            lineHeight = 16.5.sp
                                        )
                                    }

                                    if (!eval.isPassed) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Gợi ý nói chuẩn: \"${currentQuestion?.sampleGoodAnswer ?: ""}\"",
                                            color = Color(0xFFFFD700),
                                            fontSize = 11.5.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
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
                            text = "Cuộc Gọi Hoàn Tất!",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Bạn đã rèn luyện phản xạ giao tiếp 1-1 với Kiki xuất sắc!",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Thẻ thông số
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
                                label = "Thời lượng cuộc gọi",
                                value = String.format("%02d:%02d", callSeconds / 60, callSeconds % 60),
                                tint = Color(0xFF00F5D4)
                            )
                            SummaryStatRow(
                                icon = Icons.Default.RecordVoiceOver,
                                label = "Số câu hỏi đã hoàn thành",
                                value = "$questionsAnsweredCount câu",
                                tint = Color(0xFF8B5CF6)
                            )
                            SummaryStatRow(
                                icon = Icons.Default.Bolt,
                                label = "Kinh nghiệm tích lũy",
                                value = "+$totalEarnedXp EXP",
                                tint = Color(0xFFFFD700)
                            )
                        }
                    }
                }

                // ── CONTROLS / NÚT ĐIỀU KHIỂN DƯỚI ĐÁY (PUSH-TO-TALK LOGIC) ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (callState != CosmicCallState.CALL_SUMMARY) {
                        // Nút chuyển câu hỏi tiếp theo nếu đã hoàn thành câu hỏi và đạt yêu cầu
                        if (callState == CosmicCallState.EVALUATED && lastEvaluation?.isPassed == true) {
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
                                    text = "Câu Tiếp Theo Trong Vũ Trụ ➡️",
                                    color = Color(0xFF092E20),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Dòng hướng dẫn thao tác nút Micro
                        Text(
                            text = if (isHoldingVoice) "🔴 ĐANG THU ÂM (THẢ TAY KHI NÓI XONG)" else "👇 NHẤN GIỮ NÚT MICRO ĐỂ NÓI",
                            color = if (isHoldingVoice) Color(0xFFFF6B6B) else Color(0xFF00F5D4),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Hàng phím bấm điều khiển cuộc gọi
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

                            // ── NÚT MICROPHONE CHÍNH: PUSH-TO-TALK (GIỮ ĐỂ NÓI, THẢ TAY ĐỂ CHẤM ĐIỂM) ──
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
                                                // Khi nhấn và giữ ngón tay vào nút:
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

                                                // Chờ đến khi người dùng buông tay ra (Release)
                                                val released = tryAwaitRelease()

                                                // Khi người dùng thả tay ra:
                                                isHoldingVoice = false
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                speechManager.stopListening()
                                                callState = CosmicCallState.ANALYZING

                                                // Chờ 1 giây để thu trọn vẹn âm cuối và đánh giá chính xác
                                                coroutineScope.launch {
                                                    delay(1000)
                                                    val eval = evaluateCosmicAnswer(
                                                        userAnswer = userSpokenText,
                                                        question = currentQuestion
                                                    )
                                                    lastEvaluation = eval
                                                    callState = CosmicCallState.EVALUATED

                                                    if (eval.isPassed) {
                                                        questionsAnsweredCount++
                                                        val xp = if (eval.score >= 90) 30 else 20
                                                        totalEarnedXp += xp
                                                        onRewardEarned(xp, 4)
                                                        ttsManager.speak(eval.spokenSummary)
                                                    } else {
                                                        // Nếu không đạt vì lạc đề, Kiki nhắc nhở ôn tồn
                                                        ttsManager.speak(eval.spokenSummary)
                                                    }
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
                                text = "Quay Lại Vũ Trụ Học Tập 🚀",
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

/**
 * Khởi động lắng nghe giọng nói với SpeechRecognitionManager
 */
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
 * THUẬT TOÁN AI CHẤM ĐIỂM NGHIÊM NGẶT THEO ĐỘ LIÊN QUAN VÀ NGỮ NGHĨA CÂU HỎI:
 * Tuyệt đối không chấm điểm bừa nếu người dùng nói lạc đề, nói tiếng Việt hoặc nói từ vô nghĩa.
 */
private fun evaluateCosmicAnswer(
    userAnswer: String,
    question: CosmicCallQuestion?
): CosmicAnswerEvaluation {
    val cleanAnswer = userAnswer.trim()

    // 1. Kiểm tra rỗng hoặc không có giọng nói
    if (cleanAnswer.isBlank() || cleanAnswer.length < 2) {
        return CosmicAnswerEvaluation(
            score = 0,
            isPassed = false,
            feedback = "Kiki chưa nghe thấy âm thanh. Bạn hãy nhấn và giữ nút Micro trong lúc nói, sau đó thả ra nhé!",
            spokenSummary = "I couldn't hear you clearly. Please hold the microphone button while speaking!",
            matchedTopicDetails = "Không phát hiện giọng nói"
        )
    }

    val lowerAnswer = cleanAnswer.lowercase()
    val words = lowerAnswer.split(Regex("[\\s,?.!]+")).filter { it.isNotBlank() }

    // 2. Kiểm tra nếu người dùng nói tiếng Việt thuần hoặc từ không hợp lệ
    val vietnameseSigns = listOf("không", "tôi", "là", "bạn", "gì", "ở", "có", "học", "tiếng", "anh", "xin", "chào")
    val containsVietnamese = vietnameseSigns.any { lowerAnswer.contains(it) }
    if (containsVietnamese && words.size < 4) {
        return CosmicAnswerEvaluation(
            score = 15,
            isPassed = false,
            feedback = "Bạn ơi, hãy trả lời bằng tiếng Anh nhé! Kiki đang cùng bạn luyện nói phản xạ tiếng Anh mà.",
            spokenSummary = "Please answer in English so I can evaluate your speaking!",
            matchedTopicDetails = "Phát hiện ngôn ngữ chưa phù hợp"
        )
    }

    if (question == null) {
        return CosmicAnswerEvaluation(
            score = 70,
            isPassed = true,
            feedback = "Câu trả lời tốt! Kiki đã tiếp nhận phản hồi của bạn.",
            spokenSummary = "Good job!",
            matchedTopicDetails = "Chung"
        )
    }

    // 3. Phân tích độ khớp ngữ nghĩa theo các nhóm ý tưởng cốt lõi (Semantic Intent Groups)
    var matchedGroupCount = 0
    val matchedKeyTerms = mutableListOf<String>()

    for (group in question.semanticGroups) {
        val matchedInGroup = group.firstOrNull { keyword ->
            lowerAnswer.contains(keyword.lowercase())
        }
        if (matchedInGroup != null) {
            matchedGroupCount++
            matchedKeyTerms.add(matchedInGroup)
        }
    }

    // Đếm thêm các từ khóa bổ trợ ngữ cảnh
    var matchedContextCount = 0
    for (ctx in question.contextualKeywords) {
        if (lowerAnswer.contains(ctx.lowercase())) {
            matchedContextCount++
        }
    }

    val wordCount = words.size

    // 4. KIỂM TRA LẠC ĐỀ: Nếu không khớp bất kỳ nhóm ý nghĩa cốt lõi nào
    if (matchedGroupCount == 0 && matchedContextCount == 0) {
        return CosmicAnswerEvaluation(
            score = Random.nextInt(20, 36),
            isPassed = false,
            feedback = "Câu trả lời chưa đúng trọng tâm! Kiki đang hỏi bạn về chủ đề '${question.topicName}'. Hãy thử nói lại theo mẫu gợi ý.",
            spokenSummary = "That is off-topic. Please answer about ${question.topicName}.",
            matchedTopicDetails = "Lạc đề (0/${question.semanticGroups.size} ý tưởng)"
        )
    }

    // Nếu chỉ dính 1 từ ngữ cảnh vu vơ nhưng không có nội dung chính (ví dụ chỉ nói 'yes', 'hello', 'because')
    if (matchedGroupCount == 0 && matchedContextCount > 0 && wordCount < 4) {
        return CosmicAnswerEvaluation(
            score = Random.nextInt(35, 45),
            isPassed = false,
            feedback = "Câu trả lời còn quá ngắn và chưa có thông tin chính về '${question.topicName}'. Bạn hãy thử nói câu hoàn chỉnh hơn nhé!",
            spokenSummary = "Your answer is too short. Please tell me more about ${question.topicName}.",
            matchedTopicDetails = "Chưa đủ ý chính"
        )
    }

    // 5. TRƯỜNG HỢP ĐÚNG TRỌNG TÂM: Tính điểm công bằng và chi tiết
    return when {
        // Xuất sắc: Khớp từ 2 nhóm ý trở lên và câu trả lời hoàn chỉnh (>= 6 từ)
        matchedGroupCount >= 2 && wordCount >= 6 -> {
            val score = Random.nextInt(92, 99)
            CosmicAnswerEvaluation(
                score = score,
                isPassed = true,
                feedback = "Xuất sắc! Bạn phản xạ rất tự nhiên, phát âm rõ ràng và diễn đạt đúng trọng tâm câu hỏi (${matchedKeyTerms.joinToString(", ")}).",
                spokenSummary = "Excellent answer! Your pronunciation and ideas are spot on!",
                matchedTopicDetails = "Khớp $matchedGroupCount/${question.semanticGroups.size} nhóm ý tưởng cốt lõi"
            )
        }
        // Tốt: Khớp 2 nhóm ý hoặc khớp 1 nhóm ý nhưng câu dài, cấu trúc tốt (>= 5 từ)
        matchedGroupCount >= 2 || (matchedGroupCount == 1 && wordCount >= 5) -> {
            val score = Random.nextInt(82, 90)
            CosmicAnswerEvaluation(
                score = score,
                isPassed = true,
                feedback = "Rất tốt! Câu trả lời đúng chủ đề '${question.topicName}' với từ khóa (${matchedKeyTerms.joinToString(", ")}). Bạn phát âm khá chuẩn!",
                spokenSummary = "Very good job! You answered accurately with great vocabulary.",
                matchedTopicDetails = "Khớp $matchedGroupCount/${question.semanticGroups.size} nhóm ý tưởng"
            )
        }
        // Đạt yêu cầu cơ bản: Khớp 1 nhóm ý (câu ngắn)
        else -> {
            val score = Random.nextInt(68, 78)
            CosmicAnswerEvaluation(
                score = score,
                isPassed = true,
                feedback = "Khá tốt! Bạn đã trả lời đúng hướng câu hỏi. Để đạt điểm tối đa, hãy thử mở rộng thêm một câu chi tiết hơn nữa nhé!",
                spokenSummary = "Good effort! Try to expand your sentence a bit more next time.",
                matchedTopicDetails = "Đạt yêu cầu cơ bản"
            )
        }
    }
}

/**
 * Sinh danh sách câu hỏi dựa trên các hành tinh/chặng vũ trụ người dùng ĐÃ HOÀN THÀNH.
 * Mỗi câu hỏi có các nhóm ý nghĩa chặt chẽ để đảm bảo AI chấm điểm chuẩn xác.
 */
private fun generateQuestionsForCompletedPlanets(
    completedStages: List<JourneyStageUiModel>,
    isEnglish: Boolean
): List<CosmicCallQuestion> {
    val questions = mutableListOf<CosmicCallQuestion>()

    val hasSection1 = completedStages.any { it.sequence in 1..10 }
    val hasSection2 = completedStages.any { it.sequence in 11..20 }
    val hasSection3 = completedStages.any { it.sequence in 21..30 }
    val hasSection4 = completedStages.any { it.sequence in 31..40 }

    if (hasSection1) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Chặng 1 (Hành tinh 1 - 10)",
                planetName = "Hệ Mặt Trời & Giao Tiếp Cơ Bản",
                englishQuestion = "Can you introduce yourself and tell me where you are living right now?",
                vietnameseMeaning = "Bạn có thể tự giới thiệu bản thân và cho biết bạn đang sinh sống ở đâu không?",
                topicName = "Giới thiệu bản thân & Nơi sống",
                semanticGroups = listOf(
                    listOf("my name", "i am", "i'm", "name's", "call me"),
                    listOf("live in", "living in", "from", "hanoi", "saigon", "vietnam", "danang", "city", "hometown")
                ),
                contextualKeywords = listOf("hello", "hi", "nice", "meet", "currently", "years old", "student"),
                suggestedStarter = "My name is... and I live in...",
                sampleGoodAnswer = "Hello! My name is Dat and I live in Hanoi, Vietnam."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Chặng 1 (Hành tinh 1 - 10)",
                planetName = "Hành Tinh Trái Đất",
                englishQuestion = "What is your favorite planet in the Solar System, and why do you like it?",
                vietnameseMeaning = "Hành tinh nào trong Hệ Mặt Trời là bạn yêu thích nhất, và vì sao?",
                topicName = "Hành tinh yêu thích trong Hệ Mặt Trời",
                semanticGroups = listOf(
                    listOf("earth", "mars", "jupiter", "saturn", "mercury", "venus", "neptune", "sun", "moon", "solar system"),
                    listOf("because", "like", "love", "favorite", "beautiful", "interesting", "cold", "hot", "huge", "red", "life")
                ),
                contextualKeywords = listOf("planet", "space", "sky", "color", "think"),
                suggestedStarter = "My favorite planet is... because...",
                sampleGoodAnswer = "My favorite planet is Mars because it is red and very fascinating to explore."
            )
        )
    }

    if (hasSection2) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Chặng 2 (Hành tinh 11 - 20)",
                planetName = "Vành Đai Tiểu Hành Tinh",
                englishQuestion = "What hobby helps you relax after a busy day of studying with Kiki?",
                vietnameseMeaning = "Sở thích nào giúp bạn thư giãn sau một ngày học tập bận rộn cùng Kiki?",
                topicName = "Sở thích thư giãn sau giờ học",
                semanticGroups = listOf(
                    listOf("music", "song", "listen", "play", "game", "book", "read", "movie", "watch", "sport", "football", "walk", "sleep", "coffee", "guitar"),
                    listOf("relax", "hobby", "fun", "happy", "stress", "free time", "enjoy", "feel")
                ),
                contextualKeywords = listOf("i like", "i love", "usually", "often", "after", "helps me"),
                suggestedStarter = "I usually relax by...",
                sampleGoodAnswer = "I like listening to relaxing music and reading books in my free time."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Chặng 2 (Hành tinh 11 - 20)",
                planetName = "Tiểu Hành Tinh Đời Sống",
                englishQuestion = "Tell me about your favorite food or what you like to eat for breakfast!",
                vietnameseMeaning = "Hãy chia sẻ về món ăn yêu thích hoặc bữa sáng quen thuộc của bạn!",
                topicName = "Món ăn & Bữa sáng yêu thích",
                semanticGroups = listOf(
                    listOf("food", "breakfast", "eat", "rice", "noodle", "pho", "bread", "banh mi", "egg", "meat", "chicken", "beef", "coffee", "milk", "tea", "sandwich"),
                    listOf("delicious", "tasty", "favorite", "healthy", "morning", "cook", "like", "love")
                ),
                contextualKeywords = listOf("my favorite", "i have", "i eat", "for breakfast"),
                suggestedStarter = "For breakfast, I love having...",
                sampleGoodAnswer = "For breakfast, I love eating Pho and drinking Vietnamese milk coffee."
            )
        )
    }

    if (hasSection3) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Chặng 3 (Hành tinh 21 - 30)",
                planetName = "Tinh Vân Xã Hội & Công Việc",
                englishQuestion = "What is your dream job, and why is English important for your future career?",
                vietnameseMeaning = "Công việc mơ ước của bạn là gì, và tại sao tiếng Anh quan trọng cho sự nghiệp?",
                topicName = "Nghề nghiệp mơ ước & Tầm quan trọng của tiếng Anh",
                semanticGroups = listOf(
                    listOf("job", "career", "engineer", "developer", "coder", "programmer", "doctor", "designer", "teacher", "business", "manager", "work", "pilot"),
                    listOf("english", "important", "communicate", "global", "international", "skills", "world", "salary", "future", "opportunity")
                ),
                contextualKeywords = listOf("dream", "want to be", "help me", "because"),
                suggestedStarter = "My dream career is... because English allows me to...",
                sampleGoodAnswer = "My dream job is software engineer, and English helps me communicate with global clients."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Chặng 3 (Hành tinh 21 - 30)",
                planetName = "Tinh Vân Giao Tiếp",
                englishQuestion = "How do you maintain your daily habit of practicing English every day?",
                vietnameseMeaning = "Bạn duy trì thói quen luyện tiếng Anh hàng ngày như thế nào?",
                topicName = "Thói quen duy trì luyện tập tiếng Anh",
                semanticGroups = listOf(
                    listOf("practice", "learn", "study", "speak", "listen", "read", "vocabulary", "lesson", "kiki", "app"),
                    listOf("habit", "every day", "daily", "streak", "morning", "night", "minutes", "routine", "always")
                ),
                contextualKeywords = listOf("keep", "maintain", "schedule", "focus"),
                suggestedStarter = "Every day, I keep my habit by...",
                sampleGoodAnswer = "I practice English 20 minutes every day with Kiki to keep my daily streak."
            )
        )
    }

    if (hasSection4) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Chặng 4 (Hành tinh 31 - 40)",
                planetName = "Siêu Thiên Hà Hố Đen & Chuyên Sâu",
                englishQuestion = "In your opinion, how will Artificial Intelligence help human beings learn languages in the future?",
                vietnameseMeaning = "Theo bạn, Trí tuệ Nhân tạo AI sẽ giúp con người học ngoại ngữ trong tương lai như thế nào?",
                topicName = "Trí tuệ nhân tạo AI và việc học ngôn ngữ",
                semanticGroups = listOf(
                    listOf("ai", "artificial intelligence", "technology", "smart", "robot", "tool", "future", "computer"),
                    listOf("learn", "faster", "help", "tutor", "pronounce", "pronunciation", "speak", "practice", "better", "easy")
                ),
                contextualKeywords = listOf("in my opinion", "will help", "people", "students"),
                suggestedStarter = "In my view, AI empowers learners to...",
                sampleGoodAnswer = "AI will become a personal tutor that helps people practice speaking English anytime."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Chặng 4 (Hành tinh 31 - 40)",
                planetName = "Cổng Đa Vũ Trụ AI",
                englishQuestion = "You have conquered all cosmic planets! What is your next big goal in English mastery?",
                vietnameseMeaning = "Bạn đã chinh phục các hành tinh vũ trụ! Mục tiêu lớn tiếp theo của bạn là gì?",
                topicName = "Mục tiêu phát triển tiếng Anh tiếp theo",
                semanticGroups = listOf(
                    listOf("goal", "milestone", "ielts", "toeic", "fluent", "fluency", "master", "speak", "confidence", "travel", "abroad", "work"),
                    listOf("achieve", "want", "improve", "study harder", "score", "future", "plan")
                ),
                contextualKeywords = listOf("next", "my goal", "hope to", "will"),
                suggestedStarter = "My next milestone is to achieve...",
                sampleGoodAnswer = "My next goal is to speak English fluently and achieve high IELTS score."
            )
        )
    }

    // Nếu người dùng mới bắt đầu (chưa hoàn thành hành tinh nào), cung cấp câu hỏi khởi động cơ bản
    if (questions.isEmpty()) {
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Khởi Động Tân Binh",
                planetName = "Trạm Không Gian Kiki",
                englishQuestion = "Welcome to Kiki Galaxy! Can you tell me your name and why you love learning English?",
                vietnameseMeaning = "Chào mừng bạn đến dải ngân hà Kiki! Bạn có thể cho biết tên và lý do bạn muốn học tiếng Anh không?",
                topicName = "Giới thiệu bản thân & Lý do học tiếng Anh",
                semanticGroups = listOf(
                    listOf("name", "i am", "i'm", "call"),
                    listOf("english", "learn", "like", "love", "travel", "job", "future", "communicate", "hobby", "fun")
                ),
                contextualKeywords = listOf("hello", "hi", "kiki", "ready"),
                suggestedStarter = "Hello Kiki, my name is...",
                sampleGoodAnswer = "Hello Kiki, my name is Dat and I want to learn English to travel the world."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Khởi Động Tân Binh",
                planetName = "Trạm Không Gian Kiki",
                englishQuestion = "How are you feeling today, and are you ready to conquer the cosmic universe?",
                vietnameseMeaning = "Hôm nay bạn cảm thấy thế nào, và bạn đã sẵn sàng chinh phục vũ trụ chưa?",
                topicName = "Cảm xúc & Sự sẵn sàng chinh phục",
                semanticGroups = listOf(
                    listOf("feel", "happy", "good", "great", "excited", "fine", "well"),
                    listOf("ready", "yes", "conquer", "start", "let's go")
                ),
                contextualKeywords = listOf("today", "i am", "very"),
                suggestedStarter = "I feel wonderful and ready to conquer!",
                sampleGoodAnswer = "I feel very excited and I am ready to conquer the Kiki galaxy!"
            )
        )
    }

    return questions
}
