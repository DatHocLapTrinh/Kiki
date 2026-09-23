package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sqlite.room.VocabularyEntity
import com.example.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyVaultDialog(
    viewModel: StudyViewModel,
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    val vocabularyList by viewModel.vocabularyList.observeAsState(emptyList())
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Flashcard, 1: Danh sách
    var showAddDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            color = Color(0xFF0D0A1A),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x3351FAC1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CollectionsBookmark, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "VOCABULARY VAULT" else "SỔ TAY TỪ VỰNG",
                                color = Color(0xFF51FAC1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (isEnglish) "Master & Flashcards" else "Ôn Tập & Ghi Nhớ Sâu",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showAddDialog = true
                            }
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add Word", tint = Color(0xFFFFD166), modifier = Modifier.size(26.dp))
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0x99FFFFFF))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(4.dp)
                ) {
                    val tab1Title = if (isEnglish) "🃏 Flashcards (${vocabularyList.size})" else "🃏 Luyện Flashcard (${vocabularyList.size})"
                    val tab2Title = if (isEnglish) "📖 All Words" else "📖 Danh Sách Từ"

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 0) Color(0xFF51FAC1) else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTab = 0
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab1Title,
                            color = if (selectedTab == 0) Color(0xFF0D0A1A) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 1) Color(0xFF51FAC1) else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTab = 1
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab2Title,
                            color = if (selectedTab == 1) Color(0xFF0D0A1A) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Content
                if (vocabularyList.isEmpty()) {
                    EmptyVaultState(isEnglish = isEnglish, onAddClick = { showAddDialog = true })
                } else {
                    if (selectedTab == 0) {
                        FlashcardDeckView(
                            items = vocabularyList,
                            viewModel = viewModel,
                            isEnglish = isEnglish
                        )
                    } else {
                        VocabularyListView(
                            items = vocabularyList,
                            viewModel = viewModel,
                            isEnglish = isEnglish
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddWordDialog(
            isEnglish = isEnglish,
            onDismiss = { showAddDialog = false },
            onSave = { word, phonetic, meaning, example ->
                viewModel.saveWord(word, phonetic, meaning, example)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun FlashcardDeckView(
    items: List<VocabularyEntity>,
    viewModel: StudyViewModel,
    isEnglish: Boolean
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember(currentIndex) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val safeIndex = currentIndex.coerceIn(0, items.size - 1)
    val currentItem = items[safeIndex]

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400),
        label = "card_flip"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Counter & Progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${safeIndex + 1} / ${items.size}",
                color = Color(0xFF51FAC1),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (currentItem.isMastered) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x3322C55E))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Mastered ⭐" else "Đã thuộc ⭐",
                        color = Color(0xFF22C55E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3D Flip Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (rotation <= 90f) {
                        Brush.linearGradient(listOf(Color(0xFF1B1633), Color(0xFF131026)))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFF13242B), Color(0xFF0F1E19)))
                    }
                )
                .border(
                    1.5.dp,
                    if (rotation <= 90f) Color(0x6651FAC1) else Color(0x6622C55E),
                    RoundedCornerShape(24.dp)
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isFlipped = !isFlipped
                }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Mặt trước: Từ vựng & Phát âm
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isEnglish) "WORD / PHRASE" else "TỪ VỰNG / CÂU HỎI",
                        color = Color(0x88FFFFFF),
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = currentItem.word,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    if (currentItem.phonetic.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "/${currentItem.phonetic}/",
                            color = Color(0xFF51FAC1),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    IconButton(
                        onClick = {
                            viewModel.ttsManager.speak(currentItem.word)
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0x3351FAC1))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = Color(0xFF51FAC1), modifier = Modifier.size(28.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (isEnglish) "Tap card to flip for meaning 🔄" else "Chạm vào thẻ để lật xem nghĩa 🔄",
                        color = Color(0x66FFFFFF),
                        fontSize = 12.sp
                    )
                }
            } else {
                // Mặt sau: Nghĩa tiếng Việt & Ví dụ (Cần lật lại text 180 độ để không bị ngược)
                Column(
                    modifier = Modifier.graphicsLayer { rotationY = 180f },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isEnglish) "MEANING & CONTEXT" else "Ý NGHĨA & NGỮ CẢNH",
                        color = Color(0xFF22C55E),
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = currentItem.meaning,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (currentItem.example.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "\"${currentItem.example}\"",
                            color = Color(0xCCFFFFFF),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleVocabMastered(currentItem.vocabId, !currentItem.isMastered)
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentItem.isMastered) Color(0x3322C55E) else Color(0xFF22C55E),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(if (currentItem.isMastered) Icons.Default.Check else Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentItem.isMastered) (if (isEnglish) "Mastered" else "Đã thuộc") else (if (isEnglish) "Mark Mastered" else "Đánh dấu đã thuộc")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Navigation Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (currentIndex > 0) currentIndex--
                },
                enabled = currentIndex > 0,
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev")
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isEnglish) "Previous" else "Trước")
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (currentIndex < items.size - 1) currentIndex++
                },
                enabled = currentIndex < items.size - 1,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1), contentColor = Color(0xFF0D0A1A))
            ) {
                Text(if (isEnglish) "Next" else "Tiếp")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
            }
        }
    }
}

@Composable
private fun VocabularyListView(
    items: List<VocabularyEntity>,
    viewModel: StudyViewModel,
    isEnglish: Boolean
) {
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(items, key = { it.vocabId }) { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, if (item.isMastered) Color(0x4422C55E) else Color(0x22FFFFFF), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.word,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.phonetic.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "/${item.phonetic}/",
                                    color = Color(0xFF51FAC1),
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.meaning,
                            color = Color(0xFFFFD166),
                            fontSize = 14.sp
                        )
                        if (item.example.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.example,
                                color = Color(0x99FFFFFF),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                viewModel.ttsManager.speak(item.word)
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = Color(0xFF51FAC1), modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.toggleVocabMastered(item.vocabId, !item.isMastered)
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isMastered) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Toggle Mastered",
                                tint = if (item.isMastered) Color(0xFF22C55E) else Color(0x66FFFFFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.deleteVocab(item.vocabId)
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyVaultState(isEnglish: Boolean, onAddClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = Color(0x6651FAC1), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isEnglish) "Your Vocabulary Vault is empty" else "Sổ tay từ vựng đang trống",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isEnglish) "Save words or questions from Quests, or add new words manually to practice with 3D Flashcards!" else "Bấm ⭐ lưu câu hỏi từ bài kiểm tra, hoặc thêm từ mới để luyện thẻ Flashcard 3D nhé!",
                color = Color(0x99FFFFFF),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAddClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1), contentColor = Color(0xFF0D0A1A))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isEnglish) "Add First Word" else "Thêm từ vựng đầu tiên", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AddWordDialog(
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onSave: (word: String, phonetic: String, meaning: String, example: String) -> Unit
) {
    var word by remember { mutableStateOf("") }
    var phonetic by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181329),
        title = {
            Text(
                text = if (isEnglish) "Add New Word / Phrase" else "Thêm Từ Vựng Mới",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text(if (isEnglish) "Word / English phrase" else "Từ / Cụm từ tiếng Anh") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF51FAC1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phonetic,
                    onValueChange = { phonetic = it },
                    label = { Text(if (isEnglish) "Phonetic (optional)" else "Phiên âm (tùy chọn)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF51FAC1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it },
                    label = { Text(if (isEnglish) "Vietnamese Meaning" else "Nghĩa tiếng Việt") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF51FAC1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    label = { Text(if (isEnglish) "Example Sentence (optional)" else "Câu ví dụ (tùy chọn)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF51FAC1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (word.isNotBlank() && meaning.isNotBlank()) {
                        onSave(word, phonetic, meaning, example)
                    }
                },
                enabled = word.isNotBlank() && meaning.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1), contentColor = Color(0xFF0D0A1A))
            ) {
                Text(if (isEnglish) "Save" else "Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEnglish) "Cancel" else "Hủy", color = Color(0x99FFFFFF))
            }
        }
    )
}
