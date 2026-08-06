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
import com.example.viewmodel.StudyViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestReviewScreen(viewModel: StudyViewModel, onBack: () -> Unit) {
    val results by viewModel.lastQuestResults.observeAsState(emptyList())
    val analysis by viewModel.questAnalysis.observeAsState()
    
    val totalQuestions = results.size
    val correctCount = results.count { it.selectedIndex == it.correctIndex }
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
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    strings.questionDetails,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            itemsIndexed(results) { index, item ->
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
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                "${strings.questionLabel}${index + 1}",
                                color = if (isCorrect) Color(0xFF51FAC1) else if (!hasAnswered) Color(0xCCFFFFFF) else Color(0xFFFF5252),
                                fontWeight = FontWeight.Bold
                            )
                            if (isCorrect) {
                                Icon(Icons.Default.Check, contentDescription = "Correct", tint = Color(0xFF51FAC1), modifier = Modifier.size(20.dp))
                            } else if (hasAnswered) {
                                Icon(Icons.Default.Close, contentDescription = "Incorrect", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
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
                        }
                    }
                }
            }
        }
    }
}
