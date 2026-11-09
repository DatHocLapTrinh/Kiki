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
import androidx.compose.material.icons.filled.Favorite
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
 * Hộp Thoại Tri Ân & Thư Tay Cảm Ơn Từ Chú Kiến Kiki (Cosmic Gratitude & Feedback Dialog):
 * - Bày tỏ lòng biết ơn chân thành đến người học đã gắn bó và thám hiểm vũ trụ Kiki.
 * - Cung cấp nút bấm chuyển hướng trực tiếp đến Google Form khảo sát chính thức.
 * - Tặng phần thưởng +50 ⭐ Năng Lượng Vũ Trụ như một món quà cảm ơn.
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
        targetValue = 1.05f,
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
                            Color(0xFFFF758F),
                            Color(0xFF00F5D4),
                            Color(0xFFFFD166)
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
                // Huy hiệu tri ân trên đỉnh
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x29FF758F))
                        .border(1.dp, Color(0x66FF758F), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF758F),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "A THANK-YOU FROM KIKI" else "LỜI CẢM ƠN TỪ KIẾN KIKI",
                            color = Color(0xFFFF758F),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar linh thú Kiki chào đón ấm áp với ánh hào quang
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Vòng hào quang tỏa sáng nền đa sắc
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x44FF758F),
                                        Color(0x3300F5D4),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Image(
                        painter = painterResource(id = R.drawable.kiki_welcome),
                        contentDescription = "Kiki Mascot",
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tiêu đề cảm ơn chân thành
                Text(
                    text = if (isEnglish)
                        "Thank You for Learning with Kiki!"
                    else
                        "Cảm Ơn Bạn Đã Đồng Hành Cùng Kiki!",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Lời nhắn tri ân cảm xúc
                Text(
                    text = if (isEnglish)
                        "Your daily dedication brings the Kiki universe to life! To make lessons smarter, smoother, and better for you, our team would love to hear your honest thoughts."
                    else
                        "Sự đồng hành của bạn giúp vũ trụ Kiki ngày càng sống động! Để Kiki tiến hóa thông minh, mượt mà và gần gũi hơn nữa, nhóm phát triển rất mong được lắng nghe cảm nhận thực tế của bạn qua 2 phút chia sẻ.",
                    color = Color(0xDDFFFFFF),
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hộp quà tặng tri ân (+50 Sao Năng Lượng Vũ Trụ)
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
                                "Gratitude Gift: +50 Cosmic Stars!"
                            else
                                "Quà tri ân: Nhận ngay +50 ⭐ Năng Lượng!",
                            color = Color(0xFFFFD166),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Nút hành động chính: Dẫn trực tiếp đến Google Form
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
                                text = if (isEnglish) "Send Feedback to Kiki Team 💌" else "Gửi Góp Ý Cho Nhóm Kiki 💌",
                                color = Color(0xFF05131A),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.5.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open Google Form",
                                tint = Color(0xFF05131A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Nút phụ: Để lúc khác
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Remind Me Later" else "Để Kiki sau nhé",
                        color = Color(0x88FFFFFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
