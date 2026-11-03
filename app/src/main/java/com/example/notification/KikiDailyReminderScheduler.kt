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

    // Các mốc giờ nhắc nhở thông minh:
    // 1. Mốc 19:15 - Nhắc nhở nhẹ nhàng (Friendly Nudge)
    const val SLOT_EVENING = "SLOT_EVENING"
    const val REQUEST_CODE_EVENING = 3001
    const val HOUR_EVENING = 19
    const val MINUTE_EVENING = 15

    // 2. Mốc 21:15 - Báo động giữ Streak trước nửa đêm (Urgent Streak Alarm)
    const val SLOT_NIGHT = "SLOT_NIGHT"
    const val REQUEST_CODE_NIGHT = 3002
    const val HOUR_NIGHT = 21
    const val MINUTE_NIGHT = 15

    // 3. Mốc 09:00 sáng hôm sau - Cứu Streak bằng Khiên Vũ Trụ nếu bị đóng băng
    const val SLOT_MORNING_SHIELD = "SLOT_MORNING_SHIELD"
    const val REQUEST_CODE_MORNING = 3003
    const val HOUR_MORNING = 9
    const val MINUTE_MORNING = 0

    const val EXTRA_REMINDER_SLOT = "extra_reminder_slot"

    /**
     * Lên lịch toàn bộ hệ thống nhắc nhở thông minh 2 mốc vàng trong ngày + sáng hôm sau
     */
    fun scheduleAllReminders(context: Context) {
        scheduleSlot(context, SLOT_EVENING, HOUR_EVENING, MINUTE_EVENING, REQUEST_CODE_EVENING)
        scheduleSlot(context, SLOT_NIGHT, HOUR_NIGHT, MINUTE_NIGHT, REQUEST_CODE_NIGHT)
        scheduleSlot(context, SLOT_MORNING_SHIELD, HOUR_MORNING, MINUTE_MORNING, REQUEST_CODE_MORNING)
    }

    /**
     * Tương thích với lệnh cũ scheduleDailyReminder
     */
    fun scheduleDailyReminder(context: Context, hour: Int = HOUR_EVENING, minute: Int = MINUTE_EVENING) {
        scheduleAllReminders(context)
    }

    /**
     * Lên lịch cho một mốc cụ thể
     */
    fun scheduleSlot(context: Context, slot: String, hour: Int, minute: Int, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_SLOT, slot)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Nếu thời điểm hiện tại đã quá giờ này hôm nay, dời sang ngày mai
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val triggerTime = calendar.timeInMillis
        Log.d(TAG, "Lên lịch [$slot] vào lúc: ${calendar.time} (epoch: $triggerTime)")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
            Log.w(TAG, "Không thể đặt lịch AlarmManager cho [$slot]: ${e.message}")
        }
    }

    /**
     * Hủy toàn bộ lịch nhắc nhở khi người dùng tắt thông báo
     */
    fun cancelAllReminders(context: Context) {
        cancelSlot(context, REQUEST_CODE_EVENING)
        cancelSlot(context, REQUEST_CODE_NIGHT)
        cancelSlot(context, REQUEST_CODE_MORNING)
        Log.d(TAG, "Đã hủy toàn bộ lịch nhắc nhở thông minh.")
    }

    fun cancelDailyReminder(context: Context) {
        cancelAllReminders(context)
    }

    private fun cancelSlot(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Bắn thông báo thử nghiệm sau [delaySeconds] giây (Mặc định 5s)
     */
    fun triggerTestNotificationIn(context: Context, delaySeconds: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            putExtra("is_test", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (delaySeconds * 1000L)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d(TAG, "Đã lên lịch thông báo thử nghiệm sau $delaySeconds giây.")
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi lên lịch test notification: ${e.message}")
        }
    }
}
