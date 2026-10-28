package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.viewmodel.StudyViewModel

// ===================== DATA MODELS =====================

enum class ShopItemType {
    HEART,   // Trái tim - bảo vệ khi trả lời sai
    SHIELD   // Khiên - bảo vệ chuỗi streak
}

data class ShopItem(
    val id: String,
    val type: ShopItemType,
    val name: String,
    val description: String,
    val starCost: Int,
    val quantity: Int,        // Số lượng mua được mỗi lần
    val icon: ImageVector,
    val iconColor: Color,
    val cardGradient: List<Color>,
    val borderColor: Color
)

// ===================== MAIN SCREEN =====================

@Composable
fun ShopScreen(viewModel: StudyViewModel) {
    val isEnglish by viewModel.isEnglish.observeAsState(false)
    val stars by viewModel.stars.observeAsState(0)
    val hearts by viewModel.hearts.observeAsState(5)
    val streakShields by viewModel.streakShields.observeAsState(0)
    val haptic = LocalHapticFeedback.current

    var showConfirmDialog by remember { mutableStateOf<ShopItem?>(null) }
    var showSuccessToast by remember { mutableStateOf<String?>(null) }

    val shopItems = listOf(
        ShopItem(
            id = "heart_1",
            type = ShopItemType.HEART,
            name = if (isEnglish) "Heart Potion" else "Hồi Phục Tim",
            description = if (isEnglish) "Restores 1 heart. Protects against wrong answers!" else "Khôi phục 1 trái tim. Bảo vệ bạn khi trả lời sai bài học!",
            starCost = 10,
            quantity = 1,
            icon = Icons.Default.Favorite,
            iconColor = Color(0xFFFF6B8A),
            cardGradient = listOf(Color(0xFF2C1326), Color(0xFF191128)),
            borderColor = Color(0xFFFF6B8A)
        ),
        ShopItem(
            id = "heart_3",
            type = ShopItemType.HEART,
            name = if (isEnglish) "Heart Bundle ×3" else "Gói Tim ×3",
            description = if (isEnglish) "Get 3 hearts at a discount. Smart savings!" else "Nhận 3 trái tim giá ưu đãi. Tiết kiệm hơn!",
            starCost = 25,
            quantity = 3,
            icon = Icons.Default.Favorite,
            iconColor = Color(0xFFFF6B8A),
            cardGradient = listOf(Color(0xFF33152C), Color(0xFF1E132E)),
            borderColor = Color(0xFFFF6B8A)
        ),
        ShopItem(
            id = "shield_1",
            type = ShopItemType.SHIELD,
            name = if (isEnglish) "Streak Shield" else "Khiên Bảo Vệ Chuỗi",
            description = if (isEnglish) "Protects your streak for 1 day if you miss a session!" else "Bảo vệ chuỗi streak 1 ngày nếu bạn lỡ quên học!",
            starCost = 30,
            quantity = 1,
            icon = Icons.Default.Shield,
            iconColor = Color(0xFF4FC3F7),
            cardGradient = listOf(Color(0xFF14243D), Color(0xFF12162C)),
            borderColor = Color(0xFF4FC3F7)
        ),
        ShopItem(
            id = "shield_3",
            type = ShopItemType.SHIELD,
            name = if (isEnglish) "Shield Bundle ×3" else "Gói Khiên ×3",
            description = if (isEnglish) "3 streak shields at a great price. Keep streaks alive!" else "3 khiên bảo vệ giá tốt. Duy trì chuỗi ngày liên tục!",
            starCost = 75,
            quantity = 3,
            icon = Icons.Default.Shield,
            iconColor = Color(0xFF4FC3F7),
            cardGradient = listOf(Color(0xFF172B45), Color(0xFF131830)),
            borderColor = Color(0xFF4FC3F7)
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Header ──
            item(key = "shop_header") {
                ShopHeader(isEnglish = isEnglish)
            }

            // ── Currency Bar (Làm nổi bật số dư) ──
            item(key = "currency_bar") {
                CurrencyBar(
                    stars = stars,
                    hearts = hearts,
                    shields = streakShields,
                    isEnglish = isEnglish
                )
            }

            item(key = "item_heart_1") {
                ShopItemCard(
                    item = shopItems[0],
                    userStars = stars,
                    isEnglish = isEnglish,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showConfirmDialog = shopItems[0]
                    }
                )
            }

            item(key = "item_heart_3") {
                ShopItemCard(
                    item = shopItems[1],
                    userStars = stars,
                    isEnglish = isEnglish,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showConfirmDialog = shopItems[1]
                    }
                )
            }

            item(key = "item_shield_1") {
                ShopItemCard(
                    item = shopItems[2],
                    userStars = stars,
                    isEnglish = isEnglish,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showConfirmDialog = shopItems[2]
                    }
                )
            }

            item(key = "item_shield_3") {
                ShopItemCard(
                    item = shopItems[3],
                    userStars = stars,
                    isEnglish = isEnglish,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showConfirmDialog = shopItems[3]
                    }
                )
            }
        }

        // Toast thông báo mua thành công
        showSuccessToast?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2000)
                showSuccessToast = null
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF51FAC1))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(msg, color = Color(0xFF0A0A1A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    // ── Confirm Dialog ──
    showConfirmDialog?.let { item ->
        ShopConfirmDialog(
            item = item,
            userStars = stars,
            isEnglish = isEnglish,
            onConfirm = {
                val success = viewModel.purchaseShopItem(item.id, item.type, item.starCost, item.quantity)
                if (success) {
                    val msg = if (isEnglish)
                        "✅ Purchased! ${item.name} ×${item.quantity}"
                    else
                        "✅ Mua thành công! ${item.name} ×${item.quantity}"
                    showSuccessToast = msg
                }
                showConfirmDialog = null
            },
            onDismiss = { showConfirmDialog = null }
        )
    }
}

// ===================== HEADER =====================

@Composable
fun ShopHeader(isEnglish: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x33FFD166))
                .border(1.5.dp, Color(0x66FFD166), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Store,
                contentDescription = null,
                tint = Color(0xFFFFD166),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = if (isEnglish) "Reward Shop" else "Cửa Hàng Đổi Thưởng",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (isEnglish) "Spend ⭐ Stars to get power-ups!" else "Dùng ⭐ Sao để đổi vật phẩm hỗ trợ!",
                color = Color(0xB3FFFFFF),
                fontSize = 13.sp
            )
        }
    }
}

// ===================== CURRENCY BAR =====================

@Composable
fun CurrencyBar(stars: Int, hearts: Int, shields: Int, isEnglish: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF281E44), Color(0xFF171128))
                )
            )
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(Color(0x80FFD166), Color(0x55FF6B8A), Color(0x804FC3F7))
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(vertical = 16.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrencyChip(
                emoji = "⭐",
                value = stars,
                label = if (isEnglish) "Stars" else "Sao",
                color = Color(0xFFFFD166)
            )
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .width(1.dp)
                    .background(Color(0x33FFFFFF))
            )
            CurrencyChip(
                emoji = "❤️",
                value = hearts,
                label = if (isEnglish) "Hearts" else "Trái Tim",
                color = Color(0xFFFF6B8A)
            )
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .width(1.dp)
                    .background(Color(0x33FFFFFF))
            )
            CurrencyChip(
                emoji = "🛡️",
                value = shields,
                label = if (isEnglish) "Shields" else "Khiên",
                color = Color(0xFF4FC3F7)
            )
        }
    }
}

@Composable
fun CurrencyChip(emoji: String, value: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value.toString(),
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = Color(0xB3FFFFFF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ===================== SHOP ITEM CARD =====================

@Composable
fun ShopItemCard(
    item: ShopItem,
    userStars: Int,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    val canAfford = userStars >= item.starCost

    // Card nổi bật rõ ràng so với nền (gradient sâu, viền sáng màu đặc trưng)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(item.cardGradient)
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        item.borderColor.copy(alpha = if (canAfford) 0.8f else 0.45f),
                        item.borderColor.copy(alpha = if (canAfford) 0.35f else 0.2f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(enabled = canAfford) { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon vật phẩm nổi bật
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(item.iconColor.copy(alpha = 0.2f))
                    .border(1.5.dp, item.iconColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.iconColor,
                    modifier = Modifier.size(30.dp)
                )
                // Badge số lượng (ví dụ ×3)
                if (item.quantity > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD166))
                            .border(1.dp, Color(0xFF140F26), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "×${item.quantity}",
                            color = Color(0xFF140F26),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Tên + mô tả (luôn rõ ràng, sắc nét)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    color = Color(0xB3FFFFFF),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Cột giá sao & nút mua
            Column(horizontalAlignment = Alignment.End) {
                // Hộp giá sao nổi bật
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FFD166))
                        .border(1.2.dp, Color(0x99FFD166), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${item.starCost}",
                            color = Color(0xFFFFD166),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Trạng thái / Nút hành động
                if (canAfford) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFD166), Color(0xFFFF9E00))
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "CLAIM" else "ĐỔI",
                            color = Color(0xFF140E26),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.5.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x2BFF6B8A))
                            .border(1.dp, Color(0x66FF6B8A), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "Need ⭐" else "Chưa đủ ⭐",
                            color = Color(0xFFFF8FA3),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ===================== CONFIRM DIALOG =====================

@Composable
fun ShopConfirmDialog(
    item: ShopItem,
    userStars: Int,
    isEnglish: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1236),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(item.iconColor.copy(alpha = 0.2f))
                        .border(1.5.dp, item.iconColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, contentDescription = null, tint = item.iconColor, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = item.name,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.description,
                    color = Color(0xCCFFFFFF),
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x2BFFD166))
                        .border(1.dp, Color(0x66FFD166), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⭐", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.starCost} ${if (isEnglish) "Stars" else "Sao"}",
                        color = Color(0xFFFFD166),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "→ ${userStars - item.starCost} ${if (isEnglish) "left" else "còn lại"}",
                        color = Color(0xB3FFFFFF),
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFFD166), Color(0xFFFF9E00))
                        )
                    )
                    .clickable { onConfirm() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEnglish) "Confirm Purchase" else "Xác Nhận Đổi",
                    color = Color(0xFF140E26),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isEnglish) "Cancel" else "Hủy",
                    color = Color(0x99FFFFFF)
                )
            }
        }
    )
}
