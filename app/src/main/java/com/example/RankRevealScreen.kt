package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun RankRevealScreen(onEnterRealm: () -> Unit, onViewAnalysis: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isWarping by remember { mutableStateOf(false) }
    var warpSpeed by remember { mutableFloatStateOf(0f) }
    var whiteoutAlpha by remember { mutableFloatStateOf(0f) }
    var zoomOutScale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "rank_anim")
    
    // Animations
    val floatAnimSlow by infiniteTransition.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "float_slow"
    )
    
    val floatAnimFast by infiniteTransition.animateFloat(
        initialValue = -15f, targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse),
        label = "float_fast"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = zoomOutScale
                scaleY = zoomOutScale
                rotationZ = rotation
            }
    ) {
        // Background Floating Icons (Parallax Layer 3 equivalent)
        Icon(
            imageVector = Icons.Default.Stars,
            contentDescription = null,
            tint = Color(0xFFFFD166).copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 64.dp, y = (100 + floatAnimSlow).dp)
                .size(64.dp)
                .blur(4.dp)
        )
        
        Icon(
            imageVector = Icons.Default.Diamond,
            contentDescription = null,
            tint = Color(0xFF51FAC1).copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-32).dp, y = (40 + floatAnimSlow).dp)
                .size(80.dp)
                .blur(6.dp)
        )
        
        Icon(
            imageVector = Icons.Default.WorkspacePremium,
            contentDescription = null,
            tint = Color(0xFFFFCCB3).copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-80).dp, y = (-200 + floatAnimFast).dp)
                .size(48.dp)
                .blur(4.dp)
        )

        // Main UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Avatar Section
            Box(
                modifier = Modifier
                    .size(256.dp)
                    .offset(y = floatAnimSlow.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Glow
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .scale(pulseGlow)
                        .blur(30.dp)
                        .background(Color(0xFFFFD166).copy(alpha = 0.3f), CircleShape)
                )

                // Glass Pane Background
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, Color(0xCCFFD166), CircleShape)
                )

                // Crown Icon
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFFFD166),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 16.dp)
                        .size(40.dp)
                )

                // Kiki Image
                AsyncImage(
                    model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDHEU1luTpzRMj0AWjSdkrnM7_zqT6q0q2FslynO_5_1cv8tT93rUwkLgv3TCVA6OIKrsN3uY_7GvArdkxJRh6QUHgxg1uy_KrdaaDubiosxU1_D7RJSx7EzX8Um8G1GzxzLPPhd-MydROGtSyI5h-5e4CAMRnPcdG4scRuG0MHAiktwaPicNEkblA9GWH7ufeuVt_eJr8Q2FTnfsFGgknh56kd5Eh2wFZzQVxLWOrIYTfKGl1alFeXtVDmralygqCUa_WYd2Ea34w",
                    contentDescription = "Rank Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(top = 40.dp)
                        .size(192.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Victory Text
            val strings = LocalAppStrings.current
            Text(
                text = strings.destinyRevealed,
                color = Color(0xFFFFD166),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = strings.crystalKnight,
                color = Color.White,
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 56.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    shadowElevation = 20f
                    ambientShadowColor = Color(0xFFFFD166)
                    spotShadowColor = Color(0xFFFF9E00)
                }
            )

            Text(
                text = strings.levelAchieved,
                color = Color(0xFFEAE1D5),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, bottom = 32.dp).width(280.dp)
            )

            // Loot
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x3314141E))
                    .border(1.dp, Color(0x66FFD166), CircleShape)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.startingManaAwarded, color = Color(0xFFFFD166), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // View AI Analysis Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(CircleShape)
                        .background(Color(0x3351FAC1))
                        .border(1.dp, Color(0xFF51FAC1), CircleShape)
                        .clickable { onViewAnalysis() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF51FAC1))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.checkAIAnalysis, color = Color(0xFF51FAC1), fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
                    }
                }

                // Enter Realm Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFF9E00), Color(0xFFFFD166), Color(0xFFFF9E00))))
                        .clickable {
                            if (!isWarping) {
                                coroutineScope.launch {
                                    isWarping = true
                                    
                                    // Accelerate warp speed loop
                                    val startTime = System.currentTimeMillis()
                                    while(System.currentTimeMillis() - startTime < 1000) {
                                        val progress = (System.currentTimeMillis() - startTime) / 1000f
                                        warpSpeed = (warpSpeed + 1.8f).coerceAtMost(120f)
                                        zoomOutScale = 1f - (progress * 0.95f) // Shrink to center
                                        rotation = progress * 45f // Spin
                                        delay(16)
                                    }
                                    
                                    // Whiteout
                                    whiteoutAlpha = 1f
                                    delay(400)
                                    
                                    // Navigate
                                    onEnterRealm()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.continueJourney, color = Color(0xFF4E4636), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF4E4636))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Warp Canvas Overlay
        if (isWarping) {
            val stars = remember { List(300) { RankWarpStar(
                x = Random.nextFloat() * 2000 - 1000,
                y = Random.nextFloat() * 2000 - 1000,
                z = Random.nextFloat() * 1000 + 100,
                color = listOf(Color(0xFFFFD166), Color(0xFF00F5D4), Color(0xFF8A2BE2), Color.White).random(),
                vel = Random.nextFloat() * 2 + 1
            ) } }

            Canvas(modifier = Modifier.fillMaxSize().background(Color(0x2605000A))) {
                val cx = size.width / 2
                val cy = size.height / 2
                
                for (star in stars) {
                    star.prevZ = star.z
                    star.z -= (star.vel + warpSpeed * 80)
                    if (star.z <= 0) {
                        star.x = Random.nextFloat() * size.width * 2 - size.width
                        star.y = Random.nextFloat() * size.height * 2 - size.height
                        star.z = size.width
                        star.prevZ = star.z
                    }
                    
                    val px = (star.x / star.prevZ) * cx + cx
                    val py = (star.y / star.prevZ) * cy + cy
                    val x = (star.x / star.z) * cx + cx
                    val y = (star.y / star.z) * cy + cy
                    
                    val alpha = (warpSpeed / 50f).coerceIn(0f, 1f)
                    val strokeW = 1f + (1f - star.z / size.width) * (10f + warpSpeed * 15f)
                    
                    drawLine(
                        color = star.color.copy(alpha = alpha.coerceAtLeast(0.1f)),
                        start = Offset(px, py),
                        end = Offset(x, y),
                        strokeWidth = strokeW.coerceAtMost(20f),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Whiteout Overlay
        if (whiteoutAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = whiteoutAlpha))
            )
        }
    }
}

class RankWarpStar(
    var x: Float,
    var y: Float,
    var z: Float,
    var color: Color,
    var vel: Float
) {
    var prevZ: Float = z
}
