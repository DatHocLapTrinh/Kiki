package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ARCHITECTURAL DESIGN SYSTEM UTILITY:
 * Tự động tính toán khoảng đệm an toàn cho thanh dock nút bấm đáy trên TOÀN BỘ ứng dụng.
 * Khắc phục triệt để lỗi mất WindowInsets trong Sub-Window (Dialog / Modal) và
 * đảm bảo nút bấm luôn nằm phía trên thanh điều hướng hệ thống (Gesture pill / 3 phím ảo)
 * cũng như không bị góc bo màn hình (Display Corner Radius) cắt xén.
 */
fun Modifier.safeBottomDockPadding(
    minClearance: Dp = 36.dp,
    extraPadding: Dp = 16.dp
): Modifier = composed {
    val navInsets = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Nếu hệ thống trả về insets hợp lệ (> 0dp) thì dùng insets + extraPadding.
    // Nếu insets bị nuốt (như trong Compose Dialog = 0dp), tự động kích hoạt ngưỡng an toàn tối thiểu (minClearance + extraPadding).
    val baseClearance = if (navInsets > 0.dp) navInsets else minClearance
    this.padding(bottom = baseClearance + extraPadding)
}

/**
 * Standardized Unified Bottom Action Dock Component
 * Chuẩn hóa toàn bộ dock hành động ở đáy màn hình trong Design System Kiki.
 */
@Composable
fun KikiBottomActionDock(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF140F24),
    borderColor: Brush = Brush.horizontalGradient(listOf(Color(0x4D51FAC1), Color(0x33FFFFFF), Color(0x4D51FAC1))),
    minClearance: Dp = 36.dp,
    extraPadding: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        color = backgroundColor,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeBottomDockPadding(minClearance = minClearance, extraPadding = extraPadding)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
