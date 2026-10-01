package com.example

import com.example.util.Sm2Algorithm
import com.example.util.Sm2State
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Sm2AlgorithmTest {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Test
    fun testFirstSuccessfulReview_GradeGood() {
        val initial = Sm2State(
            repetition = 0,
            intervalDays = 1,
            easinessFactor = 2.5f,
            nextReviewDate = "",
            isMastered = false
        )
        val baseDate = Date()
        val next = Sm2Algorithm.calculateNextReview(initial, quality = 4, baseDate = baseDate)

        assertEquals("Repetition should increment to 1", 1, next.repetition)
        assertEquals("First interval must be 1 day", 1, next.intervalDays)
        assertFalse("Card is not mastered on first review", next.isMastered)
        assertTrue("EF should remain close to 2.5 on grade 4", next.easinessFactor in 2.45f..2.55f)
    }

    @Test
    fun testSecondReview_GradeEasy() {
        val firstStep = Sm2State(
            repetition = 1,
            intervalDays = 1,
            easinessFactor = 2.5f
        )
        val next = Sm2Algorithm.calculateNextReview(firstStep, quality = 5)

        assertEquals("Repetition should increment to 2", 2, next.repetition)
        assertEquals("Second interval on grade 5 should be 4 days", 4, next.intervalDays)
        assertTrue("EF should increase on grade 5", next.easinessFactor > 2.5f)
    }

    @Test
    fun testSecondReview_GradeGood() {
        val firstStep = Sm2State(
            repetition = 1,
            intervalDays = 1,
            easinessFactor = 2.5f
        )
        val next = Sm2Algorithm.calculateNextReview(firstStep, quality = 4)

        assertEquals("Repetition should increment to 2", 2, next.repetition)
        assertEquals("Second interval on grade 4 should be 3 days", 3, next.intervalDays)
    }

    @Test
    fun testFailureReview_ResetsRepetitionAndInterval() {
        val learnedState = Sm2State(
            repetition = 3,
            intervalDays = 12,
            easinessFactor = 2.6f,
            isMastered = false
        )
        // User forgets the word (Again - quality 1)
        val next = Sm2Algorithm.calculateNextReview(learnedState, quality = 1)

        assertEquals("Repetition should reset to 0 on failure", 0, next.repetition)
        assertEquals("Interval should reset to 1 day on failure", 1, next.intervalDays)
        assertFalse("Card should not be mastered when forgotten", next.isMastered)
        assertTrue("Easiness factor should decrease on failure", next.easinessFactor < 2.6f)
    }

    @Test
    fun testEasinessFactorFloorLimit() {
        var state = Sm2State(
            repetition = 0,
            intervalDays = 1,
            easinessFactor = 1.35f
        )
        // Repeated failures should not reduce EF below 1.3
        for (i in 1..5) {
            state = Sm2Algorithm.calculateNextReview(state, quality = 0)
        }
        assertEquals("EF must never fall below 1.3", 1.3f, state.easinessFactor, 0.001f)
    }

    @Test
    fun testMasteryCondition_RepetitionThreshold() {
        val advancedState = Sm2State(
            repetition = 3,
            intervalDays = 10,
            easinessFactor = 2.5f
        )
        val next = Sm2Algorithm.calculateNextReview(advancedState, quality = 4)

        assertEquals(4, next.repetition)
        assertTrue("Card should be marked mastered once repetition reaches 4", next.isMastered)
    }

    @Test
    fun testMasteryCondition_IntervalThreshold() {
        val highIntervalState = Sm2State(
            repetition = 2,
            intervalDays = 15,
            easinessFactor = 2.6f
        )
        val next = Sm2Algorithm.calculateNextReview(highIntervalState, quality = 5)

        assertTrue("Interval multiplied past 21 should trigger mastery", next.intervalDays >= 21)
        assertTrue("Card should be marked mastered when interval >= 21", next.isMastered)
    }

    @Test
    fun testIsDueHelper() {
        val todayStr = "2026-10-02"
        assertTrue("Blank next review date is considered due", Sm2Algorithm.isDue("", todayStr))
        assertTrue("Null next review date is considered due", Sm2Algorithm.isDue(null, todayStr))
        assertTrue("Past date is due", Sm2Algorithm.isDue("2026-10-01", todayStr))
        assertTrue("Today date is due", Sm2Algorithm.isDue("2026-10-02", todayStr))
        assertFalse("Future date is not due", Sm2Algorithm.isDue("2026-10-03", todayStr))
    }
}
