package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.model.LeagueSettlementResult
import com.example.model.LeagueTier
import com.example.model.LeagueZone
import com.example.model.RankPlayer
import com.example.viewmodel.StudyViewModel

@Composable
fun RankScreen(viewModel: StudyViewModel) {
    val selectedTier by viewModel.selectedTierTab.observeAsState(LeagueTier.BRONZE)
    val userTier by viewModel.userTier.observeAsState(LeagueTier.BRONZE)
    val players by viewModel.realtimeLeaderboard.observeAsState(emptyList())
    val isLoading by viewModel.isLeaderboardLoading.observeAsState(false)
    val settlementEvent by viewModel.leagueSettlementEvent.observeAsState(null)
    val weeklyXp by viewModel.weeklyXp.observeAsState(0)
    val millisUntilMonday by viewModel.millisUntilMonday.observeAsState(0L)
    val userName by viewModel.userName.observeAsState("")
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.initializeLeagueData()
    }

    // Modal vinh danh hoặc thông báo kết toán Thứ 2 đầu tuần
    if (settlementEvent != null) {
        LeagueSettlementModal(
            result = settlementEvent!!,
            strings = strings,
            onDismiss = { viewModel.dismissLeagueSettlementDialog() }
        )
    }

    // Tìm vị trí của người dùng hiện tại trong giải
    val currentUserPlayer = players.find { it.isCurrentUser } ?: RankPlayer(
        rank = if (players.isNotEmpty()) players.size + 1 else 1,
        name = userName.ifBlank { strings.you },
        title = userTier.subtitle,
        score = weeklyXp,
        avatarUrl = "",
        isCurrentUser = true,
        zone = viewModel.leagueManager.determineZone(
            if (players.isNotEmpty()) players.size + 1 else 1,
            selectedTier
        ),
        tier = selectedTier
    )

    // Tính khoảng cách XP đến Top 5 (thăng hạng) hoặc vị trí an toàn
    val top5CutoffScore = players.getOrNull(4)?.score ?: 0
    val xpGapToTop5 = if (currentUserPlayer.rank > 5) {
        maxOf(1, top5CutoffScore - currentUserPlayer.score + 1)
    } else null

    val safeCutoffScore = players.getOrNull(14)?.score ?: 0
    val xpGapToSafe = if (currentUserPlayer.rank > 15) {
        maxOf(1, safeCutoffScore - currentUserPlayer.score + 1)
    } else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F0C20), Color(0xFF0B0914), Color(0xFF07050C))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_nav_ranking),
                        contentDescription = null,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.rankedArena,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Đấu trường giải đấu • Vinh danh tuần mới",
                            color = Color(0x99FFFFFF),
                            fontSize = 11.sp
                        )
                    }
                }

                // Refresh Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.fetchTierLeaderboard(selectedTier)
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "spin")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rot"
                    )
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Làm mới",
                        tint = if (isLoading) Color(0xFF51FAC1) else Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .then(if (isLoading) Modifier.rotate(rotation) else Modifier)
                    )
                }
            }

            // Horizontal 5-Cup League Tabs
            TierSelectorBar(
                selectedTier = selectedTier,
                userTier = userTier,
                onTierSelected = { tier ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.selectTierTab(tier)
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Main Content Area
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. 3D Trophy Stage & Season Countdown
                item {
                    TrophyStageSection(
                        tier = selectedTier,
                        isUserCurrentTier = selectedTier == userTier,
                        millisUntilMonday = millisUntilMonday,
                        strings = strings
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 2. League Rules Legend Bar (🟢 Top 5, ⚪ 6-15, 🔴 16+)
                item {
                    LeagueRulesLegendBar(strings = strings)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (isLoading && players.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = selectedTier.primaryColor,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                } else if (players.isEmpty()) {
                    item {
                        EmptyLeagueState(selectedTier = selectedTier, strings = strings)
                    }
                } else {
                    // 3. Royal Top 3 Podium
                    item {
                        RoyalPodiumSection(
                            first = players.getOrNull(0) ?: RankPlayer(1, "-", strings.unranked, 0, ""),
                            second = players.getOrNull(1) ?: RankPlayer(2, "-", strings.unranked, 0, ""),
                            third = players.getOrNull(2) ?: RankPlayer(3, "-", strings.unranked, 0, ""),
                            tier = selectedTier
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // 4. Rank 4 and 5 in Promotion Zone (if available)
                    val rank4And5 = players.drop(3).take(2)
                    if (rank4And5.isNotEmpty()) {
                        itemsIndexed(rank4And5, key = { _, p -> "top5_${selectedTier.name}_${p.rank}_${p.name}" }) { _, player ->
                            LeagueRankItem(player = player, tier = selectedTier)
                        }
                    }

                    // 5. Divider to Safe Zone (Top 6 - 15)
                    val safeZonePlayers = players.drop(5).take(10)
                    if (safeZonePlayers.isNotEmpty()) {
                        item {
                            ZoneDividerBanner(
                                title = "VÙNG AN TOÀN (TRỤ HẠNG)",
                                subtitle = "Hạng 6 - 15 giữ nguyên cúp",
                                color = Color(0xFF94A3B8)
                            )
                        }

                        itemsIndexed(safeZonePlayers, key = { _, p -> "safe_${selectedTier.name}_${p.rank}_${p.name}" }) { _, player ->
                            LeagueRankItem(player = player, tier = selectedTier)
                        }
                    }

                    // 6. Divider to Demotion Zone (Top 16+)
                    val demotionZonePlayers = players.drop(15)
                    if (demotionZonePlayers.isNotEmpty()) {
                        item {
                            ZoneDividerBanner(
                                title = if (selectedTier == LeagueTier.BRONZE) "VÙNG CUỐI BẢNG" else "VÙNG NGUY HIỂM (TỤT 1 CÚP)",
                                subtitle = if (selectedTier == LeagueTier.BRONZE) "Hạng 16+ không bị tụt cúp" else "Hạng 16+ sẽ rớt xuống ${selectedTier.previousTier().vietnameseTitle}",
                                color = if (selectedTier == LeagueTier.BRONZE) Color(0xFF94A3B8) else Color(0xFFFF5C5C)
                            )
                        }

                        itemsIndexed(demotionZonePlayers, key = { _, p -> "demote_${selectedTier.name}_${p.rank}_${p.name}" }) { _, player ->
                            LeagueRankItem(player = player, tier = selectedTier)
                        }
                    }
                }
            }
        }

        // 7. Elevated Floating Sticky User Card (nổi trên thanh điều hướng)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 86.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            CurrentUserLeagueBottomCard(
                player = currentUserPlayer,
                userTier = userTier,
                isViewingOwnTier = selectedTier == userTier,
                xpGapToTop5 = xpGapToTop5,
                xpGapToSafe = xpGapToSafe,
                strings = strings
            )
        }
    }
}

/**
 * 3D Trophy Stage Section với hiệu ứng Hào quang (Aura Glow) và đồng hồ đếm ngược
 */
@Composable
fun TrophyStageSection(
    tier: LeagueTier,
    isUserCurrentTier: Boolean,
    millisUntilMonday: Long,
    strings: AppStrings
) {
    val countdownText = remember(millisUntilMonday) {
        val totalSec = millisUntilMonday / 1000
        val days = totalSec / (24 * 3600)
        val hours = (totalSec % (24 * 3600)) / 3600
        val mins = (totalSec % 3600) / 60
        if (days > 0) "${days} ngày ${hours} giờ" else "${hours} giờ ${mins} phút"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        tier.primaryColor.copy(alpha = 0.16f),
                        Color(0xFF131024),
                        Color(0xFF0D0A18)
                    )
                )
            )
            .border(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(tier.primaryColor.copy(alpha = 0.6f), Color(0x22FFFFFF))
                ),
                RoundedCornerShape(26.dp)
            )
            .padding(vertical = 18.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Trophy with Aura
            Box(contentAlignment = Alignment.Center) {
                // Soft radial glow aura
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .blur(28.dp)
                        .background(tier.primaryColor.copy(alpha = 0.35f), CircleShape)
                )

                // 3D Trophy Image from WebP
                Image(
                    painter = painterResource(tier.trophyResId),
                    contentDescription = tier.vietnameseTitle,
                    modifier = Modifier
                        .size(130.dp)
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // League Name
            Text(
                text = tier.vietnameseTitle.uppercase(),
                color = tier.primaryColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            // Subtitle
            Text(
                text = tier.subtitle,
                color = Color(0xCCFFFFFF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Badges Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isUserCurrentTier) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(tier.primaryColor.copy(alpha = 0.22f))
                            .border(1.dp, tier.primaryColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⭐ GIẢI ĐẤU CỦA BẠN",
                            color = tier.primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Countdown Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Còn $countdownText",
                            color = Color(0xFFF1F5F9),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 5-Cup League Selector Tabs (Đồng, Bạc, Vàng, Bạch Kim, Kim Cương)
 */
@Composable
fun TierSelectorBar(
    selectedTier: LeagueTier,
    userTier: LeagueTier,
    onTierSelected: (LeagueTier) -> Unit
) {
    val tiers = LeagueTier.entries.toTypedArray()
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tiers.forEach { tier ->
            val isSelected = selectedTier == tier
            val isUserTier = userTier == tier

            val tabBg by animateColorAsState(
                targetValue = if (isSelected) tier.primaryColor.copy(alpha = 0.25f) else Color(0x12FFFFFF),
                animationSpec = tween(200),
                label = "tab_bg"
            )
            val tabBorder by animateColorAsState(
                targetValue = if (isSelected) tier.primaryColor else Color(0x20FFFFFF),
                animationSpec = tween(200),
                label = "tab_border"
            )
            val tabTextColor by animateColorAsState(
                targetValue = if (isSelected) tier.primaryColor else Color(0x99FFFFFF),
                animationSpec = tween(200),
                label = "tab_text"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(tabBg)
                    .border(1.2.dp, tabBorder, RoundedCornerShape(14.dp))
                    .clickable { onTierSelected(tier) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(tier.trophyResId),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tier.vietnameseTitle.replace("Giải ", ""),
                        color = tabTextColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                    )
                    if (isUserTier) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF51FAC1))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Thanh giải thích 3 phân vùng (Top 1-5, 6-15, 16+)
 */
@Composable
fun LeagueRulesLegendBar(strings: AppStrings) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x1EFFFFFF), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Promotion Rule
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("▲", color = Color(0xFF51FAC1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Top 1-5: Thăng cúp", color = Color(0xFF51FAC1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color(0x33FFFFFF)))

        // Safe Rule
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("—", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Hạng 6-15: Giữ", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }

        Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color(0x33FFFFFF)))

        // Demotion Rule
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("▼", color = Color(0xFFFF5C5C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Hạng 16+: Tụt cúp", color = Color(0xFFFF5C5C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Royal Top 3 Podium Vinh Danh
 */
@Composable
fun RoyalPodiumSection(
    first: RankPlayer,
    second: RankPlayer,
    third: RankPlayer,
    tier: LeagueTier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .height(230.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // Rank 2 (Silver)
        RoyalPodiumItem(player = second, rank = 2, color = Color(0xFFC0C0C0), pillarHeight = 115.dp)

        // Rank 1 (Gold/Tier Primary)
        RoyalPodiumItem(player = first, rank = 1, color = Color(0xFFFFD166), pillarHeight = 150.dp, isChampion = true)

        // Rank 3 (Bronze)
        RoyalPodiumItem(player = third, rank = 3, color = Color(0xFFCD7F32), pillarHeight = 90.dp)
    }
}

@Composable
fun RoyalPodiumItem(
    player: RankPlayer,
    rank: Int,
    color: Color,
    pillarHeight: androidx.compose.ui.unit.Dp,
    isChampion: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(105.dp)
    ) {
        // Avatar Box
        Box(contentAlignment = Alignment.Center) {
            if (isChampion) {
                // Crown for Rank 1
                Text(
                    text = "👑",
                    fontSize = 24.sp,
                    modifier = Modifier.offset(y = (-32).dp)
                )
            }

            // Glow Circle
            Box(
                modifier = Modifier
                    .size(if (isChampion) 76.dp else 58.dp)
                    .blur(14.dp)
                    .background(color.copy(alpha = 0.5f), CircleShape)
            )

            // Avatar Container
            Box(
                modifier = Modifier
                    .size(if (isChampion) 68.dp else 52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14141E))
                    .border(2.5.dp, color, CircleShape)
            ) {
                if (player.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = player.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(28.dp)
                    )
                }
            }

            // Green promotion arrow badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF51FAC1))
                    .border(1.dp, Color(0xFF0F0C20), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("▲", color = Color(0xFF0F172A), fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Pillar Platform
        Box(
            modifier = Modifier
                .width(86.dp)
                .height(pillarHeight)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0.1f), Color.Transparent)
                    )
                )
                .border(
                    1.2.dp,
                    color.copy(alpha = 0.7f),
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = "#$rank",
                    color = color,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (player.isCurrentUser) "${player.name} (Bạn)" else player.name,
                    color = if (player.isCurrentUser) Color(0xFF51FAC1) else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${player.score} XP",
                    color = Color(0xFFFFD166),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * Item từng hàng trong danh sách xếp hạng
 */
@Composable
fun LeagueRankItem(player: RankPlayer, tier: LeagueTier) {
    val zoneColor = when (player.zone) {
        LeagueZone.PROMOTION -> Color(0xFF51FAC1)
        LeagueZone.SAFE -> Color(0xFF94A3B8)
        LeagueZone.DEMOTION -> Color(0xFFFF5C5C)
    }

    val arrowSymbol = when (player.zone) {
        LeagueZone.PROMOTION -> "▲"
        LeagueZone.SAFE -> "—"
        LeagueZone.DEMOTION -> "▼"
    }

    val itemBg = if (player.isCurrentUser) {
        Color(0x2851FAC1)
    } else {
        Color(0x14FFFFFF)
    }

    val itemBorder = if (player.isCurrentUser) {
        Color(0xFF51FAC1)
    } else {
        zoneColor.copy(alpha = 0.3f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(itemBg)
            .border(if (player.isCurrentUser) 1.5.dp else 1.dp, itemBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Number & Arrow
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(46.dp)
        ) {
            Text(
                text = "#${player.rank}",
                color = if (player.isCurrentUser) Color(0xFF51FAC1) else Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = arrowSymbol,
                color = zoneColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF14141E))
                .border(1.2.dp, if (player.isCurrentUser) Color(0xFF51FAC1) else zoneColor, CircleShape)
        ) {
            if (player.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = player.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = zoneColor,
                    modifier = Modifier.align(Alignment.Center).size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name & Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = player.name,
                    color = if (player.isCurrentUser) Color(0xFF51FAC1) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (player.isCurrentUser) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x3351FAC1))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BẠN",
                            color = Color(0xFF51FAC1),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            if (player.streak > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF9F1C),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${player.streak} ngày streak",
                        color = Color(0x99FFFFFF),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Score Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x22FFD166))
                .border(1.dp, Color(0x44FFD166), RoundedCornerShape(10.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_action_star),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${player.score} XP",
                    color = Color(0xFFFFD166),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Vạch phân cách các khu vực giải đấu
 */
@Composable
fun ZoneDividerBanner(title: String, subtitle: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, color.copy(alpha = 0.5f))))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
            Text(
                text = subtitle,
                color = color.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.5f), Color.Transparent)))
        )
    }
}

/**
 * Thẻ ghim người dùng hiện tại ở đáy màn hình
 */
@Composable
fun CurrentUserLeagueBottomCard(
    player: RankPlayer,
    userTier: LeagueTier,
    isViewingOwnTier: Boolean,
    xpGapToTop5: Int?,
    xpGapToSafe: Int?,
    strings: AppStrings
) {
    val zoneColor = when (player.zone) {
        LeagueZone.PROMOTION -> Color(0xFF51FAC1)
        LeagueZone.SAFE -> Color(0xFF94A3B8)
        LeagueZone.DEMOTION -> Color(0xFFFF5C5C)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF51E1438), Color(0xF50D0A18))
                )
            )
            .border(1.5.dp, zoneColor, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(userTier.trophyResId),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${userTier.vietnameseTitle.uppercase()} • HẠNG CỦA BẠN",
                    color = userTier.primaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
            }

            // Dynamic guidance message
            if (player.rank in 1..5) {
                Text(
                    text = "🟢 Đang trong vùng Thăng hạng! ▲",
                    color = Color(0xFF51FAC1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (xpGapToTop5 != null) {
                Text(
                    text = "🔥 Cần +$xpGapToTop5 XP lên Top 5",
                    color = Color(0xFFFFD166),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (xpGapToSafe != null) {
                Text(
                    text = "⚠️ Cần +$xpGapToSafe XP để trụ hạng",
                    color = Color(0xFFFF5C5C),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Info Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Number
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(zoneColor.copy(alpha = 0.2f))
                    .border(1.dp, zoneColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${player.rank}",
                    color = zoneColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14141E))
                    .border(1.5.dp, zoneColor, CircleShape)
            ) {
                if (player.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = player.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = zoneColor,
                        modifier = Modifier.align(Alignment.Center).size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${player.name} (Bạn)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = userTier.subtitle,
                    color = userTier.primaryColor,
                    fontSize = 11.sp
                )
            }

            // Score Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x3351FAC1))
                    .border(1.dp, Color(0x6651FAC1), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_action_star),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${player.score} XP",
                        color = Color(0xFF51FAC1),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Modal hiển thị kết quả mùa giải Thứ 2 đầu tuần (Thăng hạng, Trụ hạng, Tụt hạng)
 */
@Composable
fun LeagueSettlementModal(
    result: LeagueSettlementResult,
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val (title, message, trophyRes, accentColor) = when (result) {
        is LeagueSettlementResult.Promoted -> {
            Tuple4(
                strings.promotedTitle,
                "Chúc mừng bạn đã kết thúc tuần trước ở vị trí #${result.finalRank} và xuất sắc thăng hạng lên ${result.newTier.vietnameseTitle}!",
                result.newTier.trophyResId,
                Color(0xFF51FAC1)
            )
        }
        is LeagueSettlementResult.Maintained -> {
            Tuple4(
                strings.maintainedTitle,
                "Bạn đã kết thúc tuần trước ở vị trí #${result.finalRank} và tiếp tục trụ hạng thành công tại ${result.currentTier.vietnameseTitle}!",
                result.currentTier.trophyResId,
                Color(0xFFFFD166)
            )
        }
        is LeagueSettlementResult.Demoted -> {
            Tuple4(
                strings.demotedTitle,
                "Bạn đã kết thúc tuần trước ở vị trí #${result.finalRank} và tạm thời tụt xuống ${result.newTier.vietnameseTitle}. Hãy luyện tập tuần này để lấy lại cúp nhé!",
                result.newTier.trophyResId,
                Color(0xFFFF5C5C)
            )
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16122E)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, accentColor, RoundedCornerShape(26.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glow & Trophy
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .blur(24.dp)
                            .background(accentColor.copy(alpha = 0.4f), CircleShape)
                    )
                    Image(
                        painter = painterResource(trophyRes),
                        contentDescription = null,
                        modifier = Modifier.size(110.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    color = accentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text(
                        text = if (result is LeagueSettlementResult.Demoted) strings.tryHarder else strings.awesome,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun EmptyLeagueState(selectedTier: LeagueTier, strings: AppStrings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(selectedTier.trophyResId),
            contentDescription = null,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Chưa có ai ở ${selectedTier.vietnameseTitle}",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Hãy học thật chăm chỉ để trở thành người đầu tiên đạt được chiếc cúp này!",
            color = Color(0x99FFFFFF),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
