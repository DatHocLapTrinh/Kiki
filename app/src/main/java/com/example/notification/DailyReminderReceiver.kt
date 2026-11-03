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
        val slot = intent.getStringExtra(KikiDailyReminderScheduler.EXTRA_REMINDER_SLOT)
            ?: KikiDailyReminderScheduler.SLOT_EVENING
        Log.d(TAG, "DailyReminderReceiver nhận broadcast [action: $action, slot: $slot]")

        // 1. Yêu cầu bắn thông báo thử nghiệm (Test tức thì / Hẹn giờ 5s)
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

        // 2. Nếu thiết bị vừa khởi động lại (Reboot) hoặc App vừa cập nhật -> Lên lịch lại toàn bộ
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = context.getSharedPreferences(SoundEffectManager.PREFS_NAME, Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("notifications_enabled", true)
            if (enabled) {
                Log.d(TAG, "Khởi tạo lại toàn bộ lịch nhắc nhở sau khi thiết bị khởi động lại.")
                KikiDailyReminderScheduler.scheduleAllReminders(context)
            }
            return
        }

        // 3. Tự động lên lịch lại mốc này cho chu kỳ ngày kế tiếp
        when (slot) {
            KikiDailyReminderScheduler.SLOT_EVENING -> {
                KikiDailyReminderScheduler.scheduleSlot(
                    context,
                    KikiDailyReminderScheduler.SLOT_EVENING,
                    KikiDailyReminderScheduler.HOUR_EVENING,
                    KikiDailyReminderScheduler.MINUTE_EVENING,
                    KikiDailyReminderScheduler.REQUEST_CODE_EVENING
                )
            }
            KikiDailyReminderScheduler.SLOT_NIGHT -> {
                KikiDailyReminderScheduler.scheduleSlot(
                    context,
                    KikiDailyReminderScheduler.SLOT_NIGHT,
                    KikiDailyReminderScheduler.HOUR_NIGHT,
                    KikiDailyReminderScheduler.MINUTE_NIGHT,
                    KikiDailyReminderScheduler.REQUEST_CODE_NIGHT
                )
            }
            KikiDailyReminderScheduler.SLOT_MORNING_SHIELD -> {
                KikiDailyReminderScheduler.scheduleSlot(
                    context,
                    KikiDailyReminderScheduler.SLOT_MORNING_SHIELD,
                    KikiDailyReminderScheduler.HOUR_MORNING,
                    KikiDailyReminderScheduler.MINUTE_MORNING,
                    KikiDailyReminderScheduler.REQUEST_CODE_MORNING
                )
            }
        }

        // 4. Kiểm tra cài đặt người dùng có cho phép nhận thông báo hay không
        val prefs = context.getSharedPreferences(SoundEffectManager.PREFS_NAME, Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        val isEnglish = prefs.getBoolean("english", false)
        if (!notificationsEnabled) {
            Log.d(TAG, "Người dùng đã tắt nhận thông báo, bỏ qua chuông nhắc nhở [$slot].")
            return
        }

        // 5. Kiểm tra thông minh với cơ sở dữ liệu:
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

                when (slot) {
                    KikiDailyReminderScheduler.SLOT_MORNING_SHIELD -> {
                        // Sáng hôm sau: nếu có Khiên (> 0) mà Streak bị lỡ hôm qua -> báo cứu Streak bằng khiên
                        val shields = profile?.streakShields ?: 0
                        if (shields > 0 && profile != null && profile.lastActiveDate != todayStr) {
                            Log.d(TAG, "Kích hoạt thông báo dùng Khiên cứu Streak sáng sớm.")
                            KikiNotificationManager.showShieldRecoveryReminder(context, shields, isEnglish)
                        }
                    }
                    KikiDailyReminderScheduler.SLOT_EVENING -> {
                        // 19:15 Nhắc nhở nhẹ nhàng: chỉ gửi nếu HÔM NAY CHƯA HỌC
                        if (profile != null && profile.lastActiveDate == todayStr) {
                            Log.d(TAG, "Hôm nay ($todayStr) bạn đã học rồi! Bỏ qua mốc 19:15 để không gây phiền.")
                        } else {
                            Log.d(TAG, "Người dùng chưa học hôm nay, gửi nhắc nhở nhẹ nhàng 19:15.")
                            KikiNotificationManager.showGentleDailyReminder(context, isEnglish)
                        }
                    }
                    KikiDailyReminderScheduler.SLOT_NIGHT -> {
                        // 21:15 Báo động giữ Streak: chỉ gửi nếu HÔM NAY CHƯA HỌC
                        if (profile != null && profile.lastActiveDate == todayStr) {
                            Log.d(TAG, "Hôm nay ($todayStr) bạn đã học rồi! Bỏ qua mốc 21:15 để không gây phiền.")
                        } else {
                            val streak = profile?.currentStreak ?: 0
                            Log.d(TAG, "Người dùng chưa học hôm nay, kích hoạt báo động ngọn lửa Streak $streak ngày.")
                            KikiNotificationManager.showUrgentStreakReminder(context, streak, isEnglish)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi kiểm tra trạng thái học: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
