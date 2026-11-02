package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
    val grammarTips: List<Pair<String, String>> = emptyList(),
    val keyPhrases: List<Pair<String, String>> = emptyList()
)

object CosmicSectionConfig {
    private val sectionPresets = listOf(
        CosmicSectionData(
            sectionNumber = 1,
            startPlanet = 1,
            endPlanet = 10,
            title = "Chào Hỏi, Làm Quen & Khởi Nguyên Giao Tiếp",
            description = "Làm chủ những mẫu câu nền tảng nhất: chào hỏi, giới thiệu bản thân, gia đình, đồ vật và thời gian trong không gian vũ trụ.",
            completedCount = 0,
            totalPlanets = 10,
            grammarTips = listOf(
                "Đại từ nhân xưng & To Be" to "I am, You are, He/She/It is, We/They are đi kèm tính từ hoặc danh từ.",
                "Thì Hiện tại đơn (Present Simple)" to "Diễn tả thói quen hoặc sự thật hiển nhiên: S + V(s/es) + O.",
                "Cấu trúc câu hỏi Wh-" to "Who, What, Where, When, Why, How + Trợ động từ + S + V?"
            ),
            keyPhrases = listOf(
                "Nice to meet you!" to "Rất vui được gặp bạn!",
                "Where are you from?" to "Bạn đến từ đâu?",
                "Could you speak slower, please?" to "Bạn có thể nói chậm hơn một chút được không?",
                "Have a great day in the galaxy!" to "Chúc bạn một ngày tuyệt vời giữa thiên hà!"
            )
        ),
        CosmicSectionData(
            sectionNumber = 2,
            startPlanet = 11,
            endPlanet = 20,
            title = "Nơi Chốn, Du Lịch & Mua Sắm Không Gian",
            description = "Tự tin hỏi đường, đặt vé tàu con thoi, giao dịch mua sắm và thưởng thức ẩm thực ngân hà.",
            completedCount = 0,
            totalPlanets = 10,
            grammarTips = listOf(
                "Giới từ chỉ nơi chốn" to "In (trong), On (trên), At (tại địa điểm cụ thể), Next to (bên cạnh).",
                "Cách hỏi giá tiền & số lượng" to "How much is this? / How many items do you need?",
                "Thì Hiện tại tiếp diễn" to "S + am/is/are + V-ing diễn tả hành động đang diễn ra ngay lúc nói."
            ),
            keyPhrases = listOf(
                "How much does this cost?" to "Cái này giá bao nhiêu?",
                "Can you show me the way to the station?" to "Bạn có thể chỉ đường cho tôi tới trạm được không?",
                "I would like to order this dish." to "Tôi muốn gọi món này."
            )
        ),
        CosmicSectionData(
            sectionNumber = 3,
            startPlanet = 21,
            endPlanet = 30,
            title = "Công Việc, Đời Sống & Phản Xạ Nâng Cao",
            description = "Trình bày quan điểm cá nhân, trao đổi trong môi trường làm việc và tự do đàm thoại lưu loát.",
            completedCount = 0,
            totalPlanets = 10,
            grammarTips = listOf(
                "Thì Quá khứ đơn (Past Simple)" to "S + V2/ed diễn tả hành động đã hoàn tất trong quá khứ.",
                "Động từ khuyết thiếu (Modal Verbs)" to "Can, Could, Should, Must đi kèm động từ nguyên mẫu không 'to'."
            ),
            keyPhrases = listOf(
                "In my opinion, this plan is feasible." to "Theo ý kiến tôi, kế hoạch này rất khả thi.",
                "Let's schedule a meeting tomorrow." to "Hãy lên lịch một cuộc họp vào ngày mai nhé."
            )
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
                grammarTips = listOf("Củng cố phản xạ" to "Thực hành đều đặn mỗi ngày 1 bài học để duy trì streak."),
                keyPhrases = listOf("Keep up the great work!" to "Tiếp tục phát huy phong độ nhé!")
            )
        }
    }
}

/**
 * Banner Chặng học phong cách Duolingo (như ảnh người dùng yêu cầu):
 * - Nền màu xanh dương tươi sáng năng động
 * - Bên trái: Tag "PHẦN X, CỬA A - B" + Tiêu đề chặng to rõ + Thanh tiến độ mini
 * - Bên phải: Đường kẻ chia cắt + Icon Sổ tay hướng dẫn (Guidebook)
 */
@Composable
fun CosmicSectionBanner(
    section: CosmicSectionData,
    modifier: Modifier = Modifier,
    onOpenGuidebook: (CosmicSectionData) -> Unit
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x330075FF), spotColor = Color(0x5500A2FE)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0096FE), // Xanh dương tươi sáng phong cách Duolingo
                            Color(0xFF0070E6)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    color = Color(0x66BCE0FD),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cột nội dung chính bên trái
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Tag tiêu đề nhỏ: "PHẦN 1, CỬA 1 - 10"
                    Text(
                        text = "PHẦN ${section.sectionNumber}, CỬA ${section.startPlanet} - ${section.endPlanet}".uppercase(),
                        color = Color(0xE6FFFFFF),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Tiêu đề trọng tâm của chặng
                    Text(
                        text = section.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    // Thanh tiến độ mini của chặng
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color(0x3DFFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedProgress)
                                    .clip(CircleShape)
                                    .background(Color(0xFF51FAC1))
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${section.completedCount}/${section.totalPlanets} hành tinh",
                            color = Color(0xE6FFFFFF),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Đường phân cách mỏng dọc
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .width(1.dp)
                        .height(46.dp)
                        .background(Color(0x33FFFFFF))
                )

                // Nút bấm Sổ Tay Hướng Dẫn (Guidebook) bên phải (với biểu tượng gáy sổ lò xo như ảnh)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenGuidebook(section) }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GuidebookNotebookIcon(
                        modifier = Modifier.size(28.dp),
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Biểu tượng quyển sổ tay có 3 khuyên còng lò xo bên trái và 3 gạch dòng kẻ,
 * vẽ trực tiếp chuẩn xác 100% theo ảnh Duolingo người dùng cung cấp.
 */
@Composable
fun GuidebookNotebookIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Thân cuốn sổ bìa chữ nhật bo tròn mép phải
        val bookLeft = w * 0.22f
        val bookTop = h * 0.08f
        val bookWidth = w * 0.72f
        val bookHeight = h * 0.84f
        val corner = 4.dp.toPx()

        drawRoundRect(
            color = color,
            topLeft = Offset(bookLeft, bookTop),
            size = Size(bookWidth, bookHeight),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 2.2.dp.toPx())
        )

        // 3 dòng kẻ ngang trong sổ
        val lineXStart = bookLeft + bookWidth * 0.24f
        val lineXEnd = bookLeft + bookWidth * 0.80f
        val lineY1 = bookTop + bookHeight * 0.30f
        val lineY2 = bookTop + bookHeight * 0.52f
        val lineY3 = bookTop + bookHeight * 0.74f

        drawLine(color = color, start = Offset(lineXStart, lineY1), end = Offset(lineXEnd, lineY1), strokeWidth = 2.dp.toPx())
        drawLine(color = color, start = Offset(lineXStart, lineY2), end = Offset(lineXEnd, lineY2), strokeWidth = 2.dp.toPx())
        drawLine(color = color, start = Offset(lineXStart, lineY3), end = Offset(lineXEnd, lineY3), strokeWidth = 2.dp.toPx())

        // 3 khuyên còng lò xo móc ở gáy sổ bên trái
        val ringRadiusX = w * 0.12f
        val ringHeight = 3.dp.toPx()
        val ringYPositions = listOf(
            bookTop + bookHeight * 0.25f,
            bookTop + bookHeight * 0.50f,
            bookTop + bookHeight * 0.75f
        )

        for (ry in ringYPositions) {
            drawRoundRect(
                color = color,
                topLeft = Offset(bookLeft - ringRadiusX * 0.7f, ry - ringHeight / 2),
                size = Size(ringRadiusX * 1.4f, ringHeight),
                cornerRadius = CornerRadius(ringHeight / 2, ringHeight / 2)
            )
        }
    }
}

/**
 * Hộp thoại Cẩm Nang Hướng Dẫn Chặng Học (Guidebook Dialog)
 */
@Composable
fun CosmicGuidebookDialog(
    section: CosmicSectionData,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, Color(0xFF0096FE), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B142B)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GuidebookNotebookIcon(modifier = Modifier.size(24.dp), color = Color(0xFF00F5D4))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sổ Tay Chặng ${section.sectionNumber}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0x99FFFFFF))
                    }
                }

                Text(
                    text = section.title,
                    color = Color(0xFF51FAC1),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Giới thiệu
                    Text(
                        text = section.description,
                        color = Color(0xCCFFFFFF),
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Ngữ pháp cốt lõi
                    if (section.grammarTips.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Điểm Ngữ Pháp Trọng Tâm",
                                color = Color(0xFFFFD166),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        section.grammarTips.forEach { (rule, exp) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x221E3A8A))
                                    .border(1.dp, Color(0x443B82F6), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(text = rule, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = exp, color = Color(0xB3FFFFFF), fontSize = 12.5.sp, lineHeight = 17.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Mẫu câu giao tiếp
                    if (section.keyPhrases.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mẫu Câu Giao Tiếp Thông Dụng",
                                color = Color(0xFF00F5D4),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        section.keyPhrases.forEach { (en, vi) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x22064E3B))
                                    .border(1.dp, Color(0x4410B981), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(text = en, color = Color(0xFF51FAC1), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = vi, color = Color(0xB3FFFFFF), fontSize = 12.5.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Nút Đóng / Tiếp tục học
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0096FE)
                    )
                ) {
                    Text(
                        text = "Đã hiểu, vào học tiếp!",
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

