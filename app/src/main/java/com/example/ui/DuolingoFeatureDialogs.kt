package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.model.MascotEmotion
import com.example.ui.mascot.LivingMascotView
import com.example.viewmodel.StudyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Cuộc gọi video tương tác trực tiếp với linh vật Kiki AI (tương tự Video Call với Lily của Duolingo Max).
 * Hiển thị Kiki toàn thân sống động, đàm thoại âm thanh hai chiều qua STT và TTS.
 */
@Composable
fun KikiVideoCallDialog(
    viewModel: StudyViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val isEnglish by viewModel.isEnglish.observeAsState(false)

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }

    var isMicActive by remember { mutableStateOf(false) }
    var kikiSpeech by remember {
        mutableStateOf(
            if (isEnglish) "Hi there! I'm Kiki. It's so exciting to video call with you! What do you want to talk about today?"
            else "Chào bạn! Mình là Kiki nè. Rất vui được gọi video cùng bạn! Hôm nay bạn muốn cùng mình luyện chủ đề gì nào?"
        )
    }
    var userSpeech by remember { mutableStateOf("") }
    var emotion by remember { mutableStateOf(MascotEmotion.GREETING) }

    // Tự động phát âm thanh lời chào khi bắt đầu cuộc gọi
    LaunchedEffect(Unit) {
        delay(300)
        viewModel.ttsManager.speak(kikiSpeech)
    }

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val liveDotAlpha by pulseAnim.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "live_dot"
    )

    Dialog(
        onDismissRequest = {
            viewModel.ttsManager.stop()
            viewModel.speechRecognitionManager.stopListening()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F0C29), Color(0xFF1E1B4B), Color(0xFF0A071B))
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header: Tiêu đề cuộc gọi + Nút đóng
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00F5D4).copy(alpha = liveDotAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEnglish) "LIVE VIDEO CALL WITH KIKI" else "CUỘC GỌI VIDEO VỚI KIKI",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.ttsManager.stop()
                        viewModel.speechRecognitionManager.stopListening()
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Body: Nhân vật Kiki toàn thân động + Phụ đề thoại
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 60.dp, bottom = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Kiki Toàn thân tương tác sống động
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LivingMascotView(
                        emotion = emotion,
                        isFullBody = true,
                        modifier = Modifier.fillMaxHeight(),
                        onClick = {
                            coroutineScope.launch {
                                emotion = MascotEmotion.CHEER
                                val cheerMsg = if (isEnglish) "You're doing fantastic! Keep speaking with me!" else "Bạn nói hay lắm! Hãy tiếp tục luyện cùng mình nhé!"
                                kikiSpeech = cheerMsg
                                viewModel.ttsManager.speak(cheerMsg)
                                delay(2000)
                                emotion = MascotEmotion.GREETING
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bong bóng phụ đề của Kiki
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0x2E1E293B))
                        .border(1.dp, Color(0x448E24AA), RoundedCornerShape(22.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = kikiSpeech,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (userSpeech.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Bạn: \"$userSpeech\"",
                        color = Color(0xFF00F5D4),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Thanh điều khiển cuộc gọi dưới đáy màn hình (Bottom Call Dock)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 30.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nút Loa: Phát lại giọng của Kiki
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.ttsManager.speak(kikiSpeech)
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speak",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Nút Micro chính: Nhấn giữ hoặc chạm để nói chuyện với Kiki
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (isMicActive) listOf(Color(0xFFFF5252), Color(0xFFFF1744))
                                else listOf(Color(0xFFAB47BC), Color(0xFF8E24AA))
                            )
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!hasAudioPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@clickable
                            }
                            if (!isMicActive) {
                                isMicActive = true
                                emotion = MascotEmotion.LISTENING
                                viewModel.speechRecognitionManager.startListening(
                                    onResult = { text ->
                                        isMicActive = false
                                        userSpeech = text
                                        emotion = MascotEmotion.CHEER
                                        val reply = if (isEnglish) "That sounds great! \"$text\" - you have nice pronunciation!"
                                        else "Nghe rất tuyệt! Bạn phát âm từ \"$text\" rất tự nhiên đó!"
                                        kikiSpeech = reply
                                        viewModel.ttsManager.speak(reply)
                                    },
                                    onError = {
                                        isMicActive = false
                                        emotion = MascotEmotion.GREETING
                                    }
                                )
                            } else {
                                isMicActive = false
                                viewModel.speechRecognitionManager.stopListening()
                                emotion = MascotEmotion.GREETING
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMicActive) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Nút Cúp máy đỏ: Kết thúc cuộc gọi
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.ttsManager.stop()
                        viewModel.speechRecognitionManager.stopListening()
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3366))
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Hang up",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

/**
 * Phòng luyện phát âm tương tác chuyên sâu (Pronunciation Practice).
 * Cho phép người học luyện nói các câu mẫu tiếng Anh, xem sóng âm và nhận điểm chuẩn xác IPA.
 */
@Composable
fun PronunciationPracticeDialog(
    viewModel: StudyViewModel,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isEnglish by viewModel.isEnglish.observeAsState(false)

    val practiceSentences = remember {
        listOf(
            "Hello! Welcome to the cosmic English adventure with Kiki.",
            "Practice makes perfect every single day.",
            "Exploring new vocabulary opens doors to the universe.",
            "Consistent daily practice builds confident communication.",
            "Never stop learning because knowledge has no boundaries."
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentSentence = practiceSentences[currentIndex]

    Dialog(
        onDismissRequest = {
            viewModel.ttsManager.stop()
            viewModel.speechRecognitionManager.stopListening()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F111A))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF6F59)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "Pronunciation Practice" else "Luyện Phát Âm Chuẩn",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Câu ${currentIndex + 1}/${practiceSentences.size}",
                                color = Color(0x88FFFFFF),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.ttsManager.stop()
                            viewModel.speechRecognitionManager.stopListening()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Khung thử thách phát âm với sóng âm & chấm điểm IPA
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    SpeakingChallengeView(
                        targetSentence = currentSentence,
                        isEnglish = isEnglish,
                        speechManager = viewModel.speechRecognitionManager,
                        onSpeakSentence = { isSlow ->
                            viewModel.ttsManager.speak(currentSentence, isSlow)
                        },
                        onSpeakWord = { word ->
                            viewModel.ttsManager.speak(word)
                        },
                        onEvaluationComplete = { result ->
                            // Đã hoàn tất đánh giá phát âm
                        }
                    )
                }

                // Nút chuyển câu tiếp theo
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentIndex = (currentIndex + 1) % practiceSentences.size
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6F59)
                    )
                ) {
                    Text(
                        text = if (isEnglish) "Next Sentence" else "Luyện Câu Tiếp Theo",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

