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
 * Nền Không Gian Vũ Trụ Siêu Thực (Cosmic Deep Space Nebula & Asteroid Field):
 * 1. Dải Tinh Vân Thiên Hà Cực Quang Tím-Hồng (Upper Diagonal Violet-Magenta Nebula River)
 *    chạy chéo từ góc trên-phải qua giữa-trái với cụm sao siêu phát quang (Star Flare).
 * 2. Dải Tinh Vân Lam Điện-Tím (Lower Sweeping Cyan-Blue Nebula Arc) uốn cong ở nửa dưới màn hình
 *    với vườn ươm sao sáng chói (Stellar Nursery Core).
 * 3. Hệ thống Ngàn Sao Đa Tầng & Các Ngôi Sao 4 Cánh Phát Quang (Diffraction Spike Stars)
 *    tỏa sáng lấp lánh như kính thiên văn không gian Hubble/James Webb.
 * 4. Vành Đai Tiểu Hành Tinh & Thiên Thạch Đá 3D (3D Cratered Asteroid Field)
 *    trôi dạt chân thực ở tiền cảnh với hiệu ứng nổi khối, miệng hố va chạm và đổ bóng theo chiều sâu.
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
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nebula_drift"
    )

    // Nhịp thở phát quang của các vì sao 4 cánh và lõi tinh vân
    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_twinkle"
    )

    // Góc tự xoay chậm của các khối tiểu hành tinh đá
    val asteroidTumble by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "asteroid_tumble"
    )

    // Khởi tạo danh sách sao ngẫu nhiên cố định (Seed cố định để không bị giật khi re-compose)
    val backgroundStars = remember {
        val random = java.util.Random(133742L)
        List(190) {
            val x = random.nextFloat()
            val y = random.nextFloat()
            val size = when {
                random.nextFloat() < 0.70f -> 0.7f + random.nextFloat() * 0.7f // Sao nhỏ
                random.nextFloat() < 0.90f -> 1.4f + random.nextFloat() * 0.8f // Sao vừa
                else                       -> 2.2f + random.nextFloat() * 0.9f // Sao lớn
            }
            val baseAlpha = 0.35f + random.nextFloat() * 0.60f
            val color = when (random.nextInt(5)) {
                0 -> Color.White
                1 -> Color(0xFFE0F2FE) // Xanh băng
                2 -> Color(0xFFF3E8FF) // Tím ngọc trai
                3 -> Color(0xFFDDD6FE) // Tím nhạt
                else -> Color(0xFFBAE6FD) // Lam ngọc
            }
            CosmicBackgroundStar(
                xFrac = x,
                yFrac = y,
                sizeDp = size,
                baseAlpha = baseAlpha,
                color = color,
                twinkleSpeed = 0.7f + random.nextFloat() * 0.8f,
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
                    Color(0xFF02000E), // Đen tím cực sâu trên cùng
                    Color(0xFF06001E), // Tím đen vũ trụ trung tâm
                    Color(0xFF080126), // Lam đen thẳm
                    Color(0xFF020010)  // Đen tuyền đáy màn hình
                )
            )
        )

        // Độ dịch chuyển thị sai Parallax theo ngón tay người dùng
        val parallaxNebulaX = currentX * 8.dp.toPx()
        val parallaxNebulaY = currentY * 8.dp.toPx()
        val parallaxStarsX = currentX * 16.dp.toPx()
        val parallaxStarsY = currentY * 16.dp.toPx()
        val parallaxAsteroidsX = currentX * 26.dp.toPx()
        val parallaxAsteroidsY = currentY * 26.dp.toPx()

        // ─── 2. Dải Tinh Vân Thiên Hà Chạy Chéo Trên (Upper Diagonal Violet Nebula) ─
        withTransform({
            translate(parallaxNebulaX + nebulaDrift, parallaxNebulaY + (nebulaDrift * 0.4f))
        }) {
            // Lớp 1: Khí ion tím thẫm lan tỏa nền rộng
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x604A044E), Color(0x303B0764), Color.Transparent),
                    center = Offset(w * 0.80f, h * 0.16f),
                    radius = w * 0.58f
                ),
                center = Offset(w * 0.80f, h * 0.16f),
                radius = w * 0.58f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x70581C87), Color(0x354A044E), Color.Transparent),
                    center = Offset(w * 0.55f, h * 0.28f),
                    radius = w * 0.52f
                ),
                center = Offset(w * 0.55f, h * 0.28f),
                radius = w * 0.52f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x756B21A8), Color(0x383B0764), Color.Transparent),
                    center = Offset(w * 0.36f, h * 0.43f),
                    radius = w * 0.55f
                ),
                center = Offset(w * 0.36f, h * 0.43f),
                radius = w * 0.55f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x55581C87), Color(0x203B0764), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.54f),
                    radius = w * 0.42f
                ),
                center = Offset(w * 0.15f, h * 0.54f),
                radius = w * 0.42f
            )

            // Lớp 2: Luồng mây phát quang Magenta & Ultraviolet rực rỡ
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x909333EA), Color(0x457C3AED), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.22f),
                    radius = w * 0.32f
                ),
                center = Offset(w * 0.70f, h * 0.22f),
                radius = w * 0.32f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xAAC026D3), Color(0x509333EA), Color.Transparent),
                    center = Offset(w * 0.48f, h * 0.35f),
                    radius = w * 0.30f
                ),
                center = Offset(w * 0.48f, h * 0.35f),
                radius = w * 0.30f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xBBDA46EF), Color(0x60C026D3), Color.Transparent),
                    center = Offset(w * 0.362f, h * 0.433f),
                    radius = w * 0.32f
                ),
                center = Offset(w * 0.362f, h * 0.433f),
                radius = w * 0.32f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x88A855F7), Color(0x356B21A8), Color.Transparent),
                    center = Offset(w * 0.24f, h * 0.49f),
                    radius = w * 0.24f
                ),
                center = Offset(w * 0.24f, h * 0.49f),
                radius = w * 0.24f
            )

            // Lớp 3: Xoáy sợi bụi khí plasma hồng-trắng chói lòa tại trung tâm tinh vân
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xDDE879F9), Color(0x70C026D3), Color.Transparent),
                    center = Offset(w * 0.362f, h * 0.433f),
                    radius = w * 0.16f
                ),
                center = Offset(w * 0.362f, h * 0.433f),
                radius = w * 0.16f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xEEFDF4FF), Color(0x99F472B6), Color.Transparent),
                    center = Offset(w * 0.362f, h * 0.433f),
                    radius = w * 0.08f
                ),
                center = Offset(w * 0.362f, h * 0.433f),
                radius = w * 0.08f
            )
        }

        // ─── 3. Dải Tinh Vân Cung Cực Lam-Tím Dưới Đáy (Lower Sweeping Cyan Nebula) ──
        withTransform({
            translate(parallaxNebulaX - nebulaDrift * 0.7f, parallaxNebulaY - nebulaDrift * 0.3f)
        }) {
            // Nền mây lam đậm & chàm tím uốn quanh góc dưới
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x654C1D95), Color(0x250891B2), Color.Transparent),
                    center = Offset(w * 0.68f, h * 0.77f),
                    radius = w * 0.48f
                ),
                center = Offset(w * 0.68f, h * 0.77f),
                radius = w * 0.48f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x750E7490), Color(0x356D28D9), Color.Transparent),
                    center = Offset(w * 0.38f, h * 0.86f),
                    radius = w * 0.48f
                ),
                center = Offset(w * 0.38f, h * 0.86f),
                radius = w * 0.48f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x554338CA), Color(0x2006B6D4), Color.Transparent),
                    center = Offset(w * 0.12f, h * 0.95f),
                    radius = w * 0.42f
                ),
                center = Offset(w * 0.12f, h * 0.95f),
                radius = w * 0.42f
            )

            // Dải lam ngọc & cyan điện quang rực rỡ
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x8506B6D4), Color(0x350284C7), Color.Transparent),
                    center = Offset(w * 0.52f, h * 0.81f),
                    radius = w * 0.28f
                ),
                center = Offset(w * 0.52f, h * 0.81f),
                radius = w * 0.28f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xAA38BDF8), Color(0x500891B2), Color.Transparent),
                    center = Offset(w * 0.375f, h * 0.865f),
                    radius = w * 0.26f
                ),
                center = Offset(w * 0.375f, h * 0.865f),
                radius = w * 0.26f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x7506B6D4), Color(0x300369A1), Color.Transparent),
                    center = Offset(w * 0.22f, h * 0.93f),
                    radius = w * 0.22f
                ),
                center = Offset(w * 0.22f, h * 0.93f),
                radius = w * 0.22f
            )

            // Lõi ươm sao lam-trắng rực rỡ (Stellar Nursery Core)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xCC67E8F9), Color(0x6006B6D4), Color.Transparent),
                    center = Offset(w * 0.375f, h * 0.865f),
                    radius = w * 0.14f
                ),
                center = Offset(w * 0.375f, h * 0.865f),
                radius = w * 0.14f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0x9967E8F9), Color.Transparent),
                    center = Offset(w * 0.375f, h * 0.865f),
                    radius = w * 0.05f
                ),
                center = Offset(w * 0.375f, h * 0.865f),
                radius = w * 0.05f
            )
        }

        // ─── 4. Ngân Hà Ngàn Vì Sao (Dense Starfield Micro Stars) ────────────────
        withTransform({
            translate(parallaxStarsX, parallaxStarsY)
        }) {
            backgroundStars.forEach { star ->
                val sx = star.xFrac * w
                val sy = star.yFrac * h
                val twinkleMod = 0.85f + 0.30f * sin((starTwinkle * star.twinkleSpeed) + star.phaseOffset)
                val alpha = (star.baseAlpha * twinkleMod).coerceIn(0.15f, 1.0f)
                val r = star.sizeDp.dp.toPx()

                if (star.sizeDp >= 2.0f) {
                    // Quầng sáng mềm cho sao lớn
                    drawCircle(
                        color = star.color.copy(alpha = alpha * 0.40f),
                        radius = r * 2.5f,
                        center = Offset(sx, sy)
                    )
                }
                drawCircle(
                    color = star.color.copy(alpha = alpha),
                    radius = r,
                    center = Offset(sx, sy)
                )
            }
        }

        // ─── 5. Các Ngôi Sao 4 Cánh Phát Quang (Featured Diffraction Spike Stars) ──
        withTransform({
            translate(parallaxStarsX, parallaxStarsY)
        }) {
            // 1. Sao Siêu Lóa Trung Tâm Tinh Vân (Super-Flare Center Star): (0.362, 0.433)
            drawCosmicDiffractionStar(
                cx = w * 0.362f,
                cy = h * 0.433f,
                spikeH = 58.dp.toPx(),
                spikeV = 72.dp.toPx(),
                coreRadius = 3.4.dp.toPx(),
                haloRadius = 28.dp.toPx(),
                haloColor = Color(0xFFF472B6),
                pulse = starTwinkle
            )

            // 2. Sao Lõi Lam Dưới Đáy (Lower Cyan Stellar Core): (0.375, 0.865)
            drawCosmicDiffractionStar(
                cx = w * 0.375f,
                cy = h * 0.865f,
                spikeH = 46.dp.toPx(),
                spikeV = 56.dp.toPx(),
                coreRadius = 3.0.dp.toPx(),
                haloRadius = 24.dp.toPx(),
                haloColor = Color(0xFF22D3EE),
                pulse = starTwinkle * 1.05f
            )

            // 3. Sao Xanh Lam Khổng Lồ Góc Trên-Trái (Top-Left Blue Giant): (0.117, 0.175)
            drawCosmicDiffractionStar(
                cx = w * 0.117f,
                cy = h * 0.175f,
                spikeH = 38.dp.toPx(),
                spikeV = 48.dp.toPx(),
                coreRadius = 2.6.dp.toPx(),
                haloRadius = 20.dp.toPx(),
                haloColor = Color(0xFF38BDF8),
                pulse = starTwinkle * 0.95f
            )

            // 4. Sao Lam Rực Rỡ Giữa-Phải (Mid-Right Azure Star): (0.728, 0.440)
            drawCosmicDiffractionStar(
                cx = w * 0.728f,
                cy = h * 0.440f,
                spikeH = 32.dp.toPx(),
                spikeV = 38.dp.toPx(),
                coreRadius = 2.4.dp.toPx(),
                haloRadius = 18.dp.toPx(),
                haloColor = Color(0xFF60A5FA),
                pulse = starTwinkle
            )

            // 5. Sao Tím Hồng Dưới-Phải (Lower-Right Violet Star): (0.915, 0.455)
            drawCosmicDiffractionStar(
                cx = w * 0.915f,
                cy = h * 0.455f,
                spikeH = 26.dp.toPx(),
                spikeV = 32.dp.toPx(),
                coreRadius = 2.0.dp.toPx(),
                haloRadius = 15.dp.toPx(),
                haloColor = Color(0xFFE879F9),
                pulse = starTwinkle * 1.08f
            )

            // 6. Sao Phụ Dưới-Phải (Lower-Right Second Star): (0.920, 0.525)
            drawCosmicDiffractionStar(
                cx = w * 0.920f,
                cy = h * 0.525f,
                spikeH = 20.dp.toPx(),
                spikeV = 24.dp.toPx(),
                coreRadius = 1.8.dp.toPx(),
                haloRadius = 12.dp.toPx(),
                haloColor = Color(0xFFA78BFA),
                pulse = starTwinkle * 0.92f
            )

            // 7. Sao Lam Đỉnh Đầu (Top-Center Blue Star): (0.550, 0.082)
            drawCosmicDiffractionStar(
                cx = w * 0.550f,
                cy = h * 0.082f,
                spikeH = 22.dp.toPx(),
                spikeV = 28.dp.toPx(),
                coreRadius = 1.8.dp.toPx(),
                haloRadius = 14.dp.toPx(),
                haloColor = Color(0xFF93C5FD),
                pulse = starTwinkle * 1.02f
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
                sizeDp = 46.dp.toPx(),
                rotationDeg = -15f + (asteroidTumble * 0.15f),
                seed = 101L
            )

            // 2. Tiểu hành tinh nhỏ đồng hành bên cạnh: (0.925, 0.720)
            draw3DRockyAsteroid(
                cx = w * 0.925f,
                cy = h * 0.720f,
                sizeDp = 18.dp.toPx(),
                rotationDeg = 35f - (asteroidTumble * 0.20f),
                seed = 102L
            )

            // 3. Tiểu hành tinh vừa ở giữa-dưới: (0.590, 0.660)
            draw3DRockyAsteroid(
                cx = w * 0.590f,
                cy = h * 0.660f,
                sizeDp = 22.dp.toPx(),
                rotationDeg = 45f + (asteroidTumble * 0.18f),
                seed = 103L
            )

            // 4. Tiểu hành tinh chuỗi bên trái: (0.122, 0.656)
            draw3DRockyAsteroid(
                cx = w * 0.122f,
                cy = h * 0.656f,
                sizeDp = 17.dp.toPx(),
                rotationDeg = -40f + (asteroidTumble * 0.25f),
                seed = 104L
            )

            // 5. Tiểu hành tinh vừa bên trái-dưới: (0.051, 0.727)
            draw3DRockyAsteroid(
                cx = w * 0.051f,
                cy = h * 0.727f,
                sizeDp = 24.dp.toPx(),
                rotationDeg = 20f - (asteroidTumble * 0.12f),
                seed = 105L
            )

            // 6. Tiểu hành tinh nhỏ bên trái-trên: (0.044, 0.582)
            draw3DRockyAsteroid(
                cx = w * 0.044f,
                cy = h * 0.582f,
                sizeDp = 14.dp.toPx(),
                rotationDeg = 75f + (asteroidTumble * 0.30f),
                seed = 106L
            )
        }
    }
}

/**
 * Hàm vẽ Ngôi sao 4 cánh phát quang (Diffraction Spike Star) chuẩn thiên văn:
 * - Tia ngang và tia dọc phát quang xa
 * - Quầng sáng tròn mềm bao bọc
 * - Lõi sao trắng sáng rực rỡ ở tâm
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
    val actualPulse = pulse.coerceIn(0.75f, 1.30f)

    // 1. Quầng sáng nhật hoa nở rộng
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                haloColor.copy(alpha = 0.65f * actualPulse),
                haloColor.copy(alpha = 0.20f * actualPulse),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = haloRadius * actualPulse
        ),
        center = Offset(cx, cy),
        radius = haloRadius * actualPulse
    )

    // 2. Tia nhiễu xạ phát quang ngang (Horizontal Spike)
    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                haloColor.copy(alpha = 0.45f * actualPulse),
                Color.White,
                haloColor.copy(alpha = 0.45f * actualPulse),
                Color.Transparent
            ),
            startX = cx - spikeH * actualPulse,
            endX = cx + spikeH * actualPulse
        ),
        start = Offset(cx - spikeH * actualPulse, cy),
        end = Offset(cx + spikeH * actualPulse, cy),
        strokeWidth = 1.6.dp.toPx()
    )

    // 3. Tia nhiễu xạ phát quang dọc (Vertical Spike)
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                haloColor.copy(alpha = 0.45f * actualPulse),
                Color.White,
                haloColor.copy(alpha = 0.45f * actualPulse),
                Color.Transparent
            ),
            startY = cy - spikeV * actualPulse,
            endY = cy + spikeV * actualPulse
        ),
        start = Offset(cx, cy - spikeV * actualPulse),
        end = Offset(cx, cy + spikeV * actualPulse),
        strokeWidth = 1.6.dp.toPx()
    )

    // 4. Lõi sao trắng chói ở tâm
    drawCircle(
        color = Color.White,
        radius = coreRadius * actualPulse,
        center = Offset(cx, cy)
    )
}

/**
 * Hàm vẽ Tiểu Hành Tinh Đá 3D (3D Faceted Cratered Asteroid):
 * - Đường viền đa giác góc cạnh tự nhiên
 * - Chiếu sáng định hướng từ góc trên-trái (Key Light 10h) với mặt sáng đá tím-xám
 * - Đổ bóng không gian sâu ở nửa dưới-phải (Space Umbra Shadow)
 * - Miệng hố va chạm (Impact Craters) với gờ nổi và lòng hố tối
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
        // Tạo hình khối đa giác đá gồ ghề từ 9 đỉnh
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

        // 1. Tô khối đa giác với gradient ánh sáng 3D từ góc trên-trái xuống dưới-phải
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFC4B5FD), // Mặt sáng đón ánh sao: tím hoa cà sáng
                    Color(0xFF7C6C93), // Mặt đá trung gian: xám tím khoáng thạch
                    Color(0xFF38294E), // Mặt chuyển tối
                    Color(0xFF140B22), // Bóng tối không gian sâu
                    Color(0xFF090412)  // Đáy bóng tối
                ),
                start = Offset(-r, -r),
                end = Offset(r, r)
            )
        )

        // 2. Viền phản quang sáng cạnh trên-trái (Specular Rim Highlight)
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
            color = Color(0xFFDDD6FE).copy(alpha = 0.70f),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3. Các hố va chạm thiên thạch (Impact Craters)
        // Hố va chạm 1 (Lớn)
        val c1X = -r * 0.22f
        val c1Y = -r * 0.15f
        val c1R = r * 0.28f
        drawCircle(
            color = Color(0xFF180F29),
            radius = c1R,
            center = Offset(c1X, c1Y)
        )
        drawArc(
            color = Color(0xFFE9D5FF).copy(alpha = 0.80f),
            startAngle = 135f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(c1X - c1R, c1Y - c1R),
            size = Size(c1R * 2f, c1R * 2f),
            style = Stroke(width = 1.1.dp.toPx())
        )

        // Hố va chạm 2 (Vừa)
        val c2X = r * 0.18f
        val c2Y = r * 0.22f
        val c2R = r * 0.22f
        drawCircle(
            color = Color(0xFF120820),
            radius = c2R,
            center = Offset(c2X, c2Y)
        )
        drawArc(
            color = Color(0xFFC4B5FD).copy(alpha = 0.65f),
            startAngle = 140f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(c2X - c2R, c2Y - c2R),
            size = Size(c2R * 2f, c2R * 2f),
            style = Stroke(width = 1.0.dp.toPx())
        )

        // Hố va chạm 3 (Nhỏ)
        val c3X = -r * 0.10f
        val c3Y = r * 0.36f
        val c3R = r * 0.16f
        drawCircle(
            color = Color(0xFF0F061C),
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
