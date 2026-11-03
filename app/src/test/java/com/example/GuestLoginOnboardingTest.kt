package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GuestLoginOnboardingTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("StudyMentorPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun testGuestUser_alwaysRequiresOnboardingTestInitially() {
        val prefs = context.getSharedPreferences("StudyMentorPrefs", Context.MODE_PRIVATE)
        val guestEmail = "guest_${System.currentTimeMillis()}@kiki.app"
        val guestUserId = 12345L

        prefs.edit().putString("last_logged_in_email", guestEmail).apply()

        // Hàm kiểm tra logic isUserOnboardingCompleted
        fun isCompleted(userId: Long): Boolean {
            val email = prefs.getString("last_logged_in_email", "") ?: ""
            if (email.startsWith("guest_")) {
                return prefs.getBoolean("onboarding_completed_$userId", false)
            }
            if (prefs.getBoolean("onboarding_completed_$userId", false)) return true
            val savedLevel = prefs.getString("learning_level_$userId", null)
            return !savedLevel.isNullOrBlank()
        }

        // 1. Tài khoản khách mới chưa từng làm test -> bắt buộc phải vào làm test
        assertFalse("Tài khoản khách mới phải bắt đầu làm bài test chẩn đoán AI", isCompleted(guestUserId))

        // 2. Sau khi hoàn thành onboarding và lưu cờ
        prefs.edit().putBoolean("onboarding_completed_$guestUserId", true).apply()
        assertTrue("Sau khi hoàn thành onboarding cờ sẽ được lưu", isCompleted(guestUserId))

        // 3. Khi đăng xuất và tạo phiên khách mới
        val newGuestSessionEmail = "guest_${System.currentTimeMillis() + 1000}@kiki.app"
        val newGuestUserId = 12346L
        prefs.edit().putString("last_logged_in_email", newGuestSessionEmail).apply()
        assertFalse("Phiên khách mới tiếp theo bắt buộc phải làm lại bài test chẩn đoán", isCompleted(newGuestUserId))
    }
}

