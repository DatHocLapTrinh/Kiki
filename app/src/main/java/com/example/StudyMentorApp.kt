package com.example

import android.content.Context
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.repository.DataRepository
import com.example.ui.PlacementQuizView
import com.example.viewmodel.StudyViewModel
import com.example.model.LearnerPersonaProfile
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- App Entry ---
enum class AuthStep { SPLASH, IDENTITY, ONBOARDING, LOGGED_IN }

data class ParallaxController(
    val setTarget: (x: Float?, y: Float?) -> Unit,
    val fireBurst: (x: Float, y: Float) -> Unit,
    val currentX: Float,
    val currentY: Float
)
val LocalParallax = staticCompositionLocalOf<ParallaxController> { error("No Parallax provided") }

@Composable
fun StudyMentorApp() {
    val viewModel: StudyViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    var authStep by remember { mutableStateOf(AuthStep.SPLASH) }

    var targetX by remember { mutableStateOf<Float?>(null) }
    var targetY by remember { mutableStateOf<Float?>(null) }
    
    val scope = rememberCoroutineScope()
    var isWarping by remember { mutableStateOf(false) }
    val warpSpeedMultiplier by animateFloatAsState(
        targetValue = if (isWarping) 15f else 1f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "warp"
    )
    val batterySaver by viewModel.batterySaver.observeAsState(false)

    CosmicScaffold(
        externalTargetX = targetX, 
        externalTargetY = targetY,
        warpSpeedMultiplier = warpSpeedMultiplier,
        batterySaver = batterySaver
    ) { currentX, currentY, onBurst ->
        val parallaxController = remember(currentX, currentY) {
            ParallaxController(
                setTarget = { x, y -> targetX = x; targetY = y },
                fireBurst = onBurst,
                currentX = currentX,
                currentY = currentY
            )
        }

        val isEnglish by viewModel.isEnglish.observeAsState(false)
        val appStrings = if (isEnglish) EnglishStrings else VietnameseStrings

        CompositionLocalProvider(
            LocalParallax provides parallaxController,
            LocalAppStrings provides appStrings
        ) {
            AnimatedContent(
                targetState = authStep,
                transitionSpec = {
                    if (targetState == AuthStep.ONBOARDING || targetState == AuthStep.LOGGED_IN) {
                        (fadeIn(tween(800)) + scaleIn(initialScale = 0.5f, animationSpec = tween(800, easing = FastOutSlowInEasing))) togetherWith
                        fadeOut(tween(800)) + scaleOut(targetScale = 1.5f, animationSpec = tween(800))
                    } else {
                        (fadeIn(animationSpec = tween(800)) + scaleIn(initialScale = 0.92f, animationSpec = tween(800))) togetherWith
                        fadeOut(animationSpec = tween(800)) + scaleOut(targetScale = 1.08f, animationSpec = tween(800))
                    }
                },
                label = "AuthTransition",
                modifier = Modifier.fillMaxSize()
            ) { step ->
                when (step) {
                    AuthStep.SPLASH -> {
                        IntroContent(
                            currentX = currentX,
                            currentY = currentY,
                            onBurst = onBurst,
                            onIgnite = {
                                val currentUser = viewModel.authManager.getCurrentUser()
                                val lastEmail = viewModel.getLastLoggedInEmail()
                                val targetEmail = currentUser?.email ?: lastEmail
                                if (!targetEmail.isNullOrBlank()) {
                                    scope.launch {
                                        isWarping = true
                                        val userId = viewModel.loadUserData(targetEmail)
                                        delay(800)
                                        isWarping = false
                                        if (userId != -1L) {
                                            val isCompleted = viewModel.isUserOnboardingCompleted(userId)
                                            authStep = if (!isCompleted) AuthStep.ONBOARDING else AuthStep.LOGGED_IN
                                        } else {
                                            authStep = AuthStep.IDENTITY
                                        }
                                    }
                                } else {
                                    authStep = AuthStep.IDENTITY
                                }
                            }
                        )
                    }
                    AuthStep.IDENTITY -> {
                        IdentityContent(
                            currentX = currentX,
                            currentY = currentY,
                            viewModel = viewModel,
                            onLoginSuccess = { 
                                scope.launch {
                                    isWarping = true
                                    delay(1000)
                                    isWarping = false
                                    val userId = viewModel.currentUserId.value ?: -1L
                                    val isCompleted = viewModel.isUserOnboardingCompleted(userId)
                                    authStep = if (!isCompleted) AuthStep.ONBOARDING else AuthStep.LOGGED_IN
                                }
                            }
                        )
                    }
                    AuthStep.ONBOARDING -> {
                        OnboardingScreen(viewModel, onFinish = {
                            viewModel.addXp(50)
                            val userId = viewModel.currentUserId.value ?: -1L
                            if (userId != -1L) {
                                viewModel.checkAndUpdateStreak(userId)
                            } else {
                                viewModel.setStreak(1)
                            }
                            viewModel.completeOnboarding(userId, viewModel.selectedLevel.value ?: "Beginner")
                            authStep = AuthStep.LOGGED_IN
                        })
                    }
                    AuthStep.LOGGED_IN -> {
                        MainNavigation(viewModel, onLogout = {
                            viewModel.logout()
                            authStep = AuthStep.SPLASH
                        })
                    }
                }
            }
        }
    }
}


// --- Onboarding Screen ---
@Composable
fun OnboardingScreen(viewModel: StudyViewModel, onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var selectedLevel by remember { mutableStateOf("Beginner") }
    var selectedGoal by remember { mutableStateOf("Everyday Fluency") }
    var selectedModality by remember { mutableStateOf("AUDIO_DIALOG") }
    var selectedPacingMinutes by remember { mutableIntStateOf(5) }
    var selectedSelfEfficacy by remember { mutableStateOf("ELEMENTARY") }
    var initialTheta by remember { mutableFloatStateOf(-0.3f) }
    var evaluatedProfile by remember { mutableStateOf<LearnerPersonaProfile?>(null) }
    val scope = rememberCoroutineScope()
    val parallax = LocalParallax.current
    val haptic = LocalHapticFeedback.current
    
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
                .background(Brush.linearGradient(
                    colors = listOf(Color(0x33FFFFFF), Color(0x0AFFFFFF)),
                    start = Offset.Zero, end = Offset.Infinite
                ))
                .border(1.dp, Brush.linearGradient(listOf(Color(0x66FFFFFF), Color(0x00FFFFFF), Color(0x33FFFFFF))), RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // Progress Indicator (5 bước)
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(bottom = 20.dp)) {
                    for (i in 0 until 5) {
                        val color = if (i <= step) Color(0xFF00F5D4) else Color(0x33FFFFFF)
                        val width by animateDpAsState(if (i == step) 24.dp else 8.dp, label = "progressWidth")
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
                
                AnimatedContent(
                    targetState = step, 
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
                    },
                    label = "onboarding"
                ) { currentStep ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        val strings = LocalAppStrings.current
                        when (currentStep) {
                            0 -> {
                                // BƯỚC 0: MỤC TIÊU & PHƯƠNG THỨC HỌC (INTAKE PROFILING)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0x2E00F5D4))
                                        .border(1.dp, Color(0x6600F5D4), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 14.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "🎯 HỒ SƠ HỌC TẬP (INTAKE PROFILE)",
                                        color = Color(0xFF00F5D4),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Mục Tiêu & Phương Thức Chủ Đạo",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Kiki cá nhân hóa từng bài microlearning theo mục tiêu thực tế của bạn.",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                val goalOptions = listOf(
                                    Triple("Everyday Fluency", "💬 Giao Tiếp Phản Xạ Hàng Ngày", "Tự tin nói trôi chảy, phản xạ tự nhiên."),
                                    Triple("Grammar & Vocabulary", "📝 Ngữ Pháp & Cấu Trúc Chuẩn Xác", "Nắm vững bản chất cú pháp, xóa lỗi sai."),
                                    Triple("IELTS & TOEIC", "🎓 Luyện Thi Chứng Chỉ (IELTS/TOEIC)", "Bứt phá band điểm, mở rộng từ học thuật."),
                                    Triple("Career & Business", "💼 Tiếng Anh Công Việc & Phỏng Vấn", "Giao tiếp công sở & đàm phán tự tin.")
                                )

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    goalOptions.forEach { (goalKey, goalTitle, goalDesc) ->
                                        val isSelected = selectedGoal == goalKey
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSelected) Color(0x3300F5D4) else Color(0x14FFFFFF))
                                                .border(
                                                    1.dp,
                                                    if (isSelected) Color(0xFF00F5D4) else Color(0x26FFFFFF),
                                                    RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.soundEffectManager.playClick()
                                                    selectedGoal = goalKey
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = goalTitle,
                                                        color = if (isSelected) Color(0xFF00F5D4) else Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.5.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = goalDesc,
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                if (isSelected) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = Color(0xFF00F5D4),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Bộ chọn Phương thức học
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val modalities = listOf(
                                        Pair("AUDIO_DIALOG", "🎙️ Gemini Native Audio"),
                                        Pair("SYNTAX_READING", "📖 Cú Pháp & Đọc Hiểu")
                                    )
                                    modalities.forEach { (modKey, modTitle) ->
                                        val isSelected = selectedModality == modKey
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) Color(0x33FF9E00) else Color(0x14FFFFFF))
                                                .border(1.dp, if (isSelected) Color(0xFFFF9E00) else Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.soundEffectManager.playClick()
                                                    selectedModality = modKey
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = modTitle,
                                                color = if (isSelected) Color(0xFFFF9E00) else Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFFFF9E00), Color(0xFF00F5D4))))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.soundEffectManager.playClick()
                                            scope.launch { delay(180); step++ }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.continueJourney,
                                        color = Color(0xFF0B0D1B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            1 -> {
                                // BƯỚC 1: THỜI LƯỢNG MICROLEARNING & ĐIỂM TỰ TIN BAN ĐẦU
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0x2E00F5D4))
                                        .border(1.dp, Color(0x6600F5D4), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 14.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "⏱️ THỜI LƯỢNG & NĂNG LỰC BAN ĐẦU",
                                        color = Color(0xFF00F5D4),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Nhịp Độ Học & Điểm Tự Tin Ban Đầu",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Microlearning 3-5 phút giúp duy trì bền bỉ và tối ưu trí nhớ dài hạn.",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "THỜI LƯỢNG HỌC MỖI NGÀY",
                                    color = Color(0xFFFF9E00),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                )

                                val pacingOptions = listOf(
                                    Triple(3, "⚡ 3 Phút", "Siêu Vi Mô"),
                                    Triple(5, "🎯 5 Phút", "Khuyến Nghị"),
                                    Triple(10, "🚀 10 Phút", "Tăng Tốc")
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    pacingOptions.forEach { (minutes, label, sub) ->
                                        val isSelected = selectedPacingMinutes == minutes
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) Color(0x3300F5D4) else Color(0x14FFFFFF))
                                                .border(1.dp, if (isSelected) Color(0xFF00F5D4) else Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.soundEffectManager.playClick()
                                                    selectedPacingMinutes = minutes
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color(0xFF00F5D4) else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = sub,
                                                    color = Color.White.copy(alpha = 0.65f),
                                                    fontSize = 9.5.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "ĐIỂM TỰ TIN KHỞI ĐIỂM (SELF-EFFICACY)",
                                    color = Color(0xFFFF9E00),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                )

                                val selfEfficacyOptions = listOf(
                                    Triple("NEWCOMER", -1.0f, "🌱 Người mới bắt đầu / Mất gốc (A1)"),
                                    Triple("ELEMENTARY", -0.3f, "🌿 Đã biết từ vựng cơ bản, còn ngại nói (A2)"),
                                    Triple("INTERMEDIATE", 0.2f, "🌳 Giao tiếp ổn, muốn chuẩn hóa ngữ pháp (B1-B2)"),
                                    Triple("CONFIDENT", 0.8f, "🌟 Tự tin, hướng đến C1 chuyên nghiệp")
                                )

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    selfEfficacyOptions.forEach { (key, theta, title) ->
                                        val isSelected = selectedSelfEfficacy == key
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) Color(0x3300F5D4) else Color(0x14FFFFFF))
                                                .border(1.dp, if (isSelected) Color(0xFF00F5D4) else Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.soundEffectManager.playClick()
                                                    selectedSelfEfficacy = key
                                                    initialTheta = theta
                                                }
                                                .padding(horizontal = 14.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = title,
                                                    color = if (isSelected) Color(0xFF00F5D4) else Color.White,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color(0xFF00F5D4),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFFFF9E00), Color(0xFF00F5D4))))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.soundEffectManager.playClick()
                                            scope.launch { delay(180); step++ }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Bắt Đầu Test Thích Ứng (CAT) ✨",
                                        color = Color(0xFF0B0D1B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            2 -> {
                                // BƯỚC 2: BÀI TEST CHẨN ĐOÁN THÍCH ỨNG CAT (IRT) & RADAR NĂNG LỰC
                                PlacementQuizView(
                                    soundEffectManager = viewModel.soundEffectManager,
                                    ttsManager = viewModel.ttsManager,
                                    speechRecognitionManager = viewModel.speechRecognitionManager,
                                    initialTheta = initialTheta,
                                    goal = selectedGoal,
                                    dailyTargetMinutes = selectedPacingMinutes,
                                    selfEfficacy = selectedSelfEfficacy,
                                    priorityModality = selectedModality,
                                    userId = viewModel.currentUserId.value ?: 0L,
                                    onCompleted = { profile ->
                                        evaluatedProfile = profile
                                        selectedLevel = profile.evaluatedCefr
                                        scope.launch {
                                            delay(200)
                                            step = 3
                                        }
                                    }
                                )
                            }
                            3 -> {
                                // BƯỚC 3: AI ENGINE KIẾN TẠO LỘ TRÌNH DỰA TRÊN ĐIỂM YẾU CHẨN ĐOÁN
                                var phaseIndex by remember { mutableIntStateOf(0) }
                                LaunchedEffect(Unit) {
                                    val userId = viewModel.currentUserId.value ?: 0L
                                    evaluatedProfile?.let { profile ->
                                        viewModel.initializeAiMicrolearningRoadmap(userId, profile) {
                                            viewModel.refreshChapters()
                                        }
                                    } ?: run {
                                        viewModel.setPreferences(selectedLevel, "English")
                                        viewModel.generateCustomChapter(selectedLevel, selectedGoal) { _, _ ->
                                            viewModel.refreshChapters()
                                        }
                                    }
                                    delay(900)
                                    phaseIndex = 1
                                    delay(900)
                                    phaseIndex = 2
                                    delay(900)
                                    viewModel.refreshChapters()
                                    delay(300)
                                    step = 4
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(Color(0xFF00F5D4).copy(alpha = 0.35f), Color(0x1A7209B7))
                                                )
                                            )
                                            .border(1.5.dp, Color(0xFF00F5D4), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFF00F5D4),
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        text = strings.aiGeneratingRoadmapTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = strings.aiGeneratingRoadmapDesc,
                                        textAlign = TextAlign.Center,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.5.sp,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )

                                    Spacer(modifier = Modifier.height(28.dp))

                                    // 3 Bouncing Dots Animation
                                    BouncingDotsIndicator(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        dotSize = 13.dp,
                                        bounceHeight = 16.dp
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    val statusText = when (phaseIndex) {
                                        0 -> "🧠 Đang hiệu chuẩn vector năng lực θ & tham số SM-2..."
                                        1 -> "✨ Ép khuôn JSON Schema 3C (Concept-Check-Consolidate)..."
                                        else -> "📚 Khởi tạo Chapter 1 nhắm trúng các điểm yếu chẩn đoán..."
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0x2E00F5D4))
                                            .border(1.dp, Color(0x6600F5D4), RoundedCornerShape(20.dp))
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = statusText,
                                            color = Color(0xFF00F5D4),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            4 -> {
                                // BƯỚC 4: HỒ SƠ & LỘ TRÌNH ĐÃ SẴN SÀNG
                                Text(strings.profileInitialized, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(strings.profileReady, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.8f), fontSize = 12.5.sp)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Banner kết quả phân loại AI
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.linearGradient(listOf(Color(0x3300F5D4), Color(0x1A7209B7))))
                                        .border(1.2.dp, Color(0xFF00F5D4), RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "✨ " + strings.evaluatedLevelBanner.uppercase(),
                                            color = Color(0xFF00F5D4),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = when (selectedLevel) {
                                                "Beginner" -> strings.beginnerTitle
                                                "Intermediate" -> strings.intermediateTitle
                                                else -> strings.advancedTitle
                                            },
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "🎯 Mục tiêu: $selectedGoal\n⏱️ Nhịp độ: $selectedPacingMinutes phút/ngày • 🎙️ Gemini 2.5 Flash Native Audio",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.5.sp,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(22.dp))
                                UltimateButton(text = strings.enterMap, icon = Icons.AutoMirrored.Filled.ArrowForward) { 
                                    for(i in 0 until 50) parallax.fireBurst(500f, 1500f)
                                    onFinish() 
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BouncingDotsIndicator(
    modifier: Modifier = Modifier,
    dotSize: androidx.compose.ui.unit.Dp = 14.dp,
    bounceHeight: androidx.compose.ui.unit.Dp = 16.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bouncing_dots")

    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -bounceHeight.value,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(0)
        ),
        label = "dot1"
    )

    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -bounceHeight.value,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(140)
        ),
        label = "dot2"
    )

    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -bounceHeight.value,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(280)
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .offset(y = dot1Offset.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(Color(0xFF00F5D4))
                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
        )
        Box(
            modifier = Modifier
                .offset(y = dot2Offset.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(Color(0xFFFF9E00))
                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
        )
        Box(
            modifier = Modifier
                .offset(y = dot3Offset.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(Color(0xFF7209B7))
                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
        )
    }
}

@Composable
fun StaggeredOptions(options: List<String>, onSelect: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    
    var selectedIndex by remember { mutableStateOf(-1) }
    
    Column {
        options.forEachIndexed { index, text ->
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn(tween(500, delayMillis = index * 100))
            ) {
                OptionButton(
                    text = text, 
                    isSelected = selectedIndex == index,
                    onClick = { 
                        selectedIndex = index
                        onSelect(text)
                    }
                )
            }
        }
    }
}

@Composable
fun UltimateButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(Color(0xCCFF9E00), Color(0xCC00F5D4))))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Icon(icon, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun OptionButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val glowAlpha by animateFloatAsState(if (isSelected) 1f else 0f, label = "glow")
    
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0x3300F5D4) else Color(0x1AFFFFFF))
            .border(
                1.dp, 
                if (isSelected) Brush.linearGradient(listOf(Color(0xFF00F5D4), Color(0xFF00B4D8))) else SolidColor(Color(0x33FFFFFF)), 
                RoundedCornerShape(16.dp)
            )
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick() 
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 16.sp, color = if (isSelected) Color(0xFF00F5D4) else Color.White, fontWeight = FontWeight.SemiBold)
    }
}

// --- Main Navigation ---
@Composable
fun MainNavigation(viewModel: StudyViewModel, onLogout: () -> Unit) {
    val navController = rememberNavController()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "map",
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(260)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) },
            exitTransition = { fadeOut(tween(260)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) },
            popEnterTransition = { fadeIn(tween(260)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) },
            popExitTransition = { fadeOut(tween(260)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) }
        ) {
            composable("map") {
                JourneyMapScreen(
                    viewModel = viewModel,
                    onStartQuest = { navController.navigate("progression_map") },
                    onStartActiveLesson = { navController.navigate("quest_active") },
                    onLogout = onLogout,
                    onViewStats = { navController.navigate("profile") { launchSingleTop = true } }
                )
            }
            composable("progression_map") { ChapterJourneyMapScreen(viewModel, onBack = { navController.popBackStack() }, onStartQuest = { lessonIndex ->
                viewModel.startLesson(lessonIndex)
                navController.navigate("quest_active")
            }) }
            composable("quest_active") { QuestScreen(viewModel, onFinish = { navController.navigate("rank_reveal") }) }
            composable("rank_reveal") {
                RankRevealScreen(
                    viewModel = viewModel,
                    onEnterRealm = {
                        navController.navigate("progression_map") {
                            popUpTo("map")
                        }
                    },
                    onViewAnalysis = {
                        navController.navigate("quest_review")
                    },
                    onRetryLesson = {
                        val currentChapter = viewModel.currentChapterTitle.value ?: ""
                        val currentNode = viewModel.currentNode.value ?: 1
                        viewModel.selectChapterByTitle(currentChapter, currentNode)
                        navController.navigate("quest_active") {
                            popUpTo("progression_map")
                        }
                    }
                )
            }
            composable("quest_review") { QuestReviewScreen(viewModel, onBack = { navController.popBackStack() }) }
            composable("quests") { QuestsScreen(viewModel) }
            composable("ask") { AskScreen(viewModel) }
            composable("rank") { RankScreen(viewModel) }
            composable("profile") { ProfileScreen(viewModel, onLogout = onLogout) }
        }

        FloatingNavBar(
            navController = navController,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingNavBar(navController: androidx.navigation.NavHostController, modifier: Modifier = Modifier) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val strings = LocalAppStrings.current
    val isImeVisible = WindowInsets.isImeVisible

    if (isImeVisible || currentRoute == "quest_active" || currentRoute == "rank_reveal" || currentRoute == "progression_map" || currentRoute == "quest_review") return

    Row(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .height(64.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF2181226), Color(0xF20F0B18))
                )
            )
            .border(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(Color(0x6651FAC1), Color(0x22FFFFFF))
                ),
                RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem("map", Icons.Default.Map, strings.navMap, currentRoute, navController)
        NavItem("quests", Icons.Default.TaskAlt, strings.navLearn, currentRoute, navController)
        NavItem("ask", Icons.Default.ChatBubbleOutline, strings.navTutor, currentRoute, navController)
        NavItem("rank", Icons.Default.EmojiEvents, strings.navRank, currentRoute, navController)
        NavItem("profile", Icons.Default.Person, strings.navProfile, currentRoute, navController)
    }
}

@Composable
fun NavItem(
    route: String,
    icon: ImageVector,
    label: String,
    currentRoute: String?,
    navController: androidx.navigation.NavHostController
) {
    val isSelected = currentRoute == route
    val haptic = LocalHapticFeedback.current
    val color = if (isSelected) Color(0xFF51FAC1) else Color(0x99FFFFFF)
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "nav_scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color(0x2E4EF3C5) else Color.Transparent)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) Color(0x4D51FAC1) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(26.dp)
        )
    }
}

