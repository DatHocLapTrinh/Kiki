package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

import androidx.compose.runtime.livedata.observeAsState
import com.example.viewmodel.StudyViewModel

data class RankPlayer(
    val rank: Int,
    val name: String,
    val title: String,
    val score: Int,
    val avatarUrl: String,
    val isCurrentUser: Boolean = false
)

@Composable
fun RankScreen(viewModel: StudyViewModel) {
    val rawLeaderboard by viewModel.leaderboard.observeAsState(emptyList())
    val currentUserId by viewModel.currentUserId.observeAsState(-1L)

    val players = rawLeaderboard.mapIndexed { index, profile ->
        RankPlayer(
            rank = index + 1,
            name = profile.displayName,
            title = viewModel.calculateRankTitle(profile.totalXp),
            score = profile.totalXp,
            avatarUrl = profile.avatarUri ?: "",
            isCurrentUser = profile.userId == currentUserId
        )
    }

    val strings = LocalAppStrings.current
    val currentUserRank = players.find { it.isCurrentUser } ?: RankPlayer(
        rank = 0, 
        name = viewModel.userName.value ?: strings.unknown, 
        title = viewModel.rankTitle.value ?: "Bronze Novice", 
        score = viewModel.xp.value ?: 0, 
        avatarUrl = "", 
        isCurrentUser = true
    )

    Box(modifier = Modifier.fillMaxSize()) {
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 76.dp, end = 24.dp, bottom = 16.dp),
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
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        viewModel.fetchLeaderboard()
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF51FAC1))
                }
            }

            if (players.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(strings.noRankings, color = Color(0xB3FFFFFF), modifier = Modifier.padding(32.dp))
                }
            } else {
                // Podium Top 3
                PodiumSection(
                    first = players.getOrNull(0) ?: RankPlayer(1, "-", strings.unranked, 0, ""),
                    second = players.getOrNull(1) ?: RankPlayer(2, "-", strings.unranked, 0, ""),
                    third = players.getOrNull(2) ?: RankPlayer(3, "-", strings.unranked, 0, "")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Leaderboard List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 200.dp), // Extra padding for sticky bar & bottom nav
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        players.drop(3),
                        key = { _, player -> "rank_${player.rank}_${player.name}" }
                    ) { _, player ->
                        RankListItem(player)
                    }
                }
            }
        }

        // Sticky Bottom Bar for Current User
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp) // Avoid main BottomNavBar
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xE63C0A78), Color(0xE614141E))))
                .border(2.dp, Color(0xFF51FAC1), RoundedCornerShape(20.dp))
        ) {
            RankListItem(player = currentUserRank, isSticky = true)
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
                Text("${player.score}", color = Color(0xB3FFFFFF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSticky) Color.Transparent else Color(0x1AFFFFFF))
            .border(1.dp, if (isSticky) Color.Transparent else Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Number
        Text(
            text = "#${player.rank}",
            color = rankColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            modifier = Modifier.width(40.dp)
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
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Name & Title
        Column(modifier = Modifier.weight(1f)) {
            Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(player.title, color = rankColor, fontSize = 12.sp)
        }
        
        // Score
        Text("${player.score} XP", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
    }
}
