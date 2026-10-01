package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Đánh giá chất lượng nhớ thẻ theo thang đo SuperMemo-2 (SM-2)
 */
enum class Sm2Quality(val grade: Int, val labelVi: String, val labelEn: String, val nextDayHint: String) {
    AGAIN(1, "Quên", "Again", "< 1 ngày"),
    HARD(3, "Khó", "Hard", "1 ngày"),
    GOOD(4, "Tốt", "Good", "3 ngày"),
    EASY(5, "Dễ", "Easy", "7+ ngày")
}

data class Sm2State(
    val repetition: Int = 0,
    val intervalDays: Int = 1,
    val easinessFactor: Float = 2.5f,
    val nextReviewDate: String = "",
    val isMastered: Boolean = false
)

object Sm2Algorithm {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Tính toán khoảng thời gian và hệ số dễ nhớ tiếp theo theo thuật toán SM-2
     *
     * Công thức SM-2 chuẩn:
     * EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
     * EF' = max(1.3, EF')
     */
    fun calculateNextReview(
        currentState: Sm2State,
        quality: Int,
        baseDate: Date = Date()
    ): Sm2State {
        val q = quality.coerceIn(0, 5)

        // 1. Tính toán Easiness Factor mới
        val newEf = max(
            1.3f,
            currentState.easinessFactor + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        )

        // 2. Tính toán Repetition & Interval
        val newRepetition: Int
        val newInterval: Int
        val newMastered: Boolean

        if (q < 3) {
            // Trả lời sai/quên: reset chuỗi lặp lại
            newRepetition = 0
            newInterval = 1
            newMastered = false
        } else {
            // Trả lời đúng (Khó, Tốt hoặc Dễ)
            newRepetition = currentState.repetition + 1
            newInterval = when (newRepetition) {
                1 -> 1
                2 -> if (q == 5) 4 else 3
                else -> (currentState.intervalDays * newEf).roundToInt().coerceAtLeast(currentState.intervalDays + 1)
            }
            // Coi là Mastered nếu đã ôn tập thành công >= 4 lần hoặc interval >= 21 ngày
            newMastered = newRepetition >= 4 || newInterval >= 21
        }

        // 3. Tính toán ngày ôn tập tiếp theo
        val calendar = Calendar.getInstance().apply {
            time = baseDate
            add(Calendar.DAY_OF_YEAR, newInterval)
        }
        val nextReviewStr = dateFormat.format(calendar.time)

        return Sm2State(
            repetition = newRepetition,
            intervalDays = newInterval,
            easinessFactor = newEf,
            nextReviewDate = nextReviewStr,
            isMastered = newMastered
        )
    }

    fun isDue(nextReviewDateStr: String?, todayStr: String): Boolean {
        if (nextReviewDateStr.isNullOrBlank()) return true
        return nextReviewDateStr <= todayStr
    }
}
