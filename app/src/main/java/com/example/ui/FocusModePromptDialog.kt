package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun FocusModePromptDialog(
    isEnglish: Boolean,
    onEnableFocus: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E1738), Color(0xFF120E22))
                    )
                )
                .border(1.2.dp, Color(0x6651FAC1), RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Focus Icon Box with cosmic glow
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0x2251FAC1))
                        .border(1.5.dp, Color(0xFF51FAC1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = "Focus Mode",
                        tint = Color(0xFF51FAC1),
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Title - Big White Text
                Text(
                    text = if (isEnglish) "Deep Focus Study Mode" else "Chế Độ Tập Trung Tuyệt Đối",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Body text
                Text(
                    text = if (isEnglish)
                        "Would you like Kiki to automatically silence notifications from other apps (Zalo, Facebook, TikTok...) while you study so you maintain pure focus?\n\nYou can easily change this anytime in Settings."
                    else
                        "Bạn có muốn Kiki tự động tắt chuông và làm im lặng thông báo từ các ứng dụng khác (Zalo, Facebook, TikTok...) khi bạn đang học để không bị phân tâm không?\n\nBạn có thể bật/tắt lại bất cứ lúc nào trong mục Cài đặt.",
                    color = Color(0xCCFFFFFF),
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Button: Enable
                Button(
                    onClick = onEnableFocus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF51FAC1)
                    )
                ) {
                    Text(
                        text = if (isEnglish) "Enable Focus Mode" else "Bật Chế Độ Tập Trung",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Button: Later
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Maybe Later" else "Để sau",
                        color = Color(0x99FFFFFF),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

