package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val strings = LocalAppStrings.current

    val userTier by viewModel.userTier.observeAsState(LeagueTier.BRONZE)
    val players by viewModel.realtimeLeaderboard.observeAsState(emptyList())
    val isLoading by viewModel.isLeaderboardLoading.observeAsState(false)
    val settlementEvent by viewModel.leagueSettlementEvent.observeAsState(null)
    val weeklyXp by viewModel.weeklyXp.observeAsState(0)
    val millisUntilMonday by viewModel.millisUntilMonday.observeAsState(0L)
    val userName by viewModel.userName.observeAsState("")
    val isAuthLoading by viewModel.isAuthLoading.observeAsState(false)

    // Kiểm tra đăng nhập Google thời gian thực
    var isGoogleLoggedIn by remember {
        mutableStateOf(viewModel.authManager.isGoogleSignedIn())
    }

    LaunchedEffect(Unit) {
        isGoogleLoggedIn = viewModel.authManager.isGoogleSignedIn()
        if (isGoogleLoggedIn) {
            viewModel.initializeLeagueData()
        }
    }

    // Modal vinh danh hoặc thông báo kết toán Thứ 2 đầu tuần
    if (settlementEvent != null) {
        LeagueSettlementModal(
            result = settlementEvent!!,
            strings = strings,
            onDismiss = { viewModel.dismissLeagueSettlementDialog() }
        )
    }

    // Nếu chưa đăng nhập bằng Google, hiển thị cổng mở khóa Bảng Xếp Hạng
    if (!isGoogleLoggedIn) {
        GoogleSignInGateScreen(
            userTier = userTier,
            isLoading = isAuthLoading,
            onSignInClick = {
                viewModel.signInWithGoogle(
                    activityContext = context,
                    onSuccess = {
                        isGoogleLoggedIn = true
                        viewModel.initializeLeagueData()
                        Toast.makeText(context, "Chào mừng bạn gia nhập Bảng Xếp Hạng!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { errMsg ->
                        Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
        return
    }

    // Người dùng hiện tại trong giải
    val currentUserPlayer = players.find { it.isCurrentUser } ?: RankPlayer(
        rank = if (players.isNotEmpty()) players.size + 1 else 1,
        name = userName.ifBlank { strings.you },
        title = userTier.subtitle,
        score = weeklyXp,
        avatarUrl = "",
        isCurrentUser = true,
        zone = viewModel.leagueManager.determineZone(
            if (players.isNotEmpty()) players.size + 1 else 1,
            userTier
        ),
        tier = userTier
    )

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
            .background(Color(0xFF13171B)) // Nền xám đen phẳng tinh tế chuẩn Duolingo
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Header phong cách Duolingo: Tên giải đấu & Đếm ngược
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Giải đấu ${userTier.vietnameseTitle.replace("Giải ", "")}",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Countdown: ⏱ X NGÀY
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF8E9BAE),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = formatDaysRemaining(millisUntilMonday),
                                color = Color(0xFF8E9BAE),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Nút làm mới nhẹ nhàng
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.fetchTierLeaderboard(userTier)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới",
                            tint = if (isLoading) Color(0xFF51FAC1) else Color(0xFF8E9BAE),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Hàng cúp tiến trình nằm ngang (Duolingo Trophy Track)
                DuolingoTrophyTrack(currentTier = userTier)
            }

            // Divider mờ ngăn cách header với danh sách
            HorizontalDivider(
                color = Color(0x1FFFFFFF),
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Danh sách xếp hạng người dùng thật từ Firebase
            if (isLoading && players.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF51FAC1),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else if (players.isEmpty()) {
                // Chưa có người khác trên Firebase
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(userTier.trophyResId),
                            contentDescription = null,
                            modifier = Modifier.size(90.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Bạn là người tiên phong!",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Hiện tại bạn đang dẫn đầu ${userTier.vietnameseTitle}. Hãy tích lũy thêm XP để củng cố vị trí trước ngày chốt sổ Thứ 2!",
                            color = Color(0x99FFFFFF),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 170.dp)
                ) {
                    itemsIndexed(players, key = { _, p -> "fb_${p.uid}_${p.rank}" }) { index, player ->
                        // Hiển thị vạch phân cách thăng hạng (Top 5)
                        if (index == 5) {
                            ZoneLineDivider(
                                title = "VÙNG THĂNG HẠNG (TOP 5)",
                                color = Color(0xFF51FAC1)
                            )
                        }
                        // Hiển thị vạch phân cách rớt hạng (Top 16+)
                        if (index == 15 && userTier != LeagueTier.BRONZE) {
                            ZoneLineDivider(
                                title = "VÙNG TỤT HẠNG (HẠNG 16+)",
                                color = Color(0xFFFF5C5C)
                            )
                        }

                        DuolingoRankRow(
                            player = player,
                            isCurrentUser = player.isCurrentUser
                        )
                    }
                }
            }
        }

        // 4. Thẻ ghim người dùng hiện tại ở đáy màn hình
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 84.dp)
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            DuolingoCurrentUserCard(
                player = currentUserPlayer,
                userTier = userTier,
                xpGapToTop5 = xpGapToTop5,
                xpGapToSafe = xpGapToSafe
            )
        }
    }
}

/**
 * Hàng Cúp Tiến Trình Duolingo (Trophy Track)
 * 5 Cúp nằm ngang trên mặt phẳng: Các cúp khác nhỏ và mờ, cúp hiện tại nổi bật to hơn
 */
@Composable
fun DuolingoTrophyTrack(currentTier: LeagueTier) {
    val allTiers = LeagueTier.entries.toTypedArray()
    val grayscaleFilter = remember {
        val matrix = ColorMatrix().apply { setToSaturation(0f) }
        ColorFilter.colorMatrix(matrix)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        allTiers.forEach { tier ->
            val isCurrent = tier == currentTier
            val isUnlocked = tier.order <= currentTier.order

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(tier.trophyResId),
                    contentDescription = tier.vietnameseTitle,
                    modifier = Modifier
                        .size(if (isCurrent) 68.dp else 42.dp)
                        .padding(2.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = if (isCurrent) null else grayscaleFilter,
                    alpha = if (isCurrent) 1.0f else if (isUnlocked) 0.45f else 0.25f
                )

                if (isCurrent) {
                    Spacer(modifier = Modifier.height(4.dp))
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

/**
 * Từng dòng trong bảng xếp hạng tối giản mượt mà theo chuẩn Duolingo
 */
@Composable
fun DuolingoRankRow(player: RankPlayer, isCurrentUser: Boolean) {
    val rowBg = if (isCurrentUser) Color(0x1F51FAC1) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Huy hiệu thứ hạng (Top 1, 2, 3 có huy hiệu ribbon; 4+ là số phẳng)
        Box(
            modifier = Modifier.width(36.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            when (player.rank) {
                1 -> {
                    // Gold Medal #1
                    RankRibbonMedal(rank = 1, bgColor = Color(0xFFFFD166), textColor = Color(0xFF8B5A00))
                }
                2 -> {
                    // Silver Medal #2
                    RankRibbonMedal(rank = 2, bgColor = Color(0xFFC0C0C0), textColor = Color(0xFF4A4A4A))
                }
                3 -> {
                    // Bronze Medal #3
                    RankRibbonMedal(rank = 3, bgColor = Color(0xFFCD7F32), textColor = Color(0xFF4A2500))
                }
                else -> {
                    Text(
                        text = "${player.rank}",
                        color = if (isCurrentUser) Color(0xFF51FAC1) else Color(0xFF8E9BAE),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 2. Avatar tròn
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E262B))
                .border(
                    width = if (isCurrentUser) 2.dp else 1.dp,
                    color = if (isCurrentUser) Color(0xFF51FAC1) else Color(0x33FFFFFF),
                    shape = CircleShape
                )
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
                    tint = if (isCurrentUser) Color(0xFF51FAC1) else Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // 3. Tên người dùng thật từ Firebase
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isCurrentUser) "${player.name} (Bạn)" else player.name,
                color = if (isCurrentUser) Color(0xFF51FAC1) else Color.White,
                fontWeight = if (isCurrentUser) FontWeight.Black else FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (player.streak > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF9F1C),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${player.streak}",
                        color = Color(0xFF8E9BAE),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Điểm XP tuần (Ví dụ: "1290 XP")
        Text(
            text = "${player.score} XP",
            color = if (isCurrentUser) Color(0xFF51FAC1) else Color(0xFF8E9BAE),
            fontWeight = FontWeight.Black,
            fontSize = 15.sp
        )
    }
}

/**
 * Huy hiệu Ribbon huy chương Top 1, 2, 3 phong cách Duolingo
 */
@Composable
fun RankRibbonMedal(rank: Int, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$rank",
            color = textColor,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp
        )
    }
}

/**
 * Vạch phân cách thăng hạng / rớt hạng tinh gọn
 */
@Composable
fun ZoneLineDivider(title: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(color.copy(alpha = 0.5f))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(color.copy(alpha = 0.5f))
        )
    }
}

/**
 * Thẻ cố định ở đáy hiển thị trạng thái của người dùng
 */
@Composable
fun DuolingoCurrentUserCard(
    player: RankPlayer,
    userTier: LeagueTier,
    xpGapToTop5: Int?,
    xpGapToSafe: Int?
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C242A)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .border(1.5.dp, Color(0xFF51FAC1), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Number
            Text(
                text = "#${player.rank}",
                color = Color(0xFF51FAC1),
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(36.dp)
            )

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${player.name} (Bạn)",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = when {
                        player.rank in 1..5 -> "🟢 Đang trong vùng Thăng Hạng!"
                        xpGapToTop5 != null -> "🔥 Cần +$xpGapToTop5 XP lên Top 5"
                        xpGapToSafe != null -> "⚠️ Cần +$xpGapToSafe XP để trụ hạng"
                        else -> "⭐ Tiếp tục tích lũy điểm nhé"
                    },
                    color = when {
                        player.rank in 1..5 -> Color(0xFF51FAC1)
                        xpGapToTop5 != null -> Color(0xFFFFD166)
                        else -> Color(0xFFFF5C5C)
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Score
            Text(
                text = "${player.score} XP",
                color = Color(0xFF51FAC1),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/**
 * Màn hình yêu cầu đăng nhập bằng Google để mở khóa Bảng Xếp Hạng
 */
@Composable
fun GoogleSignInGateScreen(
    userTier: LeagueTier,
    isLoading: Boolean,
    onSignInClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13171B))
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Cúp Vàng 3D cắt gọn sắc nét, không viền hộp
            Image(
                painter = painterResource(R.drawable.trophy_gold),
                contentDescription = "Cúp Vàng",
                modifier = Modifier
                    .size(140.dp)
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Mở Khóa Đấu Trường",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Bảng xếp hạng đấu trường chỉ dành cho người dùng đăng nhập tài khoản Google để đồng bộ điểm số và tranh tài cùng cộng đồng học viên thực tế!",
                color = Color(0xFF8E9BAE),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Nút đăng nhập Google phong cách chuẩn
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSignInClick()
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1E262B)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFF1E262B),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Icon Google G
                        Text(
                            text = "G",
                            color = Color(0xFF4285F4),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Tiếp tục với Google",
                            color = Color(0xFF1E262B),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modal hiển thị kết quả mùa giải Thứ 2 đầu tuần
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
                Image(
                    painter = painterResource(trophyRes),
                    contentDescription = null,
                    modifier = Modifier.size(110.dp)
                )

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

private fun formatDaysRemaining(millis: Long): String {
    val totalSec = millis / 1000
    val days = totalSec / (24 * 3600)
    val hours = (totalSec % (24 * 3600)) / 3600
    return if (days > 0) "$days NGÀY" else "$hours GIỜ"
}
