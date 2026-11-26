package com.example.ui

import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Full-screen in-window overlay destinations hosted directly in MainNavigation so they
 * inherit true Activity edge-to-edge WindowInsets without OEM Dialog sub-window shifting.
 */
enum class FullscreenOverlayType {
    VOCABULARY_VAULT,
    WEAK_POINTS_CLINIC
}

/**
 * CompositionLocal launcher provided by MainNavigation to open full-screen views in-window.
 */
val LocalOpenFullscreenOverlay = staticCompositionLocalOf<((FullscreenOverlayType) -> Unit)?> { null }

/**
 * Renders full-screen modal content inside the Activity's root Compose hierarchy, handling
 * back navigation and blocking background touches without spawning a floating Dialog window.
 */
@Composable
fun FullscreenInWindowOverlay(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        content()
    }
}

/**
 * Configures a Compose Dialog's native Window to span edge-to-edge full-screen cleanly,
 * ensuring valid system bar / IME insets and dark-theme light system bar icons across OEM ROMs.
 */
@Composable
fun ConfigureFullscreenDialogWindow(adjustResize: Boolean = false) {
    val view = LocalView.current
    DisposableEffect(view, adjustResize) {
        val window = (view.parent as? DialogWindowProvider)?.window
        window?.let { w ->
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            w.setDimAmount(0f)
            w.setBackgroundDrawableResource(android.R.color.transparent)
            if (adjustResize) {
                @Suppress("DEPRECATION")
                w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
            WindowCompat.setDecorFitsSystemWindows(w, false)
            WindowInsetsControllerCompat(w, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
        onDispose {}
    }
}

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
    val bottomSystemInset = WindowInsets.navigationBars
        .union(WindowInsets.displayCutout)
        .asPaddingValues()
        .calculateBottomPadding()
    // Đảm bảo luôn đạt ít nhất minClearance ngay cả trên các ROM HyperOS/MIUI
    // báo navigationBars nhỏ hơn vùng vuốt cử chỉ & bo góc màn hình thực tế.
    val baseClearance = maxOf(bottomSystemInset, minClearance)
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
