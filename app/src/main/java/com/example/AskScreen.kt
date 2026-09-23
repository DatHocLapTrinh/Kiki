package com.example

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.viewmodel.StudyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskScreen(viewModel: StudyViewModel) {
    var text by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )
    
    val history by viewModel.history.observeAsState(emptyList())
    val isGenerating by viewModel.isGenerating().observeAsState(false)
    val latestItem = history.firstOrNull()
    val listState = rememberLazyListState()
    val quizQuestions by viewModel.quizQuestions.observeAsState(emptyList())
    val quizLoading by viewModel.isQuizLoading().observeAsState(false)
    var showQuizDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val isSpeaking by viewModel.ttsManager.isSpeaking
    val isEnglish by viewModel.isEnglish.observeAsState(false)

    LaunchedEffect(history.size, isGenerating) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size)
        }
    }

    // Hiển thị nút Scroll-to-bottom nếu người dùng cuộn lên trên xem lại
    val showScrollToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex < (history.size - 2).coerceAtLeast(0)
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
            actions = {
                if (history.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showClearConfirmDialog = true
                        }
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = Color(0x99FFFFFF))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
            if (history.isEmpty() && !isGenerating) {
                EmptyAskState()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    reverseLayout = false
                ) {
                    // Toàn bộ lịch sử chat - cũ nhất trên, mới nhất dưới
                    items(
                        count = history.size,
                        key = { index ->
                            val item = history[history.size - 1 - index]
                            if (item.id != 0) "qa_${item.id}" else "qa_${index}_${item.question.hashCode()}"
                        }
                    ) { index ->
                        val item = history[history.size - 1 - index]
                        val strings = LocalAppStrings.current
                        Spacer(modifier = Modifier.height(24.dp))

                        // Câu hỏi người dùng - bên phải kèm nút xóa item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.deleteAIQuestion(item.id.toLong())
                                },
                                modifier = Modifier.size(28.dp).padding(end = 4.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0x66FFFFFF), modifier = Modifier.size(16.dp))
                            }
                            QuestionCard(
                                question = item.question.ifBlank { strings.attachedImage },
                                imageUri = item.imageUri
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Header câu trả lời AI - bên trái kèm nút nghe toàn bộ
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.kikiAnalyzingSocratic, fontWeight = FontWeight.Bold, color = Color(0xFF51FAC1), fontSize = 14.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Nút lưu vào Sổ tay từ vựng
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val cleanQuestion = item.question.ifBlank { "AI Q&A" }
                                        val firstLine = item.answer.lineSequence().firstOrNull { it.isNotBlank() } ?: item.answer.take(80)
                                        viewModel.saveWord(
                                            word = cleanQuestion.take(100),
                                            meaning = firstLine.take(150),
                                            example = item.answer.take(250)
                                        )
                                        Toast.makeText(
                                            context,
                                            if (isEnglish) "Saved to Vocabulary Vault! ⭐" else "Đã lưu vào Sổ tay từ vựng! ⭐",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0x22FFD166))
                                ) {
                                    Icon(
                                        Icons.Default.BookmarkBorder,
                                        contentDescription = "Save to Vault",
                                        tint = Color(0xFFFFD166),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Nút nghe phát âm tiếng Anh
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.ttsManager.speak(item.answer)
                                    },
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0x2651FAC1))
                                ) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Speak Answer",
                                        tint = if (isSpeaking) Color(0xFF51FAC1) else Color(0xFFFFD166),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        val steps = item.answer.split("[STEP]").filter { it.isNotBlank() }
                        if (steps.size > 1) {
                            steps.forEachIndexed { i, stepText ->
                                StepGlassCard(
                                    index = i + 1,
                                    text = stepText.trim(),
                                    onSpeak = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.ttsManager.speak(stepText.trim())
                                    },
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(stepText.trim()))
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(context, strings.copiedToClipboard, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        } else {
                            StepGlassCard(
                                index = 1,
                                text = item.answer.trim(),
                                onSpeak = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.ttsManager.speak(item.answer.trim())
                                },
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(item.answer.trim()))
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, strings.copiedToClipboard, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    // Nút đề xuất chỉ hiện dưới câu mới nhất
                    if (latestItem != null && !isGenerating) {
                        item {
                            val strings = LocalAppStrings.current
                            Spacer(modifier = Modifier.height(14.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    QuickActionButton(
                                        text = strings.deepAnalysis,
                                        icon = Icons.Default.Analytics,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.deepAnalysis(context, latestItem.question)
                                        },
                                        enabled = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    QuickActionButton(
                                        text = strings.quickQuiz,
                                        icon = Icons.Default.Quiz,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showQuizDialog = true
                                            viewModel.generateQuickQuiz(context, latestItem.question)
                                        },
                                        enabled = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Loading indicator ở dưới cùng
                    if (isGenerating) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color(0xFF51FAC1),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    val strings = LocalAppStrings.current
                                    Text(strings.kikiDecodingMagic, color = Color(0xFF51FAC1), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Scroll-to-Bottom Floating Action Button
            if (showScrollToBottom) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            listState.animateScrollToItem(history.size)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 80.dp, end = 8.dp)
                        .size(42.dp),
                    shape = CircleShape,
                    containerColor = Color(0xFF51FAC1),
                    contentColor = Color(0xFF0F0C29)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Scroll down", modifier = Modifier.size(20.dp))
                }
            }
        }

        SpellInputBar(
            text = text,
            onTextChange = { text = it },
            selectedImageUri = selectedImageUri,
            onRemoveImage = { selectedImageUri = null },
            onPickImage = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onSend = {
                if (!isGenerating && (text.isNotBlank() || selectedImageUri != null)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.askQuestion(context, text, selectedImageUri) {
                        text = ""
                        selectedImageUri = null
                    }
                }
            },
            isGenerating = isGenerating
        )
    }

    if (showQuizDialog) {
        QuizDialog(
            quizBlocks = quizQuestions,
            isLoading = quizLoading,
            onDismiss = { showQuizDialog = false }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = Color(0xFF14142B),
            title = { Text("Xóa toàn bộ lịch sử?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Tất cả các câu hỏi và giải thích AI trước đây sẽ bị xóa hoàn toàn.", color = Color(0xCCFFFFFF)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAIHistory()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Xóa hết", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Hủy", color = Color(0xFF51FAC1))
                }
            }
        )
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
    Column(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x331E1E38))
            .border(1.dp, Color(0x4D51FAC1), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Text(question, color = Color.White, fontSize = 15.sp)
        if (imageUri != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0x3351FAC1), RoundedCornerShape(12.dp))) {
                AsyncImage(model = imageUri, contentDescription = "Attached Image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                val scanLineAnim = rememberInfiniteTransition(label = "scan_line")
                val scanY by scanLineAnim.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "scan_y")
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF51FAC1), Color.Transparent))).align(Alignment.TopCenter).offset(y = (scanY * 180).dp))
                }
            }
        }
    }
}

@Composable
fun StepGlassCard(
    index: Int,
    text: String,
    onSpeak: () -> Unit = {},
    onCopy: () -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 150L); visible = true }
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(400)) + slideInVertically(tween(400), initialOffsetY = { 40 })) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Box(
                modifier = Modifier.padding(top = 4.dp).size(26.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF8A2BE2), Color(0xFF00F5D4)))),
                contentAlignment = Alignment.Center
            ) {
                Text("$index", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                    .clickable { onCopy() }
                    .padding(14.dp)
            ) {
                Column {
                    Text(text = text, color = Color(0xE6FFFFFF), fontSize = 15.sp, lineHeight = 22.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", tint = Color(0x80FFFFFF), modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onSpeak, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Pronounce", tint = Color(0xFF51FAC1), modifier = Modifier.size(16.dp))
                        }
                    }
                }
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
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text(strings.castingQuestion, color = Color(0x80FFFFFF)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color(0xFF51FAC1)),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )
                val canSend = !isGenerating && (text.isNotBlank() || selectedImageUri != null)
                val sendBtnColor = if (canSend) Color(0xFFFF9E00) else Color(0x33FFFFFF)
                val sendIconColor = if (canSend) Color(0xFF0F0C29) else Color(0x80FFFFFF)
                IconButton(onClick = onSend, enabled = canSend, modifier = Modifier.background(sendBtnColor, CircleShape)) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = sendIconColor)
                }
            }
        }
    }
}

@Composable
fun QuizDialog(quizBlocks: List<String>, isLoading: Boolean, onDismiss: () -> Unit) {
    val strings = LocalAppStrings.current
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var quizFinished by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14142B),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Quiz, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.quizTitle, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        },
        text = {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF51FAC1))
                }
            } else if (quizBlocks.isEmpty()) {
                Text("Không thể tạo câu hỏi lúc này. Vui lòng thử lại sau.", color = Color(0xCCFFFFFF))
            } else if (quizFinished) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Hoàn thành Quiz!", fontWeight = FontWeight.Bold, color = Color(0xFF51FAC1), fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("${strings.quizResult} $score/${quizBlocks.size}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
            } else {
                val block = quizBlocks.getOrNull(currentIndex) ?: ""
                val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
                val questionLine = lines.firstOrNull { it.startsWith("Q:") }?.removePrefix("Q:")?.trim() ?: block
                val options = lines.filter { it.matches(Regex("^[A-D]:.*")) }
                val ansLine = lines.firstOrNull { it.startsWith("ANS:") }?.removePrefix("ANS:")?.trim() ?: ""

                Column {
                    Text("Câu ${currentIndex + 1}/${quizBlocks.size}", color = Color(0xFF51FAC1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(questionLine, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))

                    options.forEach { opt ->
                        val letter = opt.take(1)
                        val isSelected = selectedAnswer == letter
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0x3351FAC1) else Color(0x14FFFFFF))
                                .border(1.dp, if (isSelected) Color(0xFF51FAC1) else Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                                .clickable(enabled = !showResult) { selectedAnswer = letter }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(opt, color = if (isSelected) Color(0xFF51FAC1) else Color.White, fontSize = 14.sp)
                        }
                    }

                    if (showResult) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isCorrect = selectedAnswer.equals(ansLine, ignoreCase = true)
                        Text(
                            text = if (isCorrect) strings.quizCorrect else "${strings.quizWrong} $ansLine",
                            color = if (isCorrect) Color(0xFF22C55E) else Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isLoading && quizBlocks.isNotEmpty() && !quizFinished) {
                Button(
                    onClick = {
                        if (!showResult) {
                            if (selectedAnswer.isNotBlank()) {
                                showResult = true
                                val block = quizBlocks.getOrNull(currentIndex) ?: ""
                                val ansLine = block.lines().firstOrNull { it.trim().startsWith("ANS:") }?.removePrefix("ANS:")?.trim() ?: ""
                                if (selectedAnswer.equals(ansLine, ignoreCase = true)) score++
                            }
                        } else {
                            if (currentIndex < quizBlocks.size - 1) {
                                currentIndex++
                                selectedAnswer = ""
                                showResult = false
                            } else {
                                quizFinished = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1), contentColor = Color(0xFF0F0C29))
                ) {
                    Text(if (!showResult) "Kiểm tra" else if (currentIndex < quizBlocks.size - 1) "Tiếp theo" else "Xem kết quả", fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(strings.quizClose, color = Color(0xFF51FAC1))
                }
            }
        }
    )
}
