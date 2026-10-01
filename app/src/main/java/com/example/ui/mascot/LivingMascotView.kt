package com.example.ui.mascot

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.MascotEmotion

@Composable
fun LivingMascotView(
    emotion: MascotEmotion,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    avatarRes: Int = R.drawable.companion_mascot,
    onClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    // 1. Nhịp thở tự nhiên (Idle Breathing)
    val infiniteTransition = rememberInfiniteTransition(label = "LivingMascotInfinite")
    val idleBreathY by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdleBreath"
    )

    // 2. Chuyển động vật lý nén & giãn (Squash & Stretch)
    var isSquashing by remember { mutableStateOf(false) }

    LaunchedEffect(emotion) {
        if (emotion == MascotEmotion.CHEER || emotion == MascotEmotion.PETTED || emotion == MascotEmotion.STREAK) {
            isSquashing = true
            kotlinx.coroutines.delay(120)
            isSquashing = false
        }
    }

    val targetScaleX = when (emotion) {
        MascotEmotion.CHEER -> if (isSquashing) 1.25f else 1.05f
        MascotEmotion.STREAK -> if (isSquashing) 1.2f else 1.08f
        MascotEmotion.EMPATHY -> 1.05f
        MascotEmotion.THINKING -> 0.98f
        MascotEmotion.PETTED -> if (isSquashing) 1.22f else 1.05f
        MascotEmotion.LISTENING -> 1.02f
        MascotEmotion.IDLE -> 1.0f
    }

    val targetScaleY = when (emotion) {
        MascotEmotion.CHEER -> if (isSquashing) 0.80f else 1.15f
        MascotEmotion.STREAK -> if (isSquashing) 0.85f else 1.12f
        MascotEmotion.EMPATHY -> 0.92f
        MascotEmotion.THINKING -> 1.0f
        MascotEmotion.PETTED -> if (isSquashing) 0.82f else 1.12f
        MascotEmotion.LISTENING -> 1.02f
        MascotEmotion.IDLE -> idleBreathY
    }

    val animScaleX by animateFloatAsState(
        targetValue = targetScaleX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "SquashX"
    )

    val animScaleY by animateFloatAsState(
        targetValue = targetScaleY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "StretchY"
    )

    // 3. Góc xoay cơ thể (Tilt & Wiggle)
    val targetRotation = when (emotion) {
        MascotEmotion.THINKING -> 12f
        MascotEmotion.CHEER -> -8f
        MascotEmotion.EMPATHY -> -6f
        MascotEmotion.PETTED -> 10f
        MascotEmotion.LISTENING -> 4f
        else -> 0f
    }

    val animRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "MascotRotation"
    )

    // 4. Hào quang tỏa sáng (Aura Glow Colors)
    val auraBrush = remember(emotion) {
        when (emotion) {
            MascotEmotion.STREAK -> Brush.radialGradient(
                listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFFFD600).copy(alpha = 0.2f))
            )
            MascotEmotion.CHEER -> Brush.radialGradient(
                listOf(Color(0xFFFFD700), Color(0xFF51FAC1), Color.Transparent)
            )
            MascotEmotion.PETTED -> Brush.radialGradient(
                listOf(Color(0xFFFF4081), Color(0xFFFF80AB), Color.Transparent)
            )
            MascotEmotion.LISTENING -> Brush.radialGradient(
                listOf(Color(0xFF00E5FF), Color(0xFF00B0FF), Color.Transparent)
            )
            MascotEmotion.EMPATHY -> Brush.radialGradient(
                listOf(Color(0xFF64B5F6), Color(0xFF81C784), Color.Transparent)
            )
            MascotEmotion.THINKING -> Brush.radialGradient(
                listOf(Color(0xFFFFEE58), Color(0xFFFFCA28), Color.Transparent)
            )
            MascotEmotion.IDLE -> Brush.radialGradient(
                listOf(Color(0xFF51FAC1).copy(alpha = 0.6f), Color(0xFF006C4F).copy(alpha = 0.2f), Color.Transparent)
            )
        }
    }

    // 5. Emote Badge biểu tượng
    val emoteBadge = when (emotion) {
        MascotEmotion.CHEER -> "⭐"
        MascotEmotion.STREAK -> "🔥"
        MascotEmotion.EMPATHY -> "💖"
        MascotEmotion.THINKING -> "💡"
        MascotEmotion.LISTENING -> "🎧"
        MascotEmotion.PETTED -> "🥰"
        MascotEmotion.IDLE -> "🐾"
    }

    val emoteBadgeBg = when (emotion) {
        MascotEmotion.STREAK -> Color(0xFFFF5722)
        MascotEmotion.CHEER -> Color(0xFFFFB300)
        MascotEmotion.PETTED -> Color(0xFFE91E63)
        MascotEmotion.LISTENING -> Color(0xFF00B0FF)
        MascotEmotion.THINKING -> Color(0xFFFFC107)
        MascotEmotion.EMPATHY -> Color(0xFF4CAF50)
        MascotEmotion.IDLE -> Color(0xFF27E0A9)
    }

    Box(
        modifier = modifier
            .size(size + 36.dp) // Dành không gian cho Aura và Particles
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Lớp hiệu ứng hạt Canvas (Mascot Particle Canvas)
        MascotParticleCanvas(
            emotion = emotion,
            modifier = Modifier.fillMaxSize()
        )

        // Hào quang tỏa sáng phát quang (Aura Glow)
        Box(
            modifier = Modifier
                .size(size + 14.dp)
                .scale(if (emotion == MascotEmotion.STREAK) 1.25f else 1.12f)
                .background(auraBrush, CircleShape)
                .blur(10.dp)
        )

        // Khung hình linh vật Kiki với biến dạng Squash & Stretch
        Box(
            modifier = Modifier
                .size(size)
                .scale(scaleX = animScaleX, scaleY = animScaleY)
                .rotate(animRotation),
            contentAlignment = Alignment.Center
        ) {
            // Ảnh đại diện Kiki
            AsyncImage(
                model = avatarRes,
                contentDescription = "Living Kiki Mascot",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = when (emotion) {
                            MascotEmotion.STREAK -> Color(0xFFFFD600)
                            MascotEmotion.CHEER -> Color(0xFF51FAC1)
                            MascotEmotion.PETTED -> Color(0xFFFF80AB)
                            MascotEmotion.LISTENING -> Color(0xFF00E5FF)
                            else -> Color(0x55FFFFFF)
                        },
                        shape = CircleShape
                    )
            )

            // Emote Badge nổi bật ở góc phải trên (Floating Emote Badge)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-2).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(emoteBadgeBg)
                    .border(1.5.dp, Color(0xFF0A0A12), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoteBadge,
                    fontSize = 11.sp,
                    lineHeight = 11.sp
                )
            }
        }
    }
}
