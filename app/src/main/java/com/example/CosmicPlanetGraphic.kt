package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Phân cấp loại hành tinh trong lộ trình học tập từ bé đến lớn.
 * Mỗi hành tinh mang đặc trưng thiên văn học chân thực, sắc nét, nhìn phát nhận diện ngay lập tức!
 */
enum class CosmicPlanetType {
    MOON_GRAY,       // Cấp 1: Bé nhất (~80dp) - Mặt Trăng: Khoáng thạch bạc, miệng hố va chạm & tia phóng xạ Tycho
    MARS_CRIMSON,    // Cấp 2: Nhỏ vừa (~114dp) - Sao Hỏa: Cát đỏ rực rỡ, chỏm băng bắc cực trắng tuyết & đại hẻm núi Valles Marineris
    TERRA_GAIA,      // Cấp 3: Trung bình (~72dp) - Trái Đất: Đại dương xanh thẳm, mảng lục địa xanh, bão mây xoáy & đèn đêm
    JUPITER_STORM,   // Cấp 4: Lớn (~102dp) - Sao Mộc: Dải mây cuộn sóng đối lưu, Vết Đỏ Lớn (Great Red Spot) & chuỗi ngọc bão
    SOLAR_SUN,       // Cấp 6 / Level Cuối: To nhất (~128dp) - Mặt Trời: Ngôi sao mẹ rực lửa, lõi nhiệt hạch chói lọi, tai lửa nhật hoa & quầng bão từ khó nhất!
    SATURN_MONARCH,  // Cấp 3: Sao Thổ: Vành đai Cassini 3D đa tầng lộng lẫy & bão lục giác cực bắc (~90dp)
    NEPTUNE_STORM    // Tùy chọn mở rộng: Sao Hải Vương: Băng lam xanh thẳm & vết bão tối
}

object CosmicPlanetHelper {

    fun getPlanetTypeForStage(index: Int): CosmicPlanetType {
        return when (index % 6) {
            0 -> CosmicPlanetType.TERRA_GAIA      // Chặng 1: Trái Đất (Địa Cầu Xanh)
            1 -> CosmicPlanetType.MOON_GRAY       // Chặng 2: Mặt Trăng (Vệ Tinh Bạc)
            2 -> CosmicPlanetType.SATURN_MONARCH  // Chặng 3: Sao Thổ (Vành Đai Vàng Cassini 3D)
            3 -> CosmicPlanetType.JUPITER_STORM   // Chặng 4: Sao Mộc (Vết Đỏ Lớn)
            4 -> CosmicPlanetType.MARS_CRIMSON    // Chặng 5: Sao Hỏa (Hành Tinh Đỏ)
            else -> CosmicPlanetType.SOLAR_SUN    // Chặng 6 / Mốc Cuối: Mặt Trời (Đỉnh Cao Thái Dương)
        }
    }

    fun getPlanetDiameter(index: Int): Dp {
        return when (index % 6) {
            0 -> 72.dp   // Cấp 1: Bé nhất (Trái Đất)
            1 -> 80.dp   // Cấp 2: Nhỏ vừa (Mặt Trăng)
            2 -> 90.dp   // Cấp 3: Vừa + vành đai Cassini (Sao Thổ)
            3 -> 102.dp  // Cấp 4: Lớn (Sao Mộc)
            4 -> 114.dp  // Cấp 5: Rất lớn (Sao Hỏa)
            else -> 128.dp // Cấp 6: Mốc cuối cực đại (Mặt Trời)
        }
    }

    fun getPlanetTypeName(type: CosmicPlanetType, isEnglish: Boolean = false): String {
        return when (type) {
            CosmicPlanetType.MOON_GRAY      -> if (isEnglish) "The Moon (Lunar Outpost)" else "Mặt Trăng (Vệ Tinh Bạc)"
            CosmicPlanetType.MARS_CRIMSON   -> if (isEnglish) "Mars (Crimson Dunes)" else "Sao Hỏa (Hành Tinh Đỏ)"
            CosmicPlanetType.TERRA_GAIA     -> if (isEnglish) "Earth (Terra Gaia)" else "Trái Đất (Địa Cầu Xanh)"
            CosmicPlanetType.JUPITER_STORM  -> if (isEnglish) "Jupiter (Great Red Spot)" else "Sao Mộc (Vết Đỏ Lớn)"
            CosmicPlanetType.SOLAR_SUN      -> if (isEnglish) "The Sun (Solar Apex)" else "Mặt Trời (Đỉnh Cao Thái Dương)"
            CosmicPlanetType.SATURN_MONARCH -> if (isEnglish) "Saturn (Cassini Monarch)" else "Sao Thổ (Vành Đai Vàng)"
            CosmicPlanetType.NEPTUNE_STORM  -> if (isEnglish) "Neptune (Glacial Storm)" else "Sao Hải Vương"
        }
    }

    fun getPlanetPrimaryColor(type: CosmicPlanetType): Color {
        return when (type) {
            CosmicPlanetType.MOON_GRAY      -> Color(0xFFCFD8DC) // Lunar Silver / Cool Grey
            CosmicPlanetType.MARS_CRIMSON   -> Color(0xFFFF5722) // Mars Flame Red/Orange
            CosmicPlanetType.TERRA_GAIA     -> Color(0xFF00B4D8) // Earth Cyan Blue
            CosmicPlanetType.JUPITER_STORM  -> Color(0xFFFF9E00) // Jupiter Warm Caramel Amber
            CosmicPlanetType.SOLAR_SUN      -> Color(0xFFFFB703) // Blazing Solar Gold / Amber Orange
            CosmicPlanetType.SATURN_MONARCH -> Color(0xFFFFD166) // Saturn Golden Ring
            CosmicPlanetType.NEPTUNE_STORM  -> Color(0xFF7B2CBF) // Deep Nebula Purple
        }
    }
}

/**
 * Thành phần hiển thị Hành Tinh Vũ Trụ Chân Thực Đỉnh Cao (Photorealistic Sci-Fi Planet),
 * với hệ thống chiếu sáng không gian 3D, chi tiết nhận diện thương hiệu vũ trụ (Sao Mộc Vết Đỏ Lớn,
 * Sao Thổ Vành Đai Cassini, Trái Đất Đại Dương Xanh, Sao Hỏa Chỏm Băng & Hẻm Núi, Sao Thủy Miệng Hố).
 */
@Composable
fun CosmicPlanetView(
    stage: JourneyStageUiModel,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val planetType = remember(index) { CosmicPlanetHelper.getPlanetTypeForStage(index) }
    val planetDiameter = remember(index) { CosmicPlanetHelper.getPlanetDiameter(index) }
    val isSaturn = planetType == CosmicPlanetType.SATURN_MONARCH
    val isSun = planetType == CosmicPlanetType.SOLAR_SUN

    // Kích thước container mở rộng cho vành đai Sao Thổ hoặc hào quang nhật hoa / tai lửa Mặt Trời
    val containerWidth = when {
        isSaturn -> planetDiameter + 68.dp
        isSun    -> planetDiameter + 48.dp
        else     -> planetDiameter + 28.dp
    }
    val containerHeight = when {
        isSun    -> planetDiameter + 48.dp
        isSaturn -> planetDiameter + 38.dp
        else     -> planetDiameter + 28.dp
    }

    val isCurrent = stage.state == JourneyStageState.CURRENT
    val isCompleted = stage.state == JourneyStageState.COMPLETED
    val isLocked = stage.state == JourneyStageState.LOCKED

    val infiniteTransition = rememberInfiniteTransition(label = "planet_anim")

    // Nhịp thở tinh vân cho hành tinh hiện tại
    val pulseScale by if (isCurrent) {
        infiniteTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse_scale"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    // Góc quay của mây/khí quyển bề mặt (Dynamic rotation)
    val cloudRotationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloud_rotation"
    )

    // Góc quay của vành đai sao / hạt tiểu hành tinh Sao Thổ (Saturn Ring Orbit Dynamics)
    val ringRotationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "saturn_ring_rotation"
    )

    // Góc quay vệ tinh thăm dò 3D (Orbiting probe)
    val satelliteAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "satellite_angle"
    )

    // Hào quang nhật hoa tỏa sáng mềm mại
    val auraGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "aura_glow"
    )

    // Sóng radar quét nhiệm vụ cho chặng CURRENT
    val beaconRadarRadius by if (isCurrent) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
            label = "radar_sweep"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val contentDesc = "${stage.sequence}. ${stage.title} - ${CosmicPlanetHelper.getPlanetTypeName(planetType)}"

    Box(
        modifier = modifier
            .size(width = containerWidth, height = containerHeight)
            .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
            .semantics { contentDescription = contentDesc }
            .pointerInput(stage.id) {
                detectTapGestures(onTap = { onClick() })
            },
        contentAlignment = Alignment.Center
    ) {
        // Hào quang không gian sâu (Deep Space Cosmic Nebula Glow)
        val glowColor = when {
            isCompleted -> Color(0xFFFFD166)
            isCurrent   -> CosmicPlanetHelper.getPlanetPrimaryColor(planetType)
            else        -> CosmicPlanetHelper.getPlanetPrimaryColor(planetType).copy(alpha = 0.45f)
        }
        Box(
            modifier = Modifier
                .size(planetDiameter + 20.dp)
                .blur(20.dp)
                .background(
                    glowColor.copy(alpha = if (isCurrent) auraGlow * 0.45f else if (isCompleted) 0.30f else 0.16f),
                    CircleShape
                )
        )

        // Bề mặt hành tinh được vẽ hoàn toàn bằng Canvas với các chi tiết nhận diện đặc thù
        Canvas(modifier = Modifier.size(width = containerWidth, height = containerHeight)) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = planetDiameter.toPx() / 2f

            // Vệ tinh quay 3D: Nửa sau lưng hành tinh (khi z < 0)
            if (isCurrent) {
                draw3DOrbitingProbe(
                    centerX = centerX,
                    centerY = centerY,
                    planetRadius = radius,
                    angleDeg = satelliteAngle,
                    isBackPass = true
                )
            }

            // Vẽ toàn bộ quả cầu hành tinh (Đảm bảo sắc nét, rõ đặc trưng kể cả khi LOCKED!)
            drawPhotorealisticPlanet(
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                type = planetType,
                cloudPhase = cloudRotationPhase,
                ringPhase = ringRotationPhase,
                isLocked = isLocked,
                isCompleted = isCompleted
            )

            // Vệ tinh quay 3D: Nửa trước mặt hành tinh (khi z >= 0)
            if (isCurrent) {
                draw3DOrbitingProbe(
                    centerX = centerX,
                    centerY = centerY,
                    planetRadius = radius,
                    angleDeg = satelliteAngle,
                    isBackPass = false
                )
            }

            // Màng chắn năng lượng trường lực vi mô nếu chặng đang bị KHÓA (LOCKED)
            // (Mỏng nhẹ, trong suốt, KHÔNG che lấp chi tiết hành tinh)
            if (isLocked) {
                drawSciFiStasisShield(
                    centerX = centerX,
                    centerY = centerY,
                    radius = radius
                )
            }
        }

        // Cờ tiêu lệnh phi hành gia trên đỉnh cực bắc chặng đang học (CURRENT)
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF00F5D4))
                    .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        tint = Color(0xFF06152B),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "EXPLORING",
                        color = Color(0xFF06152B),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }

        // Vương miện thiên hà sao vàng cho chặng đã hoàn thành (COMPLETED)
        if (isCompleted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-6).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFFD166), Color(0xFFFF9E00))))
                    .border(1.dp, Color(0xFFFFF275), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF160F2B),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "CONQUERED",
                        color = Color(0xFF160F2B),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }

        // ─── Holographic Mission Telemetry Badge ──────────────────────────────
        // Thao tác nhỏ gọn ở rìa ngoài hành tinh để TUYỆT ĐỐI KHÔNG CHE KHUẤT bề mặt hành tinh
        val badgeSize = (planetDiameter * 0.28f).coerceIn(22.dp, 30.dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-2).dp, y = (-2).dp)
                .size(badgeSize)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> Brush.radialGradient(listOf(Color(0xFF261801), Color(0xFF120A01)))
                        isCurrent   -> Brush.radialGradient(listOf(Color(0xFF003830), Color(0xFF011814)))
                        isLocked    -> Brush.radialGradient(listOf(Color(0xEE1A162B), Color(0xF2090714)))
                        else        -> Brush.radialGradient(listOf(Color(0xFF092342), Color(0xFF041021)))
                    }
                )
                .border(
                    width = 1.5.dp,
                    color = when {
                        isCompleted -> Color(0xFFFFD166)
                        isCurrent   -> Color(0xFF00F5D4)
                        isLocked    -> Color(0x88FFFFFF)
                        else        -> CosmicPlanetHelper.getPlanetPrimaryColor(planetType)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Hiệu ứng sóng radar phát tín hiệu cho chặng CURRENT
            if (isCurrent) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFF00F5D4).copy(alpha = (1f - beaconRadarRadius) * 0.6f),
                        radius = (size.minDimension / 2f) * (0.8f + beaconRadarRadius * 0.6f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            Icon(
                imageVector = when {
                    isCompleted -> Icons.Default.Check
                    isCurrent   -> Icons.Default.PlayArrow
                    isLocked    -> Icons.Default.Lock
                    else        -> Icons.AutoMirrored.Filled.ArrowForward
                },
                contentDescription = null,
                tint = when {
                    isCompleted -> Color(0xFFFFD166)
                    isCurrent   -> Color(0xFF00F5D4)
                    isLocked    -> Color(0xCCFFFFFF)
                    else        -> Color.White
                },
                modifier = Modifier.size(badgeSize * 0.55f)
            )
        }
    }
}

/**
 * Hàm vẽ hình cầu hành tinh với hệ thống chiếu sáng không gian chuẩn xác:
 * - Key Light (Nguồn sáng góc 10h)
 * - Terminator Shade (Bóng tối tự nhiên của khối cầu xoay hướng xa mặt trời)
 * - Rayleigh Scattering Rim Glow (Tán xạ ánh sáng qua viền khí quyển)
 * - Chi tiết bề mặt riêng biệt cho từng loại hành tinh
 */
private fun DrawScope.drawPhotorealisticPlanet(
    centerX: Float,
    centerY: Float,
    radius: Float,
    type: CosmicPlanetType,
    cloudPhase: Float,
    ringPhase: Float = 0f,
    isLocked: Boolean,
    isCompleted: Boolean
) {
    // Giữ độ sáng và tương phản sắc nét kể cả khi bị khóa để người dùng luôn nhận ra hành tinh
    val alphaFactor = if (isLocked) 0.82f else 1.0f

    // 0. Nếu là MẶT TRỜI (THE SUN / SOLAR APEX): Ngôi sao mẹ thái dương hệ tự phát quang, không chịu bóng tối terminator của hành tinh
    if (type == CosmicPlanetType.SOLAR_SUN) {
        drawTheSunSolarApex(
            centerX = centerX,
            centerY = centerY,
            radius = radius,
            cloudPhase = cloudPhase,
            alphaFactor = alphaFactor,
            isLocked = isLocked
        )
        return
    }

    // 1. Nếu là Sao Thổ (SATURN), vẽ NỬA SAU của vành đai và bóng đổ hành tinh lên vành đai
    if (type == CosmicPlanetType.SATURN_MONARCH) {
        drawRealisticSaturnRings(
            centerX = centerX,
            centerY = centerY,
            radius = radius,
            isFront = false,
            alpha = alphaFactor,
            ringPhase = ringPhase
        )
    }

    // 2. Tọa độ tâm nguồn sáng Key Light (góc 10 giờ)
    val lightSourceX = centerX - radius * 0.42f
    val lightSourceY = centerY - radius * 0.42f

    // 3. Khối cầu nền hành tinh với quang phổ màu chiều sâu đặc trưng
    val baseGradients = when (type) {
        CosmicPlanetType.SOLAR_SUN -> listOf(
            Color(0xFFFFFFFF),
            Color(0xFFFFF9C4),
            Color(0xFFFFD54F),
            Color(0xFFFF9800),
            Color(0xFFF4511E),
            Color(0xFFB71C1C)
        )
        CosmicPlanetType.MOON_GRAY -> listOf(
            Color(0xFFF5F5F5),
            Color(0xFFCFD8DC),
            Color(0xFF90A4AE),
            Color(0xFF546E7A),
            Color(0xFF263238),
            Color(0xFF10171A)
        )
        CosmicPlanetType.MARS_CRIMSON -> listOf(
            Color(0xFFFFCCBC),
            Color(0xFFFF8A65),
            Color(0xFFF4511E),
            Color(0xFFD84315),
            Color(0xFF7A1C04),
            Color(0xFF2E0901)
        )
        CosmicPlanetType.TERRA_GAIA -> listOf(
            Color(0xFFE0F7FA),
            Color(0xFF00B4D8),
            Color(0xFF0077B6),
            Color(0xFF023E8A),
            Color(0xFF031A47),
            Color(0xFF010B22)
        )
        CosmicPlanetType.JUPITER_STORM -> listOf(
            Color(0xFFFFF8E7),
            Color(0xFFFFE0B2),
            Color(0xFFFFB74D),
            Color(0xFFE65100),
            Color(0xFF8D3405),
            Color(0xFF3E1200)
        )
        CosmicPlanetType.SATURN_MONARCH -> listOf(
            Color(0xFFFFFDE7),
            Color(0xFFFFF176),
            Color(0xFFFFD54F),
            Color(0xFFFFB300),
            Color(0xFF996500),
            Color(0xFF382000)
        )
        CosmicPlanetType.NEPTUNE_STORM -> listOf(
            Color(0xFFE1F5FE),
            Color(0xFF4FC3F7),
            Color(0xFF0288D1),
            Color(0xFF01579B),
            Color(0xFF0D254C),
            Color(0xFF040E1E)
        )
    }

    val sphereColors = if (isLocked) baseGradients.map { it.copy(alpha = it.alpha * 0.90f) } else baseGradients

    // Vẽ hình cầu nền
    drawCircle(
        brush = Brush.radialGradient(
            colors = sphereColors,
            center = Offset(lightSourceX, lightSourceY),
            radius = radius * 1.45f
        ),
        center = Offset(centerX, centerY),
        radius = radius
    )

    // 4. Clip bên trong quả cầu để vẽ toàn bộ chi tiết địa chất & khí quyển
    val spherePath = Path().apply {
        addOval(Rect(centerX - radius, centerY - radius, centerX + radius, centerY + radius))
    }

    clipPath(spherePath) {
        when (type) {
            CosmicPlanetType.MOON_GRAY -> {
                drawMoonHighlandsAndCraters(centerX, centerY, radius, alphaFactor)
            }
            CosmicPlanetType.MARS_CRIMSON -> {
                drawMarsCanyonsAndVolcano(centerX, centerY, radius, alphaFactor)
            }
            CosmicPlanetType.TERRA_GAIA -> {
                drawTerraGaiaBiosphere(centerX, centerY, radius, cloudPhase, alphaFactor)
            }
            CosmicPlanetType.JUPITER_STORM -> {
                drawJupiterTurbulentBeltsAndRedSpot(centerX, centerY, radius, alphaFactor)
            }
            CosmicPlanetType.SATURN_MONARCH -> {
                drawSaturnBeltsAndHexagon(centerX, centerY, radius, alphaFactor)
            }
            CosmicPlanetType.NEPTUNE_STORM -> {
                drawNeptuneStormBands(centerX, centerY, radius, alphaFactor)
            }
            CosmicPlanetType.SOLAR_SUN -> {
                // Đã được vẽ riêng bởi drawTheSunSolarApex
            }
        }

        // 5. Lớp bóng tối Terminator Shadow (Đường ranh giới ngày-đêm cong 3D tự nhiên)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    Color(0x22020108),
                    Color(0x77020108),
                    Color(0xDD020108)
                ),
                center = Offset(lightSourceX, lightSourceY),
                radius = radius * 1.55f
            ),
            center = Offset(centerX, centerY),
            radius = radius
        )

        // 6. Ánh sáng phản quang thứ cấp (Ambient Space Bounce Light ở viền tối)
        val bounceColor = when (type) {
            CosmicPlanetType.MOON_GRAY      -> Color(0x33B0BEC5)
            CosmicPlanetType.MARS_CRIMSON   -> Color(0x44FF5722)
            CosmicPlanetType.TERRA_GAIA     -> Color(0x4400B4D8)
            CosmicPlanetType.JUPITER_STORM  -> Color(0x44FFB74D)
            CosmicPlanetType.SOLAR_SUN      -> Color(0x44FFB703)
            CosmicPlanetType.SATURN_MONARCH -> Color(0x44FFE082)
            CosmicPlanetType.NEPTUNE_STORM  -> Color(0x4429B6F6)
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Transparent, bounceColor.copy(alpha = bounceColor.alpha * alphaFactor)),
                center = Offset(centerX + radius * 0.7f, centerY + radius * 0.7f),
                radius = radius * 0.8f
            ),
            center = Offset(centerX, centerY),
            radius = radius
        )
    }

    // 7. Tán xạ khí quyển Rayleigh Scattering (Atmospheric Fresnel Rim Glow)
    val atmosphereColor = when (type) {
        CosmicPlanetType.MOON_GRAY      -> Color(0xFFCFD8DC)
        CosmicPlanetType.MARS_CRIMSON   -> Color(0xFFFFAB91)
        CosmicPlanetType.TERRA_GAIA     -> Color(0xFF48CAE4)
        CosmicPlanetType.JUPITER_STORM  -> Color(0xFFFFCC80)
        CosmicPlanetType.SOLAR_SUN      -> Color(0xFFFFE082)
        CosmicPlanetType.SATURN_MONARCH -> Color(0xFFFFE082)
        CosmicPlanetType.NEPTUNE_STORM  -> Color(0xFF81D4FA)
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                atmosphereColor.copy(alpha = 0.25f * alphaFactor),
                atmosphereColor.copy(alpha = 0.75f * alphaFactor)
            ),
            center = Offset(centerX, centerY),
            radius = radius
        ),
        center = Offset(centerX, centerY),
        radius = radius
    )

    // Đốm sáng phản chiếu cực điểm (Specular Hotspot) ở góc đón nắng
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.60f * alphaFactor),
                Color.White.copy(alpha = 0.20f * alphaFactor),
                Color.Transparent
            ),
            center = Offset(lightSourceX, lightSourceY),
            radius = radius * 0.45f
        ),
        center = Offset(lightSourceX, lightSourceY),
        radius = radius * 0.45f
    )

    // 8. Nếu là Sao Thổ (SATURN), vẽ NỬA TRƯỚC của vành đai đè lên thân quả cầu
    if (type == CosmicPlanetType.SATURN_MONARCH) {
        drawRealisticSaturnRings(
            centerX = centerX,
            centerY = centerY,
            radius = radius,
            isFront = true,
            alpha = alphaFactor,
            ringPhase = ringPhase
        )
    }
}

// ─── Chi Tiết Hành Tinh 2: MẶT TRĂNG (MOON_GRAY) ─────────────────────────────

private fun DrawScope.drawMoonHighlandsAndCraters(
    centerX: Float,
    centerY: Float,
    radius: Float,
    alpha: Float
) {
    // 1. Biển dung nham basalt nguội sẫm màu (Lunar Maria - Biển Mặt Trăng sẫm màu đặc trưng)
    val mareColor = Color(0x77212B30).copy(alpha = 0.70f * alpha)

    // Mare Imbrium (Biển Mưa lớn ở tây bắc)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(mareColor, mareColor.copy(alpha = 0.35f * alpha), Color.Transparent),
            center = Offset(centerX - radius * 0.28f, centerY - radius * 0.28f),
            radius = radius * 0.38f
        ),
        topLeft = Offset(centerX - radius * 0.58f, centerY - radius * 0.52f),
        size = Size(radius * 0.60f, radius * 0.48f)
    )

    // Oceanus Procellarum (Đại dương Bão Tố trải dài phía tây)
    val oceanusPath = Path().apply {
        moveTo(centerX - radius * 0.75f, centerY - radius * 0.15f)
        cubicTo(
            centerX - radius * 0.45f, centerY - radius * 0.58f,
            centerX - radius * 0.15f, centerY - radius * 0.40f,
            centerX - radius * 0.18f, centerY + radius * 0.10f
        )
        cubicTo(
            centerX - radius * 0.35f, centerY + radius * 0.32f,
            centerX - radius * 0.85f, centerY + radius * 0.15f,
            centerX - radius * 0.75f, centerY - radius * 0.15f
        )
        close()
    }
    drawPath(oceanusPath, color = mareColor)

    // Mare Serenitatis & Mare Tranquillitatis (Biển Thanh Bình & Biển Tĩnh Lặng phía đông)
    val serenitatisPath = Path().apply {
        moveTo(centerX + radius * 0.12f, centerY - radius * 0.25f)
        cubicTo(
            centerX + radius * 0.48f, centerY - radius * 0.35f,
            centerX + radius * 0.68f, centerY - radius * 0.05f,
            centerX + radius * 0.42f, centerY + radius * 0.28f
        )
        cubicTo(
            centerX + radius * 0.15f, centerY + radius * 0.35f,
            centerX + radius * 0.05f, centerY - radius * 0.05f,
            centerX + radius * 0.12f, centerY - radius * 0.25f
        )
        close()
    }
    drawPath(serenitatisPath, color = mareColor)

    // 2. Miệng hố va chạm chính Tycho Crater (ở cực nam) với các tia văng thiên thạch trắng sáng rực rỡ
    val c1x = centerX - radius * 0.22f
    val c1y = centerY + radius * 0.38f
    val rayColor = Color(0x77FFFFFF).copy(alpha = 0.72f * alpha)
    for (i in 0 until 14) {
        val rayAngle = i * (2f * PI.toFloat() / 14f)
        val endRx = c1x + cos(rayAngle) * radius * 0.95f
        val endRy = c1y + sin(rayAngle) * radius * 0.95f
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(rayColor, rayColor.copy(alpha = 0.15f * alpha), Color.Transparent),
                start = Offset(c1x, c1y),
                end = Offset(endRx, endRy)
            ),
            start = Offset(c1x, c1y),
            end = Offset(endRx, endRy),
            strokeWidth = 1.3.dp.toPx()
        )
    }

    // 3. Hệ thống tia phóng xạ Copernicus Crater ở phía bắc
    val copX = centerX - radius * 0.20f
    val copY = centerY - radius * 0.10f
    for (i in 0 until 8) {
        val rayAngle = (i * 45f + 15f) * (PI.toFloat() / 180f)
        val endRx = copX + cos(rayAngle) * radius * 0.45f
        val endRy = copY + sin(rayAngle) * radius * 0.45f
        drawLine(
            color = Color(0x44FFFFFF).copy(alpha = 0.50f * alpha),
            start = Offset(copX, copY),
            end = Offset(endRx, endRy),
            strokeWidth = 1.0.dp.toPx()
        )
    }

    // 4. Các miệng hố va chạm 3D sắc nét nổi khối trên bề mặt
    drawRealistic3DCrater(c1x, c1y, radius * 0.22f, alpha) // Tycho
    drawRealistic3DCrater(copX, copY, radius * 0.16f, alpha) // Copernicus
    drawRealistic3DCrater(centerX + radius * 0.28f, centerY - radius * 0.26f, radius * 0.18f, alpha) // Posidonius
    drawRealistic3DCrater(centerX + radius * 0.25f, centerY + radius * 0.42f, radius * 0.14f, alpha) // Theophilus
    drawRealistic3DCrater(centerX - radius * 0.48f, centerY - radius * 0.32f, radius * 0.12f, alpha) // Aristarchus
    drawRealistic3DCrater(centerX + radius * 0.48f, centerY + radius * 0.05f, radius * 0.10f, alpha) // Langrenus
    drawRealistic3DCrater(centerX - radius * 0.05f, centerY + radius * 0.12f, radius * 0.09f, alpha) // Ptolemaeus
    drawRealistic3DCrater(centerX - radius * 0.36f, centerY + radius * 0.18f, radius * 0.08f, alpha) // Kepler
}

private fun DrawScope.drawRealistic3DCrater(
    x: Float,
    y: Float,
    craterRadius: Float,
    alpha: Float
) {
    // Đáy hố sâu màu tối
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xDD1A2327), Color(0xFF0F1517)),
            center = Offset(x + craterRadius * 0.30f, y + craterRadius * 0.30f),
            radius = craterRadius
        ),
        center = Offset(x, y),
        radius = craterRadius,
        alpha = alpha
    )
    // Đỉnh chóp trung tâm miệng hố (Central Mountain Peak)
    drawCircle(
        color = Color(0xEEFFFFFF),
        center = Offset(x - craterRadius * 0.08f, y - craterRadius * 0.08f),
        radius = craterRadius * 0.22f,
        alpha = alpha
    )
    // Vành hố sáng đón ánh sáng từ góc 10h
    drawArc(
        color = Color(0xFFFFFFFF),
        startAngle = 135f,
        sweepAngle = 170f,
        useCenter = false,
        topLeft = Offset(x - craterRadius, y - craterRadius),
        size = Size(craterRadius * 2f, craterRadius * 2f),
        style = Stroke(width = 1.6.dp.toPx()),
        alpha = alpha
    )
    // Vành hố tối ở phía khuất sáng
    drawArc(
        color = Color(0xBB000000),
        startAngle = -45f,
        sweepAngle = 170f,
        useCenter = false,
        topLeft = Offset(x - craterRadius, y - craterRadius),
        size = Size(craterRadius * 2f, craterRadius * 2f),
        style = Stroke(width = 1.6.dp.toPx()),
        alpha = alpha
    )
}

// ─── Chi Tiết Hành Tinh 2: SAO HỎA (MARS_CRIMSON) ───────────────────────────

private fun DrawScope.drawMarsCanyonsAndVolcano(
    centerX: Float,
    centerY: Float,
    radius: Float,
    alpha: Float
) {
    // Vùng cao nguyên dung nham sẫm màu Syrtis Major (Mảng tối đặc trưng của Sao Hỏa)
    val syrtisMajorColor = Color(0x774A1204).copy(alpha = 0.8f * alpha)
    val syrtisPath = Path().apply {
        moveTo(centerX + radius * 0.1f, centerY - radius * 0.45f)
        cubicTo(
            centerX + radius * 0.45f, centerY - radius * 0.25f,
            centerX + radius * 0.55f, centerY + radius * 0.15f,
            centerX + radius * 0.20f, centerY + radius * 0.35f
        )
        cubicTo(
            centerX + radius * 0.05f, centerY + radius * 0.15f,
            centerX, centerY - radius * 0.2f,
            centerX + radius * 0.1f, centerY - radius * 0.45f
        )
        close()
    }
    drawPath(syrtisPath, color = syrtisMajorColor)

    // Chỏm băng Bắc Cực (North Polar Ice Cap) trắng tuyết tinh khôi có rãnh xoắn
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, Color(0xFFFFFFFF), Color(0x00FFFFFF)),
            center = Offset(centerX, centerY - radius * 0.90f),
            radius = radius * 0.45f
        ),
        topLeft = Offset(centerX - radius * 0.38f, centerY - radius * 0.98f),
        size = Size(radius * 0.76f, radius * 0.28f),
        alpha = alpha
    )

    // Siêu núi lửa lớn nhất hệ mặt trời Olympus Mons
    val volcanoX = centerX - radius * 0.32f
    val volcanoY = centerY - radius * 0.25f
    val calderaRadius = radius * 0.20f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x995C1705), Color(0x66FF8A65), Color.Transparent),
            center = Offset(volcanoX, volcanoY),
            radius = calderaRadius * 1.6f
        ),
        center = Offset(volcanoX, volcanoY),
        radius = calderaRadius * 1.6f,
        alpha = alpha
    )
    drawCircle(
        color = Color(0xDD2E0901),
        center = Offset(volcanoX, volcanoY),
        radius = calderaRadius * 0.45f,
        alpha = alpha
    )

    // Đại hẻm núi Valles Marineris xé ngang xích đạo Sao Hỏa
    val canyonMain = Path().apply {
        moveTo(centerX - radius * 0.70f, centerY + radius * 0.08f)
        cubicTo(
            centerX - radius * 0.25f, centerY + radius * 0.28f,
            centerX + radius * 0.15f, centerY + radius * 0.08f,
            centerX + radius * 0.68f, centerY + radius * 0.35f
        )
    }
    // Lòng hẻm vực sâu thăm thẳm
    drawPath(
        canyonMain,
        color = Color(0xFF260600),
        style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha
    )
    // Vách đá hẻm núi rực sáng màu cam hổ phách
    drawPath(
        canyonMain,
        color = Color(0xFFFFB74D),
        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha
    )

    // Nhánh đứt gãy hẻm núi phụ
    val canyonBranch = Path().apply {
        moveTo(centerX - radius * 0.15f, centerY + radius * 0.24f)
        quadraticTo(centerX + radius * 0.12f, centerY + radius * 0.40f, centerX + radius * 0.45f, centerY + radius * 0.50f)
    }
    drawPath(
        canyonBranch,
        color = Color(0xFF260600),
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha
    )
}

// ─── Chi Tiết Hành Tinh 3: TRÁI ĐẤT (TERRA_GAIA) ────────────────────────────

private fun DrawScope.drawTerraGaiaBiosphere(
    centerX: Float,
    centerY: Float,
    radius: Float,
    cloudPhase: Float,
    alpha: Float
) {
    // 1. Thềm lục địa nông viền màu ngọc lam rực rỡ (Turquoise shallow ocean shelves)
    val shelfColor = Color(0x8848CAE4).copy(alpha = 0.88f * alpha)
    val landColor = Color(0xFF2D6A4F).copy(alpha = 0.95f * alpha)
    val desertColor = Color(0xFFDDA15E).copy(alpha = 0.88f * alpha)

    // Lục địa Bắc (Hình khối Bắc Mỹ / Âu-Á rõ rệt)
    val northLand = Path().apply {
        moveTo(centerX - radius * 0.60f, centerY - radius * 0.28f)
        cubicTo(
            centerX - radius * 0.35f, centerY - radius * 0.70f,
            centerX + radius * 0.15f, centerY - radius * 0.60f,
            centerX + radius * 0.48f, centerY - radius * 0.18f
        )
        cubicTo(
            centerX + radius * 0.25f, centerY + radius * 0.05f,
            centerX - radius * 0.15f, centerY - radius * 0.05f,
            centerX - radius * 0.60f, centerY - radius * 0.28f
        )
        close()
    }
    // Viền thềm san hô ngọc lam
    drawPath(northLand, color = shelfColor, style = Stroke(width = 4.dp.toPx()))
    // Mảng lục địa xanh rừng rậm
    drawPath(northLand, color = landColor)

    // Sa mạc Sahara màu cát vàng trên lục địa bắc
    drawCircle(
        color = desertColor,
        center = Offset(centerX + radius * 0.08f, centerY - radius * 0.32f),
        radius = radius * 0.18f
    )

    // Lục địa Nam (Hình khối Nam Mỹ / Châu Phi rõ rệt)
    val southLand = Path().apply {
        moveTo(centerX - radius * 0.28f, centerY + radius * 0.12f)
        cubicTo(
            centerX + radius * 0.12f, centerY + radius * 0.05f,
            centerX + radius * 0.38f, centerY + radius * 0.22f,
            centerX + radius * 0.22f, centerY + radius * 0.68f
        )
        cubicTo(
            centerX - radius * 0.05f, centerY + radius * 0.72f,
            centerX - radius * 0.28f, centerY + radius * 0.42f,
            centerX - radius * 0.28f, centerY + radius * 0.12f
        )
        close()
    }
    drawPath(southLand, color = shelfColor, style = Stroke(width = 3.5.dp.toPx()))
    drawPath(southLand, color = landColor)

    // 2. Ánh đèn thành phố rực sáng về đêm (Night-side City Lights)
    val cityLightColor = Color(0xFFFFD54F).copy(alpha = 0.95f * alpha)
    val cityPoints = listOf(
        Offset(centerX + radius * 0.32f, centerY + radius * 0.15f),
        Offset(centerX + radius * 0.44f, centerY + radius * 0.25f),
        Offset(centerX + radius * 0.36f, centerY + radius * 0.42f),
        Offset(centerX + radius * 0.26f, centerY + radius * 0.50f),
        Offset(centerX + radius * 0.50f, centerY - radius * 0.05f)
    )
    cityPoints.forEach { pt ->
        drawCircle(color = cityLightColor, center = pt, radius = 1.6.dp.toPx())
        drawCircle(color = Color(0x66FFA000), center = pt, radius = 4.dp.toPx())
    }

    // 3. Mây bão khí quyển xoắn ốc tuyệt đẹp (Atmospheric Cyclone with Eye)
    val cloudOffsetRad = Math.toRadians(cloudPhase.toDouble() * 0.25)
    val cloudShiftX = (cos(cloudOffsetRad) * radius * 0.08f).toFloat()

    val cycloneCenter = Offset(centerX - radius * 0.15f + cloudShiftX, centerY - radius * 0.18f)

    // Bóng đổ của mây lên đại dương
    drawCycloneClouds(cycloneCenter.x + 3.5.dp.toPx(), cycloneCenter.y + 3.5.dp.toPx(), radius, Color(0x44000000))
    // Đám mây trắng xóa tinh khôi
    drawCycloneClouds(cycloneCenter.x, cycloneCenter.y, radius, Color(0xEEFFFFFF))
}

private fun DrawScope.drawCycloneClouds(cx: Float, cy: Float, radius: Float, color: Color) {
    // Mắt bão
    drawCircle(
        color = color,
        center = Offset(cx, cy),
        radius = radius * 0.09f,
        style = Stroke(width = 2.2.dp.toPx())
    )
    // 2 cánh tay xoắn ốc của bão
    val arm1 = Path().apply {
        moveTo(cx, cy)
        cubicTo(
            cx - radius * 0.28f, cy - radius * 0.18f,
            cx - radius * 0.48f, cy + radius * 0.12f,
            cx - radius * 0.38f, cy + radius * 0.38f
        )
    }
    drawPath(arm1, color = color, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))

    val arm2 = Path().apply {
        moveTo(cx, cy)
        cubicTo(
            cx + radius * 0.22f, cy + radius * 0.18f,
            cx + radius * 0.42f, cy - radius * 0.05f,
            cx + radius * 0.48f, cy - radius * 0.28f
        )
    }
    drawPath(arm2, color = color, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
}

// ─── Chi Tiết Hành Tinh 4: SAO MỘC (JUPITER_STORM - VẾT ĐỎ LỚN & VÂN MÂY ĐỐI LƯU) ───

private fun DrawScope.drawJupiterTurbulentBeltsAndRedSpot(
    centerX: Float,
    centerY: Float,
    radius: Float,
    alpha: Float
) {
    // 1. Các dải khí quyển đối lưu đặc trưng của Sao Mộc (Alternating Belts & Zones)
    // Bảng màu chuẩn mực NASA: Vàng kem vani, nâu caramel, cam quế, và đỏ gạch
    val beltColors = listOf(
        Color(0xFF3E1200), // Cực Bắc nâu sẫm
        Color(0xFFFFCC80), // Vùng ôn đới vàng kem
        Color(0xFF8D3405), // Dải xích đạo Bắc nâu đỏ (NEB)
        Color(0xFFFFE0B2), // Dải xích đạo trắng kem (EZ)
        Color(0xFFD84315), // Dải xích đạo Nam cam đỏ (SEB - nơi chứa Vết Đỏ Lớn)
        Color(0xFFFFB74D), // Dải ôn đới Nam caramel
        Color(0xFF4A1800)  // Cực Nam nâu đậm
    )

    for (i in -4..4) {
        val yPos = centerY + (i * radius * 0.20f)
        val beltColor = beltColors[(i + 4) % beltColors.size]
        val waveAmplitude = (if (i % 2 == 0) 1 else -1) * radius * 0.05f

        // Đường vân mây uốn lượn hình sin mô phỏng sóng cuộn Kelvin-Helmholtz
        val beltPath = Path().apply {
            moveTo(centerX - radius, yPos)
            cubicTo(
                centerX - radius * 0.5f, yPos + waveAmplitude,
                centerX, yPos - waveAmplitude,
                centerX + radius * 0.5f, yPos + waveAmplitude
            )
            cubicTo(
                centerX + radius * 0.75f, yPos - waveAmplitude,
                centerX + radius * 0.95f, yPos + waveAmplitude,
                centerX + radius, yPos
            )
        }
        drawPath(
            beltPath,
            color = beltColor.copy(alpha = 0.85f * alpha),
            style = Stroke(width = radius * 0.16f, cap = StrokeCap.Round)
        )
    }

    // 2. VẾT ĐỎ LỚN (THE GREAT RED SPOT) - BIỂU TƯỢNG BẤT HỦ CỦA SAO MỘC
    // Nằm ở Dải xích đạo Nam (SEB), kích thước khổng lồ, hình bầu dục cuốn xoáy
    val spotX = centerX + radius * 0.22f
    val spotY = centerY + radius * 0.24f
    val spotWidth = radius * 0.46f
    val spotHeight = radius * 0.28f

    // Vệt dòng xoáy rẽ mây uốn quanh Vết Đỏ Lớn
    val deflectionPath = Path().apply {
        moveTo(spotX - spotWidth * 0.8f, spotY - spotHeight * 0.5f)
        quadraticTo(spotX, spotY - spotHeight * 0.85f, spotX + spotWidth * 0.8f, spotY - spotHeight * 0.5f)
    }
    drawPath(
        deflectionPath,
        color = Color(0xFFFFF3E0),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha
    )

    // Khối bão Vết Đỏ Lớn với tâm đỏ gạch rực lửa và viền cam xoáy
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFB71C1C), // Tâm đỏ gạch sẫm
                Color(0xFFE53935), // Đỏ tươi
                Color(0xFFFF7043), // Cam san hô
                Color(0x00FF8A65)  // Rìa ngoài tan dần
            ),
            center = Offset(spotX, spotY),
            radius = spotWidth * 0.75f
        ),
        topLeft = Offset(spotX - spotWidth / 2f, spotY - spotHeight / 2f),
        size = Size(spotWidth, spotHeight),
        alpha = alpha
    )

    // Vòng mây trắng cuộn xoáy trong mắt bão Vết Đỏ Lớn
    drawOval(
        color = Color(0xEEFFFFFF),
        topLeft = Offset(spotX - spotWidth * 0.25f, spotY - spotHeight * 0.25f),
        size = Size(spotWidth * 0.50f, spotHeight * 0.50f),
        style = Stroke(width = 1.6.dp.toPx()),
        alpha = alpha
    )

    // 3. Chuỗi ngọc bão trắng (String of Pearls) nối đuôi sau Vết Đỏ Lớn
    val pearl1 = Offset(spotX - spotWidth * 0.75f, spotY + spotHeight * 0.05f)
    val pearl2 = Offset(spotX - spotWidth * 1.25f, spotY + spotHeight * 0.08f)
    drawCircle(color = Color(0xEEFFFFFF), center = pearl1, radius = radius * 0.055f, alpha = alpha)
    drawCircle(color = Color(0xCCFFFFFF), center = pearl2, radius = radius * 0.045f, alpha = alpha)
}

// ─── Chi Tiết Hành Tinh 5: SAO THỔ (SATURN_MONARCH - VÀNH ĐAI CASSINI 3D) ──────

private fun DrawScope.drawSaturnBeltsAndHexagon(
    centerX: Float,
    centerY: Float,
    radius: Float,
    alpha: Float
) {
    // 1. Các dải khí quyển vàng bơ, hổ phách dịu ngọt của Sao Thổ
    for (i in -5..5) {
        val yPos = centerY + (i * radius * 0.18f)
        val bandColor = when (abs(i) % 4) {
            0 -> Color(0x55FFE082)
            1 -> Color(0x66FFB300)
            2 -> Color(0x44FFA000)
            else -> Color(0x336D3B00)
        }
        drawRect(
            color = bandColor.copy(alpha = bandColor.alpha * alpha),
            topLeft = Offset(centerX - radius, yPos - radius * 0.08f),
            size = Size(radius * 2f, radius * 0.16f)
        )
    }

    // 2. Cơn bão lục giác kỳ bí ở Cực Bắc Sao Thổ (Saturn's North Polar Hexagon)
    val hexRadius = radius * 0.22f
    val hexCenterY = centerY - radius * 0.82f
    val hexPath = Path()
    for (step in 0 until 6) {
        val angle = Math.toRadians((step * 60.0) - 30.0)
        val px = (centerX + hexRadius * cos(angle)).toFloat()
        val py = (hexCenterY + (hexRadius * 0.55f) * sin(angle)).toFloat()
        if (step == 0) hexPath.moveTo(px, py) else hexPath.lineTo(px, py)
    }
    hexPath.close()
    drawPath(
        hexPath,
        color = Color(0xAAFFB300),
        style = Stroke(width = 2.dp.toPx()),
        alpha = alpha
    )

    // 3. Bóng đổ cong của vành đai vắt ngang bề mặt Sao Thổ (Ring Shadow on Globe)
    val ringShadowPath = Path().apply {
        moveTo(centerX - radius * 0.96f, centerY - radius * 0.02f)
        quadraticTo(centerX, centerY + radius * 0.32f, centerX + radius * 0.96f, centerY - radius * 0.02f)
        lineTo(centerX + radius * 0.96f, centerY + radius * 0.16f)
        quadraticTo(centerX, centerY + radius * 0.50f, centerX - radius * 0.96f, centerY + radius * 0.16f)
        close()
    }
    drawPath(ringShadowPath, color = Color(0xCC180800), alpha = alpha)
}

/**
 * Vẽ Hệ Thống Vành Đai Sao Thổ 3D Đa Tầng Lộng Lẫy (Realistic Multi-Ring System):
 * - Vành A, Rãnh tối Cassini (Cassini Division), Vành B rực rỡ, Vành C mờ
 * - Phân chia chuẩn xác nửa sau (Back Ring) và nửa trước (Front Ring)
 * - Bóng đổ của hình cầu hành tinh hắt lên nửa sau vành đai
 */
private fun DrawScope.drawRealisticSaturnRings(
    centerX: Float,
    centerY: Float,
    radius: Float,
    isFront: Boolean,
    alpha: Float,
    ringPhase: Float = 0f
) {
    val ringTiltAngle = -24f
    val ringOuterA = radius * 2.95f
    val ringOuterB = radius * 0.90f

    withTransform({
        rotate(ringTiltAngle, pivot = Offset(centerX, centerY))
    }) {
        // Cắt nửa trước hoặc nửa sau theo trục hoành
        val clipRect = if (isFront) {
            Rect(centerX - ringOuterA, centerY, centerX + ringOuterA, centerY + ringOuterB * 1.5f)
        } else {
            Rect(centerX - ringOuterA, centerY - ringOuterB * 1.5f, centerX + ringOuterA, centerY)
        }

        clipRect(clipRect.left, clipRect.top, clipRect.right, clipRect.bottom) {
            // Vành C trong cùng (Crepe Ring - mờ khói)
            val ringCW = ringOuterA * 0.65f
            val ringCH = ringOuterB * 0.65f
            drawOval(
                color = Color(0x55FFD54F),
                topLeft = Offset(centerX - ringCW / 2f, centerY - ringCH / 2f),
                size = Size(ringCW, ringCH),
                style = Stroke(width = radius * 0.10f),
                alpha = alpha
            )

            // Vành B rực rỡ dày đặc (Dense main Ring B) - Vàng kim lộng lẫy
            val ringBW = ringOuterA * 0.84f
            val ringBH = ringOuterB * 0.84f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x00FFCA28),
                        Color(0xFFFFB300),
                        Color(0xFFFFF176),
                        Color(0xFFFFC107),
                        Color(0x00FF8F00)
                    ),
                    center = Offset(centerX, centerY),
                    radius = ringBW / 2f
                ),
                topLeft = Offset(centerX - ringBW / 2f, centerY - ringBH / 2f),
                size = Size(ringBW, ringBH),
                style = Stroke(width = radius * 0.24f),
                alpha = alpha
            )

            // Các rãnh vi mô bên trong Vành B (Fine sub-grooves)
            for (g in 1..3) {
                val grooveW = ringBW * (0.88f + g * 0.035f)
                val grooveH = ringBH * (0.88f + g * 0.035f)
                drawOval(
                    color = Color(0x334E2600),
                    topLeft = Offset(centerX - grooveW / 2f, centerY - grooveH / 2f),
                    size = Size(grooveW, grooveH),
                    style = Stroke(width = 0.8.dp.toPx()),
                    alpha = alpha
                )
            }

            // Rãnh chia Cassini (Cassini Division - Khe hở không gian tối đen như mực)
            val cassiniW = ringOuterA * 0.88f
            val cassiniH = ringOuterB * 0.88f
            drawOval(
                color = Color(0xFF040209),
                topLeft = Offset(centerX - cassiniW / 2f, centerY - cassiniH / 2f),
                size = Size(cassiniW, cassiniH),
                style = Stroke(width = radius * 0.05f),
                alpha = alpha
            )

            // Vành A ngoài (Outer Ring A)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x00FFE082),
                        Color(0xAAFFCA28),
                        Color(0x99FFB300),
                        Color(0x00FF8F00)
                    ),
                    center = Offset(centerX, centerY),
                    radius = ringOuterA / 2f
                ),
                topLeft = Offset(centerX - ringOuterA / 2f, centerY - ringOuterB / 2f),
                size = Size(ringOuterA, ringOuterB),
                style = Stroke(width = radius * 0.13f),
                alpha = alpha
            )

            // Rãnh Encke (Encke Gap) trong Vành A
            val enckeW = ringOuterA * 0.96f
            val enckeH = ringOuterB * 0.96f
            drawOval(
                color = Color(0x55040209),
                topLeft = Offset(centerX - enckeW / 2f, centerY - enckeH / 2f),
                size = Size(enckeW, enckeH),
                style = Stroke(width = 0.8.dp.toPx()),
                alpha = alpha
            )

            // Vành F mảnh mai ngoài cùng (Narrow F-Ring)
            val ringFW = ringOuterA * 1.05f
            val ringFH = ringOuterB * 1.05f
            drawOval(
                color = Color(0x77FFE082),
                topLeft = Offset(centerX - ringFW / 2f, centerY - ringFH / 2f),
                size = Size(ringFW, ringFH),
                style = Stroke(width = 1.4.dp.toPx()),
                alpha = alpha
            )

            // CHUYỂN ĐỘNG THẬT CỦA VÀNH ĐAI: CÁC TIỂU HÀNH TINH & HẠT BỤI BĂNG QUAY THEO QUỸ ĐẠO KEPLER
            val particleCount = 36
            for (p in 0 until particleCount) {
                // Phân bổ hạt bụi trên 4 dải quỹ đạo: Vành C, Vành B trong, Vành B ngoài, Vành A
                val trackFraction = when (p % 4) {
                    0 -> 0.65f // Vành C
                    1 -> 0.77f // Vành B trong
                    2 -> 0.84f // Vành B ngoài
                    else -> 0.95f // Vành A
                }
                // Tốc độ góc Kepler: vành trong quay nhanh hơn vành ngoài
                val orbitalSpeed = when (p % 4) {
                    0 -> 1.35f
                    1 -> 1.15f
                    2 -> 0.95f
                    else -> 0.75f
                }

                val angleDeg = (p * (360f / particleCount) + ringPhase * orbitalSpeed) % 360f
                val rad = Math.toRadians(angleDeg.toDouble())
                val px = centerX + (ringOuterA * 0.5f * trackFraction * cos(rad)).toFloat()
                val py = centerY + (ringOuterB * 0.5f * trackFraction * sin(rad)).toFloat()

                // Hiệu ứng ánh sáng tán xạ thuận (Forward scattering) rực rỡ hơn khi ở phía trước
                val isFrontSide = py > centerY
                val baseSparkle = (0.45f + 0.45f * sin(rad * 2.0 + p).absoluteValue.toFloat())
                val particleAlpha = if (isFrontSide) {
                    (baseSparkle * 0.95f).coerceIn(0.2f, 1f) * alpha
                } else {
                    (baseSparkle * 0.60f).coerceIn(0.1f, 0.7f) * alpha
                }

                val particleColor = when (p % 3) {
                    0 -> Color(0xFFFFF9C4) // Tinh thể băng trắng ngà
                    1 -> Color(0xFFFFD54F) // Bụi silicat vàng kim
                    else -> Color(0xFFFFFFFF) // Ánh sao lấp lánh chói sáng
                }

                val pRadius = when {
                    p % 6 == 0 -> 1.8.dp.toPx() // Tiểu hành tinh nhỏ
                    p % 3 == 0 -> 1.2.dp.toPx()
                    else -> 0.8.dp.toPx()       // Hạt bụi băng vi mô
                }

                drawCircle(
                    color = particleColor.copy(alpha = particleAlpha),
                    radius = pRadius,
                    center = Offset(px, py)
                )

                // Hào quang lấp lánh cho hạt lớn
                if (p % 6 == 0 && isFrontSide) {
                    drawCircle(
                        color = Color.White.copy(alpha = particleAlpha * 0.5f),
                        radius = pRadius * 2.0f,
                        center = Offset(px, py)
                    )
                }
            }

            // Nếu là nửa sau (Back Ring), vẽ bóng đổ của quả cầu Sao Thổ cắt qua vành đai
            if (!isFront) {
                val shadowWidth = radius * 1.10f
                val shadowHeight = ringOuterB * 0.85f
                drawOval(
                    color = Color(0xEE000000),
                    topLeft = Offset(centerX + radius * 0.15f, centerY - shadowHeight * 0.95f),
                    size = Size(shadowWidth, shadowHeight),
                    alpha = alpha
                )
            }
        }
    }
}

// ─── Chi Tiết Hành Tinh Mở Rộng: SAO HẢI VƯƠNG (NEPTUNE_STORM) ────────────────

private fun DrawScope.drawNeptuneStormBands(
    centerX: Float,
    centerY: Float,
    radius: Float,
    alpha: Float
) {
    for (i in -4..4) {
        val yPos = centerY + (i * radius * 0.21f)
        val waveAmplitude = (if (i % 2 == 0) 1 else -1) * radius * 0.06f
        val bandPath = Path().apply {
            moveTo(centerX - radius, yPos)
            cubicTo(
                centerX - radius * 0.4f, yPos + waveAmplitude,
                centerX + radius * 0.4f, yPos - waveAmplitude,
                centerX + radius, yPos
            )
        }
        val bandColor = when (abs(i) % 3) {
            0 -> Color(0x4481D4FA)
            1 -> Color(0x5501579B)
            else -> Color(0x330288D1)
        }
        drawPath(
            bandPath,
            color = bandColor.copy(alpha = bandColor.alpha * alpha),
            style = Stroke(width = radius * 0.15f, cap = StrokeCap.Round)
        )
    }

    val spotX = centerX + radius * 0.25f
    val spotY = centerY + radius * 0.15f
    val spotW = radius * 0.42f
    val spotH = radius * 0.22f

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF001529), Color(0xDD002D54), Color.Transparent),
            center = Offset(spotX, spotY),
            radius = spotW
        ),
        topLeft = Offset(spotX - spotW / 2f, spotY - spotH / 2f),
        size = Size(spotW, spotH),
        alpha = alpha
    )

    val scooterPath = Path().apply {
        moveTo(spotX - spotW * 0.4f, spotY + spotH * 0.3f)
        quadraticTo(spotX, spotY + spotH * 0.55f, spotX + spotW * 0.45f, spotY + spotH * 0.1f)
    }
    drawPath(
        scooterPath,
        color = Color(0xEEFFFFFF),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha
    )
}

/**
 * Vẽ vệ tinh thám hiểm 3D bay quanh quỹ đạo nghiêng:
 * - Khi bay ra sau (isBackPass = true): thu nhỏ kích thước, làm mờ
 * - Khi bay ra trước (isBackPass = false): phóng to, bật sáng đuôi phản lực neon
 */
private fun DrawScope.draw3DOrbitingProbe(
    centerX: Float,
    centerY: Float,
    planetRadius: Float,
    angleDeg: Float,
    isBackPass: Boolean
) {
    val orbitA = planetRadius * 1.52f
    val orbitB = planetRadius * 0.68f
    val orbitTiltDeg = -28f

    val angleRad = Math.toRadians(angleDeg.toDouble())
    val sinAngle = sin(angleRad).toFloat()
    val cosAngle = cos(angleRad).toFloat()

    // Quy ước: sinAngle < 0 là nửa vòng sau lưng quả cầu, sinAngle >= 0 là nửa trước mặt
    val isCurrentlyBehind = sinAngle < 0
    if (isBackPass != isCurrentlyBehind) return

    withTransform({
        rotate(orbitTiltDeg, pivot = Offset(centerX, centerY))
    }) {
        val satX = centerX + (orbitA * cosAngle)
        val satY = centerY + (orbitB * sinAngle)

        val probeScale = if (isCurrentlyBehind) 0.65f else 1.15f
        val probeAlpha = if (isCurrentlyBehind) 0.45f else 1.0f

        // Đuôi động cơ ion phản lực xanh ngọc
        drawCircle(
            color = Color(0xFF00F5D4).copy(alpha = 0.50f * probeAlpha),
            radius = 7.dp.toPx() * probeScale,
            center = Offset(satX, satY)
        )
        // Thân phi thuyền / trạm vũ trụ
        drawCircle(
            color = Color.White.copy(alpha = probeAlpha),
            radius = 3.2.dp.toPx() * probeScale,
            center = Offset(satX, satY)
        )
        // Tấm pin mặt trời 2 bên
        val wingSpan = 6.dp.toPx() * probeScale
        drawLine(
            color = Color(0xFF48CAE4).copy(alpha = probeAlpha),
            start = Offset(satX - wingSpan, satY),
            end = Offset(satX + wingSpan, satY),
            strokeWidth = 1.5.dp.toPx() * probeScale
        )
    }
}

/**
 * Vẽ màng bảo vệ trường lực Stasis Shield cho chặng LOCKED:
 * - Lưới từ trường lục giác mỏng nhẹ, trong suốt, KHÔNG che lấp bề mặt hành tinh
 */
private fun DrawScope.drawSciFiStasisShield(
    centerX: Float,
    centerY: Float,
    radius: Float
) {
    // Vành đai từ trường khóa mỏng nhẹ
    drawCircle(
        color = Color(0x6600F5D4),
        center = Offset(centerX, centerY),
        radius = radius * 1.04f,
        style = Stroke(
            width = 1.2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        )
    )
}

// ─── Đỉnh Cao Vũ Trụ: MẶT TRỜI (THE SUN / SOLAR APEX - Level Cuối To Nhất) ────

/**
 * Hàm vẽ MẶT TRỜI (THE SUN / SOLAR APEX) - Đỉnh cao vũ trụ, chặng cuối cùng to nhất và khó nhất!
 * Đặc trưng thiên văn siêu thực chân thực:
 * 1. Hào quang nhật hoa (Solar Corona) tỏa sáng rực rỡ với các tia bức xạ xung động.
 * 2. Tai lửa mặt trời (Solar Prominences & Coronal Loops) uốn cung từ trường vươn ra ngoài bề mặt.
 * 3. Đĩa quang cầu nhiệt hạch (Thermonuclear Photosphere) với hiệu ứng tự phát sáng (Limb Darkening)
 *    chuyển từ lõi trắng nóng chói lòa sang vàng lửa, cam plasma và đỏ đậm ở rìa.
 * 4. Mạng lưới vi mô các hạt plasma sôi sùng sục (Solar Granulation convection cells).
 * 5. Cụm vết đen Mặt Trời khổng lồ (Active Sunspot Regions AR13664) với lõi tối Umbra, viền Penumbra và dải từ trường Faculae.
 * 6. Tia phát quang hình sao 4 cánh (Stellar Diffraction Spikes) thể hiện đẳng cấp tối thượng của ngôi sao trung tâm.
 */
private fun DrawScope.drawTheSunSolarApex(
    centerX: Float,
    centerY: Float,
    radius: Float,
    cloudPhase: Float,
    alphaFactor: Float,
    isLocked: Boolean
) {
    // 1. Vòng hào quang Nhật hoa mở rộng (Outer Solar Corona)
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color(0xFFFFD54F).copy(alpha = 0.52f * alphaFactor),
            0.55f to Color(0xFFFF8F00).copy(alpha = 0.28f * alphaFactor),
            0.85f to Color(0xFFD84315).copy(alpha = 0.12f * alphaFactor),
            1.0f to Color.Transparent,
            center = Offset(centerX, centerY),
            radius = radius * 1.48f
        ),
        center = Offset(centerX, centerY),
        radius = radius * 1.48f
    )

    // 2. Tia bức xạ nhật hoa động (Dynamic Coronal Streamers / Solar Rays)
    val rayCount = 18
    for (i in 0 until rayCount) {
        val baseAngle = (i * (360f / rayCount) + cloudPhase * 0.15f) * (PI.toFloat() / 180f)
        val rayLength = radius * (1.18f + 0.20f * sin(baseAngle * 3f + cloudPhase * 0.05f).absoluteValue)
        val rayWidth = 2.5f + 1.2f * (i % 3)
        val cosA = cos(baseAngle)
        val sinA = sin(baseAngle)

        drawLine(
            brush = Brush.radialGradient(
                0.6f to Color(0xFFFFE082).copy(alpha = 0.40f * alphaFactor),
                1.0f to Color.Transparent,
                center = Offset(centerX, centerY),
                radius = rayLength
            ),
            start = Offset(centerX + cosA * (radius * 0.95f), centerY + sinA * (radius * 0.95f)),
            end = Offset(centerX + cosA * rayLength, centerY + sinA * rayLength),
            strokeWidth = rayWidth.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // 3. Tai lửa Mặt Trời uốn cung từ trường vọt ra ngoài viền (Solar Prominences / Coronal Loops)
    drawSolarLoopProminence(centerX, centerY, 38f, 24f, radius, 0.20f, Color(0xFFFF5722), alphaFactor)
    drawSolarLoopProminence(centerX, centerY, 138f, 28f, radius, 0.24f, Color(0xFFFF7043), alphaFactor)
    drawSolarLoopProminence(centerX, centerY, 228f, 20f, radius, 0.18f, Color(0xFFFF9800), alphaFactor)
    drawSolarLoopProminence(centerX, centerY, 312f, 26f, radius, 0.22f, Color(0xFFFF3D00), alphaFactor)

    // 4. Khối cầu quang quyển (Photosphere Core) với hiệu ứng Limb Darkening
    // Mặt Trời tự phát sáng: tâm cực nóng trắng-vàng, ra mép tối dần sang cam-đỏ plasma
    val solarColors = listOf(
        Color(0xFFFFFFFF),       // 0%: Tâm nhiệt hạch siêu nóng trắng chói
        Color(0xFFFFF9C4),       // 25%: Vàng nhạt rực rỡ
        Color(0xFFFFD54F),       // 50%: Vàng quang cầu
        Color(0xFFFF9800),       // 75%: Cam lửa
        Color(0xFFF4511E),       // 90%: Cam đậm
        Color(0xFFB71C1C)        // 100%: Đỏ sẫm viền nhật quyển
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = if (isLocked) solarColors.map { it.copy(alpha = it.alpha * 0.92f) } else solarColors,
            center = Offset(centerX, centerY),
            radius = radius
        ),
        center = Offset(centerX, centerY),
        radius = radius
    )

    // 5. Chi tiết bề mặt bên trong đĩa quang quyển
    val sunPath = Path().apply {
        addOval(Rect(centerX - radius, centerY - radius, centerX + radius, centerY + radius))
    }

    clipPath(sunPath) {
        // A. Hạt plasma đối lưu sôi sục (Solar Convection Granulation)
        drawSolarGranulation(centerX, centerY, radius, cloudPhase, alphaFactor)

        // B. Cụm vết đen Mặt Trời khổng lồ (Active Sunspot Group AR13664)
        // Cụm 1 ở bán cầu đông nam
        drawRealisticSunspot(
            centerX = centerX + radius * 0.28f,
            centerY = centerY + radius * 0.14f,
            umbraRadius = radius * 0.085f,
            penumbraRadius = radius * 0.17f,
            rotationDeg = 25f,
            alpha = alphaFactor
        )
        drawRealisticSunspot(
            centerX = centerX + radius * 0.44f,
            centerY = centerY + radius * 0.20f,
            umbraRadius = radius * 0.055f,
            penumbraRadius = radius * 0.11f,
            rotationDeg = -15f,
            alpha = alphaFactor
        )

        // Cụm 2 ở bán cầu tây bắc
        drawRealisticSunspot(
            centerX = centerX - radius * 0.32f,
            centerY = centerY - radius * 0.24f,
            umbraRadius = radius * 0.065f,
            penumbraRadius = radius * 0.13f,
            rotationDeg = 45f,
            alpha = alphaFactor
        )
        drawRealisticSunspot(
            centerX = centerX - radius * 0.18f,
            centerY = centerY - radius * 0.32f,
            umbraRadius = radius * 0.040f,
            penumbraRadius = radius * 0.085f,
            rotationDeg = 10f,
            alpha = alphaFactor
        )

        // C. Dải từ trường sáng Faculae rực rỡ xung quanh cụm vết đen
        drawSolarFaculaeVeins(centerX, centerY, radius, alphaFactor)
    }

    // 6. Viền sắc quyển phát sáng (Chromosphere Ring Glow)
    drawCircle(
        color = Color(0xFFFFE082).copy(alpha = 0.75f * alphaFactor),
        radius = radius,
        center = Offset(centerX, centerY),
        style = Stroke(width = 2.5.dp.toPx())
    )

    // 7. Tia sao 4 cánh khúc xạ quang học từ tâm nhiệt hạch (Stellar Diffraction Spikes)
    drawStellarSpikes(centerX, centerY, radius, alphaFactor)
}

/**
 * Vẽ vòng tai lửa uốn cong hình cung theo từ trường mặt trời vọt ra ngoài mép đĩa (Coronal Loop Prominence)
 */
private fun DrawScope.drawSolarLoopProminence(
    centerX: Float,
    centerY: Float,
    baseAngleDeg: Float,
    loopSpanDeg: Float,
    radius: Float,
    extensionRatio: Float,
    color: Color,
    alpha: Float
) {
    val a1 = (baseAngleDeg - loopSpanDeg / 2f) * (PI.toFloat() / 180f)
    val a2 = (baseAngleDeg + loopSpanDeg / 2f) * (PI.toFloat() / 180f)
    val midA = baseAngleDeg * (PI.toFloat() / 180f)

    val startX = centerX + cos(a1) * (radius * 0.98f)
    val startY = centerY + sin(a1) * (radius * 0.98f)
    val endX = centerX + cos(a2) * (radius * 0.98f)
    val endY = centerY + sin(a2) * (radius * 0.98f)

    val peakDist = radius * (1f + extensionRatio)
    val apexX = centerX + cos(midA) * peakDist
    val apexY = centerY + sin(midA) * peakDist

    val loopPath = Path().apply {
        moveTo(startX, startY)
        quadraticTo(apexX, apexY, endX, endY)
    }

    // Lớp vầng sáng mờ bao quanh tai lửa
    drawPath(
        path = loopPath,
        color = color.copy(alpha = 0.40f * alpha),
        style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
    )
    // Lõi tai lửa plasma nóng trắng sáng
    drawPath(
        path = loopPath,
        color = Color(0xFFFFF9C4).copy(alpha = 0.85f * alpha),
        style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round)
    )
}

/**
 * Vẽ các hạt plasma đối lưu sôi sục trên bề mặt quang cầu (Solar Granulation Cells)
 */
private fun DrawScope.drawSolarGranulation(
    centerX: Float,
    centerY: Float,
    radius: Float,
    cloudPhase: Float,
    alpha: Float
) {
    val granuleColor = Color(0x33E65100).copy(alpha = 0.40f * alpha)
    val hotCellColor = Color(0x33FFF59D).copy(alpha = 0.35f * alpha)

    // Lưới hạt dòng xoáy plasma
    for (step in 1..4) {
        val r = radius * (0.22f * step)
        val cellCount = 6 + step * 4
        for (c in 0 until cellCount) {
            val cellAngle = (c * (360f / cellCount) + cloudPhase * (0.04f * step)) * (PI.toFloat() / 180f)
            val cx = centerX + cos(cellAngle) * r
            val cy = centerY + sin(cellAngle) * r
            val cellRadius = radius * (0.065f + 0.02f * ((c + step) % 3))

            drawCircle(
                color = if ((c + step) % 2 == 0) hotCellColor else granuleColor,
                radius = cellRadius,
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * Vẽ vết đen Mặt Trời chân thực (Photorealistic Sunspot với Umbra, Penumbra & cấu trúc dạng sợi)
 */
private fun DrawScope.drawRealisticSunspot(
    centerX: Float,
    centerY: Float,
    umbraRadius: Float,
    penumbraRadius: Float,
    rotationDeg: Float,
    alpha: Float
) {
    // 1. Quầng Penumbra dạng sợi màu nâu cam sẫm
    val penumbraBrush = Brush.radialGradient(
        0.0f to Color(0xFF3E1200).copy(alpha = 0.90f * alpha),
        0.55f to Color(0xFF7A2000).copy(alpha = 0.85f * alpha),
        0.90f to Color(0xFFB73A00).copy(alpha = 0.60f * alpha),
        1.0f to Color.Transparent,
        center = Offset(centerX, centerY),
        radius = penumbraRadius
    )
    drawOval(
        brush = penumbraBrush,
        topLeft = Offset(centerX - penumbraRadius * 1.15f, centerY - penumbraRadius * 0.85f),
        size = Size(penumbraRadius * 2.3f, penumbraRadius * 1.7f)
    )

    // Các sợi từ trường penumbra hướng tâm (Penumbra radial filaments)
    val filamentColor = Color(0xFF2E0900).copy(alpha = 0.65f * alpha)
    for (i in 0 until 12) {
        val angle = (i * 30f + rotationDeg) * (PI.toFloat() / 180f)
        val fx1 = centerX + cos(angle) * (umbraRadius * 0.9f)
        val fy1 = centerY + sin(angle) * (umbraRadius * 0.9f)
        val fx2 = centerX + cos(angle) * (penumbraRadius * 0.95f)
        val fy2 = centerY + sin(angle) * (penumbraRadius * 0.95f)
        drawLine(
            color = filamentColor,
            start = Offset(fx1, fy1),
            end = Offset(fx2, fy2),
            strokeWidth = 1.0.dp.toPx()
        )
    }

    // 2. Lõi tối Umbra đen thẳm (Nhiệt độ thấp hơn quang cầu ~1500K)
    val umbraBrush = Brush.radialGradient(
        0.0f to Color(0xFF0F0300).copy(alpha = 0.98f * alpha),
        0.80f to Color(0xFF1F0600).copy(alpha = 0.95f * alpha),
        1.0f to Color(0xFF380C00).copy(alpha = 0.90f * alpha),
        center = Offset(centerX, centerY),
        radius = umbraRadius
    )
    drawOval(
        brush = umbraBrush,
        topLeft = Offset(centerX - umbraRadius * 1.1f, centerY - umbraRadius * 0.9f),
        size = Size(umbraRadius * 2.2f, umbraRadius * 1.8f)
    )
}

/**
 * Vẽ các mạng lưới dải từ trường sáng Faculae bao quanh vết đen Mặt Trời
 */
private fun DrawScope.drawSolarFaculaeVeins(
    centerX: Float,
    centerY: Float,
    radius: Float,
    alpha: Float
) {
    val faculaeColor = Color(0x66FFFDE7).copy(alpha = 0.55f * alpha)

    // Dải sáng 1 quanh cụm đông nam
    val path1 = Path().apply {
        moveTo(centerX + radius * 0.16f, centerY + radius * 0.08f)
        cubicTo(
            centerX + radius * 0.24f, centerY + radius * 0.04f,
            centerX + radius * 0.38f, centerY + radius * 0.06f,
            centerX + radius * 0.52f, centerY + radius * 0.16f
        )
        cubicTo(
            centerX + radius * 0.40f, centerY + radius * 0.28f,
            centerX + radius * 0.22f, centerY + radius * 0.24f,
            centerX + radius * 0.16f, centerY + radius * 0.08f
        )
        close()
    }
    drawPath(path1, color = faculaeColor, style = Stroke(width = 1.5.dp.toPx()))

    // Dải sáng 2 quanh cụm tây bắc
    val path2 = Path().apply {
        moveTo(centerX - radius * 0.40f, centerY - radius * 0.18f)
        cubicTo(
            centerX - radius * 0.36f, centerY - radius * 0.34f,
            centerX - radius * 0.14f, centerY - radius * 0.38f,
            centerX - radius * 0.10f, centerY - radius * 0.22f
        )
        cubicTo(
            centerX - radius * 0.20f, centerY - radius * 0.16f,
            centerX - radius * 0.32f, centerY - radius * 0.14f,
            centerX - radius * 0.40f, centerY - radius * 0.18f
        )
        close()
    }
    drawPath(path2, color = faculaeColor, style = Stroke(width = 1.5.dp.toPx()))
}

/**
 * Vẽ tia khúc xạ hình sao 4 cánh (Stellar Diffraction Spikes) từ tâm Mặt Trời
 */
private fun DrawScope.drawStellarSpikes(
    centerX: Float,
    centerY: Float,
    radius: Float,
    alpha: Float
) {
    val spikeLength = radius * 0.95f

    // Tia ngang
    drawLine(
        brush = Brush.radialGradient(
            0.0f to Color.White.copy(alpha = 0.70f * alpha),
            1.0f to Color.Transparent,
            center = Offset(centerX, centerY),
            radius = spikeLength
        ),
        start = Offset(centerX - spikeLength, centerY),
        end = Offset(centerX + spikeLength, centerY),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Tia dọc
    drawLine(
        brush = Brush.radialGradient(
            0.0f to Color.White.copy(alpha = 0.70f * alpha),
            1.0f to Color.Transparent,
            center = Offset(centerX, centerY),
            radius = spikeLength
        ),
        start = Offset(centerX, centerY - spikeLength),
        end = Offset(centerX, centerY + spikeLength),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Điểm sáng hạt nhân rực rỡ ở chính giữa
    drawCircle(
        color = Color.White.copy(alpha = 0.90f * alpha),
        radius = 4.dp.toPx(),
        center = Offset(centerX, centerY)
    )
}

