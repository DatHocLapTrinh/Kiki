package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.JourneyStageUiModel

/**
 * Hộp thoại "Cổng Dịch Chuyển Ngân Hà" (Cosmic Stargate Section Selector).
 * Cho phép người dùng chuyển nhanh giữa 4 Chặng lớn (40 hành tinh),
 * theo dõi tiến độ hoàn thành và trình độ CEFR tương ứng.
 */
@Composable
fun CosmicSectionSelectorDialog(
    currentSectionNumber: Int,
    stages: List<JourneyStageUiModel>,
    onSelectSection: (sectionNumber: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sections = remember(stages) {
        CosmicSectionConfig.getAllSections(stages)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Scrim nền mờ bán trong suốt
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC070514))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Container chính của Dialog
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.86f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Chặn click xuyên thấu
                    )
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = Color(0xFF00F5D4),
                        spotColor = Color(0xFFC084FC)
                    ),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF2100E22))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 1.4.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0xFF00F5D4),
                                    Color(0xFFC084FC),
                                    Color(0xFFFBBF24),
                                    Color(0xFF34D399)
                                )
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    // ─── Header ───
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x2200F5D4))
                                .border(1.2.dp, Color(0xFF00F5D4), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = Color(0xFF00F5D4),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CỔNG DỊCH CHUYỂN NGÂN HÀ 🛸",
                                color = Color(0xFF00F5D4),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Lộ trình 4 Chặng Không Gian • 40 Hành Tinh",
                                color = Color(0xB3FFFFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ─── Danh Sách 4 Chặng Không Gian ───
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        sections.forEach { sectionData ->
                            CosmicSectionCard(
                                section = sectionData,
                                isCurrent = (sectionData.sectionNumber == currentSectionNumber),
                                onSelect = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectSection(sectionData.sectionNumber)
                                    onDismiss()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ─── Footer Gợi Ý ───
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x1800F5D4))
                            .border(1.dp, Color(0x3300F5D4), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "💡 Mỗi chặng vũ trụ gồm 10 hành tinh với các bài học vi mô thích ứng thông minh, giúp bạn duy trì phản xạ tiếng Anh mỗi ngày.",
                            color = Color(0xCC00F5D4),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CosmicSectionCard(
    section: CosmicSectionData,
    isCurrent: Boolean,
    onSelect: () -> Unit
) {
    val palette = remember(section.sectionNumber) {
        CosmicSectionConfig.getSectionPalette(section.sectionNumber)
    }

    val progress = (section.completedCount.toFloat() / section.totalPlanets.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600),
        label = "dialog_sec_progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (isCurrent) 2.dp else 1.2.dp,
                brush = Brush.horizontalGradient(palette.borderColors),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(palette.bgStart, palette.bgMid, palette.bgEnd)
                    )
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Hàng nhãn trên cùng (Số chặng + CEFR Pill + Trạng thái đang học)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Tag số chặng và hành tinh
                    Text(
                        text = "CHẶNG ${section.sectionNumber} • HÀNH TINH ${section.startPlanet} - ${section.endPlanet}".uppercase(),
                        color = palette.primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )

                    // Pill CEFR
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.primaryColor.copy(alpha = 0.16f))
                            .border(1.dp, palette.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = section.cefrLevel,
                            color = palette.primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tiêu đề chặng
                Text(
                    text = section.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Mô tả chặng
                Text(
                    text = section.description,
                    color = Color(0xB3FFFFFF),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Thanh tiến độ hoàn thành
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(palette.primaryColor, palette.secondaryColor)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "${section.completedCount}/${section.totalPlanets}",
                        color = Color(0xCCFFFFFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nút hành động "Du hành tới chặng này"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x2210B981))
                                .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🚀 ĐANG Ở CHẶNG NÀY",
                                color = Color(0xFF34D399),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(palette.primaryColor, palette.secondaryColor)
                                )
                            )
                            .clickable(onClick = onSelect)
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = Color(0xFF0F0C20),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (section.completedCount == section.totalPlanets) "Xem lại chặng" else "Du hành tới chặng",
                                color = Color(0xFF0F0C20),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}
