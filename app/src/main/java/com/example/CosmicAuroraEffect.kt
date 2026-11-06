package com.example

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.pow

/**
 * State holding continuous animated values for the Cosmic Aurora Ribbon path.
 */
class CosmicAuroraAnimationState(
    val phase: State<Float>,
    val waveOffset: State<Float>
)

@Composable
fun rememberCosmicAuroraAnimation(): CosmicAuroraAnimationState {
    val transition = rememberInfiniteTransition(label = "cosmic_aurora")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora_phase"
    )
    val waveOffset = transition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora_wave_dash"
    )
    return CosmicAuroraAnimationState(phase, waveOffset)
}

/**
 * Reusable scratch buffer to eliminate all heap allocations during the 120 FPS draw loop.
 */
private class AuroraSegmentScratch {
    val ptsX = FloatArray(33)
    val ptsY = FloatArray(33)
    val tanX = FloatArray(33)
    val tanY = FloatArray(33)
    val normX = FloatArray(33)
    val normY = FloatArray(33)
    val mainPath = Path()
    val fiber1Path = Path()
    val fiber2Path = Path()
    val fiber4Path = Path()
}

private val auroraScratchPool = ThreadLocal.withInitial { AuroraSegmentScratch() }
private fun getAuroraScratch(): AuroraSegmentScratch = auroraScratchPool.get() ?: AuroraSegmentScratch()

/**
 * Draws a flowing cosmic aurora energy ribbon connecting two nodes (spheres)
 * with translucent waving ribbon bands, stardust, 4-point sparkle cross stars,
 * and luminous neon filaments.
 *
 * Zero-allocation & GPU-optimized for rock-solid 120 FPS performance.
 */
fun DrawScope.drawCosmicAuroraSegment(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    startColor: Color,
    endColor: Color,
    animPhase: Float,
    waveOffsetPx: Float,
    isActive: Boolean,
    alphaScale: Float = 1f,
    surgePulseProgress: Float? = null
) {
    val dx = endX - startX
    val dy = endY - startY

    // Cubic bezier control points for a smooth, natural vertical S-curve
    val c1x = startX + dx * 0.10f
    val c1y = startY + dy * 0.52f
    val c2x = endX - dx * 0.10f
    val c2y = startY + dy * 0.48f

    val scratch = getAuroraScratch()
    val mainPath = scratch.mainPath
    mainPath.reset()
    mainPath.moveTo(startX, startY)
    mainPath.cubicTo(c1x, c1y, c2x, c2y, endX, endY)

    val samples = 32
    val ptsX = scratch.ptsX
    val ptsY = scratch.ptsY
    val tanX = scratch.tanX
    val tanY = scratch.tanY
    val normX = scratch.normX
    val normY = scratch.normY

    for (k in 0..samples) {
        val t = k.toFloat() / samples
        val u = 1f - t
        val px = u * u * u * startX + 3f * u * u * t * c1x + 3f * u * t * t * c2x + t * t * t * endX
        val py = u * u * u * startY + 3f * u * u * t * c1y + 3f * u * t * t * c2y + t * t * t * endY

        val tx = 3f * u * u * (c1x - startX) + 6f * u * t * (c2x - c1x) + 3f * t * t * (endX - c2x)
        val ty = 3f * u * u * (c1y - startY) + 6f * u * t * (c2y - c1y) + 3f * t * t * (endY - c2y)
        val len = sqrt(tx * tx + ty * ty).coerceAtLeast(0.001f)

        ptsX[k] = px
        ptsY[k] = py
        tanX[k] = tx / len
        tanY[k] = ty / len
        normX[k] = -ty / len
        normY[k] = tx / len
    }

    // ── 1. Cosmic Dust Gas Clouds (Mây khói tinh vân vũ trụ nền - Deep Violet & Magenta) ──
    val cloudAlpha = if (isActive) 0.28f * alphaScale else 0.16f * alphaScale
    val cloudSteps = 5
    for (c in 1 until cloudSteps) {
        val t = c.toFloat() / cloudSteps
        val idx = (t * samples).toInt().coerceIn(0, samples)
        val lateralOffset = (sin(c * 2.3f + animPhase * 0.4f) * 16.dp.toPx())
        val cx = ptsX[idx] + normX[idx] * lateralOffset
        val cy = ptsY[idx] + normY[idx] * lateralOffset
        val r = (48 + (c % 3) * 10).dp.toPx()

        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color(0xFF4C1D95).copy(alpha = cloudAlpha * 0.85f),
                    Color(0xFF2E1065).copy(alpha = cloudAlpha * 0.55f),
                    Color(0xFF1E1035).copy(alpha = cloudAlpha * 0.25f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = r
            ),
            radius = r,
            center = Offset(cx, cy)
        )
    }

    // ── 2. Braided Multi-Strand Cosmic Fibers (Các chùm sợi plasma đan bện xoắn ốc) ──
    val fiberAlpha = if (isActive) 0.78f * alphaScale else 0.48f * alphaScale

    val fiber1Path = scratch.fiber1Path
    val fiber2Path = scratch.fiber2Path
    val fiber4Path = scratch.fiber4Path
    fiber1Path.reset()
    fiber2Path.reset()
    fiber4Path.reset()

    for (k in 0..samples) {
        val t = k.toFloat() / samples
        val sinT = sin(PI.toFloat() * t)

        val disp1 = normX[k] * (sin(3.5f * PI.toFloat() * t + animPhase) * 11.dp.toPx() + sin(7f * PI.toFloat() * t) * 3.dp.toPx()) * (0.4f + 0.6f * sinT)
        val disp1Y = normY[k] * (sin(3.5f * PI.toFloat() * t + animPhase) * 11.dp.toPx() + sin(7f * PI.toFloat() * t) * 3.dp.toPx()) * (0.4f + 0.6f * sinT)

        val disp2 = normX[k] * (-cos(3.2f * PI.toFloat() * t + animPhase * 0.8f) * 14.dp.toPx() + cos(6f * PI.toFloat() * t) * 3.5.dp.toPx()) * (0.4f + 0.6f * sinT)
        val disp2Y = normY[k] * (-cos(3.2f * PI.toFloat() * t + animPhase * 0.8f) * 14.dp.toPx() + cos(6f * PI.toFloat() * t) * 3.5.dp.toPx()) * (0.4f + 0.6f * sinT)

        val disp4 = normX[k] * (cos(4.8f * PI.toFloat() * t + animPhase * 1.3f) * 6.dp.toPx() + sin(9f * PI.toFloat() * t) * 2.dp.toPx()) * (0.4f + 0.6f * sinT)
        val disp4Y = normY[k] * (cos(4.8f * PI.toFloat() * t + animPhase * 1.3f) * 6.dp.toPx() + sin(9f * PI.toFloat() * t) * 2.dp.toPx()) * (0.4f + 0.6f * sinT)

        val p1x = ptsX[k] + disp1
        val p1y = ptsY[k] + disp1Y
        val p2x = ptsX[k] + disp2
        val p2y = ptsY[k] + disp2Y
        val p4x = ptsX[k] + disp4
        val p4y = ptsY[k] + disp4Y

        if (k == 0) {
            fiber1Path.moveTo(p1x, p1y)
            fiber2Path.moveTo(p2x, p2y)
            fiber4Path.moveTo(p4x, p4y)
        } else {
            fiber1Path.lineTo(p1x, p1y)
            fiber2Path.lineTo(p2x, p2y)
            fiber4Path.lineTo(p4x, p4y)
        }
    }

    // Fiber 2 (Sợi hồng plasma rực sáng)
    drawPath(
        path = fiber2Path,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFEC4899).copy(alpha = fiberAlpha * 0.80f),
                Color(0xFFFF2A85).copy(alpha = fiberAlpha * 0.95f),
                Color(0xFFD946EF).copy(alpha = fiberAlpha * 0.85f)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 3.6.dp.toPx(), cap = StrokeCap.Round)
    )

    // Fiber 1 (Sợi cam lửa hổ phách nóng bỏng)
    drawPath(
        path = fiber1Path,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFFB923C).copy(alpha = fiberAlpha * 0.85f),
                Color(0xFFFFA050).copy(alpha = fiberAlpha * 0.95f),
                Color(0xFFF43F5E).copy(alpha = fiberAlpha * 0.85f)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 3.0.dp.toPx(), cap = StrokeCap.Round)
    )

    // Fiber 4 (Sợi vàng kim starlight đan xen)
    drawPath(
        path = fiber4Path,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFFFF0B3).copy(alpha = fiberAlpha * 0.90f),
                Color(0xFFFFDD80).copy(alpha = fiberAlpha * 1.0f),
                Color(0xFFFF8AD8).copy(alpha = fiberAlpha * 0.90f)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round)
    )

    // ── 3. Lõi dung nham plasma rực lửa ở trung tâm (White-Hot Core Spine) ──
    val coreAlpha = if (isActive) 0.96f * alphaScale else 0.65f * alphaScale

    // 3.1 Vệt phát quang hổ phách - hoa hồng
    drawPath(
        path = mainPath,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFFF7E40).copy(alpha = coreAlpha * 0.65f),
                Color(0xFFFF3366).copy(alpha = coreAlpha * 0.80f),
                Color(0xFFFFA050).copy(alpha = coreAlpha * 0.75f),
                Color(0xFFFF7E40).copy(alpha = coreAlpha * 0.65f)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
    )

    // 3.2 Lõi sợi vàng kim rực rỡ
    drawPath(
        path = mainPath,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFFFE066).copy(alpha = coreAlpha * 0.92f),
                Color(0xFFFF6699).copy(alpha = coreAlpha * 0.96f),
                Color(0xFFFFD166).copy(alpha = coreAlpha * 0.92f)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
    )

    // 3.3 Tia sáng trắng trung tâm
    drawPath(
        path = mainPath,
        color = Color.White.copy(alpha = coreAlpha * 0.95f),
        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
    )

    // ── 4. Traveling Star Pulse Orb (Ngôi sao truyền từ hành tinh này sang hành tinh khác) ──
    val isSurging = surgePulseProgress != null && surgePulseProgress in 0.001f..0.999f
    if (isActive || isSurging) {
        val pulseT = if (isSurging) surgePulseProgress!!.coerceIn(0f, 1f) else (((animPhase / (2f * PI.toFloat())) % 1f + 1f) % 1f)
        val pulseIdx = (pulseT * samples).toInt().coerceIn(0, samples - 1)
        val subT = (pulseT * samples) - pulseIdx
        val pulseX = ptsX[pulseIdx] + (ptsX[pulseIdx + 1] - ptsX[pulseIdx]) * subT
        val pulseY = ptsY[pulseIdx] + (ptsY[pulseIdx + 1] - ptsY[pulseIdx]) * subT

        val pulseRadius = if (isSurging) 34.dp.toPx() else 24.dp.toPx()
        val pulseHaloColor = if (isSurging) Color(0xFF51FAC1) else Color(0x99FFA050)
        val pulseCoreColor = if (isSurging) Color(0xFFFFFFFF) else Color(0xFFFFD166)

        // Soft outer energy flare
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color.White.copy(alpha = if (isSurging) 1.0f else 0.9f),
                    pulseHaloColor,
                    Color(0x55FF2A85),
                    Color.Transparent
                ),
                center = Offset(pulseX, pulseY),
                radius = pulseRadius
            ),
            radius = pulseRadius,
            center = Offset(pulseX, pulseY)
        )
        // Radiant fiery core
        drawCircle(
            color = pulseCoreColor,
            radius = (if (isSurging) 10.dp else 7.dp).toPx(),
            center = Offset(pulseX, pulseY)
        )
        // White center spark
        drawCircle(
            color = Color.White,
            radius = (if (isSurging) 5.dp else 3.2.dp).toPx(),
            center = Offset(pulseX, pulseY)
        )
    }
}
