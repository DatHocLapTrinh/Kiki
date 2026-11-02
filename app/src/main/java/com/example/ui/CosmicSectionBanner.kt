package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val totalPlanets: Int = 10
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
            totalPlanets = 10
        ),
        CosmicSectionData(
            sectionNumber = 2,
            startPlanet = 11,
            endPlanet = 20,
            title = "Thiên Hà & Nơi Chốn Du Lịch",
            description = "Tự tin hỏi đường, đặt vé tàu con thoi, giao dịch mua sắm và thưởng thức ẩm thực ngân hà.",
            completedCount = 0,
            totalPlanets = 10
        ),
        CosmicSectionData(
            sectionNumber = 3,
            startPlanet = 21,
            endPlanet = 30,
            title = "Tinh Vân & Đời Sống Công Sở",
            description = "Trình bày quan điểm cá nhân, trao đổi trong môi trường làm việc và tự do đàm thoại lưu loát.",
            completedCount = 0,
            totalPlanets = 10
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
                totalPlanets = totalInThisSection
            )
        }
    }
}

/**
 * Banner Chặng học chuẩn phong cách Vũ Trụ Sci-Fi / Cyberpunk Neon của Kiki:
 * - Thuần túy mang tính chất thông báo định vị chặng cho người dùng, không có popup hay thao tác thừa
 * - Nền kính mờ Cosmic Deep Purple/Navy/Cyan Gradient
 * - Viền Neon phát sáng đa sắc (Cyan - Purple - Sky Blue)
 * - Bên trái: Tag "CHẶNG X • HÀNH TINH A - B" + Tiêu đề chặng to rõ + Thanh tiến độ hoàn thành
 * - Bên phải: Biểu tượng sách trang trí chính thức (R.drawable.ic_open_book)
 */
@Composable
fun CosmicSectionBanner(
    section: CosmicSectionData,
    modifier: Modifier = Modifier
) {
    val progress = (section.completedCount.toFloat() / section.totalPlanets.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(700),
        label = "section_banner_progress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0x6600F5D4),
                spotColor = Color(0x888B5CF6)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xF518123A), // Deep cosmic purple
                            Color(0xF5111E48), // Deep navy
                            Color(0xF50C283F)  // Cyan undertone
                        )
                    )
                )
                .border(
                    width = 1.3.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFF00F5D4), // Neon Cyan
                            Color(0xFF8B5CF6), // Neon Purple
                            Color(0xFF38BDF8)  // Sky Blue
                        )
                    ),
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
                    // Tag tiêu đề nhỏ: "CHẶNG 1 • HÀNH TINH 1 - 10"
                    Text(
                        text = "CHẶNG ${section.sectionNumber} • HÀNH TINH ${section.startPlanet} - ${section.endPlanet}".uppercase(),
                        color = Color(0xFF00F5D4),
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
                                            listOf(Color(0xFF00F5D4), Color(0xFF51FAC1))
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
                        .background(Color(0x3300F5D4))
                )

                // Biểu tượng sách trang trí (thuần túy thông báo hiển thị, không click)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x2200F5D4))
                        .border(1.dp, Color(0x4400F5D4), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_open_book),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
