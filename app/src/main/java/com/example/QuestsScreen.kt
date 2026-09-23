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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
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
    val currentUserId by viewModel.currentUserId.observeAsState(-1L)
    val dailyTasks by viewModel.dailyTasks.observeAsState(emptyList())
    val chestOpened by viewModel.isDailyChestOpened.observeAsState(false)

    LaunchedEffect(currentUserId) {
        if (currentUserId != -1L) viewModel.refreshDailyTasks()
    }

    val quests = dailyTasks.map { task ->
        val meta = when (task.taskType) {
            "LESSON_COMPLETE" -> QuestMeta(strings.quest1Title, strings.quest1Desc, Icons.Default.AutoStories, Color(0xFF51FAC1))
            "PERFECT_SCORE" -> QuestMeta(strings.quest2Title, strings.quest2Desc, Icons.Default.Star, Color(0xFFFFD166))
            else -> QuestMeta(strings.quest3Title, strings.quest3Desc, Icons.AutoMirrored.Filled.MenuBook, Color(0xFF27E0A9))
        }
        QuestData(
            taskId = task.taskId,
            title = meta.title,
            desc = meta.desc,
            current = task.currentValue.coerceAtMost(task.targetValue),
            total = task.targetValue,
            reward = "${task.rewardXp} XP",
            icon = meta.icon,
            color = meta.color,
            isCompleted = task.currentValue >= task.targetValue,
            isClaimed = task.status == "CLAIMED"
        )
    }

    val totalQuests = quests.size
    val completedAndClaimedQuests = quests.count { it.isClaimed }
    val progress = if (totalQuests > 0) completedAndClaimedQuests.toFloat() / totalQuests else 0f

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 76.dp, end = 24.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(strings.dailyQuests, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
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
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(strings.noActiveQuests, color = Color(0xB3FFFFFF), fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(quests, key = { it.taskId }) { quest ->
                    QuestItemCard(
                        quest = quest,
                        onClaim = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.soundEffectManager.playCorrect()
                            viewModel.claimDailyTask(quest.taskId)
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
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x1A000000))
            .border(1.dp, if (quest.isCompleted && !quest.isClaimed) quest.color.copy(alpha = 0.5f) else Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(quest.color.copy(alpha = 0.15f))
                        .border(1.dp, quest.color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(quest.icon, contentDescription = null, tint = quest.color, modifier = Modifier.size(24.dp))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Texts
                Column(modifier = Modifier.weight(1f)) {
                    Text(quest.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(quest.desc, color = Color(0x99FFFFFF), fontSize = 13.sp, lineHeight = 18.sp)
                }
                
                // Reward Badge
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFD166).copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(quest.reward, color = Color(0xFFFFD166), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
