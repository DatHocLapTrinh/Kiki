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

    @Test
    fun testObjectiveQuality_FastCorrectYieldsGrade5() {
        val q = Sm2Algorithm.calculateObjectiveQuality(
            isCorrect = true,
            responseLatencyMs = 2100L,
            usedHint = false
        )
        assertEquals("Fast response under 3s should yield grade 5 (Easy/Reflex)", 5, q)
    }

    @Test
    fun testObjectiveQuality_NormalCorrectYieldsGrade4() {
        val q = Sm2Algorithm.calculateObjectiveQuality(
            isCorrect = true,
            responseLatencyMs = 4500L,
            usedHint = false
        )
        assertEquals("Normal response between 3s and 6s should yield grade 4 (Good)", 4, q)
    }

    @Test
    fun testObjectiveQuality_SlowCorrectYieldsGrade3() {
        val q = Sm2Algorithm.calculateObjectiveQuality(
            isCorrect = true,
            responseLatencyMs = 8000L,
            usedHint = false
        )
        assertEquals("Slow cognitive retrieval over 6s should yield grade 3 (Hard)", 3, q)
    }

    @Test
    fun testObjectiveQuality_HintUsedYieldsGrade2() {
        val q = Sm2Algorithm.calculateObjectiveQuality(
            isCorrect = true,
            responseLatencyMs = 2000L,
            usedHint = true
        )
        assertEquals("Correct answer with hint assistance must yield grade 2", 2, q)
    }

    @Test
    fun testObjectiveQuality_IncorrectYieldsGrade1() {
        val q = Sm2Algorithm.calculateObjectiveQuality(
            isCorrect = false,
            responseLatencyMs = 2500L
        )
        assertEquals("Incorrect response must yield grade 1 (Again)", 1, q)
    }

    @Test
    fun testObjectiveQuality_GiveUpYieldsGrade0() {
        val q = Sm2Algorithm.calculateObjectiveQuality(
            isCorrect = false,
            responseLatencyMs = 5000L,
            isGiveUp = true
        )
        assertEquals("Giving up or blackout must yield grade 0", 0, q)
    }

    @Test
    fun testObjectiveQuality_DistractionOutlierCapsAtGrade3() {
        // Latency > 30s is classified as distracted
        val q = Sm2Algorithm.calculateObjectiveQuality(
            isCorrect = true,
            responseLatencyMs = 35_000L,
            usedHint = false
        )
        assertEquals("Outlier response (>30s) must be capped at grade 3 to prevent skewing EF", 3, q)
    }

    @Test
    fun testEbbinghausRetentionProbability_DecaysExponentially() {
        val stability = 10.0 // 10 days
        val rAtDay0 = Sm2Algorithm.calculateRetentionProbability(0.0, stability)
        assertEquals(1.0, rAtDay0, 0.001)

        val rAtDay10 = Sm2Algorithm.calculateRetentionProbability(10.0, stability)
        // e^(-1) is approximately 0.367879
        assertEquals(0.368, rAtDay10, 0.005)

        val rAtDay1 = Sm2Algorithm.calculateRetentionProbability(1.0, stability)
        // e^(-0.1) is approx 0.9048
        assertTrue("Retention after 1 day with 10d stability should be > 90%", rAtDay1 > 0.90)
    }

    @Test
    fun testIsDueByRetention() {
        // With interval = 5 days and threshold = 0.85
        // At day 0: R = 1.0 -> not due
        assertFalse(Sm2Algorithm.isDueByRetention(elapsedDays = 0.0, intervalDays = 5, retentionThreshold = 0.85))

        // At day 5: elapsed >= interval -> due
        assertTrue(Sm2Algorithm.isDueByRetention(elapsedDays = 5.0, intervalDays = 5, retentionThreshold = 0.85))

        // If elapsed is 1 day with interval = 2 days: R = e^(-0.5) ~ 0.606 < 0.85 -> due!
        assertTrue(Sm2Algorithm.isDueByRetention(elapsedDays = 1.0, intervalDays = 2, retentionThreshold = 0.85))
    }
}
