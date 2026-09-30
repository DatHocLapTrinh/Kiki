package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.Room
import com.example.audio.SoundEffectManager
import com.example.sqlite.room.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DailyReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "DailyReminderReceiver nhận broadcast với action: $action")

        // 1. Nếu đây là yêu cầu bắn thông báo thử nghiệm (Test tức thì / Hẹn giờ 5s)
        if (intent.getBooleanExtra("is_test", false)) {
            Log.d(TAG, "Kích hoạt thông báo thử nghiệm từ người dùng.")
            KikiNotificationManager.showFcmNotification(
                context = context,
                title = "🔥 Kiki: Bắn thử thông báo thành công!",
                body = "Hệ thống thông báo đẩy và đồng hồ báo thức ngầm đang hoạt động hoàn hảo trên máy bạn!",
                channelId = KikiNotificationManager.CHANNEL_STREAK
            )
            return
        }

        // 2. Nếu thiết bị vừa khởi động lại (Reboot) hoặc App vừa được cập nhật -> Lên lịch lại
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = context.getSharedPreferences(SoundEffectManager.PREFS_NAME, Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("notifications_enabled", true)
            if (enabled) {
                Log.d(TAG, "Khởi tạo lại lịch nhắc nhở sau khi thiết bị khởi động lại.")
                KikiDailyReminderScheduler.scheduleDailyReminder(context)
            }
            return
        }

        // 2. Tự động lên lịch lại cho ngày kế tiếp (Chu kỳ mỗi ngày 20:00)
        KikiDailyReminderScheduler.scheduleDailyReminder(context)

        // 3. Kiểm tra cài đặt người dùng có cho phép nhận thông báo hay không
        val prefs = context.getSharedPreferences(SoundEffectManager.PREFS_NAME, Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        if (!notificationsEnabled) {
            Log.d(TAG, "Người dùng đã tắt nhận thông báo, bỏ qua chuông nhắc nhở.")
            return
        }

        // 4. Kiểm tra thông minh: Nếu hôm nay người dùng ĐÃ HỌC RỒI -> Bỏ qua, không làm phiền!
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "AIStudyMentorRoom.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()

                val profile = db.appDao().getAnyUserProfile()
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                if (profile != null && profile.lastActiveDate == todayStr) {
                    Log.d(TAG, "Hôm nay ($todayStr) bạn đã học rồi! Không gửi thông báo để tránh spam.")
                } else {
                    val streak = profile?.currentStreak ?: 0
                    Log.d(TAG, "Người dùng chưa học hôm nay, kích hoạt thông báo cứu Streak ($streak ngày).")
                    KikiNotificationManager.showStreakReminderNotification(context, streak)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi kiểm tra trạng thái học: ${e.message}", e)
                // Dự phòng: Vẫn nhắc nhở để người dùng không quên học
                KikiNotificationManager.showStreakReminderNotification(context, 0)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
