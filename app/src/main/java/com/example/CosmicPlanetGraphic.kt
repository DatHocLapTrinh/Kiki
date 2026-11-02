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
    TERRA_GAIA,      // Chặng 1: Trái Đất: Đại dương xanh thẳm, mảng lục địa xanh, mây lông vũ tự nhiên
    MOON_GRAY,       // Chặng 2: Mặt Trăng: Khoáng thạch bạc, miệng hố va chạm 3D & tia Tycho
    SATURN_MONARCH,  // Chặng 3: Sao Thổ: Vành đai Cassini 3D đa tầng xoay Kepler lộng lẫy
    JUPITER_STORM,   // Chặng 4: Sao Mộc: Dải mây cuộn sóng đối lưu & Vết Đỏ Lớn (Great Red Spot)
    MARS_CRIMSON,    // Chặng 5: Sao Hỏa: Cát đỏ rực rỡ, chỏm băng bắc cực & đại hẻm núi Valles Marineris
    URANUS_ICE,      // Chặng 6: Sao Thiên Vương: Khối băng ngọc bích, chỏm cực bắc phát sáng chói lọi & dải vân mây êm dịu
    NEPTUNE_STORM,   // Chặng 7: Sao Hải Vương: Băng lam coban sâu thẳm, Vết Tối Lớn (Great Dark Spot) & mây trắng Scooter
    SOLAR_SUN        // Chặng 8 / Cuối: Mặt Trời: Siêu lóa sáng CME mép phải, tai lửa plasma & quang quyển sôi sục
}

object CosmicPlanetHelper {

    fun getPlanetTypeForStage(index: Int, totalStages: Int = -1): CosmicPlanetType {
        if (totalStages > 1 && index == totalStages - 1) {
            return CosmicPlanetType.SOLAR_SUN
        }
        return when (index % 8) {
            0 -> CosmicPlanetType.TERRA_GAIA      // Chặng 1: Trái Đất (Địa Cầu Xanh)
            1 -> CosmicPlanetType.MOON_GRAY       // Chặng 2: Mặt Trăng (Vệ Tinh Bạc)
            2 -> CosmicPlanetType.SATURN_MONARCH  // Chặng 3: Sao Thổ (Vành Đai Vàng Cassini 3D)
            3 -> CosmicPlanetType.JUPITER_STORM   // Chặng 4: Sao Mộc (Vết Đỏ Lớn)
            4 -> CosmicPlanetType.MARS_CRIMSON    // Chặng 5: Sao Hỏa (Hành Tinh Đỏ)
            5 -> CosmicPlanetType.URANUS_ICE      // Chặng 6: Sao Thiên Vương (Băng Lam Ngọc Bích)
            6 -> CosmicPlanetType.NEPTUNE_STORM   // Chặng 7: Sao Hải Vương (Lam Thẫm & Vết Bão Tối)
            else -> CosmicPlanetType.SOLAR_SUN    // Chặng 8 / Mốc Cuối: Mặt Trời (Đỉnh Cao Thái Dương)
        }
    }

    fun getPlanetDiameter(index: Int, totalStages: Int = -1): Dp {
        if (totalStages > 1 && index == totalStages - 1) {
            return 132.dp
        }
        return when (index % 8) {
            0 -> 74.dp   // Chặng 1: Trái Đất (~74dp)
            1 -> 80.dp   // Chặng 2: Mặt Trăng (~80dp)
            2 -> 88.dp   // Chặng 3: Sao Thổ (~88dp + vành đai)
            3 -> 96.dp   // Chặng 4: Sao Mộc (~96dp)
            4 -> 104.dp  // Chặng 5: Sao Hỏa (~104dp)
            5 -> 112.dp  // Chặng 6: Sao Thiên Vương (~112dp)
            6 -> 120.dp  // Chặng 7: Sao Hải Vương (~120dp)
            else -> 132.dp // Chặng 8 / Mốc Cuối: Mặt Trời (~132dp)
        }
    }

    fun getPlanetTypeName(type: CosmicPlanetType, isEnglish: Boolean = false): String {
        return when (type) {
            CosmicPlanetType.TERRA_GAIA     -> if (isEnglish) "Earth (Terra Gaia)" else "Trái Đất (Địa Cầu Xanh)"
            CosmicPlanetType.MOON_GRAY      -> if (isEnglish) "The Moon (Lunar Outpost)" else "Mặt Trăng (Vệ Tinh Bạc)"
            CosmicPlanetType.SATURN_MONARCH -> if (isEnglish) "Saturn (Cassini Monarch)" else "Sao Thổ (Vành Đai Vàng)"
            CosmicPlanetType.JUPITER_STORM  -> if (isEnglish) "Jupiter (Great Red Spot)" else "Sao Mộc (Vết Đỏ Lớn)"
            CosmicPlanetType.MARS_CRIMSON   -> if (isEnglish) "Mars (Crimson Dunes)" else "Sao Hỏa (Hành Tinh Đỏ)"
            CosmicPlanetType.URANUS_ICE     -> if (isEnglish) "Uranus (Cyan Ice Giant)" else "Sao Thiên Vương"
            CosmicPlanetType.NEPTUNE_STORM  -> if (isEnglish) "Neptune (Great Dark Spot)" else "Sao Hải Vương"
            CosmicPlanetType.SOLAR_SUN      -> if (isEnglish) "The Sun (Solar Apex)" else "Mặt Trời (Đỉnh Cao Thái Dương)"
        }
    }

    fun getPlanetPrimaryColor(type: CosmicPlanetType): Color {
        return when (type) {
            CosmicPlanetType.TERRA_GAIA     -> Color(0xFF00B4D8) // Earth Cyan Blue
            CosmicPlanetType.MOON_GRAY      -> Color(0xFFCFD8DC) // Lunar Slate Silver
            CosmicPlanetType.SATURN_MONARCH -> Color(0xFFE2CA9E) // Saturn Warm Sand & Champagne Gold
            CosmicPlanetType.JUPITER_STORM  -> Color(0xFFF97316) // Jupiter Warm Amber Caramel
            CosmicPlanetType.MARS_CRIMSON   -> Color(0xFFEA580C) // Mars Terracotta Red
            CosmicPlanetType.URANUS_ICE     -> Color(0xFF26C6DA) // Uranus Aquamarine / Pale Turquoise
            CosmicPlanetType.NEPTUNE_STORM  -> Color(0xFF2563EB) // Neptune Deep Azure Cobalt
            CosmicPlanetType.SOLAR_SUN      -> Color(0xFFFFB703) // Blazing Solar Gold / Amber Orange
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
        isSun    -> planetDiameter + 56.dp
        else     -> planetDiameter + 28.dp
    }
    val containerHeight = when {
        isSun    -> planetDiameter + 56.dp
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

        // Bề mặt hành tinh được vẽ hoàn toàn bằng Canvas với các chi tiết nhận diện đặc thù
        // (Tuyệt đối không dùng Modifier.blur để tránh bị viền khung vuông do bounding box clipping)
        Canvas(modifier = Modifier.size(width = containerWidth, height = containerHeight)) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = planetDiameter.toPx() / 2f

            // Hào quang vũ trụ tỏa tròn tự nhiên không viền góc
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (isCurrent) auraGlow * 0.35f else if (isCompleted) 0.22f else 0.10f),
                        glowColor.copy(alpha = if (isCurrent) auraGlow * 0.12f else 0.03f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = radius * 1.50f
                ),
                center = Offset(centerX, centerY),
                radius = radius * 1.50f
            )

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
            Color(0xFFFBF6EC), // Highlight champagne đón sáng (NASA Cassini true-color)
            Color(0xFFECD8B4), // Vàng cát ấm áp (warm golden sand)
            Color(0xFFD8BE93), // Nâu vàng mật ong (honey ochre)
            Color(0xFFA6855B), // Dải sẫm caramel
            Color(0xFF5A3E20), // Hoàng hôn gỗ tếch
            Color(0xFF191006)  // Góc khuất bóng tối sâu thẳm
        )
        CosmicPlanetType.URANUS_ICE -> listOf(
            Color(0xFFE0F7FA), // Highlight đón sáng trắng ngọc ở cực bắc
            Color(0xFFB2EBF2), // Xanh ngọc lam nhạt
            Color(0xFF4DD0E1), // Xanh ngọc bích tươi sáng
            Color(0xFF26C6DA), // Cyan dịu
            Color(0xFF0097A7), // Cyan sẫm
            Color(0xFF004D40), // Chàm ngọc sâu thẳm
            Color(0xFF00252B)  // Bóng tối vũ trụ
        )
        CosmicPlanetType.NEPTUNE_STORM -> listOf(
            Color(0xFFDBEAFE), // Highlight đón sáng xanh lam ngọc nhạt
            Color(0xFF93C5FD), // Lam ngọc sáng
            Color(0xFF3B82F6), // Xanh dương coban
            Color(0xFF2563EB), // Xanh sapphire đại dương
            Color(0xFF1D4ED8), // Xanh thẳm
            Color(0xFF1E3A8A), // Chàm sẫm
            Color(0xFF0F172A)  // Bóng tối đại dương sâu thẳm
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
                drawMoonHighlandsAndCraters(centerX, centerY, radius, cloudPhase, alphaFactor)
            }
            CosmicPlanetType.MARS_CRIMSON -> {
                drawMarsCanyonsAndVolcano(centerX, centerY, radius, cloudPhase, alphaFactor)
            }
            CosmicPlanetType.TERRA_GAIA -> {
                drawTerraGaiaBiosphere(centerX, centerY, radius, cloudPhase, alphaFactor)
            }
            CosmicPlanetType.JUPITER_STORM -> {
                drawJupiterTurbulentBeltsAndRedSpot(centerX, centerY, radius, cloudPhase, alphaFactor)
            }
            CosmicPlanetType.SATURN_MONARCH -> {
                drawSaturnBeltsAndHexagon(centerX, centerY, radius, alphaFactor)
            }
            CosmicPlanetType.URANUS_ICE -> {
                drawUranusIceAtmosphere(centerX, centerY, radius, cloudPhase, alphaFactor)
            }
            CosmicPlanetType.NEPTUNE_STORM -> {
                drawNeptuneStormBands(centerX, centerY, radius, cloudPhase, alphaFactor)
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
            CosmicPlanetType.SATURN_MONARCH -> Color(0x44D8BE93)
            CosmicPlanetType.URANUS_ICE     -> Color(0x444DD0E1)
            CosmicPlanetType.NEPTUNE_STORM  -> Color(0x443B82F6)
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
        CosmicPlanetType.SATURN_MONARCH -> Color(0xFFECD8B4)
        CosmicPlanetType.URANUS_ICE     -> Color(0xFF4DD0E1)
        CosmicPlanetType.NEPTUNE_STORM  -> Color(0xFF60A5FA)
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
    cloudPhase: Float,
    alpha: Float
) {
    // 0. Dao động trục Libration tự nhiên (Lunar Libration Sway):
    // Mặt Trăng lắc lư nhẹ theo chu kỳ quỹ đạo hình sin, tạo cảm giác khối cầu lơ lửng 3D sống động
    val librationAngle = Math.toRadians(cloudPhase.toDouble() * 0.8)
    val libShiftX = (sin(librationAngle) * radius * 0.032f).toFloat()
    val libShiftY = (cos(librationAngle * 2.0) * radius * 0.016f).toFloat()

    val moonCenterX = centerX + libShiftX
    val moonCenterY = centerY + libShiftY

    // 1. Biển dung nham basalt nguội sẫm màu (Lunar Maria - Chuẩn ảnh thiên văn Mặt Trăng)
    val mareDark = Color(0x881F262B).copy(alpha = 0.78f * alpha)
    val mareMid = Color(0x5537474F).copy(alpha = 0.70f * alpha)

    // Mare Imbrium (Biển Mưa lớn ở tây bắc)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(mareDark, mareMid, Color.Transparent),
            center = Offset(moonCenterX - radius * 0.25f, moonCenterY - radius * 0.30f),
            radius = radius * 0.40f
        ),
        topLeft = Offset(moonCenterX - radius * 0.58f, moonCenterY - radius * 0.55f),
        size = Size(radius * 0.62f, radius * 0.50f)
    )

    // Oceanus Procellarum (Đại dương Bão Tố trải dài phía tây)
    val oceanusPath = Path().apply {
        moveTo(moonCenterX - radius * 0.75f, moonCenterY - radius * 0.15f)
        cubicTo(
            moonCenterX - radius * 0.50f, moonCenterY - radius * 0.60f,
            moonCenterX - radius * 0.18f, moonCenterY - radius * 0.38f,
            moonCenterX - radius * 0.20f, moonCenterY + radius * 0.12f
        )
        cubicTo(
            moonCenterX - radius * 0.38f, moonCenterY + radius * 0.35f,
            moonCenterX - radius * 0.85f, moonCenterY + radius * 0.18f,
            moonCenterX - radius * 0.75f, moonCenterY - radius * 0.15f
        )
        close()
    }
    drawPath(oceanusPath, color = mareDark)

    // Mare Serenitatis & Mare Tranquillitatis (Biển Thanh Bình & Biển Tĩnh Lặng phía đông bắc)
    val serenitatisPath = Path().apply {
        moveTo(moonCenterX + radius * 0.08f, moonCenterY - radius * 0.28f)
        cubicTo(
            moonCenterX + radius * 0.45f, moonCenterY - radius * 0.38f,
            moonCenterX + radius * 0.65f, moonCenterY - radius * 0.08f,
            moonCenterX + radius * 0.40f, moonCenterY + radius * 0.26f
        )
        cubicTo(
            moonCenterX + radius * 0.12f, moonCenterY + radius * 0.32f,
            moonCenterX + radius * 0.02f, moonCenterY - radius * 0.08f,
            moonCenterX + radius * 0.08f, moonCenterY - radius * 0.28f
        )
        close()
    }
    drawPath(serenitatisPath, color = mareMid)

    // 2. Tia phóng xạ Tycho Crater lấp lánh (Tycho Ejecta Rays Shimmer) - vệt bụi trắng mịn tỏa ra tự nhiên
    val tychoX = moonCenterX - radius * 0.14f
    val tychoY = moonCenterY + radius * 0.48f
    val rayGlimmer = 0.38f + 0.22f * sin(Math.toRadians(cloudPhase.toDouble() * 2.5)).toFloat()
    val rayColor = Color(0x44ECEFF1).copy(alpha = rayGlimmer * alpha)
    for (i in 0 until 12) {
        val rayAngle = (i * 30f + 12f) * (PI.toFloat() / 180f)
        val dynamicRayLen = radius * (0.80f + 0.15f * sin(Math.toRadians(cloudPhase.toDouble() * 1.8 + i * 30.0)).toFloat())
        val endRx = tychoX + cos(rayAngle) * dynamicRayLen
        val endRy = tychoY + sin(rayAngle) * dynamicRayLen
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(rayColor, rayColor.copy(alpha = 0.10f), Color.Transparent),
                start = Offset(tychoX, tychoY),
                end = Offset(endRx, endRy)
            ),
            start = Offset(tychoX, tychoY),
            end = Offset(endRx, endRy),
            strokeWidth = 1.1.dp.toPx()
        )
    }

    // 3. CÁC VẾT LÕM 3D CHÂN THỰC TRÊN MẶT TRĂNG VỚI HIỆU ỨNG PHẢN QUANG ĐỘNG
    val glint = 0.80f + 0.20f * sin(Math.toRadians(cloudPhase.toDouble() * 1.6)).toFloat()

    // - Tycho (Nam Cực, có chóp núi trung tâm)
    drawRealisticLunarCrater(tychoX, tychoY, radius * 0.20f, alpha, hasCentralPeak = true, glintFactor = glint)

    // - Copernicus (Vùng xích đạo tây, lòng chảo sâu)
    drawRealisticLunarCrater(moonCenterX - radius * 0.28f, moonCenterY - radius * 0.08f, radius * 0.17f, alpha, hasCentralPeak = true, glintFactor = glint)

    // - Kepler (Phía tây Procellarum)
    drawRealisticLunarCrater(moonCenterX - radius * 0.54f, moonCenterY + radius * 0.06f, radius * 0.10f, alpha, glintFactor = glint)

    // - Plato (Miệng hố phẳng sẫm dung nham ở rìa bắc Biển Mưa)
    drawRealisticLunarCrater(moonCenterX - radius * 0.16f, moonCenterY - radius * 0.52f, radius * 0.13f, alpha, glintFactor = glint)

    // - Theophilus (Chuỗi hố va chạm phía đông nam)
    drawRealisticLunarCrater(moonCenterX + radius * 0.26f, moonCenterY + radius * 0.16f, radius * 0.15f, alpha, hasCentralPeak = true, glintFactor = glint)

    // - Langrenus (Rìa đông)
    drawRealisticLunarCrater(moonCenterX + radius * 0.50f, moonCenterY + radius * 0.02f, radius * 0.12f, alpha, glintFactor = glint)

    // - Clavius (Lòng chảo cổ đại khổng lồ ở cực nam)
    drawRealisticLunarCrater(moonCenterX - radius * 0.02f, moonCenterY + radius * 0.72f, radius * 0.18f, alpha, glintFactor = glint)

    // - Các vết lõm vi mô dọc đường ranh giới bóng tối Terminator
    drawRealisticLunarCrater(moonCenterX + radius * 0.18f, moonCenterY + radius * 0.42f, radius * 0.08f, alpha, glintFactor = glint)
    drawRealisticLunarCrater(moonCenterX + radius * 0.36f, moonCenterY - radius * 0.22f, radius * 0.09f, alpha, glintFactor = glint)
    drawRealisticLunarCrater(moonCenterX - radius * 0.42f, moonCenterY - radius * 0.32f, radius * 0.08f, alpha, glintFactor = glint)

    // 4. Các điểm tinh thể Regolith lấp lánh vi mô (Subtle Lunar Regolith Twinkles)
    val twinklePoints = listOf(
        Offset(moonCenterX - radius * 0.35f, moonCenterY + radius * 0.15f),
        Offset(moonCenterX + radius * 0.12f, moonCenterY - radius * 0.45f),
        Offset(moonCenterX - radius * 0.05f, moonCenterY - radius * 0.18f),
        Offset(moonCenterX + radius * 0.38f, moonCenterY + radius * 0.32f)
    )
    for ((idx, pt) in twinklePoints.withIndex()) {
        val ptAlpha = (0.20f + 0.50f * sin(Math.toRadians(cloudPhase.toDouble() * 3.0 + idx * 80.0)).toFloat()).coerceIn(0f, 1f) * alpha
        drawCircle(
            color = Color.White.copy(alpha = ptAlpha),
            center = pt,
            radius = 1.2.dp.toPx()
        )
    }
}

/**
 * Vẽ vết lõm miệng hố va chạm 3D chân thực (Concave 3D Crater Depression):
 * - Không dùng nét vẽ bánh răng / vòng tròn cứng
 * - Khối lõm lòng chảo với bóng đổ sâu từ vách đón nắng (góc 10h)
 * - Vách lòng chảo đối diện hắt sáng tạo chiều sâu thụt vào trong
 */
private fun DrawScope.drawRealisticLunarCrater(
    cx: Float,
    cy: Float,
    craterRadius: Float,
    alpha: Float,
    hasCentralPeak: Boolean = false,
    glintFactor: Float = 1.0f
) {
    // 1. Vành ngoài đón sáng tạo gờ nổi (Raised outer sunlit rim highlight)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xBBECEFF1).copy(alpha = (0.85f * glintFactor).coerceIn(0f, 1f) * alpha),
                Color(0x44B0BEC5).copy(alpha = 0.40f * alpha),
                Color.Transparent
            ),
            center = Offset(cx - craterRadius * 0.35f, cy - craterRadius * 0.35f),
            radius = craterRadius * 1.30f
        ),
        center = Offset(cx, cy),
        radius = craterRadius * 1.30f
    )

    // 2. Lòng chảo trũng sâu (Concave bowl cavity)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF14191C), // Đáy sâu sẫm màu
                Color(0xFF21292D), // Lòng chảo trũng
                Color(0xFF37474F)  // Gờ vách ngoài
            ),
            center = Offset(cx - craterRadius * 0.15f, cy - craterRadius * 0.15f),
            radius = craterRadius
        ),
        center = Offset(cx, cy),
        radius = craterRadius,
        alpha = alpha
    )

    // 3. Bóng đổ lòng hố sâu sắc nét từ vách đón nắng phía 10h (Inner bowl shadow)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xF0080B0D),
                Color(0xAA080B0D),
                Color.Transparent
            ),
            center = Offset(cx - craterRadius * 0.25f, cy - craterRadius * 0.25f),
            radius = craterRadius * 0.75f
        ),
        topLeft = Offset(cx - craterRadius * 0.70f, cy - craterRadius * 0.70f),
        size = Size(craterRadius * 1.30f, craterRadius * 1.10f),
        alpha = alpha
    )

    // 4. Vách trong lòng chảo đối diện đón ánh sáng góc 4h (Sunlit inner wall)
    drawArc(
        color = Color(0xBBECEFF1).copy(alpha = (0.80f * glintFactor).coerceIn(0f, 1f) * alpha),
        startAngle = 15f,
        sweepAngle = 100f,
        useCenter = false,
        topLeft = Offset(cx - craterRadius * 0.82f, cy - craterRadius * 0.82f),
        size = Size(craterRadius * 1.64f, craterRadius * 1.64f),
        style = Stroke(width = (craterRadius * 0.20f).coerceIn(1.2f, 3.2f)),
        alpha = alpha
    )

    // 5. Đỉnh chóp núi trung tâm miệng hố (Central Mountain Peak)
    if (hasCentralPeak) {
        drawCircle(
            color = Color(0xDDECEFF1).copy(alpha = 0.90f * alpha),
            center = Offset(cx - craterRadius * 0.05f, cy - craterRadius * 0.05f),
            radius = (craterRadius * 0.16f).coerceAtLeast(1.2.dp.toPx()),
            alpha = alpha
        )
    }
}

// ─── Chi Tiết Hành Tinh 2: SAO HỎA (MARS_CRIMSON) ───────────────────────────

private fun DrawScope.drawMarsCanyonsAndVolcano(
    centerX: Float,
    centerY: Float,
    radius: Float,
    cloudPhase: Float,
    alpha: Float
) {
    // 0. Chuyển động tự quay chậm của bề mặt Sao Hỏa (Mars Rotational Sway)
    val marsSwayAngle = Math.toRadians(cloudPhase.toDouble() * 0.6)
    val marsShiftX = (sin(marsSwayAngle) * radius * 0.038f).toFloat()

    val mCenterX = centerX + marsShiftX

    // Vùng cao nguyên dung nham sẫm màu Syrtis Major (Mảng tối đặc trưng của Sao Hỏa)
    val syrtisMajorColor = Color(0x774A1204).copy(alpha = 0.8f * alpha)
    val syrtisPath = Path().apply {
        moveTo(mCenterX + radius * 0.1f, centerY - radius * 0.45f)
        cubicTo(
            mCenterX + radius * 0.45f, centerY - radius * 0.25f,
            mCenterX + radius * 0.55f, centerY + radius * 0.15f,
            mCenterX + radius * 0.20f, centerY + radius * 0.35f
        )
        cubicTo(
            mCenterX + radius * 0.05f, centerY + radius * 0.15f,
            mCenterX, centerY - radius * 0.2f,
            mCenterX + radius * 0.1f, centerY - radius * 0.45f
        )
        close()
    }
    drawPath(syrtisPath, color = syrtisMajorColor)

    // Chỏm băng Bắc Cực (North Polar Ice Cap) phát quang thăng hoa nhịp nhàng
    val icePulse = 0.82f + 0.18f * sin(Math.toRadians(cloudPhase.toDouble() * 1.5)).toFloat()
    val iceCapW = radius * (0.76f + 0.04f * sin(Math.toRadians(cloudPhase.toDouble() * 1.5)).toFloat())
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = icePulse * alpha),
                Color(0xFFFFFFFF).copy(alpha = (icePulse * 0.85f) * alpha),
                Color(0x00FFFFFF)
            ),
            center = Offset(centerX, centerY - radius * 0.90f),
            radius = radius * 0.45f
        ),
        topLeft = Offset(centerX - iceCapW / 2f, centerY - radius * 0.98f),
        size = Size(iceCapW, radius * 0.28f)
    )

    // Siêu núi lửa lớn nhất hệ mặt trời Olympus Mons với luồng nhiệt vi mô
    val volcanoX = mCenterX - radius * 0.32f
    val volcanoY = centerY - radius * 0.25f
    val calderaRadius = radius * 0.20f
    val volcanoPulse = 1.0f + 0.06f * sin(Math.toRadians(cloudPhase.toDouble() * 1.8)).toFloat()

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x995C1705), Color(0x66FF8A65), Color.Transparent),
            center = Offset(volcanoX, volcanoY),
            radius = calderaRadius * 1.6f * volcanoPulse
        ),
        center = Offset(volcanoX, volcanoY),
        radius = calderaRadius * 1.6f * volcanoPulse,
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
        moveTo(mCenterX - radius * 0.70f, centerY + radius * 0.08f)
        cubicTo(
            mCenterX - radius * 0.25f, centerY + radius * 0.28f,
            mCenterX + radius * 0.15f, centerY + radius * 0.08f,
            mCenterX + radius * 0.68f, centerY + radius * 0.35f
        )
    }
    // Lòng hẻm vực sâu thăm thẳm
    drawPath(
        canyonMain,
        color = Color(0xFF260600),
        style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha
    )
    // Vách đá hẻm núi rực sáng màu cam hổ phách có ánh nắng chiếu xiên lấp lánh
    val chasmGlint = 0.75f + 0.25f * sin(Math.toRadians(cloudPhase.toDouble() * 2.0)).toFloat()
    drawPath(
        canyonMain,
        color = Color(0xFFFFB74D).copy(alpha = chasmGlint * alpha),
        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
    )

    // Nhánh đứt gãy hẻm núi phụ
    val canyonBranch = Path().apply {
        moveTo(mCenterX - radius * 0.15f, centerY + radius * 0.24f)
        quadraticTo(mCenterX + radius * 0.12f, centerY + radius * 0.40f, mCenterX + radius * 0.45f, centerY + radius * 0.50f)
    }
    drawPath(
        canyonBranch,
        color = Color(0xFF260600),
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        alpha = alpha
    )

    // 4. Bão cát bụi đỏ khí quyển Sao Hỏa bay dập dờn (Martian Dust Storm Streams)
    val dustAngle1 = Math.toRadians(cloudPhase.toDouble() * 1.1)
    val dustShift1 = (sin(dustAngle1) * radius * 0.12f).toFloat()
    val dustAlpha1 = (0.28f + 0.12f * cos(dustAngle1).toFloat()) * alpha

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x88FF7043).copy(alpha = dustAlpha1),
                Color(0x44D84315).copy(alpha = dustAlpha1 * 0.6f),
                Color.Transparent
            ),
            center = Offset(centerX + dustShift1, centerY + radius * 0.12f),
            radius = radius * 0.48f
        ),
        topLeft = Offset(centerX - radius * 0.48f + dustShift1, centerY - radius * 0.05f),
        size = Size(radius * 0.96f, radius * 0.34f)
    )

    val dustAngle2 = Math.toRadians(cloudPhase.toDouble() * 0.7 + 90.0)
    val dustShift2 = (cos(dustAngle2) * radius * 0.10f).toFloat()
    val dustAlpha2 = (0.22f + 0.10f * sin(dustAngle2).toFloat()) * alpha
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x66FFAB91).copy(alpha = dustAlpha2),
                Color.Transparent
            ),
            center = Offset(centerX + dustShift2, centerY - radius * 0.35f),
            radius = radius * 0.35f
        ),
        topLeft = Offset(centerX - radius * 0.35f + dustShift2, centerY - radius * 0.48f),
        size = Size(radius * 0.70f, radius * 0.26f)
    )
}

// ─── Chi Tiết Hành Tinh 1: TRÁI ĐẤT (TERRA_GAIA) ────────────────────────────

private fun DrawScope.drawTerraGaiaBiosphere(
    centerX: Float,
    centerY: Float,
    radius: Float,
    cloudPhase: Float,
    alpha: Float
) {
    // 1. Thềm lục địa nông viền màu ngọc lam rực rỡ (Turquoise shallow ocean shelves - Chuẩn ảnh Blue Marble)
    val shelfColor = Color(0x9900B4D8).copy(alpha = 0.90f * alpha)
    val deepShelfColor = Color(0x6648CAE4).copy(alpha = 0.85f * alpha)
    val landColor = Color(0xFF235A3E).copy(alpha = 0.95f * alpha)
    val highlandColor = Color(0xFF52796F).copy(alpha = 0.92f * alpha)
    val desertColor = Color(0xFFC98A52).copy(alpha = 0.92f * alpha)
    val aridColor = Color(0xFFBC6C25).copy(alpha = 0.90f * alpha)

    // Lục địa Châu Á & Đông Nam Á (Bán đảo Đông Dương, Trung Quốc, Ấn Độ - Giống góc chụp ảnh 1)
    val asiaLand = Path().apply {
        moveTo(centerX - radius * 0.58f, centerY - radius * 0.22f)
        cubicTo(
            centerX - radius * 0.45f, centerY - radius * 0.65f,
            centerX + radius * 0.08f, centerY - radius * 0.58f,
            centerX + radius * 0.38f, centerY - radius * 0.28f
        )
        cubicTo(
            centerX + radius * 0.22f, centerY - radius * 0.05f,
            centerX + radius * 0.05f, centerY + radius * 0.18f, // Mũi Cà Mau / Đông Nam Á
            centerX - radius * 0.15f, centerY + radius * 0.12f
        )
        cubicTo(
            centerX - radius * 0.22f, centerY - radius * 0.05f,
            centerX - radius * 0.45f, centerY + radius * 0.05f,
            centerX - radius * 0.58f, centerY - radius * 0.22f
        )
        close()
    }
    // Viền thềm san hô ngọc lam rộng tỏa ra đại dương
    drawPath(asiaLand, color = deepShelfColor, style = Stroke(width = 5.dp.toPx()))
    drawPath(asiaLand, color = shelfColor, style = Stroke(width = 2.5.dp.toPx()))
    // Mảng lục địa xanh rừng rậm
    drawPath(asiaLand, color = landColor)

    // Vùng cao nguyên Tây Tạng & sa mạc Gobi màu cát nâu vàng
    drawOval(
        color = desertColor,
        topLeft = Offset(centerX - radius * 0.22f, centerY - radius * 0.48f),
        size = Size(radius * 0.38f, radius * 0.24f)
    )
    drawOval(
        color = highlandColor,
        topLeft = Offset(centerX - radius * 0.32f, centerY - radius * 0.38f),
        size = Size(radius * 0.22f, radius * 0.16f)
    )

    // Lục địa Châu Úc (Australia - Nổi bật ở góc đông nam như ảnh 1)
    val australiaLand = Path().apply {
        moveTo(centerX + radius * 0.18f, centerY + radius * 0.38f)
        cubicTo(
            centerX + radius * 0.38f, centerY + radius * 0.32f,
            centerX + radius * 0.58f, centerY + radius * 0.42f,
            centerX + radius * 0.48f, centerY + radius * 0.68f
        )
        cubicTo(
            centerX + radius * 0.28f, centerY + radius * 0.72f,
            centerX + radius * 0.12f, centerY + radius * 0.55f,
            centerX + radius * 0.18f, centerY + radius * 0.38f
        )
        close()
    }
    drawPath(australiaLand, color = deepShelfColor, style = Stroke(width = 4.dp.toPx()))
    drawPath(australiaLand, color = shelfColor, style = Stroke(width = 2.dp.toPx()))
    drawPath(australiaLand, color = aridColor) // Vùng sa mạc đỏ Red Centre đặc trưng
    // Bờ biển phía đông nước Úc xanh tươi
    drawCircle(
        color = landColor,
        center = Offset(centerX + radius * 0.44f, centerY + radius * 0.52f),
        radius = radius * 0.10f
    )

    // Quần đảo Indonesia & Philippines (Chuỗi đảo ngọc lam)
    val islands = listOf(
        Offset(centerX - radius * 0.05f, centerY + radius * 0.24f),
        Offset(centerX + radius * 0.10f, centerY + radius * 0.22f),
        Offset(centerX + radius * 0.22f, centerY + radius * 0.16f),
        Offset(centerX + radius * 0.15f, centerY + radius * 0.05f)
    )
    for (pt in islands) {
        drawCircle(color = shelfColor, center = pt, radius = 3.5.dp.toPx())
        drawCircle(color = landColor, center = pt, radius = 2.dp.toPx())
    }

    // 2. Ánh đèn thành phố rực sáng về đêm ở nửa khuất sáng (Night-side City Lights)
    val cityLightColor = Color(0xFFFFD54F).copy(alpha = 0.95f * alpha)
    val cityPoints = listOf(
        Offset(centerX + radius * 0.35f, centerY + radius * 0.15f),
        Offset(centerX + radius * 0.48f, centerY + radius * 0.25f),
        Offset(centerX + radius * 0.40f, centerY + radius * 0.42f),
        Offset(centerX + radius * 0.30f, centerY + radius * 0.50f),
        Offset(centerX + radius * 0.52f, centerY - radius * 0.05f)
    )
    cityPoints.forEach { pt ->
        drawCircle(color = cityLightColor, center = pt, radius = 1.6.dp.toPx())
        drawCircle(color = Color(0x66FFA000), center = pt, radius = 3.8.dp.toPx())
    }

    // 3. Hệ mây khí quyển phiêu dạt tự nhiên (Soft Natural Atmospheric Clouds - Chuẩn ảnh Địa Cầu Apollo/Apple)
    val cloudOffsetRad = Math.toRadians(cloudPhase.toDouble() * 0.20)
    val cloudShiftX = (cos(cloudOffsetRad) * radius * 0.06f).toFloat()

    // Tiện ích vẽ cụm mây lông vũ mềm mại với gradient tán sắc êm dịu, không nét vẽ thô
    fun drawCloudPuff(cx: Float, cy: Float, rx: Float, ry: Float, peakAlpha: Float = 0.55f) {
        val shadowOffset = radius * 0.025f
        // Bóng đổ mây mờ ảo trên mặt biển & lục địa tạo độ cao tầng mây
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x22000A18).copy(alpha = 0.22f * alpha),
                    Color(0x0E000A18).copy(alpha = 0.09f * alpha),
                    Color.Transparent
                ),
                center = Offset(cx + shadowOffset, cy + shadowOffset),
                radius = maxOf(rx, ry)
            ),
            topLeft = Offset(cx + shadowOffset - rx, cy + shadowOffset - ry),
            size = Size(rx * 2f, ry * 2f)
        )
        // Mảng mây bồng bềnh chuyển sắc mượt mà từ lõi ra rìa
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = peakAlpha * alpha),
                    Color(0xCCFFFFFF).copy(alpha = (peakAlpha * 0.65f) * alpha),
                    Color(0x44FFFFFF).copy(alpha = (peakAlpha * 0.20f) * alpha),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = maxOf(rx, ry)
            ),
            topLeft = Offset(cx - rx, cy - ry),
            size = Size(rx * 2f, ry * 2f)
        )
    }

    // A. Hệ thống mây nhiệt đới & gió mùa Đông Á / Thái Bình Dương (phủ tự nhiên qua Đông Dương, Biển Đông)
    val monsoonCenterX = centerX - radius * 0.08f + cloudShiftX
    val monsoonCenterY = centerY - radius * 0.12f
    drawCloudPuff(monsoonCenterX - radius * 0.20f, monsoonCenterY - radius * 0.15f, radius * 0.28f, radius * 0.16f, 0.48f)
    drawCloudPuff(monsoonCenterX, monsoonCenterY - radius * 0.08f, radius * 0.32f, radius * 0.18f, 0.58f)
    drawCloudPuff(monsoonCenterX + radius * 0.25f, monsoonCenterY + radius * 0.05f, radius * 0.30f, radius * 0.14f, 0.52f)
    drawCloudPuff(monsoonCenterX - radius * 0.10f, monsoonCenterY + radius * 0.10f, radius * 0.22f, radius * 0.12f, 0.42f)

    // B. Dải mây ti xoáy êm dịu vùng Tây Bắc Thái Bình Dương (Curved Wispy Cloud Veil)
    val pacificSwirl = Path().apply {
        moveTo(centerX - radius * 0.55f + cloudShiftX, centerY - radius * 0.45f)
        cubicTo(
            centerX - radius * 0.20f + cloudShiftX, centerY - radius * 0.55f,
            centerX + radius * 0.22f + cloudShiftX, centerY - radius * 0.35f,
            centerX + radius * 0.48f + cloudShiftX, centerY - radius * 0.15f
        )
        cubicTo(
            centerX + radius * 0.20f + cloudShiftX, centerY - radius * 0.22f,
            centerX - radius * 0.15f + cloudShiftX, centerY - radius * 0.38f,
            centerX - radius * 0.55f + cloudShiftX, centerY - radius * 0.45f
        )
        close()
    }
    drawPath(
        pacificSwirl,
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.35f * alpha),
                Color.White.copy(alpha = 0.50f * alpha),
                Color.Transparent
            ),
            startX = centerX - radius * 0.55f + cloudShiftX,
            endX = centerX + radius * 0.48f + cloudShiftX
        )
    )

    // C. Dải mây xích đạo ITCZ (Intertropical Convergence Zone) mềm mại, phiêu bồng
    val itczCloud = Path().apply {
        moveTo(centerX - radius * 0.65f + cloudShiftX * 0.7f, centerY + radius * 0.08f)
        cubicTo(
            centerX - radius * 0.25f + cloudShiftX * 0.7f, centerY + radius * 0.02f,
            centerX + radius * 0.10f + cloudShiftX * 0.7f, centerY + radius * 0.15f,
            centerX + radius * 0.55f + cloudShiftX * 0.7f, centerY + radius * 0.18f
        )
        cubicTo(
            centerX + radius * 0.20f + cloudShiftX * 0.7f, centerY + radius * 0.24f,
            centerX - radius * 0.15f + cloudShiftX * 0.7f, centerY + radius * 0.14f,
            centerX - radius * 0.65f + cloudShiftX * 0.7f, centerY + radius * 0.08f
        )
        close()
    }
    drawPath(
        itczCloud,
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.40f * alpha),
                Color.White.copy(alpha = 0.48f * alpha),
                Color.Transparent
            ),
            startX = centerX - radius * 0.65f,
            endX = centerX + radius * 0.55f
        )
    )

    // D. Dải mây ôn đới Nam Bán Cầu uốn lượn quanh phía nam Châu Úc (Southern Oceanic Cloud Stream)
    val southOceanStream = Path().apply {
        moveTo(centerX - radius * 0.68f + cloudShiftX * 0.5f, centerY + radius * 0.68f)
        cubicTo(
            centerX - radius * 0.25f + cloudShiftX * 0.5f, centerY + radius * 0.54f,
            centerX + radius * 0.15f + cloudShiftX * 0.5f, centerY + radius * 0.72f,
            centerX + radius * 0.65f + cloudShiftX * 0.5f, centerY + radius * 0.58f
        )
        cubicTo(
            centerX + radius * 0.35f + cloudShiftX * 0.5f, centerY + radius * 0.78f,
            centerX - radius * 0.05f + cloudShiftX * 0.5f, centerY + radius * 0.68f,
            centerX - radius * 0.68f + cloudShiftX * 0.5f, centerY + radius * 0.68f
        )
        close()
    }
    drawPath(
        southOceanStream,
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.38f * alpha),
                Color.White.copy(alpha = 0.45f * alpha),
                Color.Transparent
            ),
            startX = centerX - radius * 0.68f,
            endX = centerX + radius * 0.65f
        )
    )

    // E. Các chùm mây nhỏ rải rác ngoài khơi bờ biển Úc và Ấn Độ Dương
    drawCloudPuff(centerX + radius * 0.42f + cloudShiftX, centerY + radius * 0.40f, radius * 0.18f, radius * 0.10f, 0.42f)
    drawCloudPuff(centerX - radius * 0.45f + cloudShiftX, centerY - radius * 0.05f, radius * 0.22f, radius * 0.11f, 0.38f)
    drawCloudPuff(centerX - radius * 0.35f + cloudShiftX, centerY + radius * 0.38f, radius * 0.20f, radius * 0.09f, 0.35f)
}

// ─── Chi Tiết Hành Tinh 4: SAO MỘC (JUPITER_STORM - VẾT ĐỎ LỚN & VÂN MÂY ĐỐI LƯU) ───

private fun DrawScope.drawJupiterTurbulentBeltsAndRedSpot(
    centerX: Float,
    centerY: Float,
    radius: Float,
    cloudPhase: Float,
    alpha: Float
) {
    // 1. Các dải khí quyển đối lưu nghịch hướng của Sao Mộc (Zonal Counter-Rotating Belts & Zones)
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
        val direction = if (i % 2 == 0) 1.0 else -1.0
        val beltWavePhase = (cloudPhase * (1.2f * direction.toFloat()) + i * 40f) * (PI.toFloat() / 180f)
        val dynamicWaveAmp = (if (i % 2 == 0) 1 else -1) * radius * (0.038f + 0.022f * sin(beltWavePhase))
        val horizontalDrift = (sin(Math.toRadians(cloudPhase.toDouble() * 0.8 * direction + i * 30.0)) * radius * 0.06f).toFloat()

        // Đường vân mây uốn lượn hình sin mô phỏng sóng cuộn Kelvin-Helmholtz chuyển động
        val beltPath = Path().apply {
            moveTo(centerX - radius, yPos)
            cubicTo(
                centerX - radius * 0.5f + horizontalDrift, yPos + dynamicWaveAmp,
                centerX + horizontalDrift, yPos - dynamicWaveAmp,
                centerX + radius * 0.5f + horizontalDrift, yPos + dynamicWaveAmp
            )
            cubicTo(
                centerX + radius * 0.75f + horizontalDrift, yPos - dynamicWaveAmp,
                centerX + radius * 0.95f + horizontalDrift, yPos + dynamicWaveAmp,
                centerX + radius, yPos
            )
        }
        drawPath(
            beltPath,
            color = beltColor.copy(alpha = 0.85f * alpha),
            style = Stroke(width = radius * 0.16f, cap = StrokeCap.Round)
        )
    }

    // 2. VẾT ĐỎ LỚN (THE GREAT RED SPOT) - SIÊU BÃO XOÁY NGHỊCH CO BÓP & QUAY TRÒN
    // Nằm ở Dải xích đạo Nam (SEB), kích thước khổng lồ, hình bầu dục cuốn xoáy
    val spotDriftX = (sin(Math.toRadians(cloudPhase.toDouble() * 0.6)) * radius * 0.032f).toFloat()
    val spotDriftY = (cos(Math.toRadians(cloudPhase.toDouble() * 0.4)) * radius * 0.012f).toFloat()
    val spotX = centerX + radius * 0.22f + spotDriftX
    val spotY = centerY + radius * 0.24f + spotDriftY
    val spotPulse = sin(Math.toRadians(cloudPhase.toDouble() * 1.5)).toFloat()
    val spotWidth = radius * (0.45f + 0.03f * spotPulse)
    val spotHeight = radius * (0.28f - 0.02f * spotPulse)

    // Vệt dòng xoáy rẽ mây uốn quanh Vết Đỏ Lớn
    val deflectionWave = (cos(Math.toRadians(cloudPhase.toDouble() * 1.2)) * radius * 0.015f).toFloat()
    val deflectionPath = Path().apply {
        moveTo(spotX - spotWidth * 0.8f, spotY - spotHeight * 0.5f + deflectionWave)
        quadraticTo(spotX, spotY - spotHeight * 0.85f - deflectionWave, spotX + spotWidth * 0.8f, spotY - spotHeight * 0.5f + deflectionWave)
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

    // Vòng mây trắng cuộn xoáy trong mắt bão Vết Đỏ Lớn (Xoay ngược chiều kim đồng hồ)
    val stormRotation = (-cloudPhase * 1.6f) % 360f
    withTransform({
        rotate(degrees = stormRotation, pivot = Offset(spotX, spotY))
    }) {
        drawOval(
            color = Color(0xEEFFFFFF),
            topLeft = Offset(spotX - spotWidth * 0.26f, spotY - spotHeight * 0.26f),
            size = Size(spotWidth * 0.52f, spotHeight * 0.52f),
            style = Stroke(width = 1.6.dp.toPx()),
            alpha = alpha
        )
        // Mắt xoáy tâm bão
        drawCircle(
            color = Color(0xCCFFCCBC),
            center = Offset(spotX + spotWidth * 0.08f, spotY),
            radius = spotHeight * 0.18f,
            alpha = alpha
        )
    }

    // 3. Chuỗi ngọc bão trắng (String of Pearls) nối đuôi sau Vết Đỏ Lớn trôi dạt và lấp lánh
    val pearlDrift1 = (sin(Math.toRadians(cloudPhase.toDouble() * 0.9 + 50.0)) * radius * 0.045f).toFloat()
    val pearlDrift2 = (sin(Math.toRadians(cloudPhase.toDouble() * 0.9 + 110.0)) * radius * 0.045f).toFloat()
    val pearlGlow1 = 0.75f + 0.25f * sin(Math.toRadians(cloudPhase.toDouble() * 2.0)).toFloat()
    val pearlGlow2 = 0.70f + 0.30f * cos(Math.toRadians(cloudPhase.toDouble() * 2.2)).toFloat()

    val pearl1 = Offset(spotX - spotWidth * 0.75f + pearlDrift1, spotY + spotHeight * 0.05f)
    val pearl2 = Offset(spotX - spotWidth * 1.25f + pearlDrift2, spotY + spotHeight * 0.08f)
    drawCircle(color = Color.White.copy(alpha = pearlGlow1 * alpha), center = pearl1, radius = radius * 0.055f)
    drawCircle(color = Color.White.copy(alpha = pearlGlow2 * alpha), center = pearl2, radius = radius * 0.045f)
}

// ─── Chi Tiết Hành Tinh 3: SAO THỔ (SATURN_MONARCH - VÀNH ĐAI CASSINI 3D) ──────

private fun DrawScope.drawSaturnBeltsAndHexagon(
    centerX: Float,
    centerY: Float,
    radius: Float,
    alpha: Float
) {
    // 1. Các dải khí quyển êm dịu, uốn lượn cong theo khối cầu 3D của Sao Thổ
    // Tông màu ảnh chụp Cassini chân thực: vàng cát ấm, mật ong, caramel, nâu nhạt, và chỏm cực ôliu
    val bandData = listOf(
        Triple(-0.72f, 0.14f, Color(0x664A4436)), // Cực Bắc xám ôliu
        Triple(-0.55f, 0.13f, Color(0x44B8986E)), // Đới ôn đới bắc
        Triple(-0.38f, 0.15f, Color(0x55E8D2AF)), // Dải sáng nhiệt đới bắc
        Triple(-0.20f, 0.16f, Color(0x66A88554)), // Dải xích đạo bắc sẫm
        Triple(-0.02f, 0.18f, Color(0x44F4E4CC)), // Vùng xích đạo sáng
        Triple(0.18f, 0.16f, Color(0x55BA9665)),  // Dải xích đạo nam
        Triple(0.36f, 0.14f, Color(0x44DFCA9E)),  // Dải nhiệt đới nam
        Triple(0.54f, 0.13f, Color(0x559A774C)),  // Ôn đới nam
        Triple(0.70f, 0.16f, Color(0x66543D26))   // Cực nam nâu đậm
    )

    for ((yFrac, hFrac, color) in bandData) {
        val y = centerY + yFrac * radius
        val h = hFrac * radius
        // Dải vĩ tuyến cong tự nhiên theo độ cong khối cầu
        val bandPath = Path().apply {
            moveTo(centerX - radius, y)
            cubicTo(
                centerX - radius * 0.45f, y + radius * 0.05f,
                centerX + radius * 0.45f, y + radius * 0.05f,
                centerX + radius, y
            )
            lineTo(centerX + radius, y + h)
            cubicTo(
                centerX + radius * 0.45f, y + h + radius * 0.05f,
                centerX - radius * 0.45f, y + h + radius * 0.05f,
                centerX - radius, y + h
            )
            close()
        }
        drawPath(bandPath, color = color.copy(alpha = color.alpha * alpha))
    }

    // 2. Chỏm xoáy Cực Bắc màu ôliu êm dịu, tự nhiên (North Polar Hood - Không dùng nét vẽ đa giác thô cứng)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x664A4436),
                Color(0x334A4436),
                Color.Transparent
            ),
            center = Offset(centerX, centerY - radius * 0.82f),
            radius = radius * 0.35f
        ),
        topLeft = Offset(centerX - radius * 0.35f, centerY - radius * 0.98f),
        size = Size(radius * 0.70f, radius * 0.32f),
        alpha = alpha
    )

    // 3. Bóng đổ cong tự nhiên của vành đai vắt ngang bề mặt Sao Thổ (Ring Shadow on Globe)
    // Tái hiện chân thực ảnh chụp không gian Cassini của NASA / Apple Wallpaper
    val ringShadowPath = Path().apply {
        moveTo(centerX - radius * 0.96f, centerY - radius * 0.04f)
        cubicTo(
            centerX - radius * 0.4f, centerY + radius * 0.22f,
            centerX + radius * 0.4f, centerY + radius * 0.22f,
            centerX + radius * 0.96f, centerY - radius * 0.04f
        )
        lineTo(centerX + radius * 0.96f, centerY + radius * 0.12f)
        cubicTo(
            centerX + radius * 0.4f, centerY + radius * 0.38f,
            centerX - radius * 0.4f, centerY + radius * 0.38f,
            centerX - radius * 0.96f, centerY + radius * 0.12f
        )
        close()
    }
    drawPath(ringShadowPath, color = Color(0xDD0D0601).copy(alpha = 0.92f * alpha))
}

/**
 * Vẽ Hệ Thống Vành Đai Sao Thổ 3D Đa Tầng Lộng Lẫy (Realistic Multi-Ring System):
 * - Vành A, Rãnh tối Cassini (Cassini Division), Vành B rực rỡ, Vành C mờ
 * - Bảng màu chân thực thiên văn: Cát vàng kem, ngà ấm, nâu khói và rãnh đen thẳm
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
    val ringTiltAngle = -22f
    val ringOuterA = radius * 3.10f
    val ringOuterB = radius * 0.92f

    withTransform({
        rotate(ringTiltAngle, pivot = Offset(centerX, centerY))
    }) {
        // Cắt nửa trước hoặc nửa sau theo trục hoành
        val clipRect = if (isFront) {
            Rect(centerX - ringOuterA, centerY, centerX + ringOuterA, centerY + ringOuterB * 1.6f)
        } else {
            Rect(centerX - ringOuterA, centerY - ringOuterB * 1.6f, centerX + ringOuterA, centerY)
        }

        clipRect(clipRect.left, clipRect.top, clipRect.right, clipRect.bottom) {
            // Vành C trong cùng (Crepe Ring - nâu khói mờ ảo)
            val ringCW = ringOuterA * 0.65f
            val ringCH = ringOuterB * 0.65f
            drawOval(
                color = Color(0x444D3A27),
                topLeft = Offset(centerX - ringCW / 2f, centerY - ringCH / 2f),
                size = Size(ringCW, ringCH),
                style = Stroke(width = radius * 0.11f),
                alpha = alpha
            )

            // Vành B rực rỡ dày đặc (Dense main Ring B) - Vàng cát & ngà ấm thanh lịch (Chuẩn ảnh Apple Wallpaper)
            val ringBW = ringOuterA * 0.84f
            val ringBH = ringOuterB * 0.84f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x00D4BE98),
                        Color(0xFFCBB28C),
                        Color(0xFFE8D7B8),
                        Color(0xFFDFC49B),
                        Color(0x00A88960)
                    ),
                    center = Offset(centerX, centerY),
                    radius = ringBW / 2f
                ),
                topLeft = Offset(centerX - ringBW / 2f, centerY - ringBH / 2f),
                size = Size(ringBW, ringBH),
                style = Stroke(width = radius * 0.25f),
                alpha = alpha
            )

            // Các rãnh vi mô bên trong Vành B (Fine sub-grooves)
            for (g in 1..4) {
                val grooveW = ringBW * (0.86f + g * 0.032f)
                val grooveH = ringBH * (0.86f + g * 0.032f)
                drawOval(
                    color = Color(0x333D2A18),
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
                color = Color(0xFF030105),
                topLeft = Offset(centerX - cassiniW / 2f, centerY - cassiniH / 2f),
                size = Size(cassiniW, cassiniH),
                style = Stroke(width = radius * 0.055f),
                alpha = alpha
            )

            // Vành A ngoài (Outer Ring A - Tông taupe xám vàng thanh nhã)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x00B8A285),
                        Color(0xAAB09B7E),
                        Color(0x889E886D),
                        Color(0x006B5842)
                    ),
                    center = Offset(centerX, centerY),
                    radius = ringOuterA / 2f
                ),
                topLeft = Offset(centerX - ringOuterA / 2f, centerY - ringOuterB / 2f),
                size = Size(ringOuterA, ringOuterB),
                style = Stroke(width = radius * 0.14f),
                alpha = alpha
            )

            // Rãnh Encke (Encke Gap) trong Vành A
            val enckeW = ringOuterA * 0.96f
            val enckeH = ringOuterB * 0.96f
            drawOval(
                color = Color(0x66030105),
                topLeft = Offset(centerX - enckeW / 2f, centerY - enckeH / 2f),
                size = Size(enckeW, enckeH),
                style = Stroke(width = 0.9.dp.toPx()),
                alpha = alpha
            )

            // Vành F mảnh mai ngoài cùng (Narrow F-Ring)
            val ringFW = ringOuterA * 1.05f
            val ringFH = ringOuterB * 1.05f
            drawOval(
                color = Color(0x77C8B294),
                topLeft = Offset(centerX - ringFW / 2f, centerY - ringFH / 2f),
                size = Size(ringFW, ringFH),
                style = Stroke(width = 1.3.dp.toPx()),
                alpha = alpha
            )

            // CHUYỂN ĐỘNG THẬT CỦA VÀNH ĐAI: CÁC TIỂU HÀNH TINH & HẠT BỤI BĂNG QUAY THEO QUỸ ĐẠO KEPLER
            val particleCount = 40
            for (p in 0 until particleCount) {
                // Phân bổ hạt bụi trên các dải quỹ đạo: Vành C, Vành B trong, Vành B ngoài, Vành A
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
                    (baseSparkle * 0.92f).coerceIn(0.2f, 1f) * alpha
                } else {
                    (baseSparkle * 0.55f).coerceIn(0.1f, 0.65f) * alpha
                }

                val particleColor = when (p % 3) {
                    0 -> Color(0xFFF7EFE2) // Tinh thể băng champagne
                    1 -> Color(0xFFDFCBB0) // Bụi silicat vàng cát
                    else -> Color(0xFFFFFFFF) // Ánh sao lấp lánh chói sáng
                }

                val pRadius = when {
                    p % 6 == 0 -> 1.6.dp.toPx() // Tiểu hành tinh nhỏ
                    p % 3 == 0 -> 1.1.dp.toPx()
                    else -> 0.7.dp.toPx()       // Hạt bụi băng vi mô
                }

                drawCircle(
                    color = particleColor.copy(alpha = particleAlpha),
                    radius = pRadius,
                    center = Offset(px, py)
                )

                // Hào quang lấp lánh cho hạt lớn
                if (p % 6 == 0 && isFrontSide) {
                    drawCircle(
                        color = Color.White.copy(alpha = particleAlpha * 0.45f),
                        radius = pRadius * 1.8f,
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}

// ─── Chi Tiết Hành Tinh 6: SAO THIÊN VƯƠNG (URANUS_ICE) ─────────────────────

private fun DrawScope.drawUranusIceAtmosphere(
    centerX: Float,
    centerY: Float,
    radius: Float,
    cloudPhase: Float,
    alpha: Float
) {
    // 1. Các dải khí quyển zonal song song êm dịu (Subtle Parallel Ice Bands)
    // Sao Thiên Vương có khí quyển băng methane mượt mà, các dải mây màu ngọc bích chuyển sắc tinh tế
    val bandColors = listOf(
        Color(0x334DD0E1), // Xanh ngọc pastel
        Color(0x2280DEEA), // Xanh băng dịu
        Color(0x3800ACC1), // Cyan đậm
        Color(0x2800838F), // Cyan sâu
        Color(0x3026C6DA)  // Ngọc lam sáng
    )

    for (i in -4..4) {
        val yPos = centerY + (i * radius * 0.19f)
        val bandWavePhase = (cloudPhase * (if (i % 2 == 0) 0.8f else -0.8f) + i * 36f) * (PI.toFloat() / 180f)
        val dynamicAmp = (if (i % 2 == 0) 1 else -1) * radius * (0.018f + 0.012f * sin(bandWavePhase))
        val bandShift = (sin(Math.toRadians(cloudPhase.toDouble() * 0.5 + i * 25.0)) * radius * 0.035f).toFloat()

        val bandPath = Path().apply {
            moveTo(centerX - radius, yPos)
            cubicTo(
                centerX - radius * 0.4f + bandShift, yPos + dynamicAmp,
                centerX + radius * 0.4f + bandShift, yPos - dynamicAmp,
                centerX + radius, yPos
            )
        }
        val bandColor = bandColors[(i + 4) % bandColors.size]
        drawPath(
            bandPath,
            color = bandColor.copy(alpha = bandColor.alpha * alpha),
            style = Stroke(width = radius * 0.14f, cap = StrokeCap.Round)
        )
    }

    // 2. Chỏm Cực Bắc phát sáng rực rỡ đặc trưng (Luminous North Polar Cap/Hood) với nhịp thở cực quang
    val polarPulse = sin(Math.toRadians(cloudPhase.toDouble() * 1.5)).toFloat()
    val polarCenterY = centerY - radius * 0.82f
    val polarCapRadius = radius * (0.48f + 0.04f * polarPulse)
    val polarAlpha = (0.85f + 0.15f * polarPulse) * alpha

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = polarAlpha),
                Color(0xCCE0F7FA).copy(alpha = polarAlpha * 0.85f),
                Color(0x6680DEEA).copy(alpha = polarAlpha * 0.45f),
                Color.Transparent
            ),
            center = Offset(centerX, polarCenterY),
            radius = polarCapRadius
        ),
        topLeft = Offset(centerX - polarCapRadius * 0.94f, polarCenterY - polarCapRadius * 0.35f),
        size = Size(polarCapRadius * 1.88f, polarCapRadius * 0.80f)
    )

    // Vầng hào quang cực quang ngọc bích tỏa ra co giãn mềm mại
    val auroraRadius = radius * (0.65f + 0.08f * cos(Math.toRadians(cloudPhase.toDouble() * 1.2)).toFloat())
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x66B2EBF2).copy(alpha = (0.50f + 0.15f * polarPulse) * alpha),
                Color.Transparent
            ),
            center = Offset(centerX, polarCenterY),
            radius = auroraRadius
        ),
        topLeft = Offset(centerX - auroraRadius * 0.92f, polarCenterY - auroraRadius * 0.38f),
        size = Size(auroraRadius * 1.84f, auroraRadius * 0.76f)
    )

    // 3. Vệt mây băng Methane tầng cao thanh mảnh (Methane Ice Cirrus Streaks) lướt êm ái
    val streak1Shift = (sin(Math.toRadians(cloudPhase.toDouble() * 0.70)) * radius * 0.08f).toFloat()
    val streak2Shift = (-cos(Math.toRadians(cloudPhase.toDouble() * 0.55)) * radius * 0.07f).toFloat()
    val streak3Shift = (sin(Math.toRadians(cloudPhase.toDouble() * 0.45 + 45.0)) * radius * 0.06f).toFloat()

    // Vệt mây xích đạo mỏng manh
    val streak1 = Path().apply {
        moveTo(centerX - radius * 0.60f + streak1Shift, centerY - radius * 0.08f)
        cubicTo(
            centerX - radius * 0.20f + streak1Shift, centerY - radius * 0.12f,
            centerX + radius * 0.20f + streak1Shift, centerY - radius * 0.04f,
            centerX + radius * 0.55f + streak1Shift, centerY - radius * 0.10f
        )
    }
    drawPath(
        streak1,
        color = Color.White.copy(alpha = 0.50f * alpha),
        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Vệt mây cận cực bắc
    val streak2 = Path().apply {
        moveTo(centerX - radius * 0.40f + streak2Shift, centerY - radius * 0.42f)
        cubicTo(
            centerX - radius * 0.10f + streak2Shift, centerY - radius * 0.46f,
            centerX + radius * 0.18f + streak2Shift, centerY - radius * 0.38f,
            centerX + radius * 0.45f + streak2Shift, centerY - radius * 0.44f
        )
    }
    drawPath(
        streak2,
        color = Color(0xEEFFFFFF).copy(alpha = 0.45f * alpha),
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    )

    // Vệt mây ôn đới nam
    val streak3 = Path().apply {
        moveTo(centerX - radius * 0.50f + streak3Shift, centerY + radius * 0.28f)
        cubicTo(
            centerX - radius * 0.15f + streak3Shift, centerY + radius * 0.24f,
            centerX + radius * 0.15f + streak3Shift, centerY + radius * 0.32f,
            centerX + radius * 0.48f + streak3Shift, centerY + radius * 0.26f
        )
    }
    drawPath(
        streak3,
        color = Color(0xBBFFFFFF).copy(alpha = 0.38f * alpha),
        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
    )

    // 4. Các tinh thể băng Methane lấp lánh (Methane Diamond Ice Crystal Twinkles)
    val iceGlintPoints = listOf(
        Offset(centerX - radius * 0.25f, centerY - radius * 0.22f),
        Offset(centerX + radius * 0.20f, centerY + radius * 0.15f),
        Offset(centerX - radius * 0.10f, centerY + radius * 0.38f),
        Offset(centerX + radius * 0.35f, centerY - radius * 0.30f)
    )
    for ((idx, pt) in iceGlintPoints.withIndex()) {
        val glintAlpha = (0.20f + 0.60f * sin(Math.toRadians(cloudPhase.toDouble() * 2.8 + idx * 75.0)).toFloat()).coerceIn(0f, 1f) * alpha
        drawCircle(
            color = Color(0xFFE0F7FA).copy(alpha = glintAlpha),
            center = pt,
            radius = 1.3.dp.toPx()
        )
    }
}

// ─── Chi Tiết Hành Tinh 7: SAO HẢI VƯƠNG (NEPTUNE_STORM) ────────────────────

private fun DrawScope.drawNeptuneStormBands(
    centerX: Float,
    centerY: Float,
    radius: Float,
    cloudPhase: Float,
    alpha: Float
) {
    // 1. Các dải khí quyển zonal đối lưu sâu thẳm (Deep Azure Zonal Belts - Chuẩn ảnh 2)
    val neptuneBands = listOf(
        Color(0x3860A5FA), // Xanh lam sáng tầng cao
        Color(0x401E40AF), // Xanh coban đậm
        Color(0x303B82F6), // Lam ngọc trung tính
        Color(0x50172554), // Xanh chàm thẳm
        Color(0x352563EB)  // Xanh sapphire rực rỡ
    )

    for (i in -4..4) {
        val yPos = centerY + (i * radius * 0.20f)
        val jetPhase = (cloudPhase * (if (i % 2 == 0) 1.5f else -1.5f) + i * 45f) * (PI.toFloat() / 180f)
        val dynamicAmp = (if (i % 2 == 0) 1 else -1) * radius * (0.032f + 0.016f * sin(jetPhase))
        val jetDrift = (sin(Math.toRadians(cloudPhase.toDouble() * 0.9 * (if (i % 2 == 0) 1.0 else -1.0) + i * 35.0)) * radius * 0.045f).toFloat()

        val bandPath = Path().apply {
            moveTo(centerX - radius, yPos)
            cubicTo(
                centerX - radius * 0.4f + jetDrift, yPos + dynamicAmp,
                centerX + radius * 0.4f + jetDrift, yPos - dynamicAmp,
                centerX + radius, yPos
            )
        }
        val bandColor = neptuneBands[(i + 4) % neptuneBands.size]
        drawPath(
            bandPath,
            color = bandColor.copy(alpha = bandColor.alpha * alpha),
            style = Stroke(width = radius * 0.15f, cap = StrokeCap.Round)
        )
    }

    // 2. VẾT TỐI LỚN (The Great Dark Spot / GDS - Siêu bão xoáy bán cầu nam co bóp và xoay)
    val gdsOscillation = sin(Math.toRadians(cloudPhase.toDouble() * 1.2)).toFloat()
    val spotX = centerX + radius * 0.32f + (gdsOscillation * radius * 0.04f)
    val spotY = centerY + radius * 0.38f + (cos(Math.toRadians(cloudPhase.toDouble() * 0.8)).toFloat() * radius * 0.015f)
    val spotW = radius * (0.44f + 0.04f * gdsOscillation)
    val spotH = radius * (0.24f - 0.02f * gdsOscillation)

    // Vùng nhiễu loạn đối lưu bên ngoài Vết Tối
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x550B132B).copy(alpha = 0.65f * alpha),
                Color(0x221D4ED8).copy(alpha = 0.35f * alpha),
                Color.Transparent
            ),
            center = Offset(spotX, spotY),
            radius = spotW * 0.85f
        ),
        topLeft = Offset(spotX - spotW * 0.65f, spotY - spotH * 0.70f),
        size = Size(spotW * 1.30f, spotH * 1.40f),
        alpha = alpha
    )

    // Lõi bão Vết Tối Lớn màu xanh đen chàm hun hút xoay theo dòng lốc xoáy
    val darkVortexAngle = (-cloudPhase * 1.4f) % 360f
    withTransform({
        rotate(darkVortexAngle, Offset(spotX, spotY))
    }) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xF5060D1A).copy(alpha = 0.95f * alpha),
                    Color(0xDD0D1B2A).copy(alpha = 0.88f * alpha),
                    Color(0x881E3A8A).copy(alpha = 0.60f * alpha),
                    Color.Transparent
                ),
                center = Offset(spotX, spotY),
                radius = spotW * 0.55f
            ),
            topLeft = Offset(spotX - spotW / 2f, spotY - spotH / 2f),
            size = Size(spotW, spotH),
            alpha = alpha
        )

        // Dải mây xoáy sẫm bên trong lõi bão
        drawArc(
            color = Color(0x66000000).copy(alpha = 0.8f * alpha),
            startAngle = 30f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(spotX - spotW * 0.35f, spotY - spotH * 0.35f),
            size = Size(spotW * 0.70f, spotH * 0.70f),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }

    // 3. MÂY BĂNG METHANE "SCOOTER" SIÊU SÁNG & SIÊU TỐC (> 2.100 km/h)
    // Mây Scooter vắt ngang luồng gió xích đạo lướt nhanh vun vút
    val scooterAngle = Math.toRadians(cloudPhase.toDouble() * 2.2)
    val scooterShiftX = (sin(scooterAngle) * radius * 0.15f).toFloat()
    val scooterWaveY = (cos(scooterAngle * 1.8) * radius * 0.025f).toFloat()

    val equatorialScooter = Path().apply {
        moveTo(centerX - radius * 0.62f + scooterShiftX, centerY - radius * 0.05f + scooterWaveY)
        cubicTo(
            centerX - radius * 0.25f + scooterShiftX, centerY - radius * 0.10f - scooterWaveY,
            centerX + radius * 0.15f + scooterShiftX, centerY + radius * 0.02f + scooterWaveY,
            centerX + radius * 0.58f + scooterShiftX, centerY - radius * 0.06f - scooterWaveY
        )
    }
    drawPath(
        equatorialScooter,
        color = Color(0xEEFFFFFF).copy(alpha = 0.85f * alpha),
        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Mây viền đồng hành phía trên Vết Tối Lớn (Companion Cirrus Cloud) phập phồng theo gió bão
    val companionPuff = (0.85f + 0.15f * sin(Math.toRadians(cloudPhase.toDouble() * 1.8)).toFloat()) * alpha
    val companionShiftY = (sin(Math.toRadians(cloudPhase.toDouble() * 2.0)) * radius * 0.015f).toFloat()
    val companionCloud = Path().apply {
        moveTo(spotX - spotW * 0.45f, spotY - spotH * 0.30f + companionShiftY)
        quadraticTo(
            spotX, spotY - spotH * 0.65f - companionShiftY,
            spotX + spotW * 0.50f, spotY - spotH * 0.20f + companionShiftY
        )
    }
    drawPath(
        companionCloud,
        color = Color.White.copy(alpha = companionPuff),
        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
    )

    // Vệt mây phụ ở cực nam lướt nhanh
    val southShift = (-cos(Math.toRadians(cloudPhase.toDouble() * 1.6)) * radius * 0.08f).toFloat()
    val southStreak = Path().apply {
        moveTo(spotX - radius * 0.20f + southShift, spotY + spotH * 0.45f)
        quadraticTo(
            spotX + radius * 0.05f + southShift, spotY + spotH * 0.60f,
            spotX + radius * 0.28f + southShift, spotY + spotH * 0.38f
        )
    }
    drawPath(
        southStreak,
        color = Color(0xDDFFFFFF).copy(alpha = 0.70f * alpha),
        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
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

    // 3b. SIÊU LÓA SÁNG MẶT TRỜI & VỤ PHÓNG KHỐI LƯỢNG NHẬT HOA (GIANT X-CLASS FLARE / CME - Vị trí 3h-4h mép phải cực kỳ rực rỡ như ảnh 3)
    val flareAngleDeg = 15f
    val flareRad = Math.toRadians(flareAngleDeg.toDouble())
    val flareCenter = Offset(
        centerX + cos(flareRad).toFloat() * (radius * 0.94f),
        centerY + sin(flareRad).toFloat() * (radius * 0.94f)
    )

    // A. Quầng năng lượng plasma bùng nổ tỏa rộng ra vũ trụ
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color(0xFFFF9800).copy(alpha = 0.55f * alphaFactor),
            0.45f to Color(0xFFFF5722).copy(alpha = 0.35f * alphaFactor),
            0.85f to Color(0xFFD84315).copy(alpha = 0.15f * alphaFactor),
            1.0f to Color.Transparent,
            center = flareCenter,
            radius = radius * 0.70f
        ),
        center = flareCenter,
        radius = radius * 0.70f
    )

    // B. Chùm tia từ trường nan hoa phóng ra ngoài mép đĩa (Flare Ejection Streamers)
    val flareRayCount = 10
    for (r in 0 until flareRayCount) {
        val angleOffset = ((r - 4.5f) * 7.5f) * (PI.toFloat() / 180f)
        val beamAngle = flareRad.toFloat() + angleOffset
        val beamLength = radius * (1.30f + 0.35f * sin(r * 1.2f + cloudPhase * 0.08f).absoluteValue)
        val endX = centerX + cos(beamAngle) * beamLength
        val endY = centerY + sin(beamAngle) * beamLength
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f * alphaFactor),
                    Color(0xFFFFD54F).copy(alpha = 0.70f * alphaFactor),
                    Color(0xFFFF5722).copy(alpha = 0.25f * alphaFactor),
                    Color.Transparent
                ),
                start = flareCenter,
                end = Offset(endX, endY)
            ),
            start = flareCenter,
            end = Offset(endX, endY),
            strokeWidth = (3.5f - (r % 3) * 0.8f).dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // C. Cung từ trường lửa bọc quanh tâm lóa sáng
    val flareLoop = Path().apply {
        moveTo(flareCenter.x - radius * 0.15f, flareCenter.y - radius * 0.22f)
        cubicTo(
            flareCenter.x + radius * 0.35f, flareCenter.y - radius * 0.28f,
            flareCenter.x + radius * 0.40f, flareCenter.y + radius * 0.28f,
            flareCenter.x - radius * 0.15f, flareCenter.y + radius * 0.22f
        )
    }
    drawPath(
        flareLoop,
        color = Color(0xFFFFAB00).copy(alpha = 0.80f * alphaFactor),
        style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
    )
    drawPath(
        flareLoop,
        color = Color(0xFFFFF9C4).copy(alpha = 0.95f * alphaFactor),
        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
    )

    // D. Lõi phát nổ siêu nhiệt hạch trắng chói lòa
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 1.0f * alphaFactor),
                Color(0xFFFFF9C4).copy(alpha = 0.92f * alphaFactor),
                Color(0xFFFFD54F).copy(alpha = 0.65f * alphaFactor),
                Color.Transparent
            ),
            center = flareCenter,
            radius = radius * 0.26f
        ),
        center = flareCenter,
        radius = radius * 0.26f
    )

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
        // Hạt plasma đối lưu sôi sục & các điểm hoạt động từ trường cực nóng phát sáng (Solar Granulation & Active Magnetic Regions)
        drawSolarGranulation(centerX, centerY, radius, cloudPhase, alphaFactor)
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

    // Các điểm hoạt động từ trường cực nóng lốm đốm phát sáng rực rỡ (Active Magnetic Heating Regions - Chuẩn ảnh 3 NASA SDO)
    val activeHotspots = listOf(
        Offset(centerX + radius * 0.55f, centerY + radius * 0.18f) to 0.16f,
        Offset(centerX + radius * 0.22f, centerY - radius * 0.22f) to 0.13f,
        Offset(centerX - radius * 0.15f, centerY + radius * 0.28f) to 0.12f,
        Offset(centerX - radius * 0.38f, centerY - radius * 0.10f) to 0.14f,
        Offset(centerX + radius * 0.05f, centerY + radius * 0.42f) to 0.11f,
        Offset(centerX + radius * 0.32f, centerY - radius * 0.40f) to 0.12f,
        Offset(centerX - radius * 0.45f, centerY + radius * 0.15f) to 0.13f
    )

    for ((spot, rFactor) in activeHotspots) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.90f * alpha),
                    Color(0xFFFFF59D).copy(alpha = 0.70f * alpha),
                    Color(0xFFFFB300).copy(alpha = 0.40f * alpha),
                    Color.Transparent
                ),
                center = spot,
                radius = radius * rFactor
            ),
            center = spot,
            radius = radius * rFactor
        )
    }
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

