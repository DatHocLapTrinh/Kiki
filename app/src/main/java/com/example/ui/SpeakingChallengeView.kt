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

        Spacer(modifier = Modifier.height(14.dp))

        // Intonation & Pitch Contour Visual Guide
        IntonationContourGuide(
            sentence = targetSentence,
            isEnglish = isEnglish
        )

        Spacer(modifier = Modifier.height(14.dp))

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
private fun IntonationContourGuide(
    sentence: String,
    isEnglish: Boolean
) {
    val isQuestion = sentence.trim().endsWith("?")
    val words = remember(sentence) { sentence.split("\\s+".toRegex()).filter { it.isNotBlank() } }

    val functionWords = remember {
        setOf("a", "an", "the", "in", "on", "at", "to", "for", "of", "with", "is", "am", "are", "was", "were", "it", "and", "or", "but")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1451FAC1))
            .border(1.dp, Color(0x3351FAC1), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF51FAC1),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "INTONATION & PITCH CONTOUR" else "NGỮ ĐIỆU & CAO ĐỘ (PITCH CONTOUR)",
                        color = Color(0xFF51FAC1),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = if (isQuestion) (if (isEnglish) "Rising Tone ↗" else "Giọng lên cuối ↗") else (if (isEnglish) "Falling Tone ↘" else "Giọng hạ cuối ↘"),
                    color = if (isQuestion) Color(0xFFFFD166) else Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                val width = size.width
                val height = size.height
                if (words.isEmpty()) return@Canvas

                val step = width / (words.size.coerceAtLeast(1) + 1)
                val path = androidx.compose.ui.graphics.Path()

                val points = words.mapIndexed { index, word ->
                    val cleanWord = word.lowercase().filter { it.isLetter() }
                    val isStressed = !functionWords.contains(cleanWord) && cleanWord.length >= 3
                    val x = step * (index + 1)
                    val y = when {
                        index == words.size - 1 -> if (isQuestion) height * 0.22f else height * 0.82f
                        isStressed -> height * 0.25f
                        else -> height * 0.65f
                    }
                    androidx.compose.ui.geometry.Offset(x, y)
                }

                if (points.isNotEmpty()) {
                    path.moveTo(0f, points.first().y)
                    path.lineTo(points.first().x, points.first().y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }

                    val lastP = points.last()
                    path.lineTo(width, lastP.y)

                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF51FAC1), Color(0xFFFFD166), Color(0xFFEC4899))
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.5.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )

                    points.forEachIndexed { i, pt ->
                        val clean = words[i].lowercase().filter { it.isLetter() }
                        val isStressed = !functionWords.contains(clean) && clean.length >= 3
                        val nodeColor = if (isStressed) Color(0xFFFFD166) else Color(0xFF51FAC1)
                        drawCircle(
                            color = nodeColor,
                            radius = if (isStressed) 4.dp.toPx() else 2.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isEnglish) "Stress content words higher, then gently taper off." else "Nhấn giọng cao ở các từ quan trọng (chấm vàng) và hạ giọng tự nhiên.",
                color = Color(0x99FFFFFF),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AudioWaveformVisualizer(
    isListening: Boolean,
    rmsDb: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val dbNormalized = (rmsDb / 10f).coerceIn(0.15f, 1.2f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1F0B0E14))
            .border(1.dp, if (isListening) Color(0x6651FAC1) else Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            val width = size.width
            val height = size.height
            val midY = height / 2f

            if (!isListening) {
                // Ambient resting wave
                val restingPath = androidx.compose.ui.graphics.Path()
                restingPath.moveTo(0f, midY)
                val bars = 24
                val barSpacing = width / bars
                for (i in 0..bars) {
                    val x = i * barSpacing
                    val y = midY + kotlin.math.sin(x * 0.05f + phase).toFloat() * 2.5.dp.toPx()
                    restingPath.lineTo(x, y)
                }
                drawPath(
                    path = restingPath,
                    color = Color(0x44FFFFFF),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            } else {
                // Multi-frequency Dynamic Neon Audio Spectrum
                val barsCount = 24
                val barWidth = 4.dp.toPx()
                val totalBarsWidth = barsCount * barWidth
                val availableSpace = width - totalBarsWidth
                val spacing = availableSpace / (barsCount - 1).coerceAtLeast(1)

                for (i in 0 until barsCount) {
                    val normalizedIndex = i.toFloat() / barsCount
                    val waveMod = kotlin.math.abs(
                        kotlin.math.sin(normalizedIndex * Math.PI * 3 + phase).toFloat() * 0.6f +
                        kotlin.math.cos(normalizedIndex * Math.PI * 2 - phase * 0.7f).toFloat() * 0.4f
                    )
                    val barHeight = (height * 0.15f + height * 0.75f * waveMod * dbNormalized).coerceIn(4.dp.toPx(), height * 0.9f)
                    val x = i * (barWidth + spacing)
                    val topY = midY - barHeight / 2f

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFFEC4899),
                                Color(0xFF51FAC1),
                                Color(0xFF22C55E)
                            ),
                            startY = topY,
                            endY = topY + barHeight
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(x, topY),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
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
