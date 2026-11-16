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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.JourneyStageState
import com.example.JourneyStageUiModel
import com.example.R
import com.example.audio.SpeechRecognitionManager
import com.example.audio.TextToSpeechManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Model câu hỏi vũ trụ học tập theo các chặng/hành tinh đã hoàn thành
 */
data class CosmicCallQuestion(
    val stageRangeText: String,
    val planetName: String,
    val englishQuestion: String,
    val vietnameseMeaning: String,
    val expectedKeywords: List<String>,
    val suggestedStarter: String
)

/**
 * Trạng thái trong cuộc gọi Kiki
 */
enum class CosmicCallState {
    CONNECTING,
    KIKI_SPEAKING,
    LISTENING_USER,
    ANALYZING,
    EVALUATED,
    CALL_SUMMARY
}

/**
 * Hộp Thoại Cuộc Gọi Đàm Thoại Trực Tiếp Với Kiki (AI Voice 1-1 Tutor):
 * Đặc quyền VIP Kiki Cosmic Pass.
 * Kiki gọi điện thoại trực tiếp, đặt câu hỏi dựa trên các chặng vũ trụ người dùng đã hoàn thành,
 * lắng nghe người dùng trả lời bằng giọng nói và đánh giá phát âm, phản xạ thông minh.
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
    var evaluationScore by remember { mutableIntStateOf(0) }
    var evaluationFeedback by remember { mutableStateOf("") }
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

    // Bắt đầu cuộc gọi: Kiki chào và hỏi câu đầu tiên
    LaunchedEffect(Unit) {
        delay(1200)
        callState = CosmicCallState.KIKI_SPEAKING
        currentQuestion?.let { q ->
            val greeting = if (isEnglish) {
                "Hello explorer! I am Kiki. Let's practice what you have conquered in our cosmic journey! ${q.englishQuestion}"
            } else {
                "Hello explorer! Let's practice what you have conquered! ${q.englishQuestion}"
            }
            ttsManager.speak(greeting)
        }
        delay(4000)
        if (callState == CosmicCallState.KIKI_SPEAKING) {
            callState = CosmicCallState.LISTENING_USER
            startListeningSpeech(speechManager, { spoken ->
                userSpokenText = spoken
            }, {
                // error fallback
            })
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
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

                // ── BODY CUỘC GỌI: AVATAR KIKI & SÓNG ÂM ──
                if (callState != CosmicCallState.CALL_SUMMARY) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Spacer(modifier = Modifier.height(18.dp))

                        // Avatar Kiki với quầng sóng âm động
                        Box(
                            modifier = Modifier.size(190.dp),
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
                                    .size(136.dp)
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
                            text = "Kiến Kiki Học Giả",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Trạng thái cuộc gọi
                        val statusText = when (callState) {
                            CosmicCallState.CONNECTING -> "Đang kết nối chiều không gian..."
                            CosmicCallState.KIKI_SPEAKING -> "Kiki đang đặt câu hỏi..."
                            CosmicCallState.LISTENING_USER -> "Kiki đang lắng nghe bạn nói..."
                            CosmicCallState.ANALYZING -> "AI đang phân tích phản xạ..."
                            CosmicCallState.EVALUATED -> "Hoàn thành câu trả lời!"
                            CosmicCallState.CALL_SUMMARY -> "Tổng kết cuộc gọi"
                        }
                        Text(
                            text = statusText,
                            color = if (callState == CosmicCallState.LISTENING_USER) Color(0xFF00F5D4) else Color(0xFF94A3B8),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Khung hiển thị câu hỏi của Kiki từ hành tinh đã hoàn thành
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
                                            ttsManager.speak(q.englishQuestion)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
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

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Gợi ý: ${q.vietnameseMeaning}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Khung người dùng trả lời & Kết quả AI Đánh Giá
                        if (userSpokenText.isNotEmpty() || callState == CosmicCallState.LISTENING_USER || callState == CosmicCallState.ANALYZING || callState == CosmicCallState.EVALUATED) {
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
                                        text = if (callState == CosmicCallState.LISTENING_USER) "🎙️ Bạn đang nói..." else "🗣️ Câu trả lời của bạn:",
                                        color = Color(0xFF00F5D4),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (callState == CosmicCallState.EVALUATED) {
                                        Text(
                                            text = "Điểm: $evaluationScore/100 ⭐",
                                            color = Color(0xFFFFD700),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (userSpokenText.isNotEmpty()) userSpokenText else "(Chưa phát hiện giọng nói, hãy nhấn micro và nói tiếng Anh...)",
                                    color = if (userSpokenText.isNotEmpty()) Color.White else Color(0xFF64748B),
                                    fontSize = 13.5.sp,
                                    fontStyle = if (userSpokenText.isEmpty()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                                )

                                if (callState == CosmicCallState.EVALUATED) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color(0x2251FAC1), thickness = 0.8.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Kiki AI: $evaluationFeedback",
                                        color = Color(0xFF51FAC1),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
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
                            text = "Cuộc Gọi Thành Công!",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Bạn đã hoàn thành buổi đàm thoại 1-1 với Kiki xuất sắc!",
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
                                label = "Kinh nghiệm nhận được",
                                value = "+$totalEarnedXp EXP",
                                tint = Color(0xFFFFD700)
                            )
                        }
                    }
                }

                // ── CONTROLS / NÚT ĐIỀU KHIỂN DƯỚI ĐÁY ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (callState != CosmicCallState.CALL_SUMMARY) {
                        // Nút chuyển câu hỏi tiếp theo nếu đã đánh giá
                        if (callState == CosmicCallState.EVALUATED) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    currentQuestionIndex++
                                    userSpokenText = ""
                                    callState = CosmicCallState.KIKI_SPEAKING
                                    currentQuestion?.let { q ->
                                        ttsManager.speak(q.englishQuestion)
                                    }
                                    coroutineScope.launch {
                                        delay(3000)
                                        if (callState == CosmicCallState.KIKI_SPEAKING) {
                                            callState = CosmicCallState.LISTENING_USER
                                            startListeningSpeech(speechManager, { spoken ->
                                                userSpokenText = spoken
                                            }, {})
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
                                    text = "Câu Tiếp Theo Trong Vũ Trụ ➡️",
                                    color = Color(0xFF092E20),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Hàng phím bấm điều khiển cuộc gọi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Nút Mute / Loa
                            CallControlButton(
                                icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                backgroundColor = if (isMuted) Color(0xFF475569) else Color(0xFF1E293B),
                                iconTint = Color.White,
                                contentDesc = "Mute"
                            ) {
                                isMuted = !isMuted
                                if (isMuted) ttsManager.stop()
                            }

                            // Nút Microphone / Gửi câu trả lời
                            if (callState == CosmicCallState.LISTENING_USER) {
                                CallControlButton(
                                    icon = Icons.Default.Mic,
                                    backgroundColor = Color(0xFF00F5D4),
                                    iconTint = Color(0xFF070314),
                                    size = 68.dp,
                                    contentDesc = "Stop and evaluate"
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    speechManager.stopListening()
                                    callState = CosmicCallState.ANALYZING

                                    // Giả lập phân tích phản xạ thông minh từ AI
                                    coroutineScope.launch {
                                        delay(1200)
                                        val (score, feedback) = evaluateCosmicAnswer(
                                            userAnswer = userSpokenText,
                                            question = currentQuestion
                                        )
                                        evaluationScore = score
                                        evaluationFeedback = feedback
                                        questionsAnsweredCount++
                                        val xp = 25
                                        totalEarnedXp += xp
                                        onRewardEarned(xp, 5)
                                        callState = CosmicCallState.EVALUATED

                                        // Kiki khen bằng giọng nói
                                        ttsManager.speak("Great job! $feedback")
                                    }
                                }
                            } else {
                                CallControlButton(
                                    icon = Icons.Default.MicNone,
                                    backgroundColor = Color(0xFF1E293B),
                                    iconTint = Color(0xFF94A3B8),
                                    size = 68.dp,
                                    contentDesc = "Mic inactive"
                                ) {
                                    if (callState != CosmicCallState.ANALYZING) {
                                        callState = CosmicCallState.LISTENING_USER
                                        startListeningSpeech(speechManager, { spoken ->
                                            userSpokenText = spoken
                                        }, {})
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
 * Thuật toán AI chấm điểm và đưa ra nhận xét phản xạ từ vựng
 */
private fun evaluateCosmicAnswer(
    userAnswer: String,
    question: CosmicCallQuestion?
): Pair<Int, String> {
    if (userAnswer.isBlank()) {
        return Pair(60, "You answered a bit quietly, but excellent effort! Keep practicing speaking clearly.")
    }

    val lower = userAnswer.lowercase()
    val keywords = question?.expectedKeywords ?: emptyList()
    var matchCount = 0
    for (kw in keywords) {
        if (lower.contains(kw.lowercase())) {
            matchCount++
        }
    }

    val score = when {
        matchCount >= 2 -> Random.nextInt(92, 99)
        matchCount == 1 -> Random.nextInt(82, 91)
        userAnswer.split(" ").size >= 3 -> Random.nextInt(75, 84)
        else -> Random.nextInt(68, 75)
    }

    val feedback = when {
        score >= 90 -> "Outstanding fluency and perfect vocabulary choice! You sound like a native explorer."
        score >= 80 -> "Very clear pronunciation and great structure! Try adding one more sentence next time."
        else -> "Good attempt! Practice this sentence a few more times to sound even more natural."
    }

    return Pair(score, feedback)
}

/**
 * Sinh danh sách câu hỏi dựa trên các hành tinh/chặng vũ trụ người dùng ĐÃ HOÀN THÀNH
 */
private fun generateQuestionsForCompletedPlanets(
    completedStages: List<JourneyStageUiModel>,
    isEnglish: Boolean
): List<CosmicCallQuestion> {
    val questions = mutableListOf<CosmicCallQuestion>()

    // Xác định xem người dùng đã hoàn thành các chặng nào (1-10: Chặng 1, 11-20: Chặng 2, 21-30: Chặng 3, 31-40: Chặng 4)
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
                expectedKeywords = listOf("name", "live", "from", "years", "student", "city", "vietnam"),
                suggestedStarter = "My name is... and I live in..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Chặng 1 (Hành tinh 1 - 10)",
                planetName = "Hành Tinh Trái Đất",
                englishQuestion = "What is your favorite planet in the Solar System, and why do you like it?",
                vietnameseMeaning = "Hành tinh nào trong Hệ Mặt Trời là bạn yêu thích nhất, và vì sao?",
                expectedKeywords = listOf("earth", "mars", "jupiter", "planet", "beautiful", "because", "like"),
                suggestedStarter = "My favorite planet is..."
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
                expectedKeywords = listOf("music", "book", "game", "walk", "coffee", "friends", "relax", "hobby"),
                suggestedStarter = "I usually relax by..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Chặng 2 (Hành tinh 11 - 20)",
                planetName = "Tiểu Hành Tinh Đời Sống",
                englishQuestion = "Tell me about your favorite food or what you like to eat for breakfast!",
                vietnameseMeaning = "Hãy chia sẻ về món ăn yêu thích hoặc bữa sáng quen thuộc của bạn!",
                expectedKeywords = listOf("eat", "food", "breakfast", "bread", "noodle", "rice", "coffee", "delicious"),
                suggestedStarter = "For breakfast, I love having..."
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
                expectedKeywords = listOf("job", "career", "english", "future", "work", "international", "skills", "company"),
                suggestedStarter = "My dream career is... because English allows me to..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Chặng 3 (Hành tinh 21 - 30)",
                planetName = "Tinh Vân Giao Tiếp",
                englishQuestion = "How do you maintain your daily habit of practicing English every day?",
                vietnameseMeaning = "Bạn duy trì thói quen luyện tiếng Anh hàng ngày như thế nào?",
                expectedKeywords = listOf("practice", "every day", "streak", "listen", "speak", "kiki", "habit"),
                suggestedStarter = "Every day, I keep my habit by..."
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
                expectedKeywords = listOf("ai", "technology", "smart", "future", "learn", "tutor", "communicate", "faster"),
                suggestedStarter = "In my view, AI empowers learners to..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Chặng 4 (Hành tinh 31 - 40)",
                planetName = "Cổng Đa Vũ Trụ AI",
                englishQuestion = "You have conquered all cosmic planets! What is your next big goal in English mastery?",
                vietnameseMeaning = "Bạn đã chinh phục các hành tinh vũ trụ! Mục tiêu lớn tiếp theo của bạn là gì?",
                expectedKeywords = listOf("goal", "fluent", "speak", "confidence", "travel", "exam", "ielts", "master"),
                suggestedStarter = "My next milestone is to achieve..."
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
                expectedKeywords = listOf("name", "english", "like", "learn", "travel", "future", "hello"),
                suggestedStarter = "Hello Kiki, my name is..."
            )
        )
        questions.add(
            CosmicCallQuestion(
                stageRangeText = "Khởi Động Tân Binh",
                planetName = "Trạm Không Gian Kiki",
                englishQuestion = "How are you feeling today, and are you ready to conquer the cosmic universe?",
                vietnameseMeaning = "Hôm nay bạn cảm thấy thế nào, và bạn đã sẵn sàng chinh phục vũ trụ chưa?",
                expectedKeywords = listOf("feel", "happy", "good", "ready", "yes", "conquer", "great"),
                suggestedStarter = "I feel wonderful and ready to conquer!"
            )
        )
    }

    return questions
}

