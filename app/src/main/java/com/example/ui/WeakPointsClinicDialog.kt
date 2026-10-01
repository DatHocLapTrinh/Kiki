package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sqlite.room.WeakPointEntity
import com.example.viewmodel.StudyViewModel
import org.json.JSONArray

fun WeakPointEntity.parseOptions(): List<String> {
    return try {
        val arr = JSONArray(optionsJson)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        list
    } catch (_: Exception) {
        listOf("Option A", "Option B", "Option C", "Option D")
    }
}

val WeakPointEntity.correctAnswerText: String
    get() = parseOptions().getOrNull(correctIndex) ?: ""

@Composable
fun WeakPointsClinicDialog(
    viewModel: StudyViewModel,
    isEnglish: Boolean = false,
    onDismiss: () -> Unit
) {
    val weakPoints by viewModel.weakPoints.observeAsState(emptyList())
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Rehab Quiz, 1: Record List
    var showConfetti by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 16.dp),
            color = Color(0xFF0D0A14),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header & Tab Selector (Horizontal padding)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33FF6B6B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Healing,
                                        contentDescription = null,
                                        tint = Color(0xFFFF6B6B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isEnglish) "WEAK-POINT CLINIC" else "PHÒNG KHÁM LỖI SAI",
                                        color = Color(0xFFFF6B6B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = if (isEnglish) "${weakPoints.size} Mistakes to Review" else "${weakPoints.size} Lỗi sai cần củng cố",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tab Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x1AFFFFFF))
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedTab == 0) Color(0xFF22C55E) else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedTab = 0
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isEnglish) "⚡ Practice Quiz" else "⚡ Trắc Nghiệm Củng Cố",
                                    color = if (selectedTab == 0) Color(0xFF0F172A) else Color(0x99FFFFFF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedTab == 1) Color(0xFF22C55E) else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedTab = 1
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isEnglish) "📋 Records (${weakPoints.size})" else "📋 Hồ Sơ (${weakPoints.size})",
                                    color = if (selectedTab == 1) Color(0xFF0F172A) else Color(0x99FFFFFF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Content Area (takes weight 1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (weakPoints.isEmpty()) {
                            // Empty State: 100% Knowledge Health
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x2222C55E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(
                                        text = if (isEnglish) "100% Mastery Status!" else "Đã Nắm Vững Toàn Bộ!",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isEnglish)
                                            "You have mastered all challenged questions! No mistakes left in your review bank. Keep conquering quests!"
                                        else
                                            "Bạn đã nắm vững toàn bộ kiến thức! Không có câu hỏi nào cần ôn lại. Tiếp tục làm nhiệm vụ để tích lũy XP nhé!",
                                        color = Color(0x99FFFFFF),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        } else {
                            if (selectedTab == 0) {
                                // Rehab Quiz Mode
                                ClinicRehabQuizView(
                                    weakPoints = weakPoints,
                                    viewModel = viewModel,
                                    isEnglish = isEnglish,
                                    onHealed = {
                                        showConfetti = true
                                    }
                                )
                            } else {
                                // Records List Mode
                                ClinicRecordsListView(
                                    weakPoints = weakPoints,
                                    viewModel = viewModel,
                                    isEnglish = isEnglish,
                                    onHeal = { item ->
                                        viewModel.resolveWeakPoint(item.weakId)
                                        showConfetti = true
                                    }
                                )
                            }
                        }
                    }
                }

                // Confetti overlay on heal
                ConfettiEffect(
                    visible = showConfetti,
                    onDismiss = { showConfetti = false }
                )
            }
        }
    }
}

@Composable
private fun ClinicRehabQuizView(
    weakPoints: List<WeakPointEntity>,
    viewModel: StudyViewModel,
    isEnglish: Boolean,
    onHealed: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var currentIndex by remember { mutableIntStateOf(0) }
    val item = weakPoints.getOrNull(currentIndex) ?: weakPoints.firstOrNull()

    if (item == null) return

    val currentWeakId = item.weakId
    var selectedIndex by remember(currentWeakId) { mutableIntStateOf(-1) }
    var isSubmitted by remember(currentWeakId) { mutableStateOf(false) }
    var isCorrect by remember(currentWeakId) { mutableStateOf(false) }

    val options = remember(currentWeakId) { item.parseOptions() }
    val correctIndex = item.correctIndex
    val labels = listOf("A", "B", "C", "D")

    Column(modifier = Modifier.fillMaxSize()) {
        // Scrollable Question & Options area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Question tracker & Navigation arrows (< >)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x26FF9E00))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${if (isEnglish) "Question" else "Câu hỏi"} ${currentIndex + 1}/${weakPoints.size}",
                        color = Color(0xFFFF9E00),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (currentIndex > 0) {
                                currentIndex--
                                isSubmitted = false
                                selectedIndex = -1
                            }
                        },
                        enabled = currentIndex > 0,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous",
                            tint = if (currentIndex > 0) Color.White else Color(0x33FFFFFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (currentIndex < weakPoints.size - 1) {
                                currentIndex++
                                isSubmitted = false
                                selectedIndex = -1
                            }
                        },
                        enabled = currentIndex < weakPoints.size - 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            tint = if (currentIndex < weakPoints.size - 1) Color.White else Color(0x33FFFFFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

        // Question Card with Audio & Slow 0.68x Audio
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x1FFFFFFF))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "FAILED ${item.wrongCount}x" else "ĐÃ SAI ${item.wrongCount} LẦN",
                        color = Color(0xFFFF6B6B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        // Normal TTS
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.ttsManager.speak(item.question, isSlow = false)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x3351FAC1))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Listen Normal",
                                tint = Color(0xFF51FAC1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // Slow TTS 0.68x 🐢
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.ttsManager.speak(item.question, isSlow = true)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFB703))
                        ) {
                            Text("🐢", fontSize = 16.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = item.question,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Options List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEachIndexed { index, optText ->
                val isSelected = selectedIndex == index
                val isAnswer = index == correctIndex

                val (bg, border, textCol) = when {
                    isSubmitted && isAnswer -> Triple(Color(0x3322C55E), Color(0xFF22C55E), Color(0xFF51FAC1))
                    isSubmitted && isSelected && !isAnswer -> Triple(Color(0x33EF4444), Color(0xFFEF4444), Color(0xFFFF7A7A))
                    isSelected -> Triple(Color(0x3351FAC1), Color(0xFF51FAC1), Color.White)
                    else -> Triple(Color(0x14FFFFFF), Color(0x1AFFFFFF), Color(0xCCFFFFFF))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .border(1.dp, border, RoundedCornerShape(16.dp))
                        .clickable(enabled = !isSubmitted) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedIndex = index
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x1AFFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = labels.getOrElse(index) { "$index" },
                                color = textCol,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = optText,
                            color = textCol,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    if (isSubmitted && isAnswer) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(20.dp))
                    } else if (isSubmitted && isSelected && !isAnswer) {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val safeBottomPadding = if (navBarBottom > 0.dp) navBarBottom + 16.dp else 44.dp

    // Dedicated Elevated Bottom Dock with guaranteed safe insets!
    Surface(
        color = Color(0xFF160E1E),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0x4DFF6B6B), Color(0x33FFFFFF), Color(0x4D22C55E)))),
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = safeBottomPadding)
        ) {
            if (!isSubmitted) {
                Button(
                    onClick = {
                        if (selectedIndex != -1) {
                            isSubmitted = true
                            val won = selectedIndex == correctIndex
                            isCorrect = won
                            if (won) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.soundEffectManager.playCorrect(3)
                                viewModel.resolveWeakPoint(item.weakId)
                                onHealed()
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.soundEffectManager.playIncorrect()
                            }
                        }
                    },
                    enabled = selectedIndex != -1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22C55E),
                        disabledContainerColor = Color(0x3322C55E)
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEnglish) "Check & Overcome (+25 XP)" else "Kiểm Tra & Khắc Phục (+25 XP)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isCorrect) {
                        Button(
                            onClick = {
                                isSubmitted = false
                                selectedIndex = -1
                                if (currentIndex >= weakPoints.size - 1) {
                                    currentIndex = (weakPoints.size - 2).coerceAtLeast(0)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                        ) {
                            Text(
                                text = if (isEnglish) "Mastered! Next (+25 XP) ✨" else "Đã Nắm Vững! (+25 XP) ✨",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                isSubmitted = false
                                selectedIndex = -1
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "Try Again" else "Thử Lại Ngay",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun ClinicRecordsListView(
    weakPoints: List<WeakPointEntity>,
    viewModel: StudyViewModel,
    isEnglish: Boolean,
    onHeal: (WeakPointEntity) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val safeBottomPadding = if (navBarBottom > 0.dp) navBarBottom + 24.dp else 60.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = safeBottomPadding)
    ) {
        items(weakPoints, key = { it.weakId }) { item ->
            val options = item.parseOptions()
            val correctText = options.getOrNull(item.correctIndex) ?: ""

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x1FFFFFFF))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Mistake Count: ${item.wrongCount}x" else "Số lần làm sai: ${item.wrongCount}",
                            color = Color(0xFFFF6B6B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.ttsManager.speak(item.question, isSlow = false)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak",
                                    tint = Color(0xFF51FAC1),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.ttsManager.speak(item.question, isSlow = true)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("🐢", fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.question,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isEnglish) "Correct answer: " else "Đáp án chuẩn: ",
                            color = Color(0xFF51FAC1),
                            fontSize = 13.sp
                        )
                        Text(
                            text = correctText,
                            color = Color(0xFF51FAC1),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (item.lastFailedAt.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${if (isEnglish) "Recorded at: " else "Ghi nhận lúc: "}${item.lastFailedAt}",
                            color = Color(0x80FFFFFF),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.soundEffectManager.playCorrect(2)
                                onHeal(item)
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF22C55E)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEnglish) "Mark Mastered (+25 XP)" else "Đã Nắm Vững (+25 XP)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
