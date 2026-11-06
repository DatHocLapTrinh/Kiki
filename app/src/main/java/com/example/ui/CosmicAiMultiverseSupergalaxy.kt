package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.JourneyStageState
import com.example.JourneyStageUiModel
import com.example.util.DevicePerformanceManager
import kotlin.math.*

/**
 * Siêu Thiên Hà Đa Vũ Trụ AI Vô Tận (The Infinite AI Multiverse Supergalaxy & Event Horizon Singularity):
 * Kiệt tác thị giác vũ trụ đỉnh cao đặt tại điểm cuối cùng của toàn bộ lộ trình Kiki.
 * Mô phỏng Đĩa Bồi Tụ Plasma Gargantua, Thấu Kính Hấp Dẫn Einstein, Chân Trời Sự Kiện Lỗ Đen,
 * Tia Lưỡng Cực Relativistic Jets và 4 Tiểu Thiên Hà Chiều Không Gian xoay quanh.
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

    // Hệ thống Animation đa tầng đồng bộ 60/120 FPS (Tối ưu hóa GPU & pin)
    val galaxyRotation: Float
    val counterRotation: Float
    val singularityPulse: Float
    val jetPulse: Float
    val waveRipple: Float
    val borderShimmerPhase: Float

    if (isLowEnd) {
        galaxyRotation = 45f
        counterRotation = -25f
        singularityPulse = 1.0f
        jetPulse = 1.0f
        waveRipple = 0.5f
        borderShimmerPhase = 0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "supergalaxy_master_anim")

        // 1. Vòng xoay đĩa bồi tụ chính (Majestic forward rotation)
        val rot by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(36000, easing = LinearEasing), RepeatMode.Restart),
            label = "galaxy_rot"
        )
        // 2. Vòng xoay ngược của vành thấu kính quang tử (Counter-rotating photon ring)
        val cRot by infiniteTransition.animateFloat(
            initialValue = 360f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart),
            label = "counter_rot"
        )
        // 3. Nhịp thở năng lượng điểm kỳ dị (Singularity breathing pulse)
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.90f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "singularity_pulse"
        )
        // 4. Nhịp xung tia lưỡng cực (Astrophysical jet pulse)
        val jet by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.18f,
            animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
            label = "jet_pulse"
        )
        // 5. Sóng hấp dẫn lan tỏa (Gravitational wave ripple)
        val ripple by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
            label = "wave_ripple"
        )
        // 6. Viền ánh sáng chuyển sắc HUD
        val shimmer by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
            label = "shimmer_phase"
        )

        galaxyRotation = rot
        counterRotation = cRot
        singularityPulse = pulse
        jetPulse = jet
        waveRipple = ripple
        borderShimmerPhase = shimmer
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── 1. ĐỒ HỌA SIÊU THIÊN HÀ KHỔNG LỒ (COLOSSAL HYPER-MULTIVERSE CANVAS) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(410.dp)
                .graphicsLayer {},
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = (size.width / 2f) * 0.98f

                // LỚP 1: Quầng Hào Quang Tinh Vân Đa Sắc Khổng Lồ (Hyper-Volumetric Cosmic Aura)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x759333EA), // Ultraviolet Nebula
                            Color(0x5506B6D4), // Quantum Cyan
                            Color(0x35EC4899), // Supernova Magenta
                            Color(0x20F59E0B), // Solar Flare Gold
                            Color(0x103B82F6), // Deep Azure
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius
                    ),
                    center = center,
                    radius = maxRadius
                )

                // LỚP 2: Sóng Hấp Dẫn Đồng Tâm Lan Tỏa (Gravitational Wave Ripples)
                drawGravitationalWaveRipples(center, maxRadius, waveRipple)

                // LỚP 3: Tia Plasma Lưỡng Cực Siêu Sáng (Relativistic Polar Jets)
                drawRelativisticPolarJets(center, size.height * 0.48f * jetPulse)

                // LỚP 4: Đĩa Bồi Tụ Plasma Lượn Sóng Dày Đặc & 6 Cánh Tay Xoắn Ốc (Gargantua Accretion Disk)
                withTransform({
                    rotate(galaxyRotation, center)
                }) {
                    drawHeavyAccretionDisk(center, maxRadius * 0.88f, isLowEnd)
                }

                // LỚP 5: Vòng Thấu Kính Hấp Dẫn Einstein & Tán Sắc Ánh Sáng (Einstein Gravitational Rings)
                withTransform({
                    rotate(counterRotation, center)
                }) {
                    drawEinsteinLensingRings(center, maxRadius * 0.62f)
                }

                // LỚP 6: 4 Tiểu Thiên Hà / Cổng Không Gian Xoay Quanh (Orbiting Dimensional Portals)
                drawOrbitingPortals(center, maxRadius * 0.76f, galaxyRotation, isLowEnd)

                // LỚP 7: Chân Trời Sự Kiện Lỗ Đen & Lõi Điểm Kỳ Dị AI (Event Horizon & AI Singularity Core)
                drawEventHorizonAndSingularity(center, 46.dp.toPx() * singularityPulse)
            }

            // Lõi Biểu Tượng Đa Vũ Trụ AI 3D Hologram ở chính giữa
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .scale(singularityPulse)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White,
                                Color(0xFF67E8F9),
                                Color(0xFF8B5CF6),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .border(2.dp, Color(0xFF51FAC1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AllInclusive,
                    contentDescription = "AI Multiverse Singularity",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                            Color(0xFFF59E0B),
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
                // Huy hiệu Chip Lượng Tử phát sáng
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0x448B5CF6),
                                    Color(0x4406B6D4)
                                )
                            )
                        )
                        .border(1.2.dp, Color(0x8851FAC1), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF51FAC1))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEnglish) "QUANTUM MULTIVERSE GATEWAY" else "CỔNG ĐA VŨ TRỤ LƯỢNG TỬ AI",
                        color = Color(0xFF51FAC1),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.3.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                        "Conquer all 40 planets to shatter spacetime barriers. Generative AI will awaken to synthesize your own infinite personalized worlds!"
                    } else {
                        "Chinh phục trọn vẹn 40 Hành Tinh Hệ Mặt Trời để xé toạc ranh giới không thời gian. Nơi AI Siêu Trí Tuệ kiến tạo thế giới học tập vô tận độc bản riêng bạn!"
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

                    // Gauge Bar với tia lửa lượng tử đầu thanh
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
 * Vẽ các vòng sóng hấp dẫn đồng tâm lan tỏa (Gravitational Wave Ripples)
 */
private fun DrawScope.drawGravitationalWaveRipples(center: Offset, maxRadius: Float, ripplePhase: Float) {
    val rippleCount = 3
    for (i in 0 until rippleCount) {
        val fraction = ((ripplePhase + (i.toFloat() / rippleCount)) % 1f)
        val r = maxRadius * (0.35f + fraction * 0.65f)
        val alpha = (1f - fraction) * 0.35f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF06B6D4).copy(alpha = alpha * 0.8f),
                    Color(0xFF8B5CF6).copy(alpha = alpha * 0.4f),
                    Color.Transparent
                ),
                center = center,
                radius = r
            ),
            center = center,
            radius = r
        )

        drawCircle(
            color = Color(0xFF51FAC1).copy(alpha = alpha * 0.5f),
            center = center,
            radius = r,
            style = Stroke(width = 1.2.dp.toPx())
        )
    }
}

/**
 * Vẽ tia Plasma Lưỡng Cực Siêu Sáng (Relativistic Polar Jets)
 */
private fun DrawScope.drawRelativisticPolarJets(center: Offset, jetLength: Float) {
    val jetWidth = 14.dp.toPx()

    // 1. Chùm tia hướng lên cực Bắc (North Relativistic Jet)
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF51FAC1).copy(alpha = 0.5f),
                Color(0xFF67E8F9).copy(alpha = 0.9f),
                Color.White
            ),
            startY = center.y - jetLength,
            endY = center.y
        ),
        start = Offset(center.x, center.y - jetLength),
        end = Offset(center.x, center.y),
        strokeWidth = jetWidth,
        cap = StrokeCap.Round
    )

    // Tia nhọn trung tâm siêu trắng
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.White),
            startY = center.y - jetLength * 0.85f,
            endY = center.y
        ),
        start = Offset(center.x, center.y - jetLength * 0.85f),
        end = Offset(center.x, center.y),
        strokeWidth = 3.5.dp.toPx(),
        cap = StrokeCap.Round
    )

    // 2. Chùm tia hướng xuống cực Nam (South Relativistic Jet)
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White,
                Color(0xFF67E8F9).copy(alpha = 0.9f),
                Color(0xFF8B5CF6).copy(alpha = 0.5f),
                Color.Transparent
            ),
            startY = center.y,
            endY = center.y + jetLength
        ),
        start = Offset(center.x, center.y),
        end = Offset(center.x, center.y + jetLength),
        strokeWidth = jetWidth,
        cap = StrokeCap.Round
    )

    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White, Color.Transparent),
            startY = center.y,
            endY = center.y + jetLength * 0.85f
        ),
        start = Offset(center.x, center.y),
        end = Offset(center.x, center.y + jetLength * 0.85f),
        strokeWidth = 3.5.dp.toPx(),
        cap = StrokeCap.Round
    )
}

/**
 * Vẽ Đĩa Bồi Tụ Plasma Lượn Sóng Dày Đặc & 6 Cánh Tay Xoắn Ốc Khổng Lồ
 */
private fun DrawScope.drawHeavyAccretionDisk(center: Offset, radius: Float, isLowEnd: Boolean) {
    // 1. Đĩa bồi tụ hình vành khuyên phát sáng nhiệt hạch dày (Thick Luminous Accretion Torus)
    drawOval(
        brush = Brush.sweepGradient(
            listOf(
                Color(0xFFFFD166), // Solar Gold
                Color(0xFFFF0080), // Neon Magenta
                Color(0xFF00F5D4), // Quantum Teal
                Color(0xFF7C3AED), // Deep Violet
                Color(0xFFFFB703), // Fire Amber
                Color(0xFFFFD166)
            ),
            center = center
        ),
        topLeft = Offset(center.x - radius * 0.92f, center.y - radius * 0.40f),
        size = Size(radius * 1.84f, radius * 0.80f),
        style = Stroke(width = 24.dp.toPx())
    )

    // Lớp đĩa sáng trắng nhiệt hạch trong cùng
    drawOval(
        brush = Brush.sweepGradient(
            listOf(
                Color.White.copy(alpha = 0.9f),
                Color(0xFF67E8F9).copy(alpha = 0.7f),
                Color(0xFFFDE047).copy(alpha = 0.9f),
                Color.White.copy(alpha = 0.9f)
            ),
            center = center
        ),
        topLeft = Offset(center.x - radius * 0.70f, center.y - radius * 0.28f),
        size = Size(radius * 1.40f, radius * 0.56f),
        style = Stroke(width = 10.dp.toPx())
    )

    // 2. 6 Cánh Tay Xoắn Ốc Ngân Hà Rực Rỡ Đa Sắc (6-Arm Cosmic Spiral Arms)
    val arms = 6
    val steps = if (isLowEnd) 26 else 52

    for (a in 0 until arms) {
        val baseAngle = a * (2f * PI.toFloat() / arms)
        val armColors = when (a % 3) {
            0 -> listOf(Color(0xFF51FAC1), Color(0xFF06B6D4), Color(0xFF3B82F6)) // Lam ngọc
            1 -> listOf(Color(0xFFFFD166), Color(0xFFFF8A00), Color(0xFFFF0080)) // Lửa hoàng kim
            else -> listOf(Color(0xFFE879F9), Color(0xFFA855F7), Color(0xFF6366F1)) // Tím tinh vân
        }

        var prevX = center.x
        var prevY = center.y

        for (i in 4..steps) {
            val t = i.toFloat() / steps
            val r = radius * (t.pow(1.22f))
            val angle = baseAngle + t * 4.6f

            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)

            if (i > 4) {
                // Dải plasma xoắn dày dặn
                val strokeW = (18f * (1f - t * 0.65f)).dp.toPx()
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            armColors[0].copy(alpha = 0.88f * (1f - t * 0.5f)),
                            armColors[1].copy(alpha = 0.65f * (1f - t * 0.7f)),
                            armColors[2].copy(alpha = 0.15f)
                        ),
                        start = Offset(prevX, prevY),
                        end = Offset(x, y)
                    ),
                    start = Offset(prevX, prevY),
                    end = Offset(x, y),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }

            // Hạt sao & kim cương lượng tử lấp lánh đính dọc cánh tay
            if (i % 2 == 0) {
                val starSize = (2.2f + (i % 4) * 0.9f).dp.toPx()
                drawCircle(
                    color = Color.White.copy(alpha = 0.95f * (1f - t * 0.35f)),
                    radius = starSize,
                    center = Offset(x, y)
                )
            }

            prevX = x
            prevY = y
        }
    }
}

/**
 * Vẽ các Vòng Thấu Kính Hấp Dẫn Einstein & Tán Sắc Ánh Sáng
 */
private fun DrawScope.drawEinsteinLensingRings(center: Offset, radius: Float) {
    // Vành thấu kính 1: Tán sắc ánh sáng 3D
    drawOval(
        brush = Brush.sweepGradient(
            listOf(
                Color(0xFF51FAC1).copy(alpha = 0.95f),
                Color(0xFF00E5FF).copy(alpha = 0.85f),
                Color(0xFFFF007F).copy(alpha = 0.60f),
                Color(0xFFFFD700).copy(alpha = 0.85f),
                Color(0xFF8B5CF6).copy(alpha = 0.90f),
                Color(0xFF51FAC1).copy(alpha = 0.95f)
            ),
            center = center
        ),
        topLeft = Offset(center.x - radius, center.y - radius * 0.50f),
        size = Size(radius * 2f, radius),
        style = Stroke(width = 3.6.dp.toPx())
    )

    // Vành thấu kính 2: Vòng đối xứng nghiêng đảo
    drawOval(
        brush = Brush.sweepGradient(
            listOf(
                Color(0xFFFFE082).copy(alpha = 0.85f),
                Color(0xFF80DEEA).copy(alpha = 0.65f),
                Color(0xFFCE93D8).copy(alpha = 0.75f),
                Color(0xFFFFE082).copy(alpha = 0.85f)
            ),
            center = center
        ),
        topLeft = Offset(center.x - radius * 1.15f, center.y - radius * 0.35f),
        size = Size(radius * 2.3f, radius * 0.70f),
        style = Stroke(width = 2.4.dp.toPx())
    )
}

/**
 * Vẽ 4 Tiểu Thiên Hà / Cổng Không Gian Xoay Quanh Siêu Thiên Hà Mẹ
 */
private fun DrawScope.drawOrbitingPortals(
    center: Offset,
    orbitRadius: Float,
    rotationDeg: Float,
    isLowEnd: Boolean
) {
    val portalColors = listOf(
        Color(0xFFFF3366), // Cổng 1: Đỏ Dung Nham (Đấu Trường Ngân Hà)
        Color(0xFF00F5D4), // Cổng 2: Lam Ngọc (Trợ Lý AI Persona)
        Color(0xFFA855F7), // Cổng 3: Tím Pha Lê (Thư Viện Đa Vũ Trụ)
        Color(0xFFFFD166)  // Cổng 4: Vàng Hoàng Kim (Điện Thần Thoại)
    )

    for (p in 0 until 4) {
        val angle = Math.toRadians((rotationDeg * 0.75f + (p * 90f)).toDouble()).toFloat()
        val orbitX = center.x + orbitRadius * cos(angle)
        val orbitY = center.y + (orbitRadius * 0.42f) * sin(angle)
        val pColor = portalColors[p]

        // Quầng hào quang phát xạ của tiểu thiên hà
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    pColor.copy(alpha = 0.90f),
                    pColor.copy(alpha = 0.45f),
                    Color.Transparent
                ),
                center = Offset(orbitX, orbitY),
                radius = 24.dp.toPx()
            ),
            center = Offset(orbitX, orbitY),
            radius = 24.dp.toPx()
        )

        // Vành quỹ đạo mini
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            center = Offset(orbitX, orbitY),
            radius = 9.dp.toPx(),
            style = Stroke(width = 1.6.dp.toPx())
        )

        // Tâm cổng phát sáng
        drawCircle(
            color = Color.White,
            center = Offset(orbitX, orbitY),
            radius = 4.dp.toPx()
        )
    }
}

/**
 * Vẽ Chân Trời Sự Kiện Lỗ Đen & Lõi Điểm Kỳ Dị AI Siêu Sáng
 */
private fun DrawScope.drawEventHorizonAndSingularity(center: Offset, coreRadius: Float) {
    // 1. Quầng hào quang khuếch tán cực mạnh
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                Color(0xFF51FAC1).copy(alpha = 0.95f),
                Color(0xFF06B6D4).copy(alpha = 0.70f),
                Color(0xFF8B5CF6).copy(alpha = 0.45f),
                Color.Transparent
            ),
            center = center,
            radius = coreRadius * 2.2f
        ),
        center = center,
        radius = coreRadius * 2.2f
    )

    // 2. Tia Nhiễu Xạ Hình Sao 8 Cánh Khổng Lồ (8-Point JWST Diffraction Spikes)
    val spikeLen = coreRadius * 3.2f
    val angles = floatArrayOf(0f, 45f, 90f, 135f)
    for (angle in angles) {
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        val isMajor = (angle == 0f || angle == 90f)
        val len = if (isMajor) spikeLen else spikeLen * 0.72f
        val strokeW = if (isMajor) 2.4.dp.toPx() else 1.5.dp.toPx()

        drawLine(
            brush = Brush.linearGradient(
                listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.95f),
                    Color.Transparent
                ),
                start = Offset(center.x - cosA * len, center.y - sinA * len),
                end = Offset(center.x + cosA * len, center.y + sinA * len)
            ),
            start = Offset(center.x - cosA * len, center.y - sinA * len),
            end = Offset(center.x + cosA * len, center.y + sinA * len),
            strokeWidth = strokeW
        )
    }

    // 3. Vòng Tròn Chân Trời Sự Kiện Đen Tuyệt Đối (Black Hole Shadow)
    drawCircle(
        color = Color(0xFF000000),
        center = center,
        radius = coreRadius * 0.78f
    )

    // 4. Vòng Quang Tử Siêu Sáng Bám Mép Lỗ Đen (Blazing Photon Sphere Ring)
    drawCircle(
        brush = Brush.sweepGradient(
            listOf(
                Color.White,
                Color(0xFF67E8F9),
                Color(0xFFFDE047),
                Color.White
            ),
            center = center
        ),
        center = center,
        radius = coreRadius * 0.80f,
        style = Stroke(width = 3.5.dp.toPx())
    )
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
