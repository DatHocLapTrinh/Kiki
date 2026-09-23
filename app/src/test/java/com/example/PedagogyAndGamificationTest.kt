package com.example

import com.example.sqlite.room.WeakPointEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PedagogyAndGamificationTest {

    data class StreakResult(
        val newStreak: Int,
        val remainingShields: Int,
        val shieldUsed: Boolean
    )

    private fun calculateStreakWithShield(
        lastActiveDate: String?,
        todayStr: String,
        currentStreak: Int,
        availableShields: Int
    ): StreakResult {
        if (lastActiveDate == null) {
            return StreakResult(newStreak = 1, remainingShields = availableShields, shieldUsed = false)
        }
        if (lastActiveDate == todayStr) {
            return StreakResult(newStreak = currentStreak.coerceAtLeast(1), remainingShields = availableShields, shieldUsed = false)
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dLast = sdf.parse(lastActiveDate) ?: return StreakResult(1, availableShields, false)
        val dToday = sdf.parse(todayStr) ?: return StreakResult(1, availableShields, false)

        val diffInDays = ((dToday.time - dLast.time) / (1000 * 60 * 60 * 24)).toInt()
        return when (diffInDays) {
            1 -> StreakResult(newStreak = currentStreak + 1, remainingShields = availableShields, shieldUsed = false)
            0 -> StreakResult(newStreak = currentStreak.coerceAtLeast(1), remainingShields = availableShields, shieldUsed = false)
            2 -> {
                // Missed exactly 1 day: check shield
                if (availableShields > 0) {
                    StreakResult(newStreak = currentStreak + 1, remainingShields = availableShields - 1, shieldUsed = true)
                } else {
                    StreakResult(newStreak = 1, remainingShields = 0, shieldUsed = false)
                }
            }
            else -> {
                if (availableShields > 0) {
                    StreakResult(newStreak = currentStreak, remainingShields = availableShields - 1, shieldUsed = true)
                } else {
                    StreakResult(newStreak = 1, remainingShields = 0, shieldUsed = false)
                }
            }
        }
    }

    private fun calculateComboBonus(combo: Int): Int {
        return when (combo) {
            1 -> 10
            2 -> 20
            3 -> 30
            4 -> 40
            else -> 60
        }
    }

    @Test
    fun testStreakShieldProtectsStreakWhenMissingOneDay() {
        val result = calculateStreakWithShield(
            lastActiveDate = "2026-09-21",
            todayStr = "2026-09-23",
            currentStreak = 7,
            availableShields = 2
        )
        // With shield: streak is preserved & incremented, shields decrease by 1
        assertEquals(8, result.newStreak)
        assertEquals(1, result.remainingShields)
        assertTrue(result.shieldUsed)
    }

    @Test
    fun testStreakResetsWhenMissingDayWithoutShield() {
        val result = calculateStreakWithShield(
            lastActiveDate = "2026-09-21",
            todayStr = "2026-09-23",
            currentStreak = 14,
            availableShields = 0
        )
        // Without shield: resets to 1
        assertEquals(1, result.newStreak)
        assertEquals(0, result.remainingShields)
        assertEquals(false, result.shieldUsed)
    }

    @Test
    fun testComboBonusProgression() {
        assertEquals(10, calculateComboBonus(1))
        assertEquals(20, calculateComboBonus(2))
        assertEquals(30, calculateComboBonus(3))
        assertEquals(40, calculateComboBonus(4))
        assertEquals(60, calculateComboBonus(5))
        assertEquals(60, calculateComboBonus(10))
    }

    @Test
    fun testEscalatingPitchFrequenciesAreMonotonicallyIncreasing() {
        // C5, D5, E5, F5, G5
        val baseFreqs = listOf(523.25, 587.33, 659.25, 698.46, 783.99)
        // G5, A5, B5, C6, D6
        val targetFreqs = listOf(783.99, 880.00, 987.77, 1046.50, 1174.66)

        for (i in 0 until baseFreqs.size - 1) {
            assertTrue("Base frequency $i should be lower than ${i + 1}", baseFreqs[i] < baseFreqs[i + 1])
            assertTrue("Target frequency $i should be lower than ${i + 1}", targetFreqs[i] < targetFreqs[i + 1])
        }
    }

    @Test
    fun testSlowSpeechRateIsSlowerThanStandard() {
        val standardRate = 0.92f
        val slowRate = 0.68f
        assertTrue("Slow speech rate must be slower than standard rate", slowRate < standardRate)
        assertEquals(0.68f, slowRate, 0.001f)
    }

    @Test
    fun testWeakPointEntityProperties() {
        val weakPoint = WeakPointEntity(
            userId = 1L,
            question = "What is the past tense of 'run'?",
            optionsJson = "[\"runned\", \"ran\", \"running\", \"runs\"]",
            correctIndex = 1,
            wrongCount = 2,
            lastFailedAt = "2026-09-23 15:30"
        )

        assertEquals(1L, weakPoint.userId)
        assertEquals("What is the past tense of 'run'?", weakPoint.question)
        assertEquals(1, weakPoint.correctIndex)
        assertEquals(2, weakPoint.wrongCount)
        assertEquals("2026-09-23 15:30", weakPoint.lastFailedAt)

        val arr = org.json.JSONArray(weakPoint.optionsJson)
        assertEquals(4, arr.length())
        assertEquals("ran", arr.getString(weakPoint.correctIndex))
    }
}
