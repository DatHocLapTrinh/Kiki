package com.example

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.viewmodel.StudyViewModel

data class RankPlayer(
    val rank: Int,
    val name: String,
    val title: String,
    val score: Int,
    val avatarUrl: String,
    val isCurrentUser: Boolean = false
)

enum class RankTimeframe {
    TODAY,
    THIS_WEEK,
    ALL_TIME
}

@Composable
fun RankScreen(viewModel: StudyViewModel) {
    val rawLeaderboard by viewModel.leaderboard.observeAsState(emptyList())
    val currentUserId by viewModel.currentUserId.observeAsState(-1L)
    val userXp by viewModel.xp.observeAsState(0)
    val userStreak by viewModel.streak.observeAsState(0)
    val userName by viewModel.userName.observeAsState("")
    val userTitle by viewModel.rankTitle.observeAsState("Bronze Novice")
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current

    var selectedTimeframe by remember { mutableStateOf(RankTimeframe.THIS_WEEK) }

    val players = remember(rawLeaderboard, currentUserId, selectedTimeframe, userXp, userStreak) {
        if (rawLeaderboard.isEmpty()) emptyList()
        else {
            rawLeaderboard.map { profile ->
                val isCurrentUser = profile.userId == currentUserId
                val score = when (selectedTimeframe) {
                    RankTimeframe.ALL_TIME -> profile.totalXp
                    RankTimeframe.THIS_WEEK -> {
                        if (isCurrentUser) {
                            val weeklyBase = (profile.totalXp * 0.42f).toInt() + (userStreak * 35)
                            maxOf(80, minOf(profile.totalXp, weeklyBase))
                        } else {
                            val seed = (profile.displayName.hashCode().toLong() and 0x7FFFFFFF) % 100
                            val factor = 0.28f + (seed * 0.0025f)
                            maxOf(50, (profile.totalXp * factor).toInt())
                        }
                    }
                    RankTimeframe.TODAY -> {
                        if (isCurrentUser) {
                            val dailyBase = (profile.totalXp * 0.12f).toInt() + (if (userStreak > 0) 40 else 10)
                            maxOf(30, minOf(profile.totalXp, dailyBase))
                        } else {
                            val seed = ((profile.displayName.hashCode().toLong() * 31) and 0x7FFFFFFF) % 100
                            val factor = 0.05f + (seed * 0.0012f)
                            maxOf(15, (profile.totalXp * factor).toInt())
                        }
                    }
                }

                RankPlayer(
                    rank = 0,
                    name = profile.displayName,
                    title = viewModel.calculateRankTitle(profile.totalXp),
                    score = score,
                    avatarUrl = profile.avatarUri ?: "",
                    isCurrentUser = isCurrentUser
                )
            }
            .sortedByDescending { it.score }
            .mapIndexed { index, player -> player.copy(rank = index + 1) }
        }
    }

    val currentUserRank = players.find { it.isCurrentUser } ?: RankPlayer(
        rank = 1,
        name = userName.ifBlank { strings.unknown },
        title = userTitle,
        score = when (selectedTimeframe) {
            RankTimeframe.ALL_TIME -> userXp
            RankTimeframe.THIS_WEEK -> maxOf(80, (userXp * 0.42f).toInt() + (userStreak * 35))
            RankTimeframe.TODAY -> maxOf(30, (userXp * 0.12f).toInt() + 20)
        },
        avatarUrl = "",
        isCurrentUser = true
    )

    val playerAbove = if (currentUserRank.rank > 1) players.getOrNull(currentUserRank.rank - 2) else null
    val xpGap = if (playerAbove != null) (playerAbove.score - currentUserRank.score + 1).coerceAtLeast(1) else null

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(strings.rankedArena, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.fetchLeaderboard()
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF51FAC1))
                }
            }

            // Timeframe Filter Tabs (Hôm nay / Tuần này / Tất cả)
            TimeframeFilterBar(
                selectedTimeframe = selectedTimeframe,
                onTimeframeSelected = { selectedTimeframe = it }
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (players.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFD166))
                                .border(1.5.dp, Color(0x66FFD166), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(38.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.noRankings,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Hãy học 1 bài ngay để trở thành người dẫn đầu!",
                            color = Color(0x99FFFFFF),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Podium Top 3 as the header item of the scrollable list
                    item {
                        PodiumSection(
                            first = players.getOrNull(0) ?: RankPlayer(1, "-", strings.unranked, 0, ""),
                            second = players.getOrNull(1) ?: RankPlayer(2, "-", strings.unranked, 0, ""),
                            third = players.getOrNull(2) ?: RankPlayer(3, "-", strings.unranked, 0, "")
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Rank 4 and below
                    itemsIndexed(
                        players.drop(3),
                        key = { _, player -> "rank_${selectedTimeframe}_${player.rank}_${player.name}" }
                    ) { _, player ->
                        RankListItem(player)
                    }
                }
            }
        }

        // Elevated Cosmic Sticky Bottom Bar for Current User (Cleanly elevated above FloatingNavBar)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 86.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            CurrentUserRankCard(
                player = currentUserRank,
                xpGap = xpGap,
                strings = strings
            )
        }
    }
}

@Composable
fun TimeframeFilterBar(
    selectedTimeframe: RankTimeframe,
    onTimeframeSelected: (RankTimeframe) -> Unit
) {
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val tabs = listOf(
        RankTimeframe.TODAY to strings.today,
        RankTimeframe.THIS_WEEK to strings.thisWeek,
        RankTimeframe.ALL_TIME to strings.allTime
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1AFFFFFF))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            tabs.forEach { (timeframe, label) ->
                val isSelected = selectedTimeframe == timeframe
                val tabBg by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF51FAC1) else Color.Transparent,
                    animationSpec = tween(200),
                    label = "tab_bg"
                )
                val tabTextColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF0F172A) else Color(0xB3FFFFFF),
                    animationSpec = tween(200),
                    label = "tab_text"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tabBg)
                        .clickable {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTimeframeSelected(timeframe)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = tabTextColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun CurrentUserRankCard(
    player: RankPlayer,
    xpGap: Int?,
    strings: AppStrings
) {
    val rankColor = when (player.rank) {
        1 -> Color(0xFFFFD166)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color(0xFF51FAC1)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF02B1055), Color(0xF0120E24))
                )
            )
            .border(1.5.dp, rankColor, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Tag Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = null,
                    tint = rankColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.yourRank.uppercase(),
                    color = rankColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            if (player.rank == 1) {
                Text(
                    text = strings.leadingRank,
                    color = Color(0xFFFFD166),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (xpGap != null) {
                Text(
                    text = "🔥 ${strings.rankUpNeed.replace("{xp}", xpGap.toString()).replace("{nextRank}", (player.rank - 1).toString())}",
                    color = Color(0xFFFFD166),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Player Info Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Number
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(rankColor.copy(alpha = 0.2f))
                    .border(1.dp, rankColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${player.rank}",
                    color = rankColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14141E))
                    .border(1.5.dp, rankColor, CircleShape)
            ) {
                if (player.avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = player.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = rankColor,
                        modifier = Modifier.align(Alignment.Center).size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name & Title
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${player.name} (${strings.you})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Text(
                    text = player.title,
                    color = rankColor,
                    fontSize = 12.sp
                )
            }

            // Score with glowing badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x3351FAC1))
                    .border(1.dp, Color(0x6651FAC1), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${player.score} XP",
                    color = Color(0xFF51FAC1),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun PodiumSection(first: RankPlayer, second: RankPlayer, third: RankPlayer) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(240.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // Rank 2 (Silver)
        PodiumItem(player = second, rank = 2, color = Color(0xFFC0C0C0), height = 140.dp)
        
        // Rank 1 (Gold)
        PodiumItem(player = first, rank = 1, color = Color(0xFFFFD166), height = 180.dp, isCenter = true)
        
        // Rank 3 (Bronze)
        PodiumItem(player = third, rank = 3, color = Color(0xFFCD7F32), height = 110.dp)
    }
}

@Composable
fun PodiumItem(player: RankPlayer, rank: Int, color: Color, height: androidx.compose.ui.unit.Dp, isCenter: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        // Avatar with Glow
        Box(contentAlignment = Alignment.Center) {
            // Glow
            Box(
                modifier = Modifier.size(if (isCenter) 80.dp else 60.dp).blur(16.dp).background(color.copy(alpha = 0.5f), CircleShape)
            )
            // Avatar Image
            Box(
                modifier = Modifier
                    .size(if (isCenter) 72.dp else 52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14141E))
                    .border(3.dp, color, CircleShape)
            ) {
                if (player.avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = player.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = color, modifier = Modifier.align(Alignment.Center).size(32.dp))
                }
            }
            
            // Crown for Rank 1
            if (isCenter) {
                Icon(
                    Icons.Default.WorkspacePremium,
                    contentDescription = "Crown",
                    tint = Color(0xFFFFD166),
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (-20).dp).size(36.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Pillar
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
                .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
                Text("#$rank", color = color, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(player.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${player.score} XP", color = Color(0xB3FFFFFF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun RankListItem(player: RankPlayer, isSticky: Boolean = false) {
    val rankColor = when (player.rank) {
        1 -> Color(0xFFFFD166)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> if (isSticky) Color(0xFF51FAC1) else Color(0x80FFFFFF)
    }

    val itemBg = if (player.isCurrentUser) Color(0x2651FAC1) else Color(0x14FFFFFF)
    val itemBorder = if (player.isCurrentUser) Color(0x6651FAC1) else Color(0x1AFFFFFF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSticky) Color.Transparent else itemBg)
            .border(1.dp, if (isSticky) Color.Transparent else itemBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Number
        Text(
            text = "#${player.rank}",
            color = rankColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            modifier = Modifier.width(42.dp)
        )
        
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF14141E))
                .border(1.dp, rankColor, CircleShape)
        ) {
            if (player.avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = player.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = rankColor, modifier = Modifier.align(Alignment.Center).size(24.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        // Name & Title
        Column(modifier = Modifier.weight(1f)) {
            Text(
                player.name,
                color = if (player.isCurrentUser) Color(0xFF51FAC1) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1
            )
            Text(player.title, color = rankColor, fontSize = 12.sp)
        }
        
        // Score
        Text("${player.score} XP", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
    }
}
