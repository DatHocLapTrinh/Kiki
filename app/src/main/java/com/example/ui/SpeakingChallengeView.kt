package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.core.content.ContextCompat
import com.example.audio.SpeechRecognitionManager
import com.example.util.PronunciationResult
import com.example.util.PronunciationScorer
import com.example.util.WordAccuracy

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeakingChallengeView(
    targetSentence: String,
    isEnglish: Boolean,
    speechManager: SpeechRecognitionManager,
    onSpeakSentence: (isSlow: Boolean) -> Unit,
    onSpeakWord: (String) -> Unit,
    onEvaluationComplete: (PronunciationResult) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

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
        if (!isGranted) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val isListening by speechManager.isListening
    val rmsDb by speechManager.rmsDb
    val partialText by speechManager.partialText
    val speechError by speechManager.speechError

    var evaluationResult by remember(targetSentence) { mutableStateOf<PronunciationResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tag Header: SPEAKING CHALLENGE 🎙️
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x33EC4899))
                    .border(1.dp, Color(0x66EC4899), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color(0xFFF472B6),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "SPEAKING CHALLENGE 🎙️" else "LUYỆN NÓI & PHÁT ÂM 🎙️",
                        color = Color(0xFFF472B6),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Audio Reference Controls (Normal & Turtle 🐢)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpeakSentence(false)
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x3351FAC1))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speak Normal",
                        tint = Color(0xFF51FAC1),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpeakSentence(true)
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFB703))
                ) {
                    Text("🐢", fontSize = 15.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Target Sentence Box (Clickable words for pronunciation assistance)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0x1F1A1B36))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(22.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = targetSentence,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isEnglish) "Listen to the sample, then press the mic to repeat!" else "Nghe mẫu rồi chạm vào micro để đọc lại nhé!",
                    color = Color(0x99FFFFFF),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Live Audio Waveform Visualizer
        AudioWaveformVisualizer(
            isListening = isListening,
            rmsDb = rmsDb
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Permission Card vs Microphone Control Button
        if (!hasAudioPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x22F59E0B))
                    .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isEnglish) "Microphone permission is required to analyze your speech." else "Kiki cần quyền Microphone để chấm điểm phát âm cho bạn.",
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cấp quyền Micro 🎙️", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Main Glowing Microphone Button
            MicrophoneActionButton(
                isListening = isListening,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isListening) {
                        speechManager.stopListening()
                    } else {
                        evaluationResult = null
                        speechManager.startListening(
                            language = "en-US",
                            onResult = { recognized ->
                                val res = PronunciationScorer.evaluate(targetSentence, recognized)
                                evaluationResult = res
                                onEvaluationComplete(res)
                            },
                            onError = {
                                // Error handled by speechManager.speechError
                            }
                        )
                    }
                }
            )
        }

        // Live partial recognition / error prompt
        if (isListening) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (partialText.isNotEmpty()) "\"$partialText\"" else if (isEnglish) "Listening to you speak..." else "Đang lắng nghe bạn nói...",
                color = Color(0xFF51FAC1),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        } else if (speechError != null && evaluationResult == null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = speechError ?: "",
                color = Color(0xFFFF7A7A),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        // Tùy chọn 'Không thể nói lúc này' khi ở nơi công cộng hoặc gặp sự cố micro
        if (evaluationResult == null) {
            Spacer(modifier = Modifier.height(14.dp))
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val skipped = PronunciationResult(
                        targetSentence = targetSentence,
                        spokenSentence = targetSentence,
                        overallScore = 75,
                        scoredWords = emptyList(),
                        isPassed = true,
                        feedbackMessage = if (isEnglish) "Skipped speaking for now! Practice speaking in a quiet setting." else "Đã tạm hoãn bài nói! Hãy luyện phát âm lại khi bạn ở nơi yên tĩnh nhé."
                    )
                    evaluationResult = skipped
                    onEvaluationComplete(skipped)
                }
            ) {
                Icon(
                    Icons.Default.MicOff,
                    contentDescription = null,
                    tint = Color(0x99FFFFFF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEnglish) "Can't speak right now" else "Không thể nói lúc này",
                    color = Color(0x99FFFFFF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Evaluation Result Section (ELSA Style Color-Coded Feedback)
        evaluationResult?.let { res ->
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x22111827))
                    .border(
                        1.5.dp,
                        if (res.isPassed) Color(0xFF22C55E) else Color(0xFFF59E0B),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(18.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Circular Score Gauge Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(if (res.isPassed) Color(0x3322C55E) else Color(0x33F59E0B))
                                .border(2.dp, if (res.isPassed) Color(0xFF22C55E) else Color(0xFFF59E0B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${res.overallScore}%",
                                color = if (res.isPassed) Color(0xFF51FAC1) else Color(0xFFFFD166),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (res.isPassed) "Chúc mừng! 🎉" else "Cố gắng lên! 💪",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = res.feedbackMessage,
                                color = Color(0xCCFFFFFF),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isEnglish) "Touch any word to hear its pronunciation:" else "Chạm vào từng từ để nghe lại phát âm mẫu:",
                        color = Color(0x99FFFFFF),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Color-coded interactive word chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        res.scoredWords.forEach { word ->
                            val (chipBg, chipBorder, chipText) = when (word.accuracy) {
                                WordAccuracy.EXCELLENT -> Triple(Color(0x3322C55E), Color(0xFF22C55E), Color(0xFF51FAC1))
                                WordAccuracy.GOOD -> Triple(Color(0x33F59E0B), Color(0xFFF59E0B), Color(0xFFFFD166))
                                WordAccuracy.POOR -> Triple(Color(0x33EF4444), Color(0xFFEF4444), Color(0xFFFF7A7A))
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSpeakWord(word.originalWord)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = word.originalWord,
                                        color = chipText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = chipText.copy(alpha = 0.7f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioWaveformVisualizer(
    isListening: Boolean,
    rmsDb: Float
) {
    val barCount = 7
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val phaseAnim by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400 + i * 80, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )

            val baseHeight = if (isListening) {
                val dbFactor = (rmsDb / 8f).coerceIn(0.2f, 1.2f)
                (12.dp + (26.dp * phaseAnim * dbFactor))
            } else {
                6.dp
            }

            val animatedHeight by animateDpAsState(
                targetValue = baseHeight,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "bar_height_$i"
            )

            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(animatedHeight)
                    .clip(CircleShape)
                    .background(
                        if (isListening) Brush.verticalGradient(
                            listOf(Color(0xFF51FAC1), Color(0xFF22C55E))
                        ) else Brush.verticalGradient(
                            listOf(Color(0x33FFFFFF), Color(0x33FFFFFF))
                        )
                    )
            )
        }
    }
}

@Composable
private fun MicrophoneActionButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier.size(92.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color(0x3351FAC1))
            )
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    if (isListening) Brush.radialGradient(
                        listOf(Color(0xFFEF4444), Color(0xFF991B1B))
                    ) else Brush.radialGradient(
                        listOf(Color(0xFF51FAC1), Color(0xFF059669))
                    )
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop Recording" else "Start Speaking",
                tint = if (isListening) Color.White else Color(0xFF0F172A),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
