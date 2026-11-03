package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.CosmicPlanetHelper
import com.example.JourneyStageState
import com.example.JourneyStageUiModel
import com.example.LocalAppStrings

/**
 * Pop-up thông tin bài học phong cách Galaxy của Kiki khi chạm vào hành tinh trên lộ trình.
 * Thiết kế mượt mà theo chuẩn Duolingo với mũi tên trỏ vào tâm hành tinh và nút CTA bắt đầu.
 */
@Composable
fun CosmicPlanetLessonPopup(
    stage: JourneyStageUiModel,
    index: Int,
    totalStages: Int = 100,
    isEnglish: Boolean = false,
    arrowX: Dp,
    modifier: Modifier = Modifier,
    onStartQuest: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val planetType = remember(index, totalStages) {
        CosmicPlanetHelper.getPlanetTypeForStage(index, totalStages)
    }
    val planetName = remember(planetType) {
        CosmicPlanetHelper.getPlanetTypeName(planetType)
    }
    val planetColor = remember(planetType) {
        CosmicPlanetHelper.getPlanetPrimaryColor(planetType)
    }

    val isLocked = stage.state == JourneyStageState.LOCKED
    val isCompleted = stage.state == JourneyStageState.COMPLETED

    // Mũi tên trỏ thẳng vào tâm hành tinh
    // arrowX tính từ lề trái màn hình; trong container có padding ngang 16.dp
    val localArrowX = (arrowX - 16.dp).coerceAtLeast(36.dp)

    val popupShape = remember(localArrowX) {
        SpeechBubbleShape(
            direction = BubbleArrowDirection.TOP,
            cornerRadius = 24.dp,
            arrowWidth = 22.dp,
            arrowHeight = 12.dp,
            arrowCenterX = localArrowX
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Chặn sự kiện click xuyên qua nền
            }
    ) {
        // Hào quang vũ trụ phát sáng dịu phía sau Popup
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(top = 12.dp)
                .blur(32.dp)
                .background(planetColor.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
        )

        // Khung thân chính của Popup theo chuẩn Galaxy Design System
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(18.dp, popupShape)
                .clip(popupShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            planetColor.copy(alpha = 0.28f),
                            Color(0xF5130D26),
                            Color(0xF80B0718)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            planetColor.copy(alpha = 0.95f),
                            Color(0xFF51FAC1).copy(alpha = 0.65f),
                            Color(0x33FFFFFF)
                        )
                    ),
                    shape = popupShape
                )
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Hàng 1: Badge thông tin Chặng & Nút Đóng (✕)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Viên ngọc phát sáng theo màu hành tinh
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(planetColor)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "CHẶNG ${index + 1} • $planetName".uppercase(),
                            color = planetColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    // Nút đóng (✕) tinh gọn
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0x26FFFFFF))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = Color(0xCCFFFFFF),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Hàng 2: Tiêu đề bài học lớn & nổi bật (như Duolingo "Sử dụng thì hiện tại")
                Text(
                    text = stage.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.2).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Hàng 3: Trạng thái & thông số (Bài học 1 / 5 • +20 XP)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when {
                            isLocked -> if (isEnglish) "Locked milestone" else "Cửa ải đang khóa"
                            isCompleted -> if (isEnglish) "Completed • Review" else "Đã hoàn thành • Ôn luyện"
                            else -> if (isEnglish) "Lesson 1 / 5" else "Bài học 1 / 5"
                        },
                        color = Color(0xB3FFFFFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "•",
                        color = Color(0x66FFFFFF),
                        fontSize = 13.sp
                    )

                    // Huy hiệu phần thưởng XP
                    Text(
                        text = if (isCompleted) "+10 XP" else "+20 XP",
                        color = Color(0xFFFFD166),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hàng 4: Nút Kêu Gọi Hành Động (CTA Button) to bản chuẩn Duolingo
                if (!isLocked) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStartQuest()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleted) Color(0xFFFFD166) else Color(0xFF51FAC1)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = if (isCompleted) {
                                if (isEnglish) "REVIEW +10 XP ⭐" else "ÔN TẬP +10 XP ⭐"
                            } else {
                                if (isEnglish) "START +20 XP 🚀" else "BẮT ĐẦU +20 XP 🚀"
                            },
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x1AFFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0x99FFFFFF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "COMPLETE PREVIOUS PLANET" else "HOÀN THÀNH CHẶNG TRƯỚC",
                                color = Color(0x99FFFFFF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
