package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Nút nổi truy cập nhanh Nhiệm Vụ Vũ Trụ (Daily Cosmic Quests Floating Pill)
 * Được đặt ở góc dưới của JourneyMapScreen, giúp người dùng theo dõi và nhận thưởng
 * mà không cần phải chuyển sang tab khác.
 */
@Composable
fun CosmicDailyMissionsFloatingButton(
    completedCount: Int,
    totalCount: Int,
    hasClaimable: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "quest_btn_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (hasClaimable) 1.08f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (hasClaimable) 0.85f else 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .scale(if (hasClaimable) pulseScale else 1f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Quầng sáng hào quang vũ trụ phía sau
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(16.dp)
                .background(
                    if (hasClaimable) {
                        Brush.radialGradient(listOf(Color(0xFFFFD166).copy(alpha = glowAlpha), Color.Transparent))
                    } else {
                        Brush.radialGradient(listOf(Color(0xFF51FAC1).copy(alpha = glowAlpha), Color.Transparent))
                    },
                    RoundedCornerShape(24.dp)
                )
        )

        // Pill card chính phong cách phi thuyền vũ trụ
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xEE1E2238),
                            Color(0xEE151A2E)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        if (hasClaimable) {
                            listOf(Color(0xFFFFD166), Color(0xFFFF9800))
                        } else {
                            listOf(Color(0xFF51FAC1).copy(alpha = 0.8f), Color(0xFF38BDF8).copy(alpha = 0.6f))
                        }
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon tâm ngắm vũ trụ / nhiệm vụ
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasClaimable) Color(0x33FFD166) else Color(0x3351FAC1)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CrisisAlert,
                    contentDescription = "Cosmic Missions",
                    tint = if (hasClaimable) Color(0xFFFFD166) else Color(0xFF51FAC1),
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "Nhiệm vụ ngày",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$completedCount/$totalCount Hoàn thành",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (hasClaimable) {
                Spacer(modifier = Modifier.width(8.dp))
                // Badge thông báo có quà sẵn sàng nhận
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFF5252), Color(0xFFFF1744)))
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NHẬN!",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

