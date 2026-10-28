package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.ui.WeakPointsClinicDialog
import com.example.viewmodel.StudyViewModel

data class QuestData(
    val taskId: Long,
    val title: String,
    val desc: String,
    val current: Int,
    val total: Int,
    val reward: String,
    val icon: ImageVector,
    val color: Color,
    val isCompleted: Boolean = current >= total,
    val isClaimed: Boolean = false
)

private data class QuestMeta(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun QuestsScreen(viewModel: StudyViewModel) {
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val strings = LocalAppStrings.current
    val isEnglish by viewModel.isEnglish.observeAsState(false)
    val currentUserId by viewModel.currentUserId.observeAsState(-1L)
    val dailyTasks by viewModel.dailyTasks.observeAsState(emptyList())
    val chestOpened by viewModel.isDailyChestOpened.observeAsState(false)

    LaunchedEffect(currentUserId) {
        if (currentUserId != -1L) viewModel.refreshDailyTasks()
    }

    val quests = dailyTasks.map { task ->
        val meta = when (task.taskType) {
            "LESSON_COMPLETE" -> QuestMeta(strings.quest1Title, strings.quest1Desc, Icons.Default.AutoStories, Color(0xFF51FAC1))
            "LESSON_COMPLETE_2" -> QuestMeta(strings.quest4Title, strings.quest4Desc, Icons.Default.School, Color(0xFF64B5F6))
            "ANSWER_QUESTIONS" -> QuestMeta(strings.quest3Title, strings.quest3Desc, Icons.AutoMirrored.Filled.MenuBook, Color(0xFF27E0A9))
            "ANSWER_QUESTIONS_20" -> QuestMeta(strings.quest5Title, strings.quest5Desc, Icons.Default.Psychology, Color(0xFFFFB74D))
            "PERFECT_SCORE" -> QuestMeta(strings.quest2Title, strings.quest2Desc, Icons.Default.Star, Color(0xFFFFD166))
            "ASK_AI_TUTOR" -> QuestMeta(strings.quest6Title, strings.quest6Desc, Icons.Default.ChatBubble, Color(0xFFBA68C8))
            else -> QuestMeta(task.title, strings.quest3Desc, Icons.AutoMirrored.Filled.MenuBook, Color(0xFF27E0A9))
        }
        // Phần thưởng sao: 15 + 20 + 15 + 20 + 15 + 15 = 100 ⭐ / ngày
        val starReward = when (task.taskType) {
            "LESSON_COMPLETE" -> 15
            "LESSON_COMPLETE_2" -> 20
            "ANSWER_QUESTIONS" -> 15
            "ANSWER_QUESTIONS_20" -> 20
            "PERFECT_SCORE" -> 15
            "ASK_AI_TUTOR" -> 15
            else -> 15
        }
        QuestData(
            taskId = task.taskId,
            title = meta.title,
            desc = meta.desc,
            current = task.currentValue.coerceAtMost(task.targetValue),
            total = task.targetValue,
            reward = "${task.rewardXp} XP + ⭐$starReward",
            icon = meta.icon,
            color = meta.color,
            isCompleted = task.currentValue >= task.targetValue,
            isClaimed = task.status == "CLAIMED"
        )
    }

    // Tự động sắp xếp nhiệm vụ:
    // 1. Nhiệm vụ ĐÃ HOÀN THÀNH CHƯA NHẬN THƯỞNG: Đưa lên ĐẦU TIÊN (Rank 0) để user thấy ngay và bấm nhận, tránh bị quên.
    // 2. Nhiệm vụ ĐÃ HOÀN THÀNH VÀ ĐÃ NHẬN: Rank 1 (nằm trong nhóm đã hoàn thành).
    // 3. Nhiệm vụ ĐANG THỰC HIỆN: Rank 2 (sắp xếp theo tiến độ % giảm dần).
    val sortedQuests = remember(quests) {
        quests.sortedWith(
            compareBy(
                { quest ->
                    when {
                        quest.isCompleted && !quest.isClaimed -> 0
                        quest.isCompleted && quest.isClaimed -> 1
                        else -> 2
                    }
                },
                { quest ->
                    if (!quest.isCompleted && quest.total > 0) {
                        -((quest.current.toFloat() / quest.total) * 100).toInt()
                    } else {
                        0
                    }
                },
                { it.taskId }
            )
        )
    }

    val totalQuests = quests.size
    val completedAndClaimedQuests = quests.count { it.isClaimed }
    val progress = if (totalQuests > 0) completedAndClaimedQuests.toFloat() / totalQuests else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(strings.dailyQuests, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        text = if (isEnglish) "Complete 6 quests to earn 100 ⭐" else "Hoàn thành 6 nhiệm vụ nhận 100 ⭐",
                        color = Color(0xFFFFD166),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    viewModel.refreshDailyTasks()
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF51FAC1))
            }
        }

        // Daily Chest Section
        DailyChestHeader(
            progress = progress,
            chestOpened = chestOpened,
            onOpenChest = {
                if (progress >= 1f && !chestOpened) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    viewModel.claimDailyChest()
                    Toast.makeText(context, strings.chestOpenedMsg, Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Quests List
        if (quests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0x2251FAC1))
                            .border(1.5.dp, Color(0x6651FAC1), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Celebration,
                            contentDescription = null,
                            tint = Color(0xFF51FAC1),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isEnglish) "All quests completed for today! 🌟" else "Tuyệt vời! Bạn đã hoàn thành hết nhiệm vụ! 🌟",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isEnglish) "Check back tomorrow for fresh daily rewards." else "Quay lại vào ngày mai để nhận thêm các nhiệm vụ và XP mới nhé.",
                        color = Color(0x99FFFFFF),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(sortedQuests, key = { it.taskId }) { quest ->
                    QuestItemCard(
                        quest = quest,
                        onClaim = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.soundEffectManager.playCorrect()
                            viewModel.claimDailyTaskWithStars(quest.taskId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DailyChestHeader(progress: Float, chestOpened: Boolean, onOpenChest: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "chest_anim")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "chest_pulse"
    )
    
    val shake by infiniteTransition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(100, easing = LinearEasing), RepeatMode.Reverse),
        label = "chest_shake"
    )

    val isFull = progress >= 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x1AFFFFFF))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Circular Progress & Chest
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0x33FFFFFF),
                    strokeWidth = 6.dp
                )
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = if (isFull) Color(0xFFFFD166) else Color(0xFF51FAC1),
                    strokeWidth = 6.dp
                )
                
                // Chest Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(if (isFull && !chestOpened) pulse else 1f)
                        .graphicsLayer { rotationZ = if (isFull && !chestOpened) shake else 0f }
                        .clip(CircleShape)
                        .background(if (isFull && !chestOpened) Color(0xFFFFD166).copy(alpha = 0.2f) else Color.Transparent)
                        .clickable(enabled = isFull && !chestOpened) { onOpenChest() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (chestOpened) Icons.Default.CheckCircle else Icons.Default.Redeem, 
                        contentDescription = "Chest", 
                        tint = if (chestOpened) Color(0xFF51FAC1) else if (isFull) Color(0xFFFFD166) else Color(0x80FFFFFF),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(24.dp))
            
            Column {
                val strings = LocalAppStrings.current
                Text(
                    text = strings.dailyChest, 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (chestOpened) strings.rewardClaimed else if (isFull) strings.tapChest else strings.completeAllToOpen, 
                    color = if (chestOpened) Color(0xFF51FAC1) else if (isFull) Color(0xFFFFD166) else Color(0xB3FFFFFF), 
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
 fun QuestItemCard(quest: QuestData, onClaim: () -> Unit) {
    val progressRatio = if (quest.total > 0) quest.current.toFloat() / quest.total else 0f
    
    // Blink animation for claim button
    val infiniteTransition = rememberInfiniteTransition(label = "claim_blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "blink"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF23183A), Color(0xFF140F24))
                )
            )
            .border(
                width = if (quest.isCompleted && !quest.isClaimed) 1.5.dp else 1.2.dp,
                brush = if (quest.isCompleted && !quest.isClaimed) {
                    Brush.horizontalGradient(listOf(Color(0xFFFFD166), Color(0xFF51FAC1)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF)))
                },
                shape = RoundedCornerShape(22.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(quest.color.copy(alpha = 0.2f))
                        .border(1.2.dp, quest.color.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(quest.icon, contentDescription = null, tint = quest.color, modifier = Modifier.size(26.dp))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Texts
                Column(modifier = Modifier.weight(1f)) {
                    Text(quest.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(quest.desc, color = Color(0xB3FFFFFF), fontSize = 12.5.sp, lineHeight = 17.sp)
                }
                
                // Reward Badge
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FFD166))
                        .border(1.dp, Color(0x66FFD166), RoundedCornerShape(10.dp))
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(quest.reward, color = Color(0xFFFFD166), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress Bar & Action Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Progress Bar
                Box(modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Color(0x33FFFFFF))) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressRatio)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(quest.color.copy(alpha = 0.5f), quest.color)))
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                Text("${quest.current}/${quest.total}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                
                if (quest.isCompleted && !quest.isClaimed) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(quest.color.copy(alpha = blinkAlpha))
                            .clickable { onClaim() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        val strings = LocalAppStrings.current
                        Text(strings.claim, color = Color(0xFF0F0C29), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                } else if (quest.isClaimed) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(Icons.Default.CheckCircle, contentDescription = "Claimed", tint = Color(0xFF51FAC1), modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
