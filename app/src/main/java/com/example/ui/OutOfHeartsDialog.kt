package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R

/**
 * Hộp thoại Game Over khi người dùng hết sạch tim trong bài học hoặc bản đồ ngân hà.
 * Cho phép dùng Sao (Stars) nạp đầy 5 tim để tiếp tục thử thách ngay lập tức, hoặc rời khỏi bài học.
 */
@Composable
fun OutOfHeartsDialog(
    currentStars: Int,
    isEnglish: Boolean = false,
    starCost: Int = 20,
    onRefillWithStars: () -> Unit,
    onQuitLesson: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "heart_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_scale"
    )

    val canAfford = currentStars >= starCost

    Dialog(
        onDismissRequest = { /* Chặn đóng khi click ra ngoài để bắt buộc chọn hành động */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color(0x66FF3B5C),
                        spotColor = Color(0x88FF1744)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xF51E1328),
                                    Color(0xF5120B1C)
                                )
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(
                                listOf(Color(0xFFFF4D6D), Color(0x80818CF8))
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icon Trái tim vỡ / Hết tim phát xung nhịp nhàng
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0x26FF4D6D))
                                .border(2.dp, Color(0x66FF4D6D), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_stat_heart),
                                contentDescription = "Out of hearts",
                                modifier = Modifier.size(52.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Tiêu đề Game Over
                        Text(
                            text = if (isEnglish) "Out of Hearts! 💔" else "Hết Tim Rồi! 💔",
                            color = Color(0xFFFF5252),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Mô tả
                        Text(
                            text = if (isEnglish)
                                "You've run out of hearts to continue this challenge. Refill 5 hearts with Stars to keep learning!"
                            else
                                "Bạn đã dùng hết số tim để tiếp tục thử thách này. Hãy dùng Sao nạp đầy 5 tim để tiếp tục học ngay nhé!",
                            color = Color(0xCCFFFFFF),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Badge số sao hiện có
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x26FFD166))
                                .border(1.dp, Color(0x55FFD166), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⭐",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEnglish) "$currentStars Stars available" else "$currentStars Sao hiện có",
                                color = Color(0xFFFFD166),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Nút 1: Dùng Sao nạp đầy 5 tim (nút chính)
                        Button(
                            onClick = {
                                if (canAfford) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onRefillWithStars()
                                }
                            },
                            enabled = canAfford,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF51FAC1),
                                contentColor = Color(0xFF0F172A),
                                disabledContainerColor = Color(0x26FFFFFF),
                                disabledContentColor = Color(0x55FFFFFF)
                            )
                        ) {
                            Text(
                                text = if (canAfford) {
                                    if (isEnglish) "REFILL 5 HEARTS (-$starCost ⭐)" else "NẠP ĐẦY 5 TIM (-$starCost ⭐)"
                                } else {
                                    if (isEnglish) "NEED $starCost STARS (NOT ENOUGH)" else "CẦN $starCost SAO (KHÔNG ĐỦ SAO)"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Nút 2: Rời khỏi bài học (Game Over)
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onQuitLesson()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0x44FF4D6D)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF7A7A)
                            )
                        ) {
                            Text(
                                text = if (isEnglish) "Exit Lesson (Game Over)" else "Rời Khỏi Bài Học (Kết thúc)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

