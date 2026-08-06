package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

// --- Particle Classes ---
class ManaDust(val width: Float, val height: Float, val cursorX: Float = -1f, val cursorY: Float = -1f) {
    var x = if (cursorX >= 0) cursorX else Random.nextFloat() * width
    var y = if (cursorY >= 0) cursorY else Random.nextFloat() * height
    var size = Random.nextFloat() * 1.5f + 0.5f 
    var baseVx = (Random.nextFloat() - 0.5f) * 0.2f
    var baseVy = (Random.nextFloat() - 0.5f) * 0.2f
    val colors = listOf(Color(0xFFFFD166), Color(0xFF00F5D4), Color(0xFF8A2BE2), Color.White)
    var color = colors.random()
    var life = Random.nextFloat() * 0.5f + 0.5f
    var decay = Random.nextFloat() * 0.005f + 0.002f
    var glow = Random.nextFloat() * 8f + 4f

    fun update(windX: Float, windY: Float, warpMultiplier: Float = 1f) {
        x += (baseVx + windX) * warpMultiplier
        y += (baseVy + windY) * warpMultiplier
        life -= decay * (warpMultiplier * 0.1f + 0.9f)

        if (x > width) x = 0f
        if (x < 0f) x = width
        if (y > height) y = 0f
        if (y < 0f) y = height
    }
}

class BurstParticle(val startX: Float, val startY: Float) {
    var x = startX
    var y = startY
    var size = Random.nextFloat() * 2.5f + 1.5f
    val angle = Random.nextFloat() * PI * 2
    val speed = Random.nextFloat() * 15f + 5f 
    var vx = cos(angle).toFloat() * speed
    var vy = sin(angle).toFloat() * speed
    val colors = listOf(Color(0xFFFFD166), Color(0xFF00F5D4), Color(0xFF8A2BE2), Color.White)
    var color = colors.random()
    var life = 1f
    var decay = Random.nextFloat() * 0.03f + 0.01f
    var rotation = Random.nextFloat() * PI.toFloat()
    var rotSpeed = (Random.nextFloat() - 0.5f) * 0.2f
    val shapeType = Random.nextInt(3) // 0: circle, 1: diamond, 2: star

    fun update() {
        x += vx
        y += vy
        vx *= 0.92f 
        vy *= 0.92f
        rotation += rotSpeed
        life -= decay
    }
}

class ShootingStar(val width: Float, val height: Float) {
    var x = Random.nextFloat() * width
    var y = -Random.nextFloat() * height
    var length = Random.nextFloat() * 100f + 50f
    var speed = Random.nextFloat() * 15f + 10f
    var angle = PI.toFloat() / 4f + (Random.nextFloat() - 0.5f) * 0.2f
    var vx = cos(angle) * speed
    var vy = sin(angle) * speed
    var life = 1f
    var active = false

    fun spawn() {
        if (!active && Random.nextFloat() < 0.005f) { 
            active = true
            x = Random.nextFloat() * width
            y = -100f
            life = 1f
        }
    }

    fun update(warpMultiplier: Float = 1f) {
        if (!active) return
        x += vx * warpMultiplier
        y += vy * warpMultiplier
        life -= 0.01f * warpMultiplier
        if (life <= 0 || x > width || y > height) {
            active = false
        }
    }
}

// --- Cosmic Scaffold (Shared Background) ---
@Composable
fun CosmicScaffold(
    externalTargetX: Float? = null, 
    externalTargetY: Float? = null, 
    warpSpeedMultiplier: Float = 1f,
    content: @Composable (currentX: Float, currentY: Float, onBurst: (x: Float, y: Float) -> Unit) -> Unit
) {
    var appLaunchState by remember { mutableIntStateOf(0) } 
    val bigBangTransition = updateTransition(targetState = appLaunchState, label = "BigBang")
    
    val bgAlpha by bigBangTransition.animateFloat(
        transitionSpec = {
            if (targetState >= 3) tween(1000, easing = LinearOutSlowInEasing) else tween(0)
        },
        label = "bgAlpha"
    ) { state ->
        if (state >= 3) 1f else 0f
    }

    val singularityScale by bigBangTransition.animateFloat(
        transitionSpec = {
            when {
                targetState == 1 -> tween(1500, easing = LinearEasing)
                targetState == 2 -> tween(500, easing = FastOutLinearInEasing)
                else -> tween(0)
            }
        },
        label = "singularityScale"
    ) { state ->
        when (state) {
            0 -> 0f
            1 -> 1f 
            2 -> 60f 
            else -> 0f 
        }
    }
    
    val singularityAlpha by bigBangTransition.animateFloat(
        transitionSpec = {
            if (targetState == 3) tween(500) else tween(0)
        },
        label = "singularityAlpha"
    ) { state ->
        if (state >= 3) 0f else 1f
    }

    LaunchedEffect(Unit) {
        delay(300) 
        appLaunchState = 1 
        delay(1500) 
        appLaunchState = 2 
        delay(300) 
        appLaunchState = 3 
    }

    var touchX by remember { mutableFloatStateOf(0f) }
    var touchY by remember { mutableFloatStateOf(0f) }
    val targetX = externalTargetX ?: touchX
    val targetY = externalTargetY ?: touchY
    var currentX by remember { mutableFloatStateOf(0f) }
    var currentY by remember { mutableFloatStateOf(0f) }
    var screenWidth by remember { mutableFloatStateOf(1000f) }
    var screenHeight by remember { mutableFloatStateOf(2000f) }

    val bursts = remember { mutableStateListOf<BurstParticle>() }
    val shootingStars = remember { List(3) { ShootingStar(1000f, 2000f) } }

    LaunchedEffect(targetX, targetY) {
        while (true) {
            withFrameNanos {
                currentX += (targetX - currentX) * 0.1f
                currentY += (targetY - currentY) * 0.1f
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) 
            .onGloballyPositioned {
                screenWidth = it.size.width.toFloat()
                screenHeight = it.size.height.toFloat()
            }
            .pointerInput(externalTargetX, externalTargetY) {
                if (externalTargetX == null && externalTargetY == null && appLaunchState == 3) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val cx = w / 2f
                            val cy = h / 2f
                            touchX = (change.position.x - cx) / cx
                            touchY = (change.position.y - cy) / cy
                        },
                        onDragEnd = {
                            touchX = 0f
                            touchY = 0f
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. The Singularity (Big Bang effect)
        if (appLaunchState < 3) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .scale(singularityScale)
                    .alpha(singularityAlpha)
                    .background(Brush.radialGradient(listOf(Color.White, Color(0xFF00F5D4), Color.Transparent)), CircleShape)
            )
        }

        // 2. The Universe 
        if (appLaunchState >= 2) {
            Box(modifier = Modifier.fillMaxSize().alpha(bgAlpha)) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF3C0A78).copy(alpha = 0.6f), Color(0xFF05000A)),
                                center = Offset(screenWidth * 0.1f, screenHeight * 0.2f),
                                radius = screenWidth * 0.8f
                            )
                        )
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF0064C8).copy(alpha = 0.4f), Color.Transparent),
                                center = Offset(screenWidth * 0.9f, screenHeight * 0.8f),
                                radius = screenWidth * 0.6f
                            )
                        )
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF140028), Color.Transparent),
                                center = Offset(screenWidth * 0.5f, screenHeight * 0.5f),
                                radius = screenWidth
                            )
                        )
                )

                // Background Clouds 
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = currentX * 10f
                            translationY = currentY * 10f
                        }
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
                    val auroraOffsetX by infiniteTransition.animateFloat(
                        initialValue = -50f, targetValue = 50f,
                        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse),
                        label = "auroraX"
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = auroraOffsetX.dp, y = (auroraOffsetX * 0.5f).dp)
                            .fillMaxWidth(0.8f)
                            .fillMaxHeight(0.5f)
                            .align(Alignment.TopStart)
                            .blur(80.dp)
                            .background(Brush.radialGradient(listOf(Color(0x268A2BE2), Color.Transparent)))
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = (-auroraOffsetX).dp, y = (-auroraOffsetX * 0.5f).dp)
                            .fillMaxWidth(0.6f)
                            .fillMaxHeight(0.4f)
                            .align(Alignment.BottomEnd)
                            .blur(60.dp)
                            .background(Brush.radialGradient(listOf(Color(0x1A00F5D4), Color.Transparent)))
                    )
                }

                // Floating Icons
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = currentX * 45f
                            translationY = currentY * 45f
                        }
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "icons")
                    val float1 by infiniteTransition.animateFloat(initialValue = -20f, targetValue = 20f, animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), label = "f1")
                    val float2 by infiniteTransition.animateFloat(initialValue = -15f, targetValue = 15f, animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse), label = "f2")

                    Icon(
                        Icons.Default.AutoStories, contentDescription = null,
                        tint = Color(0xFFFFD166),
                        modifier = Modifier
                            .offset(x = 60.dp, y = (150 + float1).dp)
                            .size(64.dp)
                            .blur(8.dp)
                            .graphicsLayer { alpha = 0.4f }
                    )
                    Icon(
                        Icons.Default.Diamond, contentDescription = null,
                        tint = Color(0xFF51FAC1),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = (-40).dp, y = (100 + float1).dp)
                            .size(80.dp)
                            .blur(10.dp)
                            .graphicsLayer { alpha = 0.3f }
                    )
                    Icon(
                        Icons.Default.School, contentDescription = null,
                        tint = Color(0xFFFFCCB3),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = 80.dp, y = (-50 + float2).dp)
                            .size(48.dp)
                            .blur(6.dp)
                            .graphicsLayer { alpha = 0.5f }
                    )
                }

                // Particle System
                var timeMillis by remember { mutableLongStateOf(0L) }
                val particles = remember { mutableListOf<ManaDust>() }

                LaunchedEffect(Unit) {
                    while (true) {
                        withFrameNanos {
                            timeMillis = it
                        }
                    }
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (particles.isEmpty() && size.width > 0) {
                        for (i in 0 until 100) { 
                            particles.add(ManaDust(size.width, size.height))
                        }
                    }

                    val t = timeMillis / 1_000_000_000f
                    val windX = (sin(t) * 0.2f + 0.1f).toFloat()
                    val windY = (cos(t * 0.8f) * 0.2f - 0.2f).toFloat()

                    shootingStars.forEach { star ->
                        if (star.active) {
                            star.update(warpSpeedMultiplier)
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color.White, Color.Transparent),
                                    start = Offset(star.x, star.y),
                                    end = Offset(star.x - cos(star.angle)*star.length, star.y - sin(star.angle)*star.length)
                                ),
                                start = Offset(star.x, star.y),
                                end = Offset(star.x - cos(star.angle)*star.length, star.y - sin(star.angle)*star.length),
                                strokeWidth = 3f * star.life
                            )
                        } else {
                            star.spawn()
                        }
                    }

                    for (i in particles.indices.reversed()) {
                        val p = particles[i]
                        p.update(windX, windY, warpSpeedMultiplier)
                        if (p.life > 0) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(p.color.copy(alpha = p.life * 0.5f), Color.Transparent),
                                    center = Offset(p.x, p.y),
                                    radius = p.size + p.glow
                                ),
                                radius = p.size + p.glow, 
                                center = Offset(p.x, p.y), 
                                blendMode = BlendMode.Screen
                            )
                            drawCircle(color = Color.White.copy(alpha = p.life), radius = p.size * 0.5f, center = Offset(p.x, p.y))
                        }
                        if (p.life <= 0) {
                            particles[i] = ManaDust(size.width, size.height)
                        }
                    }

                    for (i in bursts.indices.reversed()) {
                        val b = bursts[i]
                        b.update()
                        if (b.life > 0) {
                            val alpha = Math.max(0f, b.life)
                            withTransform({
                                translate(b.x, b.y)
                                rotate(Math.toDegrees(b.rotation.toDouble()).toFloat())
                            }) {
                                when (b.shapeType) {
                                    0 -> drawCircle(color = b.color.copy(alpha = alpha), radius = b.size, blendMode = BlendMode.Screen)
                                    1 -> {
                                        val path = Path().apply {
                                            moveTo(0f, -b.size); lineTo(b.size, 0f); lineTo(0f, b.size); lineTo(-b.size, 0f); close()
                                        }
                                        drawPath(path, color = b.color.copy(alpha = alpha), blendMode = BlendMode.Screen)
                                    }
                                    2 -> {
                                        val path = Path().apply {
                                            for (j in 0 until 5) {
                                                val angle1 = (18 + j * 72) * PI / 180
                                                lineTo((cos(angle1) * b.size).toFloat(), (-sin(angle1) * b.size).toFloat())
                                                val angle2 = (54 + j * 72) * PI / 180
                                                lineTo((cos(angle2) * (b.size / 2)).toFloat(), (-sin(angle2) * (b.size / 2)).toFloat())
                                            }
                                            close()
                                        }
                                        drawPath(path, color = b.color.copy(alpha = alpha), blendMode = BlendMode.Screen)
                                    }
                                }
                            }
                        } else {
                            bursts.removeAt(i)
                        }
                    }
                    if (targetX != 0f || targetY != 0f) {
                        if (Random.nextFloat() > 0.5f) {
                            val cx = (targetX * size.width / 2f) + size.width / 2f
                            val cy = (targetY * size.height / 2f) + size.height / 2f
                            particles.add(ManaDust(size.width, size.height, cx, cy))
                        }
                    }
                }

                // Foreground Content with reveal animation
                AnimatedVisibility(
                    visible = appLaunchState == 3,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    ) + fadeIn(tween(1000))
                ) {
                    content(currentX, currentY) { x, y ->
                        bursts.add(BurstParticle(x, y))
                    }
                }
            }
        }
    }
}

// --- Intro Content ---
@Composable
fun IntroContent(currentX: Float, currentY: Float, onBurst: (x: Float, y: Float) -> Unit, onIgnite: () -> Unit) {
    var screenWidth by remember { mutableFloatStateOf(1000f) }
    var screenHeight by remember { mutableFloatStateOf(2000f) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                screenWidth = it.size.width.toFloat()
                screenHeight = it.size.height.toFloat()
            }
    ) {
        // Layer 4: Kiki Masterclass
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationX = currentX * 70f
                    translationY = currentY * 70f
                },
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "kiki")
            val kikiFloat by infiniteTransition.animateFloat(
                initialValue = -15f, targetValue = 15f,
                animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
                label = "kiki_float"
            )
            val auraPulse by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.2f,
                animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
                label = "aura_pulse"
            )

            Box(
                modifier = Modifier
                    .offset(y = kikiFloat.dp)
                    .size(320.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(auraPulse)
                        .blur(40.dp)
                        .background(Brush.radialGradient(listOf(Color(0x66FFD166), Color(0x3300F5D4), Color.Transparent)), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.8f)
                        .scale(2.2f - auraPulse)
                        .blur(30.dp)
                        .background(Brush.radialGradient(listOf(Color(0x4D8A2BE2), Color.Transparent)), CircleShape)
                )

                AsyncImage(
                    model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCHzOhZ_Yv9HSJpC8fx2uMMMtXU55pzMAiieefMr9B_K2oDsoSwZVkkpkTTpHl8NmfeQXnDVLF5S349l54_6ZSI0aD4oBRF996wrZQVz_FM5CAX2aBrRKUSSf6AHIplgdF-R5H7uQqCwTSa-f1pvHGvJpkbTjGpDZ-slJctouiKXM8_dYkJ_qe_kBYCg18FxM6J2qT10go0Tce6pm1RDZ1UpDDmW4ebutwbsErHMK1SR9mesBOGZaCZrpbpzUqNfILdHmyDLSoXHps",
                    contentDescription = "Kiki",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Layer 5: Foreground UI (Glassmorphism)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(bottom = 32.dp)
                .graphicsLayer {
                    translationX = currentX * -20f
                    translationY = currentY * -20f
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "border")
            val sweepAngle by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
                label = "sweep"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Brush.linearGradient(
                        colors = listOf(Color(0x33FFFFFF), Color(0x0AFFFFFF)),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    ))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(Color(0x66FFFFFF), Color(0x00FFFFFF), Color(0x33FFFFFF))),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .drawBehind {
                        val rbColors = listOf(Color(0xFFFF9E00), Color(0xFF00F5D4), Color(0xFF8A2BE2), Color(0xFFFF9E00))
                        withTransform({ rotate(sweepAngle) }) {
                            drawRoundRect(
                                brush = Brush.sweepGradient(rbColors, center = Offset(size.width/2, size.height/2)),
                                size = Size(size.width, size.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(32.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx()),
                                alpha = 0.5f 
                            )
                        }
                    }
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    ChromaticText("KIKI HIHI")
                    Spacer(modifier = Modifier.height(16.dp))
                    val strings = LocalAppStrings.current
                    Text(
                        strings.awakenMagic,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 6.sp,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    UltimateButton(
                        text = strings.igniteJourney,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val cx = screenWidth / 2f
                            val cy = screenHeight - 150f
                            for (i in 0 until 50) { 
                                onBurst(cx, cy)
                            }
                            onIgnite()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChromaticText(text: String) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val offsetRed by animateFloatAsState(
        targetValue = if (isPressed) -6f else 0f, 
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy), 
        label = "red"
    )
    val offsetCyan by animateFloatAsState(
        targetValue = if (isPressed) 6f else 0f, 
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy), 
        label = "cyan"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.05f else 1f, 
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), 
        label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(Color.Transparent, Color(0x99FFFFFF), Color.Transparent),
        start = Offset(shimmerTranslate, 0f),
        end = Offset(shimmerTranslate + 400f, 0f)
    )

    Box(
        modifier = Modifier
            .clickable(interactionSource = interactionSource, indication = null) { }
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xB3FF0000), 
            textAlign = TextAlign.Center,
            letterSpacing = 4.sp,
            lineHeight = 40.sp,
            modifier = Modifier.offset(x = offsetRed.dp, y = (offsetRed*0.2f).dp) 
        )
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xB300FFFF), 
            textAlign = TextAlign.Center,
            letterSpacing = 4.sp,
            lineHeight = 40.sp,
            modifier = Modifier.offset(x = offsetCyan.dp, y = (-offsetCyan*0.2f).dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center,
            letterSpacing = 4.sp,
            lineHeight = 40.sp,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium.copy(
                brush = shimmerBrush
            ),
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            letterSpacing = 4.sp,
            lineHeight = 40.sp,
            modifier = Modifier.graphicsLayer(alpha = 0.9f)
        )
    }
}

@Composable
fun UltimateButton(text: String, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "btn")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "sweep"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, delayMillis = 1000, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_btn"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(pulse)
            .clip(RoundedCornerShape(32.dp))
            .drawBehind {
                val colors = listOf(Color(0xFFFF9E00), Color(0xFFFFD166), Color(0xFF00F5D4), Color(0xFF8A2BE2), Color(0xFFFF9E00))
                withTransform({ rotate(sweepAngle) }) {
                    drawRect(Brush.sweepGradient(colors))
                }
            }
            .padding(2.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(Color(0xE6FF9E00), Color(0xE600F5D4))))
            .clickable { onClick() }
            .drawBehind {
                val shimmer = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color(0x66FFFFFF), Color.Transparent),
                    start = Offset(shimmerTranslate, 0f),
                    end = Offset(shimmerTranslate + 200f, 200f)
                )
                drawRect(shimmer, blendMode = BlendMode.Screen)
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White)
        }
    }
}
