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

    val samples = 32
    val ptsX = FloatArray(samples + 1)
    val ptsY = FloatArray(samples + 1)
    val tanX = FloatArray(samples + 1)
    val tanY = FloatArray(samples + 1)
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
        tanX[k] = tx / len
        tanY[k] = ty / len
        normX[k] = -ty / len
        normY[k] = tx / len
    }

    // ── 0. Cosmic Docking Glow (Quầng năng lượng tinh vân tiếp giáp tại 2 hành tinh) ──
    val dockAlpha = if (isActive) 0.55f * alphaScale else 0.30f * alphaScale
    val dockRadius = 88.dp.toPx()

    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color(0xFFFF8AD8).copy(alpha = dockAlpha * 0.90f),
                Color(0xFFFF2A85).copy(alpha = dockAlpha * 0.65f),
                Color(0xFF581C87).copy(alpha = dockAlpha * 0.35f),
                Color.Transparent
            ),
            center = Offset(startX, startY),
            radius = dockRadius
        ),
        radius = dockRadius,
        center = Offset(startX, startY)
    )

    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color(0xFFFFA050).copy(alpha = dockAlpha * 0.90f),
                Color(0xFF9333EA).copy(alpha = dockAlpha * 0.65f),
                Color(0xFF3B0764).copy(alpha = dockAlpha * 0.35f),
                Color.Transparent
            ),
            center = Offset(endX, endY),
            radius = dockRadius
        ),
        radius = dockRadius,
        center = Offset(endX, endY)
    )

    // ── 1. Cầu nối bụi vũ trụ cuộn xoáy làm biến dạng không-thời gian (Swirling Dust Bridge) ──
    val dustBridgeAlpha = if (isActive) 0.48f * alphaScale else 0.28f * alphaScale

    val dust1TopX = FloatArray(samples + 1)
    val dust1TopY = FloatArray(samples + 1)
    val dust1BotX = FloatArray(samples + 1)
    val dust1BotY = FloatArray(samples + 1)

    val dust2TopX = FloatArray(samples + 1)
    val dust2TopY = FloatArray(samples + 1)
    val dust2BotX = FloatArray(samples + 1)
    val dust2BotY = FloatArray(samples + 1)

    val dustW1Base = 18.dp.toPx()
    val dustW2Base = 16.dp.toPx()
    val vortexDisp = 14.dp.toPx()

    for (k in 0..samples) {
        val t = k.toFloat() / samples
        val sinT = sin(PI.toFloat() * t)
        val envelope = (0.55f + 0.45f * sinT)

        val swirlPhase = 2.5f * PI.toFloat() * t + animPhase
        val d1 = normX[k] * (sin(swirlPhase) * vortexDisp * envelope)
        val d1Y = normY[k] * (sin(swirlPhase) * vortexDisp * envelope)
        val d2 = normX[k] * (-sin(swirlPhase) * vortexDisp * envelope)
        val d2Y = normY[k] * (-sin(swirlPhase) * vortexDisp * envelope)

        val w1 = (dustW1Base + 6.dp.toPx() * sinT)
        val w2 = (dustW2Base + 6.dp.toPx() * sinT)

        dust1TopX[k] = ptsX[k] + d1 + normX[k] * w1
        dust1TopY[k] = ptsY[k] + d1Y + normY[k] * w1
        dust1BotX[k] = ptsX[k] + d1 - normX[k] * w1
        dust1BotY[k] = ptsY[k] + d1Y - normY[k] * w1

        dust2TopX[k] = ptsX[k] + d2 + normX[k] * w2
        dust2TopY[k] = ptsY[k] + d2Y + normY[k] * w2
        dust2BotX[k] = ptsX[k] + d2 - normX[k] * w2
        dust2BotY[k] = ptsY[k] + d2Y - normY[k] * w2
    }

    val dustPath1 = Path().apply {
        moveTo(dust1TopX[0], dust1TopY[0])
        for (k in 1..samples) lineTo(dust1TopX[k], dust1TopY[k])
        for (k in samples downTo 0) lineTo(dust1BotX[k], dust1BotY[k])
        close()
    }

    val dustPath2 = Path().apply {
        moveTo(dust2TopX[0], dust2TopY[0])
        for (k in 1..samples) lineTo(dust2TopX[k], dust2TopY[k])
        for (k in samples downTo 0) lineTo(dust2BotX[k], dust2BotY[k])
        close()
    }

    // Cầu bụi vũ trụ 1 (Tím thẫm - Indigo sâu thẳm)
    drawPath(
        path = dustPath1,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFF581C87).copy(alpha = dustBridgeAlpha * 0.85f),
                Color(0xFF3B0764).copy(alpha = dustBridgeAlpha),
                Color(0xFF1E1035).copy(alpha = dustBridgeAlpha * 0.90f),
                Color(0xFF4C1D95).copy(alpha = dustBridgeAlpha * 0.85f)
            ),
            startY = startY,
            endY = endY
        )
    )

    // Cầu bụi vũ trụ 2 (Mận chín - Mận hoàng gia cuộn xoáy ngược)
    drawPath(
        path = dustPath2,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFF701A75).copy(alpha = dustBridgeAlpha * 0.80f),
                Color(0xFF831843).copy(alpha = dustBridgeAlpha * 0.95f),
                Color(0xFF4A044E).copy(alpha = dustBridgeAlpha * 0.85f),
                Color(0xFF581C87).copy(alpha = dustBridgeAlpha * 0.80f)
            ),
            startY = startY,
            endY = endY
        )
    )

    // ── 2. Các dải plasma dày đặc, rực rỡ (Dense Radiant Plasma Bands) ────────
    val plasmaAlpha = if (isActive) 0.84f * alphaScale else 0.52f * alphaScale

    // Dải Plasma A: Hồng Neon - Hoa Hồng Rực Sáng (22dp - 30dp)
    val plasmaATopX = FloatArray(samples + 1)
    val plasmaATopY = FloatArray(samples + 1)
    val plasmaABotX = FloatArray(samples + 1)
    val plasmaABotY = FloatArray(samples + 1)

    // Dải Plasma B: Vàng Hổ Phách - Cam Lửa (18dp - 26dp)
    val plasmaBTopX = FloatArray(samples + 1)
    val plasmaBTopY = FloatArray(samples + 1)
    val plasmaBBotX = FloatArray(samples + 1)
    val plasmaBBotY = FloatArray(samples + 1)

    // Dải Plasma C: Sóng Năng Lượng Xanh Ngọc (8dp - 12dp)
    val plasmaCTopX = FloatArray(samples + 1)
    val plasmaCTopY = FloatArray(samples + 1)
    val plasmaCBotX = FloatArray(samples + 1)
    val plasmaCBotY = FloatArray(samples + 1)

    val plasmaWBaseA = 11.dp.toPx()
    val plasmaWBaseB = 9.dp.toPx()
    val plasmaWBaseC = 4.dp.toPx()

    for (k in 0..samples) {
        val t = k.toFloat() / samples
        val sinT = sin(PI.toFloat() * t)
        val envelope = (0.50f + 0.50f * sinT)

        val wavePhase = 3.2f * PI.toFloat() * t + animPhase
        val dispA = normX[k] * (sin(wavePhase) * 8.dp.toPx() * envelope)
        val dispAY = normY[k] * (sin(wavePhase) * 8.dp.toPx() * envelope)

        val dispB = normX[k] * (-sin(wavePhase) * 8.dp.toPx() * envelope)
        val dispBY = normY[k] * (-sin(wavePhase) * 8.dp.toPx() * envelope)

        val dispC = normX[k] * (cos(wavePhase * 1.5f) * 4.dp.toPx() * envelope)
        val dispCY = normY[k] * (cos(wavePhase * 1.5f) * 4.dp.toPx() * envelope)

        val wA = plasmaWBaseA + 4.dp.toPx() * sinT
        val wB = plasmaWBaseB + 4.dp.toPx() * sinT
        val wC = plasmaWBaseC + 2.dp.toPx() * sinT

        plasmaATopX[k] = ptsX[k] + dispA + normX[k] * wA
        plasmaATopY[k] = ptsY[k] + dispAY + normY[k] * wA
        plasmaABotX[k] = ptsX[k] + dispA - normX[k] * wA
        plasmaABotY[k] = ptsY[k] + dispAY - normY[k] * wA

        plasmaBTopX[k] = ptsX[k] + dispB + normX[k] * wB
        plasmaBTopY[k] = ptsY[k] + dispBY + normY[k] * wB
        plasmaBBotX[k] = ptsX[k] + dispB - normX[k] * wB
        plasmaBBotY[k] = ptsY[k] + dispBY - normY[k] * wB

        plasmaCTopX[k] = ptsX[k] + dispC + normX[k] * wC
        plasmaCTopY[k] = ptsY[k] + dispCY + normY[k] * wC
        plasmaCBotX[k] = ptsX[k] + dispC - normX[k] * wC
        plasmaCBotY[k] = ptsY[k] + dispCY - normY[k] * wC
    }

    val plasmaPathA = Path().apply {
        moveTo(plasmaATopX[0], plasmaATopY[0])
        for (k in 1..samples) lineTo(plasmaATopX[k], plasmaATopY[k])
        for (k in samples downTo 0) lineTo(plasmaABotX[k], plasmaABotY[k])
        close()
    }

    val plasmaPathB = Path().apply {
        moveTo(plasmaBTopX[0], plasmaBTopY[0])
        for (k in 1..samples) lineTo(plasmaBTopX[k], plasmaBTopY[k])
        for (k in samples downTo 0) lineTo(plasmaBBotX[k], plasmaBBotY[k])
        close()
    }

    val plasmaPathC = Path().apply {
        moveTo(plasmaCTopX[0], plasmaCTopY[0])
        for (k in 1..samples) lineTo(plasmaCTopX[k], plasmaCTopY[k])
        for (k in samples downTo 0) lineTo(plasmaCBotX[k], plasmaCBotY[k])
        close()
    }

    // Dải Plasma A (Hồng Neon - Magenta rực rỡ)
    drawPath(
        path = plasmaPathA,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFFF2A85).copy(alpha = plasmaAlpha * 0.90f),
                Color(0xFFEC4899).copy(alpha = plasmaAlpha),
                Color(0xFFD946EF).copy(alpha = plasmaAlpha * 0.95f),
                Color(0xFFFF2A85).copy(alpha = plasmaAlpha * 0.90f)
            ),
            startY = startY,
            endY = endY
        )
    )

    // Dải Plasma B (Vàng Hổ Phách - Cam Lửa rực sáng)
    drawPath(
        path = plasmaPathB,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFFFCC00).copy(alpha = plasmaAlpha * 0.95f),
                Color(0xFFFFA050).copy(alpha = plasmaAlpha),
                Color(0xFFFF3B30).copy(alpha = plasmaAlpha * 0.90f),
                Color(0xFFFFD166).copy(alpha = plasmaAlpha * 0.95f)
            ),
            startY = startY,
            endY = endY
        )
    )

    // Dải Plasma C (Xanh Ngọc Bích Starlight)
    drawPath(
        path = plasmaPathC,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFF51FAC1).copy(alpha = plasmaAlpha * 0.85f),
                Color(0xFF00F5FF).copy(alpha = plasmaAlpha * 0.95f),
                Color(0xFFA855F7).copy(alpha = plasmaAlpha * 0.75f)
            ),
            startY = startY,
            endY = endY
        )
    )

    // ── 3. Lõi dung nham plasma rực lửa ở trung tâm (White-Hot Core Spine) ──
    val coreAlpha = if (isActive) 0.98f * alphaScale else 0.68f * alphaScale

    // Vệt hào quang phát sáng lõi
    drawPath(
        path = mainPath,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFFFA050).copy(alpha = coreAlpha * 0.70f),
                Color(0xFFFF2A85).copy(alpha = coreAlpha * 0.85f),
                Color(0xFFFFD166).copy(alpha = coreAlpha * 0.80f),
                Color(0xFFFFA050).copy(alpha = coreAlpha * 0.70f)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
    )

    // Lõi vàng kim starlight
    drawPath(
        path = mainPath,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFFFF0B3).copy(alpha = coreAlpha * 0.95f),
                Color(0xFFFFD166).copy(alpha = coreAlpha),
                Color(0xFFFFA050).copy(alpha = coreAlpha * 0.95f)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Tia laser trắng trung tâm
    drawPath(
        path = mainPath,
        color = Color.White.copy(alpha = coreAlpha),
        style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round)
    )

    // ── 4. Traveling Star Pulse Orb (Ngôi sao truyền từ hành tinh này sang hành tinh khác) ──
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
                    Color(0xEEFFFFFF),
                    Color(0x99FFA050),
                    Color(0x55FF2A85),
                    Color.Transparent
                ),
                center = Offset(pulseX, pulseY),
                radius = 24.dp.toPx()
            ),
            radius = 24.dp.toPx(),
            center = Offset(pulseX, pulseY)
        )
        // Radiant fiery core
        drawCircle(
            color = Color(0xFFFFD166),
            radius = 7.dp.toPx(),
            center = Offset(pulseX, pulseY)
        )
        // White center spark
        drawCircle(
            color = Color.White,
            radius = 3.2.dp.toPx(),
            center = Offset(pulseX, pulseY)
        )
    }
}
