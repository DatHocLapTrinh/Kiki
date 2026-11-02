package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Nền Không Gian Vũ Trụ Siêu Thực Dịu Mắt (Subtle Cosmic Deep Space Nebula Background):
 * - Được tinh chỉnh độ sáng dịu mắt (Soft Ambient Lighting) đóng vai trò làm nền phụ trợ,
 *   tuyệt đối không lấn át hay che khuất các hành tinh và lộ trình học tập ở tiền cảnh.
 * - Dải tinh vân cực quang tím-hồng và lam điện lan tỏa mềm mại, êm ái.
 * - Các ngôi sao 4 cánh và ngàn vì sao li ti được giảm độ sáng xuống mức tinh tế, thanh thoát.
 * - Vành đai tiểu hành tinh 3D đổ bóng tự nhiên hòa vào không gian vũ trụ.
 */
@Composable
fun CosmicPhotorealisticSpaceBackground(
    currentX: Float = 0f,
    currentY: Float = 0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic_bg_anim")

    // Nhịp trôi nhẹ của tinh vân khí quyển
    val nebulaDrift by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nebula_drift"
    )

    // Nhịp thở phát quang dịu nhẹ của các vì sao (biên độ nhỏ êm mắt)
    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_twinkle"
    )

    // Góc tự xoay chậm của các khối tiểu hành tinh đá
    val asteroidTumble by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(42000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "asteroid_tumble"
    )

    // Khởi tạo danh sách sao li ti nền dịu mắt (Seed cố định)
    val backgroundStars = remember {
        val random = java.util.Random(133742L)
        List(170) {
            val x = random.nextFloat()
            val y = random.nextFloat()
            val size = when {
                random.nextFloat() < 0.82f -> 0.4f + random.nextFloat() * 0.45f // Sao chấm nhỏ li ti (0.4 - 0.85dp)
                else                       -> 0.85f + random.nextFloat() * 0.45f // Sao vừa nhẹ (0.85 - 1.3dp)
            }
            // Độ sáng dịu mắt (10% - 32%) để đóng đúng vai trò nền phụ trợ
            val baseAlpha = 0.10f + random.nextFloat() * 0.22f
            val color = when (random.nextInt(4)) {
                0 -> Color(0xDDE0F2FE) // Xanh băng dịu
                1 -> Color(0xCCF3E8FF) // Tím ngọc trai mờ
                2 -> Color(0xCCDDD6FE) // Tím lavender
                else -> Color(0xEEFFFFFF) // Trắng dịu
            }
            CosmicBackgroundStar(
                xFrac = x,
                yFrac = y,
                sizeDp = size,
                baseAlpha = baseAlpha,
                color = color,
                twinkleSpeed = 0.5f + random.nextFloat() * 0.5f,
                phaseOffset = random.nextFloat() * 6.28f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // ─── 1. Chân Không Vũ Trụ Đen Sâu Thẳm (Deep Space Vacuum Foundation) ─────
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF02000E),
                    Color(0xFF050019),
                    Color(0xFF070122),
                    Color(0xFF020010)
                )
            )
        )

        // Độ dịch chuyển thị sai Parallax theo ngón tay người dùng
        val parallaxNebulaX = currentX * 6.dp.toPx()
        val parallaxNebulaY = currentY * 6.dp.toPx()
        val parallaxStarsX = currentX * 12.dp.toPx()
        val parallaxStarsY = currentY * 12.dp.toPx()
        val parallaxAsteroidsX = currentX * 22.dp.toPx()
        val parallaxAsteroidsY = currentY * 22.dp.toPx()

        // ─── 2. Dải Tinh Vân Thiên Hà Chạy Chéo Trên (Upper Diagonal Violet Nebula) ─
        withTransform({
            translate(parallaxNebulaX + nebulaDrift, parallaxNebulaY + (nebulaDrift * 0.35f))
        }) {
            // Lớp 1: Khí ion tím thẫm lan tỏa nền rộng êm ái
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x354A044E), Color(0x153B0764), Color.Transparent),
                    center = Offset(w * 0.80f, h * 0.16f),
                    radius = w * 0.58f
                ),
                center = Offset(w * 0.80f, h * 0.16f),
                radius = w * 0.58f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x40581C87), Color(0x184A044E), Color.Transparent),
                    center = Offset(w * 0.55f, h * 0.28f),
                    radius = w * 0.52f
                ),
                center = Offset(w * 0.55f, h * 0.28f),
                radius = w * 0.52f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x456B21A8), Color(0x1C3B0764), Color.Transparent),
                    center = Offset(w * 0.36f, h * 0.43f),
                    radius = w * 0.55f
                ),
                center = Offset(w * 0.36f, h * 0.43f),
                radius = w * 0.55f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x30581C87), Color(0x123B0764), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.54f),
                    radius = w * 0.42f
                ),
                center = Offset(w * 0.15f, h * 0.54f),
                radius = w * 0.42f
            )

            // Lớp 2: Luồng mây Magenta & Ultraviolet dịu nhẹ, không lóa
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x409333EA), Color(0x1E7C3AED), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.22f),
                    radius = w * 0.32f
                ),
                center = Offset(w * 0.70f, h * 0.22f),
                radius = w * 0.32f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x45C026D3), Color(0x209333EA), Color.Transparent),
                    center = Offset(w * 0.48f, h * 0.35f),
                    radius = w * 0.30f
                ),
                center = Offset(w * 0.48f, h * 0.35f),
                radius = w * 0.30f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x48DA46EF), Color(0x22C026D3), Color.Transparent),
                    center = Offset(w * 0.362f, h * 0.433f),
                    radius = w * 0.32f
                ),
                center = Offset(w * 0.362f, h * 0.433f),
                radius = w * 0.32f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x35A855F7), Color(0x156B21A8), Color.Transparent),
                    center = Offset(w * 0.24f, h * 0.49f),
                    radius = w * 0.24f
                ),
                center = Offset(w * 0.24f, h * 0.49f),
                radius = w * 0.24f
            )

            // Lớp 3: Bụi khí plasma trung tâm mềm mại êm dịu (Loại bỏ hoàn toàn đốm trắng chói trước đây)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x35E879F9), Color(0x12C026D3), Color.Transparent),
                    center = Offset(w * 0.362f, h * 0.433f),
                    radius = w * 0.16f
                ),
                center = Offset(w * 0.362f, h * 0.433f),
                radius = w * 0.16f
            )
        }

        // ─── 3. Dải Tinh Vân Cung Cực Lam-Tím Dưới Đáy (Lower Sweeping Cyan Nebula) ──
        withTransform({
            translate(parallaxNebulaX - nebulaDrift * 0.6f, parallaxNebulaY - nebulaDrift * 0.25f)
        }) {
            // Nền mây lam đậm & chàm tím uốn quanh góc dưới
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x354C1D95), Color(0x120891B2), Color.Transparent),
                    center = Offset(w * 0.68f, h * 0.77f),
                    radius = w * 0.48f
                ),
                center = Offset(w * 0.68f, h * 0.77f),
                radius = w * 0.48f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x400E7490), Color(0x1C6D28D9), Color.Transparent),
                    center = Offset(w * 0.38f, h * 0.86f),
                    radius = w * 0.48f
                ),
                center = Offset(w * 0.38f, h * 0.86f),
                radius = w * 0.48f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x284338CA), Color(0x1006B6D4), Color.Transparent),
                    center = Offset(w * 0.12f, h * 0.95f),
                    radius = w * 0.42f
                ),
                center = Offset(w * 0.12f, h * 0.95f),
                radius = w * 0.42f
            )

            // Dải lam ngọc & cyan điện quang dịu nhẹ
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x4006B6D4), Color(0x180284C7), Color.Transparent),
                    center = Offset(w * 0.52f, h * 0.81f),
                    radius = w * 0.28f
                ),
                center = Offset(w * 0.52f, h * 0.81f),
                radius = w * 0.28f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x4538BDF8), Color(0x200891B2), Color.Transparent),
                    center = Offset(w * 0.375f, h * 0.865f),
                    radius = w * 0.26f
                ),
                center = Offset(w * 0.375f, h * 0.865f),
                radius = w * 0.26f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3006B6D4), Color(0x120369A1), Color.Transparent),
                    center = Offset(w * 0.22f, h * 0.93f),
                    radius = w * 0.22f
                ),
                center = Offset(w * 0.22f, h * 0.93f),
                radius = w * 0.22f
            )

            // Lõi ươm sao lam dịu mắt
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x4067E8F9), Color(0x1506B6D4), Color.Transparent),
                    center = Offset(w * 0.375f, h * 0.865f),
                    radius = w * 0.12f
                ),
                center = Offset(w * 0.375f, h * 0.865f),
                radius = w * 0.12f
            )
        }

        // ─── 4. Ngân Hà Ngàn Vì Sao Li Ti Nền (Subtle Background Micro Stars) ──────
        withTransform({
            translate(parallaxStarsX, parallaxStarsY)
        }) {
            backgroundStars.forEach { star ->
                val sx = star.xFrac * w
                val sy = star.yFrac * h
                val twinkleMod = 0.88f + 0.24f * sin((starTwinkle * star.twinkleSpeed) + star.phaseOffset)
                val alpha = (star.baseAlpha * twinkleMod).coerceIn(0.08f, 0.45f)
                val r = star.sizeDp.dp.toPx()

                drawCircle(
                    color = star.color.copy(alpha = alpha),
                    radius = r,
                    center = Offset(sx, sy)
                )
            }
        }

        // ─── 5. Các Ngôi Sao 4 Cánh Thanh Thoát (Delicate Background Cross Stars) ───
        withTransform({
            translate(parallaxStarsX, parallaxStarsY)
        }) {
            // 1. Sao Tinh Vân Giữa (Subtle Center Star): (0.362, 0.433) - Giảm kích thước và độ chói
            drawCosmicDiffractionStar(
                cx = w * 0.362f,
                cy = h * 0.433f,
                spikeH = 22.dp.toPx(),
                spikeV = 26.dp.toPx(),
                coreRadius = 1.3.dp.toPx(),
                haloRadius = 13.dp.toPx(),
                haloColor = Color(0xFFF472B6),
                pulse = starTwinkle
            )

            // 2. Sao Lõi Lam Dưới Đáy (Lower Cyan Star): (0.375, 0.865)
            drawCosmicDiffractionStar(
                cx = w * 0.375f,
                cy = h * 0.865f,
                spikeH = 18.dp.toPx(),
                spikeV = 22.dp.toPx(),
                coreRadius = 1.2.dp.toPx(),
                haloRadius = 11.dp.toPx(),
                haloColor = Color(0xFF22D3EE),
                pulse = starTwinkle
            )

            // 3. Sao Xanh Lam Góc Trên-Trái (Top-Left Blue Star): (0.117, 0.175)
            drawCosmicDiffractionStar(
                cx = w * 0.117f,
                cy = h * 0.175f,
                spikeH = 18.dp.toPx(),
                spikeV = 22.dp.toPx(),
                coreRadius = 1.2.dp.toPx(),
                haloRadius = 11.dp.toPx(),
                haloColor = Color(0xFF38BDF8),
                pulse = starTwinkle
            )

            // 4. Sao Lam Giữa-Phải (Mid-Right Azure Star): (0.728, 0.440)
            drawCosmicDiffractionStar(
                cx = w * 0.728f,
                cy = h * 0.440f,
                spikeH = 15.dp.toPx(),
                spikeV = 18.dp.toPx(),
                coreRadius = 1.1.dp.toPx(),
                haloRadius = 9.dp.toPx(),
                haloColor = Color(0xFF60A5FA),
                pulse = starTwinkle
            )

            // 5. Sao Tím Hồng Dưới-Phải (Lower-Right Violet Star): (0.915, 0.455)
            drawCosmicDiffractionStar(
                cx = w * 0.915f,
                cy = h * 0.455f,
                spikeH = 13.dp.toPx(),
                spikeV = 16.dp.toPx(),
                coreRadius = 1.0.dp.toPx(),
                haloRadius = 8.dp.toPx(),
                haloColor = Color(0xFFE879F9),
                pulse = starTwinkle
            )

            // 6. Sao Phụ Dưới-Phải: (0.920, 0.525)
            drawCosmicDiffractionStar(
                cx = w * 0.920f,
                cy = h * 0.525f,
                spikeH = 11.dp.toPx(),
                spikeV = 13.dp.toPx(),
                coreRadius = 0.9.dp.toPx(),
                haloRadius = 6.dp.toPx(),
                haloColor = Color(0xFFA78BFA),
                pulse = starTwinkle
            )

            // 7. Sao Lam Đỉnh Đầu: (0.550, 0.082)
            drawCosmicDiffractionStar(
                cx = w * 0.550f,
                cy = h * 0.082f,
                spikeH = 12.dp.toPx(),
                spikeV = 14.dp.toPx(),
                coreRadius = 0.9.dp.toPx(),
                haloRadius = 7.dp.toPx(),
                haloColor = Color(0xFF93C5FD),
                pulse = starTwinkle
            )
        }

        // ─── 6. Vành Đai Tiểu Hành Tinh Đá 3D (3D Rocky Asteroid Field) ───────────
        withTransform({
            translate(parallaxAsteroidsX, parallaxAsteroidsY)
        }) {
            // 1. Khối Tiểu Hành Tinh Khổng Lồ Nổi Bật Bên Dưới-Phải: (0.845, 0.755)
            draw3DRockyAsteroid(
                cx = w * 0.845f,
                cy = h * 0.755f,
                sizeDp = 42.dp.toPx(),
                rotationDeg = -15f + (asteroidTumble * 0.15f),
                seed = 101L
            )

            // 2. Tiểu hành tinh nhỏ đồng hành bên cạnh: (0.925, 0.720)
            draw3DRockyAsteroid(
                cx = w * 0.925f,
                cy = h * 0.720f,
                sizeDp = 16.dp.toPx(),
                rotationDeg = 35f - (asteroidTumble * 0.20f),
                seed = 102L
            )

            // 3. Tiểu hành tinh vừa ở giữa-dưới: (0.590, 0.660)
            draw3DRockyAsteroid(
                cx = w * 0.590f,
                cy = h * 0.660f,
                sizeDp = 20.dp.toPx(),
                rotationDeg = 45f + (asteroidTumble * 0.18f),
                seed = 103L
            )

            // 4. Tiểu hành tinh chuỗi bên trái: (0.122, 0.656)
            draw3DRockyAsteroid(
                cx = w * 0.122f,
                cy = h * 0.656f,
                sizeDp = 15.dp.toPx(),
                rotationDeg = -40f + (asteroidTumble * 0.25f),
                seed = 104L
            )

            // 5. Tiểu hành tinh vừa bên trái-dưới: (0.051, 0.727)
            draw3DRockyAsteroid(
                cx = w * 0.051f,
                cy = h * 0.727f,
                sizeDp = 22.dp.toPx(),
                rotationDeg = 20f - (asteroidTumble * 0.12f),
                seed = 105L
            )

            // 6. Tiểu hành tinh nhỏ bên trái-trên: (0.044, 0.582)
            draw3DRockyAsteroid(
                cx = w * 0.044f,
                cy = h * 0.582f,
                sizeDp = 13.dp.toPx(),
                rotationDeg = 75f + (asteroidTumble * 0.30f),
                seed = 106L
            )
        }
    }
}

/**
 * Hàm vẽ Ngôi sao 4 cánh phát quang thanh thoát, dịu mắt (Subtle Diffraction Spike Star):
 * - Tia ngang và dọc mảnh, độ sáng dịu mắt để không lấn át giao diện chính
 * - Quầng sáng mềm nhạt
 * - Lõi sao nhỏ tinh tế
 */
private fun DrawScope.drawCosmicDiffractionStar(
    cx: Float,
    cy: Float,
    spikeH: Float,
    spikeV: Float,
    coreRadius: Float,
    haloRadius: Float,
    haloColor: Color,
    pulse: Float = 1.0f
) {
    val actualPulse = pulse.coerceIn(0.85f, 1.15f)

    // 1. Quầng sáng mềm dịu mắt
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                haloColor.copy(alpha = 0.20f * actualPulse),
                haloColor.copy(alpha = 0.05f * actualPulse),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = haloRadius * actualPulse
        ),
        center = Offset(cx, cy),
        radius = haloRadius * actualPulse
    )

    // 2. Tia nhiễu xạ phát quang ngang mảnh thanh thoát
    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                haloColor.copy(alpha = 0.15f * actualPulse),
                Color.White.copy(alpha = 0.40f * actualPulse),
                haloColor.copy(alpha = 0.15f * actualPulse),
                Color.Transparent
            ),
            startX = cx - spikeH * actualPulse,
            endX = cx + spikeH * actualPulse
        ),
        start = Offset(cx - spikeH * actualPulse, cy),
        end = Offset(cx + spikeH * actualPulse, cy),
        strokeWidth = 0.8.dp.toPx()
    )

    // 3. Tia nhiễu xạ phát quang dọc mảnh thanh thoát
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                haloColor.copy(alpha = 0.15f * actualPulse),
                Color.White.copy(alpha = 0.40f * actualPulse),
                haloColor.copy(alpha = 0.15f * actualPulse),
                Color.Transparent
            ),
            startY = cy - spikeV * actualPulse,
            endY = cy + spikeV * actualPulse
        ),
        start = Offset(cx, cy - spikeV * actualPulse),
        end = Offset(cx, cy + spikeV * actualPulse),
        strokeWidth = 0.8.dp.toPx()
    )

    // 4. Lõi sao nhỏ ở tâm
    drawCircle(
        color = Color.White.copy(alpha = 0.65f),
        radius = coreRadius * actualPulse,
        center = Offset(cx, cy)
    )
}

/**
 * Hàm vẽ Tiểu Hành Tinh Đá 3D (3D Faceted Cratered Asteroid):
 * - Chiếu sáng êm dịu, hòa cùng bóng đêm không gian sâu
 */
private fun DrawScope.draw3DRockyAsteroid(
    cx: Float,
    cy: Float,
    sizeDp: Float,
    rotationDeg: Float,
    seed: Long
) {
    val r = sizeDp / 2f

    withTransform({
        translate(cx, cy)
        rotate(rotationDeg)
    }) {
        val random = java.util.Random(seed)
        val vertexCount = 9
        val path = Path()

        val radii = List(vertexCount) {
            r * (0.80f + random.nextFloat() * 0.38f)
        }

        for (i in 0 until vertexCount) {
            val angleRad = (i.toFloat() / vertexCount) * 2f * PI.toFloat()
            val vx = radii[i] * cos(angleRad)
            val vy = radii[i] * sin(angleRad)
            if (i == 0) path.moveTo(vx, vy) else path.lineTo(vx, vy)
        }
        path.close()

        // 1. Tô khối đa giác đá với ánh sáng êm dịu
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF9E8FB2), // Mặt sáng đón ánh sao: xám tím khoáng thạch dịu
                    Color(0xFF5D4E72), // Mặt đá trung gian
                    Color(0xFF2C1E40), // Mặt chuyển tối
                    Color(0xFF10071C), // Bóng tối không gian
                    Color(0xFF07020E)  // Đáy bóng tối
                ),
                start = Offset(-r, -r),
                end = Offset(r, r)
            )
        )

        // 2. Viền phản quang sáng cạnh trên-trái thanh mảnh
        val highlightPath = Path()
        for (i in 4..7) {
            val idx = i % vertexCount
            val angleRad = (idx.toFloat() / vertexCount) * 2f * PI.toFloat()
            val vx = radii[idx] * cos(angleRad)
            val vy = radii[idx] * sin(angleRad)
            if (i == 4) highlightPath.moveTo(vx, vy) else highlightPath.lineTo(vx, vy)
        }
        drawPath(
            path = highlightPath,
            color = Color(0xFFC4B5FD).copy(alpha = 0.45f),
            style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3. Các hố va chạm thiên thạch (Impact Craters)
        // Hố va chạm 1 (Lớn)
        val c1X = -r * 0.22f
        val c1Y = -r * 0.15f
        val c1R = r * 0.28f
        drawCircle(
            color = Color(0xFF140A22),
            radius = c1R,
            center = Offset(c1X, c1Y)
        )
        drawArc(
            color = Color(0xFFC4B5FD).copy(alpha = 0.50f),
            startAngle = 135f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(c1X - c1R, c1Y - c1R),
            size = Size(c1R * 2f, c1R * 2f),
            style = Stroke(width = 0.8.dp.toPx())
        )

        // Hố va chạm 2 (Vừa)
        val c2X = r * 0.18f
        val c2Y = r * 0.22f
        val c2R = r * 0.22f
        drawCircle(
            color = Color(0xFF0E0518),
            radius = c2R,
            center = Offset(c2X, c2Y)
        )
        drawArc(
            color = Color(0xFFA78BFA).copy(alpha = 0.40f),
            startAngle = 140f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(c2X - c2R, c2Y - c2R),
            size = Size(c2R * 2f, c2R * 2f),
            style = Stroke(width = 0.7.dp.toPx())
        )

        // Hố va chạm 3 (Nhỏ)
        val c3X = -r * 0.10f
        val c3Y = r * 0.36f
        val c3R = r * 0.16f
        drawCircle(
            color = Color(0xFF0A0312),
            radius = c3R,
            center = Offset(c3X, c3Y)
        )
    }
}

private data class CosmicBackgroundStar(
    val xFrac: Float,
    val yFrac: Float,
    val sizeDp: Float,
    val baseAlpha: Float,
    val color: Color,
    val twinkleSpeed: Float,
    val phaseOffset: Float
)
