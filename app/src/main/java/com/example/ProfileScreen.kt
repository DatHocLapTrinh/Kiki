package com.example

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VocabularyVaultDialog
import com.example.viewmodel.StudyViewModel

fun resolveAvatarResource(avatarUri: String?): Int {
    return when (avatarUri) {
        "kiki_mascot_head" -> R.drawable.kiki_mascot_head
        "companion_mascot" -> R.drawable.companion_mascot
        "kiki_hero_intro" -> R.drawable.kiki_hero_intro
        "kiki_hero_auth" -> R.drawable.kiki_hero_auth
        else -> R.drawable.kiki_icon
    }
}

@Composable
fun ProfileScreen(viewModel: StudyViewModel, onLogout: () -> Unit = {}) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val userName by viewModel.userName.observeAsState("Adventurer")
    val userId by viewModel.currentUserId.observeAsState(-1L)
    val xp by viewModel.xp.observeAsState(0)
    val level by viewModel.level.observeAsState(1)
    val streak by viewModel.streak.observeAsState(0)
    val rankTitle by viewModel.rankTitle.observeAsState("Bronze Novice")
    val isEnglish by viewModel.isEnglish.observeAsState(false)
    val avatarUri by viewModel.avatarUri.observeAsState(null)
    val studyMotto by viewModel.studyMotto.observeAsState("")
    val vocabularyList by viewModel.vocabularyList.observeAsState(emptyList())
    val availableChapters by viewModel.availableChapters.observeAsState(emptyList())
    val chapterProgress by viewModel.chapterProgress.observeAsState(emptyList())
    val currentChapterTitle by viewModel.currentChapterTitle.observeAsState("English Fundamentals")
    val currentNode by viewModel.currentNode.observeAsState(1)

    val journeyStages = remember(availableChapters, chapterProgress) {
        JourneyStageMapper.mapStages(availableChapters, chapterProgress)
    }
    val completedLessons = JourneyStageMapper.completedLessons(journeyStages)
    val totalLessons = JourneyStageMapper.totalLessons(journeyStages)
    val earnedStars = JourneyStageMapper.earnedStars(journeyStages)
    val completedStages = journeyStages.count { it.state == JourneyStageState.COMPLETED }
    val progress = if (totalLessons == 0) 0f else completedLessons.toFloat() / totalLessons

    val settings = remember(userId) {
        context.getSharedPreferences("profile_settings_$userId", android.content.Context.MODE_PRIVATE)
    }
    val selectedLevel by viewModel.selectedLevel.observeAsState("Beginner")
    var soundEnabled by remember(userId) { mutableStateOf(settings.getBoolean("sound_enabled", true)) }
    var notificationsEnabled by remember(userId) { mutableStateOf(settings.getBoolean("notifications_enabled", true)) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLevelDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showVaultDialog by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(userId) {
        val savedLanguage = settings.getBoolean("english", false)
        if (savedLanguage != isEnglish) viewModel.setEnglish(savedLanguage)
    }

    val xpInLevel = xp.mod(100)
    val xpProgress = xpInLevel / 100f
    val xpToNextLevel = 100 - xpInLevel
    val isEnglishText = { vi: String, en: String -> if (isEnglish) en else vi }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x66090414), Color(0x5016072B), Color(0x55071A2C))
                )
            )
    ) {
        ProfileCosmicGlow()
        GalaxyEffectOverlay(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .padding(bottom = 116.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileHeader(userName = userName, earnedStars = earnedStars, isEnglish = isEnglish)
            Spacer(modifier = Modifier.height(16.dp))

            ProfileIdentityCard(
                userName = userName,
                rankTitle = rankTitle,
                level = level,
                xp = xp,
                xpProgress = xpProgress,
                xpToNextLevel = xpToNextLevel,
                avatarUri = avatarUri,
                studyMotto = studyMotto,
                isEnglish = isEnglish,
                onEditClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    showEditProfileDialog = true
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileStatCard(Modifier.weight(1f), strings.xpLabel, "$xp", Icons.Default.AutoAwesome, Color(0xFFFFD166))
                ProfileStatCard(Modifier.weight(1f), isEnglishText("Chuỗi ngày", "Streak"), "$streak 🔥", Icons.Default.LocalFireDepartment, Color(0xFFFF9E00))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileStatCard(Modifier.weight(1f), isEnglishText("Bài học", "Lessons"), "$completedLessons/$totalLessons", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF51FAC1))
                ProfileStatCard(Modifier.weight(1f), strings.starsLabel, "$earnedStars", Icons.Default.Star, Color(0xFFC68CFF))
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Thẻ Sổ tay từ vựng & Flashcards
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF132729), Color(0xFF1B1538))))
                    .border(1.dp, Color(0x6651FAC1), RoundedCornerShape(20.dp))
                    .clickable {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        showVaultDialog = true
                    }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0x3351FAC1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CollectionsBookmark, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "VOCABULARY VAULT" else "SỔ TAY TỪ VỰNG",
                                color = Color(0xFF51FAC1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (isEnglish) "${vocabularyList.size} Saved Words & Flashcards" else "${vocabularyList.size} Từ Đã Lưu & Flashcard 3D",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            ProfileJourneyCard(
                currentChapterTitle = currentChapterTitle,
                currentNode = currentNode,
                completedStages = completedStages,
                stageCount = journeyStages.size,
                progress = progress,
                isEnglish = isEnglish
            )
            Spacer(modifier = Modifier.height(16.dp))

            ProfileAchievements(
                completedLessons = completedLessons,
                streak = streak,
                allStagesCompleted = journeyStages.isNotEmpty() && journeyStages.all { it.state == JourneyStageState.COMPLETED },
                isEnglish = isEnglish
            )
            Spacer(modifier = Modifier.height(16.dp))

            ProfileSettingsCard(
                strings = strings,
                isEnglish = isEnglish,
                selectedLevel = selectedLevel,
                soundEnabled = soundEnabled,
                notificationsEnabled = notificationsEnabled,
                onSelectLevelClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    showLevelDialog = true
                },
                onLanguageChanged = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    settings.edit().putBoolean("english", it).apply()
                    viewModel.setEnglish(it)
                },
                onSoundChanged = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    soundEnabled = it
                    settings.edit().putBoolean("sound_enabled", it).apply()
                },
                onNotificationsChanged = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    notificationsEnabled = it
                    settings.edit().putBoolean("notifications_enabled", it).apply()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    showLogoutDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B7A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x996B3047))
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(strings.logout, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showLevelDialog) {
        LevelSelectionDialog(
            currentLevel = selectedLevel,
            isEnglish = isEnglish,
            onLevelSelected = { newLevel ->
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                viewModel.updateLearningLevel(newLevel)
                android.widget.Toast.makeText(
                    context,
                    if (isEnglish) "Switched to level: $newLevel" else "Đã chuyển sang trình độ: $newLevel",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                showLevelDialog = false
            },
            onDismiss = { showLevelDialog = false }
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = userName,
            currentAvatar = avatarUri,
            currentMotto = studyMotto,
            isEnglish = isEnglish,
            onDismiss = { showEditProfileDialog = false },
            onSave = { newName, newAvatar, newMotto ->
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                viewModel.updateProfile(newName, newAvatar, newMotto)
                android.widget.Toast.makeText(
                    context,
                    if (isEnglish) "Profile updated!" else "Hồ sơ đã được cập nhật thành công!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                showEditProfileDialog = false
            }
        )
    }

    if (showVaultDialog) {
        VocabularyVaultDialog(
            viewModel = viewModel,
            isEnglish = isEnglish,
            onDismiss = { showVaultDialog = false }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(isEnglishText("Đăng xuất?", "Log out?")) },
            text = { Text(isEnglishText("Bạn có chắc muốn đăng xuất khỏi tài khoản này không?", "Are you sure you want to log out of this account?")) },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    showLogoutDialog = false
                    viewModel.logout()
                    onLogout()
                }) { Text(isEnglishText("Đăng xuất", "Log out"), color = Color(0xFFFF5F6D)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text(isEnglishText("Hủy", "Cancel")) }
            }
        )
    }
}

@Composable
private fun ProfileCosmicGlow() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.offset(x = (-80).dp, y = 80.dp).size(190.dp).blur(80.dp).background(Color(0x553D0B78), CircleShape))
        Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 70.dp, y = (-90).dp).size(220.dp).blur(90.dp).background(Color(0x4430A5C8), CircleShape))
    }
}

@Composable
fun GalaxyEffectOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "settings_galaxy")
    val shimmer by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "settings_galaxy_shimmer"
    )
    Canvas(modifier = modifier) {
        val stars = listOf(
            0.08f to 0.18f, 0.19f to 0.72f, 0.31f to 0.34f, 0.44f to 0.14f,
            0.55f to 0.82f, 0.68f to 0.27f, 0.77f to 0.62f, 0.91f to 0.16f,
            0.94f to 0.84f, 0.38f to 0.9f
        )
        stars.forEachIndexed { index, point ->
            val radius = if (index % 3 == 0) 2.1f else 1.15f
            val alpha = if (index % 3 == 0) 0.8f * shimmer else 0.42f
            if (index % 3 == 0) {
                drawCircle(
                    color = Color(0xFF51FAC1).copy(alpha = 0.12f * shimmer),
                    radius = radius * 4f,
                    center = androidx.compose.ui.geometry.Offset(size.width * point.first, size.height * point.second)
                )
            }
            drawCircle(
                color = if (index % 4 == 0) Color(0xFF51FAC1).copy(alpha = alpha) else Color.White.copy(alpha = alpha),
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(size.width * point.first, size.height * point.second)
            )
        }
    }
}

@Composable
private fun ProfileHeader(userName: String, earnedStars: Int, isEnglish: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isEnglish) "LEARNER PROFILE" else "HỒ SƠ NGƯỜI HỌC",
                color = Color(0xFF51FAC1),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(userName, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text(
                if (isEnglish) "Keep learning, keep growing" else "Tiếp tục học hỏi và phát triển",
                color = Color(0xB3FFFFFF),
                fontSize = 11.sp
            )
        }
        Box(
            modifier = Modifier
                .width(74.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x66191438))
                .border(1.dp, Color(0x995C3DA0), RoundedCornerShape(18.dp))
                .padding(vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(19.dp))
                Text("$earnedStars", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (isEnglish) "STARS" else "SAO", color = Color(0xCCFFFFFF), fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun ProfileIdentityCard(
    userName: String,
    rankTitle: String,
    level: Int,
    xp: Int,
    xpProgress: Float,
    xpToNextLevel: Int,
    avatarUri: String?,
    studyMotto: String,
    isEnglish: Boolean,
    onEditClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "profile_avatar_glow")
    val pulse by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "profile_avatar_scale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0x8C14142B))
            .border(1.dp, Color(0x995C3DA0), RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        IconButton(
            onClick = onEditClick,
            modifier = Modifier.align(Alignment.TopEnd).size(34.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color(0xFF51FAC1), modifier = Modifier.size(18.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(94.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.matchParentSize().scale(pulse).blur(18.dp).background(Color(0x6651FAC1), CircleShape))
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(Color(0xFF14141E))
                        .border(3.dp, Color(0xFFFFD166), CircleShape)
                        .padding(4.dp)
                ) {
                    Image(
                        painter = painterResource(resolveAvatarResource(avatarUri)),
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(29.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFF9E00), Color(0xFFFFD166))))
                        .border(1.5.dp, Color(0xFF14141E), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("$level", color = Color(0xFF1F1B14), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
            }
            Spacer(modifier = Modifier.height(9.dp))
            Text(userName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(rankTitle, color = Color(0xFF51FAC1), fontSize = 13.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (studyMotto.isNotBlank()) "\"$studyMotto\"" else (if (isEnglish) "\"Keep learning, keep growing\"" else "\"Tiếp tục học hỏi và phát triển\""),
                color = Color(0xB3FFFFFF),
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(15.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (isEnglish) "Level $level progress" else "Tiến độ cấp $level", color = Color(0xCCFFFFFF), fontSize = 11.sp)
                Text("$xp XP", color = Color(0xFFFFD166), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(7.dp))
            Box(modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape).background(Color(0x442B2940))) {
                Box(modifier = Modifier.fillMaxWidth(xpProgress).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF51FAC1), Color(0xFFFFD166)))))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (isEnglish) "$xpToNextLevel XP to the next level" else "Còn $xpToNextLevel XP để lên cấp tiếp theo",
                color = Color(0x99FFFFFF),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentAvatar: String?,
    currentMotto: String,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onSave: (newName: String, newAvatar: String?, newMotto: String?) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var motto by remember { mutableStateOf(currentMotto) }
    var selectedAvatar by remember { mutableStateOf(currentAvatar ?: "kiki_icon") }

    val avatars = listOf(
        "kiki_icon" to (if (isEnglish) "Hero" else "Mặc định"),
        "kiki_mascot_head" to (if (isEnglish) "Explorer" else "Thám hiểm"),
        "companion_mascot" to (if (isEnglish) "Scholar" else "Học giả"),
        "kiki_hero_intro" to (if (isEnglish) "Galactic" else "Vũ trụ"),
        "kiki_hero_auth" to (if (isEnglish) "Knight" else "Hiệp sĩ")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181329),
        title = {
            Text(
                if (isEnglish) "Edit Profile & Avatar" else "Chỉnh Sửa Hồ Sơ & Avatar",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isEnglish) "Display Name" else "Tên hiển thị") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF51FAC1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = motto,
                    onValueChange = { motto = it },
                    label = { Text(if (isEnglish) "Study Motto / Bio" else "Châm ngôn học tập") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF51FAC1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    if (isEnglish) "Choose Your Kiki Avatar" else "Chọn Avatar Kiki",
                    color = Color(0xFF51FAC1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    avatars.forEach { (avatarKey, label) ->
                        val isSelected = selectedAvatar == avatarKey
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedAvatar = avatarKey }
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF14141E))
                                    .border(
                                        2.dp,
                                        if (isSelected) Color(0xFF51FAC1) else Color(0x33FFFFFF),
                                        CircleShape
                                    )
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(resolveAvatarResource(avatarKey)),
                                    contentDescription = label,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF51FAC1)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0D0A1A), modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFF51FAC1) else Color(0x88FFFFFF),
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, selectedAvatar, motto)
                    }
                },
                enabled = name.isNotBlank(),
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

@Composable
private fun ProfileStatCard(modifier: Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(Color(0x7614142B))
            .border(1.dp, Color(0x665B39A2), RoundedCornerShape(19.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, color = Color(0xB3FFFFFF), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun ProfileJourneyCard(
    currentChapterTitle: String,
    currentNode: Int,
    completedStages: Int,
    stageCount: Int,
    progress: Float,
    isEnglish: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x7614142B))
            .border(1.dp, Color(0x665B39A2), RoundedCornerShape(22.dp))
            .padding(15.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isEnglish) "Current journey" else "Hành trình hiện tại", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(currentChapterTitle, color = Color(0xB3FFFFFF), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("${(progress * 100).toInt()}%", color = Color(0xFF51FAC1), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(11.dp))
            Box(modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color(0x442B2940))) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(Color(0xFF51FAC1)))
            }
            Spacer(modifier = Modifier.height(7.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (isEnglish) "Lesson ${currentNode.coerceIn(1, 5)}/5" else "Bài ${currentNode.coerceIn(1, 5)}/5",
                    color = Color(0x99FFFFFF), fontSize = 10.sp
                )
                Text(
                    if (isEnglish) "$completedStages/$stageCount chapters completed" else "$completedStages/$stageCount chương hoàn thành",
                    color = Color(0x99FFFFFF), fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileAchievements(
    completedLessons: Int,
    streak: Int,
    allStagesCompleted: Boolean,
    isEnglish: Boolean
) {
    val badges = listOf(
        Triple(Icons.Default.Flag, if (isEnglish) "First step" else "Bước đầu", completedLessons > 0),
        Triple(Icons.Default.LocalFireDepartment, if (isEnglish) "On fire" else "Bền bỉ", streak >= 3),
        Triple(Icons.Default.Diamond, if (isEnglish) "Mastery" else "Tinh thông", allStagesCompleted)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x7614142B))
            .border(1.dp, Color(0x665B39A2), RoundedCornerShape(22.dp))
            .padding(15.dp)
    ) {
        Column {
            Text(if (isEnglish) "Achievements" else "Thành tựu", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                badges.forEach { (icon, title, unlocked) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(86.dp)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (unlocked) Color(0x334EF3C5) else Color(0x221F1D2F))
                                .border(1.dp, if (unlocked) Color(0xFF51FAC1) else Color(0x445E5876), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = title, tint = if (unlocked) Color(0xFF51FAC1) else Color(0x667C748D), modifier = Modifier.size(23.dp))
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(title, color = if (unlocked) Color.White else Color(0x667C748D), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSettingsCard(
    strings: AppStrings,
    isEnglish: Boolean,
    selectedLevel: String,
    soundEnabled: Boolean,
    notificationsEnabled: Boolean,
    onSelectLevelClick: () -> Unit,
    onLanguageChanged: (Boolean) -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xA81B1238), Color(0x950A1F32))))
            .border(1.dp, Color(0x665B39A2), RoundedCornerShape(22.dp))
            .padding(15.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 120.dp, y = (-50).dp)
                .size(180.dp)
                .blur(42.dp)
                .background(Color(0x443D0B78), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 40.dp)
                .size(160.dp)
                .blur(45.dp)
                .background(Color(0x3330A5C8), CircleShape)
        )
        GalaxyEffectOverlay(modifier = Modifier.matchParentSize())
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(19.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEnglish) "Settings" else strings.settingsTitle, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            ProfileLevelSelectionRow(
                title = strings.selectLevelTitle,
                currentLevel = selectedLevel,
                onClick = onSelectLevelClick
            )
            Spacer(modifier = Modifier.height(10.dp))
            ProfileSettingRow(strings.languageLabel, Icons.Default.Language, isEnglish, onLanguageChanged)
            Spacer(modifier = Modifier.height(8.dp))
            ProfileSettingRow(strings.soundEffects, Icons.AutoMirrored.Filled.VolumeUp, soundEnabled, onSoundChanged)
            Spacer(modifier = Modifier.height(8.dp))
            ProfileSettingRow(strings.notifications, Icons.Default.Notifications, notificationsEnabled, onNotificationsChanged)
        }
    }
}

@Composable
private fun ProfileLevelSelectionRow(title: String, currentLevel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x14FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0x2651FAC1)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.School, contentDescription = title, tint = Color(0xFF51FAC1), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = Color.White, fontSize = 13.sp)
                Text(currentLevel, color = Color(0xFFFFD166), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color(0x66FFFFFF), modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun ProfileSettingRow(title: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0x1AFFFFFF)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, color = Color.White, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF14141E),
                checkedTrackColor = Color(0xFF51FAC1),
                uncheckedThumbColor = Color(0xFF8D839F),
                uncheckedTrackColor = Color(0x332B2940)
            )
        )
    }
}

@Composable
fun LevelSelectionDialog(
    currentLevel: String,
    isEnglish: Boolean,
    onLevelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val levels = if (isEnglish) {
        listOf(
            "Beginner" to "Foundations & Essentials",
            "Intermediate" to "Conversational & Grammar",
            "Advanced" to "Fluency & Academic",
            "Middle School" to "Junior High Standard",
            "High School" to "Exam & College Prep",
            "University / College" to "Mastery & Professional"
        )
    } else {
        listOf(
            "Beginner" to "Sơ cấp - Nền tảng & Từ vựng căn bản",
            "Intermediate" to "Trung cấp - Giao tiếp & Ngữ pháp",
            "Advanced" to "Cao cấp - Lưu loát & Học thuật",
            "Middle School" to "Cấp 2 - Kiến thức THCS chuẩn",
            "High School" to "Cấp 3 - Luyện thi THPT & Đại học",
            "University / College" to "Đại học - Chuyên sâu & Chứng chỉ"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14142B),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isEnglish) "Select English Level" else "Chọn Cấp Độ Tiếng Anh",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                levels.forEach { (lvl, desc) ->
                    val isSelected = currentLevel == lvl
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0x3351FAC1) else Color(0x14FFFFFF))
                            .border(1.dp, if (isSelected) Color(0xFF51FAC1) else Color(0x26FFFFFF), RoundedCornerShape(14.dp))
                            .clickable { onLevelSelected(lvl) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(lvl, color = if (isSelected) Color(0xFF51FAC1) else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(desc, color = Color(0x99FFFFFF), fontSize = 11.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEnglish) "Close" else "Đóng", color = Color(0xFF51FAC1))
            }
        }
    )
}
