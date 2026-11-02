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
    val dockAlpha = if (isActive) 0.52f * alphaScale else 0.28f * alphaScale
    val dockRadius = 88.dp.toPx()

    // 0.1 Quầng năng lượng bao bọc hành tinh xuất phát (Top planet)
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

    // 0.2 Quầng năng lượng đón nhận tại hành tinh đích đến (Bottom planet)
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

    // ── 1. Cosmic Dust Gas Clouds (Mây khói tinh vân vũ trụ nền - Deep Violet & Magenta) ──
    val cloudAlpha = if (isActive) 0.28f * alphaScale else 0.16f * alphaScale
    val cloudSteps = 9
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

    // ── 2. Branching Plasma Tendrils (Mạng lưới sợi nhánh tinh vân tỏa ra không gian) ──
    val tendrilAlpha = if (isActive) 0.68f * alphaScale else 0.38f * alphaScale
    val tendrilConfigs = listOf(
        Triple(0.12f, 1f, 38f to 14f),
        Triple(0.18f, -1f, 44f to -16f),
        Triple(0.26f, 1f, 52f to 18f),
        Triple(0.32f, -1f, 40f to -12f),
        Triple(0.40f, 1f, 56f to -20f),
        Triple(0.46f, -1f, 48f to 16f),
        Triple(0.54f, 1f, 50f to 18f),
        Triple(0.60f, -1f, 54f to -22f),
        Triple(0.68f, 1f, 42f to -14f),
        Triple(0.74f, -1f, 48f to 16f),
        Triple(0.82f, 1f, 36f to 12f),
        Triple(0.88f, -1f, 32f to -10f)
    )

    for ((t, side, shape) in tendrilConfigs) {
        val (lenDp, curlDp) = shape
        val idx = (t * samples).toInt().coerceIn(0, samples)
        val x0 = ptsX[idx]
        val y0 = ptsY[idx]

        val lengthPx = lenDp.dp.toPx()
        val curlPx = curlDp.dp.toPx()

        val nx = normX[idx] * side
        val ny = normY[idx] * side
        val tx = tanX[idx]
        val ty = tanY[idx]

        val cp1X = x0 + nx * (lengthPx * 0.35f) + tx * (curlPx * 0.2f)
        val cp1Y = y0 + ny * (lengthPx * 0.35f) + ty * (curlPx * 0.2f)
        val cp2X = x0 + nx * (lengthPx * 0.75f) + tx * (curlPx * 0.7f)
        val cp2Y = y0 + ny * (lengthPx * 0.75f) + ty * (curlPx * 0.7f)
        val endTendrilX = x0 + nx * lengthPx + tx * curlPx
        val endTendrilY = y0 + ny * lengthPx + ty * curlPx

        val tendrilPath = Path().apply {
            moveTo(x0, y0)
            cubicTo(cp1X, cp1Y, cp2X, cp2Y, endTendrilX, endTendrilY)
        }

        drawPath(
            path = tendrilPath,
            brush = Brush.linearGradient(
                listOf(
                    Color(0xFFFF5580).copy(alpha = tendrilAlpha * 0.90f),
                    Color(0xFFC026D3).copy(alpha = tendrilAlpha * 0.75f),
                    Color(0xFF7E22CE).copy(alpha = tendrilAlpha * 0.45f),
                    Color(0x003B0764)
                ),
                start = Offset(x0, y0),
                end = Offset(endTendrilX, endTendrilY)
            ),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )

        val tipTwinkle = (0.5f + 0.5f * sin(animPhase * 2f + t * 10f))
        drawCircle(
            color = Color(0xFFFFD166).copy(alpha = tendrilAlpha * tipTwinkle),
            radius = 1.6.dp.toPx(),
            center = Offset(endTendrilX, endTendrilY)
        )
    }

    // ── 3. Braided Multi-Strand Cosmic Fibers (Các chùm sợi plasma đan bện xoắn ốc) ──
    val fiberAlpha = if (isActive) 0.78f * alphaScale else 0.48f * alphaScale

    val fiber1Path = Path()
    val fiber2Path = Path()
    val fiber3Path = Path()
    val fiber4Path = Path()

    for (k in 0..samples) {
        val t = k.toFloat() / samples
        val sinT = sin(PI.toFloat() * t)

        val disp1 = normX[k] * (sin(3.5f * PI.toFloat() * t + animPhase) * 11.dp.toPx() + sin(7f * PI.toFloat() * t) * 3.dp.toPx()) * (0.4f + 0.6f * sinT)
        val disp1Y = normY[k] * (sin(3.5f * PI.toFloat() * t + animPhase) * 11.dp.toPx() + sin(7f * PI.toFloat() * t) * 3.dp.toPx()) * (0.4f + 0.6f * sinT)

        val disp2 = normX[k] * (-cos(3.2f * PI.toFloat() * t + animPhase * 0.8f) * 14.dp.toPx() + cos(6f * PI.toFloat() * t) * 3.5.dp.toPx()) * (0.4f + 0.6f * sinT)
        val disp2Y = normY[k] * (-cos(3.2f * PI.toFloat() * t + animPhase * 0.8f) * 14.dp.toPx() + cos(6f * PI.toFloat() * t) * 3.5.dp.toPx()) * (0.4f + 0.6f * sinT)

        val disp3 = normX[k] * (sin(2.5f * PI.toFloat() * t - animPhase * 0.7f) * 18.dp.toPx() - sin(5f * PI.toFloat() * t) * 4.dp.toPx()) * (0.4f + 0.6f * sinT)
        val disp3Y = normY[k] * (sin(2.5f * PI.toFloat() * t - animPhase * 0.7f) * 18.dp.toPx() - sin(5f * PI.toFloat() * t) * 4.dp.toPx()) * (0.4f + 0.6f * sinT)

        val disp4 = normX[k] * (cos(4.8f * PI.toFloat() * t + animPhase * 1.3f) * 6.dp.toPx() + sin(9f * PI.toFloat() * t) * 2.dp.toPx()) * (0.4f + 0.6f * sinT)
        val disp4Y = normY[k] * (cos(4.8f * PI.toFloat() * t + animPhase * 1.3f) * 6.dp.toPx() + sin(9f * PI.toFloat() * t) * 2.dp.toPx()) * (0.4f + 0.6f * sinT)

        val p1x = ptsX[k] + disp1
        val p1y = ptsY[k] + disp1Y
        val p2x = ptsX[k] + disp2
        val p2y = ptsY[k] + disp2Y
        val p3x = ptsX[k] + disp3
        val p3y = ptsY[k] + disp3Y
        val p4x = ptsX[k] + disp4
        val p4y = ptsY[k] + disp4Y

        if (k == 0) {
            fiber1Path.moveTo(p1x, p1y)
            fiber2Path.moveTo(p2x, p2y)
            fiber3Path.moveTo(p3x, p3y)
            fiber4Path.moveTo(p4x, p4y)
        } else {
            fiber1Path.lineTo(p1x, p1y)
            fiber2Path.lineTo(p2x, p2y)
            fiber3Path.lineTo(p3x, p3y)
            fiber4Path.lineTo(p4x, p4y)
        }
    }

    // Fiber 3 (Sợi tím vũ trụ bề rộng mềm mại)
    drawPath(
        path = fiber3Path,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFF7E22CE).copy(alpha = fiberAlpha * 0.70f),
                Color(0xFFA855F7).copy(alpha = fiberAlpha * 0.85f),
                Color(0xFF9333EA).copy(alpha = fiberAlpha * 0.75f)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round)
    )

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

    // ── 4. The Fiery Burning Core Spine (Lõi dung nham bụi sao vàng hồng rực lửa) ──
    val coreAlpha = if (isActive) 0.96f * alphaScale else 0.65f * alphaScale

    // 4.1 Vệt phát quang hổ phách - hoa hồng
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

    // 4.2 Lõi sợi vàng kim rực rỡ
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

    // 4.3 Tia sáng trắng trung tâm
    drawPath(
        path = mainPath,
        color = Color.White.copy(alpha = coreAlpha * 0.95f),
        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
    )

    // ── 5. Field of Scattered Stardust (Bầu trời bụi sao lơ lửng khắp dải mạng nhện vũ trụ) ──
    val stardustCount = 52
    for (i in 0 until stardustCount) {
        val seed = i * 1337 + 73
        val t = ((i * 0.0192f) % 0.88f) + 0.06f
        val idx = (t * samples).toInt().coerceIn(0, samples - 1)
        val subT = (t * samples) - idx
        val baseX = ptsX[idx] + (ptsX[idx + 1] - ptsX[idx]) * subT
        val baseY = ptsY[idx] + (ptsY[idx + 1] - ptsY[idx]) * subT

        val latOffset = (sin(seed * 0.31f) * 34.dp.toPx())
        val starX = baseX + normX[idx] * latOffset
        val starY = baseY + normY[idx] * latOffset

        val starRadius = (1.1f + ((seed % 4) * 0.45f)).dp.toPx()
        val twinkleSpeed = 1.8f + (seed % 3) * 0.8f
        val twinkle = (0.45f + 0.55f * sin(animPhase * twinkleSpeed + seed.toFloat()))

        val starColor = when (i % 5) {
            0 -> Color(0xFFFFD166) // Gold star
            1 -> Color(0xFFFF6699) // Coral/rose star
            2 -> Color(0xFFC084FC) // Violet star
            3 -> Color(0xFF51FAC1) // Cyan star
            else -> Color.White   // Pure white spark
        }

        drawCircle(
            color = starColor.copy(alpha = (starColor.alpha * twinkle * alphaScale).coerceIn(0f, 1f)),
            radius = starRadius,
            center = Offset(starX, starY)
        )
    }

    // ── 6. Traveling Star Pulse Orb (Ngôi sao truyền từ hành tinh này sang hành tinh khác) ──
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
