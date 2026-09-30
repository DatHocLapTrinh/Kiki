package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object KikiNotificationManager {
    private const val TAG = "KikiNotificationManager"

    const val CHANNEL_STREAK = "kiki_streak_channel"
    const val CHANNEL_NEWS = "kiki_news_channel"

    const val NOTIFICATION_ID_STREAK = 1001
    const val NOTIFICATION_ID_FCM_BASE = 2000

    const val EXTRA_TARGET_SCREEN = "extra_target_screen"

    /**
     * Khởi tạo các Notification Channel bắt buộc từ Android 8.0 (API 26) trở lên
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Kênh Cứu Streak & Nhắc học (Độ ưu tiên cao, rung)
            val streakChannel = NotificationChannel(
                CHANNEL_STREAK,
                "Nhắc nhở học & Cứu Streak 🔥",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo nhắc nhở giữ chuỗi ngày học tập và cứu Streak mỗi tối"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            }

            // 2. Kênh Tin tức, Thử thách & Sự kiện Kiki (Độ ưu tiên bình thường)
            val newsChannel = NotificationChannel(
                CHANNEL_NEWS,
                "Tin tức & Thử thách Kiki 🚀",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo sự kiện XP, nhiệm vụ mới và cập nhật từ Kiki"
            }

            notificationManager.createNotificationChannels(listOf(streakChannel, newsChannel))
            Log.d(TAG, "Notification channels created successfully.")
        }
    }

    /**
     * Kiểm tra quyền POST_NOTIFICATIONS trên Android 13+ (API 33)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Hiển thị thông báo Cứu Streak / Nhắc học thông minh mỗi tối 20:00
     */
    fun showStreakReminderNotification(context: Context, currentStreak: Int) {
        if (!hasNotificationPermission(context)) {
            Log.d(TAG, "Notification permission not granted, skipping streak reminder.")
            return
        }

        val title = if (currentStreak > 0) {
            "🔥 Cứu chuỗi Streak $currentStreak ngày của bạn!"
        } else {
            "🌟 Kiki nhớ bạn rồi nè!"
        }

        val message = if (currentStreak > 0) {
            "Chỉ còn vài tiếng nữa là kết thúc ngày! Hãy vào làm nhanh 1 bài tập 2 phút để không làm vụt tắt chuỗi ngọn lửa nhé."
        } else {
            "Hôm nay bạn chưa học bài nào. Dành ra 3 phút luyện tập để tích lũy XP và leo Top bảng xếp hạng cùng Kiki nha!"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_SCREEN, "journey")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_STREAK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.companion_mascot)
        } catch (_: Exception) {
            null
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_STREAK)
            .setSmallIcon(R.drawable.kiki_icon)
            .apply {
                if (largeIcon != null) setLargeIcon(largeIcon)
            }
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_STREAK, notification)
            Log.d(TAG, "Streak reminder notification shown.")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException while showing notification: ${e.message}")
        }
    }

    /**
     * Hiển thị thông báo đẩy nhận được từ Firebase Cloud Messaging
     */
    fun showFcmNotification(
        context: Context,
        title: String?,
        body: String?,
        targetScreen: String? = null,
        channelId: String = CHANNEL_NEWS
    ) {
        if (!hasNotificationPermission(context)) {
            Log.d(TAG, "Notification permission not granted, skipping FCM notification.")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!targetScreen.isNullOrEmpty()) {
                putExtra(EXTRA_TARGET_SCREEN, targetScreen)
            }
        }

        val notificationId = (NOTIFICATION_ID_FCM_BASE + (System.currentTimeMillis() % 10000)).toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.companion_mascot)
        } catch (_: Exception) {
            null
        }

        val displayTitle = title ?: "Kiki: Thông báo mới"
        val displayBody = body ?: "Bạn có tin nhắn mới từ Kiki, chạm để khám phá ngay!"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.kiki_icon)
            .apply {
                if (largeIcon != null) setLargeIcon(largeIcon)
            }
            .setContentTitle(displayTitle)
            .setContentText(displayBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayBody))
            .setPriority(
                if (channelId == CHANNEL_STREAK) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.d(TAG, "FCM notification displayed successfully (ID=$notificationId).")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException while showing FCM notification: ${e.message}")
        }
    }
}
