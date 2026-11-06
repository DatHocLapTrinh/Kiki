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
import androidx.compose.ui.graphics.drawscope.withTransform
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
 * Siêu Thiên Hà Đa Vũ Trụ AI Vô Tận (The Infinite AI Multiverse Grand Spiral Supergalaxy):
 * Kiệt tác thị giác thiên văn siêu thực chuẩn kính viễn vọng không gian Hubble / James Webb (M51 Whirlpool Galaxy).
 * Kết hợp thân thiên hà xoắn ốc photorealistic chân thực, lõi sao vàng ngọc trai rực rỡ,
 * các vườn ươm sao hồng ngọc (H-alpha ruby nebulae), bụi kim cương xanh băng lấp lánh,
 * tia nhiễu xạ sao James Webb và 4 tiểu thiên hà vệ tinh quay quanh.
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

    // Hệ thống Animation đa tầng mượt mà 60/120 FPS
    val galaxyRotation: Float
    val counterRotation: Float
    val coreBreathingPulse: Float
    val starTwinklePhase: Float
    val borderShimmerPhase: Float

    if (isLowEnd) {
        galaxyRotation = 45f
        counterRotation = -20f
        coreBreathingPulse = 1.0f
        starTwinklePhase = 0.5f
        borderShimmerPhase = 0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "grand_spiral_galaxy_anim")

        // 1. Vòng xoay chậm rãi, kỳ vĩ của thiên hà xoắn ốc (Majestic Slow Cosmic Rotation: 45s/vòng)
        val rot by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(45000, easing = LinearEasing), RepeatMode.Restart),
            label = "galaxy_rot"
        )
        // 2. Vòng xoay ngược của tia tán xạ quang học James Webb (JWST Diffraction counter-rotation)
        val cRot by infiniteTransition.animateFloat(
            initialValue = 360f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
            label = "counter_rot"
        )
        // 3. Nhịp thở năng lượng ánh sao của lõi thiên hà (Galactic Core Breathing)
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "core_pulse"
        )
        // 4. Nhịp chớp sáng nhấp nháy của các cụm tinh vân sao non (Nebula twinkle phase)
        val twinkle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
            label = "star_twinkle"
        )
        // 5. Viền ánh sáng chuyển sắc HUD Hologram
        val shimmer by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
            label = "hud_shimmer"
        )

        galaxyRotation = rot
        counterRotation = cRot
        coreBreathingPulse = pulse
        starTwinklePhase = twinkle
        borderShimmerPhase = shimmer
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── 1. ĐỒ HỌA SIÊU THIÊN HÀ XOẮN ỐC WHIRLPOOL PHOTOREALISTIC KHỔNG LỒ ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .graphicsLayer {},
            contentAlignment = Alignment.Center
        ) {
            // Lớp 1: Quầng Hào Quang Tinh Vân Vũ Trụ Sâu (Deep Space Ambient Nebula Glow)
            Canvas(
                modifier = Modifier
                    .size(410.dp)
                    .graphicsLayer {
                        scaleX = coreBreathingPulse
                        scaleY = coreBreathingPulse
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f

                // Hào quang vàng hoàng kim starlight ấm áp từ tâm tỏa ra
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x55FFE082), // Warm Starlight Gold
                            Color(0x35FF8A65), // Coral Amber Dust
                            Color(0x258B5CF6), // Cosmic Violet
                            Color(0x1206B6D4), // Deep Cyan
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius
                    ),
                    center = center,
                    radius = maxRadius
                )
            }

            // Lớp 2: Thân Thiên Hà Xoắn Ốc Photorealistic M51 (Xoay chuyển động chân thực trên GPU)
            Image(
                painter = painterResource(id = R.drawable.img_whirlpool_supergalaxy),
                contentDescription = "Grand Spiral Whirlpool Galaxy",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(390.dp)
                    .graphicsLayer {
                        rotationZ = galaxyRotation
                        scaleX = coreBreathingPulse
                        scaleY = coreBreathingPulse
                    }
            )

            // Lớp 3: Canvas Hiệu Ứng Ánh Sao Sống Động (Starlight Spikes, Ruby Nebulae, Satellites)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val coreRadius = 28.dp.toPx() * coreBreathingPulse

                // 3.1. Lõi sao trung tâm siêu sáng (Super-Luminous Galactic Starlight Core)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFFFF9C4),
                            Color(0xFFFFD54F).copy(alpha = 0.85f),
                            Color(0xFFFF8F00).copy(alpha = 0.40f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = coreRadius * 1.6f
                    ),
                    center = center,
                    radius = coreRadius * 1.6f
                )

                // 3.2. Tia Nhiễu Xạ Hình Sao James Webb 4 Cánh Lấp Lánh (JWST 4-Point Starlight Diffraction Spikes)
                withTransform({
                    rotate(counterRotation, center)
                }) {
                    drawStarlightDiffractionSpikes(center, spikeLength = coreRadius * 3.4f)
                }

                // 3.3. Các Vườn Ươm Sao Hồng Ruby & Cụm Sao Xanh Băng Lấp Lánh (Dọc theo các nhánh xoắn ốc)
                withTransform({
                    rotate(galaxyRotation, center)
                }) {
                    drawDynamicStellarNurseries(
                        center = center,
                        maxRadius = 180.dp.toPx(),
                        twinklePhase = starTwinklePhase,
                        isLowEnd = isLowEnd
                    )
                }

                // 3.4. 4 Tiểu Thiên Hà Vệ Tinh (Orbiting Satellite Dwarf Galaxies)
                drawOrbitingSatelliteGalaxies(
                    center = center,
                    orbitRadius = 192.dp.toPx(),
                    rotationDeg = galaxyRotation,
                    isLowEnd = isLowEnd
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
 * Vẽ tia Nhiễu Xạ Hình Sao James Webb 4 Cánh Lấp Lánh (JWST 4-Point Starlight Diffraction Spikes)
 */
private fun DrawScope.drawStarlightDiffractionSpikes(center: Offset, spikeLength: Float) {
    val angles = floatArrayOf(0f, 90f) // Tia chữ thập thanh mảnh chính xác
    for (angle in angles) {
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)

        // Tia sáng chính
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFFFF9C4).copy(alpha = 0.95f),
                    Color.White,
                    Color(0xFFFFF9C4).copy(alpha = 0.95f),
                    Color.Transparent
                ),
                start = Offset(center.x - cosA * spikeLength, center.y - sinA * spikeLength),
                end = Offset(center.x + cosA * spikeLength, center.y + sinA * spikeLength)
            ),
            start = Offset(center.x - cosA * spikeLength, center.y - sinA * spikeLength),
            end = Offset(center.x + cosA * spikeLength, center.y + sinA * spikeLength),
            strokeWidth = 2.2.dp.toPx()
        )

        // Tia mờ phụ 45 độ ngắn hơn
        val diagRad = Math.toRadians((angle + 45f).toDouble()).toFloat()
        val cosD = cos(diagRad)
        val sinD = sin(diagRad)
        val diagLen = spikeLength * 0.55f
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFFFD54F).copy(alpha = 0.60f),
                    Color.Transparent
                ),
                start = Offset(center.x - cosD * diagLen, center.y - sinD * diagLen),
                end = Offset(center.x + cosD * diagLen, center.y + sinD * diagLen)
            ),
            start = Offset(center.x - cosD * diagLen, center.y - sinD * diagLen),
            end = Offset(center.x + cosD * diagLen, center.y + sinD * diagLen),
            strokeWidth = 1.2.dp.toPx()
        )
    }
}

/**
 * Vẽ các Vườn Ươm Sao Hồng Ruby (H-II Emission Nebulae) & Cụm Sao Trẻ Xanh Băng lấp lánh dọc nhánh xoắn ốc
 */
private fun DrawScope.drawDynamicStellarNurseries(
    center: Offset,
    maxRadius: Float,
    twinklePhase: Float,
    isLowEnd: Boolean
) {
    val starCount = if (isLowEnd) 32 else 72

    // Tọa độ hạt mô phỏng 2 nhánh xoắn ốc logarithm chính của Whirlpool Galaxy M51
    for (i in 0 until starCount) {
        val arm = i % 2 // 2 nhánh chính
        val t = (i.toFloat() / starCount)
        val baseAngle = arm * PI.toFloat() + t * 4.2f
        val r = maxRadius * (0.22f + t * 0.72f)

        // Phân tán nhẹ quanh nhánh xoắn ốc
        val jitterRadius = (sin(i * 12.3f) * 12.dp.toPx())
        val jitterAngle = cos(i * 7.7f) * 0.25f

        val finalR = (r + jitterRadius).coerceIn(20.dp.toPx(), maxRadius)
        val finalAngle = baseAngle + jitterAngle

        val x = center.x + finalR * cos(finalAngle)
        val y = center.y + finalR * sin(finalAngle)

        // Hiệu ứng nhấp nháy luân phiên
        val individualPhase = (twinklePhase + (i * 0.17f)) % 1f
        val alpha = 0.35f + 0.65f * (0.5f + 0.5f * sin(individualPhase * 2f * PI.toFloat()))

        // Màu sắc: 60% cụm tinh vân hồng ngọc (H-alpha), 40% cụm sao xanh băng (O/B stars)
        val isRuby = (i % 5 != 0)
        val dotColor = if (isRuby) {
            Color(0xFFFF2A6D).copy(alpha = alpha) // Bright Ruby Pink
        } else {
            Color(0xFF00E5FF).copy(alpha = alpha) // Icy Cyan Blue
        }

        val dotRadius = if (isRuby) {
            (1.8f + (i % 3) * 0.9f).dp.toPx()
        } else {
            (1.4f + (i % 2) * 0.7f).dp.toPx()
        }

        // Vòng quầng sáng mờ quanh hạt sao
        drawCircle(
            color = dotColor.copy(alpha = alpha * 0.45f),
            radius = dotRadius * 2.2f,
            center = Offset(x, y)
        )
        // Tâm hạt sao sắc nét
        drawCircle(
            color = if (isRuby) Color(0xFFFF80AB) else Color.White,
            radius = dotRadius,
            center = Offset(x, y)
        )
    }
}

/**
 * Vẽ 4 Tiểu Thiên Hà Vệ Tinh (Satellite Dwarf Galaxies) quay quanh ở rìa ngoài
 */
private fun DrawScope.drawOrbitingSatelliteGalaxies(
    center: Offset,
    orbitRadius: Float,
    rotationDeg: Float,
    isLowEnd: Boolean
) {
    val portalColors = listOf(
        Color(0xFFFF3366), // Tiểu thiên hà 1: Đỏ Dung Nham (Đấu Trường Ngân Hà)
        Color(0xFF00F5D4), // Tiểu thiên hà 2: Lam Ngọc (Trợ Lý AI Persona)
        Color(0xFFA855F7), // Tiểu thiên hà 3: Tím Pha Lê (Thư Viện Đa Vũ Trụ)
        Color(0xFFFFD166)  // Tiểu thiên hà 4: Vàng Hoàng Kim (Điện Thần Thoại)
    )

    for (p in 0 until 4) {
        val angle = Math.toRadians((rotationDeg * 0.70f + (p * 90f)).toDouble()).toFloat()
        val orbitX = center.x + orbitRadius * cos(angle)
        val orbitY = center.y + (orbitRadius * 0.50f) * sin(angle)
        val pColor = portalColors[p]

        // Quầng hào quang phát xạ của tiểu thiên hà
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    pColor.copy(alpha = 0.85f),
                    pColor.copy(alpha = 0.35f),
                    Color.Transparent
                ),
                center = Offset(orbitX, orbitY),
                radius = 20.dp.toPx()
            ),
            center = Offset(orbitX, orbitY),
            radius = 20.dp.toPx()
        )

        // Vành xoắn ốc mini của tiểu thiên hà
        drawCircle(
            color = Color.White.copy(alpha = 0.75f),
            center = Offset(orbitX, orbitY),
            radius = 7.dp.toPx(),
            style = Stroke(width = 1.4.dp.toPx())
        )

        // Tâm tiểu thiên hà phát sáng
        drawCircle(
            color = Color.White,
            center = Offset(orbitX, orbitY),
            radius = 3.5.dp.toPx()
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
