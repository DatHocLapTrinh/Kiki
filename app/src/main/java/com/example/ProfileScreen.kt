package com.example

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.audio.SoundEffectManager
import com.example.model.AchievementBadge
import com.example.model.AchievementRegistry
import com.example.model.BadgeId
import com.example.notification.KikiDailyReminderScheduler
import coil.compose.AsyncImage
import com.example.ui.VocabularyVaultDialog
import com.example.ui.KikiVideoCallDialog
import com.example.ui.PronunciationPracticeDialog
import com.example.ui.WeakPointsClinicDialog
import com.example.ui.AddWordDialog
import com.example.sqlite.room.VocabularyEntity
import com.example.sqlite.room.WeakPointEntity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import com.example.viewmodel.StudyViewModel

fun resolveAvatarResource(avatarUri: String?): Int {
    return when (avatarUri) {
        "kiki_welcome" -> R.drawable.kiki_welcome
        "kiki_happy", "kiki_mascot_head", "kiki_state_happy" -> R.drawable.kiki_happy
        "kiki_sad" -> R.drawable.kiki_sad
        "kiki_speaker" -> R.drawable.kiki_speaker
        else -> R.drawable.kiki_icon
    }
}

enum class KikiExplorerSection {
    HUB,
    PROFILE,
    VOCABULARY,
    MISTAKES,
    ACHIEVEMENTS,
    SETTINGS
}

@Composable
fun ProfileScreen(
    viewModel: StudyViewModel, 
    onLogout: () -> Unit = {},
    onNavigateToRoadmap: () -> Unit = {},
    onNavigateToShop: () -> Unit = {}
) {
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

    val streakShields by viewModel.streakShields.observeAsState(2)
    val weakPoints by viewModel.weakPoints.observeAsState(emptyList())
    val unlockedBadgeIds by viewModel.unlockedBadgeIds.observeAsState(emptySet())
    val curedWeakPoints by viewModel.curedWeakPointsCount.observeAsState(0)
    val masteredVocabCount by viewModel.masteredVocabCount.observeAsState(0)
    var selectedBadgeForDetail by remember { mutableStateOf<AchievementBadge?>(null) }
    var showClinicDialog by remember { mutableStateOf(false) }

    val settings = remember(userId) {
        context.getSharedPreferences(SoundEffectManager.PREFS_NAME, android.content.Context.MODE_PRIVATE)
    }
    val selectedLevel by viewModel.selectedLevel.observeAsState("Beginner")
    val batterySaver by viewModel.batterySaver.observeAsState(false)
    var soundEnabled by remember(userId) { mutableStateOf(settings.getBoolean("sound_enabled", true)) }
    var notificationsEnabled by remember(userId) { mutableStateOf(settings.getBoolean("notifications_enabled", true)) }
    var hapticEnabled by remember(userId) { mutableStateOf(settings.getBoolean("haptic_enabled", true)) }
    var dailyGoalXp by remember(userId) { mutableStateOf(settings.getInt("daily_goal_xp", 20)) }
    var showDailyGoalDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showVideoCall by remember { mutableStateOf(false) }
    var showPronouncePractice by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLevelDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showVaultDialog by remember { mutableStateOf(false) }
    var showAddWordDialog by remember { mutableStateOf(false) }
    var showNavDrawer by remember { mutableStateOf(false) }
    val currentSection by viewModel.kikiExplorerSection.observeAsState(KikiExplorerSection.HUB)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(userId) {
        val savedLanguage = settings.getBoolean("english", false)
        if (savedLanguage != isEnglish) viewModel.setEnglish(savedLanguage)
        if (userId != -1L) viewModel.refreshBadges(userId)
    }

    val allBadges = remember(unlockedBadgeIds, streak, curedWeakPoints, masteredVocabCount, completedLessons, journeyStages) {
        AchievementRegistry.getAllBadges(
            unlockedIds = unlockedBadgeIds,
            streak = streak,
            curedWeakPoints = curedWeakPoints,
            masteredVocabCount = masteredVocabCount,
            completedLessons = completedLessons,
            hasNightOwl = unlockedBadgeIds.contains(BadgeId.NIGHT_OWL.id),
            hasSpeakingAce = unlockedBadgeIds.contains(BadgeId.SPEAKING_ACE.id),
            allStagesCompleted = journeyStages.isNotEmpty() && journeyStages.all { it.state == JourneyStageState.COMPLETED }
        )
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
                .padding(bottom = 150.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentSection != KikiExplorerSection.HUB) {
                ProfileHeader(
                    userName = userName,
                    earnedStars = earnedStars,
                    isEnglish = isEnglish,
                    currentSection = currentSection,
                    onBackClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        viewModel.setKikiExplorerSection(KikiExplorerSection.PROFILE)
                    },
                    onSettingsClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        viewModel.setKikiExplorerSection(KikiExplorerSection.SETTINGS)
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Animated Content for selected section
            AnimatedContent(
                targetState = currentSection,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
                label = "kiki_explorer_section"
            ) { section ->
                when (section) {
                    KikiExplorerSection.HUB -> {
                        KikiExplorerHubView(
                            userName = userName,
                            rankTitle = rankTitle,
                            level = level,
                            avatarUri = avatarUri,
                            earnedStars = earnedStars,
                            vocabCount = vocabularyList.size,
                            isEnglish = isEnglish,
                            onNavigateProfile = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.setKikiExplorerSection(KikiExplorerSection.PROFILE)
                            },
                            onNavigateVocab = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.setKikiExplorerSection(KikiExplorerSection.VOCABULARY)
                            },
                            onSettingsClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.setKikiExplorerSection(KikiExplorerSection.SETTINGS)
                            },
                            onNavigateToShop = onNavigateToShop,
                            onLogoutClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showLogoutDialog = true
                            }
                        )
                    }
                    KikiExplorerSection.PROFILE -> {
                        ProfileSectionView(
                            userName = userName,
                            rankTitle = rankTitle,
                            level = level,
                            xp = xp,
                            xpProgress = xpProgress,
                            xpToNextLevel = xpToNextLevel,
                            avatarUri = avatarUri,
                            studyMotto = studyMotto,
                            isEnglish = isEnglish,
                            streak = streak,
                            streakShields = streakShields,
                            completedLessons = completedLessons,
                            totalLessons = totalLessons,
                            earnedStars = earnedStars,
                            selectedLevel = selectedLevel,
                            allBadges = allBadges,
                            unlockedBadgeCount = unlockedBadgeIds.size,
                            onBadgeClick = { badge ->
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                selectedBadgeForDetail = badge
                            },
                            onEditClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showEditProfileDialog = true
                            },
                            onNavigateToRoadmap = onNavigateToRoadmap,
                            onOpenLevelDialog = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showLevelDialog = true
                            },
                            onOpenSettings = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.setKikiExplorerSection(KikiExplorerSection.SETTINGS)
                            },
                            onLogoutClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showLogoutDialog = true
                            }
                        )
                    }
                    KikiExplorerSection.VOCABULARY -> {
                        ProfileVocabularySection(
                            vocabularyList = vocabularyList,
                            masteredCount = masteredVocabCount,
                            isEnglish = isEnglish,
                            onOpenFlashcards = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showVaultDialog = true
                            },
                            onAddWord = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showAddWordDialog = true
                            },
                            onSpeakWord = { word ->
                                viewModel.ttsManager.speak(word)
                            },
                            onToggleMastered = { vocabId, isMastered ->
                                viewModel.toggleVocabMastered(vocabId, isMastered)
                            },
                            onDeleteWord = { vocabId ->
                                viewModel.deleteVocab(vocabId)
                            }
                        )
                    }
                    KikiExplorerSection.MISTAKES -> {
                        ProfileMistakesSection(
                            weakPoints = weakPoints,
                            curedCount = curedWeakPoints,
                            isEnglish = isEnglish,
                            onStartClinic = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showClinicDialog = true
                            },
                            onResolveSingle = { weakId ->
                                viewModel.resolveWeakPoint(weakId)
                            }
                        )
                    }
                    KikiExplorerSection.ACHIEVEMENTS -> {
                        ProfileAchievementsSection(
                            allBadges = allBadges,
                            unlockedCount = unlockedBadgeIds.size,
                            earnedStars = earnedStars,
                            isEnglish = isEnglish,
                            onBadgeClick = { badge ->
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                selectedBadgeForDetail = badge
                            }
                        )
                    }
                    KikiExplorerSection.SETTINGS -> {
                        ProfileSettingsSectionView(
                            isEnglish = isEnglish,
                            selectedLevel = selectedLevel,
                            dailyGoalXp = dailyGoalXp,
                            soundEnabled = soundEnabled,
                            hapticEnabled = hapticEnabled,
                            batterySaver = batterySaver,
                            notificationsEnabled = notificationsEnabled,
                            onLanguageChanged = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                settings.edit().putBoolean("english", it).apply()
                                viewModel.setEnglish(it)
                            },
                            onSelectLevelClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showLevelDialog = true
                            },
                            onSelectDailyGoalClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showDailyGoalDialog = true
                            },
                            onSoundChanged = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                soundEnabled = it
                                settings.edit().putBoolean("sound_enabled", it).apply()
                            },
                            onHapticChanged = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                hapticEnabled = it
                                settings.edit().putBoolean("haptic_enabled", it).apply()
                            },
                            onBatterySaverChanged = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.setBatterySaver(it)
                            },
                            onNotificationsChanged = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                notificationsEnabled = it
                                settings.edit().putBoolean("notifications_enabled", it).apply()
                                if (it) {
                                    KikiDailyReminderScheduler.scheduleDailyReminder(context)
                                } else {
                                    KikiDailyReminderScheduler.cancelDailyReminder(context)
                                }
                            },
                            onPrivacyClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showPrivacyDialog = true
                            },
                            onLogoutClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showLogoutDialog = true
                            }
                        )
                    }
                }
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

    if (showDailyGoalDialog) {
        DailyGoalDialog(
            currentGoal = dailyGoalXp,
            isEnglish = isEnglish,
            onGoalSelected = { newGoal ->
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                dailyGoalXp = newGoal
                settings.edit().putInt("daily_goal_xp", newGoal).apply()
                android.widget.Toast.makeText(
                    context,
                    if (isEnglish) "Daily goal set to $newGoal XP" else "Đã đặt mục tiêu: $newGoal XP / ngày",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                showDailyGoalDialog = false
            },
            onDismiss = { showDailyGoalDialog = false }
        )
    }

    if (showPrivacyDialog) {
        PrivacyAndTermsDialog(
            isEnglish = isEnglish,
            onDismiss = { showPrivacyDialog = false }
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

    if (showClinicDialog) {
        WeakPointsClinicDialog(
            viewModel = viewModel,
            isEnglish = isEnglish,
            onDismiss = { showClinicDialog = false }
        )
    }

    if (showVideoCall) {
        KikiVideoCallDialog(
            viewModel = viewModel,
            onDismiss = { showVideoCall = false }
        )
    }

    if (showPronouncePractice) {
        PronunciationPracticeDialog(
            viewModel = viewModel,
            onDismiss = { showPronouncePractice = false }
        )
    }

    if (selectedBadgeForDetail != null) {
        AchievementDetailDialog(
            badge = selectedBadgeForDetail,
            isEnglish = isEnglish,
            onDismiss = { selectedBadgeForDetail = null }
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

    if (showSettingsSheet) {
        ProfileSettingsBottomSheet(
            isEnglish = isEnglish,
            selectedLevel = selectedLevel,
            dailyGoalXp = dailyGoalXp,
            soundEnabled = soundEnabled,
            hapticEnabled = hapticEnabled,
            batterySaver = batterySaver,
            notificationsEnabled = notificationsEnabled,
            onDismiss = { showSettingsSheet = false },
            onLanguageChanged = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                settings.edit().putBoolean("english", it).apply()
                viewModel.setEnglish(it)
            },
            onSelectLevelClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                showLevelDialog = true
            },
            onSelectDailyGoalClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                showDailyGoalDialog = true
            },
            onSoundChanged = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                soundEnabled = it
                settings.edit().putBoolean("sound_enabled", it).apply()
            },
            onHapticChanged = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                hapticEnabled = it
                settings.edit().putBoolean("haptic_enabled", it).apply()
            },
            onBatterySaverChanged = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                viewModel.setBatterySaver(it)
            },
            onNotificationsChanged = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                notificationsEnabled = it
                settings.edit().putBoolean("notifications_enabled", it).apply()
                if (it) {
                    KikiDailyReminderScheduler.scheduleDailyReminder(context)
                } else {
                    KikiDailyReminderScheduler.cancelDailyReminder(context)
                }
            },
            onPrivacyClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                showPrivacyDialog = true
            },
            onLogoutClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                showSettingsSheet = false
                showLogoutDialog = true
            }
        )
    }

    if (showNavDrawer) {
        KikiExplorerNavigationSheet(
            currentSection = currentSection,
            userName = userName,
            level = level,
            rankTitle = rankTitle,
            avatarUri = avatarUri,
            vocabCount = vocabularyList.size,
            isEnglish = isEnglish,
            onNavigateProfile = {
                showNavDrawer = false
                viewModel.setKikiExplorerSection(KikiExplorerSection.PROFILE)
            },
            onNavigateVocab = {
                showNavDrawer = false
                viewModel.setKikiExplorerSection(KikiExplorerSection.VOCABULARY)
            },
            onSettingsClick = {
                showNavDrawer = false
                showSettingsSheet = true
            },
            onLogoutClick = {
                showNavDrawer = false
                showLogoutDialog = true
            },
            onDismiss = { showNavDrawer = false }
        )
    }

    if (showAddWordDialog) {
        AddWordDialog(
            isEnglish = isEnglish,
            viewModel = viewModel,
            onDismiss = { showAddWordDialog = false },
            onSave = { word, phonetic, meaning, example ->
                viewModel.saveWord(word, phonetic, meaning, example)
                showAddWordDialog = false
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
private fun ProfileHeader(
    userName: String,
    earnedStars: Int,
    isEnglish: Boolean,
    currentSection: KikiExplorerSection,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (currentSection != KikiExplorerSection.PROFILE && currentSection != KikiExplorerSection.HUB) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF51FAC1),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF51FAC1))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "KIKI EXPLORER",
                    color = Color(0xFF51FAC1),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when (currentSection) {
                    KikiExplorerSection.HUB -> "Kiki Explorer"
                    KikiExplorerSection.PROFILE -> userName
                    KikiExplorerSection.VOCABULARY -> if (isEnglish) "Vocabulary & Grammar" else "Từ Vựng & Ngữ Pháp"
                    KikiExplorerSection.ACHIEVEMENTS -> if (isEnglish) "Cosmic Titles & Badges" else "Danh Hiệu & Cột Mốc"
                    KikiExplorerSection.MISTAKES -> if (isEnglish) "Mistake Clinic" else "Phòng Khám Lỗi Sai"
                    KikiExplorerSection.SETTINGS -> if (isEnglish) "Settings & Preferences" else "Cài Đặt & Tuỳ Chọn"
                },
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when (currentSection) {
                    KikiExplorerSection.HUB -> if (isEnglish) "Galactic exploration menu" else "Danh mục khám phá vũ trụ"
                    KikiExplorerSection.PROFILE -> if (isEnglish) "Cosmic Profile & Milestones" else "Hồ sơ, danh hiệu & cột mốc"
                    KikiExplorerSection.VOCABULARY -> if (isEnglish) "Saved vocabulary, 3D cards & grammar tips" else "Sổ từ vựng, thẻ 3D & mẹo ngữ pháp"
                    KikiExplorerSection.ACHIEVEMENTS -> if (isEnglish) "Cosmic glory milestones & unlocked badges" else "Huy hiệu vũ trụ & sao tinh tú tích lũy"
                    KikiExplorerSection.MISTAKES -> if (isEnglish) "Cure errors & earn bonus XP" else "Củng cố điểm yếu & nhận điểm thưởng"
                    KikiExplorerSection.SETTINGS -> if (isEnglish) "Sound effects & account logout" else "Âm thanh & đăng xuất tài khoản"
                },
                color = Color(0xB3FFFFFF),
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Star Badge Box
            Box(
                modifier = Modifier
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x66191438))
                    .border(1.dp, Color(0x995C3DA0), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(17.dp))
                    Text("$earnedStars", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (isEnglish) "STARS" else "SAO", color = Color(0xCCFFFFFF), fontSize = 7.5.sp)
                }
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
                    if (avatarUri != null && (avatarUri.startsWith("http://") || avatarUri.startsWith("https://") || avatarUri.startsWith("content://"))) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = resolveAvatarResource(avatarUri),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    }
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
        "kiki_welcome" to (if (isEnglish) "Welcome" else "Chào đón"),
        "kiki_happy" to (if (isEnglish) "Victory" else "Ăn mừng"),
        "kiki_speaker" to (if (isEnglish) "Speaker" else "Diễn thuyết"),
        "kiki_sad" to (if (isEnglish) "Empathetic" else "Đồng cảm"),
        "kiki_icon" to (if (isEnglish) "Scholar" else "Học giả")
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

                if (currentAvatar != null && (currentAvatar.startsWith("http://") || currentAvatar.startsWith("https://"))) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedAvatar == currentAvatar) Color(0x3351FAC1) else Color(0x14FFFFFF))
                            .border(1.dp, if (selectedAvatar == currentAvatar) Color(0xFF51FAC1) else Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { selectedAvatar = currentAvatar }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (selectedAvatar == currentAvatar) Color(0xFF51FAC1) else Color(0x33FFFFFF), CircleShape)
                        ) {
                            AsyncImage(
                                model = currentAvatar,
                                contentDescription = "Google Avatar",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "Google Profile Picture" else "Ảnh đại diện Google",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (selectedAvatar == currentAvatar) (if (isEnglish) "Selected" else "Đang chọn") else (if (isEnglish) "Tap to select" else "Chạm để dùng lại"),
                                color = if (selectedAvatar == currentAvatar) Color(0xFF51FAC1) else Color(0x88FFFFFF),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

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
                                AsyncImage(
                                    model = resolveAvatarResource(avatarKey),
                                    contentDescription = label,
                                    contentScale = ContentScale.Fit,
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
private fun ProfileClusterHeader(title: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = Color(0xB3FFFFFF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ProfileCompactMetricTile(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0x991C142E), Color(0x77120D22))))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(11.dp))
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = title,
                    color = Color(0xB3FFFFFF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = accentColor.copy(alpha = 0.9f),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileCompactActionCard(
    modifier: Modifier = Modifier,
    title: String,
    badgeText: String,
    actionText: String,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    accentColor: Color,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(gradientColors))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (iconRes != null) {
                            Image(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        } else if (icon != null) {
                            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = badgeText,
                color = Color(0xCCFFFFFF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
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
    allBadges: List<AchievementBadge>,
    isEnglish: Boolean,
    onBadgeClick: (AchievementBadge) -> Unit
) {
    val unlockedCount = allBadges.count { it.isUnlocked }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x7614142B))
            .border(1.dp, Color(0x665B39A2), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFFFFD166),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEnglish) "Mastery Badges" else "Bộ Sưu Tập Thành Tựu",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FFD166))
                        .border(1.dp, Color(0x66FFD166), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$unlockedCount/${allBadges.size}",
                        color = Color(0xFFFFD166),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(allBadges.size, key = { allBadges[it].id }) { index ->
                    val badge = allBadges[index]
                    val icon = when (badge.iconKey) {
                        "NIGHT_OWL" -> Icons.Default.Bedtime
                        "CHIEF_DOCTOR" -> Icons.Default.Healing
                        "SUPER_MEMORY" -> Icons.Default.Psychology
                        "PERSISTENT_SCHOLAR" -> Icons.Default.LocalFireDepartment
                        "FIRST_STEP" -> Icons.Default.Flag
                        "SPEAKING_ACE" -> Icons.Default.RecordVoiceOver
                        else -> Icons.Default.Diamond
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(82.dp)
                            .clickable { onBadgeClick(badge) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    if (badge.isUnlocked) Brush.radialGradient(
                                        listOf(Color(0x44FFD166), Color(0x221F1D2F))
                                    ) else Brush.radialGradient(
                                        listOf(Color(0x221F1D2F), Color(0x111F1D2F))
                                    )
                                )
                                .border(
                                    1.5.dp,
                                    if (badge.isUnlocked) Color(0xFFFFD166) else Color(0x445E5876),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = if (isEnglish) badge.titleEn else badge.titleVi,
                                tint = if (badge.isUnlocked) Color(0xFFFFD166) else Color(0x667C748D),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isEnglish) badge.titleEn else badge.titleVi,
                            color = if (badge.isUnlocked) Color.White else Color(0x667C748D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (badge.isUnlocked) (if (isEnglish) "Unlocked" else "Đã mở") else "${badge.currentProgress}/${badge.targetCount}",
                            color = if (badge.isUnlocked) Color(0xFF51FAC1) else Color(0x99FFD166),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementDetailDialog(
    badge: AchievementBadge?,
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    if (badge == null) return
    val icon = when (badge.iconKey) {
        "NIGHT_OWL" -> Icons.Default.Bedtime
        "CHIEF_DOCTOR" -> Icons.Default.Healing
        "SUPER_MEMORY" -> Icons.Default.Psychology
        "PERSISTENT_SCHOLAR" -> Icons.Default.LocalFireDepartment
        "FIRST_STEP" -> Icons.Default.Flag
        "SPEAKING_ACE" -> Icons.Default.RecordVoiceOver
        else -> Icons.Default.Diamond
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141224),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (badge.isUnlocked) Color(0x33FFD166) else Color(0x22444455))
                        .border(1.dp, if (badge.isUnlocked) Color(0xFFFFD166) else Color(0x44888899), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (badge.isUnlocked) Color(0xFFFFD166) else Color(0x66888899),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isEnglish) badge.titleEn else badge.titleVi,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (badge.isUnlocked) (if (isEnglish) "✨ Unlocked" else "✨ Đã mở khóa") else (if (isEnglish) "🔒 Locked" else "🔒 Chưa mở khóa"),
                        color = if (badge.isUnlocked) Color(0xFF51FAC1) else Color(0xFFFF9E00),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = if (isEnglish) badge.descriptionEn else badge.descriptionVi,
                    color = Color(0xFFDDDDDD),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEnglish) "Progress:" else "Tiến độ:",
                        color = Color(0x99FFFFFF),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${badge.currentProgress} / ${badge.targetCount}",
                        color = if (badge.isUnlocked) Color(0xFF51FAC1) else Color(0xFFFFD166),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { badge.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (badge.isUnlocked) Color(0xFF51FAC1) else Color(0xFFFFD166),
                    trackColor = Color(0x33FFFFFF)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEnglish) "Close" else "Đóng", color = Color(0xFF51FAC1), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ProfileSettingsSectionView(
    isEnglish: Boolean,
    selectedLevel: String,
    dailyGoalXp: Int,
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    batterySaver: Boolean,
    notificationsEnabled: Boolean,
    onLanguageChanged: (Boolean) -> Unit,
    onSelectLevelClick: () -> Unit,
    onSelectDailyGoalClick: () -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onHapticChanged: (Boolean) -> Unit,
    onBatterySaverChanged: (Boolean) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onPrivacyClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
        ProfileSettingsContent(
            isEnglish = isEnglish,
            selectedLevel = selectedLevel,
            dailyGoalXp = dailyGoalXp,
            soundEnabled = soundEnabled,
            hapticEnabled = hapticEnabled,
            batterySaver = batterySaver,
            notificationsEnabled = notificationsEnabled,
            onLanguageChanged = onLanguageChanged,
            onSelectLevelClick = onSelectLevelClick,
            onSelectDailyGoalClick = onSelectDailyGoalClick,
            onSoundChanged = onSoundChanged,
            onHapticChanged = onHapticChanged,
            onBatterySaverChanged = onBatterySaverChanged,
            onNotificationsChanged = onNotificationsChanged,
            onPrivacyClick = onPrivacyClick,
            onLogoutClick = onLogoutClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSettingsBottomSheet(
    isEnglish: Boolean,
    selectedLevel: String,
    dailyGoalXp: Int,
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    batterySaver: Boolean,
    notificationsEnabled: Boolean,
    onDismiss: () -> Unit,
    onLanguageChanged: (Boolean) -> Unit,
    onSelectLevelClick: () -> Unit,
    onSelectDailyGoalClick: () -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onHapticChanged: (Boolean) -> Unit,
    onBatterySaverChanged: (Boolean) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onPrivacyClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF130E26),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0x6651FAC1))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x3351FAC1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isEnglish) "Settings & Preferences" else "Cài Đặt & Tuỳ Chọn",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEnglish) "Customize your study experience" else "Tùy chỉnh trải nghiệm học tập của bạn",
                            color = Color(0x88FFFFFF),
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            ProfileSettingsContent(
                isEnglish = isEnglish,
                selectedLevel = selectedLevel,
                dailyGoalXp = dailyGoalXp,
                soundEnabled = soundEnabled,
                hapticEnabled = hapticEnabled,
                batterySaver = batterySaver,
                notificationsEnabled = notificationsEnabled,
                onLanguageChanged = onLanguageChanged,
                onSelectLevelClick = onSelectLevelClick,
                onSelectDailyGoalClick = onSelectDailyGoalClick,
                onSoundChanged = onSoundChanged,
                onHapticChanged = onHapticChanged,
                onBatterySaverChanged = onBatterySaverChanged,
                onNotificationsChanged = onNotificationsChanged,
                onPrivacyClick = onPrivacyClick,
                onLogoutClick = onLogoutClick
            )
        }
    }
}

@Composable
private fun ProfileSettingsContent(
    isEnglish: Boolean,
    selectedLevel: String,
    dailyGoalXp: Int,
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    batterySaver: Boolean,
    notificationsEnabled: Boolean,
    onLanguageChanged: (Boolean) -> Unit,
    onSelectLevelClick: () -> Unit,
    onSelectDailyGoalClick: () -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onHapticChanged: (Boolean) -> Unit,
    onBatterySaverChanged: (Boolean) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onPrivacyClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: HỌC TẬP & NGÔN NGỮ
        Text(
            text = if (isEnglish) "LEARNING & LANGUAGE" else "HỌC TẬP & NGÔN NGỮ",
            color = Color(0xFF51FAC1),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF131722))
                .border(1.dp, Color(0x3351FAC1), RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Song ngữ (Bilingual)
                ProfileSettingRow(
                    title = if (isEnglish) "App Language / Song Ngữ" else "Chế Độ Song Ngữ (Bilingual)",
                    subtitle = if (isEnglish) "English 🇬🇧 language enabled" else "Giao diện song ngữ Việt 🇻🇳 - Anh 🇬🇧",
                    icon = Icons.Default.Language,
                    checked = isEnglish,
                    onCheckedChange = onLanguageChanged
                )

                HorizontalDivider(color = Color(0x1A51FAC1), thickness = 0.8.dp)

                // Trình độ học
                ProfileSettingActionRow(
                    title = if (isEnglish) "Learning Level" else "Trình Độ Học Tập",
                    subtitle = if (isEnglish) "Adjust curriculum syllabus" else "Điều chỉnh độ khó giáo trình & câu hỏi",
                    valueBadge = selectedLevel,
                    icon = Icons.Default.School,
                    onClick = onSelectLevelClick
                )

                HorizontalDivider(color = Color(0x1A51FAC1), thickness = 0.8.dp)

                // Mục tiêu hàng ngày
                ProfileSettingActionRow(
                    title = if (isEnglish) "Daily XP Goal" else "Mục Tiêu XP Hàng Ngày",
                    subtitle = if (isEnglish) "Strengthen your cosmic streak" else "Duy trì ngọn lửa Streak mỗi ngày",
                    valueBadge = "$dailyGoalXp XP",
                    icon = Icons.Default.EmojiEvents,
                    onClick = onSelectDailyGoalClick
                )
            }
        }

        // Section 2: ÂM THANH & TRẢI NGHIỆM
        Text(
            text = if (isEnglish) "AUDIO & EXPERIENCE" else "ÂM THANH & TRẢI NGHIỆM",
            color = Color(0xFF51FAC1),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF131722))
                .border(1.dp, Color(0x3351FAC1), RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileSettingRow(
                    title = if (isEnglish) "Sound Effects" else "Hiệu Ứng Âm Thanh",
                    subtitle = if (isEnglish) "Sound cues for answers & rewards" else "Âm thanh phản hồi khi làm bài & nhận sao",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    checked = soundEnabled,
                    onCheckedChange = onSoundChanged
                )

                HorizontalDivider(color = Color(0x1A51FAC1), thickness = 0.8.dp)

                ProfileSettingRow(
                    title = if (isEnglish) "Haptic Feedback" else "Phản Hồi Xúc Giác (Rung)",
                    subtitle = if (isEnglish) "Gentle vibration when interacting" else "Rung nhẹ khi chạm và chọn câu trả lời",
                    icon = Icons.Default.Vibration,
                    checked = hapticEnabled,
                    onCheckedChange = onHapticChanged
                )

                HorizontalDivider(color = Color(0x1A51FAC1), thickness = 0.8.dp)

                ProfileSettingRow(
                    title = if (isEnglish) "Battery Saver Mode" else "Chế Độ Tiết Kiệm Pin",
                    subtitle = if (isEnglish) "Reduce cosmic background animation" else "Giảm hoạt ảnh vũ trụ để tối ưu thời lượng pin",
                    icon = Icons.Default.BatteryChargingFull,
                    checked = batterySaver,
                    onCheckedChange = onBatterySaverChanged
                )
            }
        }

        // Section 3: NHẮC NHỞ & CHẾ ĐỘ TẬP TRUNG
        Text(
            text = if (isEnglish) "REMINDERS & FOCUS" else "NHẮC NHỞ & CHẾ ĐỘ TẬP TRUNG",
            color = Color(0xFF51FAC1),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF131722))
                .border(1.dp, Color(0x3351FAC1), RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileSettingRow(
                    title = if (isEnglish) "Daily Streak Reminders (19:15 & 21:15)" else "Nhắc Nhở Cứu Streak Hàng Ngày",
                    icon = Icons.Default.NotificationsActive,
                    checked = notificationsEnabled,
                    onCheckedChange = { checked ->
                        onNotificationsChanged(checked)
                        if (checked) {
                            com.example.notification.KikiDailyReminderScheduler.scheduleAllReminders(context)
                        } else {
                            com.example.notification.KikiDailyReminderScheduler.cancelAllReminders(context)
                        }
                    }
                )

                HorizontalDivider(color = Color(0x1A51FAC1), thickness = 0.8.dp)

                var focusModeState by remember {
                    mutableStateOf(com.example.util.FocusModeManager.isFocusModeEnabled(context))
                }
                ProfileSettingRow(
                    title = if (isEnglish) "Deep Focus Mode (Silence other apps)" else "Chế Độ Tập Trung (Tắt thông báo app khác)",
                    icon = Icons.Default.NotificationsOff,
                    checked = focusModeState,
                    onCheckedChange = { checked ->
                        focusModeState = checked
                        com.example.util.FocusModeManager.setFocusModeEnabled(context, checked)
                        if (checked && !com.example.util.FocusModeManager.hasDndPermission(context)) {
                            com.example.util.FocusModeManager.openDndSettings(context)
                        }
                    }
                )
            }
        }

        // Section 4: THÔNG TIN & BẢO MẬT
        Text(
            text = if (isEnglish) "ABOUT & PRIVACY" else "THÔNG TIN & BẢO MẬT",
            color = Color(0xFF51FAC1),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF131722))
                .border(1.dp, Color(0x3351FAC1), RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileSettingActionRow(
                    title = if (isEnglish) "App Edition" else "Phiên Bản Ứng Dụng",
                    subtitle = "Kiki Smart Learning Cosmic Edition",
                    valueBadge = "v1.0",
                    icon = Icons.Default.Info,
                    onClick = {}
                )

                HorizontalDivider(color = Color(0x1A51FAC1), thickness = 0.8.dp)

                ProfileSettingActionRow(
                    title = if (isEnglish) "Privacy & Safety Policy" else "Chính Sách & Quyền Riêng Tư",
                    subtitle = if (isEnglish) "How your progress data is secured" else "Bảo vệ thông tin cá nhân & tiến độ học",
                    icon = Icons.Default.Shield,
                    onClick = onPrivacyClick
                )
            }
        }

        // Section 5: TÀI KHOẢN & ĐĂNG XUẤT
        Text(
            text = if (isEnglish) "ACCOUNT" else "TÀI KHOẢN",
            color = Color(0xFF51FAC1),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF131722))
                .border(1.dp, Color(0x44FF5252), RoundedCornerShape(22.dp))
                .clickable(onClick = onLogoutClick)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FF5252)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isEnglish) "Log Out" else "Đăng Xuất Tài Khoản",
                            color = Color(0xFFFF5252),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEnglish) "Safely sign out from current session" else "Thoát tài khoản an toàn khỏi thiết bị",
                            color = Color(0x99FF5252),
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = Color(0x66FF5252),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Kiki Smart Learning • Cosmic Duolingo Edition",
            color = Color(0x44FFFFFF),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun ProfileSettingActionRow(
    title: String,
    subtitle: String? = null,
    valueBadge: String? = null,
    icon: ImageVector,
    iconTint: Color = Color(0xFF51FAC1),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x1A51FAC1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (valueBadge != null) {
                Text(
                    text = valueBadge,
                    color = Color(0xFFFFD166),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0x66FFFFFF),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun ProfileSettingRow(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    subtitle: String? = null,
    iconTint: Color = Color(0xFF51FAC1),
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x1A51FAC1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
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
fun DailyGoalDialog(
    currentGoal: Int,
    isEnglish: Boolean,
    onGoalSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val goals = if (isEnglish) {
        listOf(
            10 to ("Casual" to "10 XP per day • Relaxed pace"),
            20 to ("Regular" to "20 XP per day • Balanced learning"),
            30 to ("Serious" to "30 XP per day • Dedicated learner"),
            50 to ("Intense" to "50 XP per day • Maximum progress")
        )
    } else {
        listOf(
            10 to ("Dễ dàng" to "10 XP / ngày • Tốc độ thư giãn"),
            20 to ("Vừa sức" to "20 XP / ngày • Tốc độ cân bằng chuẩn"),
            30 to ("Chăm chỉ" to "30 XP / ngày • Học tập nghiêm túc"),
            50 to ("Cực gắt" to "50 XP / ngày • Tiến bộ nhanh nhất")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14142B),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isEnglish) "Daily XP Goal" else "Mục Tiêu XP Mỗi Ngày",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isEnglish) "Set a daily target to maintain your cosmic streak and habits:"
                    else "Đặt mục tiêu mỗi ngày để duy trì ngọn lửa Streak và thói quen học:",
                    color = Color(0xB3FFFFFF),
                    fontSize = 12.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                goals.forEach { (xpAmount, pair) ->
                    val (label, desc) = pair
                    val isSelected = currentGoal == xpAmount
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0x3351FAC1) else Color(0x14FFFFFF))
                            .border(1.dp, if (isSelected) Color(0xFF51FAC1) else Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .clickable { onGoalSelected(xpAmount) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, color = if (isSelected) Color(0xFF51FAC1) else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(desc, color = Color(0x99FFFFFF), fontSize = 11.sp)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEnglish) "Done" else "Xong", color = Color(0xFF51FAC1), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun PrivacyAndTermsDialog(
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14142B),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isEnglish) "Privacy & Safety" else "Quyền Riêng Tư & An Toàn",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isEnglish)
                        "Kiki Smart Learning is committed to safeguarding your personal progress data.\n\n• Your voice and learning progress are used solely to personalize spaced-repetition lessons.\n• AI Mentor interactions remain private to your session.\n• Zero third-party data tracking or ads."
                    else
                        "Kiki Smart Learning cam kết bảo vệ dữ liệu học tập cá nhân của bạn.\n\n• Dữ liệu giọng nói và tiến độ bài học chỉ dùng để tối ưu thuật toán ghi nhớ Spaced Repetition.\n• Các phiên tương tác với Gia sư AI Kiki được bảo mật an toàn cho phiên học của bạn.\n• Ứng dụng hoàn toàn không chứa quảng cáo và không chia sẻ dữ liệu cho bên thứ ba.",
                    color = Color(0xCCFFFFFF),
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEnglish) "Understood" else "Đã Hiểu", color = Color(0xFF51FAC1), fontWeight = FontWeight.Bold)
            }
        }
    )
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

@Composable
private fun ProfileSectionTabBar(
    currentSection: KikiExplorerSection,
    onSectionSelect: (KikiExplorerSection) -> Unit,
    vocabCount: Int,
    weakPointsCount: Int,
    badgeCount: Int,
    isEnglish: Boolean
) {
    val items = listOf(
        Triple(KikiExplorerSection.PROFILE, if (isEnglish) "Profile" else "Hồ Sơ", Icons.Default.Person),
        Triple(KikiExplorerSection.VOCABULARY, if (isEnglish) "Vocab & Grammar ($vocabCount)" else "Từ Vựng & Ngữ Pháp ($vocabCount)", Icons.Default.CollectionsBookmark),
        Triple(KikiExplorerSection.ACHIEVEMENTS, if (isEnglish) "Badges ($badgeCount)" else "Danh Hiệu ($badgeCount)", Icons.Default.EmojiEvents),
        Triple(KikiExplorerSection.MISTAKES, if (isEnglish) "Mistakes ($weakPointsCount)" else "Lỗi Sai ($weakPointsCount)", Icons.Default.Healing)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x6615102B))
            .border(1.dp, Color(0x3351FAC1), RoundedCornerShape(16.dp))
            .padding(4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (section, title, icon) ->
            val isSelected = currentSection == section
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) Brush.horizontalGradient(listOf(Color(0xFF003830), Color(0xFF005648)))
                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .border(
                        1.dp,
                        if (isSelected) Color(0xFF51FAC1) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSectionSelect(section) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF51FAC1) else Color(0x88FFFFFF),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = if (isSelected) Color.White else Color(0xAAFFFFFF),
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KikiExplorerNavigationSheet(
    currentSection: KikiExplorerSection,
    userName: String,
    level: Int,
    rankTitle: String,
    avatarUri: String?,
    vocabCount: Int,
    isEnglish: Boolean,
    onNavigateProfile: () -> Unit,
    onNavigateVocab: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF140F26),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0x5551FAC1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            // Header with Mini Profile Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0x662A1B4E), Color(0x44140D2B))))
                    .border(1.dp, Color(0x4451FAC1), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFF51FAC1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                            model = resolveAvatarResource(avatarUri),
                            contentScale = ContentScale.Fit,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kiki Explorer",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF51FAC1).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Lv.$level",
                                color = Color(0xFF51FAC1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = rankTitle,
                            color = Color(0xFFFFD166),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0x99FFFFFF))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isEnglish) "CHOOSE SECTION" else "CHỌN MỤC THAO TÁC",
                color = Color(0xFF51FAC1),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF131722))
                    .border(1.dp, Color(0x3351FAC1), RoundedCornerShape(22.dp))
                    .padding(vertical = 4.dp)
            ) {
                // 1. Hồ sơ (gồm danh hiệu và cột mốc bên trong)
                DuolingoStyleTile(
                    title = if (isEnglish) "Profile & Cosmic Badges" else "Hồ Sơ & Danh Hiệu Cột Mốc",
                    icon = Icons.Default.Person,
                    badgeText = "Lv.$level",
                    onClick = onNavigateProfile
                )
                HorizontalDivider(color = Color(0x1A51FAC1), thickness = 0.8.dp)

                // 2. Từ vựng & Ngữ pháp
                DuolingoStyleTile(
                    title = if (isEnglish) "Vocabulary & Grammar" else "Từ Vựng & Ngữ Pháp",
                    icon = Icons.Default.CollectionsBookmark,
                    badgeText = "$vocabCount từ",
                    onClick = onNavigateVocab
                )
                HorizontalDivider(color = Color(0x1A51FAC1), thickness = 0.8.dp)

                // 3. Cài đặt & Tuỳ chọn
                DuolingoStyleTile(
                    title = if (isEnglish) "Settings & Preferences" else "Cài Đặt & Tuỳ Chọn",
                    icon = Icons.Default.Settings,
                    onClick = onSettingsClick
                )
                HorizontalDivider(color = Color(0x1A51FAC1), thickness = 0.8.dp)

                // 4. Đăng xuất tài khoản
                DuolingoStyleTile(
                    title = if (isEnglish) "Log Out Account" else "Đăng Xuất Tài Khoản",
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    iconBg = Color(0x22FF5252),
                    iconTint = Color(0xFFFF5252),
                    onClick = onLogoutClick
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun KikiNavDrawerItem(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Brush.horizontalGradient(listOf(Color(0xFF003830), Color(0xFF00221E)))
                else Brush.linearGradient(listOf(Color(0x331C1635), Color(0x331C1635)))
            )
            .border(
                1.dp,
                if (isSelected) accentColor else Color(0x22FFFFFF),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (iconRes != null) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (icon != null) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) Color(0xFF51FAC1) else Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFF51FAC1).copy(alpha = 0.25f) else Color(0x22FFFFFF))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    color = if (isSelected) Color(0xFF51FAC1) else Color(0xCCFFFFFF),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun KikiExplorerHubView(
    userName: String,
    rankTitle: String,
    level: Int,
    avatarUri: String?,
    earnedStars: Int,
    vocabCount: Int,
    isEnglish: Boolean,
    onNavigateProfile: () -> Unit,
    onNavigateVocab: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToShop: () -> Unit = {},
    onLogoutClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Card matching original Kiki Explorer header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.horizontalGradient(listOf(Color(0x882A1B4E), Color(0x66140D2B))))
                .border(1.2.dp, Color(0x5551FAC1), RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14141E))
                    .border(2.dp, Color(0xFF51FAC1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null && (avatarUri.startsWith("http://") || avatarUri.startsWith("https://") || avatarUri.startsWith("content://"))) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    AsyncImage(
                            model = resolveAvatarResource(avatarUri),
                            contentScale = ContentScale.Fit,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Kiki Explorer",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF004D40))
                            .border(1.dp, Color(0xFF51FAC1).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Lv.$level",
                            color = Color(0xFF51FAC1),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rankTitle,
                        color = Color(0xFFFFD166),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FFD166))
                        .border(1.dp, Color(0x66FFD166), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("$earnedStars", color = Color(0xFFFFD166), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (isEnglish) "EXPLORER SECTIONS" else "CHỌN MỤC THAO TÁC",
            color = Color(0xFF51FAC1),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.3.sp
        )

        // Menu thu gọn theo đúng yêu cầu: Hồ sơ (gồm danh hiệu), Từ vựng ngữ pháp, Cài đặt
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF131722))
                .border(1.dp, Color(0x3351FAC1), RoundedCornerShape(22.dp))
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 1. Hồ sơ (gồm danh hiệu và cột mốc bên trong)
            DuolingoStyleTile(
                title = if (isEnglish) "Profile & Cosmic Badges" else "Hồ Sơ & Danh Hiệu Cột Mốc",
                icon = Icons.Default.Person,
                badgeText = "Lv.$level",
                onClick = onNavigateProfile
            )

            HorizontalDivider(color = Color(0x1A51FAC1), thickness = 0.8.dp)

            // 2. Từ vựng & Ngữ pháp
            DuolingoStyleTile(
                title = if (isEnglish) "Vocabulary & Grammar" else "Từ Vựng & Ngữ Pháp",
                icon = Icons.Default.CollectionsBookmark,
                badgeText = "$vocabCount từ",
                onClick = onNavigateVocab
            )

            HorizontalDivider(color = Color(0x1A51FAC1), thickness = 0.8.dp)

            // 3. Cosmic Exchange Shop
            DuolingoStyleTile(
                title = if (isEnglish) "Cosmic Exchange Shop" else "Cửa Hàng Quy Đổi Sao & Khiên",
                icon = Icons.Default.Store,
                iconBg = Color(0x22FFD166),
                iconTint = Color(0xFFFFD166),
                badgeText = "⭐ Shop",
                onClick = onNavigateToShop
            )

            HorizontalDivider(color = Color(0x1A51FAC1), thickness = 0.8.dp)

            // 4. Cài đặt & Tuỳ chọn
            DuolingoStyleTile(
                title = if (isEnglish) "Settings & Preferences" else "Cài Đặt & Tuỳ Chọn",
                icon = Icons.Default.Settings,
                onClick = onSettingsClick
            )

            HorizontalDivider(color = Color(0x1A51FAC1), thickness = 0.8.dp)

            // 4. Đăng xuất tài khoản
            DuolingoStyleTile(
                title = if (isEnglish) "Log Out Account" else "Đăng Xuất Tài Khoản",
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                iconBg = Color(0x22FF5252),
                iconTint = Color(0xFFFF5252),
                onClick = onLogoutClick
            )
        }
    }
}

@Composable
private fun DuolingoStyleTile(
    title: String,
    icon: ImageVector,
    iconBg: Color = Color(0x2251FAC1),
    iconTint: Color = Color(0xFF51FAC1),
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(iconBg)
                .border(1.dp, iconTint.copy(alpha = 0.4f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1A51FAC1))
                    .border(1.dp, Color(0x3351FAC1), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    color = Color(0xFF51FAC1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0x44FFFFFF),
            modifier = Modifier.size(13.dp)
        )
    }
}

@Composable
private fun ProfileSectionView(
    userName: String,
    rankTitle: String,
    level: Int,
    xp: Int,
    xpProgress: Float,
    xpToNextLevel: Int,
    avatarUri: String?,
    studyMotto: String,
    isEnglish: Boolean,
    streak: Int,
    streakShields: Int,
    completedLessons: Int,
    totalLessons: Int,
    earnedStars: Int,
    selectedLevel: String,
    allBadges: List<AchievementBadge> = emptyList(),
    unlockedBadgeCount: Int = 0,
    onBadgeClick: (AchievementBadge) -> Unit = {},
    onEditClick: () -> Unit,
    onNavigateToRoadmap: () -> Unit,
    onOpenLevelDialog: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Thẻ Nhận Diện Cá Nhân: Avatar lớn, Tên, Cấp độ, XP, Châm ngôn, Nút chỉnh sửa
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
            onEditClick = onEditClick
        )

        // 2. Thống Kê Học Tập Cá Nhân (Streak, XP, Bài học, Sao)
        ProfileClusterHeader(
            title = if (isEnglish) "LEARNER VITALS" else "CHỈ SỐ HỌC TẬP CÁ NHÂN",
            icon = Icons.Default.Insights,
            color = Color(0xFF51FAC1)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileCompactMetricTile(
                    modifier = Modifier.weight(1f),
                    title = if (isEnglish) "STREAK" else "CHUỖI NGÀY",
                    value = if (isEnglish) "$streak Days 🔥" else "$streak Ngày 🔥",
                    subtitle = if (isEnglish) "🛡️ $streakShields Shield" else "🛡️ $streakShields Khiên giữ",
                    icon = Icons.Default.LocalFireDepartment,
                    accentColor = Color(0xFFFF9E00),
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        Toast.makeText(context, if (isEnglish) "Streak: $streak days" else "Chuỗi chuyên cần: $streak ngày liên tục!", Toast.LENGTH_SHORT).show()
                    }
                )
                ProfileCompactMetricTile(
                    modifier = Modifier.weight(1f),
                    title = if (isEnglish) "TOTAL XP" else "NĂNG LƯỢNG XP",
                    value = "$xp XP",
                    subtitle = "⚡ $rankTitle",
                    icon = Icons.Default.AutoAwesome,
                    accentColor = Color(0xFFFFD166),
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        Toast.makeText(context, if (isEnglish) "XP: $xp - Rank: $rankTitle" else "Điểm XP: $xp - Hạng: $rankTitle", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val percent = if (totalLessons > 0) (completedLessons * 100 / totalLessons) else 0
                ProfileCompactMetricTile(
                    modifier = Modifier.weight(1f),
                    title = if (isEnglish) "LESSONS" else "BÀI HỌC",
                    value = "$completedLessons/$totalLessons",
                    subtitle = if (isEnglish) "$percent% Mastered" else "$percent% Hoàn thành",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    accentColor = Color(0xFF51FAC1),
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onNavigateToRoadmap()
                    }
                )
                ProfileCompactMetricTile(
                    modifier = Modifier.weight(1f),
                    title = if (isEnglish) "COSMIC STARS" else "SAO TINH TÚ",
                    value = "$earnedStars ⭐",
                    subtitle = if (isEnglish) "Milestone Vault" else "Kho báu tri thức",
                    icon = Icons.Default.Star,
                    accentColor = Color(0xFFC68CFF),
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        Toast.makeText(context, if (isEnglish) "Cosmic Stars: $earnedStars ⭐" else "Kho báu sao: $earnedStars sao tinh tú!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // 2.5 Danh Hiệu & Cột Mốc (Thiết kế nhúng ngay trong Hồ Sơ)
        ProfileClusterHeader(
            title = if (isEnglish) "COSMIC HONORS & BADGES" else "DANH HIỆU & CỘT MỐC",
            icon = Icons.Default.EmojiEvents,
            color = Color(0xFFFFD166)
        )
        ProfileAchievements(
            allBadges = allBadges,
            isEnglish = isEnglish,
            onBadgeClick = onBadgeClick
        )
    }
}

@Composable
private fun ProfileVocabularySection(
    vocabularyList: List<VocabularyEntity>,
    masteredCount: Int,
    isEnglish: Boolean,
    onOpenFlashcards: () -> Unit,
    onAddWord: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onToggleMastered: (Long, Boolean) -> Unit,
    onDeleteWord: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Learning, 2: Mastered

    val filteredList = remember(vocabularyList, searchQuery, selectedFilter) {
        vocabularyList.filter { item ->
            val matchQuery = searchQuery.isBlank() || 
                item.word.contains(searchQuery, ignoreCase = true) || 
                item.meaning.contains(searchQuery, ignoreCase = true)
            val matchFilter = when (selectedFilter) {
                1 -> !item.isMastered
                2 -> item.isMastered
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Summary & Fast Action Header Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF132729), Color(0xFF0F1826))))
                .border(1.2.dp, Color(0xFF51FAC1).copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isEnglish) "SAVED VOCABULARY" else "SỔ TAY TỪ VỰNG",
                            color = Color(0xFF51FAC1),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = if (isEnglish) "${vocabularyList.size} Words in Vault" else "${vocabularyList.size} Từ Đã Thu Thập",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF51FAC1).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Mastered: $masteredCount" else "Đã thuộc: $masteredCount",
                            color = Color(0xFF51FAC1),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Two Primary Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenFlashcards,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF51FAC1),
                            contentColor = Color(0xFF0D0A1A)
                        )
                    ) {
                        Icon(Icons.Default.CollectionsBookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "3D Flashcards" else "Luyện Thẻ 3D",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onAddWord,
                        modifier = Modifier.weight(0.9f).height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0xFF51FAC1).copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEnglish) "Add Word" else "Thêm Từ",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            placeholder = {
                Text(
                    text = if (isEnglish) "Search saved words or meaning..." else "Tìm kiếm từ hoặc nghĩa tiếng Việt...",
                    color = Color(0x66FFFFFF),
                    fontSize = 12.sp
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF51FAC1), modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0x99FFFFFF), modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF51FAC1),
                unfocusedBorderColor = Color(0x3351FAC1),
                focusedContainerColor = Color(0x44141026),
                unfocusedContainerColor = Color(0x44141026)
            )
        )

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filterOptions = listOf(
                0 to (if (isEnglish) "All (${vocabularyList.size})" else "Tất cả (${vocabularyList.size})"),
                1 to (if (isEnglish) "Learning" else "Đang học"),
                2 to (if (isEnglish) "Mastered ($masteredCount)" else "Đã thuộc ($masteredCount)")
            )
            filterOptions.forEach { (index, label) ->
                val isSelected = selectedFilter == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF51FAC1).copy(alpha = 0.2f) else Color(0x22FFFFFF))
                        .border(1.dp, if (isSelected) Color(0xFF51FAC1) else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { selectedFilter = index }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFF51FAC1) else Color(0x99FFFFFF),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // List of Words or Empty State
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x3318122C))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CollectionsBookmark,
                        contentDescription = null,
                        tint = Color(0xFF51FAC1).copy(alpha = 0.6f),
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isEnglish) "No words found in this view" else "Chưa có từ vựng nào trong mục này",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isEnglish) "Add new words or practice quests to save!" else "Hãy nhấn 'Thêm Từ' hoặc chạm vào từ mới trong bài học để lưu lại!",
                        color = Color(0x99FFFFFF),
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredList.forEach { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x55191438))
                            .border(1.dp, if (item.isMastered) Color(0x5551FAC1) else Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.word,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (item.phonetic.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.phonetic,
                                            color = Color(0xFF51FAC1),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.meaning,
                                    color = Color(0xEEFFFFFF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                )
                                if (item.example.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "\"${item.example}\"",
                                        color = Color(0x88FFFFFF),
                                        fontSize = 11.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { onSpeakWord(item.word) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Speak",
                                        tint = Color(0xFF51FAC1),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (item.isMastered) Color(0xFF003830) else Color(0x22FFFFFF))
                                        .border(1.dp, if (item.isMastered) Color(0xFF51FAC1) else Color(0x44FFFFFF), RoundedCornerShape(10.dp))
                                        .clickable { onToggleMastered(item.vocabId, !item.isMastered) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (item.isMastered) (if (isEnglish) "Mastered" else "Đã thuộc") else (if (isEnglish) "Need Practice" else "Cần ôn"),
                                        color = if (item.isMastered) Color(0xFF51FAC1) else Color(0xAAFFFFFF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Cụm Ngữ Pháp & Mẫu Câu Cốt Lõi (Core Grammar & Sentence Patterns)
        ProfileClusterHeader(
            title = if (isEnglish) "GRAMMAR & SENTENCE PATTERNS" else "MẸO NGỮ PHÁP & CẤU TRÚC CÂU",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            color = Color(0xFF00F5D4)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileGrammarTipCard(
                title = if (isEnglish) "Present Simple vs Past Simple" else "Thì Hiện Tại Đơn & Quá Khứ Đơn",
                rule = if (isEnglish) "Present: S + V(s/es) | Past: S + V-ed / V2" else "Hiện tại: S + V(s/es) | Quá khứ: S + V-ed / V2",
                example = if (isEnglish) "I study English every day ➔ I studied yesterday." else "Tôi học tiếng Anh mỗi ngày ➔ Hôm qua tôi đã học.",
                tag = "Tenses",
                accentColor = Color(0xFF51FAC1)
            )
            ProfileGrammarTipCard(
                title = if (isEnglish) "Wh- Questions Formula" else "Cấu Trúc Đặt Câu Hỏi (Wh-)",
                rule = if (isEnglish) "Wh-word + Aux (do/does/did) + S + Verb base?" else "Từ để hỏi + Trợ động từ (do/does/did) + S + V nguyên mẫu?",
                example = if (isEnglish) "Where do you practice speaking English?" else "Bạn luyện nói tiếng Anh ở đâu?",
                tag = "Questions",
                accentColor = Color(0xFFFFD166)
            )
            ProfileGrammarTipCard(
                title = if (isEnglish) "Prepositions of Time & Place" else "Giới Từ Thời Gian & Nơi Chốn: In - On - At",
                rule = if (isEnglish) "IN (Month/Year/City) - ON (Day/Street) - AT (Time/Specific place)" else "IN (Tháng/Năm/Mùa) - ON (Thứ/Ngày) - AT (Giờ giấc/Địa điểm cụ thể)",
                example = if (isEnglish) "At 8 PM, on Monday, in December." else "Vào lúc 8h tối, vào thứ Hai, vào tháng 12.",
                tag = "Prepositions",
                accentColor = Color(0xFFC68CFF)
            )
        }
    }
}

@Composable
private fun ProfileGrammarTipCard(
    title: String,
    rule: String,
    example: String,
    tag: String,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x55191438))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.18f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tag,
                        color = accentColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Text(
                text = rule,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = example,
                color = Color(0xCCFFFFFF),
                fontSize = 11.5.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
private fun ProfileMistakesSection(
    weakPoints: List<WeakPointEntity>,
    curedCount: Int,
    isEnglish: Boolean,
    onStartClinic: () -> Unit,
    onResolveSingle: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Summary Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2E131E), Color(0xFF1B0E1E))))
                .border(1.2.dp, Color(0xFFFF6B6B).copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isEnglish) "MISTAKE CLINIC" else "PHÒNG KHÁM ĐIỂM YẾU",
                            color = Color(0xFFFF6B6B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = if (isEnglish) "${weakPoints.size} Mistakes to Cure" else "${weakPoints.size} Lỗi Cần Khắc Phục",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFF6B6B).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Cured: $curedCount" else "Đã chữa: $curedCount",
                            color = Color(0xFFFF6B6B),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onStartClinic,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B6B),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Healing, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEnglish) "Start Clinic Rehab (+XP Bonus)" else "Bắt Đầu Chữa Lỗi (+XP Thưởng)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // List of Weak Points or Empty State
        if (weakPoints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x3318122C))
                    .border(1.dp, Color(0x3351FAC1), RoundedCornerShape(20.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF51FAC1),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isEnglish) "Zero Weak Points!" else "Tuyệt Vời! Không Có Lỗi Tồn Đọng",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isEnglish) "Your knowledge is sharp. Keep advancing the galactic map!" else "Kiến thức của bạn đang rất vững vàng! Tiếp tục khám phá vũ trụ nhé! 🚀",
                        color = Color(0x99FFFFFF),
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                weakPoints.forEach { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x55281422))
                            .border(1.dp, Color(0x44FF6B6B), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF6B6B))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Wrong ${item.wrongCount} times" else "Sai ${item.wrongCount} lần",
                                        color = Color(0xFFFF6B6B),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF51FAC1).copy(alpha = 0.2f))
                                        .clickable { onResolveSingle(item.weakId) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "Mark Fixed" else "Đã hiểu ✓",
                                        color = Color(0xFF51FAC1),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = item.question,
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAchievementsSection(
    allBadges: List<AchievementBadge>,
    unlockedCount: Int,
    earnedStars: Int,
    isEnglish: Boolean,
    onBadgeClick: (AchievementBadge) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Summary Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF261D10), Color(0xFF161026))))
                .border(1.2.dp, Color(0xFFFFD166).copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isEnglish) "COSMIC HONORS" else "KHO BÁU DANH DỰ",
                        color = Color(0xFFFFD166),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = if (isEnglish) "$unlockedCount / ${allBadges.size} Badges" else "$unlockedCount / ${allBadges.size} Huy Hiệu Đã Mở",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFD166).copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$earnedStars Sao",
                            color = Color(0xFFFFD166),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        ProfileAchievements(
            allBadges = allBadges,
            isEnglish = isEnglish,
            onBadgeClick = onBadgeClick
        )
    }
}



