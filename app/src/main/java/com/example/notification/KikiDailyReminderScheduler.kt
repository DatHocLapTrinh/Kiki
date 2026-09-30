package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object KikiDailyReminderScheduler {
    private const val TAG = "KikiDailyScheduler"
    private const val REQUEST_CODE_DAILY_REMINDER = 3001

    /**
     * Lên lịch hẹn giờ báo thức hàng ngày vào lúc [hour]:[minute] (mặc định 20:00)
     */
    fun scheduleDailyReminder(context: Context, hour: Int = 20, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Nếu thời điểm hiện tại đã quá 20:00 hôm nay, dời sang 20:00 ngày mai
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val triggerTime = calendar.timeInMillis
        Log.d(TAG, "Lên lịch nhắc nhở học thông minh vào: ${calendar.time} (epoch: $triggerTime)")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Kiểm tra xem có quyền exact alarm không (Android 12+)
                val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }

                if (canExact) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Không thể đặt lịch AlarmManager: ${e.message}")
        }
    }

    /**
     * Hủy lịch nhắc nhở hàng ngày (khi người dùng tắt nhận thông báo trong Cài đặt)
     */
    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_REMINDER,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Đã hủy lịch nhắc nhở hàng ngày.")
        }
    }

    /**
     * Bắn thông báo thử nghiệm sau [delaySeconds] giây (Mặc định 5s)
     * Dùng để test việc nhận thông báo khi người dùng ẩn app hoặc khóa màn hình
     */
    fun scheduleTestReminder(context: Context, delaySeconds: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            putExtra("is_test", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            3002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + (delaySeconds * 1000L)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Đã lên lịch bắn thông báo thử nghiệm sau $delaySeconds giây")
        } catch (e: Exception) {
            Log.w(TAG, "Không thể đặt lịch test: ${e.message}")
        }
    }
}
