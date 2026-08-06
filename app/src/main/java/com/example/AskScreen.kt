package com.example

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.viewmodel.StudyViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskScreen(viewModel: StudyViewModel) {
    var text by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )
    
    val history by viewModel.history.observeAsState(emptyList())
    val isGenerating by viewModel.isGenerating().observeAsState(false)
    val latestItem = history.firstOrNull()
    val listState = rememberLazyListState()

    LaunchedEffect(history.size, isGenerating) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 40.dp)) {
        TopAppBar(
            navigationIcon = { Spacer(modifier = Modifier.size(48.dp)) },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    val strings = LocalAppStrings.current
                    Text(strings.knowledgeDecoder, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) 
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
            if (isGenerating) {
                MagicalLoadingState()
            } else if (latestItem != null) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    item {
                        val strings = LocalAppStrings.current
                        QuestionCard(
                            question = latestItem.question.ifBlank { strings.attachedImage },
                            imageUri = latestItem.imageUri
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            val strings = LocalAppStrings.current
                            Text(strings.kikiAnalyzingSocratic, fontWeight = FontWeight.Bold, color = Color(0xFF51FAC1), fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    val steps = latestItem.answer.split("[STEP]").filter { it.isNotBlank() }
                    
                    itemsIndexed(steps) { index, stepText ->
                        StepGlassCard(index = index + 1, text = stepText.trim())
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val strings = LocalAppStrings.current
                            QuickActionButton(
                                text = strings.moreExplanation,
                                icon = Icons.Default.ChatBubbleOutline,
                                onClick = { viewModel.askFollowUpQuestion(context, latestItem.question, "EXPLAIN") },
                                enabled = !isGenerating,
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                text = strings.similarProblems,
                                icon = Icons.Default.ContentCopy,
                                onClick = { viewModel.askFollowUpQuestion(context, latestItem.question, "SIMILAR") },
                                enabled = !isGenerating,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                EmptyAskState()
            }
        }

        SpellInputBar(
            text = text,
            onTextChange = { text = it },
            selectedImageUri = selectedImageUri,
            onRemoveImage = { selectedImageUri = null },
            onPickImage = { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onSend = {
                if (!isGenerating && (text.isNotBlank() || selectedImageUri != null)) {
                    viewModel.askQuestion(context, text, selectedImageUri) {
                        text = ""
                        selectedImageUri = null
                    }
                }
            },
            isGenerating = isGenerating
        )
    }
}

@Composable
fun MagicalLoadingState() {
    val infiniteTransition = rememberInfiniteTransition(label = "kiki_loading")
    val pulse by infiniteTransition.animateFloat(initialValue = 0.8f, targetValue = 1.2f, animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
    val floatY by infiniteTransition.animateFloat(initialValue = -10f, targetValue = 10f, animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "floatY")
    
    var dotCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotCount = (dotCount + 1) % 4
        }
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.offset(y = floatY.dp)) {
            Box(modifier = Modifier.size(120.dp).scale(pulse).blur(20.dp).background(Color(0xFF8A2BE2).copy(alpha = 0.5f), CircleShape))
            AsyncImage(model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDHEU1luTpzRMj0AWjSdkrnM7_zqT6q0q2FslynO_5_1cv8tT93rUwkLgv3TCVA6OIKrsN3uY_7GvArdkxJRh6QUHgxg1uy_KrdaaDubiosxU1_D7RJSx7EzX8Um8G1GzxzLPPhd-MydROGtSyI5h-5e4CAMRnPcdG4scRuG0MHAiktwaPicNEkblA9GWH7ufeuVt_eJr8Q2FTnfsFGgknh56kd5Eh2wFZzQVxLWOrIYTfKGl1alFeXtVDmralygqCUa_WYd2Ea34w", contentDescription = "Kiki Loading", modifier = Modifier.size(80.dp).clip(CircleShape).border(2.dp, Color(0xFF51FAC1), CircleShape), contentScale = ContentScale.Crop)
        }
        Spacer(modifier = Modifier.height(32.dp))
        val strings = LocalAppStrings.current
        Text(text = "${strings.kikiDecodingMagic}${".".repeat(dotCount)}", color = Color(0xFF51FAC1), fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun EmptyAskState() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, tint = Color(0x33FFFFFF), modifier = Modifier.size(100.dp))
        Spacer(modifier = Modifier.height(24.dp))
        val strings = LocalAppStrings.current
        Text(strings.summonMagic, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(strings.summonMagicDesc, color = Color(0x80FFFFFF), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 14.sp)
    }
}

@Composable
fun QuestionCard(question: String, imageUri: Any?) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0x1A000000)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp)).padding(16.dp)) {
        Text(question, color = Color.White, fontSize = 16.sp)
        if (imageUri != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0x3351FAC1), RoundedCornerShape(12.dp))) {
                AsyncImage(model = imageUri, contentDescription = "Attached Image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                val scanLineAnim = rememberInfiniteTransition(label = "scan_line")
                val scanY by scanLineAnim.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "scan_y")
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF51FAC1), Color.Transparent))).align(Alignment.TopCenter).offset(y = (scanY * 200).dp))
                }
            }
        }
    }
}

@Composable
fun StepGlassCard(index: Int, text: String) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 200L); visible = true }
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(500)) + slideInVertically(tween(500), initialOffsetY = { 50 })) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Box(modifier = Modifier.padding(top = 4.dp).size(28.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF8A2BE2), Color(0xFF00F5D4)))), contentAlignment = Alignment.Center) {
                Text("$index", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)).background(Color(0x1AFFFFFF)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)).padding(16.dp)) {
                Text(text = text, color = Color(0xE6FFFFFF), fontSize = 15.sp, lineHeight = 22.sp)
            }
        }
    }
}

@Composable
fun QuickActionButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val alpha = if (enabled) 1f else 0.5f
    Row(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Color(0x33000000)).border(1.dp, Color(0x33FFFFFF).copy(alpha = alpha), RoundedCornerShape(12.dp)).clickable(enabled = enabled, onClick = onClick).padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFFFFD166).copy(alpha = alpha), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.White.copy(alpha = alpha), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SpellInputBar(text: String, onTextChange: (String) -> Unit, selectedImageUri: Uri?, onRemoveImage: () -> Unit, onPickImage: () -> Unit, onSend: () -> Unit, isGenerating: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 70.dp).clip(RoundedCornerShape(32.dp)).background(Color(0x6614141E)).border(1.dp, Color(0x4D51FAC1), RoundedCornerShape(32.dp))) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (selectedImageUri != null) {
                Box(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)) {
                    AsyncImage(model = selectedImageUri, contentDescription = "Selected", modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF51FAC1), RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                    IconButton(onClick = onRemoveImage, modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp).size(20.dp).background(Color.White, CircleShape).padding(2.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                IconButton(onClick = onPickImage) { Icon(Icons.Default.CenterFocusStrong, contentDescription = "Camera", tint = Color(0xFF51FAC1)) }
                val strings = LocalAppStrings.current
                OutlinedTextField(value = text, onValueChange = onTextChange, placeholder = { Text(strings.castingQuestion, color = Color(0x80FFFFFF)) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color(0xFF51FAC1)), maxLines = 3, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { onSend() }))
                val canSend = !isGenerating && (text.isNotBlank() || selectedImageUri != null)
                val sendBtnColor = if (canSend) Color(0xFFFF9E00) else Color(0x33FFFFFF)
                val sendIconColor = if (canSend) Color(0xFF0F0C29) else Color(0x80FFFFFF)
                IconButton(onClick = onSend, enabled = canSend, modifier = Modifier.background(sendBtnColor, CircleShape)) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = sendIconColor) }
            }
        }
    }
}
