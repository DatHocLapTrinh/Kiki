package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.CosmicPassConfig
import com.example.model.CosmicPassTier

/**
 * Hộp thoại Paywall "Kiki Cosmic Pass 👑" chuẩn phong cách Super Duolingo & ELSA Speak.
 * Sử dụng hình ảnh 3D Pixar Royal Cosmic Kiki, mô hình định giá Decoy Effect
 * và bảng so sánh đặc quyền Vô hạn Tim / Trợ lý AI không giới hạn.
 */
@Composable
fun CosmicPassPaywallDialog(
    isCurrentActive: Boolean = false,
    onSubscribeTier: (CosmicPassTier) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedTier by remember { mutableStateOf(CosmicPassTier.ANNUAL) }
    var isBenefitsExpanded by remember { mutableStateOf(true) }
    var isSubscribedCelebration by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xDD070514))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.92f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Chặn click xuyên thấu
                    )
                    .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = Color(0xFFFFD700),
                        spotColor = Color(0xFFC084FC)
                    ),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF7100D22))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 1.6.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0xFFFFD700),
                                    Color(0xFFFFA500),
                                    Color(0xFFC084FC),
                                    Color(0xFF00F5D4)
                                )
                            ),
                            shape = RoundedCornerShape(30.dp)
                        )
                ) {
                    // Header Bar với nút đóng X
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFFFD700), Color(0xFFFB923C))
                                        )
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "VIP PASS",
                                    color = Color(0xFF160E02),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "KIKI COSMIC PASS",
                                color = Color(0xFFFFD700),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Thân nội dung có thể cuộn
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ─── HERO BANNER 3D ROYAL COSMIC KIKI ───
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0x55FFD700),
                                            Color(0x33A855F7),
                                            Color(0x11070514)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0x88FFD700), Color(0x33C084FC))
                                    ),
                                    shape = RoundedCornerShape(22.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.kiki_cosmic_pass_hero),
                                contentDescription = "Royal Cosmic Kiki 3D Mascot",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(22.dp)),
                                contentScale = ContentScale.Crop
                            )

                            // Tag phủ dưới ảnh
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color(0xEE0A0718))
                                        )
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✨ ĐỒNG HÀNH KHÔNG GIỚI HẠN CÙNG KIKI HOÀNG GIA ✨",
                                    color = Color(0xFFFFE066),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tiêu đề & Lời hứa giá trị
                        Text(
                            text = "Khai Mở Toàn Bộ Sức Mạnh Vũ Trụ 👑",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Học không giới hạn • Tự tin bứt phá giao tiếp phản xạ tự nhiên",
                            color = Color(0xCCFFFFFF),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // ─── DANH SÁCH GÓI CƯỚC (PRICING TIERS) ───
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CosmicPassTier.values().forEach { tier ->
                                val isSelected = (selectedTier == tier)
                                PricingTierCard(
                                    tier = tier,
                                    isSelected = isSelected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedTier = tier
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ─── BẢNG SO SÁNH ĐẶC QUYỀN (BENEFITS COMPARISON) ───
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x18FFD700))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isBenefitsExpanded = !isBenefitsExpanded
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "💎 SO SÁNH QUYỀN LỢI VIP",
                                    color = Color(0xFFFFD700),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Icon(
                                imageVector = if (isBenefitsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(visible = isBenefitsExpanded) {
                            Column(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CosmicPassConfig.benefits.forEach { benefit ->
                                    BenefitComparisonRow(benefit = benefit)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // ─── FOOTER CTA NÚT BẤM MUA / DÙNG THỬ ───
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xF0130F2A))
                            .border(
                                width = 1.dp,
                                color = Color(0x33FFD700),
                                shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSubscribeTier(selectedTier)
                                    isSubscribedCelebration = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .shadow(
                                        elevation = 14.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        ambientColor = Color(0xFFFFD700),
                                        spotColor = Color(0xFFFFA500)
                                    ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFFFFD700),
                                                    Color(0xFFFFA500),
                                                    Color(0xFFF43F5E)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFF180E02),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = when (selectedTier) {
                                                CosmicPassTier.ANNUAL -> "DÙNG THỬ 7 NGÀY MIỄN PHÍ"
                                                CosmicPassTier.MONTHLY -> "NÂNG CẤP GÓI 1 THÁNG NGAY"
                                                CosmicPassTier.STAR_BUNDLE -> "NẠP 100 ĐÁ QUÝ VŨ TRỤ"
                                            },
                                            color = Color(0xFF180E02),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = when (selectedTier) {
                                    CosmicPassTier.ANNUAL -> "Sau 7 ngày: 708.000 đ/năm (59.000 đ/tháng) • Hủy bất kỳ lúc nào"
                                    CosmicPassTier.MONTHLY -> "Thanh toán 99.000 đ/tháng • Tự động gia hạn • Hủy mọi lúc"
                                    CosmicPassTier.STAR_BUNDLE -> "Thanh toán 1 lần 19.000 đ • Nhận 100 Đá Quý ngay lập tức"
                                },
                                color = Color(0x99FFFFFF),
                                fontSize = 10.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thẻ hiển thị một gói cước (Pricing Tier Card) với hiệu ứng Decoy Effect.
 */
@Composable
private fun PricingTierCard(
    tier: CosmicPassTier,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFFFFD700) else Color(0x28FFFFFF)
    val bgColor = if (isSelected) Color(0x2BFFD700) else Color(0x14FFFFFF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) {
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFC084FC))
                    )
                } else {
                    Brush.horizontalGradient(listOf(borderColor, borderColor))
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Radio Check Circle
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFFFFD700) else Color(0x22FFFFFF))
                            .border(1.5.dp, if (isSelected) Color(0xFFFFD700) else Color(0x55FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF160E02),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = tier.title,
                            color = if (isSelected) Color.White else Color(0xDDFFFFFF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tier.periodLabel,
                            color = Color(0x88FFFFFF),
                            fontSize = 11.sp
                        )
                    }
                }

                // Cột giá
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = tier.monthlyEquivalent,
                        color = if (isSelected) Color(0xFFFFD700) else Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    tier.originalPriceText?.let { orig ->
                        Text(
                            text = orig,
                            color = Color(0x66FFFFFF),
                            fontSize = 10.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }

            // Badge tiết kiệm nếu có
            tier.savingsBadge?.let { badge ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (tier.isBestValue) {
                                Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFB923C)))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0x3300F5D4), Color(0x3300F5D4)))
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = if (tier.isBestValue) Color(0xFF160E02) else Color(0xFF00F5D4),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "🎁 " + tier.bonusDescription,
                color = Color(0xB3FFD700),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Một dòng so sánh đặc quyền giữa Free và VIP.
 */
@Composable
private fun BenefitComparisonRow(benefit: com.example.model.CosmicPassBenefit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0EFFFFFF))
            .border(0.6.dp, Color(0x1FFFFFFF), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = benefit.iconEmoji,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = benefit.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = benefit.vipAdvantage,
                    color = Color(0xFFFFD700),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
