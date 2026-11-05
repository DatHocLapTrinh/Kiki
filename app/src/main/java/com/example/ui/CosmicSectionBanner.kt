package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Dữ liệu cho một Chặng học lớn (1 chặng = 10 hành tinh).
 */
data class CosmicSectionData(
    val sectionNumber: Int,
    val startPlanet: Int,
    val endPlanet: Int,
    val title: String,
    val description: String,
    val completedCount: Int,
    val totalPlanets: Int = 10,
    val cefrLevel: String = "A0 - A1 • Khởi Động"
)

/**
 * Bảng màu đặc trưng đại diện cho từng Chặng học không gian.
 */
data class CosmicSectionPalette(
    val primaryColor: Color,
    val secondaryColor: Color,
    val bgStart: Color,
    val bgMid: Color,
    val bgEnd: Color,
    val borderColors: List<Color>,
    val glowColor: Color
)

object CosmicSectionConfig {
    private val sectionPresets = listOf(
        CosmicSectionData(
            sectionNumber = 1,
            startPlanet = 1,
            endPlanet = 10,
            title = "Hệ Mặt Trời & Giao Tiếp Cơ Bản",
            description = "Làm chủ những mẫu câu nền tảng nhất: chào hỏi, giới thiệu bản thân, gia đình, đồ vật và thời gian trong không gian vũ trụ.",
            completedCount = 0,
            totalPlanets = 10,
            cefrLevel = "A0 - A1 • Khởi Động"
        ),
        CosmicSectionData(
            sectionNumber = 2,
            startPlanet = 11,
            endPlanet = 20,
            title = "Thiên Hà Tiên Nữ & Đời Sống Du Lịch",
            description = "Băng qua trạm không gian, tự tin đặt vé tàu bay, nhận phòng khách sạn, giao thông di chuyển và thưởng thức ẩm thực ngân hà.",
            completedCount = 0,
            totalPlanets = 10,
            cefrLevel = "A2 • Đời Sống & Du Lịch"
        ),
        CosmicSectionData(
            sectionNumber = 3,
            startPlanet = 21,
            endPlanet = 30,
            title = "Tinh Vân Vô Cực & Đời Sống Công Sở",
            description = "Làm chủ văn phong email, điều phối cuộc họp, phỏng vấn tuyển dụng, xử lý khiếu nại và thuyết trình báo cáo dữ liệu chuyên nghiệp.",
            completedCount = 0,
            totalPlanets = 10,
            cefrLevel = "B1 - B2 • Công Sở & Dự Án"
        ),
        CosmicSectionData(
            sectionNumber = 4,
            startPlanet = 31,
            endPlanet = 40,
            title = "Quần Thể Sao Hắc Mã & Thuyết Trình C1",
            description = "Khai mở kho tàng thuật ngữ học thuật, đảo ngữ hùng biện, đàm phán hợp đồng cấp cao và tranh biện phản xạ tốc độ cao.",
            completedCount = 0,
            totalPlanets = 10,
            cefrLevel = "C1 • Tinh Hoa & Hùng Biện"
        )
    )

    fun getSectionData(sectionNumber: Int, completedPlanets: Int, totalInThisSection: Int = 10): CosmicSectionData {
        val preset = sectionPresets.getOrNull(sectionNumber - 1)
        val start = (sectionNumber - 1) * 10 + 1
        val end = start + totalInThisSection - 1
        return if (preset != null) {
            preset.copy(
                completedCount = completedPlanets.coerceIn(0, totalInThisSection),
                totalPlanets = totalInThisSection
            )
        } else {
            CosmicSectionData(
                sectionNumber = sectionNumber,
                startPlanet = start,
                endPlanet = end,
                title = "Thiên Hà Chặng $sectionNumber: Mở Rộng Tri Thức",
                description = "Chinh phục các chặng bài học nâng cao để tiến gần hơn tới đỉnh cao giao tiếp toàn diện.",
                completedCount = completedPlanets.coerceIn(0, totalInThisSection),
                totalPlanets = totalInThisSection,
                cefrLevel = "C2 • Bậc Thầy Vũ Trụ"
            )
        }
    }

    fun getAllSections(stages: List<com.example.JourneyStageUiModel> = emptyList()): List<CosmicSectionData> {
        return (1..4).map { secNum ->
            val startIdx = (secNum - 1) * 10
            val sectionStages = stages.drop(startIdx).take(10)
            val completed = sectionStages.count { it.state == com.example.JourneyStageState.COMPLETED }
            val total = if (sectionStages.isNotEmpty()) sectionStages.size else 10
            getSectionData(secNum, completed, total)
        }
    }

    /**
     * Cung cấp bảng màu sống động tương ứng với từng Chặng hành tinh:
     * - Chặng 1: Xanh ngọc lam neon (Hệ Mặt Trời)
     * - Chặng 2: Tím thạch anh neon (Thiên Hà)
     * - Chặng 3: Vàng hổ phách ánh dương (Tinh Vân)
     * - Chặng 4: Lục bảo thiên hà (Quần Thể Sao)
     * - Khác: Hồng ngọc vũ trụ (Vũ Trụ Vô Tận)
     */
    fun getSectionPalette(sectionNumber: Int): CosmicSectionPalette {
        return when (sectionNumber) {
            1 -> CosmicSectionPalette(
                primaryColor = Color(0xFF00F5D4), // Neon Cyan
                secondaryColor = Color(0xFF38BDF8), // Sky Blue
                bgStart = Color(0xF518123A),
                bgMid = Color(0xF5111E48),
                bgEnd = Color(0xF50C283F),
                borderColors = listOf(Color(0xFF00F5D4), Color(0xFF8B5CF6), Color(0xFF38BDF8)),
                glowColor = Color(0x6600F5D4)
            )
            2 -> CosmicSectionPalette(
                primaryColor = Color(0xFFC084FC), // Neon Purple
                secondaryColor = Color(0xFFE879F9), // Fuchsia Pink
                bgStart = Color(0xF5261042),
                bgMid = Color(0xF51E0E38),
                bgEnd = Color(0xF52A123C),
                borderColors = listOf(Color(0xFFC084FC), Color(0xFFE879F9), Color(0xFFA855F7)),
                glowColor = Color(0x66C084FC)
            )
            3 -> CosmicSectionPalette(
                primaryColor = Color(0xFFFBBF24), // Solar Amber / Gold
                secondaryColor = Color(0xFFFB923C), // Sunset Orange
                bgStart = Color(0xF52E1E0F),
                bgMid = Color(0xF524160A),
                bgEnd = Color(0xF5331E0D),
                borderColors = listOf(Color(0xFFFBBF24), Color(0xFFFB923C), Color(0xFFF59E0B)),
                glowColor = Color(0x66FBBF24)
            )
            4 -> CosmicSectionPalette(
                primaryColor = Color(0xFF34D399), // Emerald Mint
                secondaryColor = Color(0xFF2DD4BF), // Teal
                bgStart = Color(0xF50E2E1F),
                bgMid = Color(0xF50A2418),
                bgEnd = Color(0xF5103624),
                borderColors = listOf(Color(0xFF34D399), Color(0xFF2DD4BF), Color(0xFF10B981)),
                glowColor = Color(0x6634D399)
            )
            else -> CosmicSectionPalette(
                primaryColor = Color(0xFFF43F5E), // Ruby Rose
                secondaryColor = Color(0xFFFB7185), // Coral
                bgStart = Color(0xF535101F),
                bgMid = Color(0xF5260A16),
                bgEnd = Color(0xF53C1224),
                borderColors = listOf(Color(0xFFF43F5E), Color(0xFFFB7185), Color(0xFFE11D48)),
                glowColor = Color(0x66F43F5E)
            )
        }
    }
}

/**
 * Banner Chặng học chuẩn phong cách Vũ Trụ Sci-Fi của Kiki:
 * - Ghim cố định ở đầu màn hình (Sticky Header) - KHÔNG BỊ TRÔI MẤT KHI VUỐT LÊN / XUỐNG
 * - Tự động đổi màu sắc và nội dung tượng trưng khi người dùng cuộn đến chặng khác
 * - Hiệu ứng chuyển màu mượt mà giữa các chặng học
 */
@Composable
fun CosmicSectionBanner(
    section: CosmicSectionData,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val progress = (section.completedCount.toFloat() / section.totalPlanets.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(700),
        label = "section_banner_progress"
    )

    val palette = remember(section.sectionNumber) {
        CosmicSectionConfig.getSectionPalette(section.sectionNumber)
    }

    val animatedPrimary by animateColorAsState(targetValue = palette.primaryColor, animationSpec = tween(500), label = "sec_primary")
    val animatedSecondary by animateColorAsState(targetValue = palette.secondaryColor, animationSpec = tween(500), label = "sec_secondary")
    val animatedBgStart by animateColorAsState(targetValue = palette.bgStart, animationSpec = tween(500), label = "sec_bg_start")
    val animatedBgMid by animateColorAsState(targetValue = palette.bgMid, animationSpec = tween(500), label = "sec_bg_mid")
    val animatedBgEnd by animateColorAsState(targetValue = palette.bgEnd, animationSpec = tween(500), label = "sec_bg_end")
    val animatedGlow by animateColorAsState(targetValue = palette.glowColor, animationSpec = tween(500), label = "sec_glow")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = animatedGlow,
                spotColor = animatedPrimary.copy(alpha = 0.5f)
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(animatedBgStart, animatedBgMid, animatedBgEnd)
                    )
                )
                .border(
                    width = 1.3.dp,
                    brush = Brush.horizontalGradient(palette.borderColors),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cột nội dung thông báo chính bên trái
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Tag tiêu đề nhỏ: "CHẶNG X • HÀNH TINH A - B"
                    Text(
                        text = "CHẶNG ${section.sectionNumber} • HÀNH TINH ${section.startPlanet} - ${section.endPlanet}".uppercase(),
                        color = animatedPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Tiêu đề trọng tâm của chặng
                    Text(
                        text = section.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 19.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Thanh tiến độ thông báo của chặng
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(96.dp)
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
                                            listOf(animatedPrimary, animatedSecondary)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${section.completedCount}/${section.totalPlanets} hoàn thành",
                            color = Color(0xCCFFFFFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Đường phân cách mỏng dọc
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .width(1.dp)
                        .height(44.dp)
                        .background(animatedPrimary.copy(alpha = 0.25f))
                )

                // Biểu tượng Stargate mở Galactic Section Drawer / Selector
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(animatedPrimary.copy(alpha = 0.16f))
                        .border(1.2.dp, animatedPrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .then(
                            if (onClick != null) {
                                Modifier.clickable(onClick = onClick)
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = "Cổng dịch chuyển ngân hà",
                            tint = animatedPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "CHẶNG",
                            color = animatedPrimary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cổng Dịch Chuyển Ngân Hà (Cosmic Stargate Checkpoint) giữa các Chặng 10 hành tinh:
 * - Hiển thị trạng thái mở khóa hoặc yêu cầu hoàn thành chặng trước.
 * - Tích hợp Rương thưởng vũ trụ (+100 XP, +50 Sao) khi vượt qua chặng.
 */
@Composable
fun CosmicMilestoneDivider(
    sectionNum: Int,
    isUnlocked: Boolean = true,
    completedInPrev: Int = 10,
    totalInPrev: Int = 10,
    isChestClaimed: Boolean = false,
    onClaimChest: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val palette = remember(sectionNum) { CosmicSectionConfig.getSectionPalette(sectionNum) }
    val sectionData = remember(sectionNum) { CosmicSectionConfig.getSectionData(sectionNum, 0, 10) }

    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isUnlocked) 8.dp else 2.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = if (isUnlocked) palette.glowColor else Color.Transparent,
                    spotColor = if (isUnlocked) palette.primaryColor else Color.Transparent
                ),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isUnlocked) {
                            Brush.horizontalGradient(
                                listOf(palette.bgStart, palette.bgMid, palette.bgEnd)
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(Color(0xEE141224), Color(0xEE18142C), Color(0xEE121020))
                            )
                        }
                    )
                    .border(
                        width = if (isUnlocked) 1.2.dp else 0.8.dp,
                        brush = if (isUnlocked) Brush.horizontalGradient(palette.borderColors) else Brush.horizontalGradient(listOf(Color(0x33FFFFFF), Color(0x22FFFFFF))),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon cổng dịch chuyển bên trái
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isUnlocked) palette.primaryColor.copy(alpha = 0.18f) else Color(0x22FFFFFF))
                            .border(1.dp, if (isUnlocked) palette.primaryColor.copy(alpha = 0.5f) else Color(0x33FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUnlocked) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = palette.primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0x88FFFFFF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Cột thông tin mốc Chặng
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CỔNG DỊCH CHUYỂN • CHẶNG $sectionNum".uppercase(),
                                color = if (isUnlocked) palette.primaryColor else Color(0x88FFFFFF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isUnlocked) palette.primaryColor.copy(alpha = 0.15f) else Color(0x18FFFFFF))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = sectionData.cefrLevel.split("•").firstOrNull()?.trim() ?: "A2",
                                    color = if (isUnlocked) palette.primaryColor else Color(0x99FFFFFF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = sectionData.title,
                            color = if (isUnlocked) Color.White else Color(0xB3FFFFFF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = if (isUnlocked) "✨ Cổng đã mở • Sẵn sàng du hành" else "🔒 Cần hoàn thành ${totalInPrev - completedInPrev} hành tinh nữa ở Chặng ${sectionNum - 1}",
                            color = if (isUnlocked) palette.secondaryColor else Color(0x88FFFFFF),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Rương phần thưởng mốc chặng
                    if (isUnlocked && onClaimChest != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (!isChestClaimed) {
                                        Brush.horizontalGradient(listOf(Color(0xFFFBBF24), Color(0xFFFB923C)))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color(0x33FFFFFF), Color(0x22FFFFFF)))
                                    }
                                )
                                .clickable(enabled = !isChestClaimed) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onClaimChest.invoke()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(R.drawable.ic_reward_chest),
                                    contentDescription = "Rương phần thưởng",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (!isChestClaimed) "+100 XP" else "Đã nhận",
                                    color = if (!isChestClaimed) Color(0xFF0F172A) else Color(0xCCFFFFFF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
