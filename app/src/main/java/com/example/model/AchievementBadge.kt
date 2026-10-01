package com.example.model

/**
 * Định nghĩa các huy hiệu thành tựu trong Kiki
 */
enum class BadgeId(val id: String) {
    NIGHT_OWL("night_owl"),                 // Cú Đêm Chăm Chỉ: Hoàn thành 1 bài học sau 22:00
    CHIEF_DOCTOR("chief_doctor"),           // Bác Sĩ Trưởng: Chữa khỏi 10 lỗi sai trong Phòng khám lỗi sai
    SUPER_MEMORY("super_memory"),           // Siêu Trí Nhớ: Học thuộc 30 thẻ Flashcard trong Sổ tay từ vựng
    PERSISTENT_SCHOLAR("persistent_scholar"),// Học Giả Bền Bỉ: Duy trì chuỗi Streak 7 ngày không gián đoạn
    FIRST_STEP("first_step"),               // Bước Đầu Tiên: Hoàn thành bài học tiếng Anh đầu tiên
    SPEAKING_ACE("speaking_ace"),           // Chiến Thần Phát Âm: Đạt điểm phát âm AI >= 85%
    ALL_STAGES_MASTERY("all_stages_mastery")// Bậc Thầy Tinh Thông: Hoàn thành toàn bộ các chặng học
}

data class AchievementBadge(
    val id: String,
    val titleVi: String,
    val titleEn: String,
    val descriptionVi: String,
    val descriptionEn: String,
    val targetCount: Int,
    val currentProgress: Int,
    val isUnlocked: Boolean,
    val iconKey: String
) {
    val progressFraction: Float
        get() = if (targetCount > 0) (currentProgress.toFloat() / targetCount).coerceIn(0f, 1f) else 0f
}

object AchievementRegistry {

    fun getAllBadges(
        unlockedIds: Set<String>,
        streak: Int = 0,
        curedWeakPoints: Int = 0,
        masteredVocabCount: Int = 0,
        completedLessons: Int = 0,
        hasNightOwl: Boolean = false,
        hasSpeakingAce: Boolean = false,
        allStagesCompleted: Boolean = false
    ): List<AchievementBadge> {
        return listOf(
            AchievementBadge(
                id = BadgeId.NIGHT_OWL.id,
                titleVi = "Cú Đêm Chăm Chỉ",
                titleEn = "Night Owl",
                descriptionVi = "Hoàn thành 1 bài học sau 22:00 đêm",
                descriptionEn = "Complete a lesson after 22:00 at night",
                targetCount = 1,
                currentProgress = if (hasNightOwl || unlockedIds.contains(BadgeId.NIGHT_OWL.id)) 1 else 0,
                isUnlocked = hasNightOwl || unlockedIds.contains(BadgeId.NIGHT_OWL.id),
                iconKey = "NIGHT_OWL"
            ),
            AchievementBadge(
                id = BadgeId.CHIEF_DOCTOR.id,
                titleVi = "Bác Sĩ Trưởng",
                titleEn = "Chief Doctor",
                descriptionVi = "Chữa khỏi hoàn toàn 10 lỗi sai trong Phòng khám lỗi sai",
                descriptionEn = "Cure 10 mistakes in the Weak-Point Clinic",
                targetCount = 10,
                currentProgress = curedWeakPoints.coerceAtMost(10),
                isUnlocked = curedWeakPoints >= 10 || unlockedIds.contains(BadgeId.CHIEF_DOCTOR.id),
                iconKey = "CHIEF_DOCTOR"
            ),
            AchievementBadge(
                id = BadgeId.SUPER_MEMORY.id,
                titleVi = "Siêu Trí Nhớ",
                titleEn = "Super Memory",
                descriptionVi = "Học thuộc 30 thẻ Flashcard trong Sổ tay từ vựng",
                descriptionEn = "Master 30 flashcard words in Vocabulary Vault",
                targetCount = 30,
                currentProgress = masteredVocabCount.coerceAtMost(30),
                isUnlocked = masteredVocabCount >= 30 || unlockedIds.contains(BadgeId.SUPER_MEMORY.id),
                iconKey = "SUPER_MEMORY"
            ),
            AchievementBadge(
                id = BadgeId.PERSISTENT_SCHOLAR.id,
                titleVi = "Học Giả Bền Bỉ",
                titleEn = "Persistent Scholar",
                descriptionVi = "Duy trì chuỗi Streak 7 ngày không gián đoạn",
                descriptionEn = "Maintain a 7-day learning streak without break",
                targetCount = 7,
                currentProgress = streak.coerceAtMost(7),
                isUnlocked = streak >= 7 || unlockedIds.contains(BadgeId.PERSISTENT_SCHOLAR.id),
                iconKey = "PERSISTENT_SCHOLAR"
            ),
            AchievementBadge(
                id = BadgeId.FIRST_STEP.id,
                titleVi = "Bước Đầu Tiên",
                titleEn = "First Step",
                descriptionVi = "Hoàn thành bài học tiếng Anh đầu tiên",
                descriptionEn = "Complete your first English lesson",
                targetCount = 1,
                currentProgress = if (completedLessons > 0 || unlockedIds.contains(BadgeId.FIRST_STEP.id)) 1 else 0,
                isUnlocked = completedLessons > 0 || unlockedIds.contains(BadgeId.FIRST_STEP.id),
                iconKey = "FIRST_STEP"
            ),
            AchievementBadge(
                id = BadgeId.SPEAKING_ACE.id,
                titleVi = "Chiến Thần Phát Âm",
                titleEn = "Speaking Ace",
                descriptionVi = "Đạt điểm phát âm AI >= 85% trong bài Luyện Nói",
                descriptionEn = "Score >= 85% accuracy in an AI Speaking Challenge",
                targetCount = 1,
                currentProgress = if (hasSpeakingAce || unlockedIds.contains(BadgeId.SPEAKING_ACE.id)) 1 else 0,
                isUnlocked = hasSpeakingAce || unlockedIds.contains(BadgeId.SPEAKING_ACE.id),
                iconKey = "SPEAKING_ACE"
            ),
            AchievementBadge(
                id = BadgeId.ALL_STAGES_MASTERY.id,
                titleVi = "Bậc Thầy Tinh Thông",
                titleEn = "Mastery",
                descriptionVi = "Hoàn thành toàn bộ các chặng học trong chương",
                descriptionEn = "Complete all journey stages in the chapter",
                targetCount = 1,
                currentProgress = if (allStagesCompleted || unlockedIds.contains(BadgeId.ALL_STAGES_MASTERY.id)) 1 else 0,
                isUnlocked = allStagesCompleted || unlockedIds.contains(BadgeId.ALL_STAGES_MASTERY.id),
                iconKey = "ALL_STAGES_MASTERY"
            )
        )
    }
}
