package com.example.ui.mascot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.model.MascotEmotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ParticleType {
    STAR,
    FIRE,
    HEART,
    SPARKLE
}

class MascotParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var alpha: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    val color: Color,
    val type: ParticleType,
    var life: Float = 1.0f,
    val decay: Float = 0.025f
)

@Composable
fun MascotParticleCanvas(
    emotion: MascotEmotion,
    modifier: Modifier = Modifier
) {
    val particles = remember { mutableStateListOf<MascotParticle>() }

    // Sinh hạt khi thay đổi trạng thái cảm xúc
    LaunchedEffect(emotion) {
        when (emotion) {
            MascotEmotion.CHEER -> {
                // Tung 15 hạt sao lấp lánh màu vàng Gold & lục bảo
                repeat(16) {
                    val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                    val speed = Random.nextFloat() * 7f + 3f
                    particles.add(
                        MascotParticle(
                            x = 0.5f,
                            y = 0.5f,
                            vx = cos(angle) * speed,
                            vy = sin(angle) * speed - 4f, // Đẩy lên trên
                            size = Random.nextFloat() * 10f + 8f,
                            alpha = 1.0f,
                            rotation = Random.nextFloat() * 360f,
                            rotationSpeed = Random.nextFloat() * 8f - 4f,
                            color = if (Random.nextBoolean()) Color(0xFFFFD700) else Color(0xFF51FAC1),
                            type = ParticleType.STAR,
                            decay = Random.nextFloat() * 0.02f + 0.02f
                        )
                    )
                }
            }

            MascotEmotion.STREAK -> {
                // Bốc 24 hạt lửa cuộn trào
                repeat(24) {
                    val colors = listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFFFEA00))
                    particles.add(
                        MascotParticle(
                            x = Random.nextFloat() * 0.6f + 0.2f,
                            y = 0.85f,
                            vx = (Random.nextFloat() - 0.5f) * 3f,
                            vy = -(Random.nextFloat() * 7f + 5f), // Bay vọt lên trời
                            size = Random.nextFloat() * 12f + 6f,
                            alpha = 1.0f,
                            rotation = Random.nextFloat() * 360f,
                            rotationSpeed = Random.nextFloat() * 10f - 5f,
                            color = colors.random(),
                            type = ParticleType.FIRE,
                            decay = Random.nextFloat() * 0.025f + 0.02f
                        )
                    )
                }
            }

            MascotEmotion.PETTED -> {
                // Tung 12 trái tim ngọt ngào
                repeat(12) {
                    particles.add(
                        MascotParticle(
                            x = Random.nextFloat() * 0.5f + 0.25f,
                            y = 0.6f,
                            vx = (Random.nextFloat() - 0.5f) * 3.5f,
                            vy = -(Random.nextFloat() * 5f + 3f),
                            size = Random.nextFloat() * 12f + 10f,
                            alpha = 1.0f,
                            rotation = Random.nextFloat() * 40f - 20f,
                            rotationSpeed = Random.nextFloat() * 2f - 1f,
                            color = if (Random.nextBoolean()) Color(0xFFFF4081) else Color(0xFFFF80AB),
                            type = ParticleType.HEART,
                            decay = Random.nextFloat() * 0.02f + 0.015f
                        )
                    )
                }
            }

            MascotEmotion.THINKING -> {
                // 6 tia sáng ý tưởng nhỏ lấp lánh
                repeat(6) {
                    particles.add(
                        MascotParticle(
                            x = Random.nextFloat() * 0.3f + 0.5f,
                            y = 0.2f,
                            vx = (Random.nextFloat() - 0.5f) * 2f,
                            vy = -(Random.nextFloat() * 2f + 1f),
                            size = Random.nextFloat() * 6f + 4f,
                            alpha = 0.9f,
                            rotation = 0f,
                            rotationSpeed = 3f,
                            color = Color(0xFFFFEB3B),
                            type = ParticleType.SPARKLE,
                            decay = 0.03f
                        )
                    )
                }
            }

            else -> {
                // Các trạng thái khác tự tiêu biến các hạt còn sót lại
            }
        }
    }

    // Vòng lặp vật lý (Physics update loop)
    var frameTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(particles.size) {
        while (isActive && particles.isNotEmpty()) {
            delay(16) // ~60fps
            val iterator = particles.iterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.x += p.vx * 0.002f
                p.y += p.vy * 0.002f
                p.rotation += p.rotationSpeed
                p.life -= p.decay
                p.alpha = (p.life).coerceIn(0f, 1f)

                if (p.type == ParticleType.FIRE) {
                    p.vy -= 0.1f // Gia tốc tăng dần hướng lên
                } else if (p.type == ParticleType.STAR) {
                    p.vy += 0.15f // Trọng lực kéo xuống nhẹ
                }

                if (p.life <= 0f) {
                    iterator.remove()
                }
            }
            frameTrigger++
        }
    }

    if (particles.isNotEmpty()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            // Đọc frameTrigger để trigger recompose mỗi frame
            @Suppress("UNUSED_VARIABLE")
            val frame = frameTrigger
            val w = size.width
            val h = size.height

            particles.forEach { p ->
                val px = p.x * w
                val py = p.y * h

                when (p.type) {
                    ParticleType.STAR -> {
                        rotate(p.rotation, pivot = Offset(px, py)) {
                            drawStar(center = Offset(px, py), size = p.size, color = p.color.copy(alpha = p.alpha))
                        }
                    }
                    ParticleType.FIRE -> {
                        rotate(p.rotation, pivot = Offset(px, py)) {
                            drawCircle(
                                color = p.color.copy(alpha = p.alpha),
                                radius = p.size * (0.5f + p.life * 0.5f),
                                center = Offset(px, py)
                            )
                        }
                    }
                    ParticleType.HEART -> {
                        rotate(p.rotation, pivot = Offset(px, py)) {
                            drawHeart(center = Offset(px, py), size = p.size, color = p.color.copy(alpha = p.alpha))
                        }
                    }
                    ParticleType.SPARKLE -> {
                        drawCircle(
                            color = p.color.copy(alpha = p.alpha),
                            radius = p.size * p.life,
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawStar(center: Offset, size: Float, color: Color) {
    val path = Path()
    val outerRadius = size
    val innerRadius = size * 0.45f
    val numPoints = 5

    for (i in 0 until numPoints * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = (i * Math.PI / numPoints - Math.PI / 2).toFloat()
        val x = center.x + cos(angle) * radius
        val y = center.y + sin(angle) * radius
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color)
}

private fun DrawScope.drawHeart(center: Offset, size: Float, color: Color) {
    val path = Path()
    val s = size * 0.6f
    path.moveTo(center.x, center.y + s * 0.8f)
    path.cubicTo(
        center.x - s * 1.4f, center.y,
        center.x - s * 1.4f, center.y - s * 1.2f,
        center.x, center.y - s * 0.4f
    )
    path.cubicTo(
        center.x + s * 1.4f, center.y - s * 1.2f,
        center.x + s * 1.4f, center.y,
        center.x, center.y + s * 0.8f
    )
    path.close()
    drawPath(path = path, color = color)
}
