package com.example.util

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Quản lý chu kỳ năng lượng vũ trụ và ôn tập ngắt quãng (Spaced Repetition Decay)
 * cho 40 hành tinh theo đường cong quên lãng Ebbinghaus kết hợp cơ chế Duolingo-style:
 *
 * - Hành tinh sau khi hoàn thành sẽ có năng lượng tối đa 100% (1.0f).
 * - Sau một thời gian không ôn tập:
 *   - < 24 giờ: 1.0f (Năng lượng dồi dào, phát sáng rực rỡ).
 *   - 24h - 36h: 0.85f (Năng lượng ổn định).
 *   - 36h - 48h: 0.65f (Bắt đầu suy giảm -> CẦN TÁI NẠP ⚡).
 *   - 48h - 72h: 0.45f (Cạn kiệt năng lượng -> CẦN TÁI NẠP ⚡).
 *   - > 72h: 0.25f (Mức tối thiểu -> CẦN TÁI NẠP KHẨN CẤP ⚡).
 * - Khi người dùng ấn "Tái nạp năng lượng":
 *   - Nhận thêm +30 XP và +5 Sao.
 *   - Hồi phục năng lượng lên 100%.
 *   - Cập nhật tiến độ nhiệm vụ ngày PLANET_RECHARGE.
 */
class CosmicSpacedRepetitionManager(
    context: Context? = null,
    injectedPrefs: SharedPreferences? = null
) {
    private val prefs: SharedPreferences? =
        injectedPrefs ?: context?.getSharedPreferences("kiki_cosmic_spaced_repetition", Context.MODE_PRIVATE)

    // Fallback in-memory map cho unit testing thuần hoặc khi SharedPreferences chưa sẵn sàng
    private val memoryStore = ConcurrentHashMap<String, Long>()

    companion object {
        const val FULL_ENERGY = 1.0f
        const val REVIEW_THRESHOLD_HOURS = 36L
        const val CRITICAL_DECAY_HOURS = 72L
        const val ENERGY_REVIEW_REQUIRED_THRESHOLD = 0.70f

        const val RECHARGE_BONUS_XP = 30
        const val RECHARGE_BONUS_STARS = 5
    }

    /**
     * Tính toán mức năng lượng của hành tinh dựa trên thời điểm ôn tập cuối cùng.
     */
    fun calculatePlanetEnergy(
        userId: Long,
        planetIndex: Int,
        isCompleted: Boolean,
        totalCompletedPlanets: Int = 1,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Float {
        if (!isCompleted) return FULL_ENERGY

        val lastReview = getLastReviewTimestamp(userId, planetIndex)

        val effectiveTimestamp = if (lastReview <= 0L) {
            // Tự động phân tầng tự nhiên: Các hành tinh cũ hơn được xem là đã hoàn thành từ trước
            // để người học luôn có bài cần ôn tập luân phiên (khoảng cách 12-24h giữa các hành tinh)
            val offsetHours = if (totalCompletedPlanets > 1 && planetIndex < totalCompletedPlanets - 1) {
                // Ví dụ: hành tinh cách mốc hiện tại từ 38h trở lên
                38L + (totalCompletedPlanets - 1 - planetIndex) * 12L
            } else {
                0L
            }
            val simulatedTime = currentTimeMillis - TimeUnit.HOURS.toMillis(offsetHours)
            setLastReviewTimestamp(userId, planetIndex, simulatedTime)
            simulatedTime
        } else {
            lastReview
        }

        val elapsedMillis = (currentTimeMillis - effectiveTimestamp).coerceAtLeast(0L)
        val elapsedHours = TimeUnit.MILLISECONDS.toHours(elapsedMillis)

        return when {
            elapsedHours < 24 -> 1.0f
            elapsedHours < REVIEW_THRESHOLD_HOURS -> 0.85f
            elapsedHours < 48 -> 0.65f
            elapsedHours < CRITICAL_DECAY_HOURS -> 0.45f
            else -> 0.25f
        }
    }

    /**
     * Xác định xem hành tinh có cần tái nạp năng lượng (ôn tập) hay không.
     */
    fun isReviewNeeded(energy: Float): Boolean {
        return energy <= ENERGY_REVIEW_REQUIRED_THRESHOLD
    }

    fun getLastReviewTimestamp(userId: Long, planetIndex: Int): Long {
        val key = "last_review_${userId}_$planetIndex"
        return prefs?.getLong(key, 0L) ?: memoryStore[key] ?: 0L
    }

    fun setLastReviewTimestamp(userId: Long, planetIndex: Int, timestamp: Long) {
        val key = "last_review_${userId}_$planetIndex"
        prefs?.edit()?.putLong(key, timestamp)?.apply()
        memoryStore[key] = timestamp
    }

    /**
     * Tái nạp năng lượng hành tinh lên 100%.
     */
    fun rechargePlanet(userId: Long, planetIndex: Int, timestamp: Long = System.currentTimeMillis()) {
        setLastReviewTimestamp(userId, planetIndex, timestamp)
    }

    /**
     * Xóa dữ liệu ôn tập (dùng cho debug/test).
     */
    fun clearUserData(userId: Long) {
        val prefix = "last_review_${userId}_"
        prefs?.let {
            val editor = it.edit()
            for (key in it.all.keys) {
                if (key.startsWith(prefix)) editor.remove(key)
            }
            editor.apply()
        }
        memoryStore.keys.removeIf { it.startsWith(prefix) }
    }
}
