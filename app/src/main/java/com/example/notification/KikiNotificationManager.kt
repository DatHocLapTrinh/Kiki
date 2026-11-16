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
    const val CHANNEL_ACHIEVEMENT = "kiki_achievement_channel"

    const val NOTIFICATION_ID_ACHIEVEMENT_BASE = 3000
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

            // 3. Kênh Vinh danh & Mở khóa Danh hiệu (Độ ưu tiên cao, rung vinh danh)
            val achievementChannel = NotificationChannel(
                CHANNEL_ACHIEVEMENT,
                "Vinh danh & Mở khóa Danh hiệu 🎖️",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo chúc mừng khi đạt danh hiệu và mốc thành tựu mới"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 150, 100, 250)
            }

            notificationManager.createNotificationChannels(listOf(streakChannel, newsChannel, achievementChannel))
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
     * Hiển thị thông báo Nhắc nhở nhẹ nhàng (Mốc 19:15 - Friendly Nudge)
     */
    fun showGentleDailyReminder(context: Context, isEnglish: Boolean = false) {
        if (!hasNotificationPermission(context)) return

        val messages = if (isEnglish) {
            listOf(
                "🍃 Just 2 minutes with Kiki!" to "Unlock new vocabulary and power up your skills today. Small steps, huge progress!",
                "🌟 Kiki is waiting in the Cosmos!" to "Take a quick learning break. Fresh challenges are ready for you!",
                "🎯 1% better every day" to "You haven't practiced yet today! Hop in for a quick 2-minute session."
            )
        } else {
            listOf(
                "🍃 Chỉ 2 phút giải lao cùng Kiki!" to "Cùng Kiki tích lũy thêm từ vựng mới cho ngày hôm nay nào. Kiến thức nhỏ, tiến bộ to!",
                "🌟 Kiki đang đợi bạn trong vũ trụ!" to "Dành chút thời gian giải lao nạp năng lượng nhé. Thử thách thú vị đang chờ bạn!",
                "🎯 Tiến bộ 1% mỗi ngày cùng Kiki" to "Hôm nay bạn chưa luyện tập đó nha! Vào làm nhanh 1 bài để giữ phong độ nào."
            )
        }

        val (title, body) = messages.random()
        showNotification(context, title, body)
    }

    /**
     * Hiển thị thông báo Cảnh báo giữ Streak khẩn trương (Mốc 21:15 - Urgent Streak Alarm)
     */
    fun showUrgentStreakReminder(context: Context, currentStreak: Int, isEnglish: Boolean = false) {
        if (!hasNotificationPermission(context)) return

        val (title, body) = when {
            currentStreak <= 3 -> {
                if (isEnglish) {
                    val pool = listOf(
                        "🔥 Keep the flame alive!" to "Complete a quick 2-minute lesson before midnight to lock in your daily streak!",
                        "🚀 The day is almost over!" to "2 minutes before bed to hit your daily goal and earn bonus cosmic stars!"
                    )
                    pool.random()
                } else {
                    val pool = listOf(
                        "🔥 Đừng để ngọn lửa Streak vụt tắt!" to "Vào làm nhanh 1 bài ngắn 2 phút để giữ chuỗi ngày đầu tiên bạn ơi. Bắt đầu thói quen ngay!",
                        "🚀 Hôm nay sắp khép lại rồi!" to "2 phút cùng Kiki trước giờ ngủ để hoàn thành mục tiêu ngày và nhận ⭐ Sao thưởng nhé!"
                    )
                    pool.random()
                }
            }
            currentStreak in 4..14 -> {
                if (isEnglish) {
                    val pool = listOf(
                        "🔥 Alert! Your $currentStreak-day streak is on the line!" to "Midnight is approaching! Don't let your $currentStreak days of dedication slip away.",
                        "🛡️ Guard your cosmic streak!" to "Kiki alert: Your streak is counting down! Hop in for 2 minutes to keep the fire burning."
                    )
                    pool.random()
                } else {
                    val pool = listOf(
                        "🔥 Nguy hiểm! Chuỗi $currentStreak ngày rực lửa sắp tắt!" to "Chỉ còn vài tiếng nữa là qua ngày mới. Công sức $currentStreak ngày không thể uổng phí, vào học ngay!",
                        "🛡️ Đừng để lửa tắt lúc nửa đêm!" to "Kiki cảnh báo: Chuỗi $currentStreak ngày đang đếm ngược! Chỉ 1 bài học ngắn để bảo vệ chuỗi."
                    )
                    pool.random()
                }
            }
            else -> {
                if (isEnglish) {
                    val pool = listOf(
                        "👑 $currentStreak-Day Legend at risk!" to "You've built an incredible record! Don't let tonight break your legendary cosmic streak.",
                        "⚡ Keep the $currentStreak-day streak blazing!" to "Extend your record run! Rescue the streak before the clock strikes midnight."
                    )
                    pool.random()
                } else {
                    val pool = listOf(
                        "👑 Huyền thoại $currentStreak ngày đang gặp nguy hiểm!" to "Bạn là một trong những học viên kiên trì nhất! Đừng để đêm nay cắt đứt chuỗi phong độ đỉnh cao.",
                        "⚡ Kỷ lục $currentStreak ngày rực rỡ!" to "Nối dài chuỗi ngày vàng nào! Vào giải cứu ngọn lửa Streak trước khi đồng hồ điểm 00:00."
                    )
                    pool.random()
                }
            }
        }

        showNotification(context, title, body)
    }

    /**
     * Hiển thị thông báo Khôi phục Streak bằng Khiên Vũ Trụ (Mốc 09:00 sáng hôm sau)
     */
    fun showShieldRecoveryReminder(context: Context, streakShields: Int, isEnglish: Boolean = false) {
        if (!hasNotificationPermission(context)) return

        val title = if (isEnglish) "🛡️ Streak Frozen! Restore with Shield" else "🛡️ Chuỗi Streak đã đóng băng! Khôi phục ngay"
        val body = if (isEnglish) {
            "You have $streakShields Cosmic Shield(s) available. Open Kiki to thaw and rescue your streak right now!"
        } else {
            "Bạn đang có $streakShields Khiên Vũ Trụ. Hãy mở Kiki để dùng Khiên hồi sinh ngọn lửa Streak ngay hôm nay!"
        }

        showNotification(context, title, body)
    }

    /**
     * Phương thức hiển thị Notification chung với Icon, âm thanh và chuyển hướng
     */
    private fun showNotification(context: Context, title: String, message: String) {
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
            BitmapFactory.decodeResource(context.resources, R.drawable.kiki_mascot_head)
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
            Log.d(TAG, "Notification shown: $title")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException while showing notification: ${e.message}")
        }
    }

    /**
     * Hiển thị thông báo Cứu Streak / Nhắc học thông minh mỗi tối (Hỗ trợ tương thích ngược)
     */
    fun showStreakReminderNotification(context: Context, currentStreak: Int) {
        showUrgentStreakReminder(context, currentStreak, isEnglish = false)
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
            BitmapFactory.decodeResource(context.resources, R.drawable.kiki_mascot_head)
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

    /**
     * Hiển thị thông báo khi người dùng đạt được yêu cầu của danh hiệu (mở khóa danh hiệu mới)
     */
    fun showAchievementUnlockedNotification(
        context: Context,
        badgeTitle: String,
        badgeDescription: String,
        badgeDrawableRes: Int,
        isEnglish: Boolean = false
    ) {
        if (!hasNotificationPermission(context)) {
            Log.d(TAG, "Permission not granted, skipping achievement notification: $badgeTitle")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_SCREEN, "profile")
        }

        val notificationId = (NOTIFICATION_ID_ACHIEVEMENT_BASE + (System.currentTimeMillis() % 1000)).toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, badgeDrawableRes)
        } catch (_: Exception) {
            null
        }

        val title = if (isEnglish) "🎉 New Achievement Unlocked: $badgeTitle!" else "🎉 Mở Khóa Danh Hiệu Mới: $badgeTitle!"
        val body = if (isEnglish) "Congratulations! $badgeDescription. Tap to view your badge collection!" else "Chúc mừng bạn! $badgeDescription. Chạm để xem bộ sưu tập huy hiệu!"

        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENT)
            .setSmallIcon(R.drawable.kiki_icon)
            .apply {
                if (largeIcon != null) setLargeIcon(largeIcon)
            }
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.d(TAG, "Achievement notification displayed successfully (ID=$notificationId): $badgeTitle")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException while showing achievement notification: ${e.message}")
        }
    }
}
