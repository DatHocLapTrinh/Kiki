package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.exp
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
    val isMastered: Boolean = false,
    val retentionProbability: Float = 1.0f,
    val objectiveQuality: Int? = null
)

object Sm2Algorithm {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Ngưỡng thời gian xác định xao nhãng ngoại cảnh (Outlier Filtering) */
    const val DISTRACTION_LATENCY_THRESHOLD_MS = 30_000L

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
            isMastered = newMastered,
            retentionProbability = 1.0f,
            objectiveQuality = q
        )
    }

    /**
     * Thuật toán Objective SM-2: Tự động chấm điểm chất lượng phản xạ nhận thức (q: 0..5)
     * Dựa trên: Độ chính xác, Thời gian phản hồi (Response Latency) và Hành vi tra cứu gợi ý.
     *
     * Phân loại:
     * - q = 5 (Easy): Đúng, không gợi ý, phản xạ tự động chớp nhoáng (t < 3000ms).
     * - q = 4 (Good): Đúng, không gợi ý, nhớ chuẩn với tốc độ ổn định (3000ms <= t <= 6000ms).
     * - q = 3 (Hard): Đúng, nhưng ngập ngừng truy xuất nhận thức lâu (t > 6000ms).
     * - q = 2: Trả lời sai lần đầu hoặc cần mở gợi ý / hint mới làm đúng.
     * - q = 1 (Again): Trả lời sai hoàn toàn.
     * - q = 0 (Blackout): Bỏ cuộc / hết thời gian mà không trả lời.
     *
     * Lọc nhiễu Outlier (Yerkes-Dodson Law):
     * Nếu thời gian t > 30s (người học bị xao nhãng ngoại cảnh), đánh dấu isDistracted = true
     * và chặn trần q = 3 để không làm sai lệch hệ số EF.
     */
    fun calculateObjectiveQuality(
        isCorrect: Boolean,
        responseLatencyMs: Long,
        usedHint: Boolean = false,
        isDistracted: Boolean = (responseLatencyMs > DISTRACTION_LATENCY_THRESHOLD_MS),
        isGiveUp: Boolean = false
    ): Int {
        if (isGiveUp) return 0
        if (!isCorrect) {
            return 1
        }
        if (usedHint) {
            return 2
        }
        if (isDistracted) {
            // Giảm thiểu nhiễu xao nhãng cho dữ liệu nghiên cứu
            return 3
        }

        return when {
            responseLatencyMs < 3000L -> 5
            responseLatencyMs <= 6000L -> 4
            else -> 3
        }
    }

    /**
     * Đo lường xác suất lưu giữ trí nhớ (Retention Probability R) theo Đường cong quên Ebbinghaus:
     * R = exp(- t / S)
     * Trong đó:
     * - t: Thời gian trôi qua kể từ lần ôn tập gần nhất (ngày).
     * - S: Độ bền trí nhớ (Memory Stability), tương ứng với intervalDays trong SM-2.
     */
    fun calculateRetentionProbability(elapsedDays: Double, stabilityDays: Double): Double {
        if (elapsedDays <= 0.0) return 1.0
        val s = max(1.0, stabilityDays)
        val r = exp(-elapsedDays / s)
        return r.coerceIn(0.0, 1.0)
    }

    /**
     * Xác định mục cần ôn tập dựa trên ngưỡng xác suất ghi nhớ (Retention Threshold, mặc định 85%)
     * Hoặc khi đã vượt quá khoảng cách ngày dự kiến.
     */
    fun isDueByRetention(
        elapsedDays: Double,
        intervalDays: Int,
        retentionThreshold: Double = 0.85
    ): Boolean {
        if (elapsedDays >= intervalDays) return true
        val r = calculateRetentionProbability(elapsedDays, intervalDays.toDouble())
        return r < retentionThreshold
    }

    fun isDue(nextReviewDateStr: String?, todayStr: String): Boolean {
        if (nextReviewDateStr.isNullOrBlank()) return true
        return nextReviewDateStr <= todayStr
    }

    /**
     * Tính toán số ngày đã trôi qua giữa hai mốc thời gian dạng yyyy-MM-dd
     */
    fun calculateElapsedDays(startDateStr: String?, endDateStr: String): Double {
        if (startDateStr.isNullOrBlank()) return 1.0
        return try {
            val start = dateFormat.parse(startDateStr)?.time ?: return 1.0
            val end = dateFormat.parse(endDateStr)?.time ?: return 1.0
            val diffMs = max(0L, end - start)
            diffMs.toDouble() / (1000.0 * 60 * 60 * 24)
        } catch (_: Exception) {
            1.0
        }
    }
}
