package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.model.QuestItem
import com.example.viewmodel.StudyViewModel

@Composable
fun QuestScreen(viewModel: StudyViewModel, onFinish: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val strings = LocalAppStrings.current
    val isEnglish by viewModel.isEnglish.observeAsState(false)
    val questions by viewModel.questions.observeAsState(emptyList())
    val chapterTitle by viewModel.currentChapterTitle.observeAsState("English Fundamentals")
    val isSpeaking by viewModel.ttsManager.isSpeaking

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF27E0A9))
        }
        return
    }

    var currentQuestion by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableIntStateOf(-1) }
    var isChecked by remember { mutableStateOf(false) }
    var isCurrentCorrect by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var mascotSpeech by remember { mutableStateOf<String?>(null) }
    var isSavedToVault by remember(currentQuestion) { mutableStateOf(false) }

    // Map lưu câu trả lời bất biến của người dùng
    val userAnswers = remember { mutableStateMapOf<Int, Int>() }
    val totalQuestions = questions.size

    val infiniteTransition = rememberInfiniteTransition(label = "quest_anim")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "float"
    )
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Thanh tiến độ & Linh vật
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nút đóng / thoát bài học
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showExitDialog = true
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x26FFFFFF))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val progressRatio = (currentQuestion + 1).toFloat() / totalQuestions
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressRatio)
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(Color(0xFFA3E635), Color(0xFF2DD4BF))))
                        )
                    }
                    Text(
                        text = "${strings.manaLevel}${currentQuestion + 1}/$totalQuestions",
                        color = Color(0xFF27E0A9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Avatar linh vật Kiki tương tác chạm
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val quotes = if (isEnglish) listOf(
                                "You've got this! Step by step to English fluency!",
                                "Mistakes are proof that you're learning! Keep going!",
                                "Every question makes your mind sharper!",
                                "Kiki believes in your superpower!"
                            ) else listOf(
                                "Cố lên bạn ơi! Từng bước một sẽ nói tiếng Anh lưu loát!",
                                "Lỗi sai là bước đệm để thành công! Tiếp tục nhé!",
                                "Mỗi câu hỏi giúp phản xạ tiếng Anh của bạn nhạy bén hơn!",
                                "Kiki luôn đồng hành và cổ vũ bạn hết mình!"
                            )
                            val q = quotes.random()
                            mascotSpeech = q
                            viewModel.ttsManager.speak(q)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .scale(pulseAnim)
                            .background(Brush.linearGradient(listOf(Color(0xFF785A00), Color(0xFF006C4F))), CircleShape)
                            .blur(8.dp)
                    )
                    AsyncImage(
                        model = R.drawable.companion_mascot,
                        contentDescription = "Companion",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0x33FFFFFF), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF27E0A9))
                            .border(2.dp, Color(0xFF0A0A12), CircleShape)
                    )
                }
            }

            // Bong bóng thoại động viên của linh vật Kiki
            AnimatedVisibility(
                visible = mascotSpeech != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                mascotSpeech?.let { speech ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x2251FAC1))
                            .border(1.dp, Color(0x6651FAC1), RoundedCornerShape(16.dp))
                            .clickable { mascotSpeech = null }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kiki: \"$speech\"",
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0x88FFFFFF), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Card câu hỏi chính
            AnimatedContent(
                targetState = currentQuestion,
                transitionSpec = {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                },
                label = "question_transition"
            ) { qIdx ->
                val currentQ = questions[qIdx]
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .offset(y = floatAnim.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(32.dp))
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "${strings.questPrefix}${qIdx + 1}: $chapterTitle",
                                color = Color(0xFF27E0A9),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Nút nghe phát âm tiếng Anh
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.ttsManager.speak(currentQ.question)
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x3351FAC1))
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak Question",
                                    tint = if (isSpeaking) Color(0xFF51FAC1) else Color(0xFFFFD166),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = currentQ.question,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        val options = currentQ.options
                        val labels = listOf("A", "B", "C", "D")

                        options.forEachIndexed { index, text ->
                            val isSelected = selectedOption == index
                            val isCorrectAnswer = index == currentQ.correctIndex

                            // Màu sắc hiển thị sau khi bấm Kiểm tra
                            val (optionBg, optionBorder, optionTextColor) = when {
                                isChecked && isCorrectAnswer -> Triple(Color(0x3322C55E), Color(0xFF22C55E), Color(0xFF51FAC1))
                                isChecked && isSelected && !isCorrectAnswer -> Triple(Color(0x33EF4444), Color(0xFFEF4444), Color(0xFFFF7A7A))
                                isSelected -> Triple(Color(0x2600F5D4), Color(0xFF00F5D4), Color.White)
                                else -> Triple(Color(0x0DFFFFFF), Color(0x1AFFFFFF), Color(0xCCFFFFFF))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clip(CircleShape)
                                    .background(optionBg)
                                    .border(width = if (isSelected || (isChecked && isCorrectAnswer)) 2.dp else 1.dp, color = optionBorder, shape = CircleShape)
                                    .clickable(enabled = !isChecked) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedOption = index
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x0DFFFFFF))
                                            .border(1.dp, optionBorder, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(labels[index], color = optionTextColor, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(text, color = optionTextColor, fontSize = 16.sp)
                                }

                                if (isChecked && isCorrectAnswer) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(22.dp))
                                } else if (isChecked && isSelected && !isCorrectAnswer) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                                } else if (isSelected) {
                                    Icon(Icons.Default.Diamond, contentDescription = null, tint = Color(0xFF27E0A9), modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Nút hành động: KIỂM TRA (khi chưa check)
                        if (!isChecked) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedOption != -1) Brush.linearGradient(listOf(Color(0xFFEAB308), Color(0xFF22C55E)))
                                        else Brush.linearGradient(listOf(Color(0x44EAB308), Color(0x4422C55E)))
                                    )
                                    .clickable(enabled = selectedOption != -1) {
                                        isChecked = true
                                        val isCorrect = selectedOption == currentQ.correctIndex
                                        isCurrentCorrect = isCorrect
                                        if (isCorrect) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.soundEffectManager.playCorrect()
                                        } else {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.soundEffectManager.playIncorrect()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        strings.checkAnswer,
                                        color = if (selectedOption != -1) Color(0xFF1A1A2E) else Color(0x661A1A2E),
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = if (selectedOption != -1) Color(0xFF1A1A2E) else Color(0x661A1A2E)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer bỏ qua
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable { showExitDialog = true }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = Color(0x99FFFFFF), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.skipQuest, color = Color(0x99FFFFFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Instant Feedback Bottom Sheet / Banner khi vừa kiểm tra đáp án
        AnimatedVisibility(
            visible = isChecked,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val currentQ = questions[currentQuestion]
            val bannerBg = if (isCurrentCorrect) Color(0xF00D3320) else Color(0xF0401015)
            val bannerBorder = if (isCurrentCorrect) Color(0xFF22C55E) else Color(0xFFEF4444)
            val iconTint = if (isCurrentCorrect) Color(0xFF51FAC1) else Color(0xFFFF7A7A)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(bannerBg)
                    .border(1.5.dp, bannerBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCurrentCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isCurrentCorrect) strings.correctNotice else strings.incorrectNotice,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                if (!isCurrentCorrect) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val correctText = currentQ.options.getOrNull(currentQ.correctIndex) ?: ""
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${strings.correctAnswerNotice} $correctText",
                            color = Color(0xFFFFD166),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.ttsManager.speak(correctText)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = Color(0xFFFFD166), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Nút lưu vào Sổ tay từ vựng
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            if (!isSavedToVault) {
                                isSavedToVault = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val correctText = currentQ.options.getOrNull(currentQ.correctIndex) ?: ""
                                viewModel.saveWord(
                                    word = currentQ.question,
                                    meaning = correctText,
                                    example = "Answer: $correctText"
                                )
                                Toast.makeText(
                                    context,
                                    if (isEnglish) "Saved to Vocabulary Vault! ⭐" else "Đã lưu vào Sổ tay từ vựng! ⭐",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSavedToVault) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                            contentDescription = "Save to vault",
                            tint = Color(0xFF51FAC1),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSavedToVault) (if (isEnglish) "Saved ⭐" else "Đã lưu ⭐") else (if (isEnglish) "Bookmark question" else "Lưu vào sổ tay"),
                            color = Color(0xFF51FAC1),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Nút TIẾP TỤC
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        userAnswers[currentQuestion] = selectedOption
                        if (currentQuestion < totalQuestions - 1) {
                            currentQuestion++
                            selectedOption = -1
                            isChecked = false
                        } else {
                            // Cập nhật câu trả lời đã chọn vào danh sách kết quả bất biến
                            val finalResults = questions.mapIndexed { idx, item ->
                                val copy = QuestItem(item.question, item.options, item.correctIndex)
                                copy.selectedIndex = userAnswers[idx] ?: -1
                                copy
                            }
                            viewModel.completeCurrentLesson()
                            viewModel.analyzeQuestResults(context, finalResults)
                            onFinish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentCorrect) Color(0xFF22C55E) else Color(0xFFEF4444),
                        contentColor = Color.White
                    )
                ) {
                    Text(strings.continueLesson, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // Hộp thoại xác nhận thoát bài học
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                containerColor = Color(0xFF1A1A2E),
                title = {
                    Text(strings.pauseLessonTitle, color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(strings.pauseLessonDesc, color = Color(0xCCFFFFFF), fontSize = 14.sp)
                },
                confirmButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text(strings.resumeLesson, color = Color(0xFF51FAC1), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            onFinish()
                        }
                    ) {
                        Text(strings.exitLesson, color = Color(0xFFFF6B6B))
                    }
                }
            )
        }
    }
}
