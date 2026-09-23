package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.widget.Toast
import com.example.viewmodel.StudyViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestReviewScreen(viewModel: StudyViewModel, onBack: () -> Unit) {
    val results by viewModel.lastQuestResults.observeAsState(emptyList())
    val analysis by viewModel.questAnalysis.observeAsState()
    val isEnglish by viewModel.isEnglish.observeAsState(false)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    val totalQuestions = results.size
    val correctCount = results.count { it.selectedIndex == it.correctIndex }
    val wrongCount = totalQuestions - correctCount
    var filterMode by remember { mutableStateOf("ALL") } // "ALL", "MISTAKES", "CORRECT"

    val indexedResults = remember(results) { results.mapIndexed { idx, item -> idx to item } }
    val displayedList = remember(indexedResults, filterMode) {
        when (filterMode) {
            "MISTAKES" -> indexedResults.filter { it.second.selectedIndex != it.second.correctIndex }
            "CORRECT" -> indexedResults.filter { it.second.selectedIndex == it.second.correctIndex }
            else -> indexedResults
        }
    }
    val strings = LocalAppStrings.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.historyAnalysis, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0615))
            )
        },
        containerColor = Color(0xFF0B0615)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val percentage = if (totalQuestions > 0) (correctCount * 100) / totalQuestions else 0
                    val grade = when {
                        percentage >= 90 -> "S"
                        percentage >= 75 -> "A"
                        percentage >= 50 -> "B"
                        else -> "C"
                    }
                    val message = when (grade) {
                        "S" -> strings.gradeS
                        "A" -> strings.gradeA
                        "B" -> strings.gradeB
                        else -> strings.gradeC
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(message, color = Color(0xCCFFFFFF), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$correctCount / $totalQuestions", color = Color(0xFF51FAC1), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.width(12.dp))
                            if (correctCount > 0) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x3351FAC1)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("+${correctCount * 20} XP", color = Color(0xFF51FAC1), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0x3351FAC1))
                            .border(2.dp, Color(0xFF51FAC1), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(grade, color = Color(0xFF51FAC1), fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            item {
                Text(
                    strings.aiMentorInsight,
                    color = Color(0xFF51FAC1),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x0D51FAC1))
                        .border(1.dp, Color(0x3351FAC1), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    if (analysis == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF51FAC1), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(strings.analyzing, color = Color(0xCCFFFFFF))
                        }
                    } else {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF51FAC1))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.analysisComplete, color = Color(0xFF51FAC1), fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = analysis ?: "",
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        strings.questionDetails,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Filter Chips phân loại câu hỏi
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterMode == "ALL",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            filterMode = "ALL"
                        },
                        label = { Text(if (isEnglish) "All ($totalQuestions)" else "Tất cả ($totalQuestions)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF51FAC1),
                            selectedLabelColor = Color(0xFF0B0615),
                            containerColor = Color(0x1AFFFFFF),
                            labelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = filterMode == "MISTAKES",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            filterMode = "MISTAKES"
                        },
                        label = { Text(if (isEnglish) "Mistakes ($wrongCount)" else "Câu sai ($wrongCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF5252),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0x1AFFFFFF),
                            labelColor = Color(0xFFFF7A7A)
                        )
                    )
                    FilterChip(
                        selected = filterMode == "CORRECT",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            filterMode = "CORRECT"
                        },
                        label = { Text(if (isEnglish) "Correct ($correctCount)" else "Câu đúng ($correctCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF22C55E),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0x1AFFFFFF),
                            labelColor = Color(0xFF51FAC1)
                        )
                    )
                }
            }

            items(displayedList.size, key = { displayedList[it].first }) { listIdx ->
                val (originalIndex, item) = displayedList[listIdx]
                val isCorrect = item.selectedIndex == item.correctIndex
                val hasAnswered = item.selectedIndex != -1
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isCorrect) Color(0x0A51FAC1) else if (!hasAnswered) Color(0x0AFFFFFF) else Color(0x0AFF5252))
                        .border(1.dp, if (isCorrect) Color(0x3351FAC1) else if (!hasAnswered) Color(0x1AFFFFFF) else Color(0x33FF5252), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${strings.questionLabel}${originalIndex + 1}",
                                color = if (isCorrect) Color(0xFF51FAC1) else if (!hasAnswered) Color(0xCCFFFFFF) else Color(0xFFFF5252),
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Nút phát âm TTS
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.ttsManager.speak(item.question)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = Color(0xFF51FAC1), modifier = Modifier.size(18.dp))
                                }

                                // Nút lưu vào Sổ tay từ vựng
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val correctText = item.options.getOrNull(item.correctIndex) ?: ""
                                        viewModel.saveWord(
                                            word = item.question,
                                            meaning = correctText,
                                            example = "Answer: $correctText"
                                        )
                                        Toast.makeText(
                                            context,
                                            if (isEnglish) "Bookmarked to Vocabulary Vault! ⭐" else "Đã lưu câu hỏi vào Sổ tay từ vựng! ⭐",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmark", tint = Color(0xFFFFD166), modifier = Modifier.size(18.dp))
                                }

                                if (isCorrect) {
                                    Icon(Icons.Default.Check, contentDescription = "Correct", tint = Color(0xFF51FAC1), modifier = Modifier.size(20.dp))
                                } else if (hasAnswered) {
                                    Icon(Icons.Default.Close, contentDescription = "Incorrect", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        
                        Text(
                            text = item.question,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        
                        Row(verticalAlignment = Alignment.Top) {
                            Text(strings.youChose, color = Color(0x99FFFFFF), fontSize = 14.sp)
                            Text(
                                text = if (hasAnswered) item.options.getOrNull(item.selectedIndex) ?: strings.errorString else strings.skipped,
                                color = if (isCorrect) Color(0xFF51FAC1) else if (!hasAnswered) Color(0x99FFFFFF) else Color(0xFFFF5252),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (!isCorrect) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Text(strings.correctAnswer, color = Color(0x99FFFFFF), fontSize = 14.sp)
                                Text(
                                    text = item.options[item.correctIndex],
                                    color = Color(0xFF51FAC1),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Nút hỏi Kiki phân tích sâu bằng AI
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val correctText = item.options.getOrNull(item.correctIndex) ?: ""
                                    viewModel.askQuestion(
                                        context,
                                        "Giải thích chi tiết vì sao câu: \"${item.question}\" lại có đáp án đúng là \"$correctText\". Phân tích từ vựng và quy tắc ngữ pháp liên quan.",
                                        null
                                    )
                                    Toast.makeText(
                                        context,
                                        if (isEnglish) "Kiki is analyzing in the Ask AI tab!" else "Kiki đang giải thích câu này tại tab Hỏi Kiki AI!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(38.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF51FAC1)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4451FAC1))
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "Deep explain with Kiki AI" else "Hỏi Kiki AI giải thích chi tiết câu này",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
