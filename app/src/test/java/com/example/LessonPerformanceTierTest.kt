package com.example

import com.example.model.LessonPerformanceEvaluator
import com.example.model.LessonPerformanceTier
import org.junit.Assert.*
import org.junit.Test

class LessonPerformanceTierTest {

    @Test
    fun testEvaluationTiers() {
        // Failed / Needs Work: < 50%
        assertEquals(LessonPerformanceTier.NEEDS_WORK, LessonPerformanceEvaluator.evaluate(0, 10))
        assertEquals(LessonPerformanceTier.NEEDS_WORK, LessonPerformanceEvaluator.evaluate(2, 10))
        assertEquals(LessonPerformanceTier.NEEDS_WORK, LessonPerformanceEvaluator.evaluate(4, 10))
        assertEquals(LessonPerformanceTier.NEEDS_WORK, LessonPerformanceEvaluator.evaluate(0, 0))

        // Good / Passed: 50% - 79%
        assertEquals(LessonPerformanceTier.GOOD, LessonPerformanceEvaluator.evaluate(5, 10))
        assertEquals(LessonPerformanceTier.GOOD, LessonPerformanceEvaluator.evaluate(6, 10))
        assertEquals(LessonPerformanceTier.GOOD, LessonPerformanceEvaluator.evaluate(7, 10))

        // Excellent: >= 80%
        assertEquals(LessonPerformanceTier.EXCELLENT, LessonPerformanceEvaluator.evaluate(8, 10))
        assertEquals(LessonPerformanceTier.EXCELLENT, LessonPerformanceEvaluator.evaluate(9, 10))
        assertEquals(LessonPerformanceTier.EXCELLENT, LessonPerformanceEvaluator.evaluate(10, 10))
    }

    @Test
    fun testCalculateAccuracy() {
        assertEquals(0, LessonPerformanceEvaluator.calculateAccuracy(0, 10))
        assertEquals(25, LessonPerformanceEvaluator.calculateAccuracy(2, 8))
        assertEquals(50, LessonPerformanceEvaluator.calculateAccuracy(5, 10))
        assertEquals(80, LessonPerformanceEvaluator.calculateAccuracy(8, 10))
        assertEquals(100, LessonPerformanceEvaluator.calculateAccuracy(10, 10))
        assertEquals(0, LessonPerformanceEvaluator.calculateAccuracy(0, 0))
    }

    @Test
    fun testIsPassedLogic() {
        assertFalse("Needs work (<50%) should not be passed", LessonPerformanceEvaluator.isPassed(LessonPerformanceTier.NEEDS_WORK))
        assertTrue("Good (50-79%) should be passed", LessonPerformanceEvaluator.isPassed(LessonPerformanceTier.GOOD))
        assertTrue("Excellent (>=80%) should be passed", LessonPerformanceEvaluator.isPassed(LessonPerformanceTier.EXCELLENT))
    }

    @Test
    fun testCalculateXpReward() {
        val xpFailed = LessonPerformanceEvaluator.calculateXpReward(LessonPerformanceTier.NEEDS_WORK, correctCount = 2)
        val xpGood = LessonPerformanceEvaluator.calculateXpReward(LessonPerformanceTier.GOOD, correctCount = 6)
        val xpExcellent = LessonPerformanceEvaluator.calculateXpReward(LessonPerformanceTier.EXCELLENT, correctCount = 10)

        assertEquals(20, xpFailed)    // 10 base + 2*5
        assertEquals(60, xpGood)      // 30 base + 6*5
        assertEquals(100, xpExcellent) // 50 base + 10*5

        assertTrue("Excellent should reward more XP than Good", xpExcellent > xpGood)
        assertTrue("Good should reward more XP than Needs Work", xpGood > xpFailed)
    }
}
