package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R

/**
 * 5 Cấp bậc Giải đấu (League Tiers) của Kiki tương ứng với 5 Cúp danh dự.
 */
enum class LeagueTier(
    val tierName: String,
    val vietnameseTitle: String,
    val subtitle: String,
    @param:DrawableRes val trophyResId: Int,
    val primaryColor: Color,
    val accentColor: Color,
    val order: Int
) {
    BRONZE(
        tierName = "Bronze",
        vietnameseTitle = "Giải Đồng",
        subtitle = "Người Tò Mò Tìm Tòi",
        trophyResId = R.drawable.trophy_bronze,
        primaryColor = Color(0xFFCD7F32),
        accentColor = Color(0xFFFFB86C),
        order = 1
    ),
    SILVER(
        tierName = "Silver",
        vietnameseTitle = "Giải Bạc",
        subtitle = "Người Học Siêng Năng",
        trophyResId = R.drawable.trophy_silver,
        primaryColor = Color(0xFFC0C0C0),
        accentColor = Color(0xFFE2E8F0),
        order = 2
    ),
    GOLD(
        tierName = "Gold",
        vietnameseTitle = "Giải Vàng",
        subtitle = "Nhà Học Giả Xuất Chúng",
        trophyResId = R.drawable.trophy_gold,
        primaryColor = Color(0xFFFFD166),
        accentColor = Color(0xFFFFA500),
        order = 3
    ),
    PLATINUM(
        tierName = "Platinum",
        vietnameseTitle = "Giải Bạch Kim",
        subtitle = "Bậc Thầy Tốt Nghiệp",
        trophyResId = R.drawable.trophy_platinum,
        primaryColor = Color(0xFF00E5FF),
        accentColor = Color(0xFF80D8FF),
        order = 4
    ),
    DIAMOND(
        tierName = "Diamond",
        vietnameseTitle = "Giải Kim Cương",
        subtitle = "Đại Học Giả Uyên Bác",
        trophyResId = R.drawable.trophy_diamond,
        primaryColor = Color(0xFFB388FF),
        accentColor = Color(0xFF51FAC1),
        order = 5
    );

    /**
     * Thăng lên 1 cúp cao hơn (tối đa là Kim Cương)
     */
    fun nextTier(): LeagueTier {
        return when (this) {
            BRONZE -> SILVER
            SILVER -> GOLD
            GOLD -> PLATINUM
            PLATINUM -> DIAMOND
            DIAMOND -> DIAMOND
        }
    }

    /**
     * Tụt xuống 1 cúp thấp hơn (tối thiểu là Đồng)
     */
    fun previousTier(): LeagueTier {
        return when (this) {
            DIAMOND -> PLATINUM
            PLATINUM -> GOLD
            GOLD -> SILVER
            SILVER -> BRONZE
            BRONZE -> BRONZE
        }
    }

    companion object {
        fun fromString(value: String?): LeagueTier {
            if (value.isNullOrBlank()) return BRONZE
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                it.tierName.equals(value, ignoreCase = true) ||
                it.vietnameseTitle.contains(value, ignoreCase = true)
            } ?: BRONZE
        }
    }
}

/**
 * 3 Vùng phân hạng trong giải đấu:
 * - PROMOTION: Top 1 - 5 (Thăng hạng)
 * - SAFE: Top 6 - 15 (Giữ nguyên hạng)
 * - DEMOTION: Top 16 trở đi (Rớt 1 cúp nếu đang ở Giải > Đồng)
 */
enum class LeagueZone(val label: String, val color: Color) {
    PROMOTION("Thăng hạng", Color(0xFF51FAC1)),
    SAFE("Trụ hạng", Color(0xFF94A3B8)),
    DEMOTION("Nguy cơ tụt hạng", Color(0xFFFF5C5C))
}

/**
 * Kết quả kết toán giải đấu đầu tuần mỗi Thứ 2
 */
sealed class LeagueSettlementResult {
    data class Promoted(
        val oldTier: LeagueTier,
        val newTier: LeagueTier,
        val finalRank: Int,
        val weekId: String
    ) : LeagueSettlementResult()

    data class Maintained(
        val currentTier: LeagueTier,
        val finalRank: Int,
        val weekId: String
    ) : LeagueSettlementResult()

    data class Demoted(
        val oldTier: LeagueTier,
        val newTier: LeagueTier,
        val finalRank: Int,
        val weekId: String
    ) : LeagueSettlementResult()
}

/**
 * Mô hình đại diện cho một người học trong bảng xếp hạng giải đấu
 */
data class RankPlayer(
    val rank: Int,
    val name: String,
    val title: String,
    val score: Int,
    val avatarUrl: String,
    val isCurrentUser: Boolean = false,
    val zone: LeagueZone = LeagueZone.SAFE,
    val streak: Int = 0,
    val tier: LeagueTier = LeagueTier.BRONZE,
    val uid: String = ""
)

