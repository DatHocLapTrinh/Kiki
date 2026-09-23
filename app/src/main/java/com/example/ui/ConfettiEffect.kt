package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val sizeWidth: Float,
    val sizeHeight: Float,
    val color: Color,
    var rotation: Float,
    val vRotation: Float
)

@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    durationMs: Int = 3200,
    onDismiss: () -> Unit = {},
    onFinished: () -> Unit = onDismiss
) {
    if (!visible) return

    val progress = remember { Animatable(0f) }

    val colors = listOf(
        Color(0xFFFFD166), // Gold
        Color(0xFF51FAC1), // Emerald Teal
        Color(0xFFFF5252), // Coral Red
        Color(0xFF2DD4BF), // Neon Cyan
        Color(0xFFC084FC), // Lavender Violet
        Color(0xFFFB923C), // Bright Amber
        Color(0xFF38BDF8)  // Sky Blue
    )

    val particles = remember {
        List(85) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = Random.nextFloat() * 14f + 6f
            ConfettiParticle(
                x = 0.5f,
                y = 0.35f,
                vx = kotlin.math.cos(angle) * speed,
                vy = kotlin.math.sin(angle) * speed - 12f,
                sizeWidth = Random.nextFloat() * 12f + 8f,
                sizeHeight = Random.nextFloat() * 8f + 5f,
                color = colors.random(),
                rotation = Random.nextFloat() * 360f,
                vRotation = (Random.nextFloat() - 0.5f) * 22f
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
        )
        onFinished()
    }

    val currentProgress = progress.value
    val alpha = if (currentProgress > 0.7f) (1f - (currentProgress - 0.7f) / 0.3f).coerceIn(0f, 1f) else 1f

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val t = currentProgress * 2.8f
            // Vật lý bay tỏa & rơi tự do
            val curX = (p.x * w) + (p.vx * t * 38f)
            val curY = (p.y * h) + (p.vy * t * 38f) + (0.5f * 9.8f * t * t * 65f)
            val curRot = p.rotation + p.vRotation * t * 30f

            if (curX in -50f..(w + 50f) && curY in -50f..(h + 50f)) {
                rotate(degrees = curRot, pivot = Offset(curX, curY)) {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(curX - p.sizeWidth / 2f, curY - p.sizeHeight / 2f),
                        size = Size(p.sizeWidth, p.sizeHeight)
                    )
                }
            }
        }
    }
}
