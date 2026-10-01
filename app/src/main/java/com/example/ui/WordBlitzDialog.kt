package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.viewmodel.StudyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class BlitzItem(
    val word: String,
    val meaning: String,
    val distractors: List<String>
)

private val DEFAULT_BLITZ_POOL = listOf(
    BlitzItem("Achieve", "Đạt được", listOf("Từ bỏ", "Thất bại", "Trì hoãn")),
    BlitzItem("Resilient", "Kiên cường", listOf("Yếu đuối", "Mong manh", "Nóng vội")),
    BlitzItem("Challenge", "Thử thách", listOf("An toàn", "Nghỉ ngơi", "Bỏ cuộc")),
    BlitzItem("Brilliant", "Xuất sắc", listOf("Tầm thường", "U ám", "Chậm chạp")),
    BlitzItem("Curious", "Tò mò, ham học hỏi", listOf("Thờ ơ", "Nhút nhát", "Lười biếng")),
    BlitzItem("Inspire", "Truyền cảm hứng", listOf("Ngăn cản", "Làm nản lòng", "Phán xét")),
    BlitzItem("Knowledge", "Kiến thức", listOf("Sự ngờ vực", "Hoang mang", "Sơ suất")),
    BlitzItem("Opportunity", "Cơ hội", listOf("Rào cản", "Nguy hiểm", "Khó khăn")),
    BlitzItem("Focus", "Tập trung", listOf("Xao nhãng", "Bất cẩn", "Ngủ gật")),
    BlitzItem("Confidence", "Sự tự tin", listOf("Tự ti", "Lo âu", "Hoài nghi")),
    BlitzItem("Patience", "Sự kiên nhẫn", listOf("Vội vã", "Nóng giận", "Mất kiên nhẫn")),
    BlitzItem("Discover", "Khám phá", listOf("Che giấu", "Lãng quên", "Bỏ qua")),
    BlitzItem("Effort", "Sự nỗ lực", listOf("Thụ động", "Buông xuôi", "Thờ ơ")),
    BlitzItem("Journey", "Hành trình", listOf("Điểm dừng", "Bế tắc", "Lạc lối")),
    BlitzItem("Practice", "Luyện tập", listOf("Bỏ bê", "Ngưng trệ", "Lý thuyết suông")),
    BlitzItem("Gratitude", "Lòng biết ơn", listOf("Vô ơn", "Oán trách", "Đố kỵ")),
    BlitzItem("Generous", "Hào phóng", listOf("Keo kiệt", "Ích kỷ", "Tính toán")),
    BlitzItem("Wisdom", "Trí tuệ", listOf("Nông cạn", "Mù quáng", "Hấp tấp")),
    BlitzItem("Courage", "Lòng dũng cảm", listOf("Hèn nhát", "Sợ hãi", "Do dự")),
    BlitzItem("Success", "Thành công", listOf("Thất bại", "Bế tắc", "Tuyệt vọng"))
)

private enum class BlitzState {
    READY,
    PLAYING,
    GAME_OVER
}

@Composable
fun WordBlitzDialog(
    viewModel: StudyViewModel,
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val vocabularyList by viewModel.vocabularyList.observeAsState(emptyList())

    // Game variables
    var gameState by remember { mutableStateOf(BlitzState.READY) }
    var timeLeft by remember { mutableIntStateOf(60) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var totalQuestions by remember { mutableIntStateOf(0) }
    var showTimeBonus by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }

    // Prepare quiz question
    var currentItemIndex by remember { mutableIntStateOf(0) }
    var currentOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }

    // Combine pool: user saved words + default pool
    val blitzPool = remember(vocabularyList) {
        val userItems = vocabularyList.filter { it.word.isNotBlank() && it.meaning.isNotBlank() }.map { item ->
            val otherMeanings = vocabularyList.filter { it.vocabId != item.vocabId }.map { it.meaning }
            val fallbackDistractors = listOf("Ngẫu nhiên", "Khác biệt", "Chưa xác định")
            val distractors = (otherMeanings + fallbackDistractors).take(3)
            BlitzItem(item.word, item.meaning, distractors)
        }
        if (userItems.size >= 5) (userItems + DEFAULT_BLITZ_POOL).shuffled() else DEFAULT_BLITZ_POOL.shuffled()
    }

    val currentBlitzItem = blitzPool.getOrNull(currentItemIndex % blitzPool.size) ?: DEFAULT_BLITZ_POOL.first()

    fun loadNextQuestion() {
        val nextItem = blitzPool[(currentItemIndex + 1) % blitzPool.size]
        val options = (listOf(nextItem.meaning) + nextItem.distractors.take(3)).shuffled()
        currentItemIndex++
        currentOptions = options
        selectedOption = null
        isAnswerCorrect = null
    }

    fun startGame() {
        gameState = BlitzState.PLAYING
        timeLeft = 60
        score = 0
        combo = 0
        maxCombo = 0
        correctCount = 0
        totalQuestions = 0
        currentItemIndex = 0
        val firstItem = blitzPool.first()
        currentOptions = (listOf(firstItem.meaning) + firstItem.distractors.take(3)).shuffled()
        selectedOption = null
        isAnswerCorrect = null
        showConfetti = false
    }

    // 1-second countdown timer loop
    LaunchedEffect(gameState) {
        if (gameState == BlitzState.PLAYING) {
            while (timeLeft > 0 && gameState == BlitzState.PLAYING) {
                delay(1000)
                timeLeft--
            }
            if (timeLeft <= 0 && gameState == BlitzState.PLAYING) {
                gameState = BlitzState.GAME_OVER
                val earnedXp = (score / 12).coerceIn(25, 120)
                viewModel.addXp(earnedXp)
                viewModel.soundEffectManager.playFanfare()
                showConfetti = true
            }
        }
    }

    // Floating +2s bonus anim reset
    LaunchedEffect(showTimeBonus) {
        if (showTimeBonus) {
            delay(800)
            showTimeBonus = false
        }
    }

    Dialog(
        onDismissRequest = {
            if (gameState != BlitzState.PLAYING) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE605030A))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                color = Color(0xFF0F0C1E),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFF51FAC1), Color(0xFFFFD166), Color(0xFFEC4899))))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header bar: Title & Exit
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33FFD166)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isEnglish) "WORD BLITZ ⚡" else "THỬ THÁCH 60S ⚡",
                                        color = Color(0xFFFFD166),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = if (isEnglish) "Speed Vocabulary" else "Tốc Độ & Phản Xạ",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0x99FFFFFF))
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        when (gameState) {
                            BlitzState.READY -> {
                                // Ready Screen: Rules & Start Button
                                ReadyScreenView(
                                    isEnglish = isEnglish,
                                    onStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        startGame()
                                    }
                                )
                            }
                            BlitzState.PLAYING -> {
                                // Live Playing View
                                PlayingScreenView(
                                    item = currentBlitzItem,
                                    options = currentOptions,
                                    timeLeft = timeLeft,
                                    score = score,
                                    combo = combo,
                                    showTimeBonus = showTimeBonus,
                                    selectedOption = selectedOption,
                                    isAnswerCorrect = isAnswerCorrect,
                                    isEnglish = isEnglish,
                                    onSelectOption = { option ->
                                        if (selectedOption != null) return@PlayingScreenView
                                        selectedOption = option
                                        totalQuestions++
                                        val isCorrect = option == currentBlitzItem.meaning
                                        isAnswerCorrect = isCorrect

                                        if (isCorrect) {
                                            correctCount++
                                            combo++
                                            if (combo > maxCombo) maxCombo = combo
                                            val multiplier = when {
                                                combo >= 10 -> 3.0f
                                                combo >= 6 -> 2.0f
                                                combo >= 3 -> 1.5f
                                                else -> 1.0f
                                            }
                                            score += (10 * multiplier).toInt()
                                            timeLeft = (timeLeft + 2).coerceAtMost(90)
                                            showTimeBonus = true
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.soundEffectManager.playCorrect(combo)
                                        } else {
                                            combo = 0
                                            timeLeft = (timeLeft - 2).coerceAtLeast(0)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.soundEffectManager.playIncorrect()
                                        }

                                        // Fast advance to next question
                                        coroutineScope.launch {
                                            delay(260)
                                            loadNextQuestion()
                                        }
                                    }
                                )
                            }
                            BlitzState.GAME_OVER -> {
                                // Results Summary Screen
                                GameOverScreenView(
                                    score = score,
                                    correctCount = correctCount,
                                    totalCount = totalQuestions,
                                    maxCombo = maxCombo,
                                    isEnglish = isEnglish,
                                    onPlayAgain = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        startGame()
                                    },
                                    onFinish = onDismiss
                                )
                            }
                        }
                    }

                    if (showConfetti) {
                        ConfettiEffect(
                            visible = true,
                            durationMs = 2800,
                            onDismiss = { showConfetti = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyScreenView(
    isEnglish: Boolean,
    onStart: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF51FAC1), Color(0xFF10B981))))
                .border(2.dp, Color(0xFF51FAC1), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(46.dp))
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = if (isEnglish) "60-Second Vocabulary Sprint!" else "Chạy Đua Từ Vựng 60 Giây!",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (isEnglish)
                "⚡ Correct answers give +2 seconds & boost your Combo multiplier!\n❌ Wrong answers deduct 2 seconds and reset streak."
            else
                "⚡ Trả lời đúng được cộng +2 giây & nhân hệ số Combo!\n❌ Trả lời sai trừ 2 giây và đặt lại chuỗi Combo.",
            color = Color(0xCCFFFFFF),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(26.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF51FAC1),
                contentColor = Color(0xFF0A0714)
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isEnglish) "START BLITZ NOW! ⚡" else "BẮT ĐẦU NGAY! ⚡", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun PlayingScreenView(
    item: BlitzItem,
    options: List<String>,
    timeLeft: Int,
    score: Int,
    combo: Int,
    showTimeBonus: Boolean,
    selectedOption: String?,
    isAnswerCorrect: Boolean?,
    isEnglish: Boolean,
    onSelectOption: (String) -> Unit
) {
    val isHurry = timeLeft <= 10
    val timerColor by animateColorAsState(
        targetValue = if (isHurry) Color(0xFFFF5252) else Color(0xFF51FAC1),
        label = "timer_color"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Row: Timer Ring, Score, Combo Multiplier
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timer Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(timerColor.copy(alpha = 0.2f))
                    .border(1.dp, timerColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = timerColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${timeLeft}s",
                        color = timerColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    if (showTimeBonus) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+2s", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Combo Multiplier
            if (combo >= 2) {
                val comboText = when {
                    combo >= 10 -> "3x ULTRA 💥"
                    combo >= 6 -> "2x SUPER ⚡"
                    else -> "${combo}x STREAK 🔥"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FFD166))
                        .border(1.dp, Color(0xFFFFD166), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(comboText, color = Color(0xFFFFD166), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // Score Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x2238BDF8))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("${score} PTS", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Target Word Display Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1E1738), Color(0xFF16112C))))
                .border(1.dp, Color(0x3351FAC1), RoundedCornerShape(20.dp))
                .padding(vertical = 22.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.word,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isEnglish) "Choose the correct meaning:" else "Chọn nghĩa đúng nhất:",
                    color = Color(0x99FFFFFF),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4 Fast Multiple Choice Options
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                val isCorrect = option == item.meaning

                val (btnBg, btnBorder, btnText) = when {
                    selectedOption == null -> Triple(Color(0x1AFFFFFF), Color(0x33FFFFFF), Color.White)
                    isSelected && isCorrect -> Triple(Color(0x3322C55E), Color(0xFF22C55E), Color(0xFF51FAC1))
                    isSelected && !isCorrect -> Triple(Color(0x33EF4444), Color(0xFFEF4444), Color(0xFFFF7A7A))
                    isCorrect -> Triple(Color(0x3322C55E), Color(0xFF22C55E), Color(0xFF51FAC1))
                    else -> Triple(Color(0x0FFFFFFF), Color(0x1AFFFFFF), Color(0x66FFFFFF))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(btnBg)
                        .border(1.dp, btnBorder, RoundedCornerShape(14.dp))
                        .clickable(enabled = selectedOption == null) {
                            onSelectOption(option)
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = btnText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun GameOverScreenView(
    score: Int,
    correctCount: Int,
    totalCount: Int,
    maxCombo: Int,
    isEnglish: Boolean,
    onPlayAgain: () -> Unit,
    onFinish: () -> Unit
) {
    val xpEarned = (score / 12).coerceIn(25, 120)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFFFD166), Color(0xFFD97706))))
                .border(2.dp, Color(0xFFFFD166), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(42.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isEnglish) "BLITZ COMPLETE! 🏆" else "HOÀN THÀNH THỬ THÁCH! 🏆",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "+$xpEarned XP REWARDED",
            color = Color(0xFF51FAC1),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatBox(
                title = if (isEnglish) "Score" else "Điểm số",
                value = "$score",
                color = Color(0xFFFFD166),
                modifier = Modifier.weight(1f)
            )
            StatBox(
                title = if (isEnglish) "Correct" else "Chính xác",
                value = "$correctCount / $totalCount",
                color = Color(0xFF51FAC1),
                modifier = Modifier.weight(1f)
            )
            StatBox(
                title = if (isEnglish) "Max Combo" else "Combo Max",
                value = "${maxCombo}x",
                color = Color(0xFFEC4899),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions: Play Again vs Finish
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPlayAgain,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Color(0xFF51FAC1)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF51FAC1))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isEnglish) "Play Again" else "Chơi lại", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onFinish,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1), contentColor = Color(0xFF0F0C1E))
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isEnglish) "Done" else "Xong", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, color = Color(0x99FFFFFF), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
