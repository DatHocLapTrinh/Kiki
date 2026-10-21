package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DiagnosticQuestion(
    val id: Int,
    val levelTag: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

val defaultDiagnosticQuestions = listOf(
    DiagnosticQuestion(
        id = 1,
        levelTag = "CƠ BẢN (A1 - A2)",
        question = "She ______ English with Kiki every day.",
        options = listOf("practices", "practice", "practicing", "practiced"),
        correctIndex = 0,
        explanation = "Chủ ngữ số ít 'She' chia động từ thêm -s ở thì Hiện tại đơn."
    ),
    DiagnosticQuestion(
        id = 2,
        levelTag = "TRUNG CẤP (B1 - B2)",
        question = "If I ______ more free time, I would travel around the world.",
        options = listOf("have", "had", "will have", "would have"),
        correctIndex = 1,
        explanation = "Câu điều kiện loại 2 giả định trái với hiện tại: If + S + V(quá khứ đơn), S + would + V."
    ),
    DiagnosticQuestion(
        id = 3,
        levelTag = "NÂNG CAO (B2 - C1)",
        question = "The scientists made a ______ breakthrough in artificial intelligence.",
        options = listOf("groundbreaking", "rough", "heavy", "tough"),
        correctIndex = 0,
        explanation = "Collocation tiếng Anh học thuật chuẩn xác: 'groundbreaking breakthrough' (đột phá mang tính cách mạng)."
    )
)

@Composable
fun PlacementQuizView(
    soundEffectManager: SoundEffectManager? = null,
    onCompleted: (evaluatedLevel: String, score: Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }
    var isAnswered by remember { mutableStateOf(false) }

    val currentQuestion = defaultDiagnosticQuestions.getOrNull(currentQuestionIndex)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. Header Badge: "✨ ĐÁNH GIÁ NĂNG LỰC AI"
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
                text = strings.placementQuizTitle,
                color = Color(0xFF00F5D4),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = strings.placementQuizSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Progress Indicator (3 câu)
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            defaultDiagnosticQuestions.forEachIndexed { index, _ ->
                val isActive = index == currentQuestionIndex
                val isDone = index < currentQuestionIndex
                val pillColor = when {
                    isDone -> Color(0xFF00F5D4)
                    isActive -> Color(0xFFFF9E00)
                    else -> Color(0x33FFFFFF)
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(6.dp)
                        .width(if (isActive) 28.dp else 14.dp)
                        .clip(CircleShape)
                        .background(pillColor)
                )
            }
        }

        // 3. Question Card with smooth animation
        AnimatedContent(
            targetState = currentQuestionIndex,
            transitionSpec = {
                (slideInHorizontally(tween(300)) { width -> width } + fadeIn(tween(300))) togetherWith
                (slideOutHorizontally(tween(300)) { width -> -width } + fadeOut(tween(300)))
            },
            label = "diagnostic_quiz_question"
        ) { qIndex ->
            val question = defaultDiagnosticQuestions.getOrNull(qIndex)
            if (question != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Level tag badge
                    Text(
                        text = "${strings.placementQuestionCount.replace("{current}", (qIndex + 1).toString())} • ${question.levelTag}",
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
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = question.question,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 17.sp,
                            lineHeight = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 4 Options
                    question.options.forEachIndexed { optIndex, optionText ->
                        val isSelected = selectedOptionIndex == optIndex
                        val isCorrect = optIndex == question.correctIndex

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

                                    val wasCorrect = (optIndex == question.correctIndex)
                                    if (wasCorrect) {
                                        score++
                                        soundEffectManager?.playCorrect()
                                    } else {
                                        soundEffectManager?.playIncorrect()
                                    }

                                    scope.launch {
                                        delay(450)
                                        if (currentQuestionIndex < defaultDiagnosticQuestions.size - 1) {
                                            currentQuestionIndex++
                                            selectedOptionIndex = -1
                                            isAnswered = false
                                        } else {
                                            // Finish Diagnostic Quiz: Calculate Level
                                            val finalLevel = when (score) {
                                                0, 1 -> "Beginner"
                                                2 -> "Intermediate"
                                                else -> "Advanced"
                                            }
                                            soundEffectManager?.playFanfare()
                                            onCompleted(finalLevel, score)
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
                                    fontSize = 15.sp,
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
    }
}
