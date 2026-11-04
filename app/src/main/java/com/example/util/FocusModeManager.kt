package com.example.util

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import com.example.audio.SoundEffectManager

/**
 * Quản lý Chế độ Học Tập Tập Trung (Focus Mode):
 * - Tự động tắt chuông, rung và làm im lặng thông báo từ các ứng dụng khác (Zalo, Facebook, TikTok...)
 * - Giữ màn hình luôn sáng trong suốt buổi học
 * - Tự động khôi phục trạng thái thông báo bình thường khi người dùng thoát hoặc ẩn ứng dụng
 */
object FocusModeManager {
    private const val TAG = "FocusModeManager"

    const val PREF_FOCUS_MODE = "focus_mode_silence_notifications"
    const val PREF_FOCUS_ASKED = "focus_mode_has_prompted"

    // Lưu lại filter trước khi kích hoạt Focus Mode để phục hồi chính xác
    private var previousInterruptionFilter: Int? = null

    /**
     * Kiểm tra người dùng đã bật Chế độ Tập Trung trong cài đặt chưa
     */
    fun isFocusModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SoundEffectManager.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_FOCUS_MODE, false)
    }

    /**
     * Cập nhật bật/tắt Chế độ Tập Trung
     */
    fun setFocusModeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(SoundEffectManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_FOCUS_MODE, enabled).apply()
        if (!enabled) {
            disableFocusMode(context)
        }
    }

    /**
     * Kiểm tra xem đã hiển thị hộp thoại hỏi người dùng khi mới tải/mở app chưa
     */
    fun hasPrompted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SoundEffectManager.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_FOCUS_ASKED, false)
    }

    /**
     * Đánh dấu đã hỏi ý kiến người dùng
     */
    fun markPrompted(context: Context) {
        val prefs = context.getSharedPreferences(SoundEffectManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_FOCUS_ASKED, true).apply()
    }

    /**
     * Kiểm tra quyền can thiệp Không Làm Phiền (Do Not Disturb - Notification Policy Access)
     */
    fun hasDndPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            return nm?.isNotificationPolicyAccessGranted == true
        }
        return true
    }

    /**
     * Mở màn hình Cài đặt hệ thống để người dùng cấp quyền Do Not Disturb cho Kiki trong 1 chạm
     */
    fun openDndSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Không thể mở cài đặt Notification Policy: ${e.message}")
            }
        }
    }

    /**
     * Kích hoạt Focus Mode:
     * - Giữ màn hình luôn sáng
     * - Đưa hệ thống vào chế độ im lặng ưu tiên (Priority Only - chặn mọi thông báo app ngoài)
     */
    fun enableFocusMode(context: Context, activity: Activity? = null) {
        if (!isFocusModeEnabled(context)) return

        try {
            // 1. Giữ màn hình sáng không bị tắt đột ngột
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // 2. Tắt âm & im lặng thông báo các app gây nhiễu
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (nm != null && nm.isNotificationPolicyAccessGranted) {
                    if (previousInterruptionFilter == null) {
                        previousInterruptionFilter = nm.currentInterruptionFilter
                    }
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    Log.d(TAG, "Đã kích hoạt chế độ Do Not Disturb Focus Mode.")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi khi kích hoạt Focus Mode: ${e.message}")
        }
    }

    /**
     * Hủy Focus Mode:
     * - Trả lại màn hình bình thường
     * - Khôi phục âm thanh và thông báo các app khác như cũ
     */
    fun disableFocusMode(context: Context, activity: Activity? = null) {
        try {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (nm != null && nm.isNotificationPolicyAccessGranted) {
                    val filterToRestore = previousInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
                    nm.setInterruptionFilter(filterToRestore)
                    previousInterruptionFilter = null
                    Log.d(TAG, "Đã khôi phục chế độ thông báo bình thường.")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi khi tắt Focus Mode: ${e.message}")
        }
    }
}

