package com.example

import androidx.compose.animation.core.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RewardChestCard(
    unlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val infiniteTransition = rememberInfiniteTransition(label = "reward_chest_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    val borderColor = if (unlocked) Color(0xFFFFD166).copy(alpha = glowAlpha) else Color(0x33FFFFFF)
    val bgGradient = if (unlocked) {
        Brush.linearGradient(listOf(Color(0x33FFD166), Color(0x1A27E0A9)))
    } else {
        Brush.linearGradient(listOf(Color(0x2214142B), Color(0x1114142B)))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bgGradient)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (unlocked) Color(0xFFFFD166).copy(alpha = 0.2f) else Color(0x1AFFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (unlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (unlocked) Color(0xFFFFD166) else Color(0x66FFFFFF),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (unlocked) strings.rewardUnlocked else strings.rewardLocked,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (unlocked) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFFD166), Color(0xFFFF9E00))))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = strings.openReward,
                        color = Color(0xFF11101B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun JourneyLoadingState(modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = Color(0xFF00F5D4), modifier = Modifier.size(44.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(strings.loading, color = Color(0xB3FFFFFF), fontSize = 14.sp)
    }
}

@Composable
fun JourneyErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(message, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4))
        ) {
            Text(strings.retry, color = Color(0xFF080512), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun JourneyEmptyState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Explore, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(44.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(strings.journeyEmptyTitle, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(6.dp))
        Text(strings.journeyEmptyDescription, color = Color(0xB3FFFFFF), fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4))
        ) {
            Text(strings.retry, color = Color(0xFF080512), fontWeight = FontWeight.Bold)
        }
    }
}
