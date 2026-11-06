package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
                    .height(390.dp)
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
            ) {
                // Tác Phẩm Nghệ Thuật SuperGalaxy Kiki Siêu Nét
                Image(
                    painter = painterResource(id = R.drawable.img_kiki_supergalaxy_portal),
                    contentDescription = "Kiki SuperGalaxy Multiverse Portal",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Lớp Gradient Che Phủ Mờ Dần Ở Đáy Để Chuyển Tiếp Sang Card HUD
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xA00B061A),
                                    Color(0xF0070314)
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
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── 2. THẺ HOLOGRAPHIC VIP TRẠM CỔNG ĐA VŨ TRỤ (LUXURY QUANTUM GATEWAY HUD) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF0181133),
                            Color(0xF80F0A24),
                            Color(0xFF070414)
                        )
                    )
                )
                .border(
                    2.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6),
                            Color(0xFF06B6D4),
                            Color(0xFF51FAC1),
                            Color(0xFFFFD700),
                            Color(0xFFEC4899),
                            Color(0xFF8B5CF6)
                        ),
                        start = Offset(borderShimmerPhase, 0f),
                        end = Offset(borderShimmerPhase + 600f, 600f)
                    ),
                    RoundedCornerShape(30.dp)
                )
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isEnglish) "The Final Apex of Multiverse Wisdom" else "Đỉnh Cao Tri Thức Vũ Trụ Tuyệt Đối",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isEnglish) {
                        "Conquer all 40 solar planets to awaken the Infinite AI Multiverse. Mentor Kiki stands by your side to explore boundless knowledge worlds!"
                    } else {
                        "Chinh phục trọn vẹn 40 Hành Tinh Hệ Mặt Trời để kích hoạt Đa Vũ Trụ AI Vô Tận. Nơi Kiến Kiki đồng hành cùng bạn khám phá các thế giới tri thức bất tận!"
                    },
                    color = Color(0xFFE2E8F0),
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4 Huy hiệu đặc quyền Đa Vũ Trụ AI mini
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MultiverseMiniFeatureChip(
                        icon = Icons.Default.AutoAwesome,
                        color = Color(0xFF51FAC1),
                        label = if (isEnglish) "AI Worlds" else "Vũ Trụ AI"
                    )
                    MultiverseMiniFeatureChip(
                        icon = Icons.Default.RecordVoiceOver,
                        color = Color(0xFF06B6D4),
                        label = if (isEnglish) "Living Voice" else "Hội Thoại Sống"
                    )
                    MultiverseMiniFeatureChip(
                        icon = Icons.Default.Bolt,
                        color = Color(0xFFFFB703),
                        label = if (isEnglish) "Infinite XP" else "Cấp Độ Vô Hạn"
                    )
                    MultiverseMiniFeatureChip(
                        icon = Icons.Default.WorkspacePremium,
                        color = Color(0xFFEC4899),
                        label = if (isEnglish) "Immortals" else "Điện Bất Tử"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Thanh Năng Lượng Lỗ Sâu Lượng Tử (Quantum Singularity Warp Gauge)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isFullyUnlocked) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = if (isFullyUnlocked) Color(0xFF51FAC1) else Color(0xFF67E8F9),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFullyUnlocked) {
                                    (if (isEnglish) "Quantum Gateway Stabilized" else "Cổng Không Gian Đã Mở Khóa")
                                } else {
                                    (if (isEnglish) "Singularity Charge" else "Năng Lượng Lỗ Sâu Tích Tụ")
                                },
                                color = if (isFullyUnlocked) Color(0xFF51FAC1) else Color(0xFF67E8F9),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "$completedStages/$totalStages (${(progressPercent * 100).toInt()}%)",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Gauge Bar với gradient đa sắc
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF1E1738))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressPercent)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF8B5CF6),
                                            Color(0xFF06B6D4),
                                            Color(0xFF51FAC1),
                                            Color(0xFFFFD166)
                                        )
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Nút Bấm Siêu Phẩm Phát Sáng
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onExploreClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFullyUnlocked) Color(0xFF51FAC1) else Color(0xFF8B5CF6)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isFullyUnlocked) Icons.Default.RocketLaunch else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isFullyUnlocked) Color(0xFF092E20) else Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isFullyUnlocked) {
                                (if (isEnglish) "ENTER THE MULTIVERSE" else "BƯỚC VÀO ĐA VŨ TRỤ AI")
                            } else {
                                (if (isEnglish) "EXPLORE MULTIVERSE SPECTRUM" else "CHIÊM NGƯỠNG ĐA VŨ TRỤ AI")
                            },
                            color = if (isFullyUnlocked) Color(0xFF092E20) else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 1.1.sp
                        )
                    }
                }
            }
        }

        // Đệm an toàn 180dp ở đáy để KHÔNG BAO GIỜ bị che khuất bởi thanh điều hướng Bottom Navigation Bar!
        Spacer(modifier = Modifier.height(180.dp))
    }
}

/**
 * Chip hiển thị đặc quyền Đa Vũ Trụ mini
 */
@Composable
private fun MultiverseMiniFeatureChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x221E193C))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color(0xFFCBD5E1),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
        )
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
 * Hộp Thoại Trình Diễn Đa Vũ Trụ AI Tuyệt Đẹp (Interactive Multiverse Preview Dialog)
 */
@Composable
fun CosmicMultiversePreviewDialog(
    isEnglish: Boolean = false,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF140F28),
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .border(
                2.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0xFF51FAC1),
                        Color(0xFF06B6D4),
                        Color(0xFF8B5CF6),
                        Color(0xFFEC4899),
                        Color(0xFFFFD166)
                    )
                ),
                RoundedCornerShape(32.dp)
            ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AllInclusive,
                    contentDescription = null,
                    tint = Color(0xFF51FAC1),
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isEnglish) "The Infinite AI Multiverse" else "Đa Vũ Trụ AI Vô Tận",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 21.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Banner SuperGalaxy Kiki Thu Nhỏ Ở Đầu Dialog
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.5.dp, Color(0x6651FAC1), RoundedCornerShape(18.dp))
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
                                        Color(0x99140F28)
                                    )
                                )
                            )
                    )
                }

                Text(
                    text = if (isEnglish) {
                        "Congratulations on reaching the ultimate cosmic horizon! Here are the 4 God-Tier Privileges awaiting you in the AI Multiverse:"
                    } else {
                        "Chào mừng bạn đã chạm đến đỉnh cao vũ trụ! Đây là 4 đặc quyền tối thượng của Đa Vũ Trụ AI đang chờ bạn kích hoạt:"
                    },
                    color = Color(0xFFE2E8F0),
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )

                MultiverseFeatureRow(
                    icon = Icons.Default.AutoFixHigh,
                    iconTint = Color(0xFF51FAC1),
                    title = if (isEnglish) "Autonomous AI Worlds" else "Tự Sinh Thế Giới Học Tập Vô Tận",
                    desc = if (isEnglish) "Generate any course from your wildest prompts." else "Nhập bất kỳ chủ đề hay sở thích nào, AI sẽ tự kiến tạo hành tinh học tập riêng."
                )

                MultiverseFeatureRow(
                    icon = Icons.Default.RecordVoiceOver,
                    iconTint = Color(0xFF06B6D4),
                    title = if (isEnglish) "Living AI Personas" else "Hội Thoại Thời Gian Thực Với AI Persona",
                    desc = if (isEnglish) "Converse in real-time with cosmic intelligent mentors." else "Trò chuyện tương tác phản xạ tự nhiên 1-1 không độ trễ như người bản xứ."
                )

                MultiverseFeatureRow(
                    icon = Icons.Default.Bolt,
                    iconTint = Color(0xFFFFB703),
                    title = if (isEnglish) "500% Supercharged XP" else "Gia Tốc Lượng Tử 500% XP & Mana",
                    desc = if (isEnglish) "Ascend to cosmic godhood at unprecedented speed." else "Tích lũy kinh nghiệm siêu tốc để thăng hạng liên ngân hà."
                )

                MultiverseFeatureRow(
                    icon = Icons.Default.WorkspacePremium,
                    iconTint = Color(0xFFEC4899),
                    title = if (isEnglish) "Hall of Galactic Immortals" else "Điện Thờ Bất Tử Đa Vũ Trụ",
                    desc = if (isEnglish) "Immortalize your name across the cosmic leaderboards." else "Khắc tên vĩnh hằng trên Bảng Vàng Ngân Hà và nhận vương miện AI Hoàng Kim."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = if (isEnglish) "I Will Conquer It!" else "Tôi Sẽ Chinh Phục!",
                    color = Color(0xFF092E20),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }
        }
    )
}

@Composable
private fun MultiverseFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x331E1838))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            )
            Text(
                text = desc,
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp,
                lineHeight = 15.sp
            )
        }
    }
}
