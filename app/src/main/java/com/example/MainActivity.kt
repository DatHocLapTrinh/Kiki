package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.audio.SoundEffectManager
import com.example.notification.KikiDailyReminderScheduler
import com.example.notification.KikiFirebaseMessagingService
import com.example.notification.KikiNotificationManager
import com.example.sync.FirestoreSyncManager
import com.example.ui.theme.AppTheme
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var firestoreSyncManager: FirestoreSyncManager

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Quyền thông báo đã được cấp.")
            initDailyReminder()
        } else {
            Log.d("MainActivity", "Người dùng từ chối quyền thông báo.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Khởi tạo Notification Channels (Android 8.0+)
        KikiNotificationManager.createNotificationChannels(this)

        // 2. Yêu cầu quyền thông báo trên Android 13+ và lên lịch nhắc nhở 20:00
        checkAndRequestNotificationPermission()

        // 3. Khởi tạo và đồng bộ FCM Registration Token
        initFcmToken()

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudyMentorApp()
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                initDailyReminder()
            }
        } else {
            initDailyReminder()
        }
    }

    private fun initDailyReminder() {
        val prefs = getSharedPreferences(SoundEffectManager.PREFS_NAME, Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        if (notificationsEnabled) {
            KikiDailyReminderScheduler.scheduleDailyReminder(this)
        }
    }

    @Suppress("DEPRECATION")
    private fun initFcmToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("MainActivity", "FCM Token hiện tại: $token")
                    val prefs = getSharedPreferences(SoundEffectManager.PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putString(KikiFirebaseMessagingService.PREF_FCM_TOKEN, token).apply()
                    firestoreSyncManager.updateFcmToken(token)
                } else {
                    Log.w("MainActivity", "Không thể lấy FCM Token: ${task.exception?.message}")
                }
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Lỗi khởi tạo FCM: ${e.message}")
        }
    }
}
