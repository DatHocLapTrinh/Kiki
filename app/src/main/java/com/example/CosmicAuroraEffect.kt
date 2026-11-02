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

    val samples = 36
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

    // ── 0. Gravitational Lensing & Spacetime Distortion Arcs (Thấu kính hấp dẫn Einstein) ──
    val lensAlpha = if (isActive) 0.65f * alphaScale else 0.32f * alphaScale
    // Warped Einstein Lensing Arcs around Planet 1 (Top)
    drawArc(
        brush = Brush.sweepGradient(
            listOf(
                Color.Transparent,
                Color(0x8851FAC1),
                Color(0xDDFFD166),
                Color(0x99FFA050),
                Color.Transparent
            ),
            center = Offset(startX, startY)
        ),
        startAngle = 30f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(startX - 74.dp.toPx(), startY - 74.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(148.dp.toPx(), 148.dp.toPx()),
        style = Stroke(width = 1.8.dp.toPx())
    )
    drawArc(
        color = Color(0x77FF8AD8).copy(alpha = lensAlpha),
        startAngle = 45f,
        sweepAngle = 100f,
        useCenter = false,
        topLeft = Offset(startX - 86.dp.toPx(), startY - 86.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(172.dp.toPx(), 172.dp.toPx()),
        style = Stroke(width = 1.0.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx())))
    )

    // Warped Einstein Lensing Arcs around Planet 2 (Bottom)
    drawArc(
        brush = Brush.sweepGradient(
            listOf(
                Color.Transparent,
                Color(0x88FFA050),
                Color(0xDDFF5580),
                Color(0x99C084FC),
                Color.Transparent
            ),
            center = Offset(endX, endY)
        ),
        startAngle = 210f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(endX - 74.dp.toPx(), endY - 74.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(148.dp.toPx(), 148.dp.toPx()),
        style = Stroke(width = 1.8.dp.toPx())
    )
    drawArc(
        color = Color(0x7751FAC1).copy(alpha = lensAlpha),
        startAngle = 225f,
        sweepAngle = 100f,
        useCenter = false,
        topLeft = Offset(endX - 86.dp.toPx(), endY - 86.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(172.dp.toPx(), 172.dp.toPx()),
        style = Stroke(width = 1.0.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx())))
    )

    // ── 1. Atmosphere Reflection & Volumetric Backlighting (Khí quyển phản chiếu năng lượng) ──
    val atmosAlpha = if (isActive) 0.58f * alphaScale else 0.30f * alphaScale

    // Planet 1 lower atmosphere rim reflection
    val p1RimCenter = Offset(startX + dx * 0.05f, startY + 52.dp.toPx())
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color(0xFFFF9500).copy(alpha = atmosAlpha * 0.95f),
                Color(0xFFFF2D55).copy(alpha = atmosAlpha * 0.65f),
                Color(0xFF581C87).copy(alpha = atmosAlpha * 0.30f),
                Color.Transparent
            ),
            center = p1RimCenter,
            radius = 64.dp.toPx()
        ),
        radius = 64.dp.toPx(),
        center = p1RimCenter
    )

    // Planet 2 upper atmosphere rim reflection
    val p2RimCenter = Offset(endX - dx * 0.05f, endY - 52.dp.toPx())
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color(0xFF00F5FF).copy(alpha = atmosAlpha * 0.95f),
                Color(0xFFAF52DE).copy(alpha = atmosAlpha * 0.65f),
                Color(0xFF1E1035).copy(alpha = atmosAlpha * 0.30f),
                Color.Transparent
            ),
            center = p2RimCenter,
            radius = 64.dp.toPx()
        ),
        radius = 64.dp.toPx(),
        center = p2RimCenter
    )

    // ── 2. Swirling Cosmic Dust Bridge (Cầu bụi vũ trụ dày đặc cuộn xoáy 75dp) ──
    val dustBridgeAlpha = if (isActive) 0.35f * alphaScale else 0.20f * alphaScale
    val dustSteps = 11
    for (d in 1 until dustSteps) {
        val t = d.toFloat() / dustSteps
        val idx = (t * samples).toInt().coerceIn(0, samples)
        val swirlAngle = (d * 1.8f + animPhase * 0.5f)
        val swirlOffset = (sin(swirlAngle) * 22.dp.toPx())
        val swirlX = ptsX[idx] + normX[idx] * swirlOffset
        val swirlY = ptsY[idx] + normY[idx] * swirlOffset
        val blobR = (52 + (d % 4) * 8).dp.toPx()

        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color(0xFF581C87).copy(alpha = dustBridgeAlpha * 0.85f),
                    Color(0xFF3B0764).copy(alpha = dustBridgeAlpha * 0.55f),
                    Color(0xFF19062E).copy(alpha = dustBridgeAlpha * 0.25f),
                    Color.Transparent
                ),
                center = Offset(swirlX, swirlY),
                radius = blobR
            ),
            radius = blobR,
            center = Offset(swirlX, swirlY)
        )
    }

    // ── 3. Dark Matter Threads (Sợi vật chất tối - Volumetric Deep Shadows) ────
    val darkMatterPath1 = Path()
    val darkMatterPath2 = Path()
    for (k in 0..samples) {
        val t = k.toFloat() / samples
        val sinT = sin(PI.toFloat() * t)
        val d1X = ptsX[k] + normX[k] * (sin(3f * PI.toFloat() * t + animPhase * 0.6f) * 12.dp.toPx()) * (0.3f + 0.7f * sinT)
        val d1Y = ptsY[k] + normY[k] * (sin(3f * PI.toFloat() * t + animPhase * 0.6f) * 12.dp.toPx()) * (0.3f + 0.7f * sinT)
        val d2X = ptsX[k] + normX[k] * (-cos(2.8f * PI.toFloat() * t + animPhase * 0.5f) * 16.dp.toPx()) * (0.3f + 0.7f * sinT)
        val d2Y = ptsY[k] + normY[k] * (-cos(2.8f * PI.toFloat() * t + animPhase * 0.5f) * 16.dp.toPx()) * (0.3f + 0.7f * sinT)
        if (k == 0) {
            darkMatterPath1.moveTo(d1X, d1Y)
            darkMatterPath2.moveTo(d2X, d2Y)
        } else {
            darkMatterPath1.lineTo(d1X, d1Y)
            darkMatterPath2.lineTo(d2X, d2Y)
        }
    }
    drawPath(
        path = darkMatterPath1,
        color = Color(0xDD080312),
        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
    )
    drawPath(
        path = darkMatterPath2,
        color = Color(0xBB0D051C),
        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
    )

    // ── 4. Luminous Swirling Plasma Tendrils in Celestial Dance (Double Helix) ──
    val plasmaAlpha = if (isActive) 0.85f * alphaScale else 0.50f * alphaScale

    val helixAlphaPath = Path() // Solar Gold / Amber Tendril
    val helixBetaPath = Path()  // Electric Violet / Magenta Tendril
    val warpGammaPath = Path()  // Cyan Gravitational Warp Thread

    for (k in 0..samples) {
        val t = k.toFloat() / samples
        val sinT = sin(PI.toFloat() * t)
        val envelope = (0.35f + 0.65f * sinT)

        val helixPhase = 3.6f * PI.toFloat() * t + animPhase
        val hRadius = 15.dp.toPx() * envelope

        val h1X = ptsX[k] + normX[k] * (sin(helixPhase) * hRadius)
        val h1Y = ptsY[k] + normY[k] * (sin(helixPhase) * hRadius)

        val h2X = ptsX[k] + normX[k] * (-sin(helixPhase) * hRadius)
        val h2Y = ptsY[k] + normY[k] * (-sin(helixPhase) * hRadius)

        val h3X = ptsX[k] + normX[k] * (cos(helixPhase * 1.4f) * (hRadius * 0.6f))
        val h3Y = ptsY[k] + normY[k] * (cos(helixPhase * 1.4f) * (hRadius * 0.6f))

        if (k == 0) {
            helixAlphaPath.moveTo(h1X, h1Y)
            helixBetaPath.moveTo(h2X, h2Y)
            warpGammaPath.moveTo(h3X, h3Y)
        } else {
            helixAlphaPath.lineTo(h1X, h1Y)
            helixBetaPath.lineTo(h2X, h2Y)
            warpGammaPath.lineTo(h3X, h3Y)
        }
    }

    // Tendril Beta (Violet-Magenta Cosmic Stream)
    drawPath(
        path = helixBetaPath,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFFF2D55).copy(alpha = plasmaAlpha * 0.80f),
                Color(0xFFAF52DE).copy(alpha = plasmaAlpha * 0.95f),
                Color(0xFF5856D6).copy(alpha = plasmaAlpha * 0.85f)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 5.0.dp.toPx(), cap = StrokeCap.Round)
    )

    // Tendril Alpha (Solar Gold - Amber Luminous Stream)
    drawPath(
        path = helixAlphaPath,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFFFCC00).copy(alpha = plasmaAlpha * 0.90f),
                Color(0xFFFF9500).copy(alpha = plasmaAlpha * 1.0f),
                Color(0xFFFF3B30).copy(alpha = plasmaAlpha * 0.85f)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 5.2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Tendril Gamma (Cyan Starlight Warp Thread)
    drawPath(
        path = warpGammaPath,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFF51FAC1).copy(alpha = plasmaAlpha * 0.85f),
                Color(0xFF00F5FF).copy(alpha = plasmaAlpha * 0.95f),
                Color(0xFFE056FD).copy(alpha = plasmaAlpha * 0.75f)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
    )

    // ── 5. White-Hot Singularity Core (Lõi điểm kì dị ánh sáng trắng - vàng) ────
    val coreAlpha = if (isActive) 0.98f * alphaScale else 0.68f * alphaScale

    // 5.1 Radiant Volumetric Plasma Glow
    drawPath(
        path = mainPath,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFFF9500).copy(alpha = coreAlpha * 0.70f),
                Color(0xFFFF2D55).copy(alpha = coreAlpha * 0.85f),
                Color(0xFFFFCC00).copy(alpha = coreAlpha * 0.80f),
                Color(0xFFFF9500).copy(alpha = coreAlpha * 0.70f)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 9.5.dp.toPx(), cap = StrokeCap.Round)
    )

    // 5.2 Glowing golden filament
    drawPath(
        path = mainPath,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFFFF0B3).copy(alpha = coreAlpha * 0.95f),
                Color(0xFFFFD166).copy(alpha = coreAlpha * 1.0f),
                Color(0xFFFFA050).copy(alpha = coreAlpha * 0.95f)
            ),
            startY = startY,
            endY = endY
        ),
        style = Stroke(width = 3.6.dp.toPx(), cap = StrokeCap.Round)
    )

    // 5.3 Pure white-hot laser filament
    drawPath(
        path = mainPath,
        color = Color.White.copy(alpha = coreAlpha),
        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
    )

    // ── 6. Swirling Cosmic Dust & Accretion Sparkles (Bụi bồi tụ & Hạt photon) ──
    val dustCount = 56
    for (i in 0 until dustCount) {
        val seed = i * 1973 + 101
        val t = ((i * 0.0178f) % 0.88f) + 0.06f
        val idx = (t * samples).toInt().coerceIn(0, samples - 1)
        val subT = (t * samples) - idx
        val baseX = ptsX[idx] + (ptsX[idx + 1] - ptsX[idx]) * subT
        val baseY = ptsY[idx] + (ptsY[idx + 1] - ptsY[idx]) * subT

        val orbitAngle = (seed * 0.25f + animPhase * (1.2f + (seed % 3) * 0.4f))
        val orbitRadius = (sin(seed * 0.41f) * 36.dp.toPx())
        val starX = baseX + normX[idx] * (cos(orbitAngle) * orbitRadius) + tanX[idx] * (sin(orbitAngle) * 4.dp.toPx())
        val starY = baseY + normY[idx] * (cos(orbitAngle) * orbitRadius) + tanY[idx] * (sin(orbitAngle) * 4.dp.toPx())

        val starR = (1.2f + ((seed % 4) * 0.5f)).dp.toPx()
        val twinkle = (0.4f + 0.6f * sin(animPhase * 2.2f + seed.toFloat()))

        val starColor = when (i % 6) {
            0 -> Color(0xFFFFD166) // Gold accretion
            1 -> Color(0xFFFF3B30) // Coral flame
            2 -> Color(0xFF00F5FF) // Cyan warp
            3 -> Color(0xFFAF52DE) // Violet gas
            4 -> Color(0xFFFF8AD8) // Pink nebula
            else -> Color.White   // Pure photon spark
        }

        drawCircle(
            color = starColor.copy(alpha = (starColor.alpha * twinkle * alphaScale).coerceIn(0f, 1f)),
            radius = starR,
            center = Offset(starX, starY)
        )
    }

    // ── 7. Traveling Gravitational Pulse & Einstein Ring (Ngôi sao truyền dẫn & Vòng thấu kính) ──
    if (isActive) {
        val pulseT = ((animPhase / (2f * PI.toFloat())) % 1f + 1f) % 1f
        val pulseIdx = (pulseT * samples).toInt().coerceIn(0, samples - 1)
        val subT = (pulseT * samples) - pulseIdx
        val pulseX = ptsX[pulseIdx] + (ptsX[pulseIdx + 1] - ptsX[pulseIdx]) * subT
        val pulseY = ptsY[pulseIdx] + (ptsY[pulseIdx + 1] - ptsY[pulseIdx]) * subT

        // Mini Einstein Lensing Ring surrounding the pulse
        drawCircle(
            color = Color(0x6651FAC1),
            radius = 28.dp.toPx(),
            center = Offset(pulseX, pulseY),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Volumetric energy flare
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xCCFFA050),
                    Color(0x66FF2D55),
                    Color.Transparent
                ),
                center = Offset(pulseX, pulseY),
                radius = 24.dp.toPx()
            ),
            radius = 24.dp.toPx(),
            center = Offset(pulseX, pulseY)
        )

        // Fiery golden core
        drawCircle(
            color = Color(0xFFFFCC00),
            radius = 7.5.dp.toPx(),
            center = Offset(pulseX, pulseY)
        )

        // Pure white singularity center
        drawCircle(
            color = Color.White,
            radius = 3.5.dp.toPx(),
            center = Offset(pulseX, pulseY)
        )
    }
}
