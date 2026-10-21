package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.viewmodel.StudyViewModel

@Composable
fun SettingsDialog(
    viewModel: StudyViewModel? = null,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val currentProvider = viewModel?.aiProvider?.observeAsState(StudyViewModel.PROVIDER_AUTO)?.value ?: StudyViewModel.PROVIDER_AUTO
    val currentUniversalKey = viewModel?.universalAiKey?.observeAsState("")?.value ?: ""
    val autoTtsEnabled = viewModel?.audioDialogAutoTts?.observeAsState(true)?.value ?: true
    val batterySaverEnabled = viewModel?.batterySaver?.observeAsState(false)?.value ?: false
    val soundPrefs = remember {
        context.getSharedPreferences(com.example.audio.SoundEffectManager.PREFS_NAME, android.content.Context.MODE_PRIVATE)
    }
    var soundEnabled by remember { mutableStateOf(soundPrefs.getBoolean("sound_enabled", true)) }
    var universalKeyInput by remember(currentUniversalKey) { mutableStateOf(currentUniversalKey) }
    var universalKeyVisible by remember { mutableStateOf(false) }

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
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0x33FFFFFF))
                    .border(1.dp, Color(0x4DFFFFFF), RoundedCornerShape(32.dp))
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
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

                Spacer(modifier = Modifier.height(20.dp))

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
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .border(2.dp, Color(0xFF51FAC1), CircleShape)
                    ) {
                        AsyncImage(
                            model = R.drawable.kiki_mascot_head,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        val userName = viewModel?.userName?.observeAsState("Kiki Explorer")?.value ?: "Kiki Explorer"
                        val rankTitle = viewModel?.rankTitle?.observeAsState("Bronze Novice")?.value ?: "Bronze Novice"
                        Text(userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(rankTitle, color = Color(0xFF51FAC1), fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // AI Engine Selection & Config Section
                // Header: Trí Tuệ Nhân Tạo & Gia Sư AI
                Text(
                    "Trí Tuệ Nhân Tạo Đa Nền Tảng (Universal AI)",
                    color = Color(0xFF51FAC1),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Nhập 1 API Key duy nhất của bất kỳ mô hình nào (Groq, Gemini, Claude, OpenAI). Hệ thống sẽ tự động nhận diện và kết nối chuẩn xác.",
                    color = Color(0xB3FFFFFF),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Provider Switcher Chips
                val providers = listOf(
                    StudyViewModel.PROVIDER_AUTO to "✨ Tự Động",
                    StudyViewModel.PROVIDER_GROQ to "⚡ Groq",
                    StudyViewModel.PROVIDER_GEMINI to "🧠 Gemini",
                    StudyViewModel.PROVIDER_CLAUDE to "🎭 Claude",
                    StudyViewModel.PROVIDER_OPENAI to "🤖 OpenAI"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    providers.forEach { (provId, label) ->
                        val isSelected = currentProvider == provId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Brush.horizontalGradient(listOf(Color(0xFF388E3C), Color(0xFF51FAC1)))
                                    else Brush.horizontalGradient(listOf(Color(0x22FFFFFF), Color(0x22FFFFFF)))
                                )
                                .clickable { viewModel?.setAiProvider(provId) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (isSelected) Color(0xFF0F0C29) else Color(0xCCFFFFFF),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Single Unified API Key Input Field
                OutlinedTextField(
                    value = universalKeyInput,
                    onValueChange = { universalKeyInput = it },
                    label = { Text("AI API Key (Dán key bất kỳ)") },
                    placeholder = { Text("gsk_..., AIza..., sk-ant-..., sk-...") },
                    visualTransformation = if (universalKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    trailingIcon = {
                        IconButton(onClick = { universalKeyVisible = !universalKeyVisible }) {
                            Icon(
                                if (universalKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Key",
                                tint = Color(0x99FFFFFF)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF51FAC1),
                        unfocusedBorderColor = Color(0x66FFFFFF),
                        focusedLabelColor = Color(0xFF51FAC1),
                        unfocusedLabelColor = Color(0x99FFFFFF)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Real-time Detection Badge Card
                val detected = StudyViewModel.detectAiProvider(universalKeyInput)
                val (badgeText, badgeColor) = when {
                    universalKeyInput.trim().startsWith("gsk_") ->
                        "⚡ Đã nhận diện: Groq AI (Llama 3.3 70B - Tốc độ cực nhanh)" to Color(0xFFFF9E00)
                    universalKeyInput.trim().startsWith("AIza") ->
                        "🧠 Đã nhận diện: Google Gemini (2.5 Flash Native Dialog)" to Color(0xFF51FAC1)
                    universalKeyInput.trim().startsWith("sk-ant-") ->
                        "🎭 Đã nhận diện: Anthropic Claude (3.5 Sonnet)" to Color(0xFFE07A5F)
                    universalKeyInput.trim().startsWith("sk-") ->
                        "🤖 Đã nhận diện: OpenAI (GPT-4o / GPT-4o-mini)" to Color(0xFF10A37F)
                    universalKeyInput.isNotBlank() ->
                        "✨ Đã cấu hình khóa AI tùy chỉnh (${StudyViewModel.getProviderDisplayName(detected)})" to Color(0xFF80D8FF)
                    else -> {
                        val hasEnvKey = viewModel?.getEffectiveUniversalApiKey()?.isNotBlank() == true
                        if (hasEnvKey) {
                            "🟢 Khóa AI từ file cấu hình .env đang hoạt động (${viewModel?.activeProviderDisplayTitle?.value ?: "Sẵn sàng"})." to Color(0xFF51FAC1)
                        } else {
                            "ℹ️ Hãy dán API key của bạn vào đây hoặc điền biến AI_API_KEY trong file .env." to Color(0xFFFFD166)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(badgeColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(badgeText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        viewModel?.setUniversalAiApiKey(universalKeyInput)
                        Toast.makeText(context, "Đã lưu và kích hoạt AI thành công!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51FAC1)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Lưu & Kích Hoạt Khóa AI", color = Color(0xFF14141E), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))
                SettingsToggle(
                    title = "Đối thoại âm thanh tự động (Auto-TTS)",
                    icon = Icons.Default.SpatialAudio,
                    initialValue = autoTtsEnabled,
                    onChanged = { viewModel?.setAudioDialogAutoTts(it) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Toggles
                Text(
                    "Hiệu năng & Trải nghiệm",
                    color = Color(0xFFFFD166),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggle(
                    title = "Tiết kiệm pin (Tắt hạt vũ trụ)",
                    icon = Icons.Default.BatteryChargingFull,
                    initialValue = batterySaverEnabled,
                    onChanged = { viewModel?.setBatterySaver(it) }
                )
                SettingsToggle("Nhạc nền (BGM)", Icons.Default.MusicNote, true)
                SettingsToggle(
                    title = "Hiệu ứng âm thanh",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    initialValue = soundEnabled,
                    onChanged = { enabled ->
                        soundEnabled = enabled
                        soundPrefs.edit().putBoolean("sound_enabled", enabled).apply()
                        viewModel?.soundEffectManager?.setSoundEnabled(enabled)
                    }
                )
                SettingsToggle("Rung (Haptics)", Icons.Default.Vibration, true)
                SettingsToggle("Nhắc nhở học tập", Icons.Default.Notifications, false)

                Spacer(modifier = Modifier.height(24.dp))

                // Logout Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFE53935), Color(0xFFB71C1C))))
                        .clickable { onLogout() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đăng xuất", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    icon: ImageVector,
    initialValue: Boolean,
    onChanged: ((Boolean) -> Unit)? = null
) {
    var checked by remember(initialValue) { mutableStateOf(initialValue) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = Color(0x80FFFFFF), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color.White, fontSize = 15.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                onChanged?.invoke(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF51FAC1),
                uncheckedThumbColor = Color(0xFF9E9E9E),
                uncheckedTrackColor = Color(0x4DFFFFFF)
            )
        )
    }
}
