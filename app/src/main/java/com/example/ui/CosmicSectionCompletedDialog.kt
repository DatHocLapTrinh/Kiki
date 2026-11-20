package com.example.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.model.SectionCompletionEvent

/**
 * Hộp thoại Chúc Mừng Hoàn Thành Chặng Vũ Trụ (Cosmic Section Completion Dialog).
 * Kích hoạt khi người học vượt qua hành tinh thứ 10 của một Chặng lớn,
 * vinh danh thành tích, thưởng XP & Sao, và hướng dẫn mở cổng sang chặng kế tiếp.
 */
@Composable
fun CosmicSectionCompletedDialog(
    event: SectionCompletionEvent,
    isEnglish: Boolean = false,
    onExploreNextSection: (nextSectionNumber: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val completedTitle = remember(event.completedSectionNumber, isEnglish) {
        CosmicSectionConfig.getSectionData(event.completedSectionNumber, 10).getDisplayTitle(isEnglish)
    }
    val nextTitle = remember(event.nextSectionNumber, isEnglish) {
        CosmicSectionConfig.getSectionData(event.nextSectionNumber, 0).getDisplayTitle(isEnglish)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xDD070514))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color(0xFFFFD166),
                        spotColor = Color(0xFF00F5D4)
                    ),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF5120E28))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.8.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0xFFFFD166),
                                    Color(0xFFFFB703),
                                    Color(0xFF00F5D4),
                                    Color(0xFFC084FC)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Nút đóng góc trên
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isEnglish) "Close" else "Đóng",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Mascot Kiki vui mừng hoặc Cúp vàng
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0x55FFD166),
                                        Color(0x2200F5D4),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(2.dp, Color(0xFFFFD166), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.kiki_happy),
                            contentDescription = "Kiki",
                            modifier = Modifier.size(80.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tiêu đề chúc mừng
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) {
                                "SECTION ${event.completedSectionNumber} CONQUERED!"
                            } else {
                                "CHINH PHỤC HOÀN TOÀN CHẶNG ${event.completedSectionNumber}!"
                            },
                            color = Color(0xFFFFD166),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = completedTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isEnglish) {
                            "Congratulations on mastering all 10 planets in this section! The cosmic stargate is now ready to take you to Section ${event.nextSectionNumber}: '$nextTitle'."
                        } else {
                            "Chúc mừng bạn đã xuất sắc làm chủ toàn bộ 10 hành tinh của chặng! Cổng dịch chuyển ngân hà đã sẵn sàng đưa bạn tới Chặng ${event.nextSectionNumber}: '$nextTitle'."
                        },
                        color = Color(0xCCFFFFFF),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Hộp hiển thị phần thưởng
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x1FFFFD166))
                            .border(1.2.dp, Color(0x66FFD166), RoundedCornerShape(16.dp))
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Phần thưởng XP
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "⚡",
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "+${event.rewardXp} XP",
                                        color = Color(0xFFFFD166),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = if (isEnglish) "Experience" else "Kinh nghiệm",
                                        color = Color(0x99FFFFFF),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Vạch ngăn cách dọc
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(Color(0x33FFD166))
                            )

                            // Phần thưởng Sao
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "⭐",
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (isEnglish) "+${event.rewardStars} STARS" else "+${event.rewardStars} SAO",
                                        color = Color(0xFFFFD166),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = if (isEnglish) "Cosmic Treasure" else "Kho báu vũ trụ",
                                        color = Color(0x99FFFFFF),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Nút CTA chính: Du hành tới chặng tiếp theo
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onExploreNextSection(event.nextSectionNumber)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00F5D4)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "EXPLORE SECTION ${event.nextSectionNumber} 🚀" else "KHÁM PHÁ CHẶNG ${event.nextSectionNumber} 🚀",
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = if (isEnglish) "Stay on map" else "Tiếp tục xem bản đồ",
                            color = Color(0xB3FFFFFF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

