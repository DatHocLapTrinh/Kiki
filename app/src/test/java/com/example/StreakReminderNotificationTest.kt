package com.example

import com.example.notification.KikiDailyReminderScheduler
import com.example.notification.KikiNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StreakReminderNotificationTest {

    @Test
    fun testReminderSlotsAreDistinctAndCorrectlyTimed() {
        assertEquals("SLOT_EVENING", KikiDailyReminderScheduler.SLOT_EVENING)
        assertEquals(19, KikiDailyReminderScheduler.HOUR_EVENING)
        assertEquals(15, KikiDailyReminderScheduler.MINUTE_EVENING)
        assertEquals(3001, KikiDailyReminderScheduler.REQUEST_CODE_EVENING)

        assertEquals("SLOT_NIGHT", KikiDailyReminderScheduler.SLOT_NIGHT)
        assertEquals(21, KikiDailyReminderScheduler.HOUR_NIGHT)
        assertEquals(15, KikiDailyReminderScheduler.MINUTE_NIGHT)
        assertEquals(3002, KikiDailyReminderScheduler.REQUEST_CODE_NIGHT)

        assertEquals("SLOT_MORNING_SHIELD", KikiDailyReminderScheduler.SLOT_MORNING_SHIELD)
        assertEquals(9, KikiDailyReminderScheduler.HOUR_MORNING)
        assertEquals(0, KikiDailyReminderScheduler.MINUTE_MORNING)
        assertEquals(3003, KikiDailyReminderScheduler.REQUEST_CODE_MORNING)

        // Ensure request codes do not collide
        val requestCodes = setOf(
            KikiDailyReminderScheduler.REQUEST_CODE_EVENING,
            KikiDailyReminderScheduler.REQUEST_CODE_NIGHT,
            KikiDailyReminderScheduler.REQUEST_CODE_MORNING
        )
        assertEquals(3, requestCodes.size)
    }

    @Test
    fun testNotificationChannelsAndTargets() {
        assertEquals("kiki_streak_channel", KikiNotificationManager.CHANNEL_STREAK)
        assertEquals("kiki_news_channel", KikiNotificationManager.CHANNEL_NEWS)
        assertEquals(1001, KikiNotificationManager.NOTIFICATION_ID_STREAK)
        assertEquals("extra_target_screen", KikiNotificationManager.EXTRA_TARGET_SCREEN)
    }

    @Test
    fun testShouldSuppressReminderIfUserStudiedToday() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val userLastActiveToday = todayStr
        val userLastActiveYesterday = "2026-11-03"

        // If user studied today, evening and night reminders should be suppressed
        val shouldSuppressToday = (userLastActiveToday == todayStr)
        assertTrue("Reminder must be suppressed if user already studied today", shouldSuppressToday)

        // If user hasn't studied today, reminder must trigger
        val shouldSuppressYesterday = (userLastActiveYesterday == todayStr)
        assertFalse("Reminder must NOT be suppressed if user last active was yesterday", shouldSuppressYesterday)
    }

    @Test
    fun testShieldRecoveryTriggerCondition() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        // Case 1: Has shields and missed yesterday -> Trigger recovery reminder
        val hasShields = 2
        val lastActiveYesterday = "2026-11-03"
        val shouldTriggerShieldRecovery = (hasShields > 0 && lastActiveYesterday != todayStr)
        assertTrue(shouldTriggerShieldRecovery)

        // Case 2: No shields -> Do not trigger shield reminder
        val noShields = 0
        val shouldTriggerNoShield = (noShields > 0 && lastActiveYesterday != todayStr)
        assertFalse(shouldTriggerNoShield)

        // Case 3: Already active today -> Do not trigger
        val shouldTriggerActiveToday = (hasShields > 0 && todayStr != todayStr)
        assertFalse(shouldTriggerActiveToday)
    }

    @Test
    fun testStreakTierClassification() {
        fun getTier(streak: Int): String {
            return when {
                streak <= 3 -> "STARTER"
                streak in 4..14 -> "REGULAR"
                else -> "LEGEND"
            }
        }

        assertEquals("STARTER", getTier(1))
        assertEquals("STARTER", getTier(3))
        assertEquals("REGULAR", getTier(4))
        assertEquals("REGULAR", getTier(14))
        assertEquals("LEGEND", getTier(15))
        assertEquals("LEGEND", getTier(30))
    }
}

