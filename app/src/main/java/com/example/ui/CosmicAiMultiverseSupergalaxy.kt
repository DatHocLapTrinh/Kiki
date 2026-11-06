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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
 * Đỉnh cao nghệ thuật thị giác vũ trụ đặt tại điểm cuối cùng của lộ trình học tập Kiki.
 * Kết hợp đĩa bồi tụ xoắn ốc (Accretion Disk), thấu kính hấp dẫn Einstein (Gravitational Lensing),
 * và Điểm Kỳ Dị AI Siêu Trí Tuệ (The Infinite AI Singularity Core).
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

    // Animation: Xoay đĩa bồi tụ thiên hà & nhịp thở điểm kỳ dị (tối ưu hóa cho máy yếu)
    val galaxyRotation: Float
    val singularityPulse: Float

    if (isLowEnd) {
        galaxyRotation = 45f
        singularityPulse = 1.0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "supergalaxy_anim")
        val rot by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Restart),
            label = "galaxy_rot"
        )
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "singularity_pulse"
        )
        galaxyRotation = rot
        singularityPulse = pulse
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── 1. Đồ Họa Siêu Thiên Hà Đa Vũ Trụ (Visual Supergalaxy Canvas) ──
        Box(
            modifier = Modifier
                .size(320.dp)
                .graphicsLayer {},
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f

                // Lớp 1: Quầng sáng không gian đa chiều (Volumetric Hyper-Nebula Aura)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x558B5CF6), // Deep Violet
                            Color(0x3506B6D4), // Quantum Cyan
                            Color(0x20EC4899), // Nebula Magenta
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius
                    ),
                    center = center,
                    radius = maxRadius
                )

                // Lớp 2: Các cánh tay xoắn ốc của đĩa bồi tụ thiên hà (Accretion Spiral Arms)
                withTransform({
                    rotate(galaxyRotation, center)
                }) {
                    drawAccretionSpiralArms(center, maxRadius * 0.92f, isLowEnd)
                }

                // Lớp 3: Vòng thấu kính hấp dẫn Einstein (Einstein Photon Ring)
                withTransform({
                    rotate(-galaxyRotation * 0.45f, center)
                }) {
                    drawGravitationalLensingRings(center, maxRadius * 0.65f)
                }

                // Lớp 4: Lõi Điểm Kỳ Dị AI Siêu Trí Tuệ (The AI Singularity Core)
                drawAiSingularityCore(center, maxRadius * 0.35f * singularityPulse)
            }

            // Huy hiệu lõi biểu tượng AI ở chính giữa
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFF8B5CF6),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .border(1.5.dp, Color(0xFF51FAC1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Singularity",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── 2. Thẻ Hologram Trạm Cổng Không Thời Gian (Multiverse Quantum Gateway Card) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xDD1E1538),
                            Color(0xEE120E24),
                            Color(0xFF0B0818)
                        )
                    )
                )
                .border(
                    1.5.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF8B5CF6),
                            Color(0xFF06B6D4),
                            Color(0xFF51FAC1),
                            Color(0xFFEC4899)
                        )
                    ),
                    RoundedCornerShape(26.dp)
                )
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Header Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x338B5CF6))
                        .border(1.dp, Color(0x66A78BFA), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AllInclusive,
                        contentDescription = null,
                        tint = Color(0xFF51FAC1),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "INFINITE AI MULTIVERSE" else "SIÊU THIÊN HÀ ĐA VŨ TRỤ AI",
                        color = Color(0xFF51FAC1),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isEnglish) "The Final Singularity of Learning" else "Đỉnh Cao Tri Thức Vô Tận",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isEnglish) {
                        "Conquer all 40 planets to trigger the Quantum Leap. Where Generative AI creates boundless personal learning worlds just for you!"
                    } else {
                        "Chinh phục 40 Hành Tinh Hệ Mặt Trời để kích hoạt bước nhảy Lượng Tử. Nơi AI Siêu Trí Tuệ tự động kiến tạo các thế giới học tập không giới hạn!"
                    },
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Thanh năng lượng lượng tử tích tụ
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFullyUnlocked) {
                                (if (isEnglish) "Quantum Gateway Active" else "Cổng Lượng Tử Đã Mở")
                            } else {
                                (if (isEnglish) "Warp Charging Progress" else "Năng Lượng Lỗ Sâu Tích Tụ")
                            },
                            color = if (isFullyUnlocked) Color(0xFF51FAC1) else Color(0xFFA78BFA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$completedStages/$totalStages (${(progressPercent * 100).toInt()}%)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E1B38))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressPercent)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF8B5CF6),
                                            Color(0xFF06B6D4),
                                            Color(0xFF51FAC1)
                                        )
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Nút bấm tương tác
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onExploreClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFullyUnlocked) Color(0xFF51FAC1) else Color(0xFF7C3AED)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isFullyUnlocked) Icons.Default.RocketLaunch else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (isFullyUnlocked) Color(0xFF092E20) else Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFullyUnlocked) {
                                (if (isEnglish) "ENTER AI MULTIVERSE" else "BƯỚC VÀO ĐA VŨ TRỤ AI")
                            } else {
                                (if (isEnglish) "EXPLORE AI MULTIVERSE" else "CHIÊM NGƯỠNG ĐA VŨ TRỤ AI")
                            },
                            color = if (isFullyUnlocked) Color(0xFF092E20) else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Vẽ các cánh tay xoắn ốc của đĩa bồi tụ thiên hà (Accretion Spiral Arms)
 */
private fun DrawScope.drawAccretionSpiralArms(center: Offset, radius: Float, isLowEnd: Boolean) {
    val arms = 4
    val steps = if (isLowEnd) 24 else 48

    for (a in 0 until arms) {
        val baseAngle = a * (2f * PI.toFloat() / arms)
        val armColor = when (a % 3) {
            0 -> Color(0xFF8B5CF6) // Tím neon
            1 -> Color(0xFF06B6D4) // Lam ngọc
            else -> Color(0xFFFFB703) // Hoàng kim
        }

        var prevX = center.x
        var prevY = center.y

        for (i in 4..steps) {
            val t = i.toFloat() / steps
            val r = radius * (t.pow(1.35f))
            val angle = baseAngle + t * 4.2f

            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)

            if (i > 4) {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            armColor.copy(alpha = 0.85f * (1f - t * 0.6f)),
                            armColor.copy(alpha = 0.15f)
                        ),
                        start = Offset(prevX, prevY),
                        end = Offset(x, y)
                    ),
                    start = Offset(prevX, prevY),
                    end = Offset(x, y),
                    strokeWidth = (5.5f * (1f - t * 0.5f)).dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Hạt sao li ti dọc cánh tay thiên hà
            if (i % 2 == 0) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f * (1f - t * 0.4f)),
                    radius = (1.5f + (i % 3) * 0.6f).dp.toPx(),
                    center = Offset(x, y)
                )
            }

            prevX = x
            prevY = y
        }
    }
}

/**
 * Vẽ các vành đai thấu kính hấp dẫn Einstein (Einstein Photon Rings)
 */
private fun DrawScope.drawGravitationalLensingRings(center: Offset, radius: Float) {
    // Vành 1: Nghiêng 35 độ
    drawOval(
        brush = Brush.sweepGradient(
            listOf(
                Color(0xFF51FAC1).copy(alpha = 0.95f),
                Color(0xFF06B6D4).copy(alpha = 0.65f),
                Color(0xFFEC4899).copy(alpha = 0.35f),
                Color(0xFF8B5CF6).copy(alpha = 0.85f),
                Color(0xFF51FAC1).copy(alpha = 0.95f)
            ),
            center = center
        ),
        topLeft = Offset(center.x - radius, center.y - radius * 0.42f),
        size = Size(radius * 2f, radius * 0.84f),
        style = Stroke(width = 2.2.dp.toPx())
    )

    // Vành 2: Vành đối xứng mờ
    drawOval(
        brush = Brush.sweepGradient(
            listOf(
                Color(0xFFFFD166).copy(alpha = 0.70f),
                Color(0xFF8B5CF6).copy(alpha = 0.30f),
                Color(0xFF00F5D4).copy(alpha = 0.70f)
            ),
            center = center
        ),
        topLeft = Offset(center.x - radius * 1.15f, center.y - radius * 0.32f),
        size = Size(radius * 2.3f, radius * 0.64f),
        style = Stroke(width = 1.4.dp.toPx())
    )
}

/**
 * Vẽ Lõi Điểm Kỳ Dị AI Siêu Trí Tuệ (The AI Singularity Core)
 */
private fun DrawScope.drawAiSingularityCore(center: Offset, coreRadius: Float) {
    // 1. Quầng hào quang phát xạ
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                Color(0xFF51FAC1).copy(alpha = 0.85f),
                Color(0xFF06B6D4).copy(alpha = 0.55f),
                Color(0xFF8B5CF6).copy(alpha = 0.25f),
                Color.Transparent
            ),
            center = center,
            radius = coreRadius * 1.5f
        ),
        center = center,
        radius = coreRadius * 1.5f
    )

    // 2. Tia nhiễu xạ hình sao 6 cánh (6-point Stellar Diffraction Spikes)
    val spikeLen = coreRadius * 2.4f
    val angles = floatArrayOf(0f, 60f, 120f)
    for (angle in angles) {
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        drawLine(
            brush = Brush.linearGradient(
                listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.85f),
                    Color.Transparent
                ),
                start = Offset(center.x - cosA * spikeLen, center.y - sinA * spikeLen),
                end = Offset(center.x + cosA * spikeLen, center.y + sinA * spikeLen)
            ),
            start = Offset(center.x - cosA * spikeLen, center.y - sinA * spikeLen),
            end = Offset(center.x + cosA * spikeLen, center.y + sinA * spikeLen),
            strokeWidth = 1.6.dp.toPx()
        )
    }

    // 3. Khối cầu tâm điểm kỳ dị
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFF67E8F9),
                Color(0xFF0E7490)
            ),
            center = center,
            radius = coreRadius * 0.65f
        ),
        center = center,
        radius = coreRadius * 0.65f
    )
}

/**
 * Hộp thoại Trình Diễn Đa Vũ Trụ AI Tuyệt Đẹp (Interactive Multiverse Preview Dialog)
 */
@Composable
fun CosmicMultiversePreviewDialog(
    isEnglish: Boolean = false,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF140F26),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0xFF51FAC1),
                        Color(0xFF8B5CF6),
                        Color(0xFFEC4899)
                    )
                ),
                RoundedCornerShape(28.dp)
            ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AllInclusive,
                    contentDescription = null,
                    tint = Color(0xFF51FAC1),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isEnglish) "The Infinite AI Multiverse" else "Đa Vũ Trụ AI Vô Tận",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (isEnglish) {
                        "Congratulations on gazing upon the edge of known cosmos! Here is what awaits you once you unlock the Singularity:"
                    } else {
                        "Chào mừng bạn đã chiêm ngưỡng ranh giới vũ trụ đã biết! Đây là 4 đặc quyền tối thượng của Đa Vũ Trụ AI đang chờ bạn mở khóa:"
                    },
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                MultiverseFeatureRow(
                    icon = Icons.Default.AutoFixHigh,
                    iconTint = Color(0xFF51FAC1),
                    title = if (isEnglish) "Autonomous AI Worlds" else "Tự Sinh Thế Giới Học Tập Vô Tận",
                    desc = if (isEnglish) "Generate any course from your wildest prompts." else "Nhập bất kỳ chủ đề nào, AI sẽ tự kiến tạo hành tinh học tập riêng."
                )

                MultiverseFeatureRow(
                    icon = Icons.Default.RecordVoiceOver,
                    iconTint = Color(0xFF06B6D4),
                    title = if (isEnglish) "Living AI Personas" else "Hội Thoại Thời Gian Thực Với AI Persona",
                    desc = if (isEnglish) "Converse in real-time with cosmic intelligent mentors." else "Trò chuyện tương tác phản xạ tự nhiên như người bản xứ."
                )

                MultiverseFeatureRow(
                    icon = Icons.Default.WorkspacePremium,
                    iconTint = Color(0xFFFFD166),
                    title = if (isEnglish) "Hall of Galactic Immortals" else "Điện Thờ Bất Tử Đa Vũ Trụ",
                    desc = if (isEnglish) "Immortalize your name across the cosmic leaderboards." else "Khắc tên vĩnh hằng trên Bảng Vàng Ngân Hà và nhận vương miện AI."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = if (isEnglish) "I Will Conquer It!" else "Tôi Sẽ Chinh Phục!",
                    color = Color(0xFF092E20),
                    fontWeight = FontWeight.Black
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
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x221E1B38))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = desc,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

