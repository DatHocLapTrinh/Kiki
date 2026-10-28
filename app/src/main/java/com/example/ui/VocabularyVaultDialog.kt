package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import android.view.ViewGroup
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
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
    val dueVocabularyList by viewModel.dueVocabularyList.observeAsState(emptyList())
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Flashcard, 1: Danh sách
    var showAddDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.let { w ->
                w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                w.setDimAmount(0f)
                w.setBackgroundDrawableResource(android.R.color.transparent)
            }
            onDispose {}
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0D0A1A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Header & Tab Switcher (Horizontal padding)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x22FFFFFF))
                            .padding(4.dp)
                    ) {
                        val tab1Title = if (isEnglish) {
                            "🃏 Flashcards (${vocabularyList.size})"
                        } else {
                            "🃏 Luyện Flashcard (${vocabularyList.size})"
                        }
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
                }

                // Content Area with weight(1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (vocabularyList.isEmpty()) {
                        EmptyVaultState(isEnglish = isEnglish, onAddClick = { showAddDialog = true })
                    } else if (selectedTab == 0) {
                        FlashcardDeckView(
                            items = vocabularyList,
                            dueItems = dueVocabularyList,
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
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
            onSave = { word, phonetic, meaning, example ->
                viewModel.saveWord(word, phonetic, meaning, example)
                showAddDialog = false
            }
        )
    }
}

private enum class SwipeDirection { LEFT, RIGHT }
private data class SwipeHistoryItem(
    val index: Int,
    val vocabId: Long,
    val direction: SwipeDirection
)

@Composable
private fun FlashcardDeckView(
    items: List<VocabularyEntity>,
    dueItems: List<VocabularyEntity>,
    viewModel: StudyViewModel,
    isEnglish: Boolean
) {
    var filterDueOnly by remember(dueItems.size) { mutableStateOf(dueItems.isNotEmpty()) }
    var customFilteredIds by remember { mutableStateOf<Set<Long>?>(null) }

    val baseItems = if (filterDueOnly && dueItems.isNotEmpty()) dueItems else items
    val activeItems = remember(baseItems, customFilteredIds) {
        if (customFilteredIds != null) {
            baseItems.filter { it.vocabId in customFilteredIds!! }
        } else {
            baseItems
        }
    }

    var currentIndex by remember(filterDueOnly, activeItems.size, customFilteredIds) { mutableIntStateOf(0) }
    var isFlipped by remember(currentIndex, filterDueOnly, customFilteredIds) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val swipeOffsetX = remember { Animatable(0f) }
    val swipeAlpha = remember { Animatable(1f) }
    val historyStack = remember(filterDueOnly, activeItems.size, customFilteredIds) { mutableStateListOf<SwipeHistoryItem>() }

    // Session tracking of words swiped in this session
    val learnedVocabIds = remember(filterDueOnly, customFilteredIds) { mutableStateListOf<Long>() }
    val unlearnedVocabIds = remember(filterDueOnly, customFilteredIds) { mutableStateListOf<Long>() }

    var isAnimatingSwipe by remember { mutableStateOf(false) }

    val safeIndex = currentIndex.coerceIn(0, (activeItems.size - 1).coerceAtLeast(0))
    val currentItem = activeItems.getOrNull(safeIndex)

    LaunchedEffect(safeIndex) {
        if (!isAnimatingSwipe) {
            swipeOffsetX.snapTo(0f)
            swipeAlpha.snapTo(1f)
        }
    }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400),
        label = "card_flip"
    )

    val handleMarkLearned: () -> Unit = {
        val item = activeItems.getOrNull(currentIndex)
        if (item != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.soundEffectManager.playCorrect()
            viewModel.toggleVocabMastered(item.vocabId, true)
            viewModel.reviewVocabularySm2(item, quality = 4)
            if (!learnedVocabIds.contains(item.vocabId)) {
                learnedVocabIds.add(item.vocabId)
            }
            unlearnedVocabIds.remove(item.vocabId)
            historyStack.add(SwipeHistoryItem(currentIndex, item.vocabId, SwipeDirection.RIGHT))
            isFlipped = false
            currentIndex++
        }
    }

    val handleMarkUnlearned: () -> Unit = {
        val item = activeItems.getOrNull(currentIndex)
        if (item != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.soundEffectManager.playIncorrect()
            viewModel.toggleVocabMastered(item.vocabId, false)
            viewModel.reviewVocabularySm2(item, quality = 1)
            if (!unlearnedVocabIds.contains(item.vocabId)) {
                unlearnedVocabIds.add(item.vocabId)
            }
            learnedVocabIds.remove(item.vocabId)
            historyStack.add(SwipeHistoryItem(currentIndex, item.vocabId, SwipeDirection.LEFT))
            isFlipped = false
            currentIndex++
        }
    }

    val handleUndo: () -> Unit = {
        if (!isAnimatingSwipe && historyStack.isNotEmpty()) {
            isAnimatingSwipe = true
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val lastItem = historyStack.removeAt(historyStack.lastIndex)
            learnedVocabIds.remove(lastItem.vocabId)
            unlearnedVocabIds.remove(lastItem.vocabId)

            scope.launch {
                val startOffset = if (lastItem.direction == SwipeDirection.LEFT) -1300f else 1300f
                swipeOffsetX.snapTo(startOffset)
                swipeAlpha.snapTo(0.1f)
                currentIndex = lastItem.index
                isFlipped = false
                launch {
                    swipeOffsetX.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
                }
                launch {
                    swipeAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
                }
                delay(410)
                isAnimatingSwipe = false
            }
        } else if (!isAnimatingSwipe && currentIndex > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            currentIndex--
            isFlipped = false
        }
    }

    val triggerSwipeRight: () -> Unit = {
        if (!isAnimatingSwipe && activeItems.isNotEmpty() && currentIndex < activeItems.size) {
            isAnimatingSwipe = true
            scope.launch {
                launch { swipeOffsetX.animateTo(1400f, tween(400, easing = FastOutSlowInEasing)) }
                launch { swipeAlpha.animateTo(0f, tween(380, delayMillis = 40, easing = FastOutSlowInEasing)) }
                delay(410)
                handleMarkLearned()
                swipeOffsetX.snapTo(0f)
                swipeAlpha.snapTo(1f)
                isAnimatingSwipe = false
            }
        }
    }

    val triggerSwipeLeft: () -> Unit = {
        if (!isAnimatingSwipe && activeItems.isNotEmpty() && currentIndex < activeItems.size) {
            isAnimatingSwipe = true
            scope.launch {
                launch { swipeOffsetX.animateTo(-1400f, tween(400, easing = FastOutSlowInEasing)) }
                launch { swipeAlpha.animateTo(0f, tween(380, delayMillis = 40, easing = FastOutSlowInEasing)) }
                delay(410)
                handleMarkUnlearned()
                swipeOffsetX.snapTo(0f)
                swipeAlpha.snapTo(1f)
                isAnimatingSwipe = false
            }
        }
    }

    if (filterDueOnly && dueItems.isEmpty() && customFilteredIds == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color(0x3322C55E))
                        .border(1.5.dp, Color(0xFF22C55E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isEnglish) "All reviews completed! 🎉" else "Hoàn thành ôn tập hôm nay! 🎉",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isEnglish) "You have no more due flashcards right now. Review all cards or add new ones!" else "Bạn đã ôn tập xong tất cả thẻ đến hạn. Xem lại toàn bộ kho từ nhé!",
                    color = Color(0x99FFFFFF),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { filterDueOnly = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1), contentColor = Color(0xFF0D0A1A)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (isEnglish) "Review All Words" else "Luyện toàn bộ thẻ từ", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    // Hoàn thành toàn bộ thẻ trong danh sách (Màn hình thống kê phiên học)
    if (currentIndex >= activeItems.size) {
        val totalCount = activeItems.size
        val learnedCount = learnedVocabIds.size
        val unlearnedCount = unlearnedVocabIds.size
        val percent = if (totalCount > 0) ((learnedCount.toFloat() / totalCount.toFloat()) * 100).roundToInt() else 100
        val unlearnedItems = activeItems.filter { it.vocabId in unlearnedVocabIds }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (unlearnedCount == 0) Color(0x3322C55E) else Color(0x3351FAC1))
                        .border(2.dp, if (unlearnedCount == 0) Color(0xFF22C55E) else Color(0xFF51FAC1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (unlearnedCount == 0) Icons.Default.EmojiEvents else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (unlearnedCount == 0) Color(0xFF22C55E) else Color(0xFF51FAC1),
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (unlearnedCount == 0) {
                        if (isEnglish) "Perfect! Mastered All 🎉" else "Hoàn hảo! Đã thuộc hết 🎉"
                    } else {
                        if (isEnglish) "Review Session Completed 🎯" else "Tổng kết lượt ôn tập 🎯"
                    },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isEnglish) {
                        "Completed $totalCount cards • Retention rate: $percent%"
                    } else {
                        "Đã ôn $totalCount thẻ • Tỷ lệ nhớ: $percent%"
                    },
                    color = Color(0xBBFFFFFF),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Bảng thống kê 2 mục: Đã học vs Chưa nhớ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cột Đã học (Xanh lá)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x1F22C55E),
                        border = BorderStroke(1.5.dp, Color(0x6622C55E)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "LEARNED" else "ĐÃ HỌC",
                                    color = Color(0xFF22C55E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$learnedCount",
                                color = Color(0xFF51FAC1),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (isEnglish) "/ $totalCount cards" else "/ $totalCount thẻ",
                                color = Color(0x88FFFFFF),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Cột Chưa nhớ (Đỏ)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x1FEF4444),
                        border = BorderStroke(1.5.dp, Color(0x66EF4444)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "NEED WORK" else "CHƯA NHỚ",
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$unlearnedCount",
                                color = Color(0xFFFF6B6B),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (isEnglish) "/ $totalCount cards" else "/ $totalCount thẻ",
                                color = Color(0x88FFFFFF),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Thanh tiến độ ghi nhớ trực quan
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isEnglish) "Mastery rate" else "Tiến độ ghi nhớ",
                            color = Color(0x88FFFFFF),
                            fontSize = 11.5.sp
                        )
                        Text(
                            text = "$percent%",
                            color = Color(0xFF51FAC1),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (percent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = Color(0xFF22C55E),
                        trackColor = Color(0x33EF4444),
                    )
                }

                // Danh sách từ chưa nhớ (nếu có)
                if (unlearnedItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x1AFFFFFF),
                        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isEnglish) "Words needing practice (${unlearnedItems.size}):" else "Từ cần luyện lại (${unlearnedItems.size}):",
                                color = Color(0xFFFFD166),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = unlearnedItems.joinToString(", ") { it.word },
                                color = Color(0xDDFFFFFF),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Các nút hành động
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Button 1: Làm lại các từ chưa nhớ (chỉ hiện khi có từ chưa nhớ)
                    if (unlearnedCount > 0) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                customFilteredIds = unlearnedVocabIds.toSet()
                                learnedVocabIds.clear()
                                unlearnedVocabIds.clear()
                                historyStack.clear()
                                currentIndex = 0
                                isFlipped = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "Practice unlearned words ($unlearnedCount)" else "Luyện lại từ chưa nhớ ($unlearnedCount từ)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Button 2: Ôn lại toàn bộ
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            customFilteredIds = null
                            learnedVocabIds.clear()
                            unlearnedVocabIds.clear()
                            historyStack.clear()
                            currentIndex = 0
                            isFlipped = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF51FAC1),
                            contentColor = Color(0xFF0D0A1A)
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "Review all cards (${baseItems.size})" else "Ôn lại toàn bộ (${baseItems.size} từ)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        return
    }

    if (currentItem == null) return

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Filter & Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sub-filter Chips: All vs Due vs Custom Unlearned
                if (customFilteredIds != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x33EF4444),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier.clickable {
                            customFilteredIds = null
                            learnedVocabIds.clear()
                            unlearnedVocabIds.clear()
                            historyStack.clear()
                            currentIndex = 0
                            isFlipped = false
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "⚡ Drill Unlearned (${activeItems.size}) ✕" else "⚡ Ôn từ chưa nhớ (${activeItems.size}) ✕",
                                color = Color(0xFFFF8B8B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!filterDueOnly) Color(0x3351FAC1) else Color(0x1AFFFFFF),
                            border = BorderStroke(1.dp, if (!filterDueOnly) Color(0xFF51FAC1) else Color(0x33FFFFFF)),
                            modifier = Modifier.clickable {
                                filterDueOnly = false
                                currentIndex = 0
                                isFlipped = false
                            }
                        ) {
                            Text(
                                text = if (isEnglish) "All (${items.size})" else "Tất cả (${items.size})",
                                color = if (!filterDueOnly) Color(0xFF51FAC1) else Color(0x99FFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (filterDueOnly) Color(0x33F59E0B) else Color(0x1AFFFFFF),
                            border = BorderStroke(1.dp, if (filterDueOnly) Color(0xFFF59E0B) else Color(0x33FFFFFF)),
                            modifier = Modifier.clickable {
                                filterDueOnly = true
                                currentIndex = 0
                                isFlipped = false
                            }
                        ) {
                            Text(
                                text = if (isEnglish) "🔥 Due (${dueItems.size})" else "🔥 Cần ôn (${dueItems.size})",
                                color = if (filterDueOnly) Color(0xFFFFD166) else Color(0x99FFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${safeIndex + 1} / ${activeItems.size}",
                    color = Color(0xFF51FAC1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3D Flip Card with Drag / Swipe Gestures & Fade Off
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .offset { IntOffset(swipeOffsetX.value.roundToInt(), 0) }
                    .graphicsLayer {
                        rotationY = rotation
                        rotationZ = (swipeOffsetX.value / 35f).coerceIn(-15f, 15f)
                        alpha = swipeAlpha.value
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
                    .pointerInput(safeIndex) {
                        if (isAnimatingSwipe) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var totalDragX = 0f
                            var totalDragY = 0f
                            var isDragging = false
                            val touchSlop = viewConfiguration.touchSlop

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    if (!isDragging) {
                                        // Tap gesture -> Lật thẻ
                                        if (!change.isConsumed) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isFlipped = !isFlipped
                                        }
                                    } else {
                                        // Kết thúc vuốt: kiểm tra ngưỡng vuốt
                                        val thresholdPx = 90.dp.toPx()
                                        if (swipeOffsetX.value > thresholdPx) {
                                            triggerSwipeRight()
                                        } else if (swipeOffsetX.value < -thresholdPx) {
                                            triggerSwipeLeft()
                                        } else {
                                            scope.launch {
                                                launch { swipeOffsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                                launch { swipeAlpha.animateTo(1f) }
                                            }
                                        }
                                    }
                                    break
                                } else {
                                    val dragX = change.position.x - change.previousPosition.x
                                    val dragY = change.position.y - change.previousPosition.y
                                    totalDragX += dragX
                                    totalDragY += dragY
                                    if (!isDragging && abs(totalDragX) > touchSlop) {
                                        isDragging = true
                                    }
                                    if (isDragging) {
                                        change.consume()
                                        scope.launch {
                                            swipeOffsetX.snapTo(swipeOffsetX.value + dragX)
                                            val fadeProgress = (abs(swipeOffsetX.value) / 600f).coerceIn(0f, 0.4f)
                                            swipeAlpha.snapTo(1f - fadeProgress)
                                        }
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Card face content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        // Mặt trước: Từ vựng + phiên âm + phát âm
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isEnglish) "FLASHCARD" else "THẺ TỪ VỰNG",
                                color = Color(0x88FFFFFF),
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = currentItem.word,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            val cleanPhonetic = currentItem.phonetic.trim().removeSurrounding("/")
                            if (cleanPhonetic.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "/$cleanPhonetic/",
                                    color = Color(0xFF51FAC1),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            IconButton(
                                onClick = {
                                    viewModel.ttsManager.speak(currentItem.word)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x3351FAC1))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = Color(0xFF51FAC1), modifier = Modifier.size(24.dp))
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = if (isEnglish) "Tap card to flip for meaning 🔄" else "Chạm để lật xem nghĩa 🔄",
                                color = Color(0x66FFFFFF),
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        // Mặt sau: Bố cục chia rõ 2 phần Ý NGHĨA & VÍ DỤ với highlight badge
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { rotationY = 180f }
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // --- PHẦN 1: Ý NGHĨA ---
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0x33FFD166),
                                border = BorderStroke(1.dp, Color(0xFFFFD166))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD166),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "MEANING" else "Ý NGHĨA",
                                        color = Color(0xFFFFD166),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Giải thích ý nghĩa bên dưới
                            Text(
                                text = currentItem.meaning.ifBlank { if (isEnglish) "No definition yet" else "Chưa có giải nghĩa" },
                                color = Color(0xFFFFEAA7),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 30.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Divider phân cách giữa 2 phần
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(2.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // --- PHẦN 2: VÍ DỤ ---
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0x3351FAC1),
                                border = BorderStroke(1.dp, Color(0xFF51FAC1))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = Color(0xFF51FAC1),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "EXAMPLE" else "VÍ DỤ",
                                        color = Color(0xFF51FAC1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Giải thích ví dụ bên dưới
                            val exampleText = currentItem.example.trim().ifEmpty {
                                if (isEnglish) "No context example available." else "Chưa có câu ví dụ minh họa."
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0x1F22C55E),
                                border = BorderStroke(1.dp, Color(0x4022C55E)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "“$exampleText”",
                                    color = Color(0xEEFFFFFF),
                                    fontSize = 14.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = if (isEnglish) "Tap card to flip back 🔄" else "Chạm để lật lại 🔄",
                                color = Color(0x66FFFFFF),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Header banner ĐÃ NHỚ ở mép trên cùng của thẻ (khi vuốt sang phải hoặc nhấn nút Đã nhớ)
                val rightSwipeProgress = ((swipeOffsetX.value - 12f) / 70f).coerceIn(0f, 1f)
                if (rightSwipeProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                alpha = rightSwipeProgress
                            }
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                            .background(Color(0xFF16A34A))
                            .border(
                                BorderStroke(1.5.dp, Color(0xFF51FAC1)),
                                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "Remembered" else "Đã nhớ",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Header banner CHƯA NHỚ ở mép trên cùng của thẻ (khi vuốt sang trái hoặc nhấn nút Chưa nhớ)
                val leftSwipeProgress = ((-swipeOffsetX.value - 12f) / 70f).coerceIn(0f, 1f)
                if (leftSwipeProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                alpha = leftSwipeProgress
                            }
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                            .background(Color(0xFFDC2626))
                            .border(
                                BorderStroke(1.5.dp, Color(0xFFFF8B8B)),
                                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "Need Review" else "Chưa nhớ",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isEnglish) "👈 Swipe left: Need review  •  Swipe right: Remembered 👉" else "👈 Vuốt trái: Chưa nhớ  •  Vuốt phải: Đã nhớ 👉",
                color = Color(0x66FFFFFF),
                fontSize = 11.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val safeBottomPadding = if (navBarBottom > 0.dp) navBarBottom + 12.dp else 24.dp

        // Dedicated Elevated Bottom Dock: [Chưa nhớ] [Quay lại] [Đã học]
        Surface(
            color = Color(0xFF140F24),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0x33EF4444), Color(0x4D51FAC1), Color(0x3322C55E)))),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = safeBottomPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: CHƯA NHỚ (Vuốt thẻ sang trái)
                OutlinedButton(
                    onClick = triggerSwipeLeft,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0x24EF4444),
                        contentColor = Color(0xFFFF8B8B)
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Chưa nhớ",
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Need Review" else "Chưa nhớ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Button 2: QUAY LẠI THẺ TRƯỚC ĐÓ (Undo thẻ từ hướng tương ứng)
                val canUndo = historyStack.isNotEmpty() || currentIndex > 0
                IconButton(
                    onClick = handleUndo,
                    enabled = canUndo,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (canUndo) Color(0x2651FAC1) else Color(0x0FFFFFFF))
                        .border(1.5.dp, if (canUndo) Color(0x8051FAC1) else Color(0x22FFFFFF), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Quay lại thẻ trước",
                        tint = if (canUndo) Color(0xFF51FAC1) else Color(0x44FFFFFF),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Button 3: ĐÃ NHỚ (Lướt thẻ sang phải)
                Button(
                    onClick = triggerSwipeRight,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22C55E),
                        contentColor = Color(0xFF0D0A1A)
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Đã nhớ",
                        tint = Color(0xFF0D0A1A),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Remembered" else "Đã nhớ",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
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
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val safeBottomPadding = if (navBarBottom > 0.dp) navBarBottom + 24.dp else 60.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = safeBottomPadding)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.meaning,
                                color = Color(0xFFFFD166),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (item.isMastered) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x3322C55E))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "Mastered ⭐" else "Đã thuộc ⭐",
                                        color = Color(0xFF22C55E),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x2251FAC1))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Lv.${item.repetitionLevel} • ${item.intervalDays}d",
                                        color = Color(0xFF51FAC1),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
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
    viewModel: StudyViewModel,
    onDismiss: () -> Unit,
    onSave: (word: String, phonetic: String, meaning: String, example: String) -> Unit
) {
    var word by remember { mutableStateOf("") }
    var phonetic by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var isLoadingPhonetic by remember { mutableStateOf(false) }

    // Tự động load phiên âm chuẩn xác khi user nhập từ tiếng Anh
    LaunchedEffect(word) {
        val cleanWord = word.trim()
        if (cleanWord.length >= 2) {
            delay(350)
            isLoadingPhonetic = true
            try {
                val resolved = viewModel.autoLookupPhonetic(cleanWord)
                if (!resolved.isNullOrBlank()) {
                    phonetic = resolved
                }
            } catch (_: Exception) {}
            isLoadingPhonetic = false
        }
    }

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
                    label = { Text(if (isEnglish) "Phonetic (auto-lookup)" else "Phiên âm (tự động load)") },
                    trailingIcon = {
                        if (isLoadingPhonetic) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF51FAC1),
                                strokeWidth = 2.dp
                            )
                        } else if (phonetic.isNotBlank()) {
                            IconButton(
                                onClick = { viewModel.ttsManager.speak(word) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Listen",
                                    tint = Color(0xFF51FAC1),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
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
