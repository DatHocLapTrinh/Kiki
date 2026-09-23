package com.example

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class StreakCalculationTest {

    private fun calculateNextStreak(lastActiveDate: String?, todayStr: String, currentStreak: Int): Int {
        if (lastActiveDate == null) return 1
        if (lastActiveDate == todayStr) return currentStreak.coerceAtLeast(1)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dLast = sdf.parse(lastActiveDate) ?: return 1
        val dToday = sdf.parse(todayStr) ?: return 1

        val diffInDays = ((dToday.time - dLast.time) / (1000 * 60 * 60 * 24)).toInt()
        return when (diffInDays) {
            1 -> currentStreak + 1
            0 -> currentStreak.coerceAtLeast(1)
            else -> 1
        }
    }

    @Test
    fun testFirstTimeLearningSetsStreakToOne() {
        val streak = calculateNextStreak(null, "2026-09-23", 0)
        assertEquals(1, streak)
    }

    @Test
    fun testConsecutiveDayIncrementsStreak() {
        val streak = calculateNextStreak("2026-09-22", "2026-09-23", 5)
        assertEquals(6, streak)
    }

    @Test
    fun testSameDayLearningKeepsCurrentStreak() {
        val streak = calculateNextStreak("2026-09-23", "2026-09-23", 4)
        assertEquals(4, streak)
    }

    @Test
    fun testMissedDayResetsStreakToOne() {
        val streak = calculateNextStreak("2026-09-20", "2026-09-23", 10)
        assertEquals(1, streak)
    }

    @Test
    fun testAvatarResolutionMapsCorrectly() {
        assertEquals(R.drawable.kiki_icon, resolveAvatarResource(null))
        assertEquals(R.drawable.kiki_icon, resolveAvatarResource("kiki_icon"))
        assertEquals(R.drawable.kiki_mascot_head, resolveAvatarResource("kiki_mascot_head"))
        assertEquals(R.drawable.companion_mascot, resolveAvatarResource("companion_mascot"))
        assertEquals(R.drawable.kiki_hero_intro, resolveAvatarResource("kiki_hero_intro"))
        assertEquals(R.drawable.kiki_hero_auth, resolveAvatarResource("kiki_hero_auth"))
    }
}
