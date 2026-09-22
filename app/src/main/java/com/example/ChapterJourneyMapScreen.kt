package com.example

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.StudyViewModel

private data class ChapterLessonUi(
    val index: Int,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val completed: Boolean,
    val current: Boolean,
    val locked: Boolean
)

@Composable
fun ChapterJourneyMapScreen(
    viewModel: StudyViewModel,
    onBack: () -> Unit,
    onStartQuest: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()
    val chapterTitle by viewModel.currentChapterTitle.observeAsState("English Fundamentals")
    val chapterIndex by viewModel.currentChapterIndex.observeAsState(1)
    val currentNode by viewModel.currentNode.observeAsState(1)
    val isEnglish by viewModel.isEnglish.observeAsState(false)

    val safeNode = currentNode.coerceIn(1, 6)
    val completedCount = (safeNode - 1).coerceIn(0, 5)
    val progress = completedCount / 5f
    val lessons = remember(safeNode, isEnglish) {
        chapterLessonItems(completedCount, safeNode, isEnglish)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF080512), Color(0xFF10071F), Color(0xFF07182A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = 92.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ChapterProgressSummary(
                chapterTitle = chapterTitle,
                chapterIndex = chapterIndex,
                completedCount = completedCount,
                progress = progress,
                isEnglish = isEnglish
            )
            Spacer(modifier = Modifier.height(18.dp))
            ChapterLessonPath(
                lessons = lessons,
                strings = strings,
                isEnglish = isEnglish,
                onLessonClick = onStartQuest
            )
        }

        ChapterJourneyTopBar(
            chapterTitle = chapterTitle,
            chapterIndex = chapterIndex,
            completedCount = completedCount,
            onBack = onBack,
            strings = strings
        )
    }
}

private fun chapterLessonItems(
    completedCount: Int,
    currentNode: Int,
    isEnglish: Boolean
): List<ChapterLessonUi> {
    val copies = if (isEnglish) {
        listOf(
            "Core Vocabulary & Grammar" to "Master key words and grammatical rules.",
            "Pronunciation & Usage" to "Explore sentence patterns and natural phrases.",
            "Fluency Practice" to "Apply what you learned with challenge questions.",
            "Accuracy Check" to "Review weak points and sharpen comprehension.",
            "Chapter Mastery" to "Complete the chapter and unlock your reward."
        )
    } else {
        listOf(
            "Từ Vựng & Ngữ Pháp Gốc" to "Nắm vững từ vựng và cấu trúc ngữ pháp trọng tâm.",
            "Cách Dùng & Phát Âm" to "Khám phá ngữ cảnh câu và cách dùng tự nhiên.",
            "Luyện Phản Xạ Tiếng Anh" to "Áp dụng kiến thức qua các câu hỏi thử thách.",
            "Kiểm Tra Độ Chính Xác" to "Ôn lại tiến độ và củng cố các điểm còn yếu.",
            "Chinh Phục Chương" to "Hoàn thành chương để mở khóa phần thưởng lớn."
        )
    }
    val icons = listOf(Icons.AutoMirrored.Filled.MenuBook, Icons.Default.RecordVoiceOver, Icons.Default.AutoStories, Icons.Default.Spellcheck, Icons.Default.Diamond)
    return copies.mapIndexed { index, copy ->
        ChapterLessonUi(
            index = index + 1,
            title = copy.first,
            description = copy.second,
            icon = icons[index],
            completed = index < completedCount,
            current = index == completedCount && currentNode <= 5,
            locked = index > completedCount
        )
    }
}

@Composable
private fun ChapterJourneyTopBar(
    chapterTitle: String,
    chapterIndex: Int,
    completedCount: Int,
    onBack: () -> Unit,
    strings: AppStrings
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .height(62.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x3314142C))
                .border(1.dp, Color(0x6651FAC1), CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${strings.chapterPrefix}$chapterIndex",
                color = Color(0xFF51FAC1),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = chapterTitle,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("$completedCount/5", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(Color(0x442B2940))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(completedCount / 5f)
                        .fillMaxHeight()
                        .background(Color(0xFF51FAC1))
                )
            }
        }
    }
}

@Composable
private fun ChapterProgressSummary(
    chapterTitle: String,
    chapterIndex: Int,
    completedCount: Int,
    progress: Float,
    isEnglish: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xCC15142C))
            .border(1.dp, Color(0x665C3DA0), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEnglish) "Chapter progress" else "Tiến độ chương",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEnglish) "$chapterTitle • Chapter $chapterIndex" else "$chapterTitle • Chương $chapterIndex",
                        color = Color(0xB3FFFFFF),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text("$completedCount/5", color = Color(0xFF51FAC1), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(11.dp))
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0x442B2940))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF51FAC1), Color(0xFF7E5CFF))))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isEnglish) "${(progress * 100).toInt()}% • 5 learning steps" else "${(progress * 100).toInt()}% • 5 bước học",
                color = Color(0xB3FFFFFF),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ChapterLessonPath(
    lessons: List<ChapterLessonUi>,
    strings: AppStrings,
    isEnglish: Boolean,
    onLessonClick: (Int) -> Unit
) {
    val lessonStep = 178.dp
    val rewardTop = (lessons.size * lessonStep.value + 8f).dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height((lessons.size * lessonStep.value + 188f).dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val leftX = size.width * 0.25f
            val rightX = size.width * 0.75f
            val nodeY = { index: Int -> index * lessonStep.toPx() + 60.dp.toPx() }
            val nodeX = { index: Int -> if (index % 2 == 0) leftX else rightX }

            fun pathTo(lastSegment: Int): Path {
                return Path().apply {
                    moveTo(nodeX(0), nodeY(0))
                    for (segment in 1..lastSegment) {
                        val startY = nodeY(segment - 1)
                        val endY = nodeY(segment)
                        val controlX = (nodeX(segment - 1) + nodeX(segment)) / 2f
                        quadraticTo(controlX, startY + (endY - startY) * 0.52f, nodeX(segment), endY)
                    }
                }
            }

            if (lessons.size > 1) {
                drawPath(
                    path = pathTo(lessons.lastIndex),
                    color = Color(0x665D4A88),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 8.dp.toPx()))
                    )
                )
                val completedSegments = lessons.takeWhile { it.completed }.size.coerceAtMost(lessons.lastIndex)
                if (completedSegments > 0) {
                    drawPath(
                        path = pathTo(completedSegments),
                        color = Color(0xFF51FAC1),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx()))
                        )
                    )
                }
                for (index in 0 until lessons.lastIndex) {
                    drawCircle(
                        color = Color(0xFFFFD166),
                        radius = 3.5.dp.toPx(),
                        center = Offset(
                            (nodeX(index) + nodeX(index + 1)) / 2f,
                            (nodeY(index) + nodeY(index + 1)) / 2f
                        )
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            lessons.forEachIndexed { index, lesson ->
                ChapterLessonRow(
                    lesson = lesson,
                    step = lessonStep,
                    strings = strings,
                    isEnglish = isEnglish,
                    onClick = { onLessonClick(lesson.index) }
                )
            }
        }

        RewardChestCard(
            unlocked = lessons.all { it.completed },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = rewardTop, start = 20.dp, end = 20.dp)
        )
    }
}

@Composable
private fun ChapterLessonRow(
    lesson: ChapterLessonUi,
    step: Dp,
    strings: AppStrings,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    val isLeft = lesson.index % 2 == 1
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(step)) {
        val cardWidth = maxWidth * 0.55f
        val cardModifier = if (isLeft) {
            Modifier.align(Alignment.TopEnd).padding(top = 13.dp, end = 10.dp).width(cardWidth)
        } else {
            Modifier.align(Alignment.TopStart).padding(top = 13.dp, start = 10.dp).width(cardWidth)
        }
        val nodeModifier = if (isLeft) {
            Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 24.dp)
        } else {
            Modifier.align(Alignment.TopEnd).padding(end = 18.dp, top = 24.dp)
        }

        ChapterLessonNode(lesson = lesson, modifier = nodeModifier, onClick = onClick)
        ChapterLessonCard(
            lesson = lesson,
            modifier = cardModifier,
            isEnglish = isEnglish,
            onClick = onClick
        )
    }
}

@Composable
private fun ChapterLessonNode(
    lesson: ChapterLessonUi,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val pulse = if (lesson.current) {
        val transition = rememberInfiniteTransition(label = "chapter_current_node")
        transition.animateFloat(
            initialValue = 0.97f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Reverse),
            label = "chapter_current_scale"
        ).value
    } else 1f
    val colors = when (lesson.index) {
        1 -> listOf(Color(0xFFB1FFF4), Color(0xFF13C7B8), Color(0xFF075A75))
        2 -> listOf(Color(0xFFE5C1FF), Color(0xFF974DDA), Color(0xFF33145F))
        3 -> listOf(Color(0xFFA8E0FF), Color(0xFF3887E6), Color(0xFF143B82))
        4 -> listOf(Color(0xFFFFD28A), Color(0xFFEA7218), Color(0xFF79250F))
        else -> listOf(Color(0xFFE5E1F4), Color(0xFF87809D), Color(0xFF39344D))
    }
    val accent = when {
        lesson.completed -> Color(0xFFFFD166)
        lesson.current -> Color(0xFF51FAC1)
        else -> Color(0xFF8D839F)
    }

    Box(
        modifier = modifier
            .size(104.dp)
            .graphicsLayer(scaleX = pulse, scaleY = pulse)
            .semantics { contentDescription = lesson.title }
            .clickable(enabled = !lesson.locked, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (lesson.current) {
            Box(modifier = Modifier.size(96.dp).blur(16.dp).background(Color(0x8851FAC1), CircleShape))
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 7.dp)
                .size(width = 84.dp, height = 15.dp)
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.32f))
                .border(1.dp, accent.copy(alpha = 0.75f), RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(
                    if (lesson.locked) Brush.radialGradient(listOf(colors.first().copy(alpha = 0.22f), Color(0x3314141E)))
                    else Brush.radialGradient(colors)
                )
                .border(2.dp, accent.copy(alpha = if (lesson.locked) 0.36f else 0.9f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (lesson.locked) Icons.Default.Lock else if (lesson.completed) Icons.Default.Check else lesson.icon,
                contentDescription = null,
                tint = if (lesson.locked) Color(0xB3FFFFFF) else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        if (lesson.current) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-6).dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x4420202F))
                    .border(1.5.dp, Color(0xFF51FAC1), CircleShape)
            ) {
                Image(
                    painter = painterResource(R.drawable.kiki_icon),
                    contentDescription = "Kiki",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ChapterLessonCard(
    lesson: ChapterLessonUi,
    modifier: Modifier,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    val accent = when {
        lesson.completed -> Color(0xFFFFD166)
        lesson.current -> Color(0xFF51FAC1)
        else -> Color(0xFF776A91)
    }
    val status = when {
        lesson.completed -> if (isEnglish) "Completed" else "Đã hoàn thành"
        lesson.current -> if (isEnglish) "Continue learning" else "Tiếp tục học"
        else -> if (isEnglish) "Locked" else "Đang khóa"
    }
    Box(
        modifier = modifier
            .height(142.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xE614142B))
            .border(1.dp, accent.copy(alpha = 0.72f), RoundedCornerShape(20.dp))
            .clickable(enabled = !lesson.locked, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(25.dp).clip(CircleShape).background(accent.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${lesson.index}", color = Color(0xFF11101B), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    lesson.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(lesson.description, color = Color(0xCCFFFFFF), fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                if (lesson.completed) {
                    if (isEnglish) "1/1 lesson" else "1/1 bài học"
                } else {
                    if (isEnglish) "0/1 lesson" else "0/1 bài học"
                },
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(5.dp))
            Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(Color(0x44302A4A))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (lesson.completed) 1f else 0f)
                        .fillMaxHeight()
                        .background(accent)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(status, color = Color(0x99FFFFFF), fontSize = 9.sp)
                Icon(
                    imageVector = if (lesson.locked) Icons.Default.Lock else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
