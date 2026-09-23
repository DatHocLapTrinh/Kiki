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
import com.example.viewmodel.StudyViewModel
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
    var hasCompletedOnboarding by remember { mutableStateOf(false) }

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
                            onIgnite = { authStep = AuthStep.IDENTITY }
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
                                    delay(1500)
                                    isWarping = false
                                    authStep = if (!hasCompletedOnboarding) AuthStep.ONBOARDING else AuthStep.LOGGED_IN
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
                            hasCompletedOnboarding = true
                            authStep = AuthStep.LOGGED_IN
                        })
                    }
                    AuthStep.LOGGED_IN -> {
                        MainNavigation(viewModel, onLogout = {
                            viewModel.logout()
                            authStep = AuthStep.SPLASH
                            hasCompletedOnboarding = false
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
    val scope = rememberCoroutineScope()
    val parallax = LocalParallax.current
    
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
                .background(Brush.linearGradient(
                    colors = listOf(Color(0x33FFFFFF), Color(0x0AFFFFFF)),
                    start = Offset.Zero, end = Offset.Infinite
                ))
                .border(1.dp, Brush.linearGradient(listOf(Color(0x66FFFFFF), Color(0x00FFFFFF), Color(0x33FFFFFF))), RoundedCornerShape(32.dp))
                .padding(32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // Progress Indicator
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(bottom = 24.dp)) {
                    for (i in 0 until 3) {
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
                                Text(strings.selectRealm, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                                Spacer(modifier = Modifier.height(24.dp))
                                StaggeredOptions(
                                    options = listOf(strings.middleSchool, strings.highSchool, strings.university),
                                    onSelect = { 
                                        selectedLevel = when (it) {
                                            strings.middleSchool -> "Beginner"
                                            strings.highSchool -> "Intermediate"
                                            else -> "Advanced"
                                        }
                                        scope.launch { delay(300); step++ }
                                    }
                                )
                            }
                            1 -> {
                                Text(strings.chooseDiscipline, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                                Spacer(modifier = Modifier.height(24.dp))
                                val goalOptions = listOf(strings.goalFluency, strings.goalGrammar, strings.goalExam)
                                var selectedGoals by remember { mutableStateOf(setOf(strings.goalFluency)) }
                                Column {
                                    goalOptions.forEach { option ->
                                        val isSelected = option in selectedGoals
                                        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(56.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(if (isSelected) Color(0x3300F5D4) else Color(0x1AFFFFFF))
                                                .border(1.dp, if (isSelected) Brush.linearGradient(listOf(Color(0xFF00F5D4), Color(0xFF00B4D8))) else SolidColor(Color(0x33FFFFFF)), RoundedCornerShape(16.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    selectedGoals = if (isSelected && selectedGoals.size > 1) {
                                                        selectedGoals - option
                                                    } else {
                                                        selectedGoals + option
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(option, fontSize = 16.sp, color = if (isSelected) Color(0xFF00F5D4) else Color.White, fontWeight = FontWeight.SemiBold)
                                                if (isSelected) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(56.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Brush.linearGradient(listOf(Color(0xCCFF9E00), Color(0xCC00F5D4))))
                                            .clickable {
                                                val firstGoal = selectedGoals.first()
                                                selectedGoal = when (firstGoal) {
                                                    strings.goalFluency -> "Everyday Fluency"
                                                    strings.goalGrammar -> "Grammar & Vocabulary"
                                                    else -> "IELTS & TOEIC"
                                                }
                                                scope.launch { delay(300); step++ }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(strings.continueJourney, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            }
                            2 -> {
                                Text(strings.profileInitialized, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(strings.profileReady, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(32.dp))
                                UltimateButton(text = strings.enterMap, icon = Icons.AutoMirrored.Filled.ArrowForward) { 
                                    for(i in 0 until 50) parallax.fireBurst(500f, 1500f)
                                    viewModel.setPreferences(selectedLevel, "English")
                                    viewModel.refreshChapters()
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

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val userName by viewModel.userName.observeAsState("Adventurer")
    val strings = LocalAppStrings.current
    var showSettings by remember { mutableStateOf(false) }

    val hiddenMenuRoutes = remember {
        setOf("progression_map", "quest_active", "rank_reveal", "quest_review")
    }
    val showMenu = currentRoute !in hiddenMenuRoutes

    fun navigateFromDrawer(route: String) {
        drawerScope.launch {
            drawerState.close()
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showMenu,
        drawerContent = {
            AppNavigationDrawer(
                userName = userName,
                currentRoute = currentRoute,
                strings = strings,
                onNavigate = ::navigateFromDrawer,
                onSettings = {
                    drawerScope.launch {
                        drawerState.close()
                        showSettings = true
                    }
                },
                onLogout = {
                    drawerScope.launch {
                        drawerState.close()
                        onLogout()
                    }
                }
            )
        }
    ) {
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
                        onLogout = onLogout,
                        onViewStats = { navController.navigate("profile") { launchSingleTop = true } }
                    )
                }
                composable("progression_map") { ChapterJourneyMapScreen(viewModel, onBack = { navController.popBackStack() }, onStartQuest = { lessonIndex ->
                    viewModel.startLesson(lessonIndex)
                    navController.navigate("quest_active")
                }) }
                composable("quest_active") { QuestScreen(viewModel, onFinish = { navController.navigate("rank_reveal") }) }
                composable("rank_reveal") { RankRevealScreen(
                    onEnterRealm = {
                        navController.navigate("progression_map") {
                            popUpTo("map")
                        }
                    },
                    onViewAnalysis = {
                        navController.navigate("quest_review")
                    }
                ) }
                composable("quest_review") { QuestReviewScreen(viewModel, onBack = { navController.popBackStack() }) }
                composable("quests") { QuestsScreen(viewModel) }
                composable("ask") { AskScreen(viewModel) }
                composable("rank") { RankScreen(viewModel) }
                composable("profile") { ProfileScreen(viewModel, onLogout = onLogout) }
            }

            FloatingNavBar(navController, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))

            if (showMenu) {
                IconButton(
                    onClick = { drawerScope.launch { drawerState.open() } },
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 12.dp, top = 8.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC14141E))
                        .border(1.dp, Color(0x6651FAC1), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = strings.menuLabel,
                        tint = Color(0xFF51FAC1)
                    )
                }
            }

            if (showSettings) {
                SettingsDialog(
                    viewModel = viewModel,
                    onDismiss = { showSettings = false },
                    onLogout = {
                        showSettings = false
                        onLogout()
                    }
                )
            }
        }
    }
}

private data class AppDrawerItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun AppNavigationDrawer(
    userName: String,
    currentRoute: String?,
    strings: AppStrings,
    onNavigate: (String) -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val items = listOf(
        AppDrawerItem("map", strings.menuMap, Icons.Default.Map),
        AppDrawerItem("quests", strings.menuQuests, Icons.AutoMirrored.Filled.FormatListBulleted),
        AppDrawerItem("ask", strings.menuAsk, Icons.Default.AutoFixHigh),
        AppDrawerItem("rank", strings.menuRank, Icons.Default.EmojiEvents),
        AppDrawerItem("profile", strings.menuProfile, Icons.Default.Person)
    )

    ModalDrawerSheet(
        modifier = Modifier.width(312.dp),
        drawerContainerColor = Color(0xFF151020),
        drawerContentColor = Color.White
    ) {
        Column(modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp)) {
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 36.dp, bottom = 24.dp)) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFF9E00), Color(0xFF51FAC1))))
                        .border(2.dp, Color(0x6651FAC1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("K", color = Color(0xFF151020), fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Kiki Hihi", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(userName, color = Color(0xFF51FAC1), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(strings.menuTitle, color = Color(0x99FFFFFF), fontSize = 12.sp)
            }

            HorizontalDivider(color = Color(0x1FFFFFFF))
            Spacer(modifier = Modifier.height(12.dp))

            items.forEach { item ->
                NavigationDrawerItem(
                    label = { Text(item.label, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(vertical = 3.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0x3351FAC1),
                        selectedTextColor = Color(0xFF51FAC1),
                        selectedIconColor = Color(0xFF51FAC1),
                        unselectedTextColor = Color(0xE6FFFFFF),
                        unselectedIconColor = Color(0xB3FFFFFF)
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            NavigationDrawerItem(
                label = { Text(strings.menuSettings, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                selected = false,
                onClick = onSettings,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(vertical = 3.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedTextColor = Color(0xE6FFFFFF),
                    unselectedIconColor = Color(0xB3FFFFFF)
                )
            )
            NavigationDrawerItem(
                label = { Text(strings.menuLogout, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                selected = false,
                onClick = onLogout,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(top = 3.dp, bottom = 20.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedTextColor = Color(0xFFFF8080),
                    unselectedIconColor = Color(0xFFFF4B4B)
                )
            )
        }
    }
}

@Composable
fun FloatingNavBar(navController: androidx.navigation.NavHostController, modifier: Modifier = Modifier) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val strings = LocalAppStrings.current

    if (currentRoute == "quest_active" || currentRoute == "rank_reveal" || currentRoute == "progression_map" || currentRoute == "quest_review") return

    Row(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(82.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xE614141E))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem("map", Icons.Default.Map, strings.navMap, currentRoute, navController)
        NavItem("quests", Icons.AutoMirrored.Filled.MenuBook, strings.navLearn, currentRoute, navController)
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
    val color = if (isSelected) Color(0xFF51FAC1) else Color(0x80FFFFFF)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (isSelected) Color(0x334EF3C5) else Color.Transparent)
            .clickable {
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(25.dp))
            Text(label, color = color, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}
