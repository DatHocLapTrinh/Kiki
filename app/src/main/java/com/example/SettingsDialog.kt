package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

@Composable
fun SettingsDialog(onDismiss: () -> Unit, onLogout: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xF20B0615), Color(0xE30D1630), Color(0xF20B0615))
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-70).dp, y = 90.dp)
                    .size(240.dp)
                    .blur(70.dp)
                    .background(Color(0x553D0B78), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 80.dp, y = (-70).dp)
                    .size(240.dp)
                    .blur(76.dp)
                    .background(Color(0x4430A5C8), CircleShape)
            )
            GalaxyEffectOverlay(modifier = Modifier.matchParentSize())

            // Glassmorphism Container
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0x33FFFFFF))
                    .border(1.dp, Color(0x4DFFFFFF), RoundedCornerShape(32.dp))
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cài đặt Hệ thống",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Account Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x1AFFFFFF))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .border(2.dp, Color(0xFF51FAC1), CircleShape)
                    ) {
                        AsyncImage(
                            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDHEU1luTpzRMj0AWjSdkrnM7_zqT6q0q2FslynO_5_1cv8tT93rUwkLgv3TCVA6OIKrsN3uY_7GvArdkxJRh6QUHgxg1uy_KrdaaDubiosxU1_D7RJSx7EzX8Um8G1GzxzLPPhd-MydROGtSyI5h-5e4CAMRnPcdG4scRuG0MHAiktwaPicNEkblA9GWH7ufeuVt_eJr8Q2FTnfsFGgknh56kd5Eh2wFZzQVxLWOrIYTfKGl1alFeXtVDmralygqCUa_WYd2Ea34w",
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Kiki Explorer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Cấp 12 • Hạng Bạc", color = Color(0xFF51FAC1), fontSize = 14.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Toggles
                SettingsToggle("Nhạc nền (BGM)", Icons.Default.MusicNote, true)
                SettingsToggle("Hiệu ứng âm thanh", Icons.Default.VolumeUp, true)
                SettingsToggle("Rung (Haptics)", Icons.Default.Vibration, true)
                SettingsToggle("Nhắc nhở học tập", Icons.Default.Notifications, false)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Logout Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFE53935), Color(0xFFB71C1C))))
                        .clickable { onLogout() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đăng xuất", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsToggle(title: String, icon: ImageVector, initialValue: Boolean) {
    var checked by remember { mutableStateOf(initialValue) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0x80FFFFFF), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color.White, fontSize = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF51FAC1),
                uncheckedThumbColor = Color(0xFF9E9E9E),
                uncheckedTrackColor = Color(0x4DFFFFFF)
            )
        )
    }
}
