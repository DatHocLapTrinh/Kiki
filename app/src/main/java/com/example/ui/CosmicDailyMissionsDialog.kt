package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.QuestData
import com.example.R

/**
 * Hộp thoại Trạm Nhiệm Vụ Vũ Trụ & Tái Nạp Năng Lượng (Cosmic Daily Missions Dialog)
 * Hiển thị toàn bộ nhiệm vụ ngày, rương báu bí ẩn và nhắc nhở ôn tập ngắt quãng (Spaced Repetition)
 * trực tiếp trên bản đồ mà không làm gián đoạn dòng trải nghiệm học.
 */
@Composable
fun CosmicDailyMissionsDialog(
    quests: List<QuestData>,
    streak: Int,
    isChestClaimed: Boolean,
    hasPlanetNeedingReview: Boolean = false,
    planetNeedingReviewName: String? = null,
    onClaimQuest: (Long) -> Unit,
    onClaimChest: () -> Unit,
    onReviewPlanet: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val completedCount = quests.count { it.isCompleted }
    val totalCount = quests.size.coerceAtLeast(1)
    val allCompleted = completedCount >= totalCount && quests.isNotEmpty()

    val infiniteTransition = rememberInfiniteTransition(label = "dialog_chest_glow")
    val chestPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (allCompleted && !isChestClaimed) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chest_pulse"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .clickable(enabled = false) {} // Chặn click ra ngoài
            ) {
                // Quầng sáng hào quang không gian
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(8.dp)
                        .blur(24.dp)
                        .background(
                            Color(0xFF51FAC1).copy(alpha = 0.20f),
                            RoundedCornerShape(28.dp)
                        )
                )

                // Thẻ Card chính phi thuyền
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF131728),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFF51FAC1).copy(alpha = 0.9f),
                                Color(0xFF38BDF8).copy(alpha = 0.4f),
                                Color(0xFF1E2238)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // ─── Header: Tiêu đề & Nút đóng ───
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🌌",
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Nhiệm Vụ Vũ Trụ",
                                        color = Color.White,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "Làm mới hàng ngày • Nhận XP & Sao",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0x22FFFFFF), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ─── Streak Multiplier Banner ───
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0x33FF9800),
                                            Color(0x22FF5722)
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    Color(0x44FF9800),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🔥", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Chuỗi $streak ngày: Thưởng thêm +10% XP khi hoàn thành nhiệm vụ!",
                                color = Color(0xFFFFD166),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ─── Rương Kho Báu Ngày Bí Ẩn ───
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF1E243E),
                                            Color(0xFF15192C)
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    if (allCompleted && !isChestClaimed) Color(0xFFFFD166) else Color(0x33FFFFFF),
                                    RoundedCornerShape(18.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_reward_chest),
                                    contentDescription = "Daily Chest",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .scale(chestPulse)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isChestClaimed) "Rương Ngày Đã Mở ✔️" else "Rương Bí Ẩn Hàng Ngày",
                                        color = if (allCompleted && !isChestClaimed) Color(0xFFFFD166) else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isChestClaimed) {
                                            "Bạn đã nhận trọn vẹn phần thưởng ngày hôm nay!"
                                        } else if (allCompleted) {
                                            "Đủ điều kiện! Nhấn để nhận +50 ⭐, +100 XP & 🛡️ Khiên!"
                                        } else {
                                            "Hoàn thành $completedCount/$totalCount nhiệm vụ để mở rương"
                                        },
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.5.sp
                                    )
                                }

                                if (allCompleted && !isChestClaimed) {
                                    Button(
                                        onClick = onClaimChest,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD166)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "MỞ",
                                            color = Color(0xFF0F172A),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // ─── Banner Tái Nạp Năng Lượng Vũ Trụ (Spaced Repetition Hint) ───
                        if (hasPlanetNeedingReview) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0x33FFB74D), Color(0x22FFA726))
                                        )
                                    )
                                    .border(1.dp, Color(0x66FFB74D), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB74D),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Hành tinh đang cạn năng lượng! Ôn tập nhận +30 XP (+5 ⭐)",
                                        color = Color(0xFFFFE082),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFB74D))
                                        .clickable {
                                            onDismiss()
                                            onReviewPlanet()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "ÔN TẬP ⚡",
                                        color = Color(0xFF1E2238),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ─── Danh Sách Nhiệm Vụ Ngày (Daily Tasks List) ───
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(quests, key = { it.taskId }) { quest ->
                                DailyQuestCard(
                                    quest = quest,
                                    onClaim = { onClaimQuest(quest.taskId) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ─── Footer Action ───
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Tiếp Tục Khám Phá Vũ Trụ 🚀",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyQuestCard(
    quest: QuestData,
    onClaim: () -> Unit
) {
    val progress = (quest.current.toFloat() / quest.total.coerceAtLeast(1)).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1F36))
            .border(
                width = 1.dp,
                color = if (quest.isCompleted && !quest.isClaimed) Color(0xFFFFD166) else Color(0x1FFFFFFF),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon nhiệm vụ
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(quest.color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (quest.iconRes != null) {
                Image(
                    painter = painterResource(quest.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            } else if (quest.icon != null) {
                Icon(
                    imageVector = quest.icon,
                    contentDescription = null,
                    tint = quest.color,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = quest.color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Chi tiết nhiệm vụ
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quest.title,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = quest.desc,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Thanh tiến độ mini
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0x33FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (quest.isCompleted) Color(0xFF51FAC1) else Color(0xFF38BDF8)
                            )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${quest.current}/${quest.total}",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Nút nhận hoặc trạng thái
        if (quest.isClaimed) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x2251FAC1))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Đã nhận ✔️",
                    color = Color(0xFF51FAC1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (quest.isCompleted) {
            Button(
                onClick = onClaim,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD166)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "+${quest.rewardXp} XP",
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Black,
                    fontSize = 11.5.sp
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${quest.rewardXp} XP",
                    color = Color(0xFF51FAC1),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "+${quest.rewardStar} ⭐",
                    color = Color(0xFFFFD166),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

