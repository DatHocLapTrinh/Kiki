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
 * Draws a flowing cosmic aurora energy ribbon connecting two nodes (spheres)
 * with translucent waving ribbon bands, stardust, 4-point sparkle cross stars,
 * and luminous neon filaments.
 *
 * Precisely connects from (startX, startY) to (endX, endY) with zero offset at the endpoints,
 * tapering perfectly to the center of each sphere.
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
    alphaScale: Float = 1f
) {
    val dx = endX - startX
    val dy = endY - startY

    // Cubic bezier control points for a smooth, natural vertical S-curve
    val c1x = startX + dx * 0.10f
    val c1y = startY + dy * 0.52f
    val c2x = endX - dx * 0.10f
    val c2y = startY + dy * 0.48f

    // Main cubic bezier path
    val mainPath = Path().apply {
        moveTo(startX, startY)
        cubicTo(c1x, c1y, c2x, c2y, endX, endY)
    }

    val samples = 24
    val ptsX = FloatArray(samples + 1)
    val ptsY = FloatArray(samples + 1)
    val normX = FloatArray(samples + 1)
    val normY = FloatArray(samples + 1)

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
        normX[k] = -ty / len
        normY[k] = tx / len
    }

    // ── 0. Cosmic Docking Glow (Quầng năng lượng tiếp giáp tại 2 hành tinh) ────
    val dockAlpha = if (isActive) 0.50f * alphaScale else 0.26f * alphaScale
    val dockRadius = 84.dp.toPx()

    // 0.1 Quầng năng lượng tinh vân bán nguyệt bao bọc hành tinh xuất phát (Top planet)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                startColor.copy(alpha = dockAlpha * 0.95f),
                Color(0xFFFF2A85).copy(alpha = dockAlpha * 0.65f),
                Color(0xFF7E5CFF).copy(alpha = dockAlpha * 0.35f),
                Color.Transparent
            ),
            center = Offset(startX, startY),
            radius = dockRadius
        ),
        radius = dockRadius,
        center = Offset(startX, startY)
    )

    // 0.2 Quầng năng lượng tinh vân bán nguyệt đón nhận tại hành tinh đích đến (Bottom planet)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                endColor.copy(alpha = dockAlpha * 0.95f),
                Color(0xFF51FAC1).copy(alpha = dockAlpha * 0.65f),
                Color(0xFF7E5CFF).copy(alpha = dockAlpha * 0.35f),
                Color.Transparent
            ),
            center = Offset(endX, endY),
            radius = dockRadius
        ),
        radius = dockRadius,
        center = Offset(endX, endY)
    )

    // 0.3 Vệt bừng sáng tinh vân (Emergence Flare) tại điểm xa lộ thoát ra / tiếp đất
    val exitIdx = (samples * 0.22f).toInt().coerceIn(0, samples)
    val enterIdx = (samples * 0.78f).toInt().coerceIn(0, samples)
    val flareRadius = 44.dp.toPx()

    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color(0xFFFF2A85).copy(alpha = dockAlpha * 0.85f),
                Color(0xFFEC4899).copy(alpha = dockAlpha * 0.40f),
                Color.Transparent
            ),
            center = Offset(ptsX[exitIdx], ptsY[exitIdx]),
            radius = flareRadius
        ),
        radius = flareRadius,
        center = Offset(ptsX[exitIdx], ptsY[exitIdx])
    )

    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color(0xFF51FAC1).copy(alpha = dockAlpha * 0.85f),
                Color(0xFF7E5CFF).copy(alpha = dockAlpha * 0.40f),
                Color.Transparent
            ),
            center = Offset(ptsX[enterIdx], ptsY[enterIdx]),
            radius = flareRadius
        ),
        radius = flareRadius,
        center = Offset(ptsX[enterIdx], ptsY[enterIdx])
    )

    // ── 1. Cosmic Nebula Haze (Outer Glow - 70dp Xa Lộ Vũ Trụ) ────────────────
    val hazeAlpha = if (isActive) 0.32f * alphaScale else 0.18f * alphaScale

    // 1.1 Quầng khuếch tán diện rộng (Broad Diffuse Nebula Glow: 72dp)
    drawPath(
        path = mainPath,
        brush = Brush.verticalGradient(
            listOf(
                startColor.copy(alpha = hazeAlpha * 0.65f),
                Color(0xFFEC4899).copy(alpha = hazeAlpha * 0.80f),
                Color(0xFF7E5CFF).copy(alpha = hazeAlpha * 0.70f),
                endColor.copy(alpha = hazeAlpha * 0.65f)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 72.dp.toPx(), cap = StrokeCap.Round)
    )

    // 1.2 Lõi tinh vân dày dặn (Inner Core Haze: 50dp)
    drawPath(
        path = mainPath,
        brush = Brush.verticalGradient(
            listOf(
                startColor.copy(alpha = hazeAlpha * 0.90f),
                Color(0xFFFF2A85).copy(alpha = hazeAlpha * 1.10f),
                Color(0xFF9333EA).copy(alpha = hazeAlpha * 1.05f),
                endColor.copy(alpha = hazeAlpha * 0.90f)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 50.dp.toPx(), cap = StrokeCap.Round)
    )

    // ── 2. Ribbon Band 1: Radiant Magenta / Rose Silk Ribbon (42dp - 48dp) ────
    val top1X = FloatArray(samples + 1)
    val top1Y = FloatArray(samples + 1)
    val bot1X = FloatArray(samples + 1)
    val bot1Y = FloatArray(samples + 1)

    // Bề rộng tối thiểu 37dp ở 2 đầu, phễu loe 41dp, mở rộng lên 47dp ở giữa
    val baseW1 = 18.5.dp.toPx()
    val midBonus1 = 5.0.dp.toPx()
    val flareW1 = 2.0.dp.toPx()
    val maxDisp1 = 5.dp.toPx()

    for (k in 0..samples) {
        val t = k.toFloat() / samples
        val sinT = sin(PI.toFloat() * t)
        val funnel = ((1f - t).pow(6) + t.pow(6))
        val w = ((baseW1 + midBonus1 * sinT + flareW1 * funnel) * (1f + 0.15f * sin(2f * PI.toFloat() * t + animPhase))).coerceAtLeast(0f)
        val disp = maxDisp1 * sinT * sin(2f * PI.toFloat() * t + animPhase * 0.7f)
        top1X[k] = ptsX[k] + normX[k] * (disp + w)
        top1Y[k] = ptsY[k] + normY[k] * (disp + w)
        bot1X[k] = ptsX[k] + normX[k] * (disp - w)
        bot1Y[k] = ptsY[k] + normY[k] * (disp - w)
    }

    val ribbon1Path = Path().apply {
        moveTo(top1X[0], top1Y[0])
        for (k in 1..samples) lineTo(top1X[k], top1Y[k])
        for (k in samples downTo 0) lineTo(bot1X[k], bot1Y[k])
        close()
    }

    val r1Alpha = if (isActive) 0.58f * alphaScale else 0.36f * alphaScale
    drawPath(
        path = ribbon1Path,
        brush = Brush.verticalGradient(
            listOf(
                startColor.copy(alpha = r1Alpha * 0.85f),
                Color(0xFFFF2A85).copy(alpha = r1Alpha),
                Color(0xFFEC4899).copy(alpha = r1Alpha * 0.95f),
                endColor.copy(alpha = r1Alpha * 0.85f)
            ),
            startY = startY,
            endY = endY
        )
    )

    // ── 3. Ribbon Band 2: Cosmic Violet / Purple Weaving Silk (43dp - 50dp) ───
    val top2X = FloatArray(samples + 1)
    val top2Y = FloatArray(samples + 1)
    val bot2X = FloatArray(samples + 1)
    val bot2Y = FloatArray(samples + 1)

    // Bề rộng tối thiểu 39dp ở 2 đầu, phễu loe 43dp, mở rộng lên 50dp ở giữa
    val baseW2 = 19.5.dp.toPx()
    val midBonus2 = 5.5.dp.toPx()
    val flareW2 = 2.0.dp.toPx()
    val maxDisp2 = 6.dp.toPx()

    for (k in 0..samples) {
        val t = k.toFloat() / samples
        val sinT = sin(PI.toFloat() * t)
        val funnel = ((1f - t).pow(6) + t.pow(6))
        val w = ((baseW2 + midBonus2 * sinT + flareW2 * funnel) * (1f + 0.16f * cos(2f * PI.toFloat() * t + animPhase * 0.8f))).coerceAtLeast(0f)
        val disp = -maxDisp2 * sinT * cos(2f * PI.toFloat() * t + animPhase * 0.8f)
        top2X[k] = ptsX[k] + normX[k] * (disp + w)
        top2Y[k] = ptsY[k] + normY[k] * (disp + w)
        bot2X[k] = ptsX[k] + normX[k] * (disp - w)
        bot2Y[k] = ptsY[k] + normY[k] * (disp - w)
    }

    val ribbon2Path = Path().apply {
        moveTo(top2X[0], top2Y[0])
        for (k in 1..samples) lineTo(top2X[k], top2Y[k])
        for (k in samples downTo 0) lineTo(bot2X[k], bot2Y[k])
        close()
    }

    val r2Alpha = if (isActive) 0.52f * alphaScale else 0.34f * alphaScale
    drawPath(
        path = ribbon2Path,
        brush = Brush.verticalGradient(
            listOf(
                startColor.copy(alpha = r2Alpha * 0.8f),
                Color(0xFF7E5CFF).copy(alpha = r2Alpha),
                Color(0xFF9333EA).copy(alpha = r2Alpha * 0.9f),
                Color(0xFFC084FC).copy(alpha = r2Alpha * 0.85f),
                endColor.copy(alpha = r2Alpha * 0.8f)
            ),
            startY = startY,
            endY = endY
        )
    )

    // ── 4. Ribbon Band 3: Electric Cyan / Starlight Streak (11dp - 18dp) ──────
    val top3X = FloatArray(samples + 1)
    val top3Y = FloatArray(samples + 1)
    val bot3X = FloatArray(samples + 1)
    val bot3Y = FloatArray(samples + 1)

    val baseW3 = 5.5.dp.toPx()
    val midBonus3 = 3.5.dp.toPx()
    for (k in 0..samples) {
        val t = k.toFloat() / samples
        val sinT = sin(PI.toFloat() * t)
        val w = ((baseW3 + midBonus3 * sinT) * (1f + 0.12f * sin(3f * PI.toFloat() * t + animPhase))).coerceAtLeast(0f)
        val disp = 3.dp.toPx() * sinT * sin(4f * PI.toFloat() * t + animPhase)
        top3X[k] = ptsX[k] + normX[k] * (disp + w)
        top3Y[k] = ptsY[k] + normY[k] * (disp + w)
        bot3X[k] = ptsX[k] + normX[k] * (disp - w)
        bot3Y[k] = ptsY[k] + normY[k] * (disp - w)
    }

    val ribbon3Path = Path().apply {
        moveTo(top3X[0], top3Y[0])
        for (k in 1..samples) lineTo(top3X[k], top3Y[k])
        for (k in samples downTo 0) lineTo(bot3X[k], bot3Y[k])
        close()
    }

    val r3Alpha = if (isActive) 0.44f * alphaScale else 0.24f * alphaScale
    drawPath(
        path = ribbon3Path,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFF51FAC1).copy(alpha = r3Alpha),
                Color(0xFF38BDF8).copy(alpha = r3Alpha * 1.1f),
                Color(0xFFA855F7).copy(alpha = r3Alpha * 0.9f)
            ),
            startY = startY,
            endY = endY
        )
    )

    // ── 5. Vibrant Luminous Neon Core Line ────────────────────────────────────
    val neonAlpha = if (isActive) 0.95f * alphaScale else 0.65f * alphaScale
    drawPath(
        path = mainPath,
        brush = Brush.verticalGradient(
            listOf(
                startColor.copy(alpha = neonAlpha),
                Color(0xFFFF2A85).copy(alpha = neonAlpha),
                Color(0xFFC084FC).copy(alpha = neonAlpha),
                endColor.copy(alpha = neonAlpha)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(
            width = 4.5.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = if (isActive) PathEffect.dashPathEffect(
                floatArrayOf(18.dp.toPx(), 9.dp.toPx()),
                phase = waveOffsetPx
            ) else null
        )
    )

    // ── 6. White-Hot Filament (Thin Center Beam) ─────────────────────────────
    val filamentAlpha = if (isActive) 0.90f * alphaScale else 0.50f * alphaScale
    drawPath(
        path = mainPath,
        color = Color.White.copy(alpha = filamentAlpha),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    // ── 7. Traveling Star Pulse Orb (Chuyển động truyền từ hành tinh này sang hành tinh khác) ──
    if (isActive) {
        val pulseT = ((animPhase / (2f * PI.toFloat())) % 1f + 1f) % 1f
        val pulseIdx = (pulseT * samples).toInt().coerceIn(0, samples - 1)
        val subT = (pulseT * samples) - pulseIdx
        val pulseX = ptsX[pulseIdx] + (ptsX[pulseIdx + 1] - ptsX[pulseIdx]) * subT
        val pulseY = ptsY[pulseIdx] + (ptsY[pulseIdx + 1] - ptsY[pulseIdx]) * subT

        // Soft outer energy flare
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color(0xBBFFFFFF),
                    Color(0x88FF2A85),
                    Color(0x00FF2A85)
                ),
                center = Offset(pulseX, pulseY),
                radius = 22.dp.toPx()
            ),
            radius = 22.dp.toPx(),
            center = Offset(pulseX, pulseY)
        )
        // Radiant core
        drawCircle(
            color = Color(0xFFFF8AD8),
            radius = 6.5.dp.toPx(),
            center = Offset(pulseX, pulseY)
        )
        // White center spark
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = Offset(pulseX, pulseY)
        )
    }
}
