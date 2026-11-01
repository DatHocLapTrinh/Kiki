package com.example.ui.mascot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.KikiAnimationUrls
import com.example.model.MascotEmotion

/**
 * LivingMascotView: Khung hiển thị linh vật Kiki sống động.
 * Kích thước cố định 100%, không giật phóng to, không xoay lắc tròn.
 * Bong bóng đứng im hoàn toàn, chuyển động mượt mà bằng hoạt ảnh GIF.
 */
@Composable
fun LivingMascotView(
    emotion: MascotEmotion,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isFullBody: Boolean = false,
    avatarRes: Int = R.drawable.companion_mascot,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Hào quang tỏa sáng dịu nhẹ (Aura Glow cố định, đứng im)
    val auraBrush = remember(emotion) {
        when (emotion) {
            MascotEmotion.GREETING -> Brush.radialGradient(
                listOf(Color(0xFFFFD166).copy(alpha = 0.5f), Color(0xFF00F5D4).copy(alpha = 0.25f), Color.Transparent)
            )
            MascotEmotion.STREAK -> Brush.radialGradient(
                listOf(Color(0xFFFF3D00).copy(alpha = 0.6f), Color(0xFFFF9100).copy(alpha = 0.35f), Color.Transparent)
            )
            MascotEmotion.CHEER -> Brush.radialGradient(
                listOf(Color(0xFFFFD700).copy(alpha = 0.5f), Color(0xFF51FAC1).copy(alpha = 0.25f), Color.Transparent)
            )
            MascotEmotion.PETTED -> Brush.radialGradient(
                listOf(Color(0xFFFF4081).copy(alpha = 0.5f), Color(0xFFFF80AB).copy(alpha = 0.25f), Color.Transparent)
            )
            MascotEmotion.LISTENING -> Brush.radialGradient(
                listOf(Color(0xFF00E5FF).copy(alpha = 0.5f), Color(0xFF00B0FF).copy(alpha = 0.25f), Color.Transparent)
            )
            MascotEmotion.EMPATHY -> Brush.radialGradient(
                listOf(Color(0xFFFF6B6B).copy(alpha = 0.5f), Color(0xFF81C784).copy(alpha = 0.25f), Color.Transparent)
            )
            MascotEmotion.THINKING -> Brush.radialGradient(
                listOf(Color(0xFFFFEE58).copy(alpha = 0.5f), Color(0xFFFFCA28).copy(alpha = 0.25f), Color.Transparent)
            )
            MascotEmotion.IDLE -> Brush.radialGradient(
                listOf(Color(0xFF51FAC1).copy(alpha = 0.3f), Color.Transparent)
            )
        }
    }

    val gifUrl = remember(emotion) { KikiAnimationUrls.getAnimationUrl(emotion) }
    val imageRequest = remember(gifUrl, avatarRes) {
        if (gifUrl != null) {
            ImageRequest.Builder(context)
                .data(gifUrl)
                .crossfade(true)
                .placeholder(avatarRes)
                .error(avatarRes)
                .build()
        } else {
            ImageRequest.Builder(context)
                .data(avatarRes)
                .crossfade(true)
                .build()
        }
    }

    if (isFullBody) {
        // --- Hiển thị toàn thân nhân vật Kiki với tỉ lệ dọc 9:16 (kích thước cố định 100%, không phóng to, không xoay) ---
        Box(
            modifier = modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            // Hào quang đứng im tĩnh lặng phía sau
            Box(
                modifier = Modifier
                    .fillMaxSize(0.85f)
                    .background(auraBrush, CircleShape)
                    .blur(28.dp)
            )

            // Nhân vật Kiki toàn thân giữ nguyên kích cỡ 100%, đứng im tuyệt đối, chuyển động mượt bằng frame GIF
            AsyncImage(
                model = imageRequest,
                contentDescription = "Living Kiki Mascot Full Body",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // --- Hiển thị Avatar tròn nhỏ (Bong bóng đứng im hoàn toàn, kích thước cố định) ---
        Box(
            modifier = modifier
                .size(size)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            // Hào quang dịu nhẹ quanh viền bong bóng
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(auraBrush, CircleShape)
                    .blur(6.dp)
            )

            // Ảnh đại diện Kiki bên trong bong bóng đứng im, không xoay tròn, không co giãn
            AsyncImage(
                model = imageRequest,
                contentDescription = "Living Kiki Mascot",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = when (emotion) {
                            MascotEmotion.GREETING -> Color(0xFF00F5D4)
                            MascotEmotion.STREAK -> Color(0xFFFFD600)
                            MascotEmotion.CHEER -> Color(0xFF51FAC1)
                            MascotEmotion.PETTED -> Color(0xFFFF80AB)
                            MascotEmotion.LISTENING -> Color(0xFF00E5FF)
                            MascotEmotion.EMPATHY -> Color(0xFFFF6B6B)
                            MascotEmotion.THINKING -> Color(0xFFFFCA28)
                            else -> Color(0x6651FAC1)
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}
