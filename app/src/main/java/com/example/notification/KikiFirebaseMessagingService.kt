package com.example.notification

import android.content.Context
import android.util.Log
import com.example.audio.SoundEffectManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class KikiFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "KikiFCMService"
        const val PREF_FCM_TOKEN = "fcm_registration_token"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // 1. Kiểm tra xem người dùng có bật nhận thông báo trong app không
        val prefs = applicationContext.getSharedPreferences(SoundEffectManager.PREFS_NAME, Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        if (!notificationsEnabled) {
            Log.d(TAG, "Người dùng đã tắt nhận thông báo trong Cài đặt, bỏ qua.")
            return
        }

        // 2. Trích xuất thông tin tiêu đề, nội dung từ notification hoặc data payload
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Kiki"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "Bạn có thông báo mới từ Kiki!"

        val targetScreen = remoteMessage.data["screen"]
            ?: remoteMessage.data["target_screen"]

        val channel = when (remoteMessage.data["channel"]?.lowercase()) {
            "streak", "reminder" -> KikiNotificationManager.CHANNEL_STREAK
            else -> KikiNotificationManager.CHANNEL_NEWS
        }

        // 3. Hiển thị thông báo lên thanh trạng thái thiết bị
        KikiNotificationManager.showFcmNotification(
            context = applicationContext,
            title = title,
            body = body,
            targetScreen = targetScreen,
            channelId = channel
        )
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Đã nhận FCM Token mới: $token")

        // 1. Lưu token vào SharedPreferences của máy
        val prefs = applicationContext.getSharedPreferences(SoundEffectManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_FCM_TOKEN, token).apply()

        // 2. Nếu người dùng đã đăng nhập Firebase Auth, tự động cập nhật token lên Firestore
        saveTokenToCloud(token)
    }

    private fun saveTokenToCloud(token: String) {
        try {
            val user = FirebaseAuth.getInstance().currentUser ?: return
            val db = FirebaseFirestore.getInstance()
            val tokenData = mapOf(
                "fcmToken" to token,
                "fcmUpdatedAt" to Timestamp.now()
            )
            db.collection("users")
                .document(user.uid)
                .set(tokenData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Cập nhật FCM Token lên Firestore thành công cho UID: ${user.uid}")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Không thể cập nhật FCM Token lên Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi khi lưu token lên cloud: ${e.message}")
        }
    }
}
