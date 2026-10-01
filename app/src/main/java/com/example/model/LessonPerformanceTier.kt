package com.example.model

enum class LessonPerformanceTier {
    EXCELLENT,   // >= 80% (Chinh phục xuất sắc, 8-10 / 10 câu)
    GOOD,        // 50% - 79% (Hoàn thành bài học, 5-7 / 10 câu)
    NEEDS_WORK   // < 50% (Chưa đạt yêu cầu, 0-4 / 10 câu, đặc biệt khi sai hết)
}

object LessonPerformanceEvaluator {

    fun evaluate(correctCount: Int, totalCount: Int): LessonPerformanceTier {
        if (totalCount <= 0) return LessonPerformanceTier.NEEDS_WORK
        val accuracy = (correctCount.toFloat() / totalCount) * 100f
        return when {
            accuracy >= 80f -> LessonPerformanceTier.EXCELLENT
            accuracy >= 50f -> LessonPerformanceTier.GOOD
            else -> LessonPerformanceTier.NEEDS_WORK
        }
    }

    fun calculateAccuracy(correctCount: Int, totalCount: Int): Int {
        if (totalCount <= 0) return 0
        return ((correctCount.toFloat() / totalCount) * 100f).toInt().coerceIn(0, 100)
    }

    fun isPassed(tier: LessonPerformanceTier): Boolean {
        return tier != LessonPerformanceTier.NEEDS_WORK
    }

    fun calculateXpReward(tier: LessonPerformanceTier, correctCount: Int): Int {
        return when (tier) {
            LessonPerformanceTier.EXCELLENT -> 50 + (correctCount * 5)
            LessonPerformanceTier.GOOD -> 30 + (correctCount * 5)
            LessonPerformanceTier.NEEDS_WORK -> 10 + (correctCount * 5)
        }
    }
}
