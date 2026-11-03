package com.example.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.LeagueSettlementResult
import com.example.model.LeagueTier
import com.example.model.LeagueZone
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeagueManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LeagueManager"
        private const val PREFS_NAME = "StudyMentorLeaguePrefs"
        private const val KEY_CURRENT_TIER = "key_league_tier"
        private const val KEY_WEEKLY_XP = "key_weekly_xp"
        private const val KEY_LAST_SETTLED_WEEK = "key_last_settled_week"
        private const val KEY_LAST_RECORDED_RANK = "key_last_recorded_rank"
        private const val KEY_LAST_SAVED_WEEK_FOR_XP = "key_last_saved_week_for_xp"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Lấy mã định danh của tuần hiện tại (bắt đầu từ Thứ 2 đến Chủ Nhật), ví dụ "2026-W45"
     */
    fun getCurrentWeekId(now: Calendar = Calendar.getInstance(Locale.getDefault())): String {
        val cal = now.clone() as Calendar
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.minimalDaysInFirstWeek = 4
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return String.format(Locale.US, "%04d-W%02d", year, week)
    }

    /**
     * Tính thời gian (mili-giây) còn lại cho đến 00:00:00 sáng Thứ Hai tuần tới
     */
    fun getMillisUntilNextMonday(now: Calendar = Calendar.getInstance(Locale.getDefault())): Long {
        val nextMonday = now.clone() as Calendar
        nextMonday.firstDayOfWeek = Calendar.MONDAY

        val dayOfWeek = nextMonday.get(Calendar.DAY_OF_WEEK)
        val daysToAdd = when (dayOfWeek) {
            Calendar.MONDAY -> 7
            Calendar.SUNDAY -> 1
            Calendar.SATURDAY -> 2
            Calendar.FRIDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.WEDNESDAY -> 5
            Calendar.TUESDAY -> 6
            else -> 7
        }

        nextMonday.add(Calendar.DAY_OF_YEAR, daysToAdd)
        nextMonday.set(Calendar.HOUR_OF_DAY, 0)
        nextMonday.set(Calendar.MINUTE, 0)
        nextMonday.set(Calendar.SECOND, 0)
        nextMonday.set(Calendar.MILLISECOND, 0)

        val remaining = nextMonday.timeInMillis - now.timeInMillis
        return remaining.coerceAtLeast(0L)
    }

    /**
     * Chuỗi hiển thị thân thiện: "3 ngày 14 giờ" hoặc "5 giờ 20 phút"
     */
    fun formatRemainingTime(millis: Long): String {
        val totalSec = millis / 1000
        val days = totalSec / (24 * 3600)
        val hours = (totalSec % (24 * 3600)) / 3600
        val minutes = (totalSec % 3600) / 60
        return when {
            days > 0 -> "${days} ngày ${hours} giờ"
            hours > 0 -> "${hours} giờ ${minutes} phút"
            else -> "${maxOf(1, minutes)} phút"
        }
    }

    private fun getWeeklyXpKey(userId: Long): String =
        if (userId > 0) "${userId}_$KEY_WEEKLY_XP" else KEY_WEEKLY_XP

    private fun getCurrentTierKey(userId: Long): String =
        if (userId > 0) "${userId}_$KEY_CURRENT_TIER" else KEY_CURRENT_TIER

    private fun getLastSavedWeekKey(userId: Long): String =
        if (userId > 0) "${userId}_$KEY_LAST_SAVED_WEEK_FOR_XP" else KEY_LAST_SAVED_WEEK_FOR_XP

    private fun getLastRecordedRankKey(userId: Long): String =
        if (userId > 0) "${userId}_$KEY_LAST_RECORDED_RANK" else KEY_LAST_RECORDED_RANK

    private fun getLastSettledWeekKey(userId: Long): String =
        if (userId > 0) "${userId}_$KEY_LAST_SETTLED_WEEK" else KEY_LAST_SETTLED_WEEK

    /**
     * Cấp bậc cúp hiện tại của người dùng (mặc định khởi đầu là Đồng - BRONZE)
     */
    fun getCurrentTier(userId: Long = -1L): LeagueTier {
        val key = getCurrentTierKey(userId)
        val raw = prefs.getString(key, null) ?: prefs.getString(KEY_CURRENT_TIER, LeagueTier.BRONZE.name)
        return LeagueTier.fromString(raw)
    }

    fun setCurrentTier(tier: LeagueTier, userId: Long = -1L) {
        val key = getCurrentTierKey(userId)
        prefs.edit().putString(key, tier.name).apply()
        if (userId > 0) {
            prefs.edit().putString(KEY_CURRENT_TIER, tier.name).apply()
        }
        Log.d(TAG, "Đã cập nhật cấp bậc giải đấu lên: ${tier.vietnameseTitle} (userId: $userId)")
    }

    /**
     * Điểm XP tích lũy trong tuần này
     */
    fun getWeeklyXp(userId: Long = -1L): Int {
        checkAndResetWeeklyXpIfNewWeek(userId)
        val key = getWeeklyXpKey(userId)
        return if (prefs.contains(key)) prefs.getInt(key, 0) else prefs.getInt(KEY_WEEKLY_XP, 0)
    }

    fun setWeeklyXp(amount: Int, userId: Long = -1L) {
        val key = getWeeklyXpKey(userId)
        prefs.edit()
            .putInt(key, amount)
            .putString(getLastSavedWeekKey(userId), getCurrentWeekId())
            .apply()
        if (userId > 0) {
            prefs.edit().putInt(KEY_WEEKLY_XP, amount).apply()
        }
    }

    /**
     * Tích lũy thêm XP vào bảng xếp hạng tuần
     */
    fun addWeeklyXp(amount: Int, userId: Long = -1L) {
        if (amount <= 0) return
        checkAndResetWeeklyXpIfNewWeek(userId)
        val current = getWeeklyXp(userId)
        val updated = current + amount
        setWeeklyXp(updated, userId)
        Log.d(TAG, "Cập nhật Weekly XP: +$amount => $updated XP (userId: $userId, Tuần: ${getCurrentWeekId()})")
    }

    /**
     * Lưu lại vị trí xếp hạng mới nhất của user trong tuần hiện tại
     */
    fun recordCurrentRank(rank: Int, userId: Long = -1L) {
        if (rank > 0) {
            val key = getLastRecordedRankKey(userId)
            prefs.edit().putInt(key, rank).apply()
            if (userId > 0) {
                prefs.edit().putInt(KEY_LAST_RECORDED_RANK, rank).apply()
            }
        }
    }

    /**
     * Kiểm tra và tự động kết toán giải đấu mỗi sáng Thứ 2 đầu tuần:
     * - Top 5: Thăng 1 cúp
     * - Top 6 - 15: Giữ nguyên cúp
     * - Top 16+: Tụt 1 cúp (nếu > Đồng)
     */
    fun checkAndSettleWeeklyLeague(userId: Long = -1L): LeagueSettlementResult? {
        val currentWeek = getCurrentWeekId()
        val lastSettledWeekKey = getLastSettledWeekKey(userId)
        val lastSavedWeekKey = getLastSavedWeekKey(userId)
        val lastSettledWeek = prefs.getString(lastSettledWeekKey, null) ?: prefs.getString(KEY_LAST_SETTLED_WEEK, null)

        // Nếu mới cài app lần đầu, khởi tạo tuần hiện tại mà không trừ/thăng điểm
        if (lastSettledWeek == null) {
            prefs.edit()
                .putString(lastSettledWeekKey, currentWeek)
                .putString(lastSavedWeekKey, currentWeek)
                .apply()
            return null
        }

        // Nếu vẫn còn trong tuần đã kết toán thì bỏ qua
        if (currentWeek == lastSettledWeek) {
            return null
        }

        // Đã bước sang tuần mới (Thứ 2 đầu tuần)! Bắt đầu kết toán kết quả tuần trước
        val oldTier = getCurrentTier(userId)
        val rankKey = getLastRecordedRankKey(userId)
        val finalRank = prefs.getInt(rankKey, prefs.getInt(KEY_LAST_RECORDED_RANK, 10))

        val result: LeagueSettlementResult = when {
            finalRank in 1..5 -> {
                val newTier = oldTier.nextTier()
                setCurrentTier(newTier, userId)
                LeagueSettlementResult.Promoted(oldTier, newTier, finalRank, lastSettledWeek)
            }
            finalRank in 6..15 -> {
                LeagueSettlementResult.Maintained(oldTier, finalRank, lastSettledWeek)
            }
            else -> {
                val newTier = oldTier.previousTier()
                setCurrentTier(newTier, userId)
                LeagueSettlementResult.Demoted(oldTier, newTier, finalRank, lastSettledWeek)
            }
        }

        // Đặt lại điểm XP tuần mới = 0 và cập nhật mốc tuần đã kết toán
        val xpKey = getWeeklyXpKey(userId)
        prefs.edit()
            .putInt(xpKey, 0)
            .putString(lastSettledWeekKey, currentWeek)
            .putString(lastSavedWeekKey, currentWeek)
            .putInt(rankKey, 0)
            .apply()

        Log.i(TAG, "Kết toán mùa giải tuần $lastSettledWeek (userId: $userId): Kết quả = $result")
        return result
    }

    /**
     * Xác định vùng xếp hạng dựa vào thứ hạng và cúp
     */
    fun determineZone(rank: Int, tier: LeagueTier): LeagueZone {
        return when {
            rank in 1..5 -> LeagueZone.PROMOTION
            rank in 6..15 -> LeagueZone.SAFE
            else -> {
                if (tier == LeagueTier.BRONZE) LeagueZone.SAFE else LeagueZone.DEMOTION
            }
        }
    }

    private fun checkAndResetWeeklyXpIfNewWeek(userId: Long = -1L) {
        val currentWeek = getCurrentWeekId()
        val keyWeek = getLastSavedWeekKey(userId)
        val lastSavedWeek = prefs.getString(keyWeek, null)
        if (lastSavedWeek != null && lastSavedWeek != currentWeek) {
            val keyXp = getWeeklyXpKey(userId)
            prefs.edit()
                .putInt(keyXp, 0)
                .putString(keyWeek, currentWeek)
                .apply()
        }
    }
}
