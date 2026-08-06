package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.StudyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProgressionMapScreen(viewModel: StudyViewModel, onBack: () -> Unit, onStartQuest: (Int) -> Unit) {
    val scrollState = rememberScrollState()
    val chapterTitle by viewModel.currentChapterTitle.observeAsState("Genesis Core")
    val chapterIndex by viewModel.currentChapterIndex.observeAsState(1)
    val strings = LocalAppStrings.current

    var isWarping by remember { mutableStateOf(false) }
    var warpSpeed by remember { mutableFloatStateOf(0f) }
    var bloomAlpha by remember { mutableFloatStateOf(0f) }
    var whiteoutAlpha by remember { mutableFloatStateOf(0f) }

    val shakeAnim = rememberInfiniteTransition(label = "shake")
    val shakeOffsetX by shakeAnim.animateFloat(
        initialValue = if (isWarping) -4f else 0f,
        targetValue = if (isWarping) 4f else 0f,
        animationSpec = infiniteRepeatable(tween(50, easing = LinearEasing), RepeatMode.Reverse),
        label = "shakeX"
    )
    val shakeOffsetY by shakeAnim.animateFloat(
        initialValue = if (isWarping) 4f else 0f,
        targetValue = if (isWarping) -4f else 0f,
        animationSpec = infiniteRepeatable(tween(70, easing = LinearEasing), RepeatMode.Reverse),
        label = "shakeY"
    )

    val coroutineScope = rememberCoroutineScope()
    
    // Track current node
    val currentNode by viewModel.currentNode.observeAsState(1)
    val nodeCount = 5
    val firstNodeY = 116.dp
    val nodeSpacing = 142.dp
    val mapHeight = firstNodeY + nodeSpacing * (nodeCount - 1) + 136.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0615)) // Deep space
            .offset(x = shakeOffsetX.dp, y = shakeOffsetY.dp)
    ) {
        // Map Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = 88.dp, bottom = 104.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight)
            ) {
                val cx = size.width / 2f
                val path = Path().apply {
                    moveTo(cx, firstNodeY.toPx())
                    for (i in 1 until nodeCount) {
                        val startY = firstNodeY.toPx() + (i - 1) * nodeSpacing.toPx()
                        val endY = startY + nodeSpacing.toPx()
                        val controlX = if (i % 2 == 0) cx + 72.dp.toPx() else cx - 72.dp.toPx()
                        quadraticTo(controlX, (startY + endY) / 2, cx, endY)
                    }
                }
                
                // Dim path
                drawPath(
                    path = path,
                    color = Color(0x33FFFFFF),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 8.dp.toPx()))
                    )
                )

                // Active path (up to current node)
                val activePath = Path().apply {
                    moveTo(cx, firstNodeY.toPx())
                    for (i in 1 until currentNode.coerceIn(1, nodeCount + 1)) {
                        val startY = firstNodeY.toPx() + (i - 1) * nodeSpacing.toPx()
                        val endY = startY + nodeSpacing.toPx()
                        val controlX = if (i % 2 == 0) cx + 72.dp.toPx() else cx - 72.dp.toPx()
                        quadraticTo(controlX, (startY + endY) / 2, cx, endY)
                    }
                }
                drawPath(
                    path = activePath,
                    color = Color(0xFF51FAC1),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 8.dp.toPx()))
                    )
                )
            }

            // Nodes
            val nodeIcons = listOf(
                Icons.Default.MenuBook, Icons.Default.Science, Icons.Default.Calculate,
                Icons.Default.History, Icons.Default.Diamond
            )

            for (i in 1..nodeCount) {
                val status = when {
                    i < currentNode -> OrbStatus.COMPLETED
                    i == currentNode -> OrbStatus.CURRENT
                    else -> OrbStatus.LOCKED
                }
                
                ProgressionNode(
                    index = i,
                    icon = nodeIcons[i - 1],
                    yOffset = firstNodeY + nodeSpacing * (i - 1),
                    status = status,
                    isBoss = i == 5,
                    onClick = {
                        if (status == OrbStatus.CURRENT && !isWarping) {
                            coroutineScope.launch {
                                isWarping = true
                                val startTime = System.currentTimeMillis()
                                while(System.currentTimeMillis() - startTime < 1000) {
                                    warpSpeed = (warpSpeed + 1.8f).coerceAtMost(120f)
                                    delay(16)
                                }
                                bloomAlpha = 0.95f
                                delay(400)
                                whiteoutAlpha = 1f
                                bloomAlpha = 1f
                                delay(400)
                                isWarping = false
                                warpSpeed = 0f
                                whiteoutAlpha = 0f
                                bloomAlpha = 0f
                                onStartQuest(i)
                            }
                        }
                    }
                )
            }
        }

        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xE60B0615))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = chapterTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "${strings.chapterPrefix}$chapterIndex",
                    color = Color(0xFF51FAC1),
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            
            // Mini Mana Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = "Mana", tint = Color(0xFF51FAC1), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier.width(80.dp).height(8.dp).clip(CircleShape).background(Color(0x33FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(((currentNode - 1).coerceIn(0, nodeCount) / nodeCount.toFloat()))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Color(0xFF51FAC1))
                    )
                }
            }
        }

        // Warp Overlay (from MainMapScreen)
        if (isWarping) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                for (i in 0..100) {
                    val angle = Math.random() * 2 * Math.PI
                    val r = warpSpeed * Math.random() * size.width
                    val x = cx + (r * Math.cos(angle)).toFloat()
                    val y = cy + (r * Math.sin(angle)).toFloat()
                    val len = warpSpeed * 2f
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = Offset(x, y),
                        end = Offset(x + (len * Math.cos(angle)).toFloat(), y + (len * Math.sin(angle)).toFloat()),
                        strokeWidth = 2f
                    )
                }
            }
        }
        
        // Bloom & Whiteout
        if (bloomAlpha > 0f) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF51FAC1).copy(alpha = bloomAlpha * 0.3f)))
        }
        if (whiteoutAlpha > 0f) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = whiteoutAlpha)))
        }
    }
}

@Composable
fun ProgressionNode(
    index: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    yOffset: androidx.compose.ui.unit.Dp,
    status: OrbStatus,
    isBoss: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "node_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val floatY by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = yOffset),
        contentAlignment = Alignment.Center
    ) {
        val size = if (isBoss) 88.dp else if (status == OrbStatus.CURRENT) 78.dp else 68.dp
        
        // Node Background
        val bgColor = when (status) {
            OrbStatus.COMPLETED -> Color(0xFFFFD166).copy(alpha = 0.15f)
            OrbStatus.CURRENT -> Color(0xFF51FAC1).copy(alpha = 0.15f)
            OrbStatus.AVAILABLE -> Color(0xFF51FAC1).copy(alpha = 0.10f)
            OrbStatus.LOCKED -> Color(0xFF1E1E28).copy(alpha = 0.4f)
        }
        val borderColor = when (status) {
            OrbStatus.COMPLETED -> Color(0xFFFFD166).copy(alpha = 0.5f)
            OrbStatus.CURRENT -> Color(0xFF51FAC1).copy(alpha = 0.8f)
            OrbStatus.AVAILABLE -> Color(0xFF51FAC1).copy(alpha = 0.45f)
            OrbStatus.LOCKED -> Color(0x1AFFFFFF)
        }
        val iconColor = when (status) {
            OrbStatus.COMPLETED -> Color(0xFFFFD166)
            OrbStatus.CURRENT -> Color(0xFF51FAC1)
            OrbStatus.AVAILABLE -> Color(0xFF51FAC1)
            OrbStatus.LOCKED -> Color(0x80FFFFFF)
        }
        
        Box(
            modifier = Modifier
                .size(size)
                .scale(if (status == OrbStatus.CURRENT) pulse else 1f)
                .clip(CircleShape)
                .background(bgColor)
                .border(if (isBoss) 3.dp else 1.dp, borderColor, CircleShape)
                .clickable(enabled = status == OrbStatus.CURRENT || status == OrbStatus.AVAILABLE) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = iconColor, 
                modifier = Modifier.size(if (isBoss) 38.dp else 28.dp)
            )
            
            // Checkmark for completed
            if (status == OrbStatus.COMPLETED) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(21.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF006C4F), Color(0xFF51FAC1))))
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
        
        // Kiki Avatar Floating above current node
        if (status == OrbStatus.CURRENT) {
            Box(
                modifier = Modifier
                    .offset(y = (-48).dp + floatY.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .border(2.dp, Color(0xFF51FAC1), CircleShape)
            ) {
                Image(
                    painter = painterResource(R.drawable.kiki_icon),
                    contentDescription = "Kiki",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
