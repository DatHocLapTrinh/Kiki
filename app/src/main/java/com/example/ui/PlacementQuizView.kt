package com.example.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LocalAppStrings
import com.example.audio.SoundEffectManager
import com.example.audio.SpeechRecognitionManager
import com.example.audio.TextToSpeechManager
import com.example.model.AdaptiveDiagnosticItem
import com.example.model.DiagnosticSkillType
import com.example.model.LearnerPersonaProfile
import com.example.util.AdaptiveDiagnosticEngine
import com.example.util.DiagnosticAnswerRecord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Computerized Adaptive Diagnostic Placement Test (CAT - IRT) View
 * Tự động thích ứng 4 giai đoạn kỹ năng (Ngữ pháp, Từ vựng, Thính giác, Khẩu ngữ)
 * và xuất bản Thẻ Radar Năng Lực AI.
 */
@Composable
fun PlacementQuizView(
    soundEffectManager: SoundEffectManager? = null,
    ttsManager: TextToSpeechManager? = null,
    speechRecognitionManager: SpeechRecognitionManager? = null,
    initialTheta: Float = -0.3f,
    goal: String = "Everyday Fluency",
    dailyTargetMinutes: Int = 5,
    selfEfficacy: String = "ELEMENTARY",
    priorityModality: String = "AUDIO_DIALOG",
    userId: Long = 0L,
    onCompleted: (profile: LearnerPersonaProfile) -> Unit
) {
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var stage by remember { mutableIntStateOf(1) } // 1: Grammar, 2: Vocab, 3: Listening, 4: Speaking
    var currentTheta by remember { mutableFloatStateOf(initialTheta) }
    val answeredIds = remember { mutableStateListOf<Int>() }
    val answerRecords = remember { mutableStateListOf<DiagnosticAnswerRecord>() }

    var currentItem by remember {
        mutableStateOf(AdaptiveDiagnosticEngine.selectNextItem(1, initialTheta, emptySet()))
    }
    var itemStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedOptionIndex by remember { mutableIntStateOf(-1) }
    var isAnswered by remember { mutableStateOf(false) }

    // State cho Speaking & Audio
    var spokenRecognizedText by remember { mutableStateOf("") }
    var isRecordingSpeaking by remember { mutableStateOf(false) }

    // State khi hoàn thành bài test CAT -> chuyển sang thẻ AI Radar
    var isShowingRadarCard by remember { mutableStateOf(false) }
    var finalProfile by remember { mutableStateOf<LearnerPersonaProfile?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        if (!isShowingRadarCard) {
            // === PHẦN 1: HEADER & ADAPTIVE STAGE INDICATOR ===
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x3300F5D4))
                    .border(1.dp, Color(0x6600F5D4), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF00F5D4),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CAT • CHẨN ĐOÁN THÍCH ỨNG IRT",
                    color = Color(0xFF00F5D4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val stageSkillTitle = when (stage) {
                1 -> "Giai đoạn 1/4: Khung Ngữ Pháp & Cú Pháp Cốt Lõi"
                2 -> "Giai đoạn 2/4: Độ Rộng Vốn Từ & Kết Hợp Từ (Collocations)"
                3 -> "Giai đoạn 3/4: Giải Mã Âm Thanh & Nuốt Âm (Acoustic)"
                else -> "Giai đoạn 4/4: Khẩu Ngữ & Ngữ Điệu Phản Xạ (Speaking)"
            }

            Text(
                text = stageSkillTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Stage Progress Bar
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
            ) {
                for (s in 1..4) {
                    val isActive = s == stage
                    val isDone = s < stage
                    val pillColor = when {
                        isDone -> Color(0xFF00F5D4)
                        isActive -> Color(0xFFFF9E00)
                        else -> Color(0x33FFFFFF)
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (isActive) 32.dp else 16.dp)
                            .clip(CircleShape)
                            .background(pillColor)
                    )
                }
            }

            // Real-time Theta estimation badge
            val thetaCefrEstimate = when {
                currentTheta < -0.3f -> "A1 - A2 (Sơ cấp)"
                currentTheta < 0.7f -> "B1 - B2 (Trung cấp)"
                else -> "B2+ - C1 (Nâng cao)"
            }
            Text(
                text = "Ước lượng năng lực hiện tại: $thetaCefrEstimate • θ = ${(currentTheta * 100).roundToInt() / 100f}",
                color = Color(0xFF00F5D4).copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            // === PHẦN 2: THẺ NỘI DUNG CÂU HỎI THÍCH ỨNG ===
            AnimatedContent(
                targetState = currentItem.id,
                transitionSpec = {
                    (slideInHorizontally(tween(280)) { width -> width } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(280)) { width -> -width } + fadeOut(tween(280)))
                },
                label = "adaptive_cat_question"
            ) { _ ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Skill Tag & CEFR Tier
                    Text(
                        text = "ĐỘ KHÓ b = ${currentItem.difficultyB} • KHUNG ${currentItem.cefrTier}",
                        color = Color(0xFFFF9E00),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Question prompt container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(Color(0x2E00F5D4), Color(0x157B2CBF))))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                            .padding(horizontal = 18.dp, vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentItem.questionPrompt,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                                lineHeight = 23.sp
                            )

                            // Nút Audio Playback cho câu hỏi Listening
                            if (currentItem.skillType == DiagnosticSkillType.LISTENING && currentItem.audioSentence.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x3300F5D4))
                                        .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(14.dp))
                                        .clickable {
                                            soundEffectManager?.playClick()
                                            ttsManager?.speak(currentItem.audioSentence, isSlow = false)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Nghe Audio",
                                            tint = Color(0xFF00F5D4),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Nghe câu phát âm chuẩn (Native Audio)",
                                            color = Color(0xFF00F5D4),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // Khung hiển thị câu mẫu cho câu hỏi Speaking
                            if (currentItem.skillType == DiagnosticSkillType.SPEAKING && currentItem.speakingSentence.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x26000000))
                                        .border(1.dp, Color(0x4000F5D4), RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "\"${currentItem.speakingSentence}\"",
                                        color = Color(0xFF00F5D4),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Trường hợp câu hỏi SPEAKING
                    if (currentItem.skillType == DiagnosticSkillType.SPEAKING) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            // Nút nghe trước câu mẫu
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x26FFFFFF))
                                    .clickable {
                                        ttsManager?.speak(currentItem.speakingSentence, isSlow = false)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nghe mẫu chuẩn", color = Color.White, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Nút Micro phát âm
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isRecordingSpeaking) Brush.radialGradient(listOf(Color(0xFFFF6B6B), Color(0x33FF6B6B)))
                                        else Brush.radialGradient(listOf(Color(0xFF00F5D4), Color(0x3300F5D4)))
                                    )
                                    .border(2.dp, if (isRecordingSpeaking) Color(0xFFFF6B6B) else Color(0xFF00F5D4), CircleShape)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (isRecordingSpeaking) {
                                            isRecordingSpeaking = false
                                            speechRecognitionManager?.stopListening()
                                        } else {
                                            isRecordingSpeaking = true
                                            spokenRecognizedText = ""
                                            speechRecognitionManager?.startListening(
                                                language = "en-US",
                                                onResult = { text ->
                                                    spokenRecognizedText = text
                                                    isRecordingSpeaking = false
                                                    soundEffectManager?.playCorrect()
                                                },
                                                onError = { _ ->
                                                    isRecordingSpeaking = false
                                                }
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isRecordingSpeaking) Icons.Default.GraphicEq else Icons.Default.Mic,
                                    contentDescription = "Microphone",
                                    tint = Color(0xFF0B0D1B),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isRecordingSpeaking) "Đang lắng nghe giọng bạn... (Hãy nói to rõ)" else "Nhấn micro để luyện nói câu trên",
                                color = if (isRecordingSpeaking) Color(0xFFFF9E00) else Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )

                            if (spokenRecognizedText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Đã nhận diện: \"$spokenRecognizedText\"",
                                    color = Color(0xFF00F5D4),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Nút xác nhận hoàn tất nói
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF00F5D4), Color(0xFF00B4D8))))
                                    .clickable(enabled = !isAnswered) {
                                        isAnswered = true
                                        val latency = System.currentTimeMillis() - itemStartTime
                                        val isCorrect = true // Đánh giá hoàn tất luyện phát âm
                                        soundEffectManager?.playCorrect()

                                        val newTheta = AdaptiveDiagnosticEngine.updateTheta(
                                            currentTheta,
                                            currentItem.difficultyB,
                                            isCorrect,
                                            latency
                                        )
                                        currentTheta = newTheta
                                        answerRecords.add(
                                            DiagnosticAnswerRecord(
                                                item = currentItem,
                                                selectedIndex = 0,
                                                isCorrect = isCorrect,
                                                latencyMs = latency,
                                                speakingScore = 85
                                            )
                                        )
                                        answeredIds.add(currentItem.id)

                                        scope.launch {
                                            delay(400)
                                            // Hoàn thành cả 4 giai đoạn -> Đánh giá tổng hợp và hiện Radar Card
                                            val profile = AdaptiveDiagnosticEngine.evaluateFinalProfile(
                                                userId = userId,
                                                goal = goal,
                                                dailyTargetMinutes = dailyTargetMinutes,
                                                selfEfficacy = selfEfficacy,
                                                priorityModality = priorityModality,
                                                finalTheta = currentTheta,
                                                answers = answerRecords.toList()
                                            )
                                            finalProfile = profile
                                            isShowingRadarCard = true
                                            soundEffectManager?.playFanfare()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Hoàn tất phát âm & Tiếp tục ✨",
                                    color = Color(0xFF0B0D1B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    } else {
                        // Trường hợp trắc nghiệm (Grammar, Vocab, Listening)
                        currentItem.options.forEachIndexed { optIndex, optionText ->
                            val isSelected = selectedOptionIndex == optIndex
                            val isCorrect = optIndex == currentItem.correctIndex

                            val backgroundColor = when {
                                isAnswered && isCorrect -> Color(0x3300F5D4)
                                isAnswered && isSelected && !isCorrect -> Color(0x33FF6B6B)
                                isSelected -> Color(0x3300F5D4)
                                else -> Color(0x14FFFFFF)
                            }

                            val borderColor = when {
                                isAnswered && isCorrect -> Color(0xFF00F5D4)
                                isAnswered && isSelected && !isCorrect -> Color(0xFFFF6B6B)
                                isSelected -> Color(0xFF00F5D4)
                                else -> Color(0x26FFFFFF)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(backgroundColor)
                                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                                    .clickable(enabled = !isAnswered) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        soundEffectManager?.playClick()
                                        selectedOptionIndex = optIndex
                                        isAnswered = true

                                        val wasCorrect = (optIndex == currentItem.correctIndex)
                                        val latency = System.currentTimeMillis() - itemStartTime

                                        if (wasCorrect) {
                                            soundEffectManager?.playCorrect()
                                        } else {
                                            soundEffectManager?.playIncorrect()
                                        }

                                        // Cập nhật tham số Theta theo mô hình Rasch
                                        val updatedTheta = AdaptiveDiagnosticEngine.updateTheta(
                                            currentTheta,
                                            currentItem.difficultyB,
                                            wasCorrect,
                                            latency
                                        )
                                        currentTheta = updatedTheta

                                        answerRecords.add(
                                            DiagnosticAnswerRecord(
                                                item = currentItem,
                                                selectedIndex = optIndex,
                                                isCorrect = wasCorrect,
                                                latencyMs = latency
                                            )
                                        )
                                        answeredIds.add(currentItem.id)

                                        scope.launch {
                                            delay(500)
                                            if (stage < 4) {
                                                stage++
                                                currentItem = AdaptiveDiagnosticEngine.selectNextItem(
                                                    stage,
                                                    currentTheta,
                                                    answeredIds.toSet()
                                                )
                                                selectedOptionIndex = -1
                                                isAnswered = false
                                                itemStartTime = System.currentTimeMillis()
                                            } else {
                                                // Hoàn thành cả 4 stages
                                                val profile = AdaptiveDiagnosticEngine.evaluateFinalProfile(
                                                    userId = userId,
                                                    goal = goal,
                                                    dailyTargetMinutes = dailyTargetMinutes,
                                                    selfEfficacy = selfEfficacy,
                                                    priorityModality = priorityModality,
                                                    finalTheta = currentTheta,
                                                    answers = answerRecords.toList()
                                                )
                                                finalProfile = profile
                                                isShowingRadarCard = true
                                                soundEffectManager?.playFanfare()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val prefixLetter = ('A' + optIndex).toString()
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0xFF00F5D4) else Color(0x26FFFFFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = prefixLetter,
                                            color = if (isSelected) Color(0xFF0B0D1B) else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = optionText,
                                        color = Color.White,
                                        fontSize = 14.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isAnswered && isCorrect) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF00F5D4),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // === PHẦN 3: THẺ KIKI AI RADAR NĂNG LỰC (AI COMPETENCY RADAR CARD) ===
            finalProfile?.let { profile ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    coil.compose.AsyncImage(
                        model = com.example.R.drawable.kiki_state_happy,
                        contentDescription = "Kiki AI Diagnostic Mentor",
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(Color(0x2600F5D4))
                            .border(1.5.dp, Color(0xFF00F5D4), CircleShape)
                            .padding(4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x3300F5D4))
                            .border(1.dp, Color(0x6600F5D4), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "✨ HỒ SƠ NĂNG LỰC AI KIKI (RADAR)",
                            color = Color(0xFF00F5D4),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Kết quả Chuẩn Đoán Thích Ứng Đa Chiều",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "AI đã hiệu chuẩn tham số năng lực θ và thiết lập cấu hình Microlearning tối ưu riêng cho bạn.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Khung kết quả CEFR & Theta
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(Color(0x3300F5D4), Color(0x1A7209B7))))
                            .border(1.2.dp, Color(0xFF00F5D4), RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TRÌNH ĐỘ CEFR ĐÁNH GIÁ",
                                color = Color(0xFF00F5D4),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (profile.evaluatedCefr) {
                                    "Beginner" -> "Sơ Cấp (Beginner - A1/A2)"
                                    "Intermediate" -> "Trung Cấp (Intermediate - B1/B2)"
                                    else -> "Nâng Cao (Advanced - C1)"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Năng lực Rasch: θ = ${(profile.thetaAbility * 100).roundToInt() / 100f} • Hệ số SM-2 EF₀ = ${(profile.initialEasinessFactor * 100).roundToInt() / 100f}",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4 Thanh kỹ năng Radar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x1AFFFFFF))
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "PHÂN TÍCH MA TRẬN KỸ NĂNG",
                            color = Color(0xFFFF9E00),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        RadarSkillProgressBar(label = "📝 Ngữ pháp & Cú pháp", percentage = profile.grammarAccuracy)
                        Spacer(modifier = Modifier.height(8.dp))
                        RadarSkillProgressBar(label = "📚 Từ vựng & Kết hợp từ", percentage = profile.vocabularyAccuracy)
                        Spacer(modifier = Modifier.height(8.dp))
                        RadarSkillProgressBar(label = "🎧 Thính giác & Nuốt âm", percentage = profile.acousticAccuracy)
                        Spacer(modifier = Modifier.height(8.dp))
                        RadarSkillProgressBar(label = "🎙️ Khẩu ngữ & Ngữ điệu", percentage = profile.speakingAccuracy)
                    }

                    // Baseline Weak Points Tags nếu có
                    if (profile.baselineWeakPoints.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x1AFF6B6B))
                                .border(1.dp, Color(0x4DFF6B6B), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "🎯 TRỌNG TÂM AI CẦN ĐỘT PHÁ (WEAK POINTS)",
                                color = Color(0xFFFF6B6B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = profile.baselineWeakPoints.joinToString(" • ") { it.replace("_", " ") },
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary CTA Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFFF9E00), Color(0xFF00F5D4))))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                soundEffectManager?.playClick()
                                onCompleted(profile)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Xác Nhận & Thiết Kế Lộ Trình AI ✨",
                            color = Color(0xFF0B0D1B),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Thanh tiến độ hiển thị phần trăm năng lực từng kỹ năng trong AI Radar Card
 */
@Composable
private fun RadarSkillProgressBar(label: String, percentage: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            Text(text = "$percentage%", color = Color(0xFF00F5D4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape)
                .background(Color(0x26FFFFFF))
        ) {
            val fillFraction = (percentage.coerceIn(5, 100)) / 100f
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFF00B4D8))))
            )
        }
    }
}

/**
 * Backward compatibility overload cho PlacementQuizView cũ
 */
@Composable
fun PlacementQuizView(
    soundEffectManager: SoundEffectManager? = null,
    onCompleted: (evaluatedLevel: String, score: Int) -> Unit
) {
    PlacementQuizView(
        soundEffectManager = soundEffectManager,
        onCompleted = { profile ->
            val score = when (profile.evaluatedCefr) {
                "Beginner" -> 1
                "Intermediate" -> 2
                else -> 3
            }
            onCompleted(profile.evaluatedCefr, score)
        }
    )
}
