package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Quản lý kích hoạt In-App Feedback Prompt & khảo sát Google Form:
 * - Theo dõi số lượng bài học Microlearning người dùng đã hoàn thành.
 * - Tự động kích hoạt khi người dùng đã trải nghiệm app đủ sâu (tối thiểu 3 bài học).
 * - Cơ chế Cooldown 48 giờ nếu người dùng chọn "Để sau" để tránh spam gây phiền hà.
 * - Đánh dấu trạng thái vĩnh viễn khi người dùng đã bấm mở form khảo sát.
 * - Mở an toàn link Google Form qua trình duyệt Android.
 */
object FeedbackPromptManager {
    private const val TAG = "FeedbackPromptManager"
    private const val PREFS_NAME = "kiki_feedback_prefs"

    private const val KEY_COMPLETED_LESSONS = "feedback_completed_lessons_count"
    private const val KEY_FEEDBACK_SUBMITTED = "feedback_already_submitted"
    private const val KEY_LAST_DISMISSED_TIME = "feedback_last_dismissed_time"
    private const val KEY_CUSTOM_SURVEY_URL = "feedback_custom_survey_url"

    // Ngưỡng tối thiểu bài học hoàn thành trước khi hiển thị popup khảo sát lần đầu
    const val MIN_LESSONS_BEFORE_PROMPT = 3

    // Thời gian chờ nhắc lại nếu người dùng bấm "Để sau" (48 giờ)
    const val COOLDOWN_HOURS = 48
    private const val COOLDOWN_MILLIS = COOLDOWN_HOURS * 60 * 60 * 1000L

    // Link Google Form chính thức thu thập phản hồi và khảo sát trải nghiệm Kiki
    const val DEFAULT_SURVEY_URL = "https://docs.google.com/forms/d/e/1FAIpQLSfhMLfox0eDKY62pfJ8SvRxvFQ3ymiPVneEK1E-_Lo8ANyDhA/viewform?usp=dialog"

    /**
     * Tăng số lượng bài học hoàn thành lên 1 và trả về giá trị mới
     */
    fun incrementCompletedLessons(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_COMPLETED_LESSONS, 0) + 1
        prefs.edit().putInt(KEY_COMPLETED_LESSONS, current).apply()
        return current
    }

    /**
     * Lấy số bài học đã hoàn thành được ghi nhận
     */
    fun getCompletedLessons(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_COMPLETED_LESSONS, 0)
    }

    /**
     * Kiểm tra xem người dùng đã bấm tham gia khảo sát chưa
     */
    fun isFeedbackSubmitted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FEEDBACK_SUBMITTED, false)
    }

    /**
     * Đánh dấu người dùng đã mở form khảo sát thành công (sẽ không tự động hiện lại nữa)
     */
    fun markFeedbackSubmitted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FEEDBACK_SUBMITTED, true).apply()
    }

    /**
     * Ghi nhận thời điểm người dùng bấm "Để sau" để tính cooldown 48 giờ
     */
    fun recordDismissed(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_DISMISSED_TIME, System.currentTimeMillis()).apply()
    }

    /**
     * Lấy đường dẫn link Google Form khảo sát (mặc định hoặc URL tùy biến)
     */
    fun getSurveyUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_SURVEY_URL, null)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_SURVEY_URL
    }

    /**
     * Cho phép cấu hình URL Google Form động (từ Backend API, Firebase Remote Config hoặc cài đặt)
     */
    fun setCustomSurveyUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_SURVEY_URL, url).apply()
    }

    /**
     * Điều kiện thông minh quyết định có nên tự động hiển thị popup In-App Feedback không:
     * 1. Chưa từng bấm mở khảo sát.
     * 2. Đã hoàn thành tối thiểu [MIN_LESSONS_BEFORE_PROMPT] bài học.
     * 3. Đã qua thời gian Cooldown (48 giờ) kể từ lần bấm "Để sau" gần nhất.
     */
    fun shouldShowFeedbackPrompt(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isSubmitted = prefs.getBoolean(KEY_FEEDBACK_SUBMITTED, false)
        if (isSubmitted) return false

        val completedCount = prefs.getInt(KEY_COMPLETED_LESSONS, 0)
        if (completedCount < MIN_LESSONS_BEFORE_PROMPT) return false

        val lastDismissed = prefs.getLong(KEY_LAST_DISMISSED_TIME, 0L)
        val now = System.currentTimeMillis()
        if (now - lastDismissed < COOLDOWN_MILLIS) return false

        return true
    }

    /**
     * Khởi chạy Intent mở URL Google Form an toàn qua trình duyệt ngoài
     */
    fun openSurveyInBrowser(context: Context, surveyUrl: String = getSurveyUrl(context)): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(surveyUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            markFeedbackSubmitted(context)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Không thể mở trình duyệt đến Google Form: ${e.message}", e)
            false
        }
    }
}

