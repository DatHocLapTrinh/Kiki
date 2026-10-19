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

    // ── 1. Cosmic Nebula Haze (Outer Glow) ────────────────────────────────────
    val hazeAlpha = if (isActive) 0.32f * alphaScale else 0.18f * alphaScale
    drawPath(
        path = mainPath,
        brush = Brush.verticalGradient(
            listOf(
                startColor.copy(alpha = hazeAlpha),
                Color(0xFFEC4899).copy(alpha = hazeAlpha * 1.1f),
                Color(0xFF7E5CFF).copy(alpha = hazeAlpha),
                endColor.copy(alpha = hazeAlpha)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round)
    )

    // ── 2. Ribbon Band 1: Radiant Magenta / Rose Silk Ribbon ──────────────────
    val top1X = FloatArray(samples + 1)
    val top1Y = FloatArray(samples + 1)
    val bot1X = FloatArray(samples + 1)
    val bot1Y = FloatArray(samples + 1)

    val maxW1 = 15.dp.toPx()
    val maxDisp1 = 4.dp.toPx()

    for (k in 0..samples) {
        val t = k.toFloat() / samples
        val sinT = sin(PI.toFloat() * t)
        val w = (maxW1 * sinT * (1f + 0.22f * sin(2f * PI.toFloat() * t + animPhase))).coerceAtLeast(0f)
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

    val r1Alpha = if (isActive) 0.55f * alphaScale else 0.35f * alphaScale
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

    // ── 3. Ribbon Band 2: Cosmic Violet / Purple Weaving Silk ─────────────────
    val top2X = FloatArray(samples + 1)
    val top2Y = FloatArray(samples + 1)
    val bot2X = FloatArray(samples + 1)
    val bot2Y = FloatArray(samples + 1)

    val maxW2 = 18.dp.toPx()
    val maxDisp2 = 5.dp.toPx()

    for (k in 0..samples) {
        val t = k.toFloat() / samples
        val sinT = sin(PI.toFloat() * t)
        val w = (maxW2 * sinT * (1f + 0.25f * cos(2f * PI.toFloat() * t + animPhase))).coerceAtLeast(0f)
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

    val r2Alpha = if (isActive) 0.50f * alphaScale else 0.32f * alphaScale
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

    // ── 4. Ribbon Band 3: Electric Cyan / Blue Highlight Strip ───────────────
    val top3X = FloatArray(samples + 1)
    val top3Y = FloatArray(samples + 1)
    val bot3X = FloatArray(samples + 1)
    val bot3Y = FloatArray(samples + 1)

    val maxW3 = 6.dp.toPx()
    for (k in 0..samples) {
        val t = k.toFloat() / samples
        val sinT = sin(PI.toFloat() * t)
        val w = (maxW3 * sinT).coerceAtLeast(0f)
        val disp = 2.dp.toPx() * sinT * sin(4f * PI.toFloat() * t + animPhase)
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

    val r3Alpha = if (isActive) 0.40f * alphaScale else 0.22f * alphaScale
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
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = if (isActive) PathEffect.dashPathEffect(
                floatArrayOf(16.dp.toPx(), 8.dp.toPx()),
                phase = waveOffsetPx
            ) else null
        )
    )

    // ── 6. White-Hot Filament (Thin Center Beam) ─────────────────────────────
    val filamentAlpha = if (isActive) 0.88f * alphaScale else 0.45f * alphaScale
    drawPath(
        path = mainPath,
        color = Color.White.copy(alpha = filamentAlpha),
        style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
    )

    // ── 7. Sparkle Cross Stars (4-Point Stars as in reference image) ─────────
    val starConfigs = listOf(
        StarDef(0.24f, -9f, 5.2f, Color(0xFFB1FFF4), 0.5f),
        StarDef(0.48f, 8f, 7.5f, Color(0xFFFF8AD8), 1.8f),
        StarDef(0.70f, -7f, 6.0f, Color(0xFFD4B2FF), 3.2f),
        StarDef(0.85f, 9f, 4.8f, Color(0xFFFFFFFF), 4.5f)
    )

    for (s in starConfigs) {
        val sampleIndex = (s.t * samples).toInt().coerceIn(0, samples)
        val nx = normX[sampleIndex]
        val ny = normY[sampleIndex]
        val sx = ptsX[sampleIndex] + nx * s.normOffsetDp.dp.toPx()
        val sy = ptsY[sampleIndex] + ny * s.normOffsetDp.dp.toPx()
        val twinkle = (0.55f + 0.45f * sin(animPhase + s.phaseOffset)).coerceIn(0.15f, 1f) * alphaScale
        drawSparkleStar(
            centerX = sx,
            centerY = sy,
            radiusPx = s.sizeDp.dp.toPx(),
            color = s.color,
            twinkleAlpha = twinkle
        )
    }

    // ── 8. Stardust Particles (Twinkling Cosmic Specks) ──────────────────────
    val dustDefs = listOf(
        Pair(0.16f, 12f),
        Pair(0.33f, -14f),
        Pair(0.58f, 15f),
        Pair(0.76f, -13f),
        Pair(0.90f, 7f)
    )
    for (d in dustDefs) {
        val sIdx = (d.first * samples).toInt().coerceIn(0, samples)
        val dxPos = ptsX[sIdx] + normX[sIdx] * d.second.dp.toPx()
        val dyPos = ptsY[sIdx] + normY[sIdx] * d.second.dp.toPx()
        val dustAlpha = (0.45f + 0.45f * sin(animPhase + d.first * 8f)).coerceIn(0.2f, 0.95f) * alphaScale
        drawCircle(
            color = Color.White.copy(alpha = dustAlpha),
            radius = 1.6.dp.toPx(),
            center = Offset(dxPos, dyPos)
        )
    }

    // ── 9. Traveling Energy Pulse Orb (Cosmic Plasma Flow) ───────────────────
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
                    Color(0x99FFFFFF),
                    Color(0x66FF2A85),
                    Color(0x00FF2A85)
                ),
                center = Offset(pulseX, pulseY),
                radius = 16.dp.toPx()
            ),
            radius = 16.dp.toPx(),
            center = Offset(pulseX, pulseY)
        )
        // Radiant core
        drawCircle(
            color = Color(0xFFFF8AD8),
            radius = 5.dp.toPx(),
            center = Offset(pulseX, pulseY)
        )
        // White center spark
        drawCircle(
            color = Color.White,
            radius = 2.4.dp.toPx(),
            center = Offset(pulseX, pulseY)
        )
    }
}

private data class StarDef(
    val t: Float,
    val normOffsetDp: Float,
    val sizeDp: Float,
    val color: Color,
    val phaseOffset: Float
)

/**
 * Draws a 4-point concave sparkle star (cross flare) with soft outer glow and bright center.
 */
private fun DrawScope.drawSparkleStar(
    centerX: Float,
    centerY: Float,
    radiusPx: Float,
    color: Color,
    twinkleAlpha: Float
) {
    if (radiusPx <= 0f) return
    val alphaColor = color.copy(alpha = (color.alpha * twinkleAlpha).coerceIn(0f, 1f))

    // Soft glow circle behind star
    drawCircle(
        color = alphaColor.copy(alpha = alphaColor.alpha * 0.35f),
        radius = radiusPx * 1.6f,
        center = Offset(centerX, centerY)
    )

    // 4-pointed sparkle star path (concave diamond)
    val starPath = Path().apply {
        val r = radiusPx
        val inner = radiusPx * 0.22f
        moveTo(centerX, centerY - r)
        quadraticTo(centerX, centerY, centerX + inner, centerY)
        quadraticTo(centerX, centerY, centerX + r, centerY)
        quadraticTo(centerX, centerY, centerX, centerY + inner)
        quadraticTo(centerX, centerY, centerX, centerY + r)
        quadraticTo(centerX, centerY, centerX - inner, centerY)
        quadraticTo(centerX, centerY, centerX - r, centerY)
        quadraticTo(centerX, centerY, centerX, centerY - inner)
        close()
    }
    drawPath(path = starPath, color = alphaColor)

    // Bright white core dot
    drawCircle(
        color = Color.White.copy(alpha = twinkleAlpha.coerceIn(0f, 1f)),
        radius = radiusPx * 0.28f,
        center = Offset(centerX, centerY)
    )
}
