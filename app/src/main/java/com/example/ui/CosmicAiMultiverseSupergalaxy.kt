package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.JourneyStageState
import com.example.JourneyStageUiModel
import com.example.R
import com.example.util.DevicePerformanceManager
import kotlin.math.*

/**
 * Siêu Thiên Hà Đa Vũ Trụ AI Vô Tận (The Infinite AI Multiverse Cosmic Gateway Portal):
 * Kiệt tác thị giác vũ trụ học thuật Kiki đỉnh cao đặt tại điểm kết của lộ trình 40 hành tinh.
 * Mô phỏng Cổng Không Gian Lượng Tử hiển thị bức tranh SuperGalaxy với linh vật Kiến Kiki học giả,
 * thiên hà tri thức xoắn ốc rực sáng, các chữ cái neon phát quang và các hạt bụi sao sống động 120 FPS.
 */
@Composable
fun CosmicAiMultiverseSupergalaxyView(
    stages: List<JourneyStageUiModel>,
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier,
    onExploreClick: () -> Unit
) {
    val context = LocalContext.current
    val isLowEnd = remember { DevicePerformanceManager.isLowEnd(context) }
    val haptic = LocalHapticFeedback.current

    val completedStages = stages.count { it.state == JourneyStageState.COMPLETED }
    val totalStages = if (stages.isNotEmpty()) stages.size else 40
    val progressPercent = (completedStages.toFloat() / totalStages).coerceIn(0f, 1f)
    val isFullyUnlocked = completedStages >= totalStages

    // Hệ thống Animation đa tầng mượt mà 60/120 FPS (Tự động thích ứng cấu hình máy)
    val portalBreathingPulse: Float
    val borderShimmerPhase: Float
    val starParticlePhase: Float
    val auraPulsate: Float

    if (isLowEnd) {
        portalBreathingPulse = 1.0f
        borderShimmerPhase = 0f
        starParticlePhase = 0.5f
        auraPulsate = 1.0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "supergalaxy_portal_anim")

        // 1. Nhịp thở năng lượng điện ảnh của Cổng Không Gian (Cinematic Breath: 3.6s)
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.985f,
            targetValue = 1.018f,
            animationSpec = infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "portal_pulse"
        )
        // 2. Viền ánh sáng Holographic chuyển sắc liên tục (Shimmer Glow Phase)
        val shimmer by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1200f,
            animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
            label = "border_shimmer"
        )
        // 3. Pha chuyển động hạt bụi sao & cực quang vi mô (Micro-stardust float phase)
        val stardust by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
            label = "stardust_phase"
        )
        // 4. Nhịp xung quầng hào quang sau lưng cổng (Ambient Backlight Pulse)
        val aura by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "aura_pulse"
        )

        portalBreathingPulse = pulse
        borderShimmerPhase = shimmer
        starParticlePhase = stardust
        auraPulsate = aura
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── 1. CỔNG KHÔNG GIAN TRI THỨC ĐA VŨ TRỤ AI (HOLOGRAPHIC MULTIVERSE PORTAL) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .graphicsLayer {},
            contentAlignment = Alignment.Center
        ) {
            // Lớp 1.1: Quầng Hào Quang Tinh Vân Vũ Trụ Sau Lưng Cổng (Ambient Nebula Backlight)
            Canvas(
                modifier = Modifier
                    .size(410.dp)
                    .graphicsLayer {
                        scaleX = auraPulsate
                        scaleY = auraPulsate
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x667C3AED), // Cosmic Violet
                            Color(0x4406B6D4), // Quantum Cyan
                            Color(0x33FFB703), // Starlight Gold
                            Color(0x22EC4899), // Nebula Pink
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius
                    ),
                    center = center,
                    radius = maxRadius
                )
            }

            // Lớp 1.2: Khung Cổng Không Gian Lượng Tử Bo Tròn Lộng Lẫy (Hologram Portal Frame)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .height(420.dp)
                    .scale(portalBreathingPulse)
                    .clip(RoundedCornerShape(34.dp))
                    .background(Color(0xFF0B061A))
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF00F5D4), // Cyan Laser
                                Color(0xFF8B5CF6), // Royal Purple
                                Color(0xFFFFD700), // Starlight Gold
                                Color(0xFFFF007F), // Cyber Magenta
                                Color(0xFF00F5D4)
                            ),
                            start = Offset(borderShimmerPhase, 0f),
                            end = Offset(borderShimmerPhase + 600f, 600f)
                        ),
                        shape = RoundedCornerShape(34.dp)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onExploreClick()
                    }
            ) {
                // Tác Phẩm Nghệ Thuật SuperGalaxy Kiki Siêu Nét
                Image(
                    painter = painterResource(id = R.drawable.img_kiki_supergalaxy_portal),
                    contentDescription = "Kiki SuperGalaxy Multiverse Portal",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Lớp Gradient Che Phủ Mờ Nhẹ Ở Đáy
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xB0070314)
                                )
                            )
                        )
                )

                // Lớp Hạt Bụi Sao & Tia Sáng Sống Động Bay Lơ Lửng Trên Cổng (Floating Stardust Layer)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawPortalFloatingStardust(
                        width = size.width,
                        height = size.height,
                        phase = starParticlePhase,
                        isLowEnd = isLowEnd
                    )
                }

                // Huy hiệu Cổng Chiều Không Gian Ở Đỉnh Cổng
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xCC070314))
                        .border(1.2.dp, Color(0xAA00F5D4), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00F5D4))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEnglish) "KIKI AI MULTIVERSE GATEWAY" else "CỔNG ĐA VŨ TRỤ TRI THỨC KIKI",
                        color = Color(0xFF00F5D4),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }

                // Pill Nổi Bật Ở Đáy Cổng: Chạm Để Khám Phá
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xEE1E1538),
                                    Color(0xEE120A24)
                                )
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00F5D4), Color(0xFF8B5CF6), Color(0xFFFFD700))
                            ),
                            RoundedCornerShape(22.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF51FAC1),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEnglish) "TAP TO EXPLORE AI MULTIVERSE" else "CHẠM ĐỂ CHIÊM NGƯỠNG ĐA VŨ TRỤ AI",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        // Đệm an toàn ở đáy để không bị thanh điều hướng Bottom Navigation Bar che khuất
        Spacer(modifier = Modifier.height(140.dp))
    }
}

/**
 * Vẽ các hạt bụi sao & tia sáng lơ lửng trên cổng không gian (Floating Stardust Particles)
 */
private fun DrawScope.drawPortalFloatingStardust(
    width: Float,
    height: Float,
    phase: Float,
    isLowEnd: Boolean
) {
    val count = if (isLowEnd) 20 else 46

    for (i in 0 until count) {
        // Tọa độ hạt phân bố đều quanh khu vực thiên hà và chú kiến Kiki
        val seedX = (sin(i * 91.3f) * 0.5f + 0.5f)
        val seedY = (cos(i * 47.7f) * 0.5f + 0.5f)

        // Bay nhẹ hướng lên trên theo phase
        val curY = ((seedY * height - phase * 60.dp.toPx() + height) % height)
        val curX = seedX * width + sin(phase * 2f * PI.toFloat() + i) * 10.dp.toPx()

        // Nhấp nháy luân phiên
        val starAlpha = 0.35f + 0.65f * (0.5f + 0.5f * sin((phase + i * 0.2f) * 2f * PI.toFloat()))
        val starRadius = (1.4f + (i % 3) * 0.8f).dp.toPx()

        val color = when (i % 4) {
            0 -> Color(0xFF00F5D4) // Cyan
            1 -> Color(0xFFFFD700) // Gold
            2 -> Color(0xFFFF007F) // Magenta
            else -> Color.White
        }

        // Quầng phát sáng
        drawCircle(
            color = color.copy(alpha = starAlpha * 0.4f),
            radius = starRadius * 2.2f,
            center = Offset(curX, curY)
        )
        // Tâm hạt sao
        drawCircle(
            color = Color.White.copy(alpha = starAlpha),
            radius = starRadius,
            center = Offset(curX, curY)
        )
    }
}

/**
 * Hộp Thoại Trình Diễn Đa Vũ Trụ AI Tuyệt Đẹp (Interactive Multiverse Preview Dialog):
 * Thiết kế Lưới 2x2 Holographic Grid cân đối hoàn hảo, hiển thị đồng thời trọn vẹn cả 4 đặc quyền AI,
 * hỗ trợ cuộn mượt mà không bao giờ bị cắt xén hay che khuất.
 */
@Composable
fun CosmicMultiversePreviewDialog(
    isEnglish: Boolean = false,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF120D24),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .border(
                2.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0xFF51FAC1),
                        Color(0xFF00F5D4),
                        Color(0xFF8B5CF6),
                        Color(0xFFFFD700),
                        Color(0xFFEC4899),
                        Color(0xFF51FAC1)
                    )
                ),
                RoundedCornerShape(28.dp)
            ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AllInclusive,
                    contentDescription = null,
                    tint = Color(0xFF51FAC1),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEnglish) "THE AI MULTIVERSE" else "ĐA VŨ TRỤ AI VÔ TẬN",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                    letterSpacing = 0.8.sp
                )
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Banner SuperGalaxy Kiki Điện Ảnh Thu Nhỏ
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.2.dp, Color(0x6651FAC1), RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_kiki_supergalaxy_portal),
                        contentDescription = "Multiverse Preview Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0x77120D24)
                                    )
                                )
                            )
                    )
                }

                // 2. Dòng Dẫn Dắt Ngắn Gọn Sắc Nét
                Text(
                    text = if (isEnglish) {
                        "Conquer 40 planets to unlock all 4 God-Tier AI Privileges:"
                    } else {
                        "Khám phá trọn vẹn 4 đặc quyền tối thượng của Đa Vũ Trụ AI:"
                    },
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. LƯỚI 2x2 CẢ 4 ĐẶC QUYỀN VŨ TRỤ (HIỂN THỊ ĐẦY ĐỦ 100%, KHÔNG BỊ KHUẤT)
                // Hàng 1: Vũ Trụ AI & Persona Sống
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MultiversePrivilegeGridCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AutoFixHigh,
                        accentColor = Color(0xFF51FAC1),
                        title = if (isEnglish) "AI Worlds" else "Vũ Trụ AI",
                        desc = if (isEnglish) "Generate custom courses from prompts" else "Tự tạo hành tinh học từ mọi sở thích"
                    )
                    MultiversePrivilegeGridCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.RecordVoiceOver,
                        accentColor = Color(0xFF00F5D4),
                        title = if (isEnglish) "Live Persona" else "Persona Sống",
                        desc = if (isEnglish) "Real-time 1-on-1 spoken AI practice" else "Luyện nói phản xạ 1-1 tự nhiên"
                    )
                }

                // Hàng 2: Gia Tốc 500% & Điện Bất Tử
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MultiversePrivilegeGridCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Bolt,
                        accentColor = Color(0xFFFFD700),
                        title = if (isEnglish) "500% Boost" else "Gia Tốc 500%",
                        desc = if (isEnglish) "5X multiplier for XP, Mana & Stars" else "Nhân 5 tốc độ XP & Mana"
                    )
                    MultiversePrivilegeGridCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WorkspacePremium,
                        accentColor = Color(0xFFEC4899),
                        title = if (isEnglish) "Immortals" else "Điện Bất Tử",
                        desc = if (isEnglish) "Galactic Hall of Fame & Crowns" else "Vương miện & Bảng Vàng vũ trụ"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = if (isEnglish) "I Will Conquer It! 🚀" else "Tôi Sẽ Chinh Phục! 🚀",
                    color = Color(0xFF092E20),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.5.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    )
}

/**
 * Thẻ Đặc Quyền Vũ Trụ Lưới 2x2 Cực Đẹp (Hologram Privilege Grid Card)
 */
@Composable
private fun MultiversePrivilegeGridCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1D1733),
                        Color(0xFF140F26)
                    )
                )
            )
            .border(
                1.2.dp,
                Brush.verticalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.85f),
                        accentColor.copy(alpha = 0.20f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 12.5.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = desc,
                color = Color(0xFF94A3B8),
                fontSize = 10.5.sp,
                lineHeight = 13.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
