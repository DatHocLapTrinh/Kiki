package com.example

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.QuestItem
import com.example.viewmodel.StudyViewModel

@Composable
fun QuestScreen(viewModel: StudyViewModel, onFinish: () -> Unit) {
    val context = LocalContext.current
    val questions by viewModel.questions.observeAsState(emptyList())
    val chapterTitle by viewModel.currentChapterTitle.observeAsState("English Fundamentals")

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF27E0A9))
        }
        return
    }

    var currentQuestion by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableIntStateOf(-1) }
    var mana by remember { mutableIntStateOf(1) }
    val totalQuestions = questions.size

    val infiniteTransition = rememberInfiniteTransition(label = "quest_anim")
    val floatAnim by infiniteTransition.animateFloat(initialValue = -10f, targetValue = 10f, animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse), label = "float")
    val pulseAnim by infiniteTransition.animateFloat(initialValue = 0.8f, targetValue = 1.2f, animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                val strings = LocalAppStrings.current
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0x99000000)).border(1.dp, Color(0x1AFFFFFF), CircleShape)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(mana / totalQuestions.toFloat()).clip(CircleShape).background(Brush.horizontalGradient(listOf(Color(0xFFA3E635), Color(0xFF2DD4BF)))))
                }
                Text(text = "${strings.manaLevel}$mana/$totalQuestions", color = Color(0xFF27E0A9), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.matchParentSize().scale(pulseAnim).background(Brush.linearGradient(listOf(Color(0xFF785A00), Color(0xFF006C4F))), CircleShape).blur(8.dp))
                AsyncImage(model = R.drawable.companion_mascot, contentDescription = "Companion", contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(CircleShape).border(2.dp, Color(0x33FFFFFF), CircleShape))
                Box(modifier = Modifier.align(Alignment.BottomEnd).size(16.dp).clip(CircleShape).background(Color(0xFF27E0A9)).border(2.dp, Color(0xFF0A0A12), CircleShape))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(modifier = Modifier.fillMaxWidth(0.95f).offset(y = floatAnim.dp).clip(RoundedCornerShape(40.dp)).background(Color(0x0DFFFFFF)).border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(40.dp)).padding(horizontal = 24.dp, vertical = 32.dp), contentAlignment = Alignment.TopCenter) {
            Box(modifier = Modifier.offset(y = (-48).dp).size(48.dp).clip(CircleShape).background(Color(0x1AFFFFFF)).border(1.dp, Color(0x33FFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFF27E0A9))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                val currentQ = questions[currentQuestion]
                val strings = LocalAppStrings.current
                Text(text = "${strings.questPrefix}${currentQuestion + 1}: $chapterTitle", color = Color(0xFF27E0A9), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = currentQ.question, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 36.sp, modifier = Modifier.padding(bottom = 32.dp))

                val options = currentQ.options
                val labels = listOf("A", "B", "C", "D")

                options.forEachIndexed { index, text ->
                    val isSelected = selectedOption == index
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(CircleShape).background(if (isSelected) Color(0x2600F5D4) else Color(0x0DFFFFFF)).border(width = if (isSelected) 2.dp else 1.dp, color = if (isSelected) Color(0xFF00F5D4) else Color(0x1AFFFFFF), shape = CircleShape).clickable { selectedOption = index }.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0x0DFFFFFF)).border(1.dp, Color(0x1AFFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                                Text(labels[index], color = if (isSelected) Color(0xFF00F5D4) else Color(0x66FFFFFF), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text, color = if (isSelected) Color.White else Color(0xCCFFFFFF), fontSize = 18.sp)
                        }
                        if (isSelected) { Icon(Icons.Default.Diamond, contentDescription = null, tint = Color(0xFF27E0A9)) }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth(0.8f).height(56.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFEAB308), Color(0xFF22C55E)))).clickable {
                    if (selectedOption != -1) {
                        questions[currentQuestion].selectedIndex = selectedOption
                        if (currentQuestion < totalQuestions - 1) {
                            currentQuestion++
                            mana = (mana + 1).coerceAtMost(totalQuestions)
                            selectedOption = -1
                        } else {
                            viewModel.completeCurrentLesson()
                            viewModel.analyzeQuestResults(context, questions.toList())
                            onFinish()
                        }
                    }
                }, contentAlignment = Alignment.Center) {
                    val strings = LocalAppStrings.current
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.castSpell, color = Color(0xFF1A1A2E), fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF1A1A2E))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            val strings = LocalAppStrings.current
            Row(modifier = Modifier.clickable { onFinish() }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.skipQuest, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp).width(1.dp).height(16.dp).background(Color(0x33FFFFFF)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.life, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
