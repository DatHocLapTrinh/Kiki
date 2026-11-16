package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
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

/**
 * Trích xuất tên bài học ngắn gọn, loại bỏ song ngữ tiếng Anh và tiền tố trùng lặp.
 * Ví dụ: "Chặng 1: Sao Thủy - Chào Hỏi & Làm Quen Cơ Bản (Mercury: Greetings & Hello)"
 *      -> "Chào Hỏi & Làm Quen Cơ Bản"
 */
fun cleanLessonTitle(rawTitle: String): String {
    // 1. Loại bỏ phần trong ngoặc đơn (phần song ngữ tiếng Anh)
    var cleaned = rawTitle.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
    // 2. Nếu có dấu gạch ngang (Chặng X: Tên Hành Tinh - Nội dung bài học), lấy nội dung bài học
    if (cleaned.contains(" - ")) {
        cleaned = cleaned.substringAfterLast(" - ").trim()
    } else if (cleaned.contains(" – ")) {
        cleaned = cleaned.substringAfterLast(" – ").trim()
    } else if (cleaned.contains(" — ")) {
        cleaned = cleaned.substringAfterLast(" — ").trim()
    } else if (cleaned.contains(":")) {
        cleaned = cleaned.substringAfterLast(":").trim()
    }
    return cleaned.ifBlank { rawTitle }
}

/**
 * Pop-up bài học tối giản phong cách Duolingo kết hợp Galaxy của Kiki:
 * - Nhỏ gọn, cân đối, không to thô.
 * - Loại bỏ hoàn toàn chữ thừa, tên khoáng thạch, song ngữ trùng lặp.
 * - Mũi tên trỏ chính xác vào tâm hành tinh.
 * - Điểm +20 XP chỉ xuất hiện duy nhất ở nút bấm, bỏ icon tên lửa.
 */
@Composable
fun CosmicPlanetLessonPopup(
    stage: JourneyStageUiModel,
    index: Int,
    totalStages: Int = 100,
    isEnglish: Boolean = false,
    arrowX: Dp = 70.dp,
    modifier: Modifier = Modifier,
    onStartQuest: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val planetType = remember(index, totalStages) {
        CosmicPlanetHelper.getPlanetTypeForStage(index, totalStages)
    }
    val planetColor = remember(planetType) {
        CosmicPlanetHelper.getPlanetPrimaryColor(planetType)
    }

    val isLocked = stage.state == JourneyStageState.LOCKED
    val isCompleted = stage.state == JourneyStageState.COMPLETED
    val cleanTitle = remember(stage.title) {
        cleanLessonTitle(stage.title)
    }

    val popupShape = remember(arrowX) {
        SpeechBubbleShape(
            direction = BubbleArrowDirection.TOP,
            cornerRadius = 18.dp,
            arrowWidth = 18.dp,
            arrowHeight = 10.dp,
            arrowCenterX = arrowX
        )
    }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Chặn click xuyên thấu
            }
    ) {
        // Hào quang vũ trụ phát sáng dịu phía sau popup
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 10.dp)
                .blur(20.dp)
                .background(planetColor.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
        )

        // Khung card chính tối giản, tinh tế
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, popupShape)
                .clip(popupShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E2238),
                            Color(0xFF131626)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            planetColor.copy(alpha = 0.9f),
                            Color(0xFF51FAC1).copy(alpha = 0.5f)
                        )
                    ),
                    shape = popupShape
                )
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Tiêu đề bài học ngắn gọn, rõ ràng, không lặp lại
                Text(
                    text = cleanTitle,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 2. Phụ đề tiến độ bài học & Chỉ số Năng lượng Vũ trụ
                if (isCompleted && stage.needsReview) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEnglish) "Cosmic Energy: ${(stage.energyLevel * 100).toInt()}% • Needs Recharge" else "Năng lượng: ${(stage.energyLevel * 100).toInt()}% • Cần Tái Nạp ⚡",
                            color = Color(0xFFFFB74D),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x33FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(stage.energyLevel.coerceIn(0.1f, 1f))
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFFB74D), Color(0xFFFF9800))
                                    )
                                )
                        )
                    }
                } else {
                    Text(
                        text = formatLessonSubtitle(
                            completedLessons = stage.completedLessons,
                            totalLessons = stage.totalLessons,
                            isLocked = isLocked,
                            isCompleted = isCompleted,
                            isEnglish = isEnglish
                        ),
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Nút bấm CTA phong cách Duolingo (chỉ đặt XP ở đây, bỏ icon tên lửa)
                if (!isLocked) {
                    val isRecharge = isCompleted && stage.needsReview
                    val buttonColor = when {
                        isRecharge -> Color(0xFFFF9800)
                        isCompleted -> Color(0xFFFFC72C)
                        else -> Color(0xFF51FAC1)
                    }
                    val buttonTextColor = Color(0xFF0F172A)

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStartQuest()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = when {
                                isRecharge -> if (isEnglish) "RECHARGE +30 XP (+5 ⭐)" else "TÁI NẠP NĂNG LƯỢNG +30 XP (+5 ⭐)"
                                isCompleted -> if (isEnglish) "REVIEW +10 XP" else "ÔN TẬP +10 XP"
                                else -> if (isEnglish) "START +20 XP" else "BẮT ĐẦU +20 XP"
                            },
                            color = buttonTextColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0x99FFFFFF),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEnglish) "LOCKED" else "CHƯA MỞ KHÓA",
                                color = Color(0x99FFFFFF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format phụ đề tiến độ bài học gọn gàng, chính xác theo số bài đã hoàn thành
 */
fun formatLessonSubtitle(
    completedLessons: Int,
    totalLessons: Int,
    isLocked: Boolean,
    isCompleted: Boolean,
    isEnglish: Boolean = false
): String {
    val total = totalLessons.coerceAtLeast(1)
    val currentLessonNum = (completedLessons + 1).coerceAtMost(total)
    return when {
        isLocked -> if (isEnglish) "Locked milestone" else "Chưa mở khóa"
        isCompleted -> if (isEnglish) "Completed ($total/$total)" else "Đã hoàn thành ($total/$total)"
        else -> if (isEnglish) "Lesson $currentLessonNum / $total" else "Bài học $currentLessonNum / $total"
    }
}
