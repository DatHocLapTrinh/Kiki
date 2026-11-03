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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x3300F5D4))
                    .border(1.dp, Color(0x6600F5D4), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "BÀI TEST TRÌNH ĐỘ",
                    color = Color(0xFF00F5D4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val stageSkillTitle = when (stage) {
                1 -> "Câu hỏi 1/4: Ngữ Pháp"
                2 -> "Câu hỏi 2/4: Từ Vựng"
                3 -> "Câu hỏi 3/4: Luyện Nghe"
                else -> "Câu hỏi 4/4: Phản Xạ Nói"
            }

            Text(
                text = stageSkillTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Stage Progress Bar
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
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
                    Spacer(modifier = Modifier.height(4.dp))

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
                        val currentTargetSentence = currentItem.speakingSentence
                        val evalResult = if (spokenRecognizedText.isNotBlank()) {
                            com.example.util.PronunciationScorer.evaluate(currentTargetSentence, spokenRecognizedText)
                        } else null
                        val hasSpoken = spokenRecognizedText.isNotBlank()

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
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Nghe câu phát âm mẫu", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Nút Micro phát âm
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
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
                                                    val res = com.example.util.PronunciationScorer.evaluate(currentTargetSentence, text)
                                                    if (res.overallScore >= 60) {
                                                        soundEffectManager?.playCorrect()
                                                    } else {
                                                        soundEffectManager?.playIncorrect()
                                                    }
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
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isRecordingSpeaking) "Đang lắng nghe... (Hãy nói to, rõ ràng)"
                                       else if (!hasSpoken) "Chạm micro để phát âm câu trên"
                                       else "Chạm micro nếu muốn phát âm lại",
                                color = if (isRecordingSpeaking) Color(0xFFFF9E00) else Color.White.copy(alpha = 0.8f),
                                fontSize = 12.5.sp
                            )

                            if (hasSpoken && evalResult != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                val isScorePass = evalResult.overallScore >= 60
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isScorePass) Color(0x2E00F5D4) else Color(0x2EFF6B6B))
                                        .border(1.dp, if (isScorePass) Color(0xFF00F5D4) else Color(0xFFFF6B6B), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Đã nhận diện: \"$spokenRecognizedText\" (${evalResult.overallScore}%)" +
                                               if (isScorePass) " 🎉" else " • Chưa chuẩn",
                                        color = if (isScorePass) Color(0xFF00F5D4) else Color(0xFFFF6B6B),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Nút xác nhận hoàn tất nói (chỉ mở khi đã nói)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (hasSpoken) Brush.linearGradient(listOf(Color(0xFF00F5D4), Color(0xFF00B4D8)))
                                        else Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF)))
                                    )
                                    .clickable(enabled = hasSpoken && !isAnswered) {
                                        isAnswered = true
                                        val latency = System.currentTimeMillis() - itemStartTime
                                        val finalScore = evalResult?.overallScore ?: 0
                                        val isCorrect = finalScore >= 60

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
                                                speakingScore = finalScore
                                            )
                                        )
                                        answeredIds.add(currentItem.id)

                                        scope.launch {
                                            delay(400)
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
                                    text = if (hasSpoken) "Hoàn Tất & Xem Kết Quả ➔" else "Bấm Micro Để Luyện Nói 🎙️",
                                    color = if (hasSpoken) Color(0xFF0B0D1B) else Color.White.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
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
                        model = com.example.R.drawable.kiki_mascot_head,
                        contentDescription = "Kiki AI Diagnostic Mentor",
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0x2600F5D4))
                            .border(1.5.dp, Color(0xFF00F5D4), CircleShape)
                            .padding(4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Kết Quả Đánh Giá",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Khung kết quả CEFR
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
                                text = when (profile.evaluatedCefr) {
                                    "Beginner" -> "Sơ Cấp (A1 - A2)"
                                    "Intermediate" -> "Trung Cấp (B1 - B2)"
                                    else -> "Nâng Cao (C1)"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (profile.evaluatedCefr) {
                                    "Beginner" -> "Tập trung từ vựng cốt lõi & giao tiếp nền tảng"
                                    "Intermediate" -> "Tăng tốc phản xạ & chuẩn hóa ngữ pháp"
                                    else -> "Chuyên sâu từ vựng & phản xạ tự nhiên"
                                },
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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
                            text = "KỸ NĂNG",
                            color = Color(0xFFFF9E00),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        RadarSkillProgressBar(label = "Ngữ pháp", percentage = profile.grammarAccuracy)
                        Spacer(modifier = Modifier.height(8.dp))
                        RadarSkillProgressBar(label = "Từ vựng", percentage = profile.vocabularyAccuracy)
                        Spacer(modifier = Modifier.height(8.dp))
                        RadarSkillProgressBar(label = "Nghe hiểu", percentage = profile.acousticAccuracy)
                        Spacer(modifier = Modifier.height(8.dp))
                        RadarSkillProgressBar(label = "Phát âm", percentage = profile.speakingAccuracy)
                    }

                    // Điểm cần rèn luyện gọn gàng
                    if (profile.baselineWeakPoints.isNotEmpty()) {
                        val translatedWeak = profile.baselineWeakPoints.take(3).map { tag ->
                            when (tag.replace(" ", "_").lowercase()) {
                                "conditional_type_2" -> "Câu điều kiện"
                                "basic_collocation" -> "Cụm từ thông dụng"
                                "acoustic_everyday_greeting", "acoustic_everyday_greetings" -> "Nghe giao tiếp"
                                "present_perfect" -> "Hiện tại hoàn thành"
                                "past_simple" -> "Quá khứ đơn"
                                "relative_clause" -> "Mệnh đề quan hệ"
                                "passive_voice" -> "Câu bị động"
                                "business_idiom" -> "Thành ngữ công sở"
                                "linking_words" -> "Từ nối liên từ"
                                "pronunciation_intonation" -> "Phát âm & ngữ điệu"
                                else -> tag.replace("_", " ")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Cần rèn luyện: " + translatedWeak.joinToString(" • "),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary CTA Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
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
                            text = "Tạo Lộ Trình ➔",
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
