package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
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
 * Hộp thoại Góp Ý & Khảo Sát Trải Nghiệm Phong Cách Vũ Trụ Kiki:
 * - Hiển thị thân thiện khi người dùng đã hoàn thành các bài học Microlearning đầu tiên.
 * - Tặng phần thưởng +50 Sao Năng Lượng Vũ Trụ khi người dùng đồng ý tham gia khảo sát.
 * - Dẫn trực tiếp đến biểu mẫu Google Form chuyên sâu của dự án.
 */
@Composable
fun CosmicFeedbackDialog(
    isEnglish: Boolean = false,
    onOpenSurvey: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "FeedbackAura")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1B1536),
                            Color(0xFF110D24),
                            Color(0xFF090616)
                        )
                    )
                )
                .border(
                    width = 1.6.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00F5D4),
                            Color(0xFF7B2CBF),
                            Color(0xFFFFB703)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(24.dp)
        ) {
            // Nút đóng góc trên phải
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color(0x88FFFFFF),
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    }
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Huy hiệu tiêu đề nhỏ
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x2200F5D4))
                        .border(1.dp, Color(0x6600F5D4), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isEnglish) "🌟 EXPLORER FEEDBACK" else "🌟 ĐỒNG HÀNH CÙNG KIKI",
                        color = Color(0xFF00F5D4),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar linh thú Kiki với ánh hào quang
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Vòng hào quang tỏa sáng nền
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x4400F5D4),
                                        Color(0x227B2CBF),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Image(
                        painter = painterResource(id = R.drawable.kiki_welcome),
                        contentDescription = "Kiki Mascot",
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tiêu đề chính
                Text(
                    text = if (isEnglish)
                        "How is Your Space Journey?"
                    else
                        "Chuyến Thám Hiểm Của Bạn Thế Nào?",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mô tả nội dung
                Text(
                    text = if (isEnglish)
                        "Only 2 minutes to share your thoughts on Kiki's AI Microlearning lessons! Your input directly shapes the future of our universe."
                    else
                        "Chỉ mất 2 phút chia sẻ cảm nhận của bạn về các bài học Microlearning & phản xạ AI! Ý kiến quý giá của bạn sẽ giúp Kiki tiến hóa thông minh và hoàn hảo hơn mỗi ngày.",
                    color = Color(0xCCFFFFFF),
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hộp quà tặng tri ân (+50 Sao Năng Lượng)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x22FFD166))
                        .border(1.dp, Color(0x66FFD166), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Gift Star",
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish)
                                "Gift: +50 Cosmic Energy Stars!"
                            else
                                "Quà tặng: Nhận ngay +50 ⭐ Năng Lượng!",
                            color = Color(0xFFFFD166),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Nút hành động chính: Mở Form Khảo Sát
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenSurvey()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF00F5D4),
                                        Color(0xFF00BBF9)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isEnglish) "Share Feedback Now" else "Góp Ý Ngay (+50 ⭐)",
                                color = Color(0xFF05131A),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open Survey",
                                tint = Color(0xFF05131A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Nút phụ: Để lúc khác
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Remind Me Later" else "Để lúc khác",
                        color = Color(0x88FFFFFF),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
